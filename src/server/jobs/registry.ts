// The job list. Each build step adds its jobs here.
import { sql } from "../../db/client.js";
import { env } from "../env.js";
import { syncCalendars } from "../google/calendarSync.js";
import { googleCalendar, googleTasks } from "../google/client.js";
import { syncTasks } from "../google/tasksSync.js";
import type { Job } from "./scheduler.js";

const startedAt = Date.now();

export const JOBS: Job[] = [
  {
    name: "heartbeat",
    everyS: 300,
    run: async () => ({ uptime_s: Math.round((Date.now() - startedAt) / 1000) }),
  },
  {
    name: "calendar_poll",
    everyS: env.LOR_CALENDAR_POLL_S,
    run: async () => syncCalendars(sql, googleCalendar),
  },
  {
    name: "tasks_poll",
    everyS: env.LOR_TASKS_POLL_S,
    run: async () => {
      const { touched, ...stats } = await syncTasks(sql, googleTasks);
      return stats;
    },
  },
];

/** A job is stale when its last successful run is older than this. */
export function staleAfterMs(job: Pick<Job, "everyS" | "dailyAt">): number {
  if (job.everyS) return job.everyS * 3 * 1000;
  return 26 * 3600_000;
}
