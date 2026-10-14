// The time-block state machine. A user-visible interval is always a
// `time_block`; accepting its label inserts a `commitment` and sets the block's
// denormalized (committed, commitment_id) in the same transaction. Every writer
// here takes the block lock first and re-syncs calendar fill before it commits,
// so "no overlapping actual seconds" holds at every commit.
import type { Sql, Tx } from "../../db/client.js";
import { systemIntentId } from "../../db/seed.js";
import { uuidv7 } from "../../shared/ids.js";
import { type Ymd, addDays, localDayBounds, localDayStart, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { BadInput, NotFound, Refused } from "../errors.js";
import { recordObservation } from "../observations.js";
import { scheduleRecompute } from "../scoring/recompute.js";
import { loadRules, lockBlocks, syncCalendarFill } from "./fill.js";

export interface Who {
  via: "ui" | "agent" | "rule" | "job";
  sourceRef?: string | null;
}

/** A timer shorter than this was a mis-tap: it is dropped, not kept as a block. */
const MIN_BLOCK_MS = 60_000;

interface BlockRow {
  id: string;
  t_start: Date;
  t_end: Date | null;
  paused_at: Date | null;
  origin: "auto_activity" | "calendar" | "manual" | "merged";
  intent_id: string | null;
  calendar_id: string | null;
  calendar_event_id: string | null;
  title: string | null;
  committed: boolean;
  commitment_id: string | null;
}

export const hoursBetween = (a: Date, b: Date) => Math.max(0, (b.getTime() - a.getTime()) / 3_600_000);

/** The window fill is re-synced over after a block changes: the poll window, widened to the block. */
export function fillWindow(now: Date, ...around: (Date | null | undefined)[]): { from: Date; to: Date } {
  const today = localYmd(env.LOR_TZ, now);
  let from = localDayStart(env.LOR_TZ, addDays(today, -1)).getTime();
  let to = localDayStart(env.LOR_TZ, addDays(today, 3)).getTime();
  for (const d of around) {
    if (!d) continue;
    from = Math.min(from, d.getTime());
    to = Math.max(to, d.getTime());
  }
  return { from: new Date(from), to: new Date(to) };
}

async function getBlock(tx: Tx, id: string): Promise<BlockRow> {
  const rows = await tx<BlockRow[]>`SELECT * FROM time_block WHERE id = ${id} FOR UPDATE`;
  if (!rows[0]) throw new NotFound(`no block ${id}`);
  return rows[0];
}

async function openBlock(tx: Tx): Promise<BlockRow | null> {
  const rows = await tx<BlockRow[]>`SELECT * FROM time_block WHERE t_end IS NULL FOR UPDATE`;
  return rows[0] ?? null;
}

async function pendingProposal(tx: Tx, blockId: string, intentId: string, confidence: number, who: Who, source: string, reason?: string | null): Promise<string> {
  await tx`UPDATE proposal SET status = 'expired', resolved_at = now() WHERE time_block_id = ${blockId} AND status = 'pending'`;
  const id = uuidv7();
  await tx`INSERT INTO proposal (id, confidence, time_block_id, intent_id, payload, reason, via, source_ref)
    VALUES (${id}, ${confidence}, ${blockId}, ${intentId}, ${tx.json({ source })}, ${reason ?? null}, ${who.via}, ${who.sourceRef ?? null})`;
  return id;
}

/** Close the open block at `at`. Returns it, or null if nothing was running or it was too short to keep. */
async function closeOpen(tx: Tx, at: Date, who: Who): Promise<BlockRow | null> {
  const open = await openBlock(tx);
  if (!open) return null;
  // A paused block ends when it was paused: the pause is not time spent.
  const end = open.paused_at ?? at;
  if (end.getTime() - open.t_start.getTime() < MIN_BLOCK_MS) {
    await tx`DELETE FROM time_block WHERE id = ${open.id}`;
    return null;
  }
  await tx`UPDATE time_block SET t_end = ${end}, paused_at = NULL, updated_at = now() WHERE id = ${open.id}`;
  const intentId = open.intent_id ?? (await systemIntentId(tx, "Uncategorized"));
  if (!open.committed) await pendingProposal(tx, open.id, intentId, 1, who, "manual");
  return { ...open, t_end: end, paused_at: null };
}

async function resync(tx: Tx, now: Date, ...around: (Date | null | undefined)[]): Promise<void> {
  const { from, to } = fillWindow(now, ...around);
  await syncCalendarFill(tx, from, to);
}

// ---- the timer ----

export interface StartInput {
  intentId?: string | null;
  title?: string | null;
}

export async function startManualBlock(db: Sql, input: StartInput, who: Who, now = new Date()): Promise<{ id: string }> {
  const id = uuidv7();
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    await closeOpen(tx, now, who);
    const intentId = input.intentId ?? (await systemIntentId(tx, "Uncategorized"));
    const found = await tx`SELECT 1 FROM intent WHERE id = ${intentId} AND status <> 'archived'`;
    if (found.length === 0) throw new NotFound(`no intent ${intentId}`);
    await tx`INSERT INTO time_block (id, t_start, origin, intent_id, title) VALUES (${id}, ${now}, 'manual', ${intentId}, ${input.title?.trim() || null})`;
    await recordObservation(tx, { tStart: now, tEnd: now, device: "buildbot", source: "manual", title: input.title?.trim() || "timer started", payload: { block_id: id, via: who.via }, intentHint: intentId });
    await resync(tx, now);
  });
  return { id };
}

