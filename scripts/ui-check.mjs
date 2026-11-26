#!/usr/bin/env node
// A scripted browser pass over the UI, against a server on the TEST database:
//   DATABASE_URL=postgres:///life_on_rails_test?host=/var/run/postgresql LOR_PORT=3857 LOR_OWNER_LOGIN= node scripts/serve.mjs
//   node scripts/ui-check.mjs <flow> [outDir]
// Uses the system Chrome through playwright-core. Screenshots go to outDir.
import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import { parseEnv } from "node:util";
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
/** The server's LOR_TZ, as scripts/serve.mjs gets it: the environment, then the env file (Node's own parser), then env.ts's default. */
const SERVER_TZ = (() => {
  if (process.env.LOR_TZ) return process.env.LOR_TZ;
  const file = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");
  return (existsSync(file) ? parseEnv(readFileSync(file, "utf8")).LOR_TZ : undefined) || "America/New_York";
})();
/**
 * The local day a flow works on: today in the server's time zone. A flow that fixes the clock passes it whole
 * (`?at=<DAY>T15:30`), which the server takes as is even when LOR_FAKE_NOW sets another day, so the page,
 * the fixtures and the SQL checks all mean the same day.
 */
const DAY = () => psql(`SELECT to_char(now() AT TIME ZONE '${SERVER_TZ}', 'YYYY-MM-DD')`).trim();
/** SQL for a local time on `day` in the server's time zone. */
const at = (day, hours) => `(('${day}'::date + interval '${hours} hours') AT TIME ZONE '${SERVER_TZ}')`;
/** Fail the flow with what was expected and what was found. */
const expect = (ok, what, found) => {
  if (!ok) throw new Error(`expected ${what}${found === undefined ? "" : `; found ${typeof found === "string" ? found : JSON.stringify(found)}`}`);
  console.log(`ok: ${what}`);
};

/** Run `lor` as the agent against the test server. */
const lor = (args, input) =>
  execFileSync("node", ["cli/lor.mjs", ...args], { encoding: "utf8", input, env: { ...process.env, LOR_PORT: new URL(BASE).port, LOR_THREAD: "ui-check-thread" } });

