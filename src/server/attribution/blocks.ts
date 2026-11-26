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
import { placeOf } from "./activity.js";
import { loadPlaces, loadRules, lockBlocks, syncFill } from "./fill.js";
import { clockNow } from "../clock.js";
import type { JsonObject } from "../../shared/json.js";

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
  origin: "auto_activity" | "calendar" | "manual" | "merged" | "place";
  intent_id: string | null;
  calendar_id: string | null;
  calendar_event_id: string | null;
  title: string | null;
  committed: boolean;
  commitment_id: string | null;
}

/** Blocks that follow evidence (the calendar, the phone, a place) rather than the owner's own timing. */
export const FILL_ORIGINS = ["calendar", "auto_activity", "place"] as const;
const isFill = (origin: string) => (FILL_ORIGINS as readonly string[]).includes(origin);
const followsWhat = (origin: string) => (origin === "calendar" ? "its event" : origin === "place" ? "where you were" : "your phone use");

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
  await tx`UPDATE proposal SET status = 'expired', resolved_at = ${clockNow()} WHERE time_block_id = ${blockId} AND status = 'pending'`;
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
  await tx`UPDATE time_block SET t_end = ${end}, paused_at = NULL, updated_at = ${clockNow()} WHERE id = ${open.id}`;
  const intentId = open.intent_id ?? (await systemIntentId(tx, "Uncategorized"));
  if (!open.committed) await pendingProposal(tx, open.id, intentId, 1, who, "manual");
  return { ...open, t_end: end, paused_at: null };
}

async function resync(tx: Tx, now: Date, ...around: (Date | null | undefined)[]): Promise<void> {
  const { from, to } = fillWindow(now, ...around);
  await syncFill(tx, from, to);
}

// ---- the timer ----

export interface StartInput {
  intentId?: string | null;
  title?: string | null;
}

export async function startManualBlock(db: Sql, input: StartInput, who: Who, now = clockNow()): Promise<{ id: string }> {
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

export async function stopRunningBlock(db: Sql, who: Who, now = clockNow()): Promise<{ id: string | null; undo: BlockSnapshot | null }> {
  let closed: BlockRow | null = null;
  let undo: BlockSnapshot | null = null;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const open = await openBlock(tx);
    const before = open ? await snapshotOf(tx, open.id) : null;
    closed = await closeOpen(tx, now, who);
    await resync(tx, now, open?.t_start);
    if (before) undo = { ...before, after: await rowJson(tx, before.block.id) };
  });
  return { id: (closed as BlockRow | null)?.id ?? null, undo };
}

export async function pauseRunningBlock(db: Sql, now = clockNow()): Promise<{ id: string | null }> {
  const rows = await db<{ id: string }[]>`
    UPDATE time_block SET paused_at = ${now}, updated_at = ${clockNow()} WHERE t_end IS NULL AND paused_at IS NULL RETURNING id`;
  return { id: rows[0]?.id ?? null };
}

/**
 * Resume: the paused block ends where it was paused and a new block starts
 * now with the same label, so a block's interval never contains a pause and
 * hours can always be computed from intervals.
 */
export async function resumeRunningBlock(db: Sql, who: Who, now = clockNow()): Promise<{ id: string | null }> {
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
    await tx`UPDATE time_block SET t_end = ${end}, paused_at = NULL, updated_at = ${clockNow()} WHERE id = ${block.id}`;
  }
  if (proposalId) {
    await tx`UPDATE proposal SET status = 'accepted', resolved_at = ${clockNow()} WHERE id = ${proposalId}`;
  }
  // Any other suggestion for this block is now moot.
  await tx`UPDATE proposal SET status = 'rejected', resolved_at = ${clockNow()}
    WHERE time_block_id = ${block.id} AND status = 'pending' AND id IS DISTINCT FROM ${proposalId}`;
  await tx`DELETE FROM commitment WHERE kind = 'time_spent' AND time_block_id = ${block.id}`;
  const cid = uuidv7();
  await tx`INSERT INTO commitment (id, kind, intent_id, t, t_end, value, proposal_id, time_block_id, via, source_ref)
    VALUES (${cid}, 'time_spent', ${intentId}, ${block.t_start}, ${end}, ${hoursBetween(block.t_start, end)},
      ${proposalId}, ${block.id}, ${who.via}, ${who.sourceRef ?? null})`;
  await tx`UPDATE time_block SET intent_id = ${intentId}, committed = true, commitment_id = ${cid}, updated_at = ${clockNow()}
    WHERE id = ${block.id}`;
}

