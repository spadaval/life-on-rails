// The dashboard prototype's data: a frozen snapshot of the demo set (seeds/demo/), read through
// LoR's own read paths, so the prototype draws what the real screens would.
//
//   node_modules/.bin/tsx prototype/dashboard/snapshot.ts [--day 2026-11-17]
//
// Empties the TEST database, loads the demo set dated from --day (default: the day already in
// src/data/snapshot.json, else today), and reads it at two clocks: 15:00 with the `running`
// variant (a timer 47 minutes in) and 09:00 on the base. Writes src/data/snapshot.json and
// leaves the database on the base variant. Refuses any database but life_on_rails_test: it
// must never read the real one, whose rows would then be committed here.
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(HERE, "src/data/snapshot.json");
const TEST_URL = "postgres:///life_on_rails_test?host=/var/run/postgresql";

// Before anything from src/ is imported: src/server/env.ts fills unset variables from the real
// env file, whose DATABASE_URL is the real database.
process.env.DATABASE_URL ??= TEST_URL;
process.env.LOR_SYNC_RECOMPUTE = "off";

const arg = (name: string) => {
  const i = process.argv.indexOf(`--${name}`);
  return i > 0 ? process.argv[i + 1] : undefined;
};
const previous = existsSync(OUT) ? (JSON.parse(readFileSync(OUT, "utf8")) as { day?: string }).day : undefined;

const { isTestDatabase, withClock } = await import("../../src/server/clock.js");
if (!isTestDatabase(process.env.DATABASE_URL)) {
  console.error(`snapshot: refusing ${process.env.DATABASE_URL}; it reads life_on_rails_test only. Leave DATABASE_URL unset.`);
  process.exit(2);
}

const { sql } = await import("../../src/db/client.js");
const { env } = await import("../../src/server/env.js");
const { loadDemo } = await import("../../scripts/demo.js");
const { setDemoVariant } = await import("../../src/server/demo.js");
const { getToday } = await import("../../src/server/today.js");
const { getGraph } = await import("../../src/server/graph.js");
const { recomputeScores } = await import("../../src/server/scoring/recompute.js");
const { entityNames, showEntity } = await import("../../src/server/memory/entities.js");
const { listPages, readPage } = await import("../../src/server/memory/pages.js");
const { PEOPLE_FILTERS, listPeople } = await import("../../src/server/people.js");
const { jobStatuses } = await import("../../src/server/status.js");
const { localTimeOnDay, localYmd } = await import("../../src/shared/localTime.js");

const tz = env.LOR_TZ;
const day = arg("day") ?? previous ?? localYmd(tz, new Date());
if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) throw new Error(`--day wants YYYY-MM-DD, not ${day}`);
const at = (hm: string) => localTimeOnDay(tz, day, hm);

/** What Today's loader (getTodayFn) reads that depends on the clock. Tasks, flags and goals come from the graph. */
async function todayAt(now: Date) {
  return withClock(now, async () => {
    const today = await getToday(sql, day, now);
    const recent = await sql<{ id: string; title: string }[]>`
      SELECT i.id, i.title FROM intent i
      JOIN (SELECT intent_id, max(t_start) AS last FROM time_block WHERE origin <> 'calendar' OR committed GROUP BY intent_id) b ON b.intent_id = i.id
      WHERE NOT i.system AND i.status IN ('active', 'waiting') ORDER BY b.last DESC LIMIT 5`;
    const brief = (await sql<{ body: string }[]>`SELECT body FROM page WHERE slug = ${`briefs/${day}`}`)[0]?.body ?? null;
    const jobs = await jobStatuses(now).catch(() => null);
    return {
      today,
      recent: recent.map((r) => ({ id: r.id, title: r.title })),
      brief,
      unwell: jobs ? jobs.filter((j) => j.state === "stale" || j.state === "error").map((j) => j.name) : null,
    };
  });
}

const started = Date.now();
await withClock(at("15:00"), () => loadDemo({ now: at("15:00") }));

// The afternoon: a timer running on the spec (variant `running`), read at 15:00.
const afternoon = await withClock(at("15:00"), async () => {
  await setDemoVariant("running");
  return todayAt(at("15:00"));
});

// Everything that is not about the clock, read at 15:00 on the base.
const world = await withClock(at("15:00"), async () => {
  await setDemoVariant("base");
  await recomputeScores(sql);
  const graph = await getGraph(sql, at("15:00"));
  const doneAt = await sql<{ intent_id: string; t: Date }[]>`
    SELECT intent_id, max(t) AS t FROM commitment WHERE kind = 'task_done' GROUP BY intent_id`;
  const habitDone = await sql<{ intent_id: string; t: Date }[]>`
    SELECT intent_id, t FROM commitment WHERE kind = 'habit_completion' ORDER BY t`;
  const system = await sql<{ id: string; title: string }[]>`SELECT id, title FROM intent WHERE system ORDER BY title`;
  const entities = (await entityNames(sql)).map((e) => ({ id: e.id, name: e.name }));

  const lists = Object.fromEntries(
    await Promise.all(PEOPLE_FILTERS.map(async (f) => [f, await listPeople(sql, { filter: f, limit: 200, birthdayWindow: 30 })] as const)),
  );
  const everyone = await listPeople(sql, { filter: "all", limit: 500 });
  const groups = lists.groups?.people ?? [];
  const people = [...everyone.people, ...groups.filter((g) => !everyone.people.some((p) => p.id === g.id))];
  const hubs: Record<string, unknown> = {};
  for (const p of people) {
    const entity = await showEntity(sql, p.id);
    const page = entity.page ? await readPage(sql, entity.page) : null;
    hubs[p.id] = {
      entity,
      page: page ? { slug: page.slug, title: page.title, body: page.body, via: page.via, written_at: page.written_at } : null,
    };
  }

  const pages = await listPages(sql);
  const bodies = new Map((await sql<{ slug: string; body: string }[]>`SELECT slug, body FROM page`).map((p) => [p.slug, p.body]));
  return {
    nodes: graph.nodes.map((n) => ({ ...n, done_at: doneAt.find((d) => d.intent_id === n.id)?.t.toISOString() ?? null })),
    habitDone: habitDone.map((h) => ({ intent_id: h.intent_id, t: h.t.toISOString() })),
    system: system.map((s) => ({ id: s.id, title: s.title })),
    entities,
    people,
    peopleLists: Object.fromEntries(Object.entries(lists).map(([f, l]) => [f, l.people.map((p) => p.id)])),
    hubs,
    pages: pages.map((p) => ({ ...p, body: bodies.get(p.slug) ?? "" })),
  };
});

// The morning, on the base: the brief, nothing running.
const morning = await todayAt(at("09:00"));

// Back on the base, at the clock it was loaded at, for whoever reads the test database next.
await withClock(at("15:00"), () => setDemoVariant("base"));

const snapshot = {
  about: "The demo set (seeds/demo/) as LoR's read paths return it, frozen by prototype/dashboard/snapshot.ts. Made-up data. Do not edit by hand: run the script.",
  day,
  timezone: tz,
  scenes: { afternoon: { at: at("15:00").toISOString(), ...afternoon }, morning: { at: at("09:00").toISOString(), ...morning } },
  ...world,
};

// Ids are uuids from the demo load and change every run; nothing else in the file is a secret or
// real, but check the one thing that would be: no row may come from outside the demo set.
const text = JSON.stringify(snapshot, null, 1);
writeFileSync(OUT, `${text}\n`);
console.log(JSON.stringify({ ok: true, out: path.relative(process.cwd(), OUT), day, bytes: text.length, ms: Date.now() - started }));
await sql.end({ timeout: 5 });
