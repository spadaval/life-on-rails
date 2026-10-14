// The view model behind Today (and `lor day`): one local day's planned lane,
// actual lane, what is running, what is left of the day, and what needs
// confirming.
import type { Tx } from "../db/client.js";
import { type Ymd, addDays, localDayBounds, localTimeOnDay, localYmd } from "../shared/localTime.js";
import { type PhoneStats, phoneStats } from "./attribution/activity.js";
import { FILLS_ACTUAL_SQL } from "./attribution/fill.js";
import { type Span, attributionCoverage, calendarConfirmRate, normalize, subtractSpans } from "./attribution/intervals.js";
import { env } from "./env.js";

export interface PlannedEvent {
  calendar_id: string;
  event_id: string;
  title: string | null;
  t_start: string;
  t_end: string;
  all_day: boolean;
  /** False for free, focus-time, and similar entries: shown, but they do not fill the actual lane. */
  fills: boolean;
  html_link: string | null;
}

export interface BlockView {
  id: string;
  t_start: string;
  /** Null while running or paused. */
  t_end: string | null;
  paused_at: string | null;
  origin: string;
  title: string | null;
  intent_id: string | null;
  intent: string | null;
  committed: boolean;
  /** Who confirmed it (ui, agent, rule), when committed. */
  committed_via: string | null;
  commitment_id: string | null;
  /** The pending suggestion, when not committed. */
  proposal: { id: string; via: string; reason: string | null; source_ref: string | null } | null;
  /** The label was offered and turned down; the block is still unlabelled. */
  rejected: boolean;
  /** A confirmed calendar block whose event has since moved or been cancelled. */
  mismatch: string | null;
}

export interface Gap {
  t_start: string;
  t_end: string;
  minutes: number;
  fits: string | null;
}

export interface TodayView {
  date: Ymd;
  is_today: boolean;
  prev: Ymd;
  next: Ymd;
  now: string;
  timezone: string;
  wake_start: string;
  wake_end: string;
  /** Seconds left in the waking day, and the share of it that is left. */
  remaining_s: number;
  remaining_fraction: number;
  running: BlockView | null;
  planned: PlannedEvent[];
  actual: BlockView[];
  gaps: Gap[];
  /** Blocks starting on this day (a block belongs to the day it starts) that have ended and still have a pending label. */
  to_confirm: number;
  /** When this local day starts and ends. */
  day_start: string;
  day_end: string;
  confirm_rate: number | null;
  coverage: number | null;
  /** Screen time, pickups, and glances on the phone this day; null with no phone data. */
  phone: PhoneStats | null;
}

interface BlockRow {
  id: string;
  t_start: Date;
  t_end: Date | null;
  paused_at: Date | null;
  origin: string;
  title: string | null;
  intent_id: string | null;
  intent: string | null;
  committed: boolean;
  commitment_id: string | null;
  committed_via: string | null;
  calendar_event_id: string | null;
  proposal_id: string | null;
  proposal_via: string | null;
  proposal_reason: string | null;
  proposal_source_ref: string | null;
  rejected: boolean;
  event_start: Date | null;
  event_end: Date | null;
  event_status: string | null;
}

async function loadBlocks(db: Tx, start: Date, end: Date): Promise<BlockRow[]> {
  return db<BlockRow[]>`
    SELECT b.id, b.t_start, b.t_end, b.paused_at, b.origin, b.title, b.intent_id, i.title AS intent, b.committed,
           b.commitment_id, c.via AS committed_via, b.calendar_event_id,
           p.id AS proposal_id, p.via AS proposal_via, p.reason AS proposal_reason, p.source_ref AS proposal_source_ref,
           (NOT b.committed AND p.id IS NULL AND EXISTS (
              SELECT 1 FROM proposal r WHERE r.time_block_id = b.id AND r.status = 'rejected')) AS rejected,
           e.t_start AS event_start, e.t_end AS event_end, e.status AS event_status
    FROM time_block b
    LEFT JOIN intent i ON i.id = b.intent_id
    LEFT JOIN commitment c ON c.id = b.commitment_id
    LEFT JOIN LATERAL (
      SELECT id, via, reason, source_ref FROM proposal
      WHERE time_block_id = b.id AND status = 'pending' ORDER BY created_at DESC LIMIT 1) p ON true
    LEFT JOIN google_event e ON e.calendar_id = b.calendar_id AND e.event_id = b.calendar_event_id
    WHERE b.t_start < ${end} AND coalesce(b.t_end, 'infinity') > ${start}
    ORDER BY b.t_start`;
}

