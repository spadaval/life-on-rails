#!/usr/bin/env node
// A scripted browser pass over the UI, against a server on the TEST database:
//   DATABASE_URL=postgres:///life_on_rails_test?host=/var/run/postgresql LOR_PORT=3857 LOR_OWNER_LOGIN= node scripts/serve.mjs
//   node scripts/ui-check.mjs <flow> [outDir]
// Uses the system Chrome through playwright-core. Screenshots go to outDir.
import { execFileSync } from "node:child_process";
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
  console.log(`shot ${name}${errors.length ? ` (browser errors so far: ${errors.length})` : ""}`);
};
const settle = () => page.waitForLoadState("networkidle");

/** Run SQL against the test database. */
const psql = (sql) => execFileSync("psql", ["-d", "life_on_rails_test", "-v", "ON_ERROR_STOP=1", "-Atqc", sql], { encoding: "utf8" });
const LOCAL_MIDNIGHT = `(date_trunc('day', now() AT TIME ZONE 'America/Los_Angeles') AT TIME ZONE 'America/Los_Angeles')`;
const at = (hours) => `${LOCAL_MIDNIGHT} + interval '${hours} hours'`;

const flows = {
  async today() {
    psql(`
      DELETE FROM commitment; DELETE FROM time_block; DELETE FROM google_event; DELETE FROM attribution_rule;
      DELETE FROM intent_link; DELETE FROM intent WHERE NOT system;
      INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING;
      INSERT INTO google_event (calendar_id, event_id, title, t_start, t_end, all_day) VALUES
        ('cal', 'standup', 'Standup', ${at(9)}, ${at(9.5)}, false),
        ('cal', 'hold', 'Afternoon hold', ${at(13)}, ${at(17)}, false),
        ('cal', 'dentist', 'Dentist', ${at(14)}, ${at(15)}, false),
        ('cal', 'dinner', 'Dinner with Priya', ${at(23)}, ${at(23.75)}, false),
        ('cal', 'visit', 'Priya in town', ${at(0)}, ${at(24)}, true);
      INSERT INTO google_event (calendar_id, event_id, title, t_start, t_end, event_type) VALUES ('cal', 'focus', 'Focus time', ${at(10)}, ${at(12)}, 'focusTime');
      INSERT INTO intent (id, type, title, weight, weekly_hour_target) VALUES (gen_random_uuid(), 'goal', 'Ship Life on Rails', 3, 10);
      INSERT INTO intent (id, type, title) VALUES (gen_random_uuid(), 'project', 'Health admin');
      INSERT INTO intent (id, type, title) VALUES (gen_random_uuid(), 'metric', 'Energy');
    `);
    await page.goto(`${BASE}/`);
    await settle();
    await page.click('button:has-text("Just start")');
    await page.waitForSelector("text=Running since");
    await shot("today-1-running");
    await page.click('button:has-text("Stop")');
    await page.waitForSelector("text=Start a timer");
    await settle();
    await shot("today-2-timeline");
    console.log("confirm card:", (await page.locator("h2:has-text('to confirm')").innerText()).trim());
    // Relabel the dentist block and remember the title.
    await page.click('section[aria-label="Timeline"] button:has-text("Dentist")');
    await page.waitForSelector("#relabel-search");
    await shot("today-3-panel");
    await page.check('input[type="checkbox"]');
    await page.fill("#relabel-search", "health");
    await page.click('button:has-text("Health admin")');
    await page.waitForSelector('section[aria-label="Timeline"] button:has-text("Health admin")');
    await settle();
    await page.click('button:has-text("Accept all")');
    await page.waitForSelector("text=Confirmed.");
    await settle();
    await shot("today-4-confirmed");
    console.log("footer:", (await page.locator("footer").innerText()).replace(/\s+/g, " "));
    console.log("blocks:\n" + psql(`SELECT to_char(b.t_start AT TIME ZONE 'America/Los_Angeles', 'HH24:MI') || '-' || to_char(b.t_end AT TIME ZONE 'America/Los_Angeles', 'HH24:MI') || ' ' || b.origin || ' ' || coalesce(b.title, '') || ' -> ' || i.title || CASE WHEN b.committed THEN ' [committed]' ELSE '' END FROM time_block b JOIN intent i ON i.id = b.intent_id ORDER BY b.t_start`));
    console.log("rules:", psql("SELECT match_title FROM attribution_rule").trim());
    await page.goto(`${BASE}/?date=${new Date(Date.now() - 86400000 - 7 * 3600000).toISOString().slice(0, 10)}`);
    await settle();
    await shot("today-5-yesterday");
  },

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
