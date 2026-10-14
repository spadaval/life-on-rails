#!/usr/bin/env node
// A scripted browser pass over the UI, against a server on the TEST database:
//   DATABASE_URL=postgres:///life_on_rails_test?host=/var/run/postgresql LOR_PORT=3857 LOR_OWNER_LOGIN= node scripts/serve.mjs
//   node scripts/ui-check.mjs <flow> [outDir]
// Uses the system Chrome through playwright-core. Screenshots go to outDir.
import { mkdirSync } from "node:fs";
import path from "node:path";
import { chromium } from "playwright-core";

const BASE = process.env.LOR_UI_BASE ?? "http://127.0.0.1:3857";
const flow = process.argv[2] ?? "graph";
const out = process.argv[3] ?? "/tmp/lor-ui";
mkdirSync(out, { recursive: true });

const browser = await chromium.launch({ channel: "chrome", headless: true });
const context = await browser.newContext({ viewport: { width: 412, height: 915 }, deviceScaleFactor: 2, timezoneId: "America/Los_Angeles" });
const page = await context.newPage();
const errors = [];
page.on("pageerror", (e) => errors.push(`pageerror: ${e.message}`));
page.on("console", (m) => m.type() === "error" && errors.push(`console: ${m.text()}`));
const shot = async (name) => {
  await page.screenshot({ path: path.join(out, `${name}.png`), fullPage: true });
  console.log(`shot ${name}`);
};
const settle = () => page.waitForLoadState("networkidle");

const flows = {
  async graph() {
    await page.goto(`${BASE}/graph`);
    await settle();
    // A goal with an hour target, a habit under it, a task under it.
    await page.selectOption('select[aria-label="Type"]', "goal");
    await page.fill('input[aria-label="Title"]', "Ship Life on Rails");
    await page.fill('input[placeholder="hours / week"]', "10");
    await page.click('button:has-text("Add")');
    await page.waitForSelector('li:has-text("Ship Life on Rails")');
    await page.selectOption('select[aria-label="Type"]', "habit");
    await page.fill('input[aria-label="Title"]', "Daily deep work");
    await page.selectOption('select[aria-label="Parent"]', { label: "goal: Ship Life on Rails" });
    await page.click('button:has-text("Add")');
    await page.waitForSelector('li:has-text("Daily deep work")');
    await page.selectOption('select[aria-label="Type"]', "task");
    await page.fill('input[aria-label="Title"]', "Write the fill sync");
    await page.selectOption('select[aria-label="Parent"]', { label: "goal: Ship Life on Rails" });
    await page.click('button:has-text("Add")');
    await page.waitForSelector('li:has-text("Write the fill sync")');
    await settle();
    await shot("graph-1-added");
    await page.click('button:has-text("Did it")');
    await page.waitForSelector("text=Logged");
    await page.click('li >> button:has-text("Daily deep work")');
    await page.click('button:has-text("Explain score")');
    await page.waitForSelector("pre");
    await shot("graph-2-habit-explained");
    await page.click('li:has-text("Write the fill sync") >> button:has-text("Done")');
    await settle();
    await page.click('button:has-text("Show done and later")');
    await page.waitForSelector('li:has-text("Write the fill sync")');
    await shot("graph-3-task-done");
    console.log("goal chip:", await page.locator('li:has-text("Ship Life on Rails")').first().innerText());
  },
};

try {
  if (!flows[flow]) throw new Error(`unknown flow ${flow}; have: ${Object.keys(flows).join(", ")}`);
  await flows[flow]();
} catch (err) {
  await shot(`${flow}-FAILED`).catch(() => {});
  console.error(`FLOW FAILED: ${err.message}`);
  process.exitCode = 1;
} finally {
  if (errors.length) console.log("browser errors:\n" + errors.join("\n"));
  await browser.close();
}
