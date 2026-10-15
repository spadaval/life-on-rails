// The wall display's screens as HTML pages, laid out at 1040x780 and
// rendered at 1.8x to the TRMNL X's 1872x1404. Four grays, read from across a
// kitchen. Today is home: the day arc, what is left of it, what is running,
// what comes next, and what the agent flagged. A tap on the middle of the
// touch bar steps to Goals, then Tasks. Pure: no database, no clock.
import { dueLabel, duration } from "../../components/format.js";

export const FRAME_W = 1040;
export const FRAME_H = 780;
export const FRAME_SCALE = 1.8;

export const SCREENS = ["today", "goals", "tasks"] as const;
export type Screen = (typeof SCREENS)[number];

/** The wake reasons that are a touch: the middle of the bar (left and right are handled on the device). */
const TOUCH = new Set(["button", "EXT0", "EXT1"]);

/** The screen a wake should draw: Today on a timer, the next one on a tap. */
export function nextScreen(current: Screen, updateSource: string | null): Screen {
  if (!TOUCH.has(updateSource ?? "")) return "today";
  return SCREENS[(SCREENS.indexOf(current) + 1) % SCREENS.length]!;
}

/** What every screen's head and foot need. */
export interface Shell {
  now: Date;
  timezone: string;
  date: string;
  /** Shown reversed in the head: a job that is failing, a battery that needs charging. */
  alerts: string[];
  /** @font-face rules for Saira and Saira Condensed. */
  fontCss: string;
}

export interface TodayInput extends Shell {
  wakeStart: Date;
  wakeEnd: Date;
  /** Every whole local hour inside the waking day. */
  hours: Date[];
  blocks: { t_start: string; t_end: string | null; paused: boolean; committed: boolean; bare: boolean }[];
  /** Timed calendar events, in order. */
  events: { title: string; t_start: string }[];
  running: { name: string; since: string } | null;
  recordedS: number;
  needsYou: string[];
  toConfirm: number;
}

export interface GoalsInput extends Shell {
  /** Active goals with a weekly target: hours over the last seven days. */
  goals: { title: string; hours: number; target: number; starving: boolean }[];
}

export interface TasksInput extends Shell {
  /** Open tasks in due order, undated last. */
  tasks: { title: string; due: string | null; overdue: boolean; waiting: string | null; flagged: boolean }[];
}

const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!);
const f1 = (n: number) => n.toFixed(1);

// The arc's geometry, in CSS pixels of the arc box.
const AW = 984;
const AH = 410;
const CX = AW / 2;
const BASE = 392;
const RX = 432;
const RY = 300;

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
const arcPath = (f0: number, f1v: number) => {
  const steps = Math.max(2, Math.ceil((f1v - f0) * 120));
  let d = "";
  for (let i = 0; i <= steps; i++) {
    const p = point(f0 + ((f1v - f0) * i) / steps);
    d += `${i === 0 ? "M" : "L"}${f1(p.x)},${f1(p.y)}`;
  }
  return d;
};
const STAR = "M0-10L2.2-2.2 10 0 2.2 2.2 0 10-2.2 2.2-10 0-2.2-2.2Z";

