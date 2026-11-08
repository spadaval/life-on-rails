import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { deviceHome, devicePeople, deviceState, deviceTasks, deviceTimer, ingest } from "../../src/server/device.js";
import { assignTask, claimTask, completeTask, createIntent, queueTask } from "../../src/server/intents.js";
import { createPage } from "../../src/server/memory/pages.js";
import { findEntity, updateEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { lastContacts } from "../../src/server/people.js";
import { env } from "../../src/server/env.js";
import { dayTimeline } from "../../src/server/memory/timeline.js";
import { addDays, localTimeOnDay, localYmd } from "../../src/shared/localTime.js";
import { dbDescribe } from "./helpers.js";

const obs = (id: string, extra: Record<string, unknown> = {}) => ({
  id,
  t_start: "2026-10-14T16:00:00.000Z",
  t_end: "2026-10-14T16:05:00.000Z",
  device: "pixel-9a",
  source: "android",
  app: "com.google.android.apps.maps",
  calendar_id: null,
  title: "Maps",
  url: null,
  payload: { kind: "app", package: "com.google.android.apps.maps" },
  intent_hint: null,
  ...extra,
});

dbDescribe("device ingest", () => {
  it("stores a batch, is idempotent, extends an observation, and rejects what a device may not send", async () => {
    const a = "6f1c2c1e-0000-4000-8000-000000000001";
    const b = "6f1c2c1e-0000-4000-8000-000000000002";
    const first = await ingest({ device: "pixel-9a", observations: [obs(a), obs(b, { t_end: null })] });
    expect(first).toEqual({ received: 2, stored: 2, rejected: [] });
    const again = await ingest({ device: "pixel-9a", observations: [obs(a), obs(b, { t_end: "2026-10-14T16:20:00.000Z" })] });
    expect(again).toEqual({ received: 2, stored: 0, rejected: [] });
    const [row] = await sql<{ t_end: Date }[]>`SELECT t_end FROM observation WHERE id = ${b}`;
    expect(row!.t_end.toISOString()).toBe("2026-10-14T16:20:00.000Z");

    const bad = await ingest({
      device: "pixel-9a",
      observations: [
        obs("6f1c2c1e-0000-4000-8000-000000000003", { source: "gmail" }),
        obs("6f1c2c1e-0000-4000-8000-000000000004", { device: "someone-else" }),
        { id: "not-a-uuid" },
      ],
    });
    expect(bad.stored).toBe(0);
    expect(bad.rejected.map((r) => r.error)).toEqual([
      "a device cannot send source gmail",
      "device someone-else in a batch from pixel-9a",
      expect.stringContaining("id"),
    ]);
    await expect(ingest({ observations: [] })).rejects.toThrow(/device/);
  });

  it("normalises numbers on calls and texts, so they count toward last in touch", async () => {
    const p = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "phone", value: "+1 415 555 0100" }] });
    await ingest({
      device: "pixel-9a",
      observations: [obs("6f1c2c1e-0000-4000-8000-000000000010", { app: "phone", title: "Priya", payload: { kind: "call", direction: "out", phone: "(415) 555-0100", duration_s: 120 } })],
    });
    const [o] = await sql<{ phone: string }[]>`SELECT payload->>'phone' AS phone FROM observation`;
    expect(o!.phone).toBe("+14155550100");
    expect((await lastContacts(sql, [p.id])).get(p.id)).toMatchObject({ via: "phone" });
    expect((await findEntity(sql, "415 555 0100"))?.id).toBe(p.id);
  });
});

