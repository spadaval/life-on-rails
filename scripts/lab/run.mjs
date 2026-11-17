#!/usr/bin/env node
// Plays a gesture script on the launcher lab and writes what happened to look at:
//
//   node scripts/lab/run.mjs <script> --version <v> --state <s> --out <dir>
//       [--screen cover|inner] [--wallpaper dark|pale|mixed] [--landscape] [--speed 1|0.25|0]
//       [--base http://127.0.0.1:3860]
//
// <dir> gets recording.webm (the whole run), strip.png (the screen every 50 ms from the first
// touch until it settles, each tile stamped with its time), final.png (the screen at the end)
// and run.json (the link, the gestures' times, the frames Chrome sent, the anchors at the end).
// A script is an ES module whose default export is an async function using scripts/lab/gesture.mjs;
// it may export `link` ({ screen, wallpaper, landscape }) for the screen it is written for, which
// the flags override. scripts/lab/gestures/ has one per helper, for the `gestures` version.
// Serve the lab first: android/web-dev.sh.
import { spawn } from "node:child_process";
import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { chromium } from "playwright-core";
import * as gesture from "./gesture.mjs";

const STRIP_MS = 50;
const VIDEO_FPS = 50;
const SETTLE_QUIET_MS = 400; // no new frame for this long: the screen has settled
const SETTLE_MAX_MS = 3000;
const STRIP_SCALE = 0.3;
// Room for the largest screen on its side, with the toolbar, so no touch lands off the viewport.
const VIEWPORT = { width: 1240, height: 1290 };
const FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf";

function fail(msg) {
  console.error(`run: ${msg}`);
  process.exit(2);
}

function args(argv) {
  const out = { link: {} };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    const val = () => argv[++i] ?? fail(`${a} wants a value`);
    if (a === "--version") out.version = val();
    else if (a === "--state") out.state = val();
    else if (a === "--out") out.out = val();
    else if (a === "--base") out.base = val();
    else if (a === "--screen") out.link.screen = val();
    else if (a === "--wallpaper") out.link.wallpaper = val();
    else if (a === "--landscape") out.link.landscape = true;
    else if (a === "--speed") out.speed = Number(val());
    else if (a === "--help" || a === "-h") out.help = true;
    else if (a.startsWith("--")) fail(`unknown argument ${a}; see --help`);
    else if (out.script) fail(`one script at a time (have ${out.script}, got ${a})`);
    else out.script = a;
  }
  return out;
}

const opts = args(process.argv.slice(2));
if (opts.help) {
  console.log(
    "node scripts/lab/run.mjs <script> --version <v> --state <s> --out <dir> [--screen cover|inner] [--wallpaper dark|pale|mixed] [--landscape] [--speed 1|0.25|0] [--base url]",
  );
  process.exit(0);
}
if (!opts.script || !opts.version || !opts.state || !opts.out) fail("wants <script> --version <v> --state <s> --out <dir>; see --help");

const t0 = performance.now();
const script = await import(pathToFileURL(path.resolve(opts.script)).href);
if (typeof script.default !== "function") fail(`${opts.script} has no default export to play`);
const link = { screen: "cover", wallpaper: "dark", landscape: false, ...(script.link ?? {}), ...opts.link };
const hash = [opts.version, opts.state, link.screen, link.wallpaper, ...(link.landscape ? ["landscape"] : [])].join("/");
const base = opts.base ?? "http://127.0.0.1:3860";
const out = path.resolve(opts.out);
mkdirSync(out, { recursive: true });

const browser = await chromium.launch({ channel: "chrome", headless: true });
let result;
try {
  result = await play();
} finally {
  await browser.close();
}
const total = (performance.now() - t0) / 1000;
writeFileSync(path.join(out, "run.json"), JSON.stringify({ ...result, seconds: Number(total.toFixed(2)) }, null, 2) + "\n");
console.log(
  `run: ${path.basename(opts.script)} on #${hash}: ${result.frames.sent} frames (${result.frames.fps} a second while it ran; Chrome sends one only on a change), ` +
    `${result.strip.tiles} in the strip, ${total.toFixed(1)} s in all; ${out}/{recording.webm,strip.png,final.png,run.json}`,
);

