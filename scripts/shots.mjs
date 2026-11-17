#!/usr/bin/env node
// Screenshots of every screen and state a catalogue names, at each of its widths, and a
// gallery that flips each one between this run and the one before. A design tool, not a
// check: nothing gates on it (lor-nde9).
//
//   node scripts/shots.mjs                              # shots/dashboard.json against its base (:3858, scripts/ui-dev.sh)
//   node scripts/shots.mjs --base http://127.0.0.1:3857 # or the built test server (verify-lor up)
//   node scripts/shots.mjs --only "Tasks"               # shots whose name or screen contains this (repeatable)
//   node scripts/shots.mjs --list                       # print the catalogue's names and links
//   node scripts/shots.mjs --catalogue shots/lab.json --out <dir>
//
// A catalogue is JSON: { name, base, widths, height?, scale?, timezone?, wait?, shots: [{ name,
// screen, link, capture?: "page" | "viewport", wait? }] }. `wait` is { selector?, function?, ms? }:
// a selector to be visible, a JavaScript expression to turn true (a canvas that has drawn), and a
// pause after both; a shot's own `wait` is used with the catalogue's. A shot is just its link, opened
// and captured: nothing is clicked or typed, so the same link opened by hand draws the same state.
//
// Writes <out>/<shot>@<width>.png, moving the one there to <out>/previous/, <out>/index.html, and
// <out>/.lor-stamp with this checkout's commit and the time, for the lab's index.
// <out> defaults to ~/.local/state/lor-shots/<catalogue name>.
import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright-core";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const ENV_FILE = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");
const REAL_PORTS = [3847, 3848, 3849];
const PARALLEL = 6;

function fail(code, msg) {
  console.error(`shots: ${msg}`);
  process.exit(code);
}

function args(argv) {
  const out = { only: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    const val = () => argv[++i] ?? fail(2, `${a} wants a value`);
    if (a === "--list") out.list = true;
    else if (a === "--only") out.only.push(val());
    else if (a === "--out") out.out = val();
    else if (a === "--catalogue" || a === "--catalog") out.catalogue = val();
    else if (a === "--base") out.base = val();
    else if (a === "--help" || a === "-h") out.help = true;
    else fail(2, `unknown argument ${a}; see --help`);
  }
  return out;
}