dbDescribe("launches", () => {
  // As the app's Launches.observation builds them (LaunchesTest): an instant, with where it was opened from.
  const launch = (id: string, at: string, payload: Record<string, unknown>, extra: Record<string, unknown> = {}) =>
    obs(id, { t_start: at, t_end: at, app: "com.google.android.youtube", title: "YouTube", payload: { kind: "launch", ...payload }, ...extra });

  it("stores a launch with its source once, shows it on the day's timeline, and leaves the actual lane alone", async () => {
    const at = "2026-10-14T16:02:03.456Z";
    const id = "6f1c2c1e-0000-4000-8000-000000000020";
    const search = launch(id, at, { from: "search", package: "com.google.android.youtube", activity: "com.google.android.youtube.HomeActivity", label: "YouTube", query: "you" });
    expect(await ingest({ device: "pixel-9a", observations: [search] })).toEqual({ received: 1, stored: 1, rejected: [] });
    // A retry is the same row.
    expect(await ingest({ device: "pixel-9a", observations: [search] })).toEqual({ received: 1, stored: 0, rejected: [] });
    await ingest({
      device: "pixel-9a",
      observations: [
        launch("6f1c2c1e-0000-4000-8000-000000000021", "2026-10-14T16:10:00.000Z", { from: "folder", folder: "Dev", package: "com.github.android" }, { app: "com.github.android", title: "GitHub" }),
        launch("6f1c2c1e-0000-4000-8000-000000000022", "2026-10-14T16:11:00.000Z", { from: "agent_card" }, { app: null, title: null, url: "http://buildbot:3847/tasks?id=1" }),
      ],
    });

    const [row] = await sql<{ kind: string; t_start: Date; t_end: Date; from: string; query: string }[]>`
      SELECT kind, t_start, t_end, payload->>'from' AS from, payload->>'query' AS query FROM observation WHERE id = ${id}`;
    expect(row).toEqual({ kind: "launch", t_start: new Date(at), t_end: new Date(at), from: "search", query: "you" });
    // Launches are not app sessions: no fill block comes from them.
    expect(await sql`SELECT 1 FROM time_block`).toHaveLength(0);

    const lines = (await dayTimeline(sql, localYmd(env.LOR_TZ, new Date(at)))).map((r) => [r.source, r.title, r.detail]);
    expect(lines).toEqual([
      ["android", "http://buildbot:3847/tasks?id=1", "opened from agent card"],
      ["android", "GitHub", "opened from folder Dev"],
      ["android", "YouTube", 'opened from search "you"'],
    ]);
  });

  it("is not read as an app session: beside a real session, the phone block links only the session", async () => {
    const session = "6f1c2c1e-0000-4000-8000-000000000030";
    const tap = "6f1c2c1e-0000-4000-8000-000000000031";
    await ingest({
      device: "pixel-9a",
      observations: [
        obs(session, { t_start: "2026-10-14T16:00:00.000Z", t_end: "2026-10-14T16:10:00.000Z", app: "com.google.android.youtube", title: "YouTube", payload: { kind: "app" } }),
        launch(tap, "2026-10-14T16:00:00.000Z", { from: "home", package: "com.google.android.youtube" }),
      ],
    });
    const blocks = await sql<{ origin: string; t_start: Date; t_end: Date; obs: string[] }[]>`
      SELECT b.origin, b.t_start, b.t_end, array_agg(tbo.observation_id::text) AS obs
      FROM time_block b JOIN time_block_observation tbo ON tbo.block_id = b.id GROUP BY b.id`;
    expect(blocks).toEqual([
      { origin: "auto_activity", t_start: new Date("2026-10-14T16:00:00.000Z"), t_end: new Date("2026-10-14T16:10:00.000Z"), obs: [session] },
    ]);
  });
});

dbDescribe("device timer and state", () => {
  it("toggles the timer and reports what is running", async () => {
    const empty = await deviceState("sumeet@example.com", "pixel-9a");
    expect(empty).toMatchObject({ login: "sumeet@example.com", running: null, last_upload: null });
    const started = await deviceTimer({ action: "toggle", title: "from the tile", device: "pixel-9a" }, "me");
    expect(started.running).toMatchObject({ intent: "Uncategorized", title: "from the tile", paused: false });
    const paused = await deviceTimer({ action: "pause" }, "me");
    expect(paused.running?.paused).toBe(true);
    await sql`UPDATE time_block SET t_start = now() - interval '10 minutes', paused_at = now() - interval '2 minutes'`;
    const stopped = await deviceTimer({ action: "toggle" }, "me");
    expect(stopped.running).toBeNull();
    const [b] = await sql<{ minutes: number }[]>`SELECT round(extract(epoch FROM t_end - t_start) / 60)::int AS minutes FROM time_block`;
    expect(b!.minutes).toBe(8); // ended at the pause
  });
});

