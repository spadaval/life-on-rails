import { describe, expect, it } from "vitest";
import { isDue, lastScheduledInstant } from "../../src/server/jobs/scheduler.js";
import { scrubLog } from "../../src/server/log.js";

const LA = "America/Los_Angeles";

describe("isDue", () => {
  const now = new Date("2026-10-14T18:00:00Z"); // 11:00 local

  it("interval jobs wait out their interval from the last attempt", () => {
    expect(isDue({ everyS: 300 }, now, {}, LA)).toBe(true);
    expect(isDue({ everyS: 300 }, now, { lastAttempt: now.getTime() - 299_000 }, LA)).toBe(false);
    expect(isDue({ everyS: 300 }, now, { lastAttempt: now.getTime() - 300_000 }, LA)).toBe(true);
  });

  it("a daily job runs once after its time, and catches up after a missed boot", () => {
    const scheduled = lastScheduledInstant(LA, "01:30", now);
    expect(scheduled.toISOString()).toBe("2026-10-14T08:30:00.000Z");
    // Never ran: due (this is the catch-up after a boot that missed 01:30).
    expect(isDue({ dailyAt: "01:30" }, now, {}, LA)).toBe(true);
    // Ran after today's 01:30: not due again until tomorrow.
    expect(isDue({ dailyAt: "01:30" }, now, { lastOk: scheduled.getTime() + 1000, lastAttempt: scheduled.getTime() + 1000 }, LA)).toBe(false);
    // Yesterday's run does not count for today.
    expect(isDue({ dailyAt: "01:30" }, now, { lastOk: scheduled.getTime() - 3600_000, lastAttempt: scheduled.getTime() - 3600_000 }, LA)).toBe(true);
    // A failed attempt a minute ago is not retried yet.
    expect(isDue({ dailyAt: "01:30" }, now, { lastAttempt: now.getTime() - 60_000 }, LA)).toBe(false);
  });

  it("before today's time, the last scheduled instant is yesterday's", () => {
    const early = new Date("2026-10-14T08:00:00Z"); // 01:00 local
    expect(lastScheduledInstant(LA, "01:30", early).toISOString()).toBe("2026-10-13T08:30:00.000Z");
  });
});

describe("scrubLog", () => {
  it("removes bearer tokens, Google tokens, and webhook URLs", () => {
    const line = 'auth "Bearer abcdef1234567890" tok ya29.a0AfH6SMBxyz_123456 refresh 1//0gABCDEFGHIJKLMNOPQRSTUV url http://127.0.0.1:3773/api/hooks/h_1/tok_secret';
    const out = scrubLog(line);
    expect(out).not.toMatch(/abcdef1234567890|ya29|0gABCDEFGH|tok_secret/);
    expect(scrubLog("subject: Lunch on Friday")).toBe("subject: Lunch on Friday");
  });
});
