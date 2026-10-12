// time_block state helpers (spec: state machine). All writers run inside a
// caller-owned transaction (postgres.js Sql instance). Scoring truth stays in
// `commitment`; these helpers keep the denormalized (committed, commitment_id)
// in the same transaction.
import type postgres from "postgres";
import { uuidv7 } from "../../shared/ids.js";

export type Tx = postgres.TransactionSql | postgres.Sql;

export interface BlockRow {
  id: string;
  t_start: Date;
  t_end: Date | null;
  paused_at: Date | null;
  origin: string;
  intent_id: string | null;
  calendar_id: string | null;
  calendar_event_id: string | null;
  calendar_t_start: Date | null;
  committed: boolean;
  commitment_id: string | null;
}

export async function getOpenBlock(tx: Tx): Promise<BlockRow | null> {
  const rows = (await tx`SELECT * FROM time_block WHERE t_end IS NULL LIMIT 1`) as BlockRow[];
  return rows[0] ?? null;
}

export async function closeOpenBlock(tx: Tx, now: Date): Promise<BlockRow | null> {
  const rows = (await tx`UPDATE time_block SET t_end = ${now}, updated_at = ${now}
    WHERE t_end IS NULL RETURNING *`) as BlockRow[];
  return rows[0] ?? null;
}

export async function ensureAttributionProposal(
  tx: Tx,
  blockId: string,
  intentId: string,
  source: string,
  confidence: number,
  extra?: { calendarEventId?: string; ruleId?: string },
): Promise<void> {
  const existing = (await tx`SELECT id FROM proposal
    WHERE kind = 'time_attribution' AND status = 'pending' AND time_block_id = ${blockId} LIMIT 1`) as { id: string }[];
  if (existing.length > 0) return;
  await tx`INSERT INTO proposal (id, kind, status, confidence, time_block_id, intent_id, payload)
    VALUES (${uuidv7()}, 'time_attribution', 'pending', ${confidence}, ${blockId}, ${intentId},
      ${JSON.stringify({ source, calendar_event_id: extra?.calendarEventId, rule_id: extra?.ruleId })})`;
}

/**
 * clipCalendarFill(interval): replace every UNCOMMITTED origin=calendar block
 * overlapping [s,e) with its fragments minus occupied. Committed calendar rows
 * are never touched (mismatch chip instead). Pending proposals on deleted
 * fragments cascade-delete; new fragments get fresh pending proposals.
 */
export async function clipCalendarFill(tx: Tx, s: Date, e: Date): Promise<void> {
  const overlapping = (await tx`SELECT id, t_start, t_end, calendar_id, calendar_event_id,
      calendar_t_start, intent_id
    FROM time_block
    WHERE origin = 'calendar' AND committed = false
      AND tstzrange(t_start, t_end) && tstzrange(${s}, ${e})`) as {
    id: string; t_start: Date; t_end: Date; calendar_id: string | null;
    calendar_event_id: string | null; calendar_t_start: Date | null; intent_id: string | null;
  }[];
  for (const b of overlapping) {
    // App invariant: calendar fill always carries an intent (Meetings default).
    if (!b.intent_id) throw new Error(`calendar fill block ${b.id} has null intent_id`);
    const bs = b.t_start.getTime();
    const be = b.t_end.getTime();
    const cs = s.getTime();
    const ce = e.getTime();
    const frags: { fs: Date; fe: Date }[] = [];
    if (bs < cs) frags.push({ fs: b.t_start, fe: new Date(Math.min(be, cs)) });
    if (be > ce) frags.push({ fs: new Date(Math.max(bs, ce)), fe: b.t_end });
    await tx`DELETE FROM time_block WHERE id = ${b.id}`;
    for (const f of frags) {
      if (!(f.fe.getTime() > f.fs.getTime())) continue;
      const nid = uuidv7();
      await tx`INSERT INTO time_block (id, t_start, t_end, origin, intent_id, calendar_id,
          calendar_event_id, calendar_t_start, committed)
        VALUES (${nid}, ${f.fs}, ${f.fe}, 'calendar', ${b.intent_id}, ${b.calendar_id},
          ${b.calendar_event_id}, ${b.calendar_t_start}, false)`;
      await ensureAttributionProposal(tx, nid, b.intent_id, "calendar", 0.7, {
        calendarEventId: b.calendar_event_id ?? undefined,
      });
    }
  }
}

/** refillCalendarHoles(interval): re-run syncCalendarFill per overlapping event. */
export async function eventsOverlapping(tx: Tx, s: Date, e: Date): Promise<{ calendar_id: string; event_id: string }[]> {
  return (await tx`SELECT calendar_id, event_id FROM google_event
    WHERE status <> 'cancelled' AND all_day = false
      AND tstzrange(t_start, t_end) && tstzrange(${s}, ${e})`) as {
    calendar_id: string; event_id: string;
  }[];
}
