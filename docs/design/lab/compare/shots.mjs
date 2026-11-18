#!/usr/bin/env node
// Remakes the comparison page's screenshots in shots/ from the galleries (lor-glev):
//
//   node docs/design/lab/compare/shots.mjs [--from ~/.local/state/lor-shots] [--lab http://127.0.0.1:3847/lab/]
//
// Four per version: dashboard Today at 1500x950 and at 412x892, the first screen as the
// published prototype draws it (the gallery's full-page captures can repeat a band, lor-jkcf),
// and the phone's home and agent page on the cover screen from the phone gallery, cropped to
// the device. WebP, so the page stays small enough to open on the phone. Run it where
// playwright-core resolves (the main checkout's node_modules).
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(process.env.LOR_MODULES ?? path.join(here, "../../../../package.json"));
const { chromium } = require("playwright-core");

const arg = (name, fallback) => {
  const i = process.argv.indexOf(name);
  return i > 0 ? process.argv[i + 1] : fallback;
};
const from = arg("--from", path.join(os.homedir(), ".local/state/lor-shots"));
const lab = arg("--lab", "http://127.0.0.1:3847/lab/");
const out = path.join(here, "shots");
mkdirSync(out, { recursive: true });

const VERSIONS = ["current", "queue", "timeline", "panes", "context"];
const today = "today-in-the-afternoon-a-timer-running-two-flags";
// current's phone agent page predates the version names: it is the "agent-page" catalogue.
const agent = (v) => (v === "current" ? "agent-page-approval-question-failed-turn-two-flags-cover" : `${v}-agent-waiting-cover`);
const jobs = VERSIONS.flatMap((v) => [
  { url: `dashboard/#/${v}/?still=1&shell=0`, dst: `${v}-desk.webp`, crop: { w: 1500, h: 950 }, scale: 1 },
  { url: `dashboard/#/${v}/?still=1&shell=0`, dst: `${v}-narrow.webp`, crop: { w: 412, h: 892 }, scale: 1 },
  { src: `lab/${v}-demo-cover@1000.png`, dst: `${v}-home.webp`, crop: "device", scale: 0.8 },
  { src: `lab/${agent(v)}@1000.png`, dst: `${v}-agent.webp`, crop: "device", scale: 0.8 },
]);

const browser = await chromium.launch({ channel: "chrome" });
const page = await browser.newPage();
await page.setContent("<canvas></canvas>");
for (const job of jobs) {
  let png;
  if (job.url) {
    const tab = await browser.newPage({ viewport: { width: job.crop.w, height: job.crop.h } });
    await tab.goto(new URL(job.url, lab).href, { waitUntil: "networkidle" });
    await tab.evaluate(() => document.fonts.ready);
    png = (await tab.screenshot()).toString("base64");
    await tab.close();
  } else png = readFileSync(path.join(from, job.src)).toString("base64");
  const webp = await page.evaluate(async ({ png, crop, scale }) => {
    const img = new Image();
    img.src = `data:image/png;base64,${png}`;
    await img.decode();
    let box = { x: 0, y: 0, w: Math.min(crop.w ?? img.width, img.width), h: Math.min(crop.h ?? img.height, img.height) };
    if (crop === "device") {
      // The lab's toolbar is the top 56 px; the device is whatever differs from the ground below it.
      const c = new OffscreenCanvas(img.width, img.height);
      const g = c.getContext("2d");
      g.drawImage(img, 0, 0);
      const d = g.getImageData(0, 0, img.width, img.height).data;
      const at = (x, y) => (y * img.width + x) * 4;
      const bg = d.slice(at(2, img.height - 2), at(2, img.height - 2) + 3);
      let x0 = img.width, y0 = img.height, x1 = 0, y1 = 0;
      for (let y = 56; y < img.height; y++) for (let x = 0; x < img.width; x++) {
        const p = at(x, y);
        if (Math.abs(d[p] - bg[0]) + Math.abs(d[p + 1] - bg[1]) + Math.abs(d[p + 2] - bg[2]) > 24) {
          if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
        }
      }
      box = { x: x0, y: y0, w: x1 - x0 + 1, h: y1 - y0 + 1 };
    }
    const c = document.querySelector("canvas");
    c.width = Math.round(box.w * scale);
    c.height = Math.round(box.h * scale);
    const g = c.getContext("2d");
    g.imageSmoothingQuality = "high";
    g.drawImage(img, box.x, box.y, box.w, box.h, 0, 0, c.width, c.height);
    return { url: c.toDataURL("image/webp", 0.82), w: c.width, h: c.height };
  }, { png, crop: job.crop, scale: job.scale });
  writeFileSync(path.join(out, job.dst), Buffer.from(webp.url.split(",")[1], "base64"));
  console.log(`${job.dst} ${webp.w}x${webp.h}`);
}
await browser.close();
