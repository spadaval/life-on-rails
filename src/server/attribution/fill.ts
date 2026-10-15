// Fill: the actual lane is pre-filled from evidence as uncommitted blocks,
// in order of precedence: the owner's own blocks (timer, manual, anything
// confirmed), then calendar events, then phone app use, then stays at known
// places. Each kind has one function that decides which of its blocks should
// exist and changes only what differs, so a run that finds nothing changed
// writes nothing and block ids (and the proposals on them) stay put.
import type { Tx } from "../../db/client.js";
import { systemIntentId } from "../../db/seed.js";
import { uuidv7 } from "../../shared/ids.js";
import { type AppSession, type Fix, type Place, DEFAULT_RADIUS_M, wantedActivity, wantedStays } from "./activity.js";
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
  const taken = await takenSpans(tx, "calendar", lo, hi);
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
    const rule = matchRule(rules, { app: null, title: w.title, url: null, device: null, place: null });
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

/**
 * Time already spoken for, from the point of view of one kind of fill: the
 * owner's own blocks and everything confirmed, plus the uncommitted fill of
 * every kind that outranks it. An open block is taken from its start with no end.
 */
async function takenSpans(tx: Tx, kind: "calendar" | "auto_activity" | "place", lo: Date, hi: Date): Promise<Span[]> {
  const outranks = { calendar: [], auto_activity: ["calendar"], place: ["calendar", "auto_activity"] }[kind];
  const rows = await tx<{ t_start: Date; t_end: Date | null }[]>`
    SELECT t_start, t_end FROM time_block
    WHERE (origin IN ('manual', 'merged') OR committed OR origin::text = ANY(${outranks}::text[]))
      AND t_start < ${hi} AND coalesce(t_end, 'infinity') > ${lo}`;
  return rows.map((b) => ({ t: b.t_start.getTime(), tEnd: b.t_end?.getTime() ?? FAR_FUTURE }));
}

interface Derived {
  intentId: string | null;
  ruleId: string | null;
  tStart: number;
  tEnd: number;
  title: string;
  observationIds: string[];
  payload: Record<string, string | null>;
  confidence: number;
}

/**
 * Make the uncommitted blocks of one evidence kind starting in [from, to)
 * match `wanted`. A block keeps its id while its span is unchanged; its title
 * and evidence are refreshed, and a rule's label is applied only if nobody
 * (the agent, the owner) has suggested something else for it.
 */
async function syncDerived(tx: Tx, origin: "auto_activity" | "place", from: Date, to: Date, wanted: Derived[]): Promise<FillStats> {
  const lo = from.getTime();
  const hi = to.getTime();
  const want = new Map(wanted.filter((w) => w.tStart >= lo && w.tStart < hi).map((w) => [`${w.tStart}\n${w.tEnd}`, w]));
  const existing = await tx<{ id: string; t_start: Date; t_end: Date; intent_id: string | null; title: string | null }[]>`
    SELECT id, t_start, t_end, intent_id, title FROM time_block
    WHERE origin = ${origin} AND NOT committed AND t_start >= ${from} AND t_start < ${to}`;
  const stats: FillStats = { kept: 0, inserted: 0, deleted: 0 };
  const stale: string[] = [];
  for (const b of existing) {
    const k = `${b.t_start.getTime()}\n${b.t_end.getTime()}`;
    const w = want.get(k);
    if (!w) {
      stale.push(b.id);
      continue;
    }
    want.delete(k);
    stats.kept++;
    if (b.title !== w.title) {
      await tx`UPDATE time_block SET title = ${w.title}, updated_at = now() WHERE id = ${b.id}`;
      await linkEvidence(tx, b.id, w.observationIds);
    }
    if (w.intentId && b.intent_id !== w.intentId) {
      const other = await tx`SELECT 1 FROM proposal WHERE time_block_id = ${b.id} AND status = 'pending' AND via IN ('agent', 'ui')`;
      if (other.length === 0) {
        await tx`UPDATE proposal SET status = 'expired', resolved_at = now() WHERE time_block_id = ${b.id} AND status = 'pending'`;
        await tx`UPDATE time_block SET intent_id = ${w.intentId}, updated_at = now() WHERE id = ${b.id}`;
        await insertProposal(tx, b.id, w);
      }
    }
  }
  if (stale.length > 0) {
    await tx`DELETE FROM time_block WHERE id = ANY(${stale}::uuid[])`;
    stats.deleted = stale.length;
  }
  for (const w of want.values()) {
    const id = uuidv7();
    await tx`INSERT INTO time_block (id, t_start, t_end, origin, intent_id, title)
      VALUES (${id}, ${new Date(w.tStart)}, ${new Date(w.tEnd)}, ${origin}, ${w.intentId}, ${w.title})`;
    await linkEvidence(tx, id, w.observationIds);
    if (w.intentId) await insertProposal(tx, id, w);
    stats.inserted++;
  }
  return stats;
}

async function linkEvidence(tx: Tx, blockId: string, observationIds: string[]): Promise<void> {
  await tx`DELETE FROM time_block_observation WHERE block_id = ${blockId}`;
  if (observationIds.length === 0) return;
  await tx`INSERT INTO time_block_observation (block_id, observation_id)
    SELECT ${blockId}, unnest(${observationIds}::uuid[]) ON CONFLICT DO NOTHING`;
}

