// Calendar fill: with no activity sensor, the actual lane is pre-filled from
// the calendar as uncommitted blocks with pending proposals. One function
// decides what calendar blocks should exist and changes only what differs, so
// a poll that finds nothing changed writes nothing and block ids stay put.
import type { Tx } from "../../db/client.js";
import { systemIntentId } from "../../db/seed.js";
import { uuidv7 } from "../../shared/ids.js";
import { type Span, normalize, subtractSpans } from "./intervals.js";
import { type Rule, matchRule } from "./suggest.js";

export interface FillEvent {
  calendarId: string;
  eventId: string;
  title: string | null;
  tStart: number;
  tEnd: number;
}

export interface WantedBlock {
  calendarId: string;
  eventId: string;
  title: string | null;
  tStart: number;
  tEnd: number;
}

/** Fragments shorter than this are not worth a block. */
const MIN_FRAGMENT_MS = 60_000;

/**
 * Which calendar blocks should exist. Each event gets its span minus taken
 * time; where two events overlap, the shorter one gets the overlap (ties go to
 * the earlier start), so a one-hour meeting inside an afternoon hold keeps its hour.
 */
export function wantedFill(events: FillEvent[], taken: Span[]): WantedBlock[] {
  const order = [...events].sort(
    (a, b) => a.tEnd - a.tStart - (b.tEnd - b.tStart) || a.tStart - b.tStart || a.eventId.localeCompare(b.eventId),
  );
  let used = normalize(taken);
  const out: WantedBlock[] = [];
  for (const e of order) {
    for (const f of subtractSpans([{ t: e.tStart, tEnd: e.tEnd }], used)) {
      if (f.tEnd - f.t < MIN_FRAGMENT_MS) continue;
      out.push({ calendarId: e.calendarId, eventId: e.eventId, title: e.title, tStart: f.t, tEnd: f.tEnd });
    }
    used = normalize([...used, { t: e.tStart, tEnd: e.tEnd }]);
  }
  return out.sort((a, b) => a.tStart - b.tStart);
}

/** SQL for "this event fills the actual lane"; mirrors calendarSync.fillsActual. */
export const FILLS_ACTUAL_SQL = `
  NOT e.all_day AND e.status <> 'cancelled'
  AND coalesce(e.self_response, '') <> 'declined'
  AND e.transparency <> 'transparent'
  AND e.event_type IN ('default', 'fromGmail')`;

export interface FillStats {
  kept: number;
  inserted: number;
  deleted: number;
  [key: string]: unknown;
}

const FAR_FUTURE = Date.UTC(9999, 0, 1);

/** Serialises every writer of time blocks: one at a time, for the length of its transaction. */
export async function lockBlocks(tx: Tx): Promise<void> {
  await tx`SELECT pg_advisory_xact_lock(hashtext('lor.time_block'))`;
}

/**
 * Bring calendar blocks in [from, to) in line with the calendar. Run inside
 * the transaction that changed a block or a commitment (after `lockBlocks`),
 * and after every calendar poll.
 */
export async function syncCalendarFill(tx: Tx, from: Date, to: Date): Promise<FillStats> {
  const events = await tx<{ calendar_id: string; event_id: string; title: string | null; t_start: Date; t_end: Date }[]>`
    SELECT e.calendar_id, e.event_id, e.title, e.t_start, e.t_end FROM google_event e
    WHERE e.t_start < ${to} AND e.t_end > ${from} AND ${tx.unsafe(FILLS_ACTUAL_SQL)}`;
  // Taken time must cover the whole of every event, not just the window.
  const lo = new Date(Math.min(from.getTime(), ...events.map((e) => e.t_start.getTime())));
  const hi = new Date(Math.max(to.getTime(), ...events.map((e) => e.t_end.getTime())));
  // Every non-calendar block, and every committed calendar block. An open block
  // is taken from its start with no end.
  const takenRows = await tx<{ t_start: Date; t_end: Date | null }[]>`
    SELECT t_start, t_end FROM time_block
    WHERE (origin <> 'calendar' OR committed) AND t_start < ${hi} AND coalesce(t_end, 'infinity') > ${lo}`;
  const taken = takenRows.map((b) => ({ t: b.t_start.getTime(), tEnd: b.t_end?.getTime() ?? FAR_FUTURE }));
  const wanted = wantedFill(
    events.map((e) => ({ calendarId: e.calendar_id, eventId: e.event_id, title: e.title, tStart: e.t_start.getTime(), tEnd: e.t_end.getTime() })),
    taken,
  );

  const existing = await tx<{ id: string; calendar_id: string; calendar_event_id: string; t_start: Date; t_end: Date }[]>`
    SELECT id, calendar_id, calendar_event_id, t_start, t_end FROM time_block
    WHERE origin = 'calendar' AND NOT committed AND t_start < ${hi} AND t_end > ${lo}`;
  const key = (calendarId: string, eventId: string, s: number, e: number) => `${calendarId}\n${eventId}\n${s}\n${e}`;
  const wantedKeys = new Map(wanted.map((w) => [key(w.calendarId, w.eventId, w.tStart, w.tEnd), w]));
  const stats: FillStats = { kept: 0, inserted: 0, deleted: 0 };

  const stale: string[] = [];
  for (const b of existing) {
    const k = key(b.calendar_id, b.calendar_event_id, b.t_start.getTime(), b.t_end.getTime());
    if (wantedKeys.has(k)) {
      wantedKeys.delete(k); // already there: leave it, its id, and its proposals alone
      stats.kept++;
    } else {
      stale.push(b.id);
    }
  }
  if (stale.length > 0) {
    await tx`DELETE FROM time_block WHERE id = ANY(${stale}::uuid[])`; // proposals go with them
    stats.deleted = stale.length;
  }
  if (wantedKeys.size === 0) return stats;

  const meetings = await systemIntentId(tx, "Meetings");
  const rules = await loadRules(tx);
  for (const w of wantedKeys.values()) {
    const rule = matchRule(rules, { app: null, title: w.title, url: null, device: null });
    const intentId = rule?.intentId ?? meetings;
    const id = uuidv7();
    await tx`INSERT INTO time_block (id, t_start, t_end, origin, intent_id, calendar_id, calendar_event_id, title)
      VALUES (${id}, ${new Date(w.tStart)}, ${new Date(w.tEnd)}, 'calendar', ${intentId}, ${w.calendarId}, ${w.eventId}, ${w.title})`;
    await tx`INSERT INTO proposal (id, confidence, time_block_id, intent_id, payload, via)
      VALUES (${uuidv7()}, ${rule ? 0.9 : 0.7}, ${id}, ${intentId},
        ${tx.json({ source: "calendar", calendar_event_id: w.eventId, rule_id: rule?.id ?? null })}, ${rule ? "rule" : "job"})`;
    stats.inserted++;
  }
  return stats;
}

export async function loadRules(tx: Tx): Promise<Rule[]> {
  const rows = await tx<{ id: string; match_device: string | null; match_app: string | null; match_title: string | null; match_url: string | null; intent_id: string; auto_apply: boolean; priority: number }[]>`
    SELECT id, match_device, match_app, match_title, match_url, intent_id, auto_apply, priority
    FROM attribution_rule ORDER BY priority DESC, created_at`;
  return rows.map((r) => ({
    id: r.id, matchDevice: r.match_device, matchApp: r.match_app, matchTitle: r.match_title, matchUrl: r.match_url,
    intentId: r.intent_id, autoApply: r.auto_apply, priority: r.priority,
  }));
}