async function play() {
  const context = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1, hasTouch: true });
  const page = await context.newPage();
  const errors = [];
  page.on("pageerror", (e) => errors.push(String(e)));
  await page.goto(`${base}/#${hash}`);
  await page.waitForFunction((h) => document.body.getAttribute("data-drawn") === h, `#${hash}`, { timeout: 30000 });
  const problem = await page.evaluate(() => (document.getElementById("problem").hidden ? null : document.getElementById("problem").textContent));
  if (problem) throw new Error(`the lab says: ${problem}`);
  const cdp = await context.newCDPSession(page);
  await gesture.use(page, cdp);
  if (opts.speed !== undefined) await gesture.speed(opts.speed);

  // Every frame Chrome draws, with its time; each is acknowledged at once so none is held back.
  const frames = [];
  const wallOffset = Date.now() - performance.now();
  let lastFrameAt = performance.now();
  cdp.on("Page.screencastFrame", ({ data, metadata, sessionId }) => {
    // Placed by when Chrome drew it (its own clock, in s since the epoch), not when it arrived here.
    frames.push({ at: metadata.timestamp * 1000 - wallOffset, jpeg: Buffer.from(data, "base64") });
    lastFrameAt = performance.now();
    cdp.send("Page.screencastFrameAck", { sessionId }).catch(() => {});
  });
  const phoneRect = () => page.evaluate(() => {
    const r = document.getElementById("phone").parentElement.getBoundingClientRect();
    return { x: r.x, y: r.y, width: r.width, height: r.height };
  });
  const before = await phoneRect();
  await cdp.send("Page.startScreencast", { format: "jpeg", quality: 85, maxWidth: 1240, maxHeight: 1290, everyNthFrame: 1 });
  const begin = performance.now();
  await script.default({ page, gesture });
  const played = performance.now();
  while (performance.now() - lastFrameAt < SETTLE_QUIET_MS && performance.now() - played < SETTLE_MAX_MS) await new Promise((r) => setTimeout(r, 50));
  const end = performance.now();
  await cdp.send("Page.stopScreencast");

  const { marks, firstTouch: touched } = gesture.timeline();
  const firstTouch = Math.min(touched ?? Infinity, marks[0]?.start ?? Infinity, end);
  // The phone as it is now: a fold or a turn moves it.
  const rect = await page.evaluate(() => {
    const r = document.getElementById("phone").getBoundingClientRect();
    return { x: r.x, y: r.y, width: r.width, height: r.height };
  });
  await page.screenshot({ path: path.join(out, "final.png"), clip: rect });
  const anchors = await gesture.anchors();

  if (!frames.length) throw new Error("Chrome sent no frames");
  frames.sort((a, b) => a.at - b.at);
  // The screen as it stood at time t: the latest frame drawn at or before it.
  const frameAt = (t) => {
    let f = frames[0];
    for (const g of frames) if (g.at <= t) f = g; else break;
    return f;
  };
  // The recording and the strip show the phone in its bezel; across a fold or a turn, both of its shapes.
  const after = await phoneRect();
  const x0 = Math.floor(Math.min(before.x, after.x)), y0 = Math.floor(Math.min(before.y, after.y));
  const x1 = Math.ceil(Math.max(before.x + before.width, after.x + after.width)), y1 = Math.ceil(Math.max(before.y + before.height, after.y + after.height));
  // A frame Chrome sends can come smaller than the viewport now and then (a run with a fold and
  // two turns did), which ffmpeg's crop fails on, so every frame is brought to the viewport's size first.
  const crop = `scale=${VIEWPORT.width}:${VIEWPORT.height},crop=${2 * Math.floor((x1 - x0) / 2)}:${2 * Math.floor((y1 - y0) / 2)}:${x0}:${y0}`;
  const videoTimes = [];
  for (let t = begin; t <= end; t += 1000 / VIDEO_FPS) videoTimes.push(t);
  const stripTimes = [];
  for (let t = firstTouch; t <= end; t += STRIP_MS) stripTimes.push(t);
  const cols = Math.min(10, stripTimes.length);
  const rows = Math.ceil(stripTimes.length / cols);
  const stamp = existsSync(FONT) ? `,drawtext=fontfile=${FONT}:text='%{eif\\:n*${STRIP_MS}\\:d} ms':x=6:y=6:fontsize=16:fontcolor=white:box=1:boxcolor=black@0.6` : "";
  await Promise.all([
    encode(videoTimes.map(frameAt), VIDEO_FPS, ["-vf", crop, "-c:v", "libvpx-vp9", "-deadline", "realtime", "-cpu-used", "8", "-b:v", "2M", "-an", path.join(out, "recording.webm")]),
    encode(stripTimes.map(frameAt), 1000 / STRIP_MS, [
      "-vf", `${crop},scale=iw*${STRIP_SCALE}:-2${stamp},tile=${cols}x${rows}:padding=4:margin=4:color=0x07080b`,
      "-frames:v", "1", "-update", "1", path.join(out, "strip.png"),
    ]),
  ]);

  const span = (end - begin) / 1000;
  return {
    script: opts.script,
    link: `${base}/#${hash}`,
    gestures: marks.map((m) => ({ name: m.name, from_ms: Math.round(m.start - firstTouch), to_ms: Math.round(m.end - firstTouch) })),
    // Chrome sends a frame only when the screen changes, so a still screen sends few.
    frames: { sent: frames.length, fps: Math.round(frames.length / span), longest_gap_ms: Math.round(Math.max(0, ...frames.slice(1).map((f, i) => f.at - frames[i].at))), seconds: Number(span.toFixed(2)) },
    strip: { tiles: stripTimes.length, every_ms: STRIP_MS, from: "the first touch", to: "settled" },
    anchors,
    errors,
  };
}

/** Pipes JPEG frames to ffmpeg at a fixed rate. */
function encode(jpegs, fps, outArgs) {
  return new Promise((resolve, reject) => {
    const ff = spawn("ffmpeg", ["-hide_banner", "-loglevel", "error", "-y", "-f", "image2pipe", "-c:v", "mjpeg", "-framerate", String(fps), "-i", "-", ...outArgs], {
      stdio: ["pipe", "inherit", "inherit"],
    });
    ff.on("error", reject);
    ff.on("exit", (code) => (code === 0 ? resolve() : reject(new Error(`ffmpeg exited ${code}`))));
    ff.stdin.on("error", () => {});
    (async () => {
      for (const f of jpegs) if (!ff.stdin.write(f.jpeg)) await new Promise((r) => ff.stdin.once("drain", r));
      ff.stdin.end();
    })();
  });
}
