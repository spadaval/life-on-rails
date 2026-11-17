// The demo set's named variants (src/server/demo.ts), each a small change over the same base.
import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { withClock } from "../../src/server/clock.js";
import { DEMO_TIMER_TASK, setDemoVariant } from "../../src/server/demo.js";
import { jobStatuses } from "../../src/server/status.js";
import { loadDemo } from "../../scripts/demo.js";
import { dbDescribe } from "./helpers.js";

const NOW = new Date("2026-11-17T23:00:00Z");

dbDescribe("demo variants", () => {
  it("switches between named variants, each a small change over the same base", async () => {
    await loadDemo({ now: NOW });
    const open = () => sql<{ title: string; paused: boolean }[]>`
      SELECT i.title, b.paused_at IS NOT NULL AS paused FROM time_block b JOIN intent i ON i.id = b.intent_id WHERE b.t_end IS NULL`;
    const flagged = async () => (await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM intent WHERE flagged_at IS NOT NULL`)[0]!.n;
    const failing = async () => (await jobStatuses(NOW)).filter((j) => j.state === "error").length;
    const blocks = async () => (await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM time_block`)[0]!.n;
    const base = await blocks();

    await withClock(NOW, () => setDemoVariant("running"));
    expect(await open()).toEqual([{ title: DEMO_TIMER_TASK, paused: false }]);
    await withClock(NOW, () => setDemoVariant("paused"));
    expect(await open()).toEqual([{ title: DEMO_TIMER_TASK, paused: true }]);
    await withClock(NOW, () => setDemoVariant("noflags"));
    expect(await open()).toEqual([]);
    expect(await flagged()).toBe(0);
    await withClock(NOW, () => setDemoVariant("failing"));
    expect(await flagged()).toBe(3);
    expect(await failing()).toBeGreaterThan(3);
    await withClock(NOW, () => setDemoVariant("base"));
    expect(await failing()).toBe(0);
    expect(await blocks()).toBe(base);
    await expect(withClock(NOW, () => setDemoVariant("nonsense"))).rejects.toThrow(/unknown demo variant/);
  });
});
