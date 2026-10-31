// A weekday at 14:55 in New York with the waking day 08:00-22:00.
import type { GoalsInput, Shell, TasksInput, TodayInput } from "../../src/server/trmnl/frame.js";

const at = (hm: string) => new Date(`2026-10-15T${hm}:00-04:00`);

const shell = (): Shell => ({ now: at("14:55"), timezone: "America/New_York", date: "2026-10-15", alerts: [], fontCss: "" });

export function sample(over: Partial<TodayInput> = {}): TodayInput {
  return {
    ...shell(),
    wakeStart: at("08:00"),
    wakeEnd: at("22:00"),
    hours: Array.from({ length: 15 }, (_, i) => at(`${String(8 + i).padStart(2, "0")}:00`)),
    blocks: [
      { t_start: at("08:05").toISOString(), t_end: at("09:00").toISOString(), paused: false, committed: false, bare: true },
      { t_start: at("09:00").toISOString(), t_end: at("11:00").toISOString(), paused: false, committed: true, bare: false },
      { t_start: at("11:30").toISOString(), t_end: at("12:45").toISOString(), paused: false, committed: false, bare: false },
      { t_start: at("13:20").toISOString(), t_end: null, paused: false, committed: false, bare: false },
    ],
    events: [
      { title: "Lab standup", t_start: at("10:00").toISOString() },
      { title: "Supervisor meeting", t_start: at("16:00").toISOString() },
      { title: "Dinner with Sam", t_start: at("19:30").toISOString() },
    ],
    running: { name: "Thesis", since: at("13:20").toISOString() },
    recordedS: 5 * 3600,
    needsYou: ["Reply to Priya about the lease renewal", "Approve the dentist reschedule"],
    brief: "Send the Hawthorne notice",
    ...over,
  };
}

export function sampleGoals(over: Partial<GoalsInput> = {}): GoalsInput {
  return {
    ...shell(),
    goals: [
      { title: "Finish the thesis", hours: 9.5, target: 15, starving: false },
      { title: "Strength training", hours: 3, target: 4, starving: false },
      { title: "Spanish", hours: 0.5, target: 3, starving: true },
      { title: "Keep in touch", hours: 0, target: 2, starving: true },
      { title: "Read more fiction", hours: 2.25, target: 3, starving: false },
    ],
    ...over,
  };
}

export function sampleTasks(over: Partial<TasksInput> = {}): TasksInput {
  return {
    ...shell(),
    tasks: [
      { title: "Renew car registration", due: "2026-10-13", overdue: true, waiting: null, flagged: false },
      { title: "Submit reimbursement form", due: "2026-10-15", overdue: false, waiting: null, flagged: true },
      { title: "Book flights for Thanksgiving", due: "2026-10-16", overdue: false, waiting: null, flagged: false },
      { title: "Renew passport photos", due: "2026-10-17", overdue: false, waiting: null, flagged: false },
      { title: "Lease renewal paperwork", due: "2026-10-20", overdue: false, waiting: "Priya", flagged: false },
      { title: "Send chapter 3 outline to the committee", due: "2026-10-27", overdue: false, waiting: null, flagged: false },
      { title: "Fix the bike's back brake", due: null, overdue: false, waiting: null, flagged: false },
      { title: "Sort the photo backup", due: null, overdue: false, waiting: null, flagged: false },
      { title: "Return library books", due: null, overdue: false, waiting: null, flagged: false },
      { title: "Call the bank about the card", due: null, overdue: false, waiting: null, flagged: false },
    ],
    ...over,
  };
}