function readEnvFile() {
  const env = {};
  if (!existsSync(ENV_FILE)) return env;
  for (const line of readFileSync(ENV_FILE, "utf8").split("\n")) {
    const m = /^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/.exec(line);
    if (m && !line.trim().startsWith("#")) env[m[1]] = m[2].replace(/^(['"])(.*)\1$/, "$2");
  }
  return env;
}

/** This checkout's commit, marked when it has uncommitted changes. */
export function checkoutCommit(cwd = ROOT) {
  const git = (...a) => execFileSync("git", ["-C", cwd, ...a], { encoding: "utf8" }).trim();
  try {
    const sha = git("rev-parse", "--short", "HEAD");
    return git("status", "--porcelain") ? `${sha} (uncommitted changes)` : sha;
  } catch {
    return "unknown";
  }
}

/** A file name for a shot: its name, lower case, with anything but letters and digits as dashes. */
export const slug = (name) => name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");

/**
 * lor-web's ports hold the owner's real data, where at= and data= do nothing. A catalogue may
 * point there only for /lab/, which is static files (lor-zwfd).
 */
export function refuseRealData(base, shots, env) {
  const real = new Set([...REAL_PORTS, ...["LOR_PORT", "LOR_SERVE_PORT", "LOR_TRMNL_PORT"].map((k) => Number(env[k])).filter(Boolean)]);
  const url = new URL(base);
  const port = Number(url.port || (url.protocol === "https:" ? 443 : 80));
  if (!real.has(port)) return null;
  const off = shots.filter((s) => !new URL(s.link, base).pathname.startsWith("/lab/"));
  return off.length ? `${base} is lor-web, with the owner's real data, and ${off.length} link(s) are outside /lab/ (first: ${off[0].link}). Use the dev server on :3858 (scripts/ui-dev.sh) or the test server on :3857.` : null;
}

/** Shots that change the shared test database (data=) or its clock (at=) differently run apart. */
export function groupsOf(shots, base) {
  const groups = new Map();
  for (const s of shots) {
    const q = new URL(s.link, base).searchParams;
    const key = `data=${q.get("data") ?? ""}&at=${q.get("at") ?? ""}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(s);
  }
  return [...groups.values()];
}

async function pool(items, n, fn) {
  let i = 0;
  await Promise.all(Array.from({ length: Math.min(n, items.length) }, async () => {
    while (i < items.length) await fn(items[i++]);
  }));
}

const usage = `node scripts/shots.mjs [--catalogue shots/dashboard.json] [--base <url>] [--only <text>]... [--out <dir>] [--list]`;

async function main() {
  const opt = args(process.argv.slice(2));
  if (opt.help) return console.log(usage);
  const file = path.resolve(opt.catalogue ?? path.join(ROOT, "shots/dashboard.json"));
  const cat = JSON.parse(readFileSync(file, "utf8"));
  const base = (opt.base ?? cat.base ?? fail(2, `${file} names no base; pass --base`)).replace(/\/$/, "");
  const all = cat.shots.map((s) => ({ ...s, screen: s.screen ?? "Other", file: slug(s.name) }));
  const dupes = all.filter((s, i) => all.findIndex((t) => t.file === s.file) !== i);
  if (dupes.length) fail(2, `two shots share the file name ${dupes[0].file}`);

  if (opt.list) {
    for (const s of all) console.log(`${s.name}\t${base}${s.link}`);
    return;
  }
  const want = opt.only.map((o) => o.toLowerCase());
  const shots = want.length ? all.filter((s) => want.some((w) => s.name.toLowerCase().includes(w) || s.screen.toLowerCase().includes(w))) : all;
  if (!shots.length) fail(2, `no shot matches ${opt.only.join(", ")}; see --list`);

  const env = readEnvFile();
  const refusal = refuseRealData(base, shots, env);
  if (refusal) fail(3, `refused: ${refusal}`);
  try {
    await fetch(base, { signal: AbortSignal.timeout(15000) });
  } catch (err) {
    fail(1, `${base} does not answer (${err.cause?.code ?? err.message}). Start scripts/ui-dev.sh, or verify-lor up for :3857.`);
  }

  const out = path.resolve(opt.out ?? path.join(homedir(), ".local/state/lor-shots", cat.name ?? path.basename(file, ".json")));
  mkdirSync(path.join(out, "previous"), { recursive: true });
  const widths = cat.widths ?? [1500, 1100, 412];
  const started = Date.now();
  const browser = await chromium.launch({ channel: "chrome", headless: true });
  const contexts = new Map();
  for (const w of widths) {
    contexts.set(w, await browser.newContext({
      viewport: { width: w, height: cat.height ?? 950 },
      deviceScaleFactor: cat.scale ?? 1,
      timezoneId: cat.timezone ?? env.LOR_TZ ?? "America/New_York",
      reducedMotion: "reduce",
    }));
  }
  const problems = [];
  const take = async ({ shot, width }) => {
    const page = await contexts.get(width).newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    try {
      const res = await page.goto(base + shot.link, { waitUntil: "networkidle", timeout: 30000 });
      if (res && !res.ok()) errors.push(`HTTP ${res.status()}`);
      for (const w of [cat.wait, shot.wait].filter(Boolean)) {
        if (w.selector) await page.locator(w.selector).first().waitFor({ state: "visible", timeout: 15000 });
        if (w.function) await page.waitForFunction(w.function, null, { timeout: 15000 });
        if (w.ms) await page.waitForTimeout(w.ms);
      }
      await page.evaluate(() => document.fonts.ready);
      const name = `${shot.file}@${width}.png`;
      const current = path.join(out, name);
      if (existsSync(current)) renameSync(current, path.join(out, "previous", name));
      await page.screenshot({ path: current, fullPage: shot.capture !== "viewport", animations: "disabled", caret: "hide" });
    } catch (err) {
      errors.push(err.message.split("\n")[0]);
    } finally {
      await page.close();
    }
    if (errors.length) problems.push({ shot: shot.name, width, errors });
  };

  // One group at a time, so a variant or clock one shot asks for is not drawn into another's.
  for (const group of groupsOf(shots, base)) {
    await pool(group.flatMap((shot) => widths.map((width) => ({ shot, width }))), PARALLEL, take);
  }
  await browser.close();
  // A variant stays applied in the shared test database: leave it on the base, so a link without data= reads the base.
  if (shots.some((s) => !["", "base"].includes(new URL(s.link, base).searchParams.get("data") ?? ""))) {
    await fetch(`${base}/health?data=base`).catch(() => problems.push({ shot: "(back to data=base)", width: 0, errors: ["could not reach the server"] }));
  }

  writeFileSync(path.join(out, "index.html"), gallery({ cat, base, all, widths, out }));
  // What made this gallery, for the lab's index (scripts/lab/publish.sh).
  writeFileSync(path.join(out, ".lor-stamp"), `commit=${checkoutCommit()}\ntime=${new Date().toISOString()}\n`);
  const seconds = ((Date.now() - started) / 1000).toFixed(1);
  console.log(JSON.stringify({ shots: shots.length, widths, images: shots.length * widths.length, seconds: Number(seconds), out, gallery: path.join(out, "index.html"), problems }, null, 2));
  if (problems.length) process.exitCode = 1;
}

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

/** The gallery: every shot in the catalogue that has an image, by screen, its widths side by side. Relative paths only, no requests. */
function gallery({ cat, base, all, widths, out }) {
  const stamp = (f) => (existsSync(path.join(out, f)) ? f : null);
  const screens = [...new Set(all.map((s) => s.screen))];
  const sections = screens.map((screen) => {
    const rows = all.filter((s) => s.screen === screen).map((s) => {
      const cells = widths.map((w) => {
        const f = `${s.file}@${w}.png`;
        if (!stamp(f)) return `<figure class="none"><figcaption>${w}</figcaption><p>not shot yet</p></figure>`;
        const prev = stamp(`previous/${f}`);
        return `<figure style="flex:${w}"><figcaption>${w}${prev ? ` <span class="which">now</span>` : ""}</figcaption><a href="${esc(f)}"><img loading="lazy" src="${esc(f)}" data-now="${esc(f)}"${prev ? ` data-before="${esc(prev)}"` : ""} alt="${esc(s.name)} at ${w} px"></a></figure>`;
      }).join("");
      return `<article id="${esc(s.file)}"><h3>${esc(s.name)} <a class="link" href="${esc(base + s.link)}">${esc(s.link)}</a></h3><div class="row">${cells}</div></article>`;
    }).join("");
    return `<section id="${esc(slug(screen))}"><h2>${esc(screen)}</h2>${rows}</section>`;
  }).join("");
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(cat.name ?? "Shots")} shots</title>
<style>
:root { color-scheme: dark; --bg: #0c0d12; --txt: #e8e6e1; --mute: #9a978f; --line: #2a2b33; --or: #f08a2c; }
body { margin: 0; padding: 16px 24px 48px; background: var(--bg); color: var(--txt); font: 14px/1.4 system-ui, sans-serif; }
header { position: sticky; top: 0; z-index: 1; background: var(--bg); padding: 8px 0; border-bottom: 1px solid var(--line); display: flex; gap: 16px; align-items: baseline; flex-wrap: wrap; }
header h1 { font-size: 18px; margin: 0; } nav a { color: var(--mute); margin-right: 12px; }
button { background: none; color: var(--txt); border: 1px solid var(--line); padding: 4px 10px; cursor: pointer; }
button[aria-pressed="true"] { border-color: var(--or); color: var(--or); }
h2 { margin: 28px 0 8px; font-size: 16px; color: var(--or); } h3 { font-size: 14px; font-weight: 600; margin: 18px 0 6px; }
.link { color: var(--mute); font-weight: 400; margin-left: 8px; font-size: 12px; word-break: break-all; }
.row { display: flex; gap: 12px; align-items: flex-start; }
figure { margin: 0; min-width: 0; } figure.none { flex: 0 0 120px; color: var(--mute); }
figcaption { color: var(--mute); font-size: 12px; margin-bottom: 4px; } .which { color: var(--or); }
img { display: block; width: 100%; height: auto; border: 1px solid var(--line); }
@media (max-width: 700px) { body { padding: 8px 16px 32px; } .row { flex-direction: column; } figure { width: 100%; } }
</style></head><body>
<header><h1>${esc(cat.name ?? "Shots")}</h1><span class="link">${esc(base)} · ${new Date().toISOString().slice(0, 16).replace("T", " ")} UTC</span>
<button id="flip" aria-pressed="false" title="Show the previous run (b)">Before</button>
<nav>${screens.map((s) => `<a href="#${esc(slug(s))}">${esc(s)}</a>`).join("")}</nav></header>
${sections}
<script>
const flip = document.getElementById("flip");
const show = (before) => {
  flip.setAttribute("aria-pressed", String(before));
  for (const img of document.querySelectorAll("img[data-before]")) {
    img.src = before ? img.dataset.before : img.dataset.now;
    img.closest("figure").querySelector(".which").textContent = before ? "before" : "now";
  }
};
flip.onclick = () => show(flip.getAttribute("aria-pressed") !== "true");
addEventListener("keydown", (e) => { if (e.key === "b") show(flip.getAttribute("aria-pressed") !== "true"); });
</script></body></html>
`;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) await main();
