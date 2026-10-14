import { describe, expect, it } from "vitest";
import { type AppSession, type Fix, phoneStats, unknownPlaces, wantedActivity, wantedStays } from "../../src/server/attribution/activity.js";
import type { Rule } from "../../src/server/attribution/suggest.js";

const M = 60_000;
const T0 = Date.parse("2026-10-14T14:00:00Z");
let n = 0;
const s = (startMin: number, lenMin: number, name: string, app = `pkg.${name.toLowerCase().replace(/\W/g, "")}`): AppSession => ({
  id: `s${n++}`, t: T0 + startMin * M, tEnd: T0 + (startMin + lenMin) * M, app, name, device: "pixel",
});
const rule = (id: string, app: string, intentId: string): Rule => ({
  id, matchDevice: null, matchApp: app, matchTitle: null, matchUrl: null, matchPlace: null, intentId, autoApply: false, priority: 0,
});

describe("phone blocks", () => {
  const rules = [rule("r-t3", "pkg.t3code", "lor")];

  it("joins same-label sessions within ten minutes and keeps a short reply inside the block", () => {
    const blocks = wantedActivity([s(0, 8, "T3 Code"), s(9, 1, "WhatsApp"), s(12, 6, "T3 Code"), s(30, 5, "T3 Code")], rules, []);
    expect(blocks.map((b) => [b.intentId, (b.tStart - T0) / M, (b.tEnd - T0) / M])).toEqual([["lor", 0, 18], ["lor", 30, 35]]);
    expect(blocks[0]!.title).toBe("T3 Code 14m · WhatsApp 1m");
    expect(blocks[0]!.ruleId).toBe("r-t3");
  });

  it("splits work from leisure, and leaves unlabelled time unlabelled", () => {
    const blocks = wantedActivity([s(0, 10, "T3 Code"), s(10, 12, "YouTube"), s(23, 4, "Reddit")], rules, []);
    expect(blocks.map((b) => [b.intentId, (b.tStart - T0) / M, (b.tEnd - T0) / M, b.title])).toEqual([
      ["lor", 0, 10, "T3 Code 10m"],
      [null, 10, 27, "YouTube 12m · Reddit 4m"],
    ]);
  });

  it("drops glances and cuts out time something else already claims", () => {
    expect(wantedActivity([s(0, 1, "Settings"), s(5, 1, "Gmail")], rules, [])).toEqual([]);
    const blocks = wantedActivity([s(0, 30, "YouTube")], [], [{ t: T0 + 10 * M, tEnd: T0 + 20 * M }]);
    expect(blocks.map((b) => [(b.tStart - T0) / M, (b.tEnd - T0) / M])).toEqual([[0, 10], [20, 30]]);
  });
});

describe("stays at places", () => {
  const office = { id: "office", name: "Office", lat: 35.8553, lon: -78.8752, radiusM: 150 };
  const fix = (min: number, lat: number, lon: number, accuracyM = 15): Fix => ({ id: `f${min}`, t: T0 + min * M, lat, lon, accuracyM });

  it("runs of fixes at a place are a stay, padded but never past a fix elsewhere", () => {
    const fixes = [fix(0, 35.80, -78.80), fix(20, 35.8552, -78.8751), fix(50, 35.8554, -78.8753), fix(100, 35.8553, -78.8752), fix(110, 35.80, -78.80)];
    expect(wantedStays(fixes, [office]).map((x) => [(x.tStart - T0) / M, (x.tEnd - T0) / M, x.observationIds.length])).toEqual([[10, 110, 3]]);
  });

  it("a long silence ends a stay, and vague fixes say nothing", () => {
    const fixes = [fix(0, 35.8553, -78.8752), fix(200, 35.8553, -78.8752), fix(220, 35.8553, -78.8752, 900)];
    expect(wantedStays(fixes, [office]).map((x) => [(x.tStart - T0) / M, (x.tEnd - T0) / M])).toEqual([[-10, 10], [190, 210]]);
  });

  it("finds where the phone keeps turning up that has no name", () => {
    const fixes = [0, 30, 60, 90].map((m) => fix(m, 35.9, -78.9)).concat([fix(120, 35.8553, -78.8752)]);
    const unknown = unknownPlaces(fixes, [office]);
    expect(unknown).toHaveLength(1);
    expect(unknown[0]).toMatchObject({ lat: 35.9, lon: -78.9, fixes: 4, hours: 1.5 });
  });
});

describe("distraction", () => {
  it("counts screen time, pickups, and glances", () => {
    const stats = phoneStats([s(0, 1, "Settings"), s(10, 20, "YouTube"), s(30.5, 5, "Reddit"), s(50, 0.5, "Gmail")], T0, T0 + 3_600_000);
    expect(stats).toMatchObject({ screen_s: 26.5 * 60, pickups: 3, glances: 2 });
    expect(stats.top[0]).toEqual({ name: "YouTube", s: 1200 });
  });
});
