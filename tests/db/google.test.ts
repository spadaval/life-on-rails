import { readFileSync } from "node:fs";
import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { syncCalendars } from "../../src/server/google/calendarSync.js";
import { type CalendarPort, GCalendar, GEvent, GTask, GTaskList, type TasksPort } from "../../src/server/google/client.js";
import { syncTasks } from "../../src/server/google/tasksSync.js";
import { dbDescribe } from "./helpers.js";

const fixture = (name: string) =>
  JSON.parse(readFileSync(new URL(`../google/fixtures/${name}.json`, import.meta.url), "utf8")) as { items: Record<string, unknown>[] };

const NOW = new Date("2026-10-14T18:00:00Z");

function calendarPort(events: Record<string, unknown>[], calendars = fixture("calendarList").items): CalendarPort {
  return {
    listCalendars: async () => calendars.map((c) => GCalendar.parse(c)),
    listEvents: async (calendarId) => (calendarId === "owner@example.com" ? events.map((e) => GEvent.parse(e)) : []),
  };
}

function tasksPort(listA: Record<string, unknown>[], lists = fixture("tasklists").items): TasksPort {
  return {
    listTaskLists: async () => lists.map((l) => GTaskList.parse(l)),
    listTasks: async (id) => (id === "listA" ? listA.map((t) => GTask.parse(t)) : []),
  };
}

