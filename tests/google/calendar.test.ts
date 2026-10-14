import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { fillsActual, mapEvent, pollWindow } from "../../src/server/google/calendarSync.js";
import { GCalendar, GEvent, GTask } from "../../src/server/google/client.js";
import { dueAt } from "../../src/server/google/tasksSync.js";

const LA = "America/Los_Angeles";
const fixture = (name: string) => JSON.parse(readFileSync(new URL(`./fixtures/${name}.json`, import.meta.url), "utf8")) as { items: unknown[] };

const events = fixture("events-primary")
  .items.map((raw) => GEvent.parse(raw))
  .map((e) => mapEvent("owner@example.com", e, LA));

const byId = (id: string) => events.find((e) => e?.eventId === id)!;

describe("calendar mapping", () => {
  it("parses the calendar list and keeps the flags the poll filters on", () => {
    const cals = fixture("calendarList").items.map((c) => GCalendar.parse(c));
    expect(cals.filter((c) => c.selected || c.primary).map((c) => c.summary)).toEqual([
      "owner@example.com", "Family", "Holidays in United States",
    ]);
  });

  it("maps timed events to instants and gives each recurring instance its own id", () => {
    expect(byId("standup_20261014T160000Z")).toMatchObject({ title: "Standup", allDay: false, selfResponse: "accepted", attendees: 2 });
    expect(byId("standup_20261014T160000Z").tStart.toISOString()).toBe("2026-10-14T16:00:00.000Z");
    expect(byId("standup_20261015T160000Z").tStart.toISOString()).toBe("2026-10-15T16:00:00.000Z");
    // Start and end in different zones.
    expect(byId("flight1").tEnd.toISOString()).toBe("2026-10-16T04:20:00.000Z");
  });

  it("maps all-day events to local-day bounds and drops events with no times", () => {
    const allDay = byId("allday1");
    expect(allDay.allDay).toBe(true);
    expect(allDay.tStart.toISOString()).toBe("2026-10-14T07:00:00.000Z");
    expect(allDay.tEnd.toISOString()).toBe("2026-10-16T07:00:00.000Z");
    expect(events.filter((e) => e === null)).toHaveLength(1);
  });

  it("decides which events fill the actual lane", () => {
    const fills = (id: string) => fillsActual(byId(id));
    expect(fills("standup_20261014T160000Z")).toBe(true);
    expect(fills("dentist1")).toBe(true);
    expect(fills("flight1")).toBe(true); // created by Gmail from a booking
    expect(fills("declined1")).toBe(false);
    expect(fills("free1")).toBe(false);
    expect(fills("focus1")).toBe(false);
    expect(fills("allday1")).toBe(false);
    expect(fillsActual({ ...byId("dentist1"), status: "cancelled" })).toBe(false);
  });

  it("polls from the start of local yesterday to the end of local day +2", () => {
    const w = pollWindow(new Date("2026-10-14T18:00:00Z"), LA);
    expect(w.from.toISOString()).toBe("2026-10-13T07:00:00.000Z");
    expect(w.to.toISOString()).toBe("2026-10-17T07:00:00.000Z");
  });
});

describe("tasks mapping", () => {
  it("parses tasks and reads `due` as the end of that local day", () => {
    const tasks = fixture("tasks-listA").items.map((t) => GTask.parse(t));
    expect(tasks.map((t) => [t.id, t.status, t.deleted])).toEqual([
      ["t-open", "needsAction", false], ["t-done", "completed", false], ["t-deleted", "needsAction", true],
      ["t-empty", "needsAction", false], ["t-sub", "needsAction", false],
    ]);
    expect(dueAt("2026-10-20T00:00:00.000Z", LA)?.toISOString()).toBe("2026-10-21T06:59:00.000Z");
    expect(dueAt(undefined, LA)).toBeNull();
  });
});
