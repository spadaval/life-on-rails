// The wall display's screens as HTML pages, laid out at 1040x780 and
// rendered at 1.8x to the TRMNL X's 1872x1404. Four grays, read from across a
// kitchen. Pure: no database, no clock.
//
// Today is the day arc: the waking day as a route, what happened inked by
// goal to the left of now, what is planned hollow to the right, and the one
// thing to do inside it. Goals is the week per goal with its next task.
// Tasks is everything open by when it is due.
import { dueLabel } from "../../components/format.js";

export const FRAME_W = 1040;
export const FRAME_H = 780;
export const FRAME_SCALE = 1.8;

export const SCREENS = ["today", "goals", "tasks"] as const;
export type Screen = (typeof SCREENS)[number];

/** Wake reasons that are a touch. Left and right never reach us; they browse the device's stored screens. */
const TOUCH = new Set(["button", "EXT0", "EXT1", "touchpad"]);

/** The screen a stored frame's name is of: "plugin-goals0-1793846222" is Goals. */
export function screenOf(filename: string | null): Screen | null {
  const m = /^plugin-(today|goals|tasks)0*-\d+/.exec(filename ?? "");
  return m ? (m[1] as Screen) : null;
}

/**
 * The screen a request should draw. The quarter-hourly timer (and power-on)
 * draws Today. A touch from our firmware is a middle tap, and redraws the
 * screen it names as being browsed; a touch from stock firmware is the menu's
 * Next Screen, and steps on through Today, Goals, Tasks. A special function
 * from the stock menu (Restart Playlist, say) goes home to Today.
 */
export function nextScreen(current: Screen, updateSource: string | null, specialFunction: string | null, browsing: string | null = null): Screen {
  if (specialFunction) return "today";
  if (!TOUCH.has(updateSource ?? "")) return "today";
  return screenOf(browsing) ?? SCREENS[(SCREENS.indexOf(current) + 1) % SCREENS.length]!;
}

// ---- inks ----

/**
 * How a goal is drawn: 0-3 are the own inks of the four goals with the
 * largest weekly targets, OTHER any other goal, null time or a task under no goal.
 */
export type Ink = number | null;
export const OWN_INKS = 4;
export const OTHER = 4;

function inkStroke(ink: Ink, w: number): string {
  switch (ink) {
    case 0: return `stroke="#000" stroke-width="${w}"`;
    case 1: return `stroke="#555" stroke-width="${w}"`;
    case 2: return `stroke="#000" stroke-width="${w}" stroke-dasharray="${f1(w * 0.45)} ${f1(w * 0.35)}"`;
    case 3: return `stroke="#aaa" stroke-width="${w}"`;
    case OTHER: return `stroke="#000" stroke-width="${f1(w * 0.45)}"`;
    default: return `stroke="#555" stroke-width="${f1(w * 0.35)}" stroke-dasharray="5 6"`;
  }
}

/** The goal's ink as a vertical strip at the start of a row; hollow when the row has no goal of its own. */
function inkStrip(ink: Ink, h: number): string {
  if (ink === null || ink === OTHER) return `<svg width="10" height="${h}" style="flex:none"><rect x="1" y="1" width="8" height="${h - 2}" fill="none" stroke="#aaa" stroke-width="2"/></svg>`;
  return `<svg width="10" height="${h}" style="flex:none"><line x1="5" x2="5" y1="0" y2="${h}" ${inkStroke(ink, 10).replace(/stroke-dasharray="[^"]*"/, 'stroke-dasharray="5 4"')}/></svg>`;
}

/** A title with its trailing "(...)" cut, unless the goal has a short title of its own. */
export function shortTitle(title: string, short: string | null = null): string {
  return short?.trim() || title.replace(/\s*\([^()]*\)\s*$/, "").trim() || title;
}

// ---- the one thing ----

export interface One {
  kicker: string;
  text: string;
  why: string | null;
}