function toBlockView(b: BlockRow): BlockView {
  let mismatch: string | null = null;
  if (b.origin === "calendar" && b.committed && b.t_end) {
    if (!b.event_start || b.event_status === "cancelled") mismatch = "the event was cancelled";
    else if (b.t_start.getTime() < b.event_start.getTime() || b.t_end.getTime() > (b.event_end?.getTime() ?? 0)) mismatch = "the event moved";
  }
  return {
    id: b.id,
    t_start: b.t_start.toISOString(),
    t_end: b.t_end?.toISOString() ?? null,
    paused_at: b.paused_at?.toISOString() ?? null,
    origin: b.origin,
    title: b.title,
    intent_id: b.intent_id,
    intent: b.intent,
    committed: b.committed,
    committed_via: b.committed_via,
    commitment_id: b.commitment_id,
    proposal: b.proposal_id ? { id: b.proposal_id, via: b.proposal_via ?? "job", reason: b.proposal_reason, source_ref: b.proposal_source_ref } : null,
    rejected: b.rejected,
    mismatch,
  };
}

const clip = (s: Span, lo: number, hi: number): Span => ({ t: Math.max(s.t, lo), tEnd: Math.min(s.tEnd, hi) });

/** The two success measures for a stretch of waking time that has already happened. */
async function measures(db: Tx, from: Date, to: Date): Promise<{ confirmRate: number | null; coverage: number | null; events: Span[] }> {
  const lo = from.getTime();
  const hi = to.getTime();
  if (!(hi > lo)) return { confirmRate: null, coverage: null, events: [] };
  const events = await db<{ t_start: Date; t_end: Date }[]>`
    SELECT e.t_start, e.t_end FROM google_event e
    WHERE e.t_start < ${to} AND e.t_end > ${from} AND ${db.unsafe(FILLS_ACTUAL_SQL)}`;
  const blocks = await db<{ t_start: Date; t_end: Date; origin: string }[]>`
    SELECT t_start, t_end, origin FROM time_block
    WHERE committed AND t_end IS NOT NULL AND t_start < ${to} AND t_end > ${from}`;
  const E = events.map((e) => clip({ t: e.t_start.getTime(), tEnd: e.t_end.getTime() }, lo, hi));
  const C = blocks.map((b) => clip({ t: b.t_start.getTime(), tEnd: b.t_end.getTime() }, lo, hi));
  const M = blocks.filter((b) => b.origin !== "calendar").map((b) => clip({ t: b.t_start.getTime(), tEnd: b.t_end.getTime() }, lo, hi));
  const rate = calendarConfirmRate({ committed: C, events: E, manual: M }).rate;
  const hasActivity = await db`SELECT 1 FROM time_block WHERE origin = 'auto_activity' LIMIT 1`;
  return {
    confirmRate: rate ?? null,
    // Coverage of all waking time only means something once a sensor fills it.
    coverage: hasActivity.length > 0 ? (attributionCoverage(C, lo, hi) ?? null) : null,
    events: E,
  };
}

export function wakeWindow(date: Ymd): { start: Date; end: Date } {
  const start = localTimeOnDay(env.LOR_TZ, date, env.LOR_WAKE_START);
  let end = localTimeOnDay(env.LOR_TZ, date, env.LOR_WAKE_END);
  if (end.getTime() <= start.getTime()) end = localTimeOnDay(env.LOR_TZ, addDays(date, 1), env.LOR_WAKE_END);
  return { start, end };
}

/** Calendar-confirm rate over the seven full local days before today, for the score confidence chip. */
export async function trailingConfirmRate(db: Tx, now: Date): Promise<number | null> {
  const today = localYmd(env.LOR_TZ, now);
  let confirmed = 0;
  let total = 0;
  for (let d = 1; d <= 7; d++) {
    const wake = wakeWindow(addDays(today, -d));
    const lo = wake.start.getTime();
    const hi = wake.end.getTime();
    const events = await db<{ t_start: Date; t_end: Date }[]>`
      SELECT e.t_start, e.t_end FROM google_event e
      WHERE e.t_start < ${wake.end} AND e.t_end > ${wake.start} AND ${db.unsafe(FILLS_ACTUAL_SQL)}`;
    const blocks = await db<{ t_start: Date; t_end: Date; origin: string }[]>`
      SELECT t_start, t_end, origin FROM time_block
      WHERE committed AND t_end IS NOT NULL AND t_start < ${wake.end} AND t_end > ${wake.start}`;
    const E = normalize(events.map((e) => clip({ t: e.t_start.getTime(), tEnd: e.t_end.getTime() }, lo, hi)));
    const C = blocks.map((b) => clip({ t: b.t_start.getTime(), tEnd: b.t_end.getTime() }, lo, hi));
    const M = blocks.filter((b) => b.origin !== "calendar").map((b) => clip({ t: b.t_start.getTime(), tEnd: b.t_end.getTime() }, lo, hi));
    const seconds = (spans: Span[]) => normalize(spans).reduce((a, s) => a + (s.tEnd - s.t) / 1000, 0);
    const manualOutside = seconds(subtractSpans(M, E));
    const denom = seconds(E) + manualOutside;
    if (denom <= 0) continue;
    const r = calendarConfirmRate({ committed: C, events: E, manual: M }).rate ?? 0;
    confirmed += r * denom;
    total += denom;
  }
  return total > 0 ? confirmed / total : null;
}