export async function stopRunningBlock(db: Sql, who: Who, now = new Date()): Promise<{ id: string | null }> {
  let closed: BlockRow | null = null;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    closed = await closeOpen(tx, now, who);
    await resync(tx, now, (closed as BlockRow | null)?.t_start);
  });
  return { id: (closed as BlockRow | null)?.id ?? null };
}

export async function pauseRunningBlock(db: Sql, now = new Date()): Promise<{ id: string | null }> {
  const rows = await db<{ id: string }[]>`
    UPDATE time_block SET paused_at = ${now}, updated_at = now() WHERE t_end IS NULL AND paused_at IS NULL RETURNING id`;
  return { id: rows[0]?.id ?? null };
}

/**
 * Resume: the paused block ends where it was paused and a new block starts
 * now with the same label, so a block's interval never contains a pause and
 * hours can always be computed from intervals.
 */
export async function resumeRunningBlock(db: Sql, who: Who, now = new Date()): Promise<{ id: string | null }> {
  let id: string | null = null;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const open = await openBlock(tx);
    if (!open || !open.paused_at) return;
    await closeOpen(tx, now, who);
    id = uuidv7();
    await tx`INSERT INTO time_block (id, t_start, origin, intent_id, title) VALUES (${id}, ${now}, 'manual', ${open.intent_id}, ${open.title})`;
    await resync(tx, now, open.t_start);
  });
  return { id };
}

// ---- labelling ----

/** Commit a block to an intent, replacing any earlier commitment for it. Closes it first if it is running. */
async function commitBlock(tx: Tx, block: BlockRow, intentId: string, proposalId: string | null, who: Who, now: Date): Promise<void> {
  const intent = await tx<{ status: string }[]>`SELECT status FROM intent WHERE id = ${intentId}`;
  if (!intent[0] || intent[0].status === "archived") throw new NotFound(`no intent ${intentId}`);
  let end = block.t_end;
  if (!end) {
    end = block.paused_at ?? now;
    await tx`UPDATE time_block SET t_end = ${end}, paused_at = NULL, updated_at = now() WHERE id = ${block.id}`;
  }
  if (proposalId) {
    await tx`UPDATE proposal SET status = 'accepted', resolved_at = now() WHERE id = ${proposalId}`;
  }
  // Any other suggestion for this block is now moot.
  await tx`UPDATE proposal SET status = 'rejected', resolved_at = now()
    WHERE time_block_id = ${block.id} AND status = 'pending' AND id IS DISTINCT FROM ${proposalId}`;
  await tx`DELETE FROM commitment WHERE kind = 'time_spent' AND time_block_id = ${block.id}`;
  const cid = uuidv7();
  await tx`INSERT INTO commitment (id, kind, intent_id, t, t_end, value, proposal_id, time_block_id, via, source_ref)
    VALUES (${cid}, 'time_spent', ${intentId}, ${block.t_start}, ${end}, ${hoursBetween(block.t_start, end)},
      ${proposalId}, ${block.id}, ${who.via}, ${who.sourceRef ?? null})`;
  await tx`UPDATE time_block SET intent_id = ${intentId}, committed = true, commitment_id = ${cid}, updated_at = now()
    WHERE id = ${block.id}`;
}