export interface PickInput {
  now: Date;
  timezone: string;
  pin: { text: string; why: string | null; until: string | null } | null;
  running: { name: string; since: string } | null;
  /** Timed calendar events today, in order. */
  events: { title: string; t_start: string; t_end: string }[];
  /** The first line of this morning's brief. */
  brief: string | null;
  /** Flagged tasks, oldest flag first. */
  flagged: { title: string; note: string | null }[];
}

/** An event this close counts as the next thing. */
export const SOON_MS = 30 * 60_000;

/**
 * The one thing on Today, first match wins: what the agent pinned (until it
 * expires), the running timer, an event under way or starting within half an
 * hour, the brief's next action, the oldest flagged task.
 */
export function pickOne(p: PickInput): One | null {
  const hm = clockFmt(p.timezone);
  const now = p.now.getTime();
  if (p.pin && (!p.pin.until || new Date(p.pin.until).getTime() > now)) return { kicker: "Pinned", text: p.pin.text, why: p.pin.why };
  if (p.running) return { kicker: `Running since ${hm(new Date(p.running.since))}`, text: p.running.name, why: null };
  const span = (e: PickInput["events"][number]) => [new Date(e.t_start).getTime(), new Date(e.t_end).getTime()] as const;
  const live = p.events.find((e) => span(e)[0] <= now && span(e)[1] > now);
  if (live) return { kicker: "Now", text: live.title, why: `until ${hm(new Date(live.t_end))}` };
  const soon = p.events.find((e) => span(e)[0] > now && span(e)[0] - now <= SOON_MS);
  if (soon) return { kicker: `At ${hm(new Date(soon.t_start))}`, text: soon.title, why: `${hm(new Date(soon.t_start))} – ${hm(new Date(soon.t_end))}` };
  if (p.brief?.trim()) {
    const [text, why] = splitBrief(p.brief);
    return { kicker: "From this morning's brief", text, why };
  }
  const f = p.flagged[0];
  if (f) return { kicker: "Flagged", text: f.title, why: f.note };
  return null;
}

/** "Do X. Due Fri, two days left." → the action, and what follows as its reason. */
export function splitBrief(line: string): [string, string | null] {
  const m = /^(.{15,}?[.!?])\s+(\S.*)$/.exec(line.trim());
  return m ? [m[1]!, m[2]!] : [line.trim(), null];
}

// ---- shared ----

/** What every screen's head needs. */
export interface Shell {
  now: Date;
  timezone: string;
  date: string;
  /** Shown reversed in the head: a job that is failing, a battery that needs charging. */
  alerts: string[];
  /** @font-face rules for Saira and Saira Condensed. */
  fontCss: string;
}

const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!);
const f1 = (n: number) => n.toFixed(1);
const hrs = (h: number) => (Math.round(h * 10) / 10).toString();
const clip = (s: string, n: number) => (s.length > n ? `${s.slice(0, n - 1).trimEnd()}…` : s);
const STAR = "M0-10L2.2-2.2 10 0 2.2 2.2 0 10-2.2 2.2-10 0-2.2-2.2Z";
const starSvg = (size: number) => `<svg width="${size}" height="${size}" viewBox="-10 -10 20 20" style="flex:none"><path d="${STAR}"/></svg>`;