export interface RecategorizeOptions {
  /** For a calendar block: label every future event with this exact title the same way. */
  rememberTitle?: boolean;
  /** For a phone block: label this block's main app the same way from now on. For a place block: time at that place. */
  remember?: boolean;
}

export async function recategorizeBlock(db: Sql, blockId: string, intentId: string, who: Who, opts: RecategorizeOptions = {}, now = clockNow()): Promise<void> {
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
    if (opts.remember && block.origin === "auto_activity") {
      const main = await tx<{ app: string }[]>`
        SELECT o.app FROM time_block_observation l JOIN observation o ON o.id = l.observation_id
        WHERE l.block_id = ${block.id} AND o.app IS NOT NULL
        GROUP BY o.app ORDER BY sum(extract(epoch FROM o.t_end - o.t_start)) DESC LIMIT 1`;
      if (main[0]) {
        await tx`DELETE FROM attribution_rule WHERE match_app = ${main[0].app} AND match_title IS NULL AND match_place IS NULL`;
        await tx`INSERT INTO attribution_rule (id, match_app, intent_id) VALUES (${uuidv7()}, ${main[0].app}, ${intentId})`;
      }
    }
    if (opts.remember && block.origin === "place") {
      const place = await tx<{ place_id: string }[]>`
        SELECT payload->>'place_id' AS place_id FROM proposal WHERE time_block_id = ${block.id} AND payload ? 'place_id' LIMIT 1`;
      const placeId = place[0]?.place_id ?? (await placeForBlock(tx, block.id));
      if (placeId) {
        await tx`DELETE FROM attribution_rule WHERE match_place = ${placeId}`;
        await tx`INSERT INTO attribution_rule (id, match_place, intent_id) VALUES (${uuidv7()}, ${placeId}, ${intentId})`;
      }
    }
    await resync(tx, now, block.t_start, block.t_end);
  });
  scheduleRecompute();
}

/** The place a stay block is at: the nearest known place to its first fix. */
async function placeForBlock(tx: Tx, blockId: string): Promise<string | null> {
  const fix = await tx<{ lat: number; lon: number; accuracy_m: number | null; t_start: Date; id: string }[]>`
    SELECT o.id, o.t_start, (o.payload->>'lat')::float8 AS lat, (o.payload->>'lon')::float8 AS lon, (o.payload->>'accuracy_m')::float8 AS accuracy_m
    FROM time_block_observation l JOIN observation o ON o.id = l.observation_id WHERE l.block_id = ${blockId} ORDER BY o.t_start LIMIT 1`;
  if (!fix[0]) return null;
  const f = fix[0];
  return placeOf({ id: f.id, t: f.t_start.getTime(), lat: f.lat, lon: f.lon, accuracyM: f.accuracy_m ?? 100 }, await loadPlaces(tx))?.id ?? null;
}

/** Remove a block's commitment: it goes back to being a suggestion. */
export async function uncommitBlock(db: Sql, blockId: string, now = clockNow()): Promise<void> {
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (!block.committed) return;
    await tx`DELETE FROM commitment WHERE kind = 'time_spent' AND time_block_id = ${blockId}`;
    await tx`UPDATE time_block SET committed = false, commitment_id = NULL, updated_at = ${clockNow()} WHERE id = ${blockId}`;
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
    await tx`UPDATE time_block SET intent_id = ${intentId}, updated_at = ${clockNow()} WHERE id = ${blockId}`;
  });
  return { id };
}

// ---- fixing a block ----

async function refuseOverlap(tx: Tx, blockId: string, start: Date, end: Date): Promise<void> {
  const clash = await tx<{ id: string; title: string | null }[]>`
    SELECT id, title FROM time_block
    WHERE id <> ${blockId} AND origin IN ('manual', 'merged') AND t_start < ${end} AND coalesce(t_end, 'infinity') > ${start} LIMIT 1`;
  if (clash[0]) throw new Refused("overlaps", "that would overlap another block", { block_id: clash[0].id });
}