const flows = {
  // The test server's fixed clock (src/server/clock.ts): Today drawn at 09:00 by link stays at
  // 09:00 past the page's 20-second tick, keeps the time across a link, and at 00:05 the whole waking day is ahead.
  // Changes no rows; meant for the demo set (pnpm demo:test).
  async clock() {
    await page.setViewportSize({ width: 1500, height: 950 });
    const stamp = () => page.locator("aside p").first().innerText();
    await page.goto(`${BASE}/?at=09:00`);
    await settle();
    // The hero, the minutes to the next event and the sidebar's clock: any of them moves with a running clock.
    const read = async () => {
      const text = await page.locator("main").innerText();
      return { hero: text.match(/\d+h( \d+m)?(?=\s*remaining)/i)?.[0], next: text.match(/next [^\n]*/)?.[0], stamp: await stamp() };
    };
    const before = await read();
    await shot("clock-1-0900");
    await page.waitForTimeout(21_000);
    const after = await read();
    await shot("clock-2-0900-after-tick");
    console.log("09:00 before tick:", JSON.stringify(before), "after:", JSON.stringify(after));
    if (!before.hero || JSON.stringify(before) !== JSON.stringify(after) || !/09:00/.test(before.stamp) || !/in 30m/.test(before.next ?? "")) throw new Error("the clock moved, or is not 09:00");
    await page.getByRole("link", { name: "Tasks" }).first().click();
    await settle();
    console.log("after a link:", page.url(), "stamp", await stamp());
    if (!/at=09%3A00|at=09:00/.test(page.url()) || !/09:00/.test(await stamp())) throw new Error("the link dropped the tab's clock");
    await page.goto(`${BASE}/?at=00:05`);
    await settle();
    const early = { text: await page.locator("main").innerText(), stamp: await stamp() };
    await shot("clock-3-0005");
    // Before the waking day starts (08:00), all of it remains, and the next event is hours away.
    console.log("00:05:", early.stamp, early.text.match(/\d+h( \d+m)?(?=\s*remaining)/i)?.[0], early.text.match(/next [^\n]*/)?.[0]);
    if (!/00:05/.test(early.stamp) || !/14h\s*remaining/i.test(early.text)) throw new Error("00:05 did not draw the whole waking day ahead");
  },

  async tasks() {
    psql(`DELETE FROM intent_link; DELETE FROM proposal; UPDATE time_block SET commitment_id = NULL; DELETE FROM commitment; DELETE FROM time_block_observation; DELETE FROM time_block; DELETE FROM attribution_rule; DELETE FROM intent WHERE NOT system; DELETE FROM entity_identifier; DELETE FROM entity;`);
    const id = (s) => JSON.parse(s).id;
    const goal = id(lor(["intent", "create", "--type", "goal", "--title", "Neha and the wedding"]));
    const vendors = id(lor(["intent", "create", "--type", "project", "--title", "Wedding vendors", "--parent", goal]));
    id(lor(["intent", "create", "--type", "project", "--title", "Life on Rails"]));
    lor(["entity", "upsert", "--kind", "person", "--name", "Dad"]);
    lor(["intent", "create", "--type", "task", "--title", "Shortlist ceremony venues", "--parent", vendors, "--due", new Date(Date.now() + 3 * 86400000).toISOString(), "--flag", "You fly out on the 23rd: a shortlist first means you can visit them."]);
    lor(["intent", "create", "--type", "task", "--title", "Check the H-1B change of status took effect", "--due", new Date(Date.now() - 2 * 86400000).toISOString()]);
    lor(["intent", "create", "--type", "task", "--title", "Choose the invitation vendor", "--parent", vendors]);
    psql(`INSERT INTO intent (id, type, title, google_task_id, via) VALUES (gen_random_uuid(), 'task', 'Renew passport (from Google)', 'g-ui', 'job')`);

    await page.setViewportSize({ width: 1500, height: 950 });
    await page.goto(`${BASE}/tasks`);
    await settle();
    await shot("tasks-1-open");

    // Quick add: parent, date and agent in one line, with a suggestion picked by keyboard.
    const qa = page.getByRole("combobox", { name: "Add a task" });
    await qa.click();
    await qa.pressSequentially("Ask the florist about peonies #wedd");
    await page.waitForSelector("#quick-add-choices");
    await shot("tasks-2-suggest");
    await qa.press("Enter");
    await qa.pressSequentially("next fri @agent");
    await shot("tasks-3-parsed");
    const chips = await page.locator(".chip").allInnerTexts();
    console.log("chips:", chips.join(" | "));
    await qa.press("Enter");
    await page.waitForSelector('li:has-text("Ask the florist about peonies")', { state: "attached" }).catch(() => {});
    await settle();
    const row = psql(`SELECT title, assignee, to_char(due_at AT TIME ZONE 'America/New_York', 'Dy') FROM intent WHERE title LIKE 'Ask the florist%'`);
    console.log("created:", row.trim());
    const linked = psql(`SELECT p.title FROM intent_link l JOIN intent p ON p.id = l.to_id JOIN intent t ON t.id = l.from_id WHERE t.title LIKE 'Ask the florist%'`);
    console.log("counts toward:", linked.trim());

    // Waiting on a person, and a dismissed date word.
    await qa.pressSequentially("Get the deposit back @Dad tomorrow");
    await qa.press("Enter");
    await settle();
    await qa.pressSequentially("Watch Friday Night Lights");
    await page.locator('.chip:has-text("Due") button').click();
    await qa.press("Enter");
    await settle();
    console.log("waiting:", psql(`SELECT title, status FROM intent WHERE title LIKE 'Get the deposit%'`).trim());
    console.log("literal:", psql(`SELECT title, due_at IS NULL FROM intent WHERE title LIKE 'Watch%'`).trim());

    // The global dialog from another screen.
    await page.goto(`${BASE}/`);
    await settle();
    await page.keyboard.press("q");
    await page.getByRole("dialog").waitFor();
    await shot("tasks-4-dialog");
    await page.keyboard.press("Escape");

    // Filters, keyboard, the panel.
    await page.goto(`${BASE}/tasks?for=${goal}`);
    await settle();
    await shot("tasks-5-filter-goal");
    console.log("under the goal:", (await page.locator("li[id^=task-]").allInnerTexts()).map((t) => t.split("\n")[0]).join(" | "));
    await page.goto(`${BASE}/tasks?group=for`);
    await settle();
    await page.keyboard.press("j");
    await page.keyboard.press("Enter");
    await settle();
    await shot("tasks-6-panel");

    // Bulk: select two with x, reschedule them.
    await page.keyboard.press("Escape");
    await page.keyboard.press("x");
    await page.keyboard.press("j");
    await page.keyboard.press("x");
    await shot("tasks-7-bulk");
    await page.locator('label:has(select[aria-label="Due"])').last().locator("select").selectOption({ label: "Tomorrow" });
    await settle();
    console.log("notice:", await page.getByRole("status").innerText().catch(() => "(none)"));
    await shot("tasks-8-after-bulk");

    await page.goto(`${BASE}/tasks?view=agent`);
    await settle();
    await shot("tasks-9-agent");
  },
  async people() {
    psql(`DELETE FROM page WHERE slug NOT IN ('agent', 'me', 'inbox'); UPDATE intent SET waiting_on_entity_id = NULL; DELETE FROM entity_identifier; DELETE FROM entity; DELETE FROM observation WHERE source = 'gmail';`);
    const priya = JSON.parse(lor(["entity", "upsert", "--kind", "person", "--name", "Priya Raman", "--email", "priya@example.com"]));
    lor(["entity", "upsert", "--kind", "person", "--name", "Jo Bloggs", "--email", "jo@example.com"]);
    lor(["entity", "upsert", "--kind", "org", "--name", "Acme", "--id", "domain:acme.com"]);
    const soon = new Date(Date.now() + 2 * 86400000);
    lor(["entity", "set", priya.id, "--keep-in-touch", "30", "--birthday", `${String(soon.getMonth() + 1).padStart(2, "0")}-${String(soon.getDate()).padStart(2, "0")}`]);
    lor(["entity", "set", "jo@example.com", "--keep-in-touch", "14"]);
    lor(["page", "create", "people/priya-raman", "--title", "Priya Raman", "--entity", priya.id], "# Priya Raman\n\nMoved to Austin in March. Prefers calls to email. Old friend from [[me]].\n");
    lor(["intent", "create", "--type", "task", "--title", "Get the deposit back from Priya", "--waiting-on", priya.id, "--due", new Date(Date.now() + 86400000).toISOString()]);
    psql(`INSERT INTO observation (id, t_start, t_end, device, source, title, payload) VALUES
      (gen_random_uuid(), now() - interval '45 days', now() - interval '45 days', 'buildbot', 'gmail', 'Lunch?', '{"direction":"in","from_email":"priya@example.com","gmail_id":"a1"}'),
      (gen_random_uuid(), now() - interval '3 days', now() - interval '3 days', 'buildbot', 'gmail', 'Re: plans', '{"direction":"sent","to_emails":["jo@example.com"],"gmail_id":"a2"}'),
      (gen_random_uuid(), now() - interval '1 days', now() - interval '1 days', 'buildbot', 'gmail', 'Invoice', '{"direction":"in","from_email":"billing@acme.com","gmail_id":"a3"}')`);
    await page.setViewportSize({ width: 1500, height: 950 });
    await page.goto(`${BASE}/people`);
    await settle();
    await shot("people-1-who-matters");
    await page.click('a:has-text("Due a hello")');
    await page.waitForURL(/filter=overdue/);
    await settle();
    await shot("people-2-overdue");
    await page.goto(`${BASE}/people?filter=all`);
    await settle();
    await page.selectOption('select[aria-label="Keep in touch with Acme"]', { label: "Every 3 months" });
    await page.waitForFunction(() => document.querySelector('select[aria-label="Keep in touch with Acme"]')?.value === "90");
    console.log("acme cadence:", psql("SELECT keep_in_touch_days FROM entity WHERE name = 'Acme'").trim());
    await page.click('a:has-text("Priya Raman")');
    await page.waitForSelector("text=Waiting on them");
    await settle();
    await shot("people-3-hub");
    console.log("hub waiting:", await page.locator('section:has-text("Waiting on them") li').allInnerTexts());
    console.log("hub mail:", await page.locator('section:has(h2:text("Mail")) li').allInnerTexts());
  },

  async notes() {
    psql(`DELETE FROM page WHERE slug NOT IN ('agent', 'me', 'inbox'); UPDATE intent SET waiting_on_entity_id = NULL; DELETE FROM entity_identifier; DELETE FROM entity; DELETE FROM observation;`);
    const entity = JSON.parse(lor(["entity", "upsert", "--kind", "person", "--name", "Priya Raman", "--email", "priya@example.com"]));
    lor(["page", "create", "people/priya-raman", "--title", "Priya Raman", "--entity", entity.id],
      "# Priya Raman\n\nMoved to Austin in March [^move] and prefers calls to email. See [[me]] and [[topics/austin]].\n\n[^move]: mail:18c2f4a9d0e1b2c3 \"we close on the house March 3\"\n");
    lor(["page", "update", "people/priya-raman", "--base", "1", "--find", "prefers calls to email", "--replace", "HATES phone calls", "--note", "a wrong edit"]);
    lor(["page", "create", "topics/austin", "--title", "Austin"], "# Austin\n\nWhere [[people/priya-raman|Priya]] lives now. Trip ideas in [[topics/travel]].\n");
    lor(["page", "create", "topics/travel", "--title", "Travel"], "# Travel\n\nPlaces: [[topics/austin]].\n");
    lor(["page", "create", "projects/wedding", "--title", "Wedding"], "# Wedding\n\nVenues, vendors. Ask [[people/priya-raman]].\n");
    const y = new Date(Date.now() - 86400000 - 7 * 3600000).toISOString().slice(0, 10);
    lor(["page", "create", `days/${y}`, "--title", y], `# ${y}\n\nA quiet day. Called [[people/priya-raman|Priya]] about the [[projects/wedding|wedding]].\n`);

    await page.setViewportSize({ width: 1500, height: 950 });
    await page.goto(`${BASE}/notes`);
    await settle();
    await shot("notes-1-index");
    await page.click('a:has-text("Priya Raman")');
    await page.waitForSelector("text=HATES phone calls");
    await settle();
    await shot("notes-2-page");
    console.log("gmail link:", await page.locator('[data-url*="mail.google.com"]').first().getAttribute("data-url"));
    console.log("links here:", await page.locator('section:has(h4:has-text("Links here")) li').allInnerTexts());

    await page.click(`button:has-text("Take back the agent's last edit")`);
    await page.waitForSelector("text=prefers calls to email");

    // Typing saves by itself, and a run of typing is one version.
    const ed = page.locator(".cm-content");
    const versions = () => psql("SELECT string_agg(version || ':' || via, ' ' ORDER BY version) FROM page_version v JOIN page p ON p.id = v.page_id WHERE p.slug = 'people/priya-raman'").trim();
    await page.locator(".cm-line", { hasText: "Moved to Austin" }).click();
    await page.keyboard.press("End");
    await page.keyboard.type(" She has a dog called Biscuit.");
    await page.waitForSelector("text=Saved", { timeout: 5000 });
    await page.keyboard.type(" And a cat.");
    await page.waitForFunction(() => document.body.innerText.includes("Saved") && !document.body.innerText.includes("Saving"));
    await page.waitForTimeout(1600);
    console.log("versions after typing:", versions());

    // [[ suggests pages; the link reads as the page's title once the cursor leaves the line.
    await page.keyboard.press("Enter");
    await page.keyboard.press("Enter");
    await page.keyboard.type("Ask about [[trav");
    await page.waitForSelector(".cm-tooltip-autocomplete");
    await shot("notes-2b-suggest");
    await page.keyboard.press("Enter");
    await page.keyboard.type(" and [[Home Network");
    await page.waitForSelector('.cm-tooltip-autocomplete li:has-text("New page")');
    await page.keyboard.press("Enter");
    await page.keyboard.press("ArrowUp");
    await page.waitForTimeout(1600);
    console.log("body now:", JSON.stringify(psql("SELECT body FROM page WHERE slug = 'people/priya-raman'").trim()));
    await shot("notes-2c-links");

    // The agent writes while this page is open and nothing is unsaved: it simply appears.
    lor(["page", "update", "people/priya-raman", "--append"], "- The agent added this meanwhile.\n");
    await page.evaluate(() => window.dispatchEvent(new Event("focus")));
    await page.waitForSelector(".cm-content >> text=The agent added this meanwhile");
    console.log("agent edit picked up:", true);

    console.log("agent lines marked:", await page.locator(".cm-by-agent").count());

    // The agent writes elsewhere while there is unsaved typing: merged, nothing asked.
    await page.locator(".cm-line", { hasText: "Ask about" }).click();
    await page.keyboard.press("End");
    await page.keyboard.type(" Mine.");
    lor(["page", "update", "people/priya-raman", "--append"], "- And this, too.\n");
    await page.waitForSelector("text=Merged with the agent's edit");
    await page.waitForFunction(() => document.body.innerText.includes("Saved") && !document.body.innerText.includes("Saving"));
    await page.waitForTimeout(400);
    const body = psql("SELECT body FROM page WHERE slug = 'people/priya-raman'");
    console.log("merged:", body.includes("Mine.") && body.includes("And this, too."));

    // Different words of one paragraph: merged, nothing asked.
    await page.locator(".cm-line", { hasText: "Biscuit" }).click();
    await page.keyboard.press("Control+End");
    await page.keyboard.type("\nShe walks him at seven.");
    const head0 = psql("SELECT head_version FROM page WHERE slug = 'people/priya-raman'").trim();
    lor(["page", "update", "people/priya-raman", "--base", head0, "--find", "called Biscuit", "--replace", "called Biscotti"]);
    await page.waitForFunction(() => document.body.innerText.includes("Saved") && !document.body.innerText.includes("Saving"));
    await page.waitForTimeout(400);
    const words = psql("SELECT body FROM page WHERE slug = 'people/priya-raman'");
    console.log("word-level merge:", words.includes("Biscotti") && words.includes("walks him at seven"));

    // The same words changed both ways: only that passage is put to you.
    await page.locator(".cm-line", { hasText: "walks him" }).click();
    await page.keyboard.press("Home");
    await page.keyboard.press("Shift+End");
    await page.keyboard.type("She walks him at eight.");
    const head = psql("SELECT head_version FROM page WHERE slug = 'people/priya-raman'").trim();
    lor(["page", "update", "people/priya-raman", "--base", head, "--find", "at seven", "--replace", "at six"]);
    await page.waitForSelector('section[aria-label="Settle the clash"]');
    await shot("notes-2d-clash");
    await page.click('button:has-text("Keep yours")');
    await page.click('section[aria-label="Settle the clash"] button:has-text("Done")');
    await page.waitForFunction(() => document.body.innerText.includes("Saved") && !document.body.innerText.includes("Saving"));
    await page.waitForTimeout(400);
    const settled = psql("SELECT body FROM page WHERE slug = 'people/priya-raman'");
    console.log("clash settled as mine:", settled.includes("at eight") && !settled.includes("at six") && settled.includes("Biscotti"));

    // Take back one old agent edit, keeping everything since.
    const agentV = psql("SELECT min(v.version) FROM page_version v JOIN page p ON p.id = v.page_id WHERE p.slug = 'people/priya-raman' AND v.via = 'agent' AND v.body LIKE '%meanwhile%'").trim();
    await page.click('button:has-text("History")');
    await page.locator("li", { hasText: `Version ${agentV}` }).getByRole("button", { name: "take back" }).click();
    await page.waitForSelector(`text=Took back version ${agentV}`);
    await page.waitForTimeout(800);
    const after = psql("SELECT body FROM page WHERE slug = 'people/priya-raman'");
    console.log("took back:", !after.includes("meanwhile") && after.includes("Biscotti") && after.includes("And this, too."));
    await shot("notes-2e-history");
    console.log("versions:", versions());

    // A link to a page nobody wrote starts it.
    await page.locator(".cm-wikilink-missing").first().click();
    await page.waitForURL(/slug=home-network/);
    await page.waitForSelector(".cm-content >> text=Home Network");
    console.log("started:", psql("SELECT slug || ' / ' || title FROM page WHERE slug = 'home-network'").trim());
    await page.goBack();
    await page.waitForSelector(".cm-wikilink >> text=Travel");
    await page.locator(".cm-wikilink", { hasText: "Travel" }).click();
    await page.waitForURL(/slug=topics%2Ftravel/);
    console.log("followed to:", page.url());

    await page.goto(`${BASE}/notes?view=graph`);
    await settle();
    await shot("notes-3-graph");
    await page.locator('[data-slug="topics/austin"]').click();
    await page.waitForURL(/slug=topics/);
    await settle();
    await shot("notes-4-graph-selected");
    await page.locator('[data-slug="topics/austin"]').click();
    await page.waitForSelector("text=Trip ideas");
    console.log("opened from graph:", page.url());

    await page.goto(`${BASE}/?date=${y}`);
    await settle();
    await shot("notes-5-day-record");
    await page.goto(`${BASE}/search?q=austin`);
    await settle();
    await shot("notes-6-search");
    console.log("search kinds:", await page.locator("li .chip").allInnerTexts());
  },

  // Today as src/routes/index.tsx draws it, on the test server's fixed clock (`?at=`, src/server/clock.ts)
  // so the timer has real minutes in it: the lists come right after the arc and lanes, the brief opens and closes
  // by mouse and keyboard and stays open across a data refresh, the running timer shows once; start at 15:30 from
  // the timer card, pause at 16:00, stop at 16:10, then select the Dentist block in the Planned and actual lanes
  // and relabel it, remembering the title.
  async today() {
    const day = DAY();
    psql(`
      DELETE FROM intent_link; DELETE FROM proposal; UPDATE time_block SET commitment_id = NULL; DELETE FROM commitment;
      DELETE FROM time_block_observation; DELETE FROM time_block; DELETE FROM google_event; DELETE FROM attribution_rule; DELETE FROM intent WHERE NOT system;
      INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING;
      INSERT INTO google_event (calendar_id, event_id, title, t_start, t_end, all_day) VALUES
        ('cal', 'standup', 'Standup', ${at(day, 9)}, ${at(day, 9.5)}, false),
        ('cal', 'hold', 'Afternoon hold', ${at(day, 13)}, ${at(day, 17)}, false),
        ('cal', 'dentist', 'Dentist', ${at(day, 14)}, ${at(day, 15)}, false),
        ('cal', 'visit', 'Priya in town', ${at(day, 0)}, ${at(day, 24)}, true);
      INSERT INTO google_event (calendar_id, event_id, title, t_start, t_end, event_type) VALUES ('cal', 'focus', 'Focus time', ${at(day, 10)}, ${at(day, 12)}, 'focusTime');
      INSERT INTO intent (id, type, title, weight, weekly_hour_target) VALUES (gen_random_uuid(), 'goal', 'Ship Life on Rails', 3, 10);
      INSERT INTO intent (id, type, title) VALUES (gen_random_uuid(), 'project', 'Health admin');
      -- Two tasks due tomorrow: the sooner one needs longer than any free stretch, so the slot offers the other.
      INSERT INTO intent (id, type, title, due_at, estimate_hours) VALUES
        (gen_random_uuid(), 'task', 'Rewrite the whole spec', ${at(day, 30)}, 20),
        (gen_random_uuid(), 'task', 'Renew the passport', ${at(day, 33)}, 1);
      -- A timer block nobody labelled, so the strip asks for a label.
      INSERT INTO time_block (id, t_start, t_end, origin) VALUES (gen_random_uuid(), ${at(day, 12)}, ${at(day, 12.5)}, 'manual');
      DELETE FROM page WHERE slug = 'briefs/${day}';
    `);
    lor(["intent", "create", "--type", "task", "--title", "Call the dentist back", "--flag", "They need an answer about Thursday."]);
    const agentTask = JSON.parse(lor(["intent", "create", "--type", "task", "--title", "Find a physio near work", "--assign", "agent"])).id;
    lor(["intent", "create", "--type", "task", "--title", "Compare the two quotes", "--assign", "agent"]);
    lor(["page", "create", `briefs/${day}`, "--title", `Brief ${day}`], "Finish the spec before the design review at 14:00. Lena is waiting on the icon sizes.\n\n- 09:30 standup, 14:00 dentist.\n- One flag: the dentist.\n");
    const local = (col) => `to_char(${col} AT TIME ZONE '${SERVER_TZ}', 'HH24:MI')`;
    // Today's blocks, one a line. Phone and place blocks from earlier data (the demo set) stay; they are fill below the calendar.
    const blocks = () =>
      psql(`SELECT ${local("b.t_start")} || '-' || coalesce(${local("b.t_end")}, 'open') || ' ' || b.origin || ' ' || coalesce(b.title, '') || ' -> ' || coalesce(i.title, '-')
              || CASE WHEN b.paused_at IS NOT NULL THEN ' [paused ' || ${local("b.paused_at")} || ']' ELSE '' END || CASE WHEN b.committed THEN ' [committed]' ELSE '' END
            FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id WHERE b.t_start >= ${at(day, 0)} AND b.t_start < ${at(day, 24)} ORDER BY b.t_start`).trim();
    const lanes = page.locator('section[aria-label="Planned and actual"]');
    // The page's sections in order after the lanes: each panel's heading, and the Ask box.
    const order = () =>
      page.evaluate(() => {
        const lanes = document.querySelector('section[aria-label="Planned and actual"]');
        const after = (el) => lanes && lanes.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING;
        return [...document.querySelectorAll("main h2, main input[aria-label='Ask the agent']")]
          .filter(after)
          .map((el) => (el.tagName === "INPUT" ? "Ask" : el.textContent.trim()));
      });
    const briefToggle = page.locator('button[aria-controls~="brief-lead"]');
    const briefOpen = async () => (await briefToggle.getAttribute("aria-expanded")) === "true" && (await page.locator("#brief-rest").isVisible());
    const actual = (text) => lanes.locator(`button[title*="${text}"]`);
    const stop = page.getByRole("button", { name: "Stop", exact: true });
    const card = page.locator("div", { has: stop }).last();

    await page.setViewportSize({ width: 1500, height: 950 });
    await page.goto(`${BASE}/?at=${day}T15:30`);
    await settle();
    await shot("today-1-open");
    const first = await order();
    expect(
      ["Needs you", "Due soon", "Goals this week", "This morning", "Ask"].every((h, i) => first[i] === h),
      "Needs you, Due soon and Goals this week right after the lanes, then the brief, then the Ask box",
      first,
    );
    const needs = page.locator("h2", { hasText: "Needs you" });
    const firstNeed = page.locator("section", { has: needs }).locator("li").first();
    const bottom = await firstNeed.evaluate((el) => (window.scrollY === 0 ? el.getBoundingClientRect().bottom : Infinity));
    expect(bottom <= 950, "the first Needs you item inside the 950px window without scrolling", `bottom at ${Math.round(bottom)}px`);
    // The strip under the lanes: the legend behind its help mark, the label hint while a block has no label, one free slot.
    const legendMark = page.getByRole("button", { name: "What the lanes mean" });
    const legend = page.locator("#lanes-legend");
    const legendText = "Solid: counted · dashed: a suggestion";
    expect(!(await legend.isVisible()), "the legend not printed on the page until asked for");
    await legendMark.hover();
    expect((await legend.isVisible()) && (await legend.innerText()).startsWith(legendText), "the legend shown on hovering its help mark", await legend.innerText());
    await shot("today-1c-legend");
    await page.mouse.move(5, 5);
    expect(!(await legend.isVisible()), "the legend gone when the pointer leaves");
    await legendMark.click();
    await page.mouse.move(5, 5);
    expect((await legend.isVisible()) && (await legendMark.getAttribute("aria-expanded")) === "true", "the legend held open by a click");
    await legendMark.click();
    await page.mouse.move(5, 5);
    expect(!(await legend.isVisible()), "the legend closed by a second click");
    await page.keyboard.press("Shift");
    await legendMark.focus();
    expect(await legend.isVisible(), "the legend shown while its help mark has keyboard focus");
    await legendMark.blur();
    const hint = lanes.getByText("Click a block to label it.");
    expect(await hint.isVisible(), "the label hint while the 12:00 timer block has no label");
    const slots = lanes.locator("[data-free-slot]");
    const slotText = (await slots.allInnerTexts()).join(" | ");
    expect(
      (await slots.count()) === 1 && /free at 17:00/.test(slotText) && /fits Renew the passport/.test(slotText),
      "one free slot, at 17:00 after the afternoon hold, offering the task due soonest that fits it",
      slotText,
    );
    const main = await page.locator("main").textContent();
    expect(!/Calendar confirmed|time accounted for/i.test(main) && /Too much\? Reset visibility/.test(main), "the footer without the two percentages, with Reset visibility", main.slice(-200));
    // With the agent: one task a line, each a link to it in Tasks.
    const withAgent = page.locator("div", { has: page.locator(".tag-agent") }).last().locator("li a");
    const agentLinks = await withAgent.evaluateAll((as) => as.map((a) => `${a.textContent} ${a.getAttribute("href")}`));
    expect(
      agentLinks.length === 2 && agentLinks.some((l) => l.startsWith("Find a physio near work ") && l.includes(`id=${agentTask}`) && l.includes("view=agent")),
      "\"with the agent\" as a list of two task links, each opening its task",
      agentLinks,
    );
    // The brief: one line, closed, until opened.
    expect(!(await briefOpen()), "the brief closed on load", await briefToggle.getAttribute("aria-expanded"));
    const lead = page.locator("section", { has: briefToggle }).locator("p").first();
    const leadBox = await lead.evaluate((el) => ({ text: el.textContent, h: el.getBoundingClientRect().height, line: parseFloat(getComputedStyle(el).lineHeight) }));
    expect(leadBox.text === "Finish the spec before the design review at 14:00." && leadBox.h <= leadBox.line * 1.5, "the brief's first sentence on one line", leadBox);
    await briefToggle.click();
    expect(await briefOpen(), "the brief open by mouse");
    expect(/Lena is waiting on the icon sizes/.test(await page.locator("#brief-rest").innerText()), "the rest of the brief shown when open");
    await shot("today-1b-brief-open");
    await briefToggle.click();
    expect(!(await briefOpen()), "the brief closed by mouse");
    await briefToggle.focus();
    await page.keyboard.press("Enter");
    expect(await briefOpen(), "the brief open by keyboard (Enter)");
    await page.keyboard.press("Space");
    expect(!(await briefOpen()), "the brief closed by keyboard (Space)");
    // Start: "Something else…", a title, and the goal picked by search.
    await page.getByRole("button", { name: "Something else…" }).click();
    await page.fill('input[placeholder="What are you doing? (optional)"]', "Writing the spec");
    const search = page.locator('input[placeholder="Search goals, projects, habits, tasks…"]');
    await search.fill("ship");
    // Enter picks the first match. A click cannot: the picker spills out of the day arc and the
    // lanes' hour row covers its buttons (a product bug, reported in lor-w2tw).
    expect((await page.locator("button", { hasText: "Ship Life on Rails" }).count()) === 1, "the picker to offer Ship Life on Rails for \"ship\"");
    await search.press("Enter");
    await stop.waitFor();
    await settle();
    expect(/Ship Life on Rails.*Writing the spec/s.test(await card.innerText()), "the timer card to show Ship Life on Rails — Writing the spec", await card.innerText());
    await shot("today-2-running");
    // The running timer once on Today: the card, and no chip in the page head. Other pages keep the chip, a link to Today.
    const chip = page.locator("main header a", { hasText: "Ship Life on Rails" });
    expect((await chip.count()) === 0, "no timer chip in Today's page head while the card shows the timer", await page.locator("main header").first().innerText());
    await page.goto(`${BASE}/tasks?at=${day}T15:45`);
    await settle();
    expect((await chip.count()) === 1 && new URL(await chip.getAttribute("href"), BASE).pathname === "/", "the timer chip in the Tasks page head, linking to Today", await page.locator("main header").first().innerText());

    // Pause half an hour later.
    await page.goto(`${BASE}/?at=${day}T16:00`);
    await settle();
    expect(/\b30m\b/i.test(await card.innerText()), "the running card to read 30m at 16:00", await card.innerText());
    // Open the brief first: Pause refreshes the page's data, and the brief stays open across it.
    await briefToggle.click();
    await page.getByRole("button", { name: "Pause", exact: true }).click();
    await page.getByRole("button", { name: "Resume", exact: true }).waitFor();
    await settle();
    expect(await briefOpen(), "the brief still open after Pause refreshed the page's data");
    await briefToggle.click();
    const paused = blocks();
    expect(/15:30-open manual .* -> Ship Life on Rails \[paused 16:00\]/.test(paused), "an open timer block from 15:30 for Ship Life on Rails, paused at 16:00", paused);
    await shot("today-3-paused");

    // Stop ten minutes into the pause: the block ends when it was paused.
    await page.goto(`${BASE}/?at=${day}T16:10`);
    await settle();
    expect(/\b30m\b/i.test(await card.innerText()), "the paused card to still read 30m at 16:10", await card.innerText());
    await stop.click();
    await page.getByRole("button", { name: "Something else…" }).waitFor();
    await settle();
    const stopped = blocks();
    expect(/15:30-16:00 manual Writing the spec -> Ship Life on Rails(?! \[paused)/.test(stopped) && !/-open /.test(stopped), "the timer block closed at 15:30-16:00, nothing left open", stopped);
    expect((await actual("Ship Life on Rails").count()) === 1, "one Ship Life on Rails block in the Actual lane", await lanes.locator("button").evaluateAll((bs) => bs.map((b) => b.title)));
    await shot("today-4-stopped");

    // Select the Dentist block: the calendar fill made it, labelled Meetings.
    const before = blocks();
    expect(/14:00-15:00 calendar Dentist -> Meetings/.test(before), "a 14:00-15:00 calendar block for Dentist, labelled Meetings", before);
    await actual("Dentist").click();
    await page.waitForSelector("#relabel-search");
    expect((await actual("Dentist").getAttribute("class")).includes("outline-txt"), "the Dentist block drawn as selected", await actual("Dentist").getAttribute("class"));
    const panel = page.locator("section", { has: page.locator("#relabel-search") }).last();
    expect(/from the calendar/.test(await panel.innerText()), "the block panel to say it came from the calendar", await panel.innerText());
    const withPanel = await order();
    const needsAt = withPanel.indexOf("Needs you");
    expect(needsAt >= 1 && withPanel.slice(needsAt, needsAt + 3).join() === "Needs you,Due soon,Goals this week", "the open block panel between the lanes and Needs you", withPanel);
    await shot("today-5-panel");
    // Relabel it to Health admin and remember the title.
    await page.getByLabel("Label every “Dentist” this way from now on").check();
    await page.fill("#relabel-search", "health");
    await panel.locator("button", { hasText: "Health admin" }).click();
    await page.waitForSelector("#relabel-search", { state: "detached" });
    await settle();
    const after = blocks();
    expect(/14:00-15:00 calendar Dentist -> Health admin/.test(after), "the Dentist block relabelled to Health admin", after);
    const rules = psql("SELECT match_title || ' -> ' || i.title FROM attribution_rule r JOIN intent i ON i.id = r.intent_id").trim();
    expect(rules === "^Dentist$ -> Health admin", "one rule for the exact title Dentist (^Dentist$) -> Health admin", rules);
    expect(/Health admin/.test(await actual("Dentist").getAttribute("title")), "the lane's Dentist block to read Health admin", await actual("Dentist").getAttribute("title"));
    await shot("today-6-relabelled");
    console.log("blocks:\n" + after);
    // Every block today labelled: the hint goes.
    psql(`UPDATE time_block SET intent_id = (SELECT id FROM intent WHERE title = 'Health admin')
          WHERE intent_id IS NULL AND t_start < ${at(day, 24)} AND coalesce(t_end, 'infinity') > ${at(day, 0)}`);
    await page.goto(`${BASE}/?at=${day}T16:10`);
    await settle();
    expect((await lanes.getByText("Click a block to label it.").count()) === 0, "no label hint once every block today has a label");

    await page.getByRole("link", { name: "Previous day" }).click();
    await page.waitForURL(/date=/);
    await page.locator("h1", { hasText: /^Day$/ }).waitFor({ timeout: 10_000 }).catch(() => {});
    await settle();
    expect((await page.locator("h1").first().textContent()).trim() === "Day", "the previous day headed Day", await page.locator("h1").first().textContent());
    await shot("today-7-yesterday");

    // The day's two measures moved to /status, each with what it measures.
    await page.goto(`${BASE}/status?at=${day}T16:10`);
    await settle();
    const status = await page.locator("main").textContent();
    expect(/Calendar confirmed/.test(status) && /Time accounted for/.test(status) && /calendar time is counted/.test(status), "Calendar confirmed and Time accounted for on /status, each with a sentence", status.slice(0, 600));
    await shot("today-8-status");

    // A one-sentence brief cut off at phone width still opens, by touch or keyboard, to the whole sentence.
    const sentence = "Finish the spec before the design review at 14:00 and send Lena the icon sizes.";
    psql(`DELETE FROM page WHERE slug = 'briefs/${day}'`);
    lor(["page", "create", `briefs/${day}`, "--title", `Brief ${day}`], `${sentence}\n`);
    await page.setViewportSize({ width: 412, height: 915 });
    await page.goto(`${BASE}/?at=${day}T16:10`);
    await settle();
    const leadEl = page.locator("#brief-lead");
    const cut = await leadEl.evaluate((el) => el.scrollWidth > el.clientWidth);
    expect(cut && (await briefToggle.count()) === 1, "a one-sentence brief cut off at 412px, with a control to open it", { cut, buttons: await briefToggle.count() });
    await briefToggle.focus();
    await page.keyboard.press("Enter");
    const whole = await leadEl.evaluate((el) => ({ text: el.textContent, fits: el.scrollWidth <= el.clientWidth }));
    expect((await briefToggle.getAttribute("aria-expanded")) === "true" && whole.fits && whole.text === sentence, "the whole sentence readable once opened", whole);
    await shot("today-9-brief-one-sentence-412");
    await briefToggle.click();
    expect((await briefToggle.getAttribute("aria-expanded")) === "false", "the one-sentence brief closed again");
    await page.setViewportSize({ width: 1500, height: 950 });
  },

  async goals() {
    psql(`DELETE FROM intent_link; DELETE FROM proposal; UPDATE time_block SET commitment_id = NULL; DELETE FROM commitment; DELETE FROM time_block_observation; DELETE FROM time_block; DELETE FROM attribution_rule; DELETE FROM intent WHERE NOT system;`);
    const id = (s) => JSON.parse(s).id;
    const wedding = id(lor(["intent", "create", "--type", "goal", "--title", "Neha and the wedding", "--hours", "6"]));
    const vendors = id(lor(["intent", "create", "--type", "project", "--title", "Wedding vendors", "--parent", wedding]));
    lor(["intent", "create", "--type", "task", "--title", "Shortlist venues", "--parent", vendors]);
    lor(["intent", "create", "--type", "task", "--title", "Call the florist", "--parent", vendors]);
    lor(["intent", "create", "--type", "task", "--title", "Book flights", "--parent", wedding]);
    id(lor(["intent", "create", "--type", "habit", "--title", "Call Neha", "--parent", wedding, "--frequency", "1"]));
    lor(["intent", "create", "--type", "project", "--title", "Health admin"]);

    await page.setViewportSize({ width: 1500, height: 950 });
    await page.goto(`${BASE}/goals`);
    await settle();
    await shot("goals-1");
    const card = (await page.locator('section:has-text("Neha and the wedding")').first().innerText()).replace(/\s+/g, " ");
    console.log("goal card:", card);
    expect(/0 \/ 6h this week/.test(card) && /Wedding vendors/.test(card) && /Call Neha/.test(card), "the wedding card with 0 / 6h, Wedding vendors and Call Neha", card);

    await page.click('button:has-text("New goal, project or habit")');
    await page.selectOption('select[aria-label="Kind"]', "goal");
    await page.fill('input[aria-label="Title"]', "Ship Life on Rails");
    await page.fill('input[aria-label="Hours a week"]', "10");
    await page.getByRole("button", { name: "Add", exact: true }).click();
    await page.waitForSelector('button:has-text("Ship Life on Rails")');
    expect(psql("SELECT weekly_hour_target FROM intent WHERE title = 'Ship Life on Rails' AND type = 'goal'").trim() === "10", "a goal Ship Life on Rails at 10 h a week");

    // Did it: the notice, and one habit completion in the table.
    await page.locator("li", { hasText: "Call Neha" }).getByRole("button", { name: "Did it" }).first().click();
    await page.waitForSelector("text=Logged “Call Neha”.");
    const logged = psql("SELECT count(*) FROM commitment c JOIN intent i ON i.id = c.intent_id WHERE c.kind = 'habit_completion' AND i.title = 'Call Neha'").trim();
    expect(logged === "1", "one habit_completion for Call Neha", logged);

    // Select a row: the row highlights, the link names it, and the side panel opens on it.
    await page.click('button:has-text("Wedding vendors")');
    await page.waitForURL(new RegExp(`id=${vendors}`));
    await settle();
    const aside = page.locator("aside").last();
    const row = page.locator("li > div", { has: page.locator('button:has-text("Wedding vendors")') }).first();
    expect((await row.getAttribute("class")).includes("row-sel"), "the Wedding vendors row drawn as selected", await row.getAttribute("class"));
    // innerText carries the CSS uppercase of titles and labels, so these match without case.
    const facts = (await aside.innerText()).replace(/\s+/g, " ");
    expect(/^Wedding vendors/i.test(facts) && /Kind Project/i.test(facts) && /Counts toward Neha and the wedding/i.test(facts) && /Tasks 2 tasks/i.test(facts), "the side panel on Wedding vendors: Project, under the wedding, 2 tasks", facts);
    await shot("goals-2-selected");

    // Open the editor.
    await aside.getByRole("button", { name: "Edit", exact: true }).click();
    await aside.getByRole("button", { name: "Stop editing" }).waitFor();
    const title = await aside.getByLabel("Title", { exact: true }).inputValue();
    expect(title === "Wedding vendors" && (await aside.getByLabel("Hours a week").count()) === 1, "the editor open on Wedding vendors with an Hours a week field", title);
    await shot("goals-3-editor");

    // The task count opens Tasks filtered to the project: its two tasks, not Book flights under the goal.
    await page.click('aside a:has-text("2 tasks")');
    await page.waitForURL(/\/tasks\?for=/);
    await settle();
    const rows = page.locator('li[id^="task-"] button[aria-label^="Select "]');
    await rows.first().waitFor({ state: "attached" });
    const listed = (await rows.evaluateAll((bs) => bs.map((b) => b.getAttribute("aria-label").replace(/^Select /, "")))).sort();
    console.log("tasks for vendors:", listed.join(", "));
    expect(JSON.stringify(listed) === JSON.stringify(["Call the florist", "Shortlist venues"]), "Tasks for Wedding vendors to list Call the florist and Shortlist venues, nothing else", listed);
    await shot("goals-4-tasks");
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