const MIN_GAP_MIN = 15;

export async function getToday(db: Tx, date: Ymd, now = new Date()): Promise<TodayView> {
  const tz = env.LOR_TZ;
  const { start, end } = localDayBounds(tz, date);
  const wake = wakeWindow(date);
  const isToday = localYmd(tz, now) === date;

  const events = await db<{ calendar_id: string; event_id: string; title: string | null; t_start: Date; t_end: Date; all_day: boolean; fills: boolean; html_link: string | null }[]>`
    SELECT e.calendar_id, e.event_id, e.title, e.t_start, e.t_end, e.all_day, e.html_link,
           (${db.unsafe(FILLS_ACTUAL_SQL)}) AS fills
    FROM google_event e
    WHERE e.t_start < ${end} AND e.t_end > ${start} AND e.status <> 'cancelled'
      AND coalesce(e.self_response, '') <> 'declined'
    ORDER BY e.all_day DESC, e.t_start`;
  const blocks = (await loadBlocks(db, start, end)).map(toBlockView);
  const running = (await db<{ id: string }[]>`SELECT id FROM time_block WHERE t_end IS NULL`)[0];
  const runningView = running ? (blocks.find((b) => b.id === running.id) ?? (await loadBlocks(db, new Date(0), new Date(8.64e15))).map(toBlockView).find((b) => b.id === running.id) ?? null) : null;

  // How the day has gone so far, over the waking time that has passed.
  const upTo = new Date(Math.min(now.getTime(), wake.end.getTime()));
  const m = await measures(db, wake.start, upTo);

  // Free stretches left today: between now and the end of the waking day, outside events.
  const gaps: Gap[] = [];
  if (isToday && now.getTime() < wake.end.getTime()) {
    const from = Math.max(now.getTime(), wake.start.getTime());
    const busy = events.filter((e) => e.fills).map((e) => ({ t: e.t_start.getTime(), tEnd: e.t_end.getTime() }));
    const fits = await gapSuggestion(db);
    for (const g of subtractSpans([{ t: from, tEnd: wake.end.getTime() }], busy)) {
      const minutes = Math.round((g.tEnd - g.t) / 60_000);
      if (minutes >= MIN_GAP_MIN) gaps.push({ t_start: new Date(g.t).toISOString(), t_end: new Date(g.tEnd).toISOString(), minutes, fits });
    }
  }

  const sessions = await db<{ id: string; t_start: Date; t_end: Date; app: string | null; title: string | null; device: string }[]>`
    SELECT id, t_start, t_end, app, title, device FROM observation
    WHERE source = 'android' AND payload->>'kind' = 'app' AND t_end IS NOT NULL AND t_start < ${end} AND t_end > ${start}`;
  const phone = sessions.length > 0
    ? phoneStats(sessions.map((s) => ({ id: s.id, t: s.t_start.getTime(), tEnd: s.t_end.getTime(), app: s.app ?? "", name: s.title ?? s.app ?? "app", device: s.device })), start.getTime(), end.getTime())
    : null;

  const wakeSeconds = (wake.end.getTime() - wake.start.getTime()) / 1000;
  const remaining = isToday ? Math.max(0, (wake.end.getTime() - Math.max(now.getTime(), wake.start.getTime())) / 1000) : 0;
  return {
    date,
    is_today: isToday,
    prev: addDays(date, -1),
    next: addDays(date, 1),
    now: now.toISOString(),
    timezone: tz,
    wake_start: wake.start.toISOString(),
    wake_end: wake.end.toISOString(),
    remaining_s: Math.round(remaining),
    remaining_fraction: wakeSeconds > 0 ? remaining / wakeSeconds : 0,
    running: runningView,
    planned: events.map((e) => ({
      calendar_id: e.calendar_id, event_id: e.event_id, title: e.title, t_start: e.t_start.toISOString(),
      t_end: e.t_end.toISOString(), all_day: e.all_day, fills: e.fills, html_link: e.html_link,
    })),
    actual: blocks,
    gaps,
    to_confirm: blocks.filter(
      (b) => b.proposal && b.t_end && new Date(b.t_end).getTime() <= now.getTime() && new Date(b.t_start).getTime() >= start.getTime(),
    ).length,
    day_start: start.toISOString(),
    day_end: end.toISOString(),
    confirm_rate: m.confirmRate,
    coverage: m.coverage,
    phone,
  };
}

