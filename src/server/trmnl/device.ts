// The TRMNL X on the wall, speaking the firmware's own-server protocol. It
// wakes every quarter hour, asks /api/display for an image, draws it, and
// sleeps. scripts/serve.mjs maps /api/{setup,display,log} on the LAN listener
// to /trmnl/* here. One device: LOR_TRMNL_MAC gets LOR_TRMNL_KEY at setup, and
// every later request must carry that key.
//
// Screens: a timed wake always draws Today. On firmware 1.8.18 a tap on the
// middle of the touch bar opens the device's own menu; its Next Screen calls
// /api/display as a touch wake, and we step to the next screen, which gives
// way to Today after a few minutes. Special > Restart Playlist sends a
// special_function and goes home to Today. Previous Screen, Close, and left
// and right never reach us: the firmware flips through the frames it cached,
// one per screen, kept apart by the first 14 characters of the filename.
import { timingSafeEqual } from "node:crypto";
import { sql } from "../../db/client.js";
import { addDays, localDayBounds, localTimeOnDay, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { needsOwner } from "../intents.js";
import { errorMessage, log } from "../log.js";
import { jobStatuses } from "../status.js";
import { getToday, wakeWindow } from "../today.js";
import { type GraphNode, getGraph } from "../graph.js";
import { descendantSet, mergedHours, type Span } from "../scoring/formulas.js";
import {
  FRAME_H, FRAME_SCALE, FRAME_W, type GoalsInput, type Ink, OTHER, OWN_INKS, type One, type Screen, type Shell, type TasksInput, type TodayInput,
  goalsHtml, nextScreen, pickOne, shortTitle, tasksHtml, todayHtml,
} from "./frame.js";
import { getPin } from "./pin.js";
import { grayPng2 } from "./png.js";
import { frameFonts, renderFrame } from "./render.js";

const REFRESH_S = 900;
/** How long a screen stepped to stays up before Today comes back. */
const TAPPED_S = 300;
const LOW_BATTERY_PCT = 20;

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

const sameText = (a: string, b: string) => {
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
};
const normMac = (s: string | null) => (s ?? "").trim().toUpperCase();
const keyOk = (request: Request) => env.LOR_TRMNL_KEY !== "" && sameText(request.headers.get("access-token") ?? "", env.LOR_TRMNL_KEY);

/** The last frame drawn of each screen; the device fetches it by name right after /api/display. */
const latest = new Map<Screen, { name: string; png: Buffer }>();
let shown: Screen = "today";

/** "plugin-today0-1771674964": the firmware's own shape, so its cache keeps one frame per screen and ages them out. */
const frameName = (screen: Screen, now: Date) => `plugin-${screen.padEnd(6, "0")}-${Math.floor(now.getTime() / 1000)}`;

async function shellInput(now: Date, alerts: string[]): Promise<Shell> {
  const jobs = await jobStatuses(now);
  const unwell = jobs.filter((j) => j.state === "stale" || j.state === "error").length;
  return {
    now,
    timezone: env.LOR_TZ,
    date: localYmd(env.LOR_TZ, now),
    alerts: [...(unwell > 0 ? [`${unwell} ${unwell === 1 ? "job needs" : "jobs need"} a look`] : []), ...alerts],
    fontCss: frameFonts(),
  };
}

/** Active goals in the wall's order (weekly target, largest first; then the rest) and the ink each is drawn in. */
interface Goals {
  order: GraphNode[];
  ink: (goalId: string) => Ink;
  /** The first goal, in that order, an intent counts toward; null for none. */
  goalOf: (intentId: string | null) => GraphNode | null;
  under: (goalId: string) => Set<string>;
}

function wallGoals(nodes: GraphNode[], links: { from_id: string; to_id: string; kind: string }[]): Goals {
  const goals = nodes.filter((n) => n.type === "goal" && n.status === "active");
  const targeted = goals.filter((g) => g.weekly_hour_target).sort((a, b) => b.weekly_hour_target! - a.weekly_hour_target! || a.title.localeCompare(b.title));
  const rest = goals.filter((g) => !g.weekly_hour_target).sort((a, b) => a.title.localeCompare(b.title));
  const order = [...targeted, ...rest];
  const edges = links.map((l) => ({ fromId: l.from_id, toId: l.to_id, kind: l.kind as "measures" }));
  const sets = new Map(order.map((g) => [g.id, descendantSet(g.id, edges)]));
  const inks = new Map<string, Ink>(order.map((g) => [g.id, targeted.indexOf(g) >= 0 && targeted.indexOf(g) < OWN_INKS ? targeted.indexOf(g) : OTHER]));
  return {
    order,
    ink: (id) => inks.get(id) ?? null,
    goalOf: (id) => (id ? (order.find((g) => sets.get(g.id)!.has(id)) ?? null) : null),
    under: (id) => sets.get(id) ?? new Set(),
  };
}

const goalName = (g: GraphNode) => shortTitle(g.title, g.short_title);
const openOwnerTask = (n: GraphNode) => n.type === "task" && (n.status === "active" || n.status === "waiting") && n.assignee !== "agent";

export async function goalsInput(shell: Shell): Promise<GoalsInput> {
  const graph = await getGraph(sql, shell.now);
  const g = wallGoals(graph.nodes, graph.links);
  const days = Array.from({ length: 7 }, (_, i) => addDays(shell.date, i - 6));
  const bounds = days.map((d) => localDayBounds(env.LOR_TZ, d));
  const now = shell.now.getTime();
  // The scorer's own basis: committed time, merged per goal so overlaps count once.
  const spans = await sql<{ intent_id: string; t: Date; t_end: Date }[]>`
    SELECT intent_id, t, t_end FROM commitment
    WHERE kind = 'time_spent' AND t_end IS NOT NULL AND t_end > ${bounds[0]!.start} AND t < ${shell.now}`;
  const tasks = graph.nodes.filter(openOwnerTask).sort((a, b) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title));
  return {
    ...shell,
    days,
    goals: g.order.map((goal) => {
      const under = g.under(goal.id);
      const mine: Span[] = spans.filter((s) => under.has(s.intent_id)).map((s) => ({ t: s.t.getTime(), tEnd: Math.min(s.t_end.getTime(), now) }));
      const hours = bounds.map((b) => mergedHours(mine.map((s) => ({ t: Math.max(s.t, b.start.getTime()), tEnd: Math.min(s.tEnd, b.end.getTime()) }))));
      const next = tasks.find((t) => under.has(t.id));
      return {
        title: goalName(goal),
        ink: g.ink(goal.id),
        hours,
        target: goal.weekly_hour_target,
        next: next ? { title: shortTitle(next.title), due: next.due_local, overdue: next.overdue } : null,
      };
    }),
  };
}