dbDescribe("calendar poll", () => {
  it("mirrors events, records one observation each, and writes nothing on a second identical poll", async () => {
    const items = fixture("events-primary").items;
    const first = await syncCalendars(sql, calendarPort(items), NOW);
    expect(first).toMatchObject({ calendars: 3, events: 9, changed: 9, cancelled: 0, errors: [] });
    const [counts] = await sql<{ events: number; obs: number }[]>`
      SELECT (SELECT count(*)::int FROM google_event) AS events,
             (SELECT count(*)::int FROM observation WHERE source = 'calendar') AS obs`;
    expect(counts).toEqual({ events: 9, obs: 9 });

    const stamp = await sql<{ updated_at: Date }[]>`SELECT max(updated_at) AS updated_at FROM google_event`;
    const second = await syncCalendars(sql, calendarPort(items), new Date(NOW.getTime() + 300_000));
    expect(second).toMatchObject({ events: 9, changed: 0, cancelled: 0 });
    const after = await sql<{ updated_at: Date }[]>`SELECT max(updated_at) AS updated_at FROM google_event`;
    expect(after[0]!.updated_at.getTime()).toBe(stamp[0]!.updated_at.getTime());
  });

  it("a moved event is an update, and an event that stops arriving is cancelled", async () => {
    const items = fixture("events-primary").items;
    await syncCalendars(sql, calendarPort(items), NOW);
    const moved = items
      .filter((e) => e.id !== "hold1")
      .map((e) => (e.id === "dentist1" ? { ...e, start: { dateTime: "2026-10-14T16:00:00-07:00" }, end: { dateTime: "2026-10-14T17:00:00-07:00" } } : e));
    const stats = await syncCalendars(sql, calendarPort(moved), NOW);
    expect(stats).toMatchObject({ changed: 1, cancelled: 1 });
    const rows = await sql<{ event_id: string; status: string; t_start: Date }[]>`
      SELECT event_id, status, t_start FROM google_event WHERE event_id IN ('dentist1', 'hold1') ORDER BY event_id`;
    expect(rows.map((r) => [r.event_id, r.status, r.t_start.toISOString()])).toEqual([
      ["dentist1", "confirmed", "2026-10-14T23:00:00.000Z"],
      ["hold1", "cancelled", "2026-10-14T20:00:00.000Z"],
    ]);
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM google_event`;
    expect(n!.n).toBe(9);
  });

  it("stores who else attended, so meetings count toward last in touch", async () => {
    await syncCalendars(sql, calendarPort(fixture("events-primary").items), NOW);
    const [e] = await sql<{ attendees: string[] }[]>`SELECT attendees FROM google_event WHERE event_id = 'standup_20261014T160000Z'`;
    expect(e!.attendees).toEqual(["priya@example.com"]); // the owner's own entry is left out
  });

  it("stores what decides fill: RSVP, free/busy, and event type", async () => {
    await syncCalendars(sql, calendarPort(fixture("events-primary").items), NOW);
    const rows = await sql<{ event_id: string; self_response: string | null; transparency: string; event_type: string }[]>`
      SELECT event_id, self_response, transparency, event_type FROM google_event
      WHERE event_id IN ('declined1', 'free1', 'focus1') ORDER BY event_id`;
    expect(rows).toEqual([
      { event_id: "declined1", self_response: "declined", transparency: "opaque", event_type: "default" },
      { event_id: "focus1", self_response: null, transparency: "opaque", event_type: "focusTime" },
      { event_id: "free1", self_response: null, transparency: "transparent", event_type: "default" },
    ]);
  });

  it("an unticked calendar's events stop counting", async () => {
    const items = fixture("events-primary").items;
    await syncCalendars(sql, calendarPort(items), NOW);
    const withoutPrimary = fixture("calendarList").items.filter((c) => c.id !== "owner@example.com");
    const stats = await syncCalendars(sql, calendarPort(items, withoutPrimary), NOW);
    expect(stats.cancelled).toBe(9);
  });
});

dbDescribe("tasks poll", () => {
  const taskRows = () => sql<{ gid: string; title: string; status: string; via: string; due: Date | null; done: number }[]>`
    SELECT i.google_task_id AS gid, i.title, i.status::text AS status, i.via, i.due_at AS due,
           (SELECT count(*)::int FROM commitment c WHERE c.intent_id = i.id AND c.kind = 'task_done') AS done
    FROM intent i WHERE i.google_task_id IS NOT NULL ORDER BY i.google_task_id`;

  it("imports tasks, marks completed ones done with a job commitment, skips deleted and empty rows", async () => {
    const stats = await syncTasks(sql, tasksPort(fixture("tasks-listA").items));
    expect(stats).toMatchObject({ lists: 2, tasks: 5, created: 3, completed: 1, archived: 0 });
    const rows = await taskRows();
    expect(rows.map((r) => [r.gid, r.title, r.status, r.via, r.done])).toEqual([
      ["t-done", "Book dentist", "done", "job", 1],
      ["t-open", "Renew passport", "active", "job", 0],
      ["t-sub", "Get passport photo", "active", "job", 0],
    ]);
    expect(rows[1]!.due?.toISOString()).toBe("2026-10-21T06:59:00.000Z");
    const [c] = await sql<{ via: string; source_ref: string; t: Date }[]>`SELECT via, source_ref, t FROM commitment`;
    expect(c).toMatchObject({ via: "job", source_ref: "google_task:t-done" });
    expect(c!.t.toISOString()).toBe("2026-10-13T21:15:00.000Z");
  });

  it("a second identical poll changes nothing", async () => {
    await syncTasks(sql, tasksPort(fixture("tasks-listA").items));
    const again = await syncTasks(sql, tasksPort(fixture("tasks-listA").items));
    expect(again).toMatchObject({ created: 0, updated: 0, completed: 0, reopened: 0, archived: 0, touched: [] });
  });

  it("ticking a task in Google completes it; unticking reverses that; deleting archives it", async () => {
    const items = fixture("tasks-listA").items;
    await syncTasks(sql, tasksPort(items));
    const ticked = items.map((t) => (t.id === "t-open" ? { ...t, status: "completed", completed: "2026-10-14T17:00:00.000Z" } : t));
    expect(await syncTasks(sql, tasksPort(ticked))).toMatchObject({ completed: 1 });
    expect((await taskRows()).find((r) => r.gid === "t-open")).toMatchObject({ status: "done", done: 1 });

    expect(await syncTasks(sql, tasksPort(items))).toMatchObject({ reopened: 1 });
    expect((await taskRows()).find((r) => r.gid === "t-open")).toMatchObject({ status: "active", done: 0 });

    const deleted = items.map((t) => (t.id === "t-open" ? { ...t, deleted: true } : t));
    expect(await syncTasks(sql, tasksPort(deleted))).toMatchObject({ archived: 1 });
    expect((await taskRows()).find((r) => r.gid === "t-open")).toMatchObject({ status: "archived" });
  });

  it("leaves LoR's own `later` alone while the task is still open in Google, and renames follow", async () => {
    const items = fixture("tasks-listA").items;
    await syncTasks(sql, tasksPort(items));
    await sql`UPDATE intent SET status = 'later' WHERE google_task_id = 't-open'`;
    const renamed = items.map((t) => (t.id === "t-open" ? { ...t, title: "Renew passport (expedited)" } : t));
    expect(await syncTasks(sql, tasksPort(renamed))).toMatchObject({ updated: 1, reopened: 0 });
    expect((await taskRows()).find((r) => r.gid === "t-open")).toMatchObject({ status: "later", title: "Renew passport (expedited)" });
  });

  it("archives the tasks of a list that was deleted in Google", async () => {
    await syncTasks(sql, tasksPort(fixture("tasks-listA").items));
    const onlyB = fixture("tasklists").items.filter((l) => l.id === "listB");
    const stats = await syncTasks(sql, tasksPort([], onlyB));
    expect(stats.archived).toBe(3);
  });
});
