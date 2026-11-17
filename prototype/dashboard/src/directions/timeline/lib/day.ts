// The route as data: the day cut into spans from the first thing recorded to bedtime, each
// with a height, so the screen only has to draw them. Time sets the vertical scale, but short
// blocks get a readable minimum and long empty stretches are squeezed, so a sparse day still
// fits a screen and a busy morning is not a smear of 6-minute slivers.
import { blockName, isBare, isOpen } from "../../../data/derive.js";
import type { State } from "../../../data/store.js";
import type { BlockView, PlannedEvent } from "../../../data/types.js";
import type { Plan } from "./ui.js";

export type SpanKind = "block" | "running" | "untracked" | "silent" | "event" | "plan" | "free";

export interface Span {
  kind: SpanKind;
  key: string;
  start: number;
  end: number;
  top: number;
  height: number;
  block?: BlockView;
  event?: PlannedEvent;
  plan?: Plan;
}

/** How a past block reads: counted, the agent's guess waiting on a yes, or nobody's yet. */
export type BlockTone = "tracked" | "proposed" | "bare";
export function toneOf(b: BlockView): BlockTone {
  if (b.committed) return "tracked";
  if (b.proposal && b.intent) return "proposed";
  if (b.t_end === null && b.intent) return "tracked";
  return "bare";
}

export function blockTitle(b: BlockView): string {
  // A calendar event keeps its own name once counted; what it counts toward goes in the meta line.
  if (b.origin === "calendar" && b.title) return b.title;
  if (b.committed || (b.t_end === null && b.intent)) return b.intent ?? "Unlabelled";
  if (b.origin === "calendar") return b.title ?? "Calendar";
  if (isBare(b) || !b.intent) return b.origin === "manual" ? "Unlabelled" : blockName({ ...b, intent: null });
  return b.title ?? blockName({ ...b, intent: null });
}

/** Where the block came from, in a few words. */
export function blockSource(b: BlockView): string {
  switch (b.origin) {
    case "auto_activity":
      return `Phone · ${b.title ?? "apps"}`;
    case "place":
      return b.title ?? "A place";
    case "calendar":
      return "Calendar";
    default:
      return b.t_end === null ? "Timer, running" : "Timer";
  }
}

const PX = 1.15; // px per minute where something happened
const MIN_BLOCK = 44;

function heightOf(kind: SpanKind, minutes: number): number {
  switch (kind) {
    case "block":
    case "event":
    case "plan":
      return Math.max(MIN_BLOCK, Math.min(minutes * PX, 150));
    case "running":
      return Math.max(88, Math.min(minutes * PX, 150));
    case "untracked":
      return minutes < 4 ? 6 : Math.max(22, Math.min(minutes * PX * 0.6, 56));
    case "free":
      return Math.max(40, Math.min(minutes * PX * 0.7, 84));
    case "silent":
      return 72;
  }
}

export interface RouteModel {
  spans: Span[];
  /** y of the now line. */
  nowY: number;
  height: number;
  first: number;
  last: number;
  /** Map a time to y, for hour ticks. */
  yOf: (t: number) => number | null;
}