/** What a free stretch could go to: the hungriest high-weight goal, else the next task due. */
async function gapSuggestion(db: Tx): Promise<string | null> {
  const starving = await db<{ title: string }[]>`
    SELECT i.title FROM intent i
    JOIN LATERAL (SELECT parts FROM score_snapshot s WHERE s.intent_id = i.id ORDER BY as_of DESC LIMIT 1) s ON true
    WHERE i.type = 'goal' AND i.status = 'active'
      AND (s.parts->'terms'->'last_7_days'->>'starving')::boolean IS TRUE
    ORDER BY i.weight DESC, (s.parts->'terms'->'last_7_days'->>'adequacy')::float8 ASC NULLS FIRST LIMIT 1`;
  if (starving[0]) return starving[0].title;
  const due = await db<{ title: string }[]>`
    SELECT title FROM intent WHERE type = 'task' AND status = 'active' AND assignee = 'owner' AND due_at IS NOT NULL
    ORDER BY due_at LIMIT 1`;
  return due[0]?.title ?? null;
}

const hm = (iso: string, tz: string) =>
  new Intl.DateTimeFormat("en-GB", { timeZone: tz, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(iso));

/** The day as text, with the ids the agent needs to act on a block or a suggestion. */
export function phoneLine(p: PhoneStats): string {
  const h = Math.floor(p.screen_s / 3600);
  const m = Math.round((p.screen_s % 3600) / 60);
  const top = p.top.map((a) => `${a.name} ${Math.round(a.s / 60)}m`).join(", ");
  return `${h}h ${m}m on screen, picked up ${p.pickups} times, ${p.glances} of them glances under two minutes${top ? ` (most: ${top})` : ""}`;
}

export function renderToday(t: TodayView): string {
  const out: string[] = [`# ${t.date}${t.is_today ? " (today)" : ""}, ${t.timezone}`, ""];
  if (t.is_today) {
    const h = Math.floor(t.remaining_s / 3600);
    const m = Math.round((t.remaining_s % 3600) / 60);
    out.push(`Left in the waking day: ${h}h ${m}m.`, "");
  }
  if (t.running) {
    const state = t.running.paused_at ? "paused" : "running";
    out.push(`Timer ${state} since ${hm(t.running.t_start, t.timezone)}: ${t.running.intent ?? "Uncategorized"}${t.running.title ? ` — ${t.running.title}` : ""} (block ${t.running.id})`, "");
  }
  out.push("## Planned", "");
  if (t.planned.length === 0) out.push("Nothing on the calendar.");
  for (const e of t.planned) {
    const when = e.all_day ? "all day" : `${hm(e.t_start, t.timezone)}–${hm(e.t_end, t.timezone)}`;
    out.push(`- ${when} ${e.title ?? "(no title)"}${e.fills ? "" : " (does not count as time spent)"}`);
  }
  out.push("", "## Actual", "");
  if (t.actual.length === 0) out.push("No blocks.");
  for (const b of t.actual) {
    const when = `${hm(b.t_start, t.timezone)}–${b.t_end ? hm(b.t_end, t.timezone) : "now"}`;
    const what = [b.intent ?? "unlabelled", b.title].filter(Boolean).join(" — ");
    const state = b.committed
      ? `confirmed${b.committed_via && b.committed_via !== "ui" ? ` by ${b.committed_via}` : ""}`
      : b.proposal
        ? `suggested by ${b.proposal.via}, proposal ${b.proposal.id}`
        : b.rejected
          ? "label rejected"
          : "open";
    out.push(`- ${when} [${b.origin}] ${what} (${state}; block ${b.id})${b.mismatch ? ` — ${b.mismatch}` : ""}`);
  }
  if (t.gaps.length > 0) {
    out.push("", "## Free stretches left", "");
    for (const g of t.gaps) out.push(`- ${hm(g.t_start, t.timezone)}–${hm(g.t_end, t.timezone)} (${g.minutes} min)${g.fits ? `, could go to: ${g.fits}` : ""}`);
  }
  if (t.phone) out.push("", `Phone: ${phoneLine(t.phone)}.`);
  out.push("", `To confirm: ${t.to_confirm}. Calendar-confirm rate so far: ${t.confirm_rate === null ? "—" : `${Math.round(t.confirm_rate * 100)}%`}.`);
  return out.join("\n") + "\n";
}
