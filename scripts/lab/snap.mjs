#!/usr/bin/env node
// Screenshots lab links, one page at a time, cropped to the phone: a quick look at a version's
// states while building it, without a gallery.
//
//   node scripts/lab/snap.mjs --out <dir> <hash>...        (each hash as the lab writes it, without #)
//       [--base http://127.0.0.1:3860] [--wait <ms>]
//
// Each shot is <dir>/<n>-<hash with / as _>.png, taken once the page has drawn the hash
// (body[data-drawn]) and [--wait] ms more for motion to settle. One page draws at a time.
import { mkdirSync } from "node:fs";
import path from "node:path";
import { chromium } from "playwright-core";

const argv = process.argv.slice(2);
let out = null, base = "http://127.0.0.1:3860", wait = 600;
const hashes = [];
for (let i = 0; i < argv.length; i++) {
  if (argv[i] === "--out") out = argv[++i];
  else if (argv[i] === "--base") base = argv[++i];
  else if (argv[i] === "--wait") wait = Number(argv[++i]);
  else hashes.push(argv[i]);
}
if (!out || hashes.length === 0) { console.error("snap: --out <dir> and at least one hash"); process.exit(2); }
mkdirSync(out, { recursive: true });
const browser = await chromium.launch({ channel: "chrome", headless: true });
const page = await browser.newPage({ viewport: { width: 1240, height: 1290 } });
const errors = [];
page.on("pageerror", (e) => errors.push(e.message));
let n = 0;
for (const h of hashes) {
  n++;
  const hash = "#" + h;
  await page.goto(base + "/" + hash);
  // The page writes the hash as the address bar has it (encoded), so compare in the page.
  await page.waitForFunction(() => document.body.getAttribute("data-drawn") === location.hash, null, { timeout: 120000 });
  await page.waitForTimeout(wait);
  const file = path.join(out, `${String(n).padStart(2, "0")}-${h.replace(/[^a-zA-Z0-9-]+/g, "_").slice(0, 80)}.png`);
  await page.locator("#phone").screenshot({ path: file });
  console.log(file);
}
if (errors.length) console.log("page errors:\n" + errors.join("\n"));
await browser.close();