/** `nowGap`: room left under the now line (for its Start button when nothing runs). */
export function buildRoute(s: State, now: number, plans: Plan[], staleSince: number | null, nowGap = 0): RouteModel {
  const wakeStart = Date.parse(s.day.wake_start ?? s.day.day_start);
  const wakeEnd = Date.parse(s.day.wake_end ?? s.day.day_end);
  const blocks = [...s.blocks].sort((a, b) => a.t_start.localeCompare(b.t_start));
  const events = s.day.planned.filter((e) => !e.all_day);
  const hour = 3_600_000;
  const first = Math.floor(Math.min(wakeStart, ...blocks.map((b) => Date.parse(b.t_start))) / hour) * hour;
  const last = Math.ceil(Math.max(wakeEnd, now + hour, ...events.map((e) => Date.parse(e.t_end))) / hour) * hour;
  const seen = staleSince ?? now;

  const raw: Omit<Span, "top" | "height">[] = [];
  // The past: what was recorded, and the gaps between.
  let cursor = first;
  for (const b of blocks) {
    const a = Date.parse(b.t_start);
    // A timer started this instant still shows (its start is now).
    if (a > seen || (a === seen && b.t_end !== null)) continue;
    const z = Math.min(b.t_end ? Date.parse(b.t_end) : now, seen);
    if (a > cursor) raw.push({ kind: "untracked", key: `u${cursor}`, start: cursor, end: a });
    raw.push({ kind: b.t_end === null ? "running" : "block", key: b.id, start: a, end: Math.max(z, a + 60_000), block: b });
    cursor = Math.max(cursor, z);
  }
  if (staleSince !== null) {
    if (cursor < staleSince) raw.push({ kind: "untracked", key: `u${cursor}`, start: cursor, end: staleSince });
    raw.push({ kind: "silent", key: "silent", start: staleSince, end: now });
  } else if (cursor < now && !raw.some((r) => r.kind === "running")) {
    raw.push({ kind: "untracked", key: `u${cursor}`, start: cursor, end: now });
  }
  // The future: calendar events and his own plans, with the free time between.
  const ahead = [
    ...events.map((e) => ({ kind: "event" as const, key: e.event_id, start: Math.max(Date.parse(e.t_start), now), end: Date.parse(e.t_end), event: e })),
    ...plans.map((p) => ({ kind: "plan" as const, key: `p${p.taskId}`, start: Math.max(p.start, now), end: p.start + p.minutes * 60_000, plan: p })),
  ]
    .filter((x) => x.end > now)
    .sort((a, b) => a.start - b.start);
  cursor = now;
  for (const x of ahead) {
    if (x.start < cursor) continue; // an overlap: the earlier one wins on the route
    if (x.start - cursor >= 10 * 60_000) raw.push({ kind: "free", key: `f${cursor}`, start: cursor, end: x.start });
    else if (x.start > cursor) raw.push({ kind: "untracked", key: `g${cursor}`, start: cursor, end: x.start });
    raw.push(x);
    cursor = x.end;
  }
  if (cursor < last) raw.push({ kind: "free", key: `f${cursor}`, start: cursor, end: last });

  let y = 0;
  let nowY = 0;
  let gapDone = false;
  const spans: Span[] = raw.map((r) => {
    if (!gapDone && r.start >= now && r.kind !== "silent" && r.kind !== "running") {
      y += nowGap;
      gapDone = true;
    }
    const height = heightOf(r.kind, (r.end - r.start) / 60_000);
    if (r.end <= now || r.kind === "running" || r.kind === "silent") nowY = y + height;
    const sp = { ...r, top: y, height };
    y += height;
    return sp;
  });
  if (!spans.some((sp) => sp.end <= now || sp.kind === "running")) nowY = 0;
  const yOf = (t: number) => {
    const sp = spans.find((x) => t >= x.start && t < x.end);
    if (!sp) return null;
    return sp.top + ((t - sp.start) / (sp.end - sp.start)) * sp.height;
  };
  return { spans, nowY, height: y, first, last, yOf };
}

/** Free gaps from now on, for planning a task without dragging. */
export function freeGaps(route: RouteModel): { start: number; end: number }[] {
  return route.spans.filter((x) => x.kind === "free").map((x) => ({ start: x.start, end: x.end }));
}

// ---- the untimed column ----

export interface Waiting {
  id: string;
  title: string;
  note: string;
  google: boolean;
  flaggedAt: string;
  /** Part of the crowded-queue link, not a real flag in the store. */
  crowd: boolean;
}

/** Notes for the crowded-queue link: ten more open tasks the agent would plausibly flag. */
const CROWD: Record<string, string> = {
  "Book flights to Lisbon": "Fares went up 8% this week; the Thursday flight still has seats.",
  "Move the emergency fund to the high-yield account": "Three days late, and the transfer takes three business days.",
  "Fix the crash on rotate in the map view": "Three beta testers hit it yesterday; the trace is in the thread.",
  "Choose the kitchen paint colour": "The painter has to order by Monday.",
  "Write release notes for beta 0.4": "Beta 0.4 builds tonight and the notes are half done.",
  "Return the library books": "Due tomorrow; the branch closes at 18:00.",
  "Book a sports massage": "Sunday is the long run; Thursday has openings.",
  "Renew the developer account": "It lapses 30 Nov, and the beta stops installing after that.",
  "Invite twenty beta testers": "Lena has the list ready and asked whether to send it.",
  "Send Noor the photos from the hike": "Noor asked for them again yesterday.",
};

export function waitingOn(s: State, crowded: boolean, crowdSeen: string[]): Waiting[] {
  const real = s.nodes
    .filter((n) => n.flagged_at !== null && isOpen(n))
    .map((n) => ({ id: n.id, title: n.title, note: n.flag_note ?? "", google: n.google_task_id !== null, flaggedAt: n.flagged_at!, crowd: false }));
  const extra = crowded
    ? s.nodes
        .filter((n) => n.title in CROWD && isOpen(n) && n.flagged_at === null && !crowdSeen.includes(n.id))
        .map((n, i) => ({
          id: n.id, title: n.title, note: CROWD[n.title]!, google: n.google_task_id !== null,
          flaggedAt: new Date(Date.parse(s.day.now) - (i + 1) * 47 * 60_000).toISOString(), crowd: true,
        }))
    : [];
  return [...real, ...extra].sort((a, b) => a.flaggedAt.localeCompare(b.flaggedAt));
}