const clockFmt = (tz: string) => {
  const fmt = new Intl.DateTimeFormat("en-GB", { timeZone: tz, hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
  return (d: Date) => fmt.format(d);
};
const ymdDate = (ymd: string) => new Date(`${ymd}T12:00:00Z`);
const dayDiff = (a: string, b: string) => Math.round((ymdDate(b).getTime() - ymdDate(a).getTime()) / 86_400_000);

/** When a task is due, as a list says it: "13 days late", "Today", "Fri", "Sun 15 Nov". */
export function dueWords(due: string, today: string): string {
  const late = dayDiff(due, today);
  if (late > 0) return `${late} ${late === 1 ? "day" : "days"} late`;
  return dueLabel(due, today);
}

const BASE_CSS = `
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:${FRAME_W}px;height:${FRAME_H}px;background:#fff;color:#000;font:400 26px/1.25 Saira,sans-serif;font-variant-numeric:tabular-nums;overflow:hidden}
.cond{font-family:"Saira Condensed",Saira,sans-serif;text-transform:uppercase;letter-spacing:.07em;font-weight:600}
.page{padding:16px 28px 0;height:${FRAME_H}px;display:flex;flex-direction:column}
header{display:flex;align-items:baseline;gap:16px;padding-bottom:8px;border-bottom:3px solid #000}
.title{font-size:32px;white-space:nowrap}
.alert{background:#000;color:#fff;font-size:22px;padding:2px 12px;white-space:nowrap;align-self:center}
.aside{margin-left:auto;font-size:22px;color:#555;white-space:nowrap}
main{flex:1;min-height:0;position:relative}
footer{display:flex;justify-content:center;gap:12px;padding:8px 0 12px}
footer i{display:block;width:12px;height:12px;border:2.5px solid #555}
footer i.on{background:#000;border-color:#000}
.mute{color:#555}
.late{font-weight:600;color:#000}
.ell{white-space:nowrap;overflow:hidden;text-overflow:ellipsis;min-width:0}
`;

/** The head (title, alerts, an aside), the screen, and a foot over the touch bar: one square per screen, this one filled. */
function doc(shell: Shell, screen: Screen, title: string, aside: string, css: string, body: string): string {
  const alerts = shell.alerts.map((a) => `<span class="alert cond">${esc(a)}</span>`).join("");
  const squares = SCREENS.map((s) => `<i${s === screen ? ' class="on"' : ""} data-screen="${s}"></i>`).join("");
  return `<!doctype html><html><head><meta charset="utf-8"><style>
${shell.fontCss}
${BASE_CSS}
${css}
</style></head><body><div class="page">
<header><span class="title cond">${esc(title)}</span>${alerts}<span class="aside cond">${esc(aside)}</span></header>
<main>${body}</main>
<footer>${squares}</footer>
</div></body></html>`;
}

const longDate = (ymd: string) => new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "long", day: "numeric", month: "long" }).format(ymdDate(ymd));
const shortDate = (ymd: string) => new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "short", day: "numeric", month: "short" }).format(ymdDate(ymd));

// ---- Today ----

export interface TodayInput extends Shell {
  wakeStart: Date;
  wakeEnd: Date;
  /** Every whole local hour inside the waking day. */
  hours: Date[];
  /** Today's tracked time, with the goal each stretch counts for. */
  blocks: { t_start: string; t_end: string | null; running: boolean; ink: Ink; goal: string | null }[];
  /** Timed calendar events, in order. */
  events: { title: string; t_start: string; t_end: string }[];
  /** Open tasks due today, at their due time. */
  deadlines: { title: string; at: string }[];
  one: One | null;
}

// The arc's geometry, in CSS pixels of the arc box.
const AW = 984;
const AH = 670;
const CX = AW / 2;
const BASE = 646;
const RX = 440;
const RY = 520;
const ROUTE_W = 16;
/** A stretch of one goal this long gets its name beside the route. */
const LABEL_MS = 40 * 60_000;
/** An hour label this close to now gives way to the time the frame was drawn. */
const NOW_CLEAR_MS = 30 * 60_000;
/** Where the one thing sits inside the arc; labels keep out of it. */
const ONE_LEFT = 240;
const ONE_RIGHT = 744;
const ONE_TOP = 290;
const ONE_BOTTOM = 570;
/** A label is never cut shorter than this. */
const MIN_LABEL = 20;
/** Labels are measured at this many pixels a character. */
const CHAR_W = 12;
/** Blocks of one goal closer than this read as one stretch. */
const JOIN_MS = 10 * 60_000;

