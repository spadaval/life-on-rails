import { describe, expect, it } from "vitest";
import { type ScoreInputs, computeScores } from "../../src/server/scoring/recompute.js";

const LA = "America/Los_Angeles";
const NOW = new Date("2026-10-14T20:00:00Z"); // 13:00 local, Wed
const H = 3600_000;
const at = (daysAgo: number, hour = 10) => new Date(Date.parse("2026-10-14T07:00:00Z") - daysAgo * 24 * H + hour * H);

type Intent = ScoreInputs["intents"][number];
const intent = (id: string, type: Intent["type"], extra: Partial<Intent> = {}): Intent => ({
  id, type, title: id, status: "active", weight: 1, weekly_hour_target: null, frequency: null, frequency_horizon: "day",
  completion_weight: 0.7, goal_mix: 0.7, metric_min: null, metric_max: null, estimate_hours: null,
  created_at: at(30), ...extra,
});
const measures = (from_id: string, to_id: string, weight = 1) => ({ from_id, to_id, kind: "measures" as const, weight });
const spent = (intent_id: string, daysAgo: number, hours: number, hour = 10) => ({
  kind: "time_spent", intent_id, t: at(daysAgo, hour), t_end: new Date(at(daysAgo, hour).getTime() + hours * H), value: hours,
});
const run = (i: Partial<ScoreInputs>) =>
  computeScores({ intents: [], links: [], commitments: [], now: NOW, tz: LA, weeks: 2, trailingConfirmRate: null, ...i });
const scoreOf = (results: ReturnType<typeof computeScores>, id: string) => results.find((r) => r.intentId === id)!;

describe("computeScores", () => {
  it("mixes a goal from its children and its hours, using raw child scores", () => {
    const results = run({
      intents: [
        intent("goal", "goal", { weight: 3, weekly_hour_target: 10 }),
        intent("habit", "habit", { frequency: 1 }),
        intent("metric", "metric"),
      ],
      links: [measures("habit", "goal"), measures("metric", "habit")],
      commitments: [
        ...[0, 1, 2, 3, 4, 5, 6].map((d) => ({ kind: "habit_completion", intent_id: "habit", t: at(d), t_end: null, value: null })),
        { kind: "metric_value", intent_id: "metric", t: at(0), t_end: null, value: 4 },
        { kind: "metric_value", intent_id: "metric", t: at(1), t_end: null, value: 5 },
        spent("goal", 0, 2), spent("goal", 1, 2), spent("goal", 2, 2),
      ],
    });
    expect(scoreOf(results, "metric").score).toBe(0.875);
    // adherence 7/14, quality 0.875, α 0.7
    expect(scoreOf(results, "habit").score).toBe(0.613);
    // children 0.6125, hours 6/20, β 0.7
    expect(scoreOf(results, "goal").score).toBe(0.519);
    const week = (scoreOf(results, "goal").parts.terms as { last_7_days: Record<string, unknown> }).last_7_days;
    expect(week).toMatchObject({ hours: 6, target: 10, adequacy: 0.6, starving: false });
  });

  it("counts overlapping committed time once, and a goal counts its descendants' hours", () => {
    const results = run({
      intents: [intent("goal", "goal", { weight: 2, weekly_hour_target: 4 }), intent("proj", "project", { estimate_hours: 10 }), intent("task", "task")],
      links: [measures("proj", "goal"), measures("task", "proj")],
      commitments: [
        spent("proj", 0, 2, 9), // 09–11
        spent("task", 0, 2, 10), // 10–12 overlaps one hour
      ],
    });
    const goal = scoreOf(results, "goal").parts.terms as { hours_in_window: number; last_7_days: { starving: boolean } };
    expect(goal.hours_in_window).toBe(3);
    expect(goal.last_7_days.starving).toBe(false);
    // The project's own hours term ignores the task's block; the explanation shows both numbers.
    const proj = scoreOf(results, "proj").parts.terms as Record<string, unknown>;
    expect(proj).toMatchObject({ hours_direct: 2, hours_including_children: 3, tasks_done: 0, tasks_total: 1 });
    // 0.6 × 0/1 + 0.4 × 2/10
    expect(scoreOf(results, "proj").score).toBe(0.08);
  });

  it("flags a high-weight goal under half its weekly hours as starving", () => {
    const results = run({
      intents: [intent("big", "goal", { weight: 2, weekly_hour_target: 10 }), intent("small", "goal", { weight: 1, weekly_hour_target: 10 })],
      commitments: [spent("big", 1, 3), spent("small", 1, 3)],
    });
    const starving = (id: string) => (scoreOf(results, id).parts.terms as { last_7_days: { starving: boolean } }).last_7_days.starving;
    expect(starving("big")).toBe(true);
    expect(starving("small")).toBe(false); // weight below 2
  });

  it("silence gives undefined, not zero; decay applies once, on the node shown", () => {
    const results = run({
      intents: [intent("goal", "goal"), intent("metric", "metric"), intent("old", "metric", { created_at: at(40) })],
      links: [measures("metric", "goal"), measures("old", "goal")],
      commitments: [
        { kind: "metric_value", intent_id: "metric", t: at(9), t_end: null, value: 5 },
      ],
    });
    // A metric with no values has no score at all.
    expect(scoreOf(results, "old").score).toBeNull();
    // 9 days silent: raw 1 × exp(-(9-2)/7)
    expect(scoreOf(results, "metric").score).toBe(0.368);
    expect(scoreOf(results, "metric").parts).toMatchObject({ raw: 1, silence_days: 9 });
    // The goal mixes the metric's RAW score (1), then decays once for its own 9 days of silence.
    expect(scoreOf(results, "goal").parts).toMatchObject({ raw: 1, score: 0.368 });
  });

  it("terminates on a cycle of measures links", () => {
    const results = run({
      intents: [intent("a", "goal"), intent("b", "goal")],
      links: [measures("a", "b"), measures("b", "a")],
    });
    expect(results).toHaveLength(2);
  });

  it("a task scores 1 once done, and a project counts its done tasks", () => {
    const results = run({
      intents: [intent("proj", "project"), intent("t1", "task"), intent("t2", "task"), intent("gone", "task", { status: "archived" })],
      links: [measures("t1", "proj"), measures("t2", "proj"), measures("gone", "proj")],
      commitments: [{ kind: "task_done", intent_id: "t1", t: at(0), t_end: null, value: null }],
    });
    expect(scoreOf(results, "t1").parts.raw).toBe(1);
    expect(scoreOf(results, "t2").parts.raw).toBe(0);
    expect(scoreOf(results, "proj").parts).toMatchObject({ raw: 0.5 });
    expect(results.find((r) => r.intentId === "gone")).toBeUndefined();
  });
});