export interface RecategorizeOptions {
  /** For a calendar block: label every future event with this exact title the same way. */
  rememberTitle?: boolean;
}

export async function recategorizeBlock(db: Sql, blockId: string, intentId: string, who: Who, opts: RecategorizeOptions = {}, now = new Date()): Promise<void> {
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    await commitBlock(tx, block, intentId, null, who, now);
    if (opts.rememberTitle && block.origin === "calendar" && block.title) {
      const pattern = `^${block.title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`;
      if (pattern.length <= 200) {
        await tx`DELETE FROM attribution_rule WHERE match_title = ${pattern} AND match_app IS NULL`;
        await tx`INSERT INTO attribution_rule (id, match_title, intent_id) VALUES (${uuidv7()}, ${pattern}, ${intentId})`;
      }
    }
    await resync(tx, now, block.t_start, block.t_end);
  });
  scheduleRecompute();
}

/** Remove a block's commitment: it goes back to being a suggestion. */
export async function uncommitBlock(db: Sql, blockId: string, now = new Date()): Promise<void> {
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (!block.committed) return;
    await tx`DELETE FROM commitment WHERE kind = 'time_spent' AND time_block_id = ${blockId}`;
    await tx`UPDATE time_block SET committed = false, commitment_id = NULL, updated_at = now() WHERE id = ${blockId}`;
    if (block.intent_id) await pendingProposal(tx, blockId, block.intent_id, 0.7, { via: "ui" }, "undo");
    await resync(tx, now, block.t_start, block.t_end);
  });
  scheduleRecompute();
}

/** The agent (or a rule) suggests what a block was. One pending suggestion per block. */
export async function proposeTime(db: Sql, blockId: string, intentId: string, who: Who, reason?: string | null): Promise<{ id: string }> {
  let id = "";
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (block.committed) throw new Refused("already-committed", "that block is already confirmed; only the owner changes it");
    const intent = await tx`SELECT 1 FROM intent WHERE id = ${intentId} AND status <> 'archived'`;
    if (intent.length === 0) throw new NotFound(`no intent ${intentId}`);
    id = await pendingProposal(tx, blockId, intentId, 0.8, who, who.via, reason);
    await tx`UPDATE time_block SET intent_id = ${intentId}, updated_at = now() WHERE id = ${blockId}`;
  });
  return { id };
}

// ---- fixing a block ----

async function refuseOverlap(tx: Tx, blockId: string, start: Date, end: Date): Promise<void> {
  const clash = await tx<{ id: string; title: string | null }[]>`
    SELECT id, title FROM time_block
    WHERE id <> ${blockId} AND origin <> 'calendar' AND t_start < ${end} AND coalesce(t_end, 'infinity') > ${start} LIMIT 1`;
  if (clash[0]) throw new Refused("overlaps", "that would overlap another block", { block_id: clash[0].id });
}