const point = (f: number) => {
  const th = Math.PI * (1 - f);
  return { x: CX + RX * Math.cos(th), y: BASE - RY * Math.sin(th) };
};
const normal = (f: number) => {
  const th = Math.PI * (1 - f);
  const nx = Math.cos(th) / RX;
  const ny = -Math.sin(th) / RY;
  const len = Math.hypot(nx, ny);
  return { x: nx / len, y: ny / len };
};
const arcPath = (a: number, b: number) => {
  const steps = Math.max(2, Math.ceil((b - a) * 160));
  let d = "";
  for (let i = 0; i <= steps; i++) {
    const p = point(a + ((b - a) * i) / steps);
    d += `${i === 0 ? "M" : "L"}${f1(p.x)},${f1(p.y)}`;
  }
  return d;
};

function arcSvg(input: TodayInput, hm: (d: Date) => string): string {
  const lo = input.wakeStart.getTime();
  const hi = input.wakeEnd.getTime();
  const now = input.now.getTime();
  const frac = (t: number) => Math.min(1, Math.max(0, (t - lo) / (hi - lo)));
  const parts: string[] = [];
  const labels: { f: number; text: string }[] = [];

  // Chart crosses stand in for the engraved grid.
  const grid: string[] = [];
  for (let x = 12; x < AW; x += 60) {
    for (let y = 24; y < BASE - 10; y += 60) {
      // None under the one thing, so its text stands clear.
      if (x > ONE_LEFT - 10 && x < ONE_RIGHT + 10 && y > ONE_TOP - 10 && y < ONE_BOTTOM) continue;
      grid.push(`M${x - 4},${y}h8M${x},${y - 4}v8`);
    }
  }
  parts.push(`<path d="${grid.join("")}" stroke="#aaa" stroke-width="1.5"/>`);
  parts.push(`<line x1="${CX - RX - 20}" x2="${CX + RX + 20}" y1="${BASE}" y2="${BASE}" stroke="#aaa" stroke-width="2"/>`);
  parts.push(`<path d="${arcPath(0, 1)}" fill="none" stroke="#aaa" stroke-width="4" stroke-dasharray="10 9"/>`);

  // Planned: events as hollow bands, deadlines as a black square across the route.
  for (const e of input.events) {
    const s = new Date(e.t_start).getTime();
    const z = new Date(e.t_end).getTime();
    if (z <= lo || s >= hi) continue;
    const a = frac(s);
    const b = Math.max(frac(z), a + 0.004);
    const w = ROUTE_W * 1.1;
    parts.push(`<path d="${arcPath(a, b)}" fill="none" stroke="#000" stroke-width="${f1(w)}"/>`);
    if (b - a > 0.008) parts.push(`<path d="${arcPath(a + 0.003, b - 0.003)}" fill="none" stroke="#fff" stroke-width="${f1(w - 7)}"/>`);
    if (s >= now) labels.push({ f: a, text: `${hm(new Date(s))} ${clip(e.title, 28)}` });
  }
  for (const d of input.deadlines) {
    const t = new Date(d.at).getTime();
    const f = frac(t);
    const p = point(f);
    const n = normal(f);
    parts.push(`<line x1="${f1(p.x - n.x * 22)}" y1="${f1(p.y - n.y * 22)}" x2="${f1(p.x + n.x * 22)}" y2="${f1(p.y + n.y * 22)}" stroke="#000" stroke-width="5"/>`);
    parts.push(`<rect x="${f1(p.x - 9)}" y="${f1(p.y - 9)}" width="18" height="18" fill="#000"/>`);
    labels.push({ f, text: `${hm(new Date(t))} ${clip(d.title, 26)} (due)` });
  }

  // Recorded: each block in its goal's ink, the running one heavier.
  const runs: { goal: string; a: number; z: number }[] = [];
  for (const b of input.blocks) {
    const s = new Date(b.t_start).getTime();
    const z = Math.min(b.t_end ? new Date(b.t_end).getTime() : now, now);
    const a = frac(s);
    const e = frac(z);
    if (e <= a) continue;
    parts.push(`<path d="${arcPath(a, e)}" fill="none" ${inkStroke(b.ink, b.running ? ROUTE_W + 6 : ROUTE_W)}/>`);
    if (b.goal === null || b.ink === null) continue;
    const last = runs[runs.length - 1];
    if (last && last.goal === b.goal && s - last.z <= JOIN_MS) last.z = Math.max(last.z, z);
    else runs.push({ goal: b.goal, a: s, z });
  }
  for (const r of runs) if (r.z - r.a >= LABEL_MS) labels.push({ f: (frac(r.a) + frac(r.z)) / 2, text: r.goal });

  // Taken spots for labels: hour names, the reticle, and labels already placed.
  const placed: { x0: number; x1: number; y: number }[] = [];
  if (now >= lo && now <= hi) {
    const p = point(frac(now));
    for (const dy of [-30, 0, 30]) placed.push({ x0: p.x - 34, x1: p.x + 34, y: p.y + dy });
  }

  // Hour ticks across the route, every second hour named inside it.
  for (const h of input.hours) {
    const f = frac(h.getTime());
    const p = point(f);
    const n = normal(f);
    const label = hm(h).slice(0, 2);
    const even = Number(label) % 2 === 0;
    const len = even ? 14 : 8;
    parts.push(`<line x1="${f1(p.x - n.x * len)}" y1="${f1(p.y - n.y * len)}" x2="${f1(p.x + n.x * len)}" y2="${f1(p.y + n.y * len)}" stroke="#555" stroke-width="2.5"/>`);
    if (even && Math.abs(h.getTime() - now) >= NOW_CLEAR_MS) {
      const x = p.x - n.x * 34;
      const y = p.y - n.y * 34 + 8;
      parts.push(`<text x="${f1(x)}" y="${f1(y)}" text-anchor="middle" class="tick">${label}</text>`);
      placed.push({ x0: x - 16, x1: x + 16, y });
    }
  }

  // Labels go outside the route, cut to the room on their side but never
  // below MIN_LABEL characters: near the page edge they shift inward over the
  // route instead. Kept off what is already there and pushed apart (down on
  // the sides, up over the top).
  for (const l of labels.sort((a, b) => a.f - b.f)) {
    const p = point(l.f);
    const n = normal(l.f);
    const side = l.f < 0.4 ? "end" : l.f > 0.6 ? "start" : "middle";
    let x = p.x + n.x * (ROUTE_W + 16);
    let y = p.y + n.y * (ROUTE_W + 16) + (side === "middle" ? 0 : 8);
    const room = side === "end" ? x - 4 : side === "start" ? AW - 4 - x : AW - 8;
    const text = clip(l.text, Math.max(MIN_LABEL, Math.floor(room / CHAR_W)));
    const wid = text.length * CHAR_W;
    let x0 = side === "end" ? x - wid : side === "start" ? x : x - wid / 2;
    if (x0 + wid > AW - 4) {
      x -= x0 + wid - (AW - 4);
      x0 = AW - 4 - wid;
    }
    if (x0 < 4) {
      x += 4 - x0;
      x0 = 4;
    }
    const step = side === "middle" ? -30 : 30;
    for (let i = 0; i < 6 && placed.some((q) => Math.abs(q.y - y) < 30 && x0 < q.x1 && x0 + wid > q.x0); i++) y += step;
    placed.push({ x0, x1: x0 + wid, y });
    parts.push(`<text x="${f1(x)}" y="${f1(y)}" text-anchor="${side}" class="lab">${esc(text)}</text>`);
  }

  // Now: the reticle on the route, and the time it was drawn beside it.
  if (now >= lo && now <= hi) {
    const f = frac(now);
    const p = point(f);
    const n = normal(f);
    const r = 24;
    const k = 10;
    parts.push(`<circle cx="${f1(p.x)}" cy="${f1(p.y)}" r="${r + 2}" fill="#fff"/>`);
    for (const [sx, sy] of [[-1, -1], [1, -1], [1, 1], [-1, 1]] as const) {
      parts.push(`<path d="M${f1(p.x + sx * r)},${f1(p.y + sy * (r - k))}V${f1(p.y + sy * r)}H${f1(p.x + sx * (r - k))}" fill="none" stroke="#000" stroke-width="4"/>`);
    }
    parts.push(`<path d="${STAR}" transform="translate(${f1(p.x)} ${f1(p.y)}) scale(1.2)" fill="#000"/>`);
    parts.push(`<text x="${f1(p.x - n.x * 58)}" y="${f1(p.y - n.y * 58 + 10)}" text-anchor="middle" class="now">${hm(input.now)}</text>`);
  }
  return `<svg width="${AW}" height="${AH}" viewBox="0 0 ${AW} ${AH}">${parts.join("")}</svg>`;
}