export async function tasksInput(shell: Shell): Promise<TasksInput> {
  const graph = await getGraph(sql, shell.now);
  const g = wallGoals(graph.nodes, graph.links);
  const tasks = graph.nodes
    .filter(openOwnerTask)
    .sort((a, b) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title))
    .map((n) => {
      const goal = g.goalOf(n.id);
      return {
        title: shortTitle(n.title),
        due: n.due_local,
        overdue: n.overdue,
        waiting: n.status === "waiting" ? (n.waiting_on ?? "someone") : null,
        flagged: n.flagged_at !== null,
        ink: goal ? g.ink(goal.id) : null,
      };
    });
  return { ...shell, tasks };
}

/** The one thing on Today, and what it was chosen from. */
export async function wallOne(now: Date): Promise<One | null> {
  const date = localYmd(env.LOR_TZ, now);
  const [today, needs, pin, brief] = await Promise.all([
    getToday(sql, date, now),
    needsOwner(sql),
    getPin(sql),
    sql<{ body: string }[]>`SELECT body FROM page WHERE slug = ${`briefs/${date}`}`,
  ]);
  return pickOne({
    now,
    timezone: env.LOR_TZ,
    pin,
    running: today.running ? { name: today.running.intent ?? today.running.title ?? "Unlabelled", since: today.running.t_start } : null,
    events: today.planned.filter((e) => !e.all_day).map((e) => ({ title: e.title ?? "(no title)", t_start: e.t_start, t_end: e.t_end })),
    brief: brief[0]?.body.trim().split("\n")[0] ?? null,
    flagged: needs.map((n) => ({ title: shortTitle(n.title), note: n.flag_note })),
  });
}

export async function todayInput(shell: Shell): Promise<TodayInput> {
  const { now, date } = shell;
  const [today, graph, one] = await Promise.all([getToday(sql, date, now), getGraph(sql, now), wallOne(now)]);
  const g = wallGoals(graph.nodes, graph.links);
  const wake = wakeWindow(date);
  const hours: Date[] = [];
  for (let d = 0; d < 2; d++) {
    for (let h = 0; h < 24; h++) {
      const t = localTimeOnDay(env.LOR_TZ, addDays(date, d), `${String(h).padStart(2, "0")}:00`);
      if (t >= wake.start && t <= wake.end) hours.push(t);
    }
  }
  return {
    ...shell,
    wakeStart: wake.start,
    wakeEnd: wake.end,
    hours,
    blocks: today.actual.map((b) => {
      const goal = g.goalOf(b.intent_id);
      return {
        t_start: b.t_start,
        t_end: b.t_end,
        running: b.t_end === null && b.paused_at === null,
        ink: goal ? g.ink(goal.id) : b.intent_id ? OTHER : null,
        goal: goal ? goalName(goal) : null,
      };
    }),
    events: today.planned.filter((e) => !e.all_day).map((e) => ({ title: e.title ?? "(no title)", t_start: e.t_start, t_end: e.t_end })),
    deadlines: graph.nodes
      .filter((n) => openOwnerTask(n) && n.due_local === date && n.due_at)
      .map((n) => ({ title: shortTitle(n.title), at: n.due_at! })),
    one,
  };
}

