import { describe, expect, it } from "vitest";
import {
  confidence,
  decay,
  descendantSet,
  displayed,
  expectedCompletions,
  goalScore,
  habitScore,
  mergedHours,
  mergeIntervals,
  metricRaw,
  normalizeMetricValue,
  projectScore,
} from "../../src/server/scoring/formulas.js";

const approx = (v: number | undefined, e: number) => expect(v).toBeCloseTo(e, 9);

describe("metric", () => {
  it("uses values, not logging rate (2023 bug)", () => {
    // Five 5/5 answers in 2 weeks: quality 1, not count/5/14.
    approx(metricRaw([5, 5, 5, 5, 5]), 1);
    approx(metricRaw([1, 1]), 0);
    approx(metricRaw([3]), 0.5);
  });
  it("silence is undefined, not zero", () => {
    expect(metricRaw([])).toBeUndefined();
  });
  it("clips out-of-range values", () => {
    expect(normalizeMetricValue(99, 1, 5)).toBe(1);
    expect(normalizeMetricValue(-3, 1, 5)).toBe(0);
  });
});

describe("habit", () => {
  it("mixes adherence and linked metric quality with α", () => {
    // 3x/day → E=42 over 2 weeks; 21 completions → adh 0.5; qual 1.0
    approx(
      habitScore({ completions: 21, frequency: 3, frequencyHorizon: "day", weeks: 2, alpha: 0.7, linkedMetricRaws: [1] }),
      0.7 * 0.5 + 0.3 * 1,
    );
  });
  it("caps over-logging at 1", () => {
    approx(
      habitScore({ completions: 999, frequency: 1, frequencyHorizon: "day", weeks: 2, alpha: 0.7, linkedMetricRaws: [] }),
      1,
    );
  });
  it("omits quality term when no linked metrics (α treated as 1)", () => {
    approx(
      habitScore({ completions: 7, frequency: 1, frequencyHorizon: "day", weeks: 2, alpha: 0.7, linkedMetricRaws: [] }),
      0.5,
    );
  });
  it("skips silent linked metrics without double-punishing", () => {
    approx(
      habitScore({
        completions: 14, frequency: 1, frequencyHorizon: "day", weeks: 2,
        alpha: 0.5, linkedMetricRaws: [undefined, 1],
      }),
      0.5 * 1 + 0.5 * 1,
    );
  });
  it("weekly horizon", () => {
    expect(expectedCompletions(2, "week", 2)).toBe(4);
    expect(expectedCompletions(2, "day", 2)).toBe(28);
  });
});

describe("project", () => {
  it("mixes task % and hours vs estimate", () => {
    approx(projectScore({ tasksDone: 1, tasksTotal: 2, committedHours: 5, estimateHours: 10 }), 0.6 * 0.5 + 0.4 * 0.5);
  });
  it("hours-only when no tasks; tasks-only when no estimate; undefined when empty", () => {
    approx(projectScore({ tasksDone: 0, tasksTotal: 0, committedHours: 12, estimateHours: 10 }), 1);
    approx(projectScore({ tasksDone: 2, tasksTotal: 4, committedHours: 0, estimateHours: null }), 0.5);
    expect(projectScore({ tasksDone: 0, tasksTotal: 0, committedHours: 0, estimateHours: null })).toBeUndefined();
  });
});

describe("intervals", () => {
  it("merges overlapping spans so calendar+manual in one hour counts once", () => {
    const h = 3600_000;
    // Meeting 9:00-10:00 committed + manual timer 9:30-10:30 committed.
    const spans = [
      { t: 9 * h, tEnd: 10 * h },
      { t: 9.5 * h, tEnd: 10.5 * h },
    ];
    expect(mergeIntervals(spans)).toEqual([{ t: 9 * h, tEnd: 10.5 * h }]);
    approx(mergedHours(spans), 1.5);
  });
  it("drops invalid spans", () => {
    expect(mergeIntervals([{ t: 5, tEnd: 5 }, { t: 9, tEnd: 3 }])).toEqual([]);
  });
});

describe("goal", () => {
  it("weighted children mix skips undefined children (2023 div-by-zero bug)", () => {
    approx(goalScore({ children: [{ score: 1, weight: 2 }, { score: undefined, weight: 5 }], hours: 0, weeklyHourTarget: null, weeks: 2, beta: 0.7 }), 1);
    expect(goalScore({ children: [{ score: undefined, weight: 1 }], hours: 0, weeklyHourTarget: null, weeks: 2, beta: 0.7 })).toBeUndefined();
    expect(goalScore({ children: [], hours: 0, weeklyHourTarget: null, weeks: 2, beta: 0.7 })).toBeUndefined();
  });
  it("renormalizes β when a term is missing", () => {
    approx(goalScore({ children: [{ score: 0.4, weight: 1 }], hours: 99, weeklyHourTarget: null, weeks: 2, beta: 0.7 }), 0.4);
    approx(goalScore({ children: [], hours: 5, weeklyHourTarget: 10, weeks: 1, beta: 0.7 }), 0.5);
  });
  it("mixes children and hours with β", () => {
    approx(
      goalScore({ children: [{ score: 1, weight: 1 }], hours: 5, weeklyHourTarget: 10, weeks: 1, beta: 0.7 }),
      0.7 * 1 + 0.3 * 0.5,
    );
  });
  it("descendant walk drops cycles and terminates", () => {
    const set = descendantSet("g", [
      { fromId: "p", toId: "g", kind: "measures" },
      { fromId: "t", toId: "p", kind: "measures" },
      { fromId: "g", toId: "t", kind: "measures" }, // cycle edge back to goal
      { fromId: "x", toId: "g", kind: "blocks" }, // wrong kind, ignored
    ]);
    expect([...set].sort()).toEqual(["g", "p", "t"]);
  });
});

describe("decay applied once", () => {
  it("δ(d): 1 within 2 days, exp after", () => {
    expect(decay(0)).toBe(1);
    expect(decay(2)).toBe(1);
    approx(decay(9), Math.exp(-1));
  });
  it("parent uses child RAW score then decays once (no double decay)", () => {
    const childRaw = metricRaw([5, 5]); // 1
    const parentRaw = goalScore({ children: [{ score: childRaw, weight: 1 }], hours: 0, weeklyHourTarget: null, weeks: 2, beta: 1 });
    // Parent silent 9 days: decay once on parent. Child keeps its own δ elsewhere.
    approx(displayed(parentRaw, 9), Math.exp(-1));
    // NOT displayed(child,9) fed into parent then decayed again (= exp(-2)).
    expect(displayed(parentRaw, 9)).not.toBeCloseTo(Math.exp(-2), 9);
  });
});

describe("confidence", () => {
  it("high needs recent commit + trailing coverage", () => {
    expect(confidence({ lastCommitmentDaysAgo: 0, anyCommitmentInWindow: true, trailingCoverage: 0.7, decayDominated: false })).toBe("high");
    expect(confidence({ lastCommitmentDaysAgo: 0, anyCommitmentInWindow: true, trailingCoverage: 0.2, decayDominated: false })).toBe("medium");
    expect(confidence({ lastCommitmentDaysAgo: 30, anyCommitmentInWindow: true, trailingCoverage: 0.9, decayDominated: false })).toBe("medium");
    expect(confidence({ lastCommitmentDaysAgo: null, anyCommitmentInWindow: false, trailingCoverage: null, decayDominated: true })).toBe("low");
  });
});