async function insertProposal(tx: Tx, blockId: string, w: Derived): Promise<void> {
  await tx`INSERT INTO proposal (id, confidence, time_block_id, intent_id, payload, via)
    VALUES (${uuidv7()}, ${w.confidence}, ${blockId}, ${w.intentId}, ${tx.json({ ...w.payload, rule_id: w.ruleId })}, 'rule')`;
}

/** How far back to read evidence, so a block that started before the window is rebuilt the same way. */
const LOOKBACK_MS = 6 * 3_600_000;

/** Phone app sessions in [from, to) as blocks; see `wantedActivity`. */
export async function syncActivityFill(tx: Tx, from: Date, to: Date): Promise<FillStats> {
  const lo = new Date(from.getTime() - LOOKBACK_MS);
  const rows = await tx<{ id: string; t_start: Date; t_end: Date; app: string | null; title: string | null; device: string }[]>`
    SELECT id, t_start, t_end, app, title, device FROM observation
    WHERE kind = 'app' AND t_end IS NOT NULL AND t_start < ${to} AND t_end > ${lo}`;
  const sessions: AppSession[] = rows.map((r) => ({
    id: r.id, t: r.t_start.getTime(), tEnd: r.t_end.getTime(), app: r.app ?? "", name: r.title ?? r.app ?? "app", device: r.device,
  }));
  const taken = await takenSpans(tx, "auto_activity", lo, to);
  const wanted = wantedActivity(sessions, await loadRules(tx), taken).map((w) => ({
    ...w, payload: { source: "phone" }, confidence: 0.6,
  }));
  return syncDerived(tx, "auto_activity", from, to, wanted);
}

export async function loadPlaces(tx: Tx): Promise<Place[]> {
  const rows = await tx<{ id: string; name: string; lat: number; lon: number; radius_m: number | null }[]>`
    SELECT id, name, lat, lon, radius_m FROM entity WHERE kind = 'place' AND lat IS NOT NULL AND lon IS NOT NULL`;
  return rows.map((r) => ({ id: r.id, name: r.name, lat: r.lat, lon: r.lon, radiusM: r.radius_m ?? DEFAULT_RADIUS_M }));
}

export async function loadFixes(tx: Tx, from: Date, to: Date): Promise<Fix[]> {
  const rows = await tx<{ id: string; t_start: Date; lat: number; lon: number; accuracy_m: number | null }[]>`
    SELECT id, t_start, (payload->>'lat')::float8 AS lat, (payload->>'lon')::float8 AS lon, (payload->>'accuracy_m')::float8 AS accuracy_m
    FROM observation WHERE kind = 'location' AND t_start >= ${from} AND t_start < ${to}`;
  return rows.map((r) => ({ id: r.id, t: r.t_start.getTime(), lat: r.lat, lon: r.lon, accuracyM: r.accuracy_m ?? 100 }));
}

/** Stays at known places in [from, to), in whatever time nothing else claims: a rough, low-confidence guess. */
export async function syncPlaceFill(tx: Tx, from: Date, to: Date): Promise<FillStats> {
  const places = await loadPlaces(tx);
  const lo = new Date(from.getTime() - LOOKBACK_MS);
  const stays = places.length > 0 ? wantedStays(await loadFixes(tx, lo, to), places) : [];
  const byId = new Map(places.map((p) => [p.id, p]));
  const rules = await loadRules(tx);
  const used = normalize(await takenSpans(tx, "place", lo, to));
  const wanted: Derived[] = [];
  for (const s of stays) {
    const place = byId.get(s.placeId)!;
    const rule = matchRule(rules, { app: null, title: null, url: null, device: null, place: place.id });
    for (const f of subtractSpans([{ t: s.tStart, tEnd: s.tEnd }], used)) {
      if (f.tEnd - f.t < MIN_FRAGMENT_MS * 5) continue;
      wanted.push({
        intentId: rule?.intentId ?? null, ruleId: rule?.id ?? null, tStart: f.t, tEnd: f.tEnd, title: `At ${place.name}`,
        observationIds: s.observationIds, payload: { source: "place", place_id: place.id }, confidence: 0.4,
      });
    }
  }
  return syncDerived(tx, "place", from, to, wanted);
}

/** All fill, in order of precedence. Call after `lockBlocks`, wherever calendar fill used to run. */
export async function syncFill(tx: Tx, from: Date, to: Date): Promise<{ calendar: FillStats; activity: FillStats; place: FillStats }> {
  const calendar = await syncCalendarFill(tx, from, to);
  const activity = await syncActivityFill(tx, from, to);
  const place = await syncPlaceFill(tx, from, to);
  return { calendar, activity, place };
}

export async function loadRules(tx: Tx): Promise<Rule[]> {
  const rows = await tx<{ id: string; match_device: string | null; match_app: string | null; match_title: string | null; match_url: string | null; match_place: string | null; intent_id: string; auto_apply: boolean; priority: number }[]>`
    SELECT id, match_device, match_app, match_title, match_url, match_place, intent_id, auto_apply, priority
    FROM attribution_rule ORDER BY priority DESC, created_at`;
  return rows.map((r) => ({
    id: r.id, matchDevice: r.match_device, matchApp: r.match_app, matchTitle: r.match_title, matchUrl: r.match_url, matchPlace: r.match_place,
    intentId: r.intent_id, autoApply: r.auto_apply, priority: r.priority,
  }));
}
