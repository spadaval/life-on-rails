// Overnight close (01:30 local): tidy yesterday so it is ready to confirm in
// one pass. It never confirms anything and calls no model. The scheduler runs
// it once after a boot that missed 01:30.
import type { Sql } from "../../db/client.js";
import { addDays, localDayBounds, localYmd } from "../../shared/localTime.js";
import { lockBlocks } from "../attribution/fill.js";
import { env } from "../env.js";
import { wakeSnoozed } from "../intents.js";
import { recomputeScores } from "../scoring/recompute.js";
import { uuidv7 } from "../../shared/ids.js";

/** Two blocks with the same label this close together were one stretch of work. */
const MERGE_GAP_MS = 120_000;

export interface OvernightStats {
  date: string;
  merged: number;
  proposals: number;
  woken: number;
  scored: number;
  [key: string]: unknown;
}

export async function overnightClose(db: Sql, now = new Date()): Promise<OvernightStats> {
  const date = addDays(localYmd(env.LOR_TZ, now), -1);
  const { start, end } = localDayBounds(env.LOR_TZ, date);
  let merged = 0;
  let proposals = 0;

  await db.begin(async (tx) => {
    await lockBlocks(tx);
    // Merge adjacent uncommitted timer and activity blocks with the same label.
    const rows = await tx<{ id: string; t_start: Date; t_end: Date; intent_id: string | null; title: string | null }[]>`
      SELECT id, t_start, t_end, intent_id, title FROM time_block
      WHERE NOT committed AND origin <> 'calendar' AND t_end IS NOT NULL AND t_start >= ${start} AND t_start < ${end}
      ORDER BY t_start`;
    type Row = (typeof rows)[number];
    const sameStretch = (a: Row, b: Row) =>
      a.intent_id === b.intent_id && (a.title ?? null) === (b.title ?? null) && b.t_start.getTime() - a.t_end.getTime() <= MERGE_GAP_MS;
    let keep: Row | undefined;
    for (const b of rows) {
      if (!keep || !sameStretch(keep, b)) {
        keep = b;
        continue;
      }
      await tx`UPDATE time_block_observation SET block_id = ${keep.id} WHERE block_id = ${b.id}`;
      await tx`DELETE FROM time_block WHERE id = ${b.id}`;
      await tx`UPDATE time_block SET t_end = ${b.t_end}, origin = 'merged', updated_at = now() WHERE id = ${keep.id}`;
      keep.t_end = b.t_end;
      merged++;
    }
    // Every closed, unconfirmed block gets a label to accept, unless one was already turned down.
    const bare = await tx<{ id: string; intent_id: string }[]>`
      SELECT b.id, b.intent_id FROM time_block b
      WHERE NOT b.committed AND b.t_end IS NOT NULL AND b.intent_id IS NOT NULL AND b.t_start >= ${start} AND b.t_start < ${end}
        AND NOT EXISTS (SELECT 1 FROM proposal p WHERE p.time_block_id = b.id AND p.status IN ('pending', 'rejected'))`;
    for (const b of bare) {
      await tx`INSERT INTO proposal (id, confidence, time_block_id, intent_id, payload, via)
        VALUES (${uuidv7()}, 0.5, ${b.id}, ${b.intent_id}, ${tx.json({ source: "overnight" })}, 'job')`;
      proposals++;
    }
  });

  const woken = await wakeSnoozed(db);
  const scored = await recomputeScores(db, now);
  return { date, merged, proposals, woken, scored: scored.intents };
}