dbDescribe("device home", () => {
  // Noon in Los Angeles (the test timezone) on 2026-10-14.
  const now = new Date("2026-10-14T19:00:00.000Z");
  const event = async (id: string, t_start: string, t_end: string, extra: Record<string, unknown> = {}) => {
    await sql`INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING`;
    await sql`INSERT INTO google_event ${sql({ calendar_id: "cal", event_id: id, title: id, t_start: new Date(t_start), t_end: new Date(t_end), ...extra })}`;
  };

  it("returns the one thing, the brief, Needs you and the day in one read", async () => {
    const task = await createIntent(sql, { type: "task", title: "Approve the dentist (reschedule)" }, { via: "agent", sourceRef: "t" });
    await sql`UPDATE intent SET flagged_at = ${new Date("2026-10-14T15:00:00Z")}, flag_note = 'they need a yes' WHERE id = ${task.id}`;
    await event("standup", "2026-10-14T16:00:00Z", "2026-10-14T16:30:00Z"); // 09:00, over
    await event("lunch", "2026-10-14T18:30:00Z", "2026-10-14T19:30:00Z"); // under way
    await event("review", "2026-10-14T22:00:00Z", "2026-10-14T23:00:00Z"); // 15:00
    await event("holiday", "2026-10-14T07:00:00Z", "2026-10-15T07:00:00Z", { all_day: true });
    for (let i = 0; i < 6; i++) await event(`late-${i}`, `2026-10-15T0${i}:00:00Z`, `2026-10-15T0${i}:30:00Z`); // evening, past the cap
    await createPage(sql, { slug: "briefs/2026-10-14", title: "Brief", body: "\nPost the notice today. It is due Friday.\nMore below.\n", via: "agent" });

    const home = await deviceHome(now);
    expect(JSON.parse(JSON.stringify(home))).toEqual(home); // JSON-safe as it stands
    expect(home).toMatchObject({ now: now.toISOString(), timezone: "America/Los_Angeles", brief: "Post the notice today. It is due Friday." });
    // The lunch under way wins over the brief and the flag, as on the wall.
    expect(home.one).toMatchObject({ kicker: "Now", text: "lunch" });
    expect(home.needs_you).toEqual([{ id: task.id, title: "Approve the dentist (reschedule)", note: "they need a yes", flagged_at: "2026-10-14T15:00:00.000Z" }]);
    expect(home.day.running).toBeNull();
    expect(home.day.events.map((e) => e.title)).toEqual(["lunch", "review", "late-0", "late-1", "late-2"]);
    expect(home.day.events[1]).toEqual({ title: "review", start: "2026-10-14T22:00:00.000Z", end: "2026-10-14T23:00:00.000Z" });
    expect(Date.parse(home.day.wake_start)).toBeLessThan(now.getTime());
    expect(Date.parse(home.day.wake_end)).toBeGreaterThan(now.getTime());
    expect(home.day.remaining_fraction).toBeGreaterThan(0);
    expect(home.day.remaining_fraction).toBeLessThan(1);
    expect(home.day.remaining_s).toBe(Math.round((Date.parse(home.day.wake_end) - now.getTime()) / 1000));

    // With nothing on, the brief's first line is the one thing.
    await sql`DELETE FROM google_event`;
    expect((await deviceHome(now)).one).toEqual({ kicker: "From this morning's brief", text: "Post the notice today.", why: "It is due Friday." });
  });

  it("shows the running timer, paused or not", async () => {
    await deviceTimer({ action: "start", title: "from the tile" }, "me");
    const home = await deviceHome();
    expect(home.day.running).toMatchObject({ title: "Uncategorized", paused: false, paused_at: null });
    expect(home.one?.kicker).toMatch(/^Running since/);
    expect(home.brief).toBeNull();
    expect(home.needs_you).toEqual([]);
    await deviceTimer({ action: "pause" }, "me");
    expect((await deviceHome()).day.running).toMatchObject({ paused: true, paused_at: expect.any(String) });
  });
});