const TODAY_CSS = `
.arc{position:absolute;left:0;top:0;width:${AW}px;height:${AH}px}
.tick{font:600 22px "Saira Condensed",sans-serif;fill:#555}
.lab{font:500 22px Saira,sans-serif;fill:#000}
.now{font:600 26px "Saira Condensed",sans-serif;fill:#000;letter-spacing:.04em}
.one{position:absolute;left:${ONE_LEFT}px;width:${ONE_RIGHT - ONE_LEFT}px;top:${ONE_TOP}px;max-height:${ONE_BOTTOM - ONE_TOP}px;overflow:hidden;text-align:center;padding:8px 0}
.one .k{font-size:20px;color:#555}
.one .t{font-weight:600;line-height:1.15;margin-top:6px;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:4;overflow:hidden}
.one .w{color:#555;margin-top:8px;font-size:24px}
.one .none{color:#555;font-size:34px}
`;

export function todayHtml(input: TodayInput): string {
  const hm = clockFmt(input.timezone);
  const o = input.one;
  const size = !o ? 0 : o.text.length <= 60 ? 44 : o.text.length <= 100 ? 38 : 32;
  const one = o
    ? `<div class="one"><p class="k cond">${esc(o.kicker)}</p><p class="t" style="font-size:${size}px">${esc(o.text)}</p>${o.why ? `<p class="w">${esc(o.why)}</p>` : ""}</div>`
    : `<div class="one"><p class="none">Nothing pressing</p></div>`;
  return doc(input, "today", longDate(input.date), "", TODAY_CSS, `<div class="arc">${arcSvg(input, hm)}</div>${one}`);
}

