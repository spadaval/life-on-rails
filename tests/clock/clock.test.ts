// The fixed clock is for the test database alone: LOR_FAKE_NOW and `at=` are
// ignored on any other database, the real one above all.
import { describe, expect, it } from "vitest";
import { fixedClock, isTestDatabase, parseClock, requestedAt } from "../../src/server/clock.js";

const TZ = "America/Los_Angeles";
const TEST = "postgres:///life_on_rails_test?host=/var/run/postgresql";
const REAL = "postgres:///life_on_rails?host=/var/run/postgresql";
const real = new Date("2026-11-17T20:00:00Z"); // 12:00 in Los Angeles

describe("the fixed clock", () => {
  it("is ignored on the real database and on any database but life_on_rails_test", () => {
    for (const databaseUrl of [REAL, "postgres:///life_on_rails_dev?host=/var/run/postgresql", "postgres:///life_on_rails_test2", "not a url"]) {
      expect(fixedClock({ databaseUrl, fakeNow: "2026-11-16T09:00", tz: TZ, real })).toBeNull();
      expect(fixedClock({ databaseUrl, at: "09:00", tz: TZ, real })).toBeNull();
      expect(fixedClock({ databaseUrl, fakeNow: "2026-11-16T09:00", at: "15:00", tz: TZ, real })).toBeNull();
    }
    expect(isTestDatabase(REAL)).toBe(false);
    expect(isTestDatabase(TEST)).toBe(true);
  });

  it("takes LOR_FAKE_NOW on the test database, and a tab's at= over it", () => {
    expect(fixedClock({ databaseUrl: TEST, tz: TZ, real })).toBeNull();
    expect(fixedClock({ databaseUrl: TEST, fakeNow: "2026-11-16T09:00", tz: TZ, real })?.toISOString()).toBe("2026-11-16T17:00:00.000Z");
    // A time alone is that time on the server clock's own day.
    expect(fixedClock({ databaseUrl: TEST, fakeNow: "2026-11-16T09:00", at: "21:30", tz: TZ, real })?.toISOString()).toBe("2026-11-17T05:30:00.000Z");
    expect(fixedClock({ databaseUrl: TEST, at: "00:05", tz: TZ, real })?.toISOString()).toBe("2026-11-17T08:05:00.000Z");
    // Nonsense in at= leaves the server's clock.
    expect(fixedClock({ databaseUrl: TEST, fakeNow: "2026-11-16T09:00", at: "soon", tz: TZ, real })?.toISOString()).toBe("2026-11-16T17:00:00.000Z");
  });

  it("reads an instant with an offset, a local date and time, or a local time", () => {
    expect(parseClock("2026-11-16T09:00:00-08:00", TZ, real)?.toISOString()).toBe("2026-11-16T17:00:00.000Z");
    expect(parseClock("2026-11-16T17:00:00Z", TZ, real)?.toISOString()).toBe("2026-11-16T17:00:00.000Z");
    expect(parseClock("2026-07-01T09:00", TZ, real)?.toISOString()).toBe("2026-07-01T16:00:00.000Z");
    expect(parseClock("2026-11-17T09:00:47", TZ, real)?.toISOString()).toBe("2026-11-17T17:00:47.000Z");
    expect(parseClock("9:00", TZ, real)?.toISOString()).toBe("2026-11-17T17:00:00.000Z");
    expect(parseClock("", TZ, real)).toBeNull();
    expect(parseClock("2026-11-16", TZ, real)).toBeNull();
  });

  it("takes at= from the request, or from the page that made the call", () => {
    expect(requestedAt(new Request("http://x/?at=09:00"))).toBe("09:00");
    expect(requestedAt(new Request("http://x/_serverFn/abc", { headers: { referer: "http://x/tasks?at=15%3A00" } }))).toBe("15:00");
    expect(requestedAt(new Request("http://x/tasks"))).toBeNull();
  });
});
