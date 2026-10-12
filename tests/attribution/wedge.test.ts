import { describe, expect, it } from "vitest";
import {
  attributionCoverage,
  calendarConfirmRate,
  intersectSeconds,
  subtractSpans,
  totalSeconds,
} from "../../src/server/attribution/intervals.js";
import { coalesceSamples, mergeSessions } from "../../src/server/attribution/segment.js";
import {
  calendarOverlapSuggest,
  globMatch,
  matchRule,
  validateTitleRegex,
} from "../../src/server/attribution/suggest.js";

const H = 3600_000;

describe("subtractSpans (calendar remainder fill)", () => {
  it("clips occupied manual time out of a meeting", () => {
    // Meeting 9-11, manual timer 10-10:30 → fill 9-10 + 10:30-11.
    const out = subtractSpans([{ t: 9 * H, tEnd: 11 * H }], [{ t: 10 * H, tEnd: 10.5 * H }]);
    expect(out).toEqual([
      { t: 9 * H, tEnd: 10 * H },
      { t: 10.5 * H, tEnd: 11 * H },
    ]);
  });
  it("empty remainder when fully covered", () => {
    expect(subtractSpans([{ t: 9 * H, tEnd: 10 * H }], [{ t: 8 * H, tEnd: 11 * H }])).toEqual([]);
  });
});

describe("calendarConfirmRate", () => {
  it("counts a meeting also covered by manual timer once", () => {
    // E = 9-10 (1h). C = 9-10 committed calendar + 9:30-10:30 manual.
    // |C∩E| = 1h, |M\E| = 0.5h manual outside E... manual 9:30-10:30 minus E = 10-10:30 = 0.5h.
    const { rate } = calendarConfirmRate({
      committed: [
        { t: 9 * H, tEnd: 10 * H },
        { t: 9.5 * H, tEnd: 10.5 * H },
      ],
      events: [{ t: 9 * H, tEnd: 10 * H }],
      manual: [{ t: 9.5 * H, tEnd: 10.5 * H }],
    });
    // (1 + 0.5) / (1 + 0.5) = 1
    expect(rate).toBeCloseTo(1, 9);
  });
  it("merge overlapping committed intervals before counting (ms spans)", () => {
    expect(totalSeconds([{ t: 0, tEnd: 10_000 }, { t: 5_000, tEnd: 15_000 }])).toBe(15);
    expect(intersectSeconds([{ t: 0, tEnd: 10_000 }], [{ t: 5_000, tEnd: 20_000 }])).toBe(5);
  });
  it("empty day is undefined (display —)", () => {
    expect(calendarConfirmRate({ committed: [], events: [], manual: [] }).rate).toBeUndefined();
  });
  it("manual-only day counts manual in denominator", () => {
    const { rate } = calendarConfirmRate({
      committed: [{ t: 9 * H, tEnd: 10 * H }],
      events: [],
      manual: [{ t: 9 * H, tEnd: 10 * H }],
    });
    expect(rate).toBeCloseTo(1, 9);
  });
});

describe("attributionCoverage", () => {
  it("unique committed waking seconds / wake window", () => {
    const rate = attributionCoverage(
      [{ t: 9 * H, tEnd: 10 * H }, { t: 9.5 * H, tEnd: 10.5 * H }],
      8 * H,
      22 * H,
    );
    expect(rate).toBeCloseTo(1.5 / 14, 9);
  });
});

describe("segment", () => {
  it("coalesces identical app+title within idle threshold", () => {
    const out = coalesceSamples(
      [
        { app: "Code", title: "lor", tStart: 0, tEnd: 60_000, intentId: null },
        { app: "Code", title: "lor", tStart: 90_000, tEnd: 120_000, intentId: null }, // 30s gap
        { app: "Code", title: "other", tStart: 130_000, tEnd: 140_000, intentId: null },
      ],
      180,
    );
    expect(out).toHaveLength(2);
    expect(out[0]).toMatchObject({ tStart: 0, tEnd: 120_000 });
  });
  it("NULL-safe label equality", () => {
    const out = coalesceSamples(
      [
        { app: null, title: null, tStart: 0, tEnd: 10_000, intentId: null },
        { app: null, title: null, tStart: 20_000, tEnd: 30_000, intentId: null },
      ],
      180,
    );
    expect(out).toHaveLength(1);
  });
  it("merges same-intent sessions within 120s", () => {
    const out = mergeSessions(
      [
        { app: "a", title: "x", tStart: 0, tEnd: 100_000, intentId: "i1" },
        { app: "b", title: "y", tStart: 200_000, tEnd: 300_000, intentId: "i1" }, // 100s gap
        { app: "c", title: "z", tStart: 310_000, tEnd: 320_000, intentId: "i2" },
      ],
    );
    expect(out).toHaveLength(2);
  });
});

describe("suggest", () => {
  const rules = [
    { id: "r1", matchDevice: null, matchApp: "Code", matchTitle: null, matchUrl: null, intentId: "lor", autoApply: false, priority: 1 },
    { id: "r2", matchDevice: null, matchApp: null, matchTitle: "standup|sync", matchUrl: null, intentId: "meet", autoApply: false, priority: 5 },
  ];
  it("rule priority wins; title regex is case-insensitive (~*)", () => {
    const hit = matchRule(rules, { app: "Code", title: "Team Sync", url: null, device: null });
    expect(hit?.id).toBe("r2");
    expect(matchRule(rules, { app: "Code", title: "refactor", url: null, device: null })?.id).toBe("r1");
    expect(matchRule(rules, { app: "Mail", title: "hello", url: null, device: null })).toBeNull();
  });
  it("validates title regex on write", () => {
    expect(validateTitleRegex("standup|sync")).toBe(true);
    expect(validateTitleRegex("([")).toBe(false);
    expect(validateTitleRegex("x".repeat(201))).toBe(false);
  });
  it("glob matching", () => {
    expect(globMatch("Code*", "Code - Insiders")).toBe(true);
    expect(globMatch("https://github.com/*", "https://github.com/org/repo")).toBe(true);
  });
  it("calendar overlap needs ≥50%", () => {
    expect(calendarOverlapSuggest({ blockStart: 0, blockEnd: 100, eventStart: 0, eventEnd: 60, eventIntentId: "m" })).toBe("m");
    expect(calendarOverlapSuggest({ blockStart: 0, blockEnd: 100, eventStart: 0, eventEnd: 40, eventIntentId: "m" })).toBeNull();
  });
});