function arcSvg(input: TodayInput, hm: (d: Date) => string): string {
  const lo = input.wakeStart.getTime();
  const hi = input.wakeEnd.getTime();
  const now = input.now.getTime();
  const frac = (t: number) => Math.min(1, Math.max(0, (t - lo) / (hi - lo)));
  const out: string[] = [];

  // Chart crosses stand in for the engraved grid.
  for (let x = 12; x < AW; x += 60) for (let y = 30; y < BASE - 10; y += 60) out.push(`M${x - 4},${y}h8M${x},${y - 4}v8`);
  const parts = [`<path d="${out.join("")}" stroke="#aaa" stroke-width="1.5"/>`];
  parts.push(`<line x1="${CX - RX - 24}" x2="${CX + RX + 24}" y1="${BASE}" y2="${BASE}" stroke="#aaa" stroke-width="2"/>`);
  parts.push(`<path d="${arcPath(0, 1)}" fill="none" stroke="#aaa" stroke-width="4" stroke-dasharray="10 9"/>`);

  // Recorded time: the running block heaviest, then confirmed, unconfirmed, and bare phone or place time dashed grey.
  for (const b of input.blocks) {
    const end = b.t_end ? new Date(b.t_end).getTime() : now;
    const f0 = frac(new Date(b.t_start).getTime());
    const fe = frac(Math.min(end, now));
    if (fe <= f0) continue;
    const style = !b.t_end && !b.paused
      ? 'stroke="#000" stroke-width="20"'
      : b.committed
        ? 'stroke="#000" stroke-width="15"'
        : b.bare
          ? 'stroke="#555" stroke-width="7" stroke-dasharray="6 6"'
          : 'stroke="#000" stroke-width="7"';
    parts.push(`<path d="${arcPath(f0, fe)}" fill="none" ${style}/>`);
  }

  for (const h of input.hours) {
    const f = frac(h.getTime());
    const p = point(f);
    const n = normal(f);
    const label = hm(h).slice(0, 2);
    const even = Number(label) % 2 === 0;
    const len = even ? 16 : 9;
    parts.push(`<line x1="${f1(p.x - n.x * len)}" y1="${f1(p.y - n.y * len)}" x2="${f1(p.x + n.x * len)}" y2="${f1(p.y + n.y * len)}" stroke="#555" stroke-width="2.5"/>`);
    if (even) {
      parts.push(`<text x="${f1(p.x + n.x * 38)}" y="${f1(p.y + n.y * 38 + 9)}" text-anchor="middle" class="tick">${label}</text>`);
    }
  }

  // Planned time is hollow: a waypoint diamond on the route where each event starts.
  for (const e of input.events) {
    const t = new Date(e.t_start).getTime();
    if (t < lo || t > hi) continue;
    const f = frac(t);
    const p = point(f);
    parts.push(`<rect x="${f1(p.x - 12)}" y="${f1(p.y - 12)}" width="24" height="24" transform="rotate(45 ${f1(p.x)} ${f1(p.y)})" fill="#fff" stroke="#000" stroke-width="4"/>`);
  }

  if (now >= lo && now <= hi) {
    const p = point(frac(now));
    const r = 26;
    const k = 11;
    for (const [sx, sy] of [[-1, -1], [1, -1], [1, 1], [-1, 1]] as const) {
      parts.push(`<path d="M${f1(p.x + sx * r)},${f1(p.y + sy * (r - k))}V${f1(p.y + sy * r)}H${f1(p.x + sx * (r - k))}" fill="none" stroke="#000" stroke-width="4"/>`);
    }
    parts.push(`<path d="${STAR}" transform="translate(${f1(p.x)} ${f1(p.y)}) scale(1.25)" fill="#000"/>`);
  }
  return `<svg width="${AW}" height="${AH}" viewBox="0 0 ${AW} ${AH}">${parts.join("")}</svg>`;
}

