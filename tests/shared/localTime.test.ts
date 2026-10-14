import { describe, expect, it } from "vitest";
import {
  addDays, isoWeek, localDayBounds, localDaysBetween, localHm, localTimeOnDay, localYmd, tzOffsetMinutes, weekStart,
} from "../../src/shared/localTime.js";

const LA = "America/Los_Angeles";

describe("localTime", () => {
  it("knows which local day an instant falls on", () => {
    expect(localYmd(LA, new Date("2026-10-14T06:59:59Z"))).toBe("2026-10-13");
    expect(localYmd(LA, new Date("2026-10-14T07:00:00Z"))).toBe("2026-10-14");
    expect(localHm(LA, new Date("2026-10-14T08:30:00Z"))).toBe("01:30");
  });

  it("finds local midnight across both DST changes", () => {
    expect(localDayBounds(LA, "2026-07-01").start.toISOString()).toBe("2026-07-01T07:00:00.000Z");
    expect(localDayBounds(LA, "2026-12-01").start.toISOString()).toBe("2026-12-01T08:00:00.000Z");
    // Fall back (2026-11-01): the day is 25 hours long.
    const fall = localDayBounds(LA, "2026-11-01");
    expect((fall.end.getTime() - fall.start.getTime()) / 3600_000).toBe(25);
    // Spring forward (2026-03-08): 23 hours.
    const spring = localDayBounds(LA, "2026-03-08");
    expect((spring.end.getTime() - spring.start.getTime()) / 3600_000).toBe(23);
  });

  it("places a wall-clock time on a day", () => {
    expect(localTimeOnDay(LA, "2026-10-14", "01:30").toISOString()).toBe("2026-10-14T08:30:00.000Z");
    expect(localTimeOnDay(LA, "2026-12-14", "22:00").toISOString()).toBe("2026-12-15T06:00:00.000Z");
    expect(tzOffsetMinutes(LA, new Date("2026-10-14T12:00:00Z"))).toBe(-420);
  });

  it("does calendar arithmetic", () => {
    expect(addDays("2026-10-31", 1)).toBe("2026-11-01");
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28");
    expect(weekStart("2026-10-14")).toBe("2026-10-12");
    expect(weekStart("2026-10-18")).toBe("2026-10-12");
    expect(isoWeek("2026-10-14")).toBe("2026-W42");
    expect(isoWeek("2026-01-01")).toBe("2026-W01");
    expect(isoWeek("2027-01-01")).toBe("2026-W53");
    expect(localDaysBetween(LA, new Date("2026-10-12T20:00:00Z"), new Date("2026-10-14T08:00:00Z"))).toBe(2);
  });
});