async function screenHtml(screen: Screen, shell: Shell): Promise<string> {
  if (screen === "goals") return goalsHtml(await goalsInput(shell));
  if (screen === "tasks") return tasksHtml(await tasksInput(shell));
  return todayHtml(await todayInput(shell));
}

export async function trmnlSetup(request: Request): Promise<Response> {
  const mac = normMac(request.headers.get("id"));
  if (!env.LOR_TRMNL_KEY || !mac || mac !== normMac(env.LOR_TRMNL_MAC)) {
    // The MAC is how the owner learns what to put in LOR_TRMNL_MAC.
    log("warn", "trmnl setup refused", { mac, model: request.headers.get("model") });
    return json(404, { status: 404, api_key: null, friendly_id: null, image_url: null, message: "Not this LoR's device" });
  }
  log("info", "trmnl setup", { mac, fw: request.headers.get("fw-version") });
  // The firmware insists on a setup image and draws its own screen over it; a blank one says nothing.
  const image_url = `${new URL(request.url).origin}/trmnl/image/setup.png`;
  return json(200, { status: 200, api_key: env.LOR_TRMNL_KEY, friendly_id: "LOR", image_url, filename: "setup", message: "Life on Rails" });
}

export async function trmnlDisplay(request: Request): Promise<Response> {
  if (!keyOk(request)) return json(500, { status: 500, error: "Device not found" });
  const h = (k: string) => request.headers.get(k);
  const pct = Number(h("percent-charged"));
  const charging = h("usb-connected") === "true" || h("battery-charging") === "1";
  const screen = nextScreen(shown, h("update-source"), h("special_function"));
  log("info", "trmnl display", { screen, source: h("update-source"), special: h("special_function"), battery: h("percent-charged") ?? h("battery-voltage"), usb: h("usb-connected"), rssi: h("rssi"), fw: h("fw-version") });
  const alerts = Number.isFinite(pct) && pct > 0 && pct < LOW_BATTERY_PCT && !charging ? [`Battery ${Math.round(pct)}%`] : [];
  try {
    const now = new Date();
    const png = await renderFrame(await screenHtml(screen, await shellInput(now, alerts)));
    latest.set(screen, { name: frameName(screen, now), png });
  } catch (err) {
    // The last frame stays up with its own "Updated" time, which is the honest thing to show.
    log("error", "trmnl render failed", { screen, err: errorMessage(err) });
  }
  const frame = latest.get(screen);
  if (!frame) return json(500, { status: 500, error: "No frame yet" });
  shown = screen;
  const origin = new URL(request.url).origin;
  return json(200, {
    status: 0,
    image_url: `${origin}/trmnl/image/${frame.name}.png`,
    filename: frame.name,
    refresh_rate: screen === "today" ? REFRESH_S : TAPPED_S,
    update_firmware: false,
    reset_firmware: false,
  });
}

let blank: Buffer | null = null;

export function trmnlImage(request: Request, name: string): Response {
  if (name === "setup") {
    const w = Math.round(FRAME_W * FRAME_SCALE);
    const h = Math.round(FRAME_H * FRAME_SCALE);
    blank ??= grayPng2(w, h, new Uint8Array(w * h).fill(3));
    return new Response(new Uint8Array(blank), { headers: { "content-type": "image/png", "content-length": String(blank.length) } });
  }
  const frame = [...latest.values()].find((f) => f.name === name);
  if (!keyOk(request) || !frame) return new Response("not found\n", { status: 404 });
  return new Response(new Uint8Array(frame.png), { headers: { "content-type": "image/png", "content-length": String(frame.png.length) } });
}

export async function trmnlLog(request: Request): Promise<Response> {
  if (!keyOk(request)) return json(404, { status: 404 });
  const body = (await request.json().catch(() => null)) as { logs?: unknown[] } | null;
  for (const entry of body?.logs ?? []) log("warn", "trmnl device log", { entry });
  return json(200, { status: 200 });
}
