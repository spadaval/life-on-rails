// Gestures for the launcher lab (android/homecore's wasm page), sent as real touches through
// Chrome's DevTools protocol (Input.dispatchTouchEvent), so Compose's pointer input sees what a
// finger on the phone would give it. scripts/lab/run.mjs binds them to a page and plays a script:
//
//   import { swipe, longPress, drag, fold } from "../gesture.mjs";
//   export default async function () {
//     await swipe("left", 240, { from: "pager" });
//   }
//
// Distances are dp of the phone frame (1 dp = 1 CSS px in the lab), and so are points: a point is
// [x, y] from the screen's top left, an anchor's name ("tile": its centre), or { anchor, at: [fx,
// fy] } for a place inside one by fractions of its size, with an optional [dx, dy] `by` in dp.
// Anchors are what the surface names with Modifier.labAnchor; the page lists them in data-anchors.
// Moves are sent every 8 ms (the Fold's 120 Hz) against a fixed clock, each stamped with the time
// it was due: Chrome takes 30 to 50 ms to hand a headless page one touch, so the moves are sent
// without waiting on each, and their stamps, not their arrival, give Compose the finger's speed.

const STEP_MS = 8;
/** Compose's long-press timeout is 500 ms; a long press holds past it. */
const LONG_PRESS_MS = 700;
const ID = 1;

let page = null;
let cdp = null;
let phone = null; // the phone's screen in page pixels: { x, y, width, height }
const frames = []; // every place the phone in its bezel has been, for the runner's crop
let finger = null; // where the finger is down, in dp, or null
let sent = []; // touches sent and not yet handled
let firstDown = null;
// The gesture's own clock: when the touch now being sent was due. Moves are handled later than
// they were due, so a lift is stamped by this, not by when it is sent, or the finger would seem to
// stop before it lifts and nothing would fling.
let clock = 0;
const marks = [];
const wallOffset = Date.now() - performance.now();

/** Binds the helpers to a page (run.mjs does this). `session` is a CDP session on it. */
export async function use(p, session) {
  page = p;
  cdp = session;
  finger = null;
  firstDown = null;
  marks.length = 0;
  frames.length = 0;
  await measure();
}

/**
 * When each gesture started and ended and the first touch (performance.now() in this process), and
 * every rect the phone in its bezel has filled (a fold or a turn changes it), for the runner.
 */
export function timeline() {
  return { marks: marks.slice(), firstTouch: firstDown, bezels: frames.slice() };
}

async function measure() {
  const m = await page.evaluate(() => {
    const box = (e) => { const r = e.getBoundingClientRect(); return { x: r.x, y: r.y, width: r.width, height: r.height }; };
    const p = document.getElementById("phone");
    return { phone: box(p), bezel: box(p.parentElement) };
  });
  phone = m.phone;
  frames.push(m.bezel);
}

/** Every anchor on screen now: { name: [left, top, right, bottom] } in dp. */
export async function anchors() {
  return page.evaluate(() => JSON.parse(document.body.getAttribute("data-anchors") || "{}"));
}

/** A point as [x, y] in dp, from any form a script may give. */
export async function at(point) {
  if (Array.isArray(point)) return point;
  const spec = typeof point === "string" ? { anchor: point } : point;
  const all = await anchors();
  const r = all[spec.anchor];
  if (!r) throw new Error(`gesture: no anchor "${spec.anchor}" on screen (there are: ${Object.keys(all).join(", ") || "none"})`);
  const [fx, fy] = spec.at ?? [0.5, 0.5];
  const [dx, dy] = spec.by ?? [0, 0];
  return [r[0] + (r[2] - r[0]) * fx + dx, r[1] + (r[3] - r[1]) * fy + dy];
}

const sleep = (ms) => new Promise((r) => setTimeout(r, Math.max(0, ms)));

/** Sends a touch stamped with the time it was due; `settle` waits until every touch sent is handled. */
async function touch(type, xy, due = performance.now(), settle = true) {
  const touchPoints = xy ? [{ x: phone.x + xy[0], y: phone.y + xy[1], id: ID, radiusX: 4, radiusY: 4, force: 1 }] : [];
  sent.push(cdp.send("Input.dispatchTouchEvent", { type, touchPoints, timestamp: (wallOffset + due) / 1000 }));
  if (settle) {
    const all = sent;
    sent = [];
    await Promise.all(all);
  }
}

async function mark(name, body) {
  const start = performance.now();
  try {
    return await body();
  } finally {
    marks.push({ name, start, end: performance.now() });
  }
}

// The low-level three, for a gesture the others don't cover (a fold in the middle of a drag).

/** Puts a finger down. */
export async function down(point) {
  if (finger) throw new Error("gesture: a finger is already down");
  finger = await at(point);
  clock = performance.now();
  firstDown ??= clock;
  await touch("touchStart", finger, clock);
}

/** Moves the finger down to a point in a straight line over `ms`. */
export async function move(point, ms = 200) {
  if (!finger) throw new Error("gesture: move() with no finger down");
  const from = finger, to = await at(point);
  const steps = Math.max(1, Math.round(ms / STEP_MS));
  const t0 = clock;
  for (let i = 1; i <= steps; i++) {
    const due = t0 + i * (ms / steps);
    await sleep(due - performance.now());
    const f = i / steps;
    finger = [from[0] + (to[0] - from[0]) * f, from[1] + (to[1] - from[1]) * f];
    await touch("touchMove", finger, due, i === steps);
  }
  clock = t0 + ms;
}

/** Holds the finger where it is for `ms`. */
export async function hold(ms) {
  clock = Math.max(clock, performance.now() - ms) + ms;
  await sleep(clock - performance.now());
}

