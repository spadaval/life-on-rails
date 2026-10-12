import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { GOG_EXIT } from "../../src/server/google/gog.js";
import {
  GogEventList,
  allDayBounds,
  mapGogEvent,
  pollWindow,
} from "../../src/server/google/calendarSync.js";
import { syncCalendarFill, type FillStore } from "../../src/server/attribution/fill.js";

const eventsRaw = JSON.parse(readFileSync(new URL("./fixtures/events.json", import.meta.url), "utf8"));

describe("gog adapter", () => {
  it("exit codes match the v0.43.0 contract", () => {
    expect(GOG_EXIT).toMatchObject({ ok: 0, empty: 3, authRequired: 4, notFound: 5, denied: 6, rateLimited: 7, retryable: 8 });
  });
  it("parses fixture events (titles kept; no attendee emails)", () => {
    const list = GogEventList.parse(eventsRaw);
    expect(list).toHaveLength(4);
    const m = list.map((e) => mapGogEvent(e, "primary", "America/Los_Angeles"));
    expect(m[0]).toMatchObject({ eventId: "_8d1j2k3m", title: "Team standup", allDay: false, status: "confirmed" });
    // Recurring instance has a DISTINCT id — the (calendar_id, event_id, t_start) key holds.
    expect(m[1]?.eventId).not.toBe(m[0]?.eventId);
    expect(m[2]).toMatchObject({ allDay: true });
    expect(m[3]).toMatchObject({ status: "cancelled" });
    // Times carry offsets and order correctly.
    expect(m[0]!.tEnd.getTime()).toBeGreaterThan(m[0]!.tStart.getTime());
  });
  it("rejects malformed rows (null, not throw)", () => {
    expect(mapGogEvent({ nope: true }, "primary", "America/Los_Angeles")).toBeNull();
    expect(mapGogEvent({ id: "x" }, "primary", "America/Los_Angeles")).toBeNull();
  });
  it("poll window is local-yesterday start → local +2d end", () => {
    const now = new Date("2026-10-12T12:00:00-07:00");
    const { from, to } = pollWindow(now, "America/Los_Angeles");
    expect(from.toISOString()).toBe("2026-10-11T07:00:00.000Z");
    expect(to.toISOString()).toBe("2026-10-15T07:00:00.000Z");
  });
  it("all-day bounds resolve in profile tz", () => {
    const { tStart, tEnd } = allDayBounds("2026-10-15", "2026-10-16", "America/Los_Angeles");
    expect(tStart.toISOString()).toBe("2026-10-15T07:00:00.000Z");
    expect(tEnd.toISOString()).toBe("2026-10-16T07:00:00.000Z");
  });
});

function memStore(occupied: { t: number; tEnd: number }[] = []): FillStore & { calls: string[]; rows: Map<string, { tStart: number; tEnd: number }[]> } {
  const rows = new Map<string, { tStart: number; tEnd: number }[]>();
  const calls: string[] = [];
  return {
    calls,
    rows,
    occupied: async () => occupied,
    uncommittedForEvent: async (c, e) => (rows.get(`${c}/${e}`) ?? []).map((r, i) => ({ id: `${i}`, ...r, committed: false, intentId: null })),
    replaceUncommitted: async (c, e, frags) => {
      calls.push(`replace:${c}/${e}:${frags.length}`);
      rows.set(`${c}/${e}`, frags);
    },
    deleteUncommitted: async (c, e) => {
      calls.push(`delete:${c}/${e}`);
      rows.delete(`${c}/${e}`);
    },
  };
}

describe("syncCalendarFill", () => {
  const H = 3600_000;
  it("materializes remainder fragments around occupied time", async () => {
    const s = memStore([{ t: 10 * H, tEnd: 10.5 * H }]);
    const r = await syncCalendarFill(s, { calendarId: "p", eventId: "e1", tStart: 9 * H, tEnd: 11 * H, allDay: false, status: "confirmed" }, "meet");
    expect(r).toEqual({ action: "materialized", fragments: 2 });
    expect(s.rows.get("p/e1")).toEqual([
      { tStart: 9 * H, tEnd: 10 * H },
      { tStart: 10.5 * H, tEnd: 11 * H },
    ]);
  });
  it("retracts when fully covered, cancelled, or all-day", async () => {
    const s = memStore([{ t: 8 * H, tEnd: 12 * H }]);
    expect((await syncCalendarFill(s, { calendarId: "p", eventId: "e1", tStart: 9 * H, tEnd: 11 * H, allDay: false, status: "confirmed" }, "m")).action).toBe("retracted");
    expect((await syncCalendarFill(s, { calendarId: "p", eventId: "e2", tStart: 9 * H, tEnd: 10 * H, allDay: false, status: "cancelled" }, "m")).action).toBe("skipped-cancelled-committed-left");
    expect((await syncCalendarFill(s, { calendarId: "p", eventId: "e3", tStart: 9 * H, tEnd: 10 * H, allDay: true, status: "confirmed" }, "m")).action).toBe("skipped-planned-only");
  });
});