// ---- Goals ----

export interface GoalsInput extends Shell {
  /** The seven local days ending today, oldest first. */
  days: string[];
  /** Goals with a weekly target first, largest target first; then the rest. */
  goals: {
    title: string;
    ink: Ink;
    /** Hours on each of `days`; they add up to the week. */
    hours: number[];
    target: number | null;
    next: { title: string; due: string | null; overdue: boolean } | null;
  }[];
}

const MAX_GOAL_ROWS = 7;
const BAR_H = 44;

export function goalsHtml(input: GoalsInput): string {
  const shown = input.goals.slice(0, MAX_GOAL_ROWS);
  const more = input.goals.length - shown.length;
  const peak = Math.max(1, ...shown.flatMap((g) => g.hours));
  const dow = (ymd: string) => new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "short" }).format(ymdDate(ymd)).slice(0, 2);
  const next = (g: GoalsInput["goals"][number]) => {
    if (!g.next) return `<span class="mute">Nothing open</span>`;
    const when = g.next.due ? dueWords(g.next.due, input.date) : null;
    return `<span class="ell">${esc(g.next.title)}</span>${when ? `<span class="due${g.next.overdue ? " late" : ""}">&nbsp;· ${esc(when)}</span>` : ""}`;
  };
  const rows = shown.map((g) => {
    if (g.target === null) {
      return `<div class="row slim">${inkStrip(null, 40)}<div class="name"><p class="t">${esc(g.title)} <span class="mute" style="font-weight:400;font-size:22px">no weekly target</span></p><p class="n mute">${next(g)}</p></div></div>`;
    }
    const week = g.hours.reduce((a, b) => a + b, 0);
    const pct = g.target > 0 ? Math.min(100, (week / g.target) * 100) : 0;
    const bars = g.hours.map((h, i) => {
      const height = Math.round((Math.min(h, peak) / peak) * BAR_H);
      return `<div class="day"><div class="bar">${height > 0 ? inkColumn(g.ink, height) : ""}</div><span class="cond${i === g.hours.length - 1 ? " today" : ""}">${dow(input.days[i] ?? input.date)}</span></div>`;
    }).join("");
    return `<div class="row">${inkStrip(g.ink, 66)}<div class="name"><p class="t ell">${esc(g.title)}</p><p class="n mute">${next(g)}</p></div>
<div class="days">${bars}</div>
<div class="sum"><p class="cond">${hrs(week)}<span class="mute"> / ${hrs(g.target)}h</span></p><div class="meter"><b style="width:${pct.toFixed(1)}%"></b></div></div></div>`;
  });
  const first = input.days[0];
  const last = input.days[input.days.length - 1];
  const aside = `${first && last ? `${shortDate(first)} – ${shortDate(last)} · ` : ""}hours / weekly target${more > 0 ? ` · ${more} more` : ""}`;
  return doc(input, "goals", "Goals", aside, `
.row{display:flex;align-items:center;gap:20px;padding:7px 0;border-bottom:1.5px solid #aaa}
.row:last-child{border-bottom:0}
.name{flex:1;min-width:0}
.name .t{font-size:30px;font-weight:600}
.slim .t{font-size:26px}
.name .n{font-size:22px;display:flex;min-width:0}
.name .n .due{white-space:nowrap;flex:none}
.days{display:flex;gap:6px}
.day{display:flex;flex-direction:column;align-items:center;gap:2px}
.day .bar{width:22px;height:${BAR_H}px;display:flex;align-items:flex-end;border-bottom:2px solid #aaa}
.day span{font-size:15px;color:#555}
.day span.today{color:#000}
.sum{width:170px;text-align:right}
.sum p{font-size:34px}
.sum p span{font-size:24px}
.meter{height:10px;background:#aaa;margin-top:4px}
.meter b{display:block;height:100%;background:#000}
.empty{color:#555;padding:16px 0}
`, rows.length ? rows.join("") : `<p class="empty">No goals yet</p>`);
}