export async function editBlock(db: Sql, blockId: string, patch: { tStart?: Date; tEnd?: Date; title?: string | null }, now = new Date()): Promise<void> {
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (block.origin === "calendar") throw new Refused("calendar-block", "a calendar block follows its event; relabel it, or start a timer over it");
    const start = patch.tStart ?? block.t_start;
    const end = patch.tEnd ?? block.t_end;
    if (patch.tEnd && !block.t_end) throw new BadInput("stop the timer before changing its end");
    if (start.getTime() > now.getTime()) throw new BadInput("a block cannot start in the future");
    if (end && !(end.getTime() > start.getTime())) throw new BadInput("a block must end after it starts");
    if (end && end.getTime() > now.getTime()) throw new BadInput("a block cannot end in the future");
    await refuseOverlap(tx, blockId, start, end ?? new Date(8.64e15));
    await tx`UPDATE time_block SET t_start = ${start}, t_end = ${end},
        title = ${patch.title !== undefined ? patch.title : block.title}, updated_at = now() WHERE id = ${blockId}`;
    if (block.commitment_id && end) {
      await tx`UPDATE commitment SET t = ${start}, t_end = ${end}, value = ${hoursBetween(start, end)} WHERE id = ${block.commitment_id}`;
    }
    await resync(tx, now, block.t_start, block.t_end, start, end);
  });
  scheduleRecompute();
}

export async function deleteBlock(db: Sql, blockId: string, now = new Date()): Promise<void> {
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (block.origin === "calendar") throw new Refused("calendar-block", "a calendar block follows its event; reject its label instead");
    await tx`DELETE FROM commitment WHERE time_block_id = ${blockId}`;
    await tx`DELETE FROM time_block WHERE id = ${blockId}`;
    await resync(tx, now, block.t_start, block.t_end);
  });
  scheduleRecompute();
}

/** Split a closed block at `at`: the left part keeps the id, the right part is new. */
export async function splitBlock(db: Sql, blockId: string, at: Date, now = new Date()): Promise<{ rightId: string }> {
  const rightId = uuidv7();
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const b = await getBlock(tx, blockId);
    if (!b.t_end) throw new BadInput("stop the timer before splitting it");
    if (!(at.getTime() > b.t_start.getTime() && at.getTime() < b.t_end.getTime())) {
      throw new BadInput("the split point must be inside the block");
    }
    await tx`UPDATE time_block SET t_end = ${at}, updated_at = now() WHERE id = ${blockId}`;
    await tx`INSERT INTO time_block (id, t_start, t_end, origin, intent_id, calendar_id, calendar_event_id, title)
      VALUES (${rightId}, ${at}, ${b.t_end}, ${b.origin}, ${b.intent_id}, ${b.calendar_id}, ${b.calendar_event_id}, ${b.title})`;
    await tx`UPDATE time_block_observation SET block_id = ${rightId}
      WHERE block_id = ${blockId} AND observation_id IN (
        SELECT id FROM observation WHERE t_start + (coalesce(t_end, t_start) - t_start) / 2 >= ${at})`;
    if (b.committed && b.commitment_id) {
      const c = (await tx<{ intent_id: string; via: string; source_ref: string | null }[]>`
        SELECT intent_id, via, source_ref FROM commitment WHERE id = ${b.commitment_id}`)[0]!;
      await tx`UPDATE commitment SET t_end = ${at}, value = ${hoursBetween(b.t_start, at)} WHERE id = ${b.commitment_id}`;
      const cid = uuidv7();
      await tx`INSERT INTO commitment (id, kind, intent_id, t, t_end, value, time_block_id, via, source_ref)
        VALUES (${cid}, 'time_spent', ${c.intent_id}, ${at}, ${b.t_end}, ${hoursBetween(at, b.t_end)}, ${rightId}, ${c.via}, ${c.source_ref})`;
      await tx`UPDATE time_block SET committed = true, commitment_id = ${cid} WHERE id = ${rightId}`;
    } else {
      // Each half carries its own pending suggestion.
      const p = await tx<{ intent_id: string | null; confidence: number; via: string; source_ref: string | null; payload: unknown; reason: string | null }[]>`
        SELECT intent_id, confidence, via, source_ref, payload, reason FROM proposal
        WHERE time_block_id = ${blockId} AND status = 'pending' LIMIT 1`;
      if (p[0]?.intent_id) {
        await tx`INSERT INTO proposal (id, confidence, time_block_id, intent_id, payload, reason, via, source_ref)
          VALUES (${uuidv7()}, ${p[0].confidence}, ${rightId}, ${p[0].intent_id}, ${tx.json((p[0].payload ?? {}) as never)}, ${p[0].reason}, ${p[0].via}, ${p[0].source_ref})`;
      }
    }
    // A split calendar block is two fragments the calendar did not ask for; fill sync would merge them back.
    if (b.origin !== "calendar") await resync(tx, now, b.t_start, b.t_end);
  });
  scheduleRecompute();
  return { rightId };
}