export async function editBlock(db: Sql, blockId: string, patch: { tStart?: Date; tEnd?: Date; title?: string | null }, now = clockNow()): Promise<void> {
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (isFill(block.origin)) throw new Refused("fill-block", `this block follows ${followsWhat(block.origin)}; relabel it, or start a timer over it`);
    const start = patch.tStart ?? block.t_start;
    const end = patch.tEnd ?? block.t_end;
    if (patch.tEnd && !block.t_end) throw new BadInput("stop the timer before changing its end");
    if (start.getTime() > now.getTime()) throw new BadInput("a block cannot start in the future");
    if (end && !(end.getTime() > start.getTime())) throw new BadInput("a block must end after it starts");
    if (end && end.getTime() > now.getTime()) throw new BadInput("a block cannot end in the future");
    await refuseOverlap(tx, blockId, start, end ?? new Date(8.64e15));
    await tx`UPDATE time_block SET t_start = ${start}, t_end = ${end},
        title = ${patch.title !== undefined ? patch.title : block.title}, updated_at = ${clockNow()} WHERE id = ${blockId}`;
    if (block.commitment_id && end) {
      await tx`UPDATE commitment SET t = ${start}, t_end = ${end}, value = ${hoursBetween(start, end)} WHERE id = ${block.commitment_id}`;
    }
    await resync(tx, now, block.t_start, block.t_end, start, end);
  });
  scheduleRecompute();
}

export async function deleteBlock(db: Sql, blockId: string, now = clockNow()): Promise<BlockSnapshot> {
  let undo: BlockSnapshot | null = null;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const block = await getBlock(tx, blockId);
    if (isFill(block.origin)) throw new Refused("fill-block", `this block follows ${followsWhat(block.origin)}; reject its label instead`);
    undo = { ...(await snapshotOf(tx, blockId)), after: null };
    await tx`DELETE FROM commitment WHERE time_block_id = ${blockId}`;
    await tx`DELETE FROM time_block WHERE id = ${blockId}`;
    await resync(tx, now, block.t_start, block.t_end);
  });
  scheduleRecompute();
  return undo!;
}

// ---- undo ----

/** A row as `to_jsonb` gives it: every column, timestamps as strings, so it goes back in exactly. */
type RowJson = JsonObject;

/**
 * What a stop or a delete changed, as it was just before: the block, its
 * proposals, commitments and evidence links. `after` is the block as the
 * change left it, or null when the change removed it; undo refuses if the
 * block is not still in that state. JSON-safe, so the page holds it while it
 * offers Undo and sends it back.
 */
export interface BlockSnapshot {
  block: RowJson & { id: string };
  proposals: RowJson[];
  commitments: RowJson[];
  links: RowJson[];
  after: RowJson | null;
}

async function rowJson(tx: Tx, blockId: string): Promise<RowJson | null> {
  const rows = await tx<{ row: RowJson }[]>`SELECT to_jsonb(b) AS row FROM time_block b WHERE id = ${blockId}`;
  return rows[0]?.row ?? null;
}

async function snapshotOf(tx: Tx, blockId: string): Promise<Omit<BlockSnapshot, "after">> {
  const block = (await rowJson(tx, blockId)) as BlockSnapshot["block"] | null;
  if (!block) throw new NotFound(`no block ${blockId}`);
  const rows = async (q: Promise<{ row: RowJson }[]>) => (await q).map((r) => r.row);
  return {
    block,
    proposals: await rows(tx`SELECT to_jsonb(p) AS row FROM proposal p WHERE time_block_id = ${blockId} ORDER BY id`),
    commitments: await rows(tx`SELECT to_jsonb(c) AS row FROM commitment c WHERE time_block_id = ${blockId} ORDER BY id`),
    links: await rows(tx`SELECT to_jsonb(l) AS row FROM time_block_observation l WHERE block_id = ${blockId}`),
  };
}

/**
 * Put back what a stop or a delete changed. Refused, changing nothing, when a
 * later write is in the way: the block changed since, another timer started,
 * or the restored span (an open block's runs to now) would overlap timer,
 * manual or confirmed time. Uncommitted fill under it yields, as it does to
 * any block the owner times.
 */