/** A day's bar on the Goals page, filled in the goal's ink. */
function inkColumn(ink: Ink, h: number): string {
  if (ink === 2) return `<svg width="22" height="${h}"><defs><pattern id="hatch" width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(45)"><rect width="8" height="8" fill="#fff"/><rect width="4" height="8" fill="#000"/></pattern></defs><rect width="22" height="${h}" fill="url(#hatch)"/></svg>`;
  const fill = ink === 1 ? "#555" : ink === 3 ? "#aaa" : "#000";
  return `<svg width="22" height="${h}"><rect width="${ink === OTHER || ink === null ? 8 : 22}" x="${ink === OTHER || ink === null ? 7 : 0}" height="${h}" fill="${fill}"/></svg>`;
}

// ---- Tasks ----

export interface TasksInput extends Shell {
  /** Open tasks in due order, undated last. */
  tasks: { title: string; due: string | null; overdue: boolean; waiting: string | null; flagged: boolean; ink: Ink }[];
}

// Vertical budget of the Tasks page, in CSS pixels.
const TASKS_H = 668;
const HEAD_H = 40;
const ROW_H = 39;
const PAIR_H = 26;

export function tasksHtml(input: TasksInput): string {
  const dated = input.tasks.filter((t) => t.due !== null);
  const undated = input.tasks.filter((t) => t.due === null);
  const diff = (t: TasksInput["tasks"][number]) => dayDiff(input.date, t.due!);
  const groups: [string, TasksInput["tasks"]][] = [
    ["Late", dated.filter((t) => diff(t) < 0)],
    ["Today", dated.filter((t) => diff(t) === 0)],
    ["This week", dated.filter((t) => diff(t) > 0 && diff(t) < 7)],
    ["Later", dated.filter((t) => diff(t) >= 7)],
  ];
  const late = groups[0]![1].length;

  // Dated rows first, keeping room for the undated heading and a line of them.
  let left = TASKS_H - (undated.length ? HEAD_H + PAIR_H : 0);
  let hidden = 0;
  const sections: string[] = [];
  for (const [name, tasks] of groups) {
    if (!tasks.length) continue;
    if (left < HEAD_H + ROW_H) {
      hidden += tasks.length;
      continue;
    }
    left -= HEAD_H;
    const fit = Math.min(tasks.length, Math.floor(left / ROW_H));
    left -= fit * ROW_H;
    hidden += tasks.length - fit;
    const rows = tasks.slice(0, fit).map((t) => {
      const lateRow = diff(t) < 0;
      const when = t.waiting ? `waiting on ${t.waiting}` : dueWords(t.due!, input.date);
      return `<div class="r">${inkStrip(t.ink, 30)}${t.flagged ? starSvg(24) : ""}<span class="ell t${lateRow ? " late" : ""}${t.waiting ? " mute" : ""}">${esc(t.title)}</span><span class="w${lateRow || (t.overdue && !t.waiting) ? " late" : " mute"}">${esc(when)}</span></div>`;
    });
    sections.push(`<h3 class="cond"><b>${name}</b> ${tasks.length}</h3>${rows.join("")}`);
  }
  if (undated.length) {
    left += HEAD_H + PAIR_H;
    const pairs = Math.max(1, Math.floor((left - HEAD_H) / PAIR_H));
    const room = pairs * 2;
    const shown = undated.length > room ? undated.slice(0, room - 1) : undated;
    const items = shown.map((t) => `<p class="ell u${t.waiting ? " mute" : ""}">${t.flagged ? starSvg(18) : ""}${esc(t.title)}</p>`);
    if (shown.length < undated.length) items.push(`<p class="u mute">and ${undated.length - shown.length} more</p>`);
    sections.push(`<h3 class="cond"><b>No date</b> ${undated.length}</h3><div class="und">${items.join("")}</div>`);
  }
  const aside = [`${input.tasks.length} open`, late ? `${late} late` : null, undated.length ? `${undated.length} undated` : null, hidden ? `${hidden} not shown` : null].filter(Boolean).join(" · ");
  return doc(input, "tasks", "Tasks", aside, `
h3{font-size:22px;color:#555;height:${HEAD_H}px;padding-top:12px}
h3 b{color:#000}
.r{display:flex;align-items:center;gap:14px;height:${ROW_H}px;border-bottom:1.5px solid #aaa}
.r .t{flex:1;font-size:28px}
.r .w{font-size:24px;white-space:nowrap}
.und{display:grid;grid-template-columns:1fr 1fr;gap:0 28px}
.u{font-size:21px;line-height:${PAIR_H}px;height:${PAIR_H}px;color:#555}
.u svg{margin-right:6px;vertical-align:-2px}
.empty{color:#555;padding:16px 0}
`, sections.length ? sections.join("") : `<p class="empty">Nothing open</p>`);
}