// ---- the evening confirm ----

export interface ConfirmInput {
  date: Ymd;
  /** Proposal ids. */
  accept?: string[];
  reject?: string[];
  /** Accept every other pending proposal for that date. */
  all?: boolean;
}

export async function confirmDay(db: Sql, input: ConfirmInput, who: Who, now = new Date()): Promise<{ accepted: number; rejected: number }> {
  const { start, end } = localDayBounds(env.LOR_TZ, input.date);
  let accepted = 0;
  let rejected = 0;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const reject = input.reject ?? [];
    if (reject.length > 0) {
      const r = await tx`UPDATE proposal SET status = 'rejected', resolved_at = now()
        WHERE id = ANY(${reject}::uuid[]) AND status = 'pending' RETURNING id`;
      rejected = r.length;
    }
    const ids = new Set(input.accept ?? []);
    if (input.all) {
      // Only blocks that have ended: nobody confirms a meeting that has not happened.
      const pending = await tx<{ id: string }[]>`
        SELECT p.id FROM proposal p JOIN time_block b ON b.id = p.time_block_id
        WHERE p.status = 'pending' AND b.t_start >= ${start} AND b.t_start < ${end}
          AND b.t_end IS NOT NULL AND b.t_end <= ${now}`;
      for (const p of pending) ids.add(p.id);
    }
    for (const pid of ids) {
      const rows = await tx<{ time_block_id: string; intent_id: string | null }[]>`
        SELECT time_block_id, intent_id FROM proposal WHERE id = ${pid} AND status = 'pending'`;
      const p = rows[0];
      if (!p || !p.intent_id) continue;
      const block = await getBlock(tx, p.time_block_id);
      await commitBlock(tx, block, p.intent_id, pid, who, now);
      accepted++;
    }
    await resync(tx, now, start, end);
  });
  if (accepted > 0) scheduleRecompute();
  return { accepted, rejected };
}

/**
 * After a poll: commit ended calendar blocks whose label came from a rule the
 * owner marked auto-apply. A rule the owner set up is the owner's confirmation.
 */
export async function applyAutoRules(tx: Tx, from: Date, to: Date, now = new Date()): Promise<number> {
  const rules = (await loadRules(tx)).filter((r) => r.autoApply);
  if (rules.length === 0) return 0;
  const ruleIds = rules.map((r) => r.id);
  const rows = await tx<{ block_id: string; proposal_id: string; intent_id: string }[]>`
    SELECT b.id AS block_id, p.id AS proposal_id, p.intent_id
    FROM time_block b JOIN proposal p ON p.time_block_id = b.id AND p.status = 'pending'
    WHERE NOT b.committed AND b.t_end IS NOT NULL AND b.t_end <= ${now} AND b.t_start >= ${from} AND b.t_start < ${to}
      AND p.intent_id IS NOT NULL AND p.payload->>'rule_id' = ANY(${ruleIds}::text[])`;
  for (const r of rows) {
    await commitBlock(tx, await getBlock(tx, r.block_id), r.intent_id, r.proposal_id, { via: "rule", sourceRef: null }, now);
  }
  return rows.length;
}
