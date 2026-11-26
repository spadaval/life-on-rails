import { describe, expect, it } from "vitest";
import { type TodayView, assignGapTasks, renderToday } from "../../src/server/today.js";

// Local hours on Wed 2026-10-14 in New York (EDT = UTC-4).
const at = (hour: number, minute = 0) => Date.UTC(2026, 9, 14, hour + 4, minute);

// Free after noon: 12:00–12:45, 14:00–14:30 and 16:00–22:00, between meetings.
const free = [
  { t: at(16), tEnd: at(22) },
  { t: at(12), tEnd: at(12, 45) },
  { t: at(14), tEnd: at(14, 30) },
  { t: at(13), tEnd: at(13, 10) }, // too short to offer
];
// Due soonest first: the report needs two hours, the call fits anywhere.
const tasks = [
  { title: "Write the report", estimate_min: 120 },
  { title: "Call the dentist back", estimate_min: 15 },
];

describe("free stretches and the tasks they could go to", () => {
  it("gives each gap a different task, chosen for its length, and leaves the rest empty", () => {
    const gaps = assignGapTasks(free, tasks);
    expect(gaps.map((g) => [g.minutes, g.fits])).toEqual([
      [45, "Call the dentist back"], // the report does not fit 45 minutes
      [30, null], // both tasks are taken or too long
      [360, "Write the report"],
    ]);
  });

  it("a task with no estimate fits any gap, but still only one", () => {
    const gaps = assignGapTasks(free, [{ title: "Tidy the desk", estimate_min: null }]);
    expect(gaps.map((g) => g.fits)).toEqual(["Tidy the desk", null, null]);
  });

  it("a goal behind on its hours takes the first stretch, and later stretches still get their own tasks", () => {
    const gaps = assignGapTasks(free, tasks, "Ship Life on Rails");
    expect(gaps.map((g) => [g.minutes, g.fits, g.behind])).toEqual([
      [45, "Ship Life on Rails", true],
      [30, "Call the dentist back", false], // the call was not used up by the first stretch
      [360, "Write the report", false],
    ]);
  });

  it("with no goal behind, no gap is marked behind", () => {
    expect(assignGapTasks(free, tasks, null).every((g) => !g.behind)).toBe(true);
  });

  it("a goal behind on its hours is named as a goal in the brief", () => {
    const view: TodayView = {
      date: "2026-10-14", is_today: true, prev: "2026-10-13", next: "2026-10-15", now: new Date(at(12)).toISOString(),
      timezone: "America/New_York", wake_start: new Date(at(8)).toISOString(), wake_end: new Date(at(22)).toISOString(),
      remaining_s: 10 * 3600, remaining_fraction: 10 / 14, running: null, planned: [], actual: [],
      gaps: assignGapTasks(free, tasks, "Ship Life on Rails"), to_confirm: 0, day_start: new Date(at(0)).toISOString(), day_end: new Date(at(24)).toISOString(),
      confirm_rate: null, coverage: null, phone: null,
    };
    const lines = renderToday(view).split("\n");
    const from = lines.indexOf("## Free stretches left");
    expect(lines.slice(from + 2, from + 4)).toEqual([
      "- 12:00–12:45 (45 min), could go to: Ship Life on Rails (goal behind on its hours)",
      "- 14:00–14:30 (30 min), could go to: Call the dentist back",
    ]);
  });

  it("the brief's free-stretch lines name each gap's own task", () => {
    const view: TodayView = {
      date: "2026-10-14", is_today: true, prev: "2026-10-13", next: "2026-10-15", now: new Date(at(12)).toISOString(),
      timezone: "America/New_York", wake_start: new Date(at(8)).toISOString(), wake_end: new Date(at(22)).toISOString(),
      remaining_s: 10 * 3600, remaining_fraction: 10 / 14, running: null, planned: [], actual: [],
      gaps: assignGapTasks(free, tasks), to_confirm: 0, day_start: new Date(at(0)).toISOString(), day_end: new Date(at(24)).toISOString(),
      confirm_rate: null, coverage: null, phone: null,
    };
    const lines = renderToday(view).split("\n");
    const from = lines.indexOf("## Free stretches left");
    expect(lines.slice(from + 2, from + 5)).toEqual([
      "- 12:00–12:45 (45 min), could go to: Call the dentist back",
      "- 14:00–14:30 (30 min)",
      "- 16:00–22:00 (360 min), could go to: Write the report",
    ]);
  });
});
