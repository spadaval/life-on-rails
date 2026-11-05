// A weekday at 14:55 in New York with the waking day 08:00-22:00.
import type { GoalsInput, Shell, TasksInput, TodayInput } from "../../src/server/trmnl/frame.js";

export const at = (hm: string) => new Date(`2026-10-15T${hm}:00-04:00`);
const iso = (hm: string) => at(hm).toISOString();

const shell = (): Shell => ({ now: at("14:55"), timezone: "America/New_York", date: "2026-10-15", alerts: [], fontCss: "" });

export function sample(over: Partial<TodayInput> = {}): TodayInput {
  return {
    ...shell(),
    wakeStart: at("08:00"),
    wakeEnd: at("22:00"),
    hours: Array.from({ length: 15 }, (_, i) => at(`${String(8 + i).padStart(2, "0")}:00`)),
    blocks: [
      { t_start: iso("08:05"), t_end: iso("09:00"), running: false, ink: null, goal: null },
      { t_start: iso("09:00"), t_end: iso("10:10"), running: false, ink: 0, goal: "Thesis" },
      { t_start: iso("10:15"), t_end: iso("10:30"), running: false, ink: 0, goal: "Thesis" },
      { t_start: iso("11:30"), t_end: iso("11:50"), running: false, ink: 1, goal: "Strength" },
      { t_start: iso("13:20"), t_end: null, running: true, ink: 0, goal: "Thesis" },
    ],
    events: [
      { title: "Lab standup", t_start: iso("10:00"), t_end: iso("10:15") },
      { title: "Supervisor meeting", t_start: iso("16:00"), t_end: iso("17:00") },
      { title: "Dinner with Sam", t_start: iso("19:30"), t_end: iso("21:00") },
    ],
    deadlines: [{ title: "Submit reimbursement form", at: iso("17:00") }],
    one: { kicker: "Running since 13:20", text: "Thesis", why: null },
    ...over,
  };
}

const days = ["2026-10-09", "2026-10-10", "2026-10-11", "2026-10-12", "2026-10-13", "2026-10-14", "2026-10-15"];

export function sampleGoals(over: Partial<GoalsInput> = {}): GoalsInput {
  return {
    ...shell(),
    days,
    goals: [
      { title: "Thesis", ink: 0, hours: [2, 1.5, 0, 0, 3, 1, 2], target: 15, next: { title: "Send chapter 3 outline", due: "2026-10-27", overdue: false } },
      { title: "Strength", ink: 1, hours: [1, 0, 1, 0, 0, 1, 0], target: 4, next: null },
      { title: "Spanish", ink: 2, hours: [0, 0, 0, 0, 0, 0, 0.5], target: 3, next: { title: "Book a tutor", due: "2026-10-12", overdue: true } },
      { title: "Keep in touch", ink: 3, hours: [0, 0, 0, 0, 0, 0, 0], target: 2, next: null },
      { title: "Read fiction", ink: 4, hours: [0.5, 0, 0.75, 0, 0, 1, 0], target: 1, next: null },
      { title: "Find a flat", ink: 4, hours: [0, 0, 0, 0, 0, 0, 0], target: null, next: { title: "Call the agent", due: null, overdue: false } },
    ],
    ...over,
  };
}

export function sampleTasks(over: Partial<TasksInput> = {}): TasksInput {
  const t = (title: string, due: string | null, extra: Partial<TasksInput["tasks"][number]> = {}) =>
    ({ title, due, overdue: due !== null && due < "2026-10-15", waiting: null, flagged: false, ink: null, ...extra });
  return {
    ...shell(),
    tasks: [
      t("Renew car registration", "2026-10-13", { ink: 4 }),
      t("Submit reimbursement form", "2026-10-15", { flagged: true }),
      t("Book flights for Thanksgiving", "2026-10-16", { ink: 0 }),
      t("Renew passport photos", "2026-10-17"),
      t("Lease renewal paperwork", "2026-10-20", { waiting: "Priya" }),
      t("Send chapter 3 outline to the committee", "2026-10-27", { ink: 0 }),
      t("Fix the bike's back brake", null),
      t("Sort the photo backup", null),
      t("Return library books", null),
      t("Call the bank about the card", null),
    ],
    ...over,
  };
}