const clockFmt = (tz: string) => {
  const fmt = new Intl.DateTimeFormat("en-GB", { timeZone: tz, hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
  return (d: Date) => fmt.format(d);
};
const starSvg = (size: number) => `<svg width="${size}" height="${size}" viewBox="-10 -10 20 20"><path d="${STAR}"/></svg>`;

const BASE_CSS = `
*{box-sizing:border-box;margin:0;padding:0}
html,body{width:${FRAME_W}px;height:${FRAME_H}px;background:#fff;color:#000;font:400 26px/1.3 Saira,sans-serif;font-variant-numeric:tabular-nums;overflow:hidden}
.cond{font-family:"Saira Condensed",Saira,sans-serif;text-transform:uppercase;letter-spacing:.07em;font-weight:600}
.page{padding:18px 28px 0;height:${FRAME_H}px;display:flex;flex-direction:column}
header{display:flex;align-items:center;gap:16px;padding-bottom:10px;border-bottom:3px solid #000}
.date{font-size:34px;letter-spacing:.06em;white-space:nowrap}
.alert{background:#000;color:#fff;font-size:22px;padding:2px 12px;white-space:nowrap}
.asof{margin-left:auto;font-size:24px;color:#555;white-space:nowrap}
main{flex:1;min-height:0;display:flex;flex-direction:column}
footer{display:flex;justify-content:center;gap:44px;border-top:1.5px solid #aaa;padding:6px 0 10px;font-size:22px;color:#aaa}
footer .on{color:#000;box-shadow:inset 0 -4px 0 #000}
.panel{position:relative;padding:12px 20px;background:#555;clip-path:polygon(12px 0,100% 0,100% calc(100% - 12px),calc(100% - 12px) 100%,0 100%,0 12px)}
.panel::before{content:"";position:absolute;inset:2.5px;background:#fff;clip-path:polygon(11px 0,100% 0,100% calc(100% - 11px),calc(100% - 11px) 100%,0 100%,0 11px)}
.panel>*{position:relative}
h2{display:flex;justify-content:space-between;align-items:baseline;font-size:26px;margin-bottom:4px}
h2 small{font:400 22px Saira,sans-serif;color:#555;text-transform:none;letter-spacing:0}
li{list-style:none;display:flex;gap:14px;align-items:baseline;padding:5px 0;white-space:nowrap}
li .t{min-width:0;overflow:hidden;text-overflow:ellipsis;font-weight:500}
li .at{font-weight:600;font-family:"Saira Condensed",sans-serif;font-size:28px;letter-spacing:.04em}
li svg{flex:none;transform:translateY(2px)}
.empty{color:#555;padding:5px 0}
`;

/** The head (date, alerts, when it was drawn), the screen, and a foot over the touch bar naming the screens. */
function doc(shell: Shell, screen: Screen, css: string, body: string): string {
  const dateLabel = new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "long", day: "numeric", month: "long" }).format(new Date(`${shell.date}T12:00:00Z`));
  const alerts = shell.alerts.map((a) => `<span class="alert cond">${esc(a)}</span>`).join("");
  const tabs = SCREENS.map((s) => `<span class="cond${s === screen ? " on" : ""}">${s}</span>`).join("");
  return `<!doctype html><html><head><meta charset="utf-8"><style>
${shell.fontCss}
${BASE_CSS}
${css}
</style></head><body><div class="page">
<header><span class="date cond">${esc(dateLabel)}</span>${alerts}<span class="asof cond">Updated ${clockFmt(shell.timezone)(shell.now)}</span></header>
<main>${body}</main>
<footer>${tabs}</footer>
</div></body></html>`;
}

const TODAY_CSS = `
.arc{position:relative;height:${AH}px;margin:4px auto 0;width:${AW}px;flex:none}
.arc svg{position:absolute;inset:0}
.tick{font:600 24px "Saira Condensed",sans-serif;fill:#555}
.read{position:absolute;left:0;right:0;top:130px;text-align:center}
.big{font-size:150px;line-height:.9;letter-spacing:.02em}
.big em{font-style:normal;font-size:40px;color:#555;margin-left:14px;letter-spacing:.1em}
.sub{margin-top:10px;font-size:26px;color:#555}
.slab{display:inline-flex;align-items:baseline;gap:16px;max-width:760px;margin-top:22px;background:#000;color:#fff;padding:6px 22px 9px}
.slab b{font-weight:600;font-size:34px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.slab span{font-size:26px;white-space:nowrap}
.cols{flex:1;display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1.15fr);gap:22px;margin:4px 0 14px;min-height:0}
`;

export function todayHtml(input: TodayInput): string {
  const hm = clockFmt(input.timezone);
  const now = input.now.getTime();
  const lo = input.wakeStart.getTime();
  const hi = input.wakeEnd.getTime();

  // Clock times rather than countdowns where they can be: the frame stays up for a quarter of an hour.
  let big: string;
  let unit: string;
  let sub: string;
  if (now < lo) {
    big = hm(input.wakeStart);
    unit = "rising";
    sub = "when the waking day starts";
  } else if (now < hi) {
    big = duration((hi - now) / 1000);
    unit = "left";
    sub = `of the waking day, until ${hm(input.wakeEnd)}`;
  } else {
    big = duration(input.recordedS);
    unit = "recorded";
    sub = "The waking day is over";
  }

  const next = input.events.filter((e) => new Date(e.t_start).getTime() > now).slice(0, 3);
  const needs = input.needsYou.slice(0, 3);
  const more = input.needsYou.length - needs.length;
  const aside = [more > 0 ? `${more} more` : null, input.toConfirm > 0 ? `${input.toConfirm} to confirm` : null].filter(Boolean).join(" · ");

  return doc(input, "today", TODAY_CSS, `
<div class="arc">${arcSvg(input, hm)}
<div class="read">
<p class="big cond">${esc(big)}<em>${esc(unit)}</em></p>
<p class="sub">${esc(sub)}</p>
${input.running ? `<div class="slab"><b>${esc(input.running.name)}</b><span>since ${hm(new Date(input.running.since))}</span></div>` : ""}
</div></div>
<div class="cols">
<section class="panel"><h2 class="cond">Next</h2>${
    next.length
      ? `<ul>${next.map((e) => `<li><span class="at">${hm(new Date(e.t_start))}</span><span class="t">${esc(e.title)}</span></li>`).join("")}</ul>`
      : `<p class="empty">Nothing else on the calendar today</p>`
  }</section>
<section class="panel"><h2 class="cond">Needs you${aside ? `<small>${esc(aside)}</small>` : ""}</h2>${
    needs.length
      ? `<ul>${needs.map((t) => `<li>${starSvg(20)}<span class="t">${esc(t)}</span></li>`).join("")}</ul>`
      : `<p class="empty">Nothing flagged</p>`
  }</section>
</div>`);
}