/** Lifts the finger. */
export async function up() {
  if (!finger) throw new Error("gesture: up() with no finger down");
  finger = null;
  await touch("touchEnd", null, clock);
}

/** Waits, with no finger down: for an animation to settle before the next gesture. */
export async function wait(ms) {
  await sleep(ms);
}

// The gestures.

export async function tap(point, { hold: ms = 60 } = {}) {
  await mark("tap", async () => { await down(point); await hold(ms); await up(); });
}

export async function doubleTap(point, { gap = 120 } = {}) {
  await mark("doubleTap", async () => {
    await down(point); await hold(50); await up();
    await sleep(gap);
    await down(point); await hold(50); await up();
  });
}

export async function longPress(point, { hold: ms = LONG_PRESS_MS } = {}) {
  await mark("longPress", async () => { await down(point); await hold(ms); await up(); });
}

const DIRECTIONS = { left: [-1, 0], right: [1, 0], up: [0, -1], down: [0, 1] };

function along(from, direction, distance) {
  const d = DIRECTIONS[direction];
  if (!d) throw new Error(`gesture: direction is one of ${Object.keys(DIRECTIONS).join(", ")}, not "${direction}"`);
  return [from[0] + d[0] * distance, from[1] + d[1] * distance];
}

/**
 * A finger drawn `distance` dp in a direction (left, right, up, down) over `duration` ms, from the
 * screen's centre or `from`. `hold` ms still before lifting takes the speed out of it, so nothing flings.
 */
export async function swipe(direction, distance = 200, { duration = 300, from, hold: still = 0 } = {}) {
  await mark("swipe", async () => {
    const start = from ? await at(from) : [phone.width / 2, phone.height / 2];
    await down(start);
    await move(along(start, direction, distance), duration);
    if (still) await hold(still);
    await up();
  });
}

/** A flick at `speed` dp/s, lifted while still moving: `distance` dp (default 120) in that time. */
export async function fling(direction, speed = 3000, { distance = 120, from } = {}) {
  await mark("fling", async () => {
    const start = from ? await at(from) : [phone.width / 2, phone.height / 2];
    await down(start);
    await move(along(start, direction, distance), (distance / speed) * 1000);
    await up();
  });
}

/**
 * A finger down at the path's first point, through each point in turn, and up at the last.
 * `holds` is ms to stay at each point: a number for the first point only (the long press that
 * starts a drag), or an array, one per point. `speed` is dp/s between points.
 */
export async function drag(path, holds = LONG_PRESS_MS, { speed = 600 } = {}) {
  if (path.length < 2) throw new Error("gesture: drag() wants a path of two points or more");
  const pause = Array.isArray(holds) ? holds : [holds];
  await mark("drag", async () => {
    // Every point is read before the finger goes down: an anchor that follows the finger would move on.
    const points = [];
    for (const p of path) points.push(await at(p));
    await down(points[0]);
    await hold(pause[0] ?? 0);
    for (let i = 1; i < points.length; i++) {
      const from = finger, to = points[i];
      await move(to, (Math.hypot(to[0] - from[0], to[1] - from[1]) / speed) * 1000);
      await hold(pause[i] ?? 0);
    }
    await up();
  });
}

async function press(id, until) {
  const before = await page.evaluate(() => document.body.getAttribute("data-drawn"));
  await page.click(`#${id}`);
  if (until) await page.waitForFunction((b) => document.body.getAttribute("data-drawn") === location.hash && location.hash !== b, before);
  await measure();
}

/**
 * Folds or unfolds the phone (the toolbar's Fold), with or without a finger down: the finger keeps
 * its place in dp. `to` is "cover" or "inner"; without it, the other one.
 */
export async function fold(to) {
  await mark("fold", async () => {
    const now = await page.evaluate(() => (document.getElementById("fold").textContent === "Unfold" ? "cover" : "inner"));
    if (to && to === now) return;
    await press("fold", true);
  });
}

/** Turns the phone on its side, or back upright (the toolbar's Rotate). */
export async function rotate() {
  await mark("rotate", () => press("rotate", true));
}

/** The toolbar's Home: the state drawn afresh. */
export async function home() {
  await mark("home", () => press("home", false));
}

/**
 * Back. Plain, it is the phone's back button. `{ predictive: true }` is the edge swipe instead:
 * Compose's back handlers see it start, progress to 1 over `duration` ms and complete, or cancel
 * at `to` (0 to 1) with `cancel: true`. The finger is not drawn: on a phone the system takes it.
 */
export async function back({ predictive = false, duration = 300, edge = "left", to = 1, cancel = false } = {}) {
  await mark("back", async () => {
    if (!predictive) return press("back", false);
    const y = phone.height / 2;
    const x = (p) => (edge === "left" ? p * phone.width * 0.4 : phone.width - p * phone.width * 0.4);
    const send = (detail) => page.evaluate((d) => document.dispatchEvent(new CustomEvent("lab-back", { detail: d })), detail);
    await send(`start ${x(0)} ${y} ${edge}`);
    const steps = Math.max(1, Math.round(duration / 16));
    const t0 = performance.now();
    for (let i = 1; i <= steps; i++) {
      await sleep(t0 + i * (duration / steps) - performance.now());
      const p = (i / steps) * to;
      await send(`progress ${p} ${x(p)} ${y} ${edge}`);
    }
    await send(cancel ? "cancel" : "complete");
  });
}

/** Sets the toolbar's animation speed: 1, 0.25 or 0. */
export async function speed(s) {
  for (let i = 0; i < 3; i++) {
    if (Number(await page.getAttribute("#speed", "data-speed")) === s) return;
    await page.click("#speed");
  }
  throw new Error(`gesture: speed is 1, 0.25 or 0, not ${s}`);
}