dbDescribe("device home: the agent's queue", () => {
  it("lists the agent's open tasks, queued ones first and oldest first, then those being worked", async () => {
    const ui = { via: "ui" as const };
    const older = await createIntent(sql, { type: "task", title: "Renew the passport" }, ui);
    const queued = await createIntent(sql, { type: "task", title: "Find a plumber" }, ui);
    const claimed = await createIntent(sql, { type: "task", title: "Draft the lease email" }, ui);
    const owners = await createIntent(sql, { type: "task", title: "Call mum" }, ui);
    const done = await createIntent(sql, { type: "task", title: "Book the flights", assignee: "agent" }, ui);
    await queueTask(sql, older.id);
    await queueTask(sql, queued.id);
    await sql`UPDATE intent SET queued_at = ${new Date("2026-10-14T10:00:00Z")} WHERE id = ${older.id}`;
    await sql`UPDATE intent SET queued_at = ${new Date("2026-10-14T11:00:00Z")} WHERE id = ${queued.id}`;
    await assignTask(sql, claimed.id, "agent");
    await queueTask(sql, claimed.id);
    await claimTask(sql, claimed.id, "thread-42");
    await completeTask(sql, done.id, { via: "agent" });

    const home = await deviceHome();
    expect(JSON.parse(JSON.stringify(home))).toEqual(home);
    // The claimed task changed last, yet the queued ones come first.
    expect(home.agent_queue).toEqual([
      { id: older.id, title: "Renew the passport", queued_at: "2026-10-14T10:00:00.000Z", thread: null },
      { id: queued.id, title: "Find a plumber", queued_at: "2026-10-14T11:00:00.000Z", thread: null },
      { id: claimed.id, title: "Draft the lease email", queued_at: null, thread: "thread-42" },
    ]);
    expect(home.agent_queue.map((t) => t.id)).not.toContain(owners.id);
  });

  it("is empty with nothing given to the agent, and stops at 10", async () => {
    expect((await deviceHome()).agent_queue).toEqual([]);
    for (let i = 0; i < 12; i++) await createIntent(sql, { type: "task", title: `agent ${i}`, assignee: "agent" }, { via: "ui" });
    expect((await deviceHome()).agent_queue).toHaveLength(10);
  });
});

dbDescribe("device tasks", () => {
  // Noon in Los Angeles on 2026-10-14.
  const now = new Date("2026-10-14T19:00:00.000Z");
  const ui = { via: "ui" as const };
  const on = (ymd: string) => localTimeOnDay("America/Los_Angeles", ymd, "12:00");

  it("lists the open tasks as Today's Due soon does: by due date, then title, undated last", async () => {
    const house = await createIntent(sql, { type: "project", title: "House move" }, ui);
    const priya = await upsertEntity(sql, { kind: "person", name: "Priya" });
    await createIntent(sql, { type: "task", title: "Undated" }, ui);
    await createIntent(sql, { type: "task", title: "Pack books", dueAt: on("2026-10-20"), parentId: house.id }, ui);
    await createIntent(sql, { type: "task", title: "Order boxes", dueAt: on("2026-10-20") }, ui);
    const late = await createIntent(sql, { type: "task", title: "Pay rent", dueAt: on("2026-10-12") }, { via: "agent", sourceRef: "t" });
    await createIntent(sql, { type: "task", title: "Hear back on the lease", dueAt: on("2026-10-16"), waitingOnEntityId: priya.id }, ui);
    await sql`UPDATE intent SET flagged_at = now() WHERE id = ${late.id}`;
    // Left out: done, later and archived.
    const finished = await createIntent(sql, { type: "task", title: "Done already", dueAt: on("2026-10-13") }, ui);
    await completeTask(sql, finished.id, ui);
    await createIntent(sql, { type: "task", title: "Later on", status: "later", dueAt: on("2026-10-13") }, ui);
    await createIntent(sql, { type: "task", title: "Archived", status: "archived", dueAt: on("2026-10-13") }, ui);

    const r = await deviceTasks(now);
    expect(JSON.parse(JSON.stringify(r))).toEqual(r);
    expect(r).toMatchObject({ now: now.toISOString(), timezone: "America/Los_Angeles", today: "2026-10-14", open: 5 });
    expect(r.tasks.map((t) => t.title)).toEqual(["Pay rent", "Hear back on the lease", "Order boxes", "Pack books", "Undated"]);
    expect(r.tasks[0]).toEqual({
      id: late.id, title: "Pay rent", due: "2026-10-12", overdue: true, waiting_on: null, parent: null, flagged: true, agent: true, google: false,
    });
    expect(r.tasks[1]).toMatchObject({ due: "2026-10-16", overdue: false, waiting_on: "Priya", flagged: false, agent: false });
    expect(r.tasks[3]).toMatchObject({ parent: "House move", waiting_on: null });
    expect(r.tasks[4]).toMatchObject({ due: null, overdue: false });
  });

  it("caps the list at 100 while open counts every open task", async () => {
    for (let i = 0; i < 103; i++) await createIntent(sql, { type: "task", title: `task ${String(i).padStart(3, "0")}` }, ui);
    const r = await deviceTasks(now);
    expect(r.open).toBe(103);
    expect(r.tasks).toHaveLength(100);
    expect(r.tasks.at(-1)!.title).toBe("task 099");
  });
});