const LIST_CSS = `
.panel{flex:1;margin:16px 0 14px;padding:16px 26px}
h2{font-size:30px;margin-bottom:8px}
h2 small{font-size:24px}
`;

const MAX_GOALS = 6;

export function goalsHtml(input: GoalsInput): string {
  const shown = input.goals.slice(0, MAX_GOALS);
  const more = input.goals.length - shown.length;
  const hours = (h: number) => (Math.round(h * 10) / 10).toString();
  const rows = shown.map((g) => {
    const pct = g.target > 0 ? Math.min(100, (g.hours / g.target) * 100) : 0;
    return `<li class="goal${g.starving ? " starving" : ""}"><p><span class="t">${esc(g.title)}</span><span class="h cond">${hours(g.hours)} / ${hours(g.target)} h</span></p><div class="meter"><b style="width:${pct.toFixed(1)}%"></b></div></li>`;
  });
  return doc(input, "goals", `${LIST_CSS}
.goal{display:block;padding:10px 0 12px;border-top:1.5px solid #aaa}
.goal:first-child{border-top:0}
.goal p{display:flex;justify-content:space-between;align-items:baseline;gap:20px}
.goal .t{font-size:34px}
.goal .h{font-size:32px;letter-spacing:.04em}
.goal.starving .t{color:#555}
.meter{height:14px;background:#aaa;margin-top:8px}
.meter b{display:block;height:100%;background:#000}
`, `<section class="panel"><h2 class="cond">Goals<small>hours in the last 7 days / weekly target${more > 0 ? ` · ${more} more` : ""}</small></h2>${
    rows.length ? `<ul>${rows.join("")}</ul>` : `<p class="empty">No goal has a weekly target</p>`
  }</section>`);
}

const MAX_TASKS = 9;

export function tasksHtml(input: TasksInput): string {
  const shown = input.tasks.slice(0, MAX_TASKS);
  const more = input.tasks.length - shown.length;
  const overdue = input.tasks.filter((t) => t.overdue).length;
  const aside = [`${input.tasks.length} open`, overdue > 0 ? `${overdue} overdue` : null, more > 0 ? `${more} not shown` : null].filter(Boolean).join(" · ");
  const rows = shown.map((t) => {
    const mark = t.flagged ? starSvg(24) : `<span class="box"></span>`;
    const when = t.waiting ? `waiting on ${esc(t.waiting)}` : t.due ? esc(dueLabel(t.due, input.date)) : "";
    return `<li class="task${t.waiting ? " waiting" : ""}">${mark}<span class="t">${esc(t.title)}</span><span class="when${t.overdue ? " over" : ""}">${when}</span></li>`;
  });
  return doc(input, "tasks", `${LIST_CSS}
.task{gap:18px;padding:9px 0;border-top:1.5px solid #aaa;align-items:center}
.task:first-child{border-top:0}
.task .t{flex:1;font-size:30px}
.task.waiting .t{color:#555}
.box{flex:none;width:22px;height:22px;border:3px solid #000}
.when{font-size:26px;color:#555;max-width:300px;overflow:hidden;text-overflow:ellipsis}
.when.over{color:#000;font-weight:600}
`, `<section class="panel"><h2 class="cond">To do<small>${esc(aside)}</small></h2>${
    rows.length ? `<ul>${rows.join("")}</ul>` : `<p class="empty">Nothing open</p>`
  }</section>`);
}