export async function undoBlockChange(db: Sql, snap: BlockSnapshot, now = clockNow()): Promise<void> {
  const json = (v: unknown) => db.json(v as never);
  const id = snap.block.id;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const current = await tx<{ same: boolean }[]>`
      SELECT b IS NOT DISTINCT FROM jsonb_populate_record(NULL::time_block, ${json(snap.after ?? {})}::jsonb) AS same
      FROM time_block b WHERE id = ${id} FOR UPDATE`;
    if (snap.after === null && current[0]) throw new Refused("undone", "Nothing to undo: that block is already back.");
    if (snap.after !== null && !current[0]?.same) throw new Refused("changed", "Can't undo: that block was changed since.");

    const [r] = await tx<{ t_start: Date; t_end: Date | null }[]>`
      SELECT t_start, t_end FROM jsonb_populate_record(NULL::time_block, ${json(snap.block)}::jsonb)`;
    if (!r) throw new BadInput("not a block");
    if (r.t_end === null) {
      const other = await tx`SELECT 1 FROM time_block WHERE t_end IS NULL AND id <> ${id}`;
      if (other.length > 0) throw new Refused("timer-running", "Can't undo: another timer has started since. Stop it first.");
    }
    const clash = await tx<{ id: string; title: string | null; intent: string | null }[]>`
      SELECT b.id, b.title, i.title AS intent FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id
      WHERE b.id <> ${id} AND (b.origin IN ('manual', 'merged') OR b.committed)
        AND b.t_start < ${r.t_end ?? now} AND coalesce(b.t_end, 'infinity') > ${r.t_start}
      ORDER BY b.t_start LIMIT 1`;
    if (clash[0]) {
      const name = clash[0].intent ?? clash[0].title ?? "another block";
      throw new Refused("overlaps", `Can't undo: ${name} now covers part of that time.`, { block_id: clash[0].id });
    }
    const intents = [snap.block.intent_id, ...snap.proposals.map((p) => p.intent_id), ...snap.commitments.map((c) => c.intent_id)].filter(
      (x): x is string => typeof x === "string",
    );
    const known = await tx<{ id: string }[]>`SELECT id FROM intent WHERE id = ANY(${intents}::uuid[])`;
    if (new Set(known.map((k) => k.id)).size < new Set(intents).size) throw new Refused("intent-gone", "Can't undo: a goal it was labelled with has been deleted.");

    // The block goes back with its own id, so everything that pointed at it points at it again.
    // The cycle block -> commitment -> block means the block goes in first and gets its commitment last.
    if (current[0]) await tx`DELETE FROM time_block WHERE id = ${id}`; // and the proposal the stop made
    await tx`INSERT INTO time_block SELECT * FROM jsonb_populate_record(NULL::time_block, ${json({ ...snap.block, commitment_id: null })}::jsonb)`;
    if (snap.proposals.length) await tx`INSERT INTO proposal SELECT * FROM jsonb_populate_recordset(NULL::proposal, ${json(snap.proposals)}::jsonb)`;
    if (snap.commitments.length) await tx`INSERT INTO commitment SELECT * FROM jsonb_populate_recordset(NULL::commitment, ${json(snap.commitments)}::jsonb)`;
    await tx`UPDATE time_block SET commitment_id = ${(snap.block.commitment_id as string | null) ?? null} WHERE id = ${id}`;
    if (snap.links.length) {
      await tx`INSERT INTO time_block_observation
        SELECT l.* FROM jsonb_populate_recordset(NULL::time_block_observation, ${json(snap.links)}::jsonb) l
        WHERE EXISTS (SELECT 1 FROM observation o WHERE o.id = l.observation_id)`;
    }
    const afterEnd = snap.after?.t_end;
    await resync(tx, now, r.t_start, r.t_end, typeof afterEnd === "string" ? new Date(afterEnd) : null);
  });
  scheduleRecompute();
}

/** Split a closed block at `at`: the left part keeps the id, the right part is new. */
export async function splitBlock(db: Sql, blockId: string, at: Date, now = clockNow()): Promise<{ rightId: string }> {
  const rightId = uuidv7();
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const b = await getBlock(tx, blockId);
    if (!b.t_end) throw new BadInput("stop the timer before splitting it");
    if (!(at.getTime() > b.t_start.getTime() && at.getTime() < b.t_end.getTime())) {
      throw new BadInput("the split point must be inside the block");
    }
    await tx`UPDATE time_block SET t_end = ${at}, updated_at = ${clockNow()} WHERE id = ${blockId}`;
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
    // A split fill block is two fragments the evidence did not ask for; fill sync would merge them back.
    if (!isFill(b.origin)) await resync(tx, now, b.t_start, b.t_end);
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

export async function confirmDay(db: Sql, input: ConfirmInput, who: Who, now = clockNow()): Promise<{ accepted: number; rejected: number }> {
  const { start, end } = localDayBounds(env.LOR_TZ, input.date);
  let accepted = 0;
  let rejected = 0;
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    const reject = input.reject ?? [];
    if (reject.length > 0) {
      const r = await tx`UPDATE proposal SET status = 'rejected', resolved_at = ${clockNow()}
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
export async function applyAutoRules(tx: Tx, from: Date, to: Date, now = clockNow()): Promise<number> {
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