dbDescribe("device people", () => {
  const tz = "America/Los_Angeles";
  const daysAgo = (d: number) => localTimeOnDay(tz, addDays(localYmd(tz, new Date()), -d), "12:00").toISOString();
  const call = (id: string, phone: string, at: string) =>
    obs(id, { t_start: at, t_end: at, app: "phone", title: "call", payload: { kind: "call", direction: "out", phone, duration_s: 60 } });

  it("lists who is past their cadence and whose birthday is within 30 days, without their numbers", async () => {
    const late = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "phone", value: "+1 415 555 0100" }] });
    const fine = await upsertEntity(sql, { kind: "person", name: "Jo", identifiers: [{ kind: "phone", value: "+1 415 555 0101" }] });
    const soon = await upsertEntity(sql, { kind: "person", name: "Sam" });
    const far = await upsertEntity(sql, { kind: "person", name: "Ana" });
    await updateEntity(sql, late.id, { keepInTouchDays: 30 });
    await updateEntity(sql, fine.id, { keepInTouchDays: 30 });
    const today = localYmd(tz, new Date());
    await updateEntity(sql, soon.id, { birthday: `1990-${addDays(today, 5).slice(5)}` });
    await updateEntity(sql, far.id, { birthday: addDays(today, 40).slice(5) });
    await ingest({
      device: "pixel-9a",
      observations: [call("6f1c2c1e-0000-4000-8000-000000000040", "(415) 555-0100", daysAgo(40)), call("6f1c2c1e-0000-4000-8000-000000000041", "(415) 555-0101", daysAgo(2))],
    });

    const r = await devicePeople();
    expect(JSON.parse(JSON.stringify(r))).toEqual(r);
    expect(r.today).toBe(today);
    expect(r.overdue).toEqual([
      { id: late.id, name: "Priya", kind: "person", last_at: daysAgo(40), last_via: "phone", overdue_by: 10, keep_in_touch_days: 30, birthday_in: null, turning: null },
    ]);
    expect(r.birthdays).toEqual([
      { id: soon.id, name: "Sam", kind: "person", last_at: null, last_via: null, overdue_by: null, keep_in_touch_days: null, birthday_in: 5, turning: Number(addDays(today, 5).slice(0, 4)) - 1990 },
    ]);
    for (const p of [...r.overdue, ...r.birthdays]) expect(p).not.toHaveProperty("identifiers");
    expect(JSON.stringify(r)).not.toMatch(/555/);
  });
});

dbDescribe("device routes", () => {
  it("serves the new reads on GET only, and answers POST with its 404", async () => {
    const { Route } = await import("../../src/routes/device/$.js");
    const handlers = (Route.options as unknown as { server: { handlers: Record<"GET" | "POST", (c: { request: Request }) => Promise<Response>> } }).server.handlers;
    const at = (method: "GET" | "POST", path: string) => handlers[method]({ request: new Request(`http://127.0.0.1:3847/device${path}`, { method }) });
    for (const path of ["/tasks", "/people"]) {
      const got = await at("GET", path);
      expect(got.status).toBe(200);
      expect(await got.json()).toHaveProperty("today");
      const posted = await at("POST", path);
      expect(posted.status).toBe(404);
      expect(await posted.json()).toEqual({ error: { code: "no-such-endpoint", message: `POST /device${path}` } });
    }
  });
});
