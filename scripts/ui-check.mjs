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

/** Run `lor` as the agent against the test server. */
const lor = (args, input) =>
  execFileSync("node", ["cli/lor.mjs", ...args], { encoding: "utf8", input, env: { ...process.env, LOR_PORT: new URL(BASE).port, LOR_THREAD: "ui-check-thread" } });

const flows = {
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

    // The agent writes while there is unsaved typing: asked, nothing lost.
    await page.locator(".cm-line", { hasText: "Ask about" }).click();
    await page.keyboard.press("End");
    await page.keyboard.type(" Mine.");
    lor(["page", "update", "people/priya-raman", "--append"], "- And this, too.\n");
    await page.waitForSelector("text=The agent changed this page while you were writing");
    await shot("notes-2d-conflict");
    await page.click('button:has-text("Keep mine")');
    await page.waitForSelector("text=Saved");
    await page.waitForTimeout(500);
    console.log("kept mine:", psql("SELECT body FROM page WHERE slug = 'people/priya-raman'").includes("Mine."));
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
    console.log("goal card:", (await page.locator('section:has-text("Neha and the wedding")').first().innerText()).replace(/\s+/g, " "));
    await page.click('button:has-text("New goal, project or habit")');
    await page.selectOption('select[aria-label="Kind"]', "goal");
    await page.fill('input[aria-label="Title"]', "Ship Life on Rails");
    await page.fill('input[aria-label="Hours a week"]', "10");
    await page.getByRole("button", { name: "Add", exact: true }).click();
    await page.waitForSelector('button:has-text("Ship Life on Rails")');
    await page.locator("li", { hasText: "Call Neha" }).getByRole("button", { name: "Did it" }).first().click();
    await page.waitForSelector("text=Logged");
    await page.click('button:has-text("Wedding vendors")');
    await settle();
    await shot("goals-2-selected");
    await page.click('aside a:has-text("2 tasks")');
    await page.waitForURL(/\/tasks\?for=/);
    await settle();
    console.log("tasks for vendors:", (await page.locator("main").innerText()).match(/Shortlist venues|Call the florist|Book flights/g));
    await shot("goals-3-tasks");
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
