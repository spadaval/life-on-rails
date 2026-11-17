// The demo set (seeds/demo/, scripts/demo.ts) and its variants (src/server/demo.ts).
import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { withClock } from "../../src/server/clock.js";
import { deviceHome, devicePeople, deviceTasks } from "../../src/server/device.js";
import { pageHistory } from "../../src/server/memory/pages.js";
import { listPeople } from "../../src/server/people.js";
import { jobStatuses } from "../../src/server/status.js";
import type { WeatherCache } from "../../src/server/weather.js";
import { loadDemo, refuseUnlessTest } from "../../scripts/demo.js";
import { dbDescribe } from "./helpers.js";

// A Tuesday afternoon in Los Angeles (LOR_TZ in vitest.db.config.ts).
const NOW = new Date("2026-11-17T23:00:00Z");
const weather: WeatherCache = { read: async (place) => ((await place()) ? { temp_c: 14, condition: "Partly cloudy", is_day: true, observed_at: NOW.toISOString() } : null) };

it("refuses any database but life_on_rails_test before its first query", async () => {
  expect(() => refuseUnlessTest("postgres:///life_on_rails?host=/var/run/postgresql")).toThrow(/life_on_rails_test only/);
  expect(() => refuseUnlessTest("postgres:///life_on_rails_dev")).toThrow(/life_on_rails_test only/);
  const untouchable = new Proxy(() => {}, { get: () => { throw new Error("touched the database"); }, apply: () => { throw new Error("touched the database"); } });
  await expect(loadDemo({ db: untouchable as never, databaseUrl: "postgres:///life_on_rails?host=/var/run/postgresql", now: NOW })).rejects.toThrow(/life_on_rails_test only/);
});

dbDescribe("the demo set", () => {
  it("loads a full life in well under 20 seconds, dated from the clock", async () => {
    const r = await loadDemo({ now: NOW });
    expect(r.ms).toBeLessThan(20_000);
    expect(r.counts.goals).toBe(7);
    expect(r.counts.tasks).toBeGreaterThanOrEqual(40);
    expect(r.counts.pages).toBeGreaterThanOrEqual(30);

    const [g] = await sql<{ projects: number; habits: number; flagged: number; waiting: number; later: number; agent: number; google: number; undated: number; overdue: number }[]>`
      SELECT (SELECT count(*)::int FROM intent WHERE type = 'project') AS projects,
             (SELECT count(*)::int FROM intent WHERE type = 'habit') AS habits,
             (SELECT count(*)::int FROM intent WHERE flagged_at IS NOT NULL AND flag_note IS NOT NULL) AS flagged,
             (SELECT count(*)::int FROM intent WHERE status = 'waiting') AS waiting,
             (SELECT count(*)::int FROM intent WHERE status = 'later') AS later,
             (SELECT count(*)::int FROM intent WHERE assignee = 'agent' AND status = 'active') AS agent,
             (SELECT count(*)::int FROM intent WHERE google_task_id IS NOT NULL) AS google,
             (SELECT count(*)::int FROM intent WHERE type = 'task' AND NOT system AND due_at IS NULL AND status = 'active') AS undated,
             (SELECT count(*)::int FROM intent WHERE type = 'task' AND status = 'active' AND due_at < ${NOW}) AS overdue`;
    expect(g).toMatchObject({ flagged: 3, waiting: 3, later: 4, google: 3 });
    expect(g!.projects).toBeGreaterThanOrEqual(7);
    expect(g!.habits).toBeGreaterThanOrEqual(7);
    expect(g!.agent).toBeGreaterThanOrEqual(5);
    expect(g!.undated).toBeGreaterThan(5);
    expect(g!.overdue).toBeGreaterThan(2);

    // Some goals have hours this week, some none.
    const hours = await sql<{ title: string; h: number }[]>`
      SELECT g.title, coalesce(sum(c.value) FILTER (WHERE c.t >= ${new Date(NOW.getTime() - 7 * 86_400_000)}), 0)::float AS h
      FROM intent g
      LEFT JOIN intent_link l ON l.to_id = g.id
      LEFT JOIN commitment c ON c.kind = 'time_spent' AND (c.intent_id = g.id OR c.intent_id = l.from_id)
      WHERE g.type = 'goal' GROUP BY g.title`;
    expect(hours.filter((x) => x.h > 0).length).toBeGreaterThanOrEqual(3);
    expect(hours.filter((x) => x.h === 0).length).toBeGreaterThanOrEqual(2);

    // People: cadences, birthdays within 30 days, a group, organisations.
    const people = await withClock(NOW, () => listPeople(sql, { filter: "all" }));
    expect(people.total).toBeGreaterThanOrEqual(20);
    expect((await withClock(NOW, () => listPeople(sql, { filter: "birthdays", birthdayWindow: 30 }))).people.length).toBeGreaterThanOrEqual(3);
    expect((await withClock(NOW, () => listPeople(sql, { filter: "overdue" }))).people.length).toBeGreaterThan(2);
    expect((await withClock(NOW, () => listPeople(sql, { filter: "groups" }))).people.map((p) => p.name)).toEqual(["Saturday Run Club"]);

    // The system pages carry the demo's text in every version, never the owner's seed text.
    const system = await sql<{ slug: string; versions: number; titles: string[] }[]>`
      SELECT p.slug, count(*)::int AS versions, array_agg(v.title ORDER BY v.version) AS titles
      FROM page p JOIN page_version v ON v.page_id = p.id
      WHERE p.slug IN ('agent', 'me', 'messaging-rules', 'inbox') GROUP BY p.slug ORDER BY p.slug`;
    expect(system).toEqual([
      { slug: "agent", versions: 1, titles: ["Agent"] },
      { slug: "inbox", versions: 1, titles: ["Inbox"] },
      { slug: "me", versions: 1, titles: ["Me"] },
      { slug: "messaging-rules", versions: 1, titles: ["Messaging rules"] },
    ]);
    expect((await sql`SELECT 1 FROM page_version WHERE via = 'seed'`).length).toBe(0);

    // The page with five versions, by the owner and the agent in turn.
    expect((await pageHistory(sql, "topics/lisbon-trip")).map((v) => v.via).sort()).toEqual(["agent", "agent", "ui", "ui", "ui"]);
    const briefs = await sql<{ slug: string }[]>`SELECT slug FROM page WHERE slug LIKE 'briefs/%' ORDER BY slug`;
    expect(briefs.map((b) => b.slug)).toEqual(["briefs/2026-11-16", "briefs/2026-11-17"]);

    // Every job healthy all day.
    expect((await jobStatuses(new Date("2026-11-18T07:00:00Z"))).every((j) => j.state === "ok")).toBe(true);
  });

  it("gives the phone a full /device/home, Tasks and People", async () => {
    await loadDemo({ now: NOW });
    const morning = new Date("2026-11-17T17:00:00Z"); // 09:00 local
    const home = await withClock(morning, () => deviceHome(morning, weather));
    expect(home.one).not.toBeNull();
    expect(home.brief).toMatch(/offline maps spec/);
    expect(home.needs_you).toHaveLength(3);
    expect(home.day.events.length).toBeGreaterThanOrEqual(4);
    expect(home.agent_queue.length).toBeGreaterThanOrEqual(5);
    expect(home.weather).not.toBeNull();
    const tasks = await withClock(morning, () => deviceTasks(morning));
    expect(tasks.tasks.length).toBeGreaterThan(20);
    const people = await withClock(morning, () => devicePeople(morning));
    expect(people.overdue.length).toBeGreaterThan(2);
    expect(people.birthdays.length).toBeGreaterThanOrEqual(3);
  });
});
