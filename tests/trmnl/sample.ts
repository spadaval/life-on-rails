// A weekday at 14:55 in New York with the waking day 08:00-22:00.
import type { FrameInput } from "../../src/server/trmnl/frame.js";

const at = (hm: string) => new Date(`2026-10-15T${hm}:00-04:00`);

export function sample(over: Partial<FrameInput> = {}): FrameInput {
  return {
    now: at("14:55"),
    timezone: "America/New_York",
    date: "2026-10-15",
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
    toConfirm: 4,
    alerts: [],
    fontCss: "",
    ...over,
  };
}
