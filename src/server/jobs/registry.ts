// The job list. Each build step adds its jobs here.
import { sql } from "../../db/client.js";
import { applyAutoRules, fillWindow } from "../attribution/blocks.js";
import { lockBlocks, syncCalendarFill } from "../attribution/fill.js";
import { env } from "../env.js";
import { syncCalendars } from "../google/calendarSync.js";
import { googleCalendar, googleTasks } from "../google/client.js";
import { syncTasks } from "../google/tasksSync.js";
import { wakeSnoozed } from "../intents.js";
import { recomputeScores, scheduleRecompute, setConfirmRateProvider } from "../scoring/recompute.js";
import { trailingConfirmRate } from "../today.js";

setConfirmRateProvider(trailingConfirmRate);
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
    run: async () => {
      const stats = await syncCalendars(sql, googleCalendar);
      const { from, to } = fillWindow(new Date());
      const fill = await sql.begin(async (tx) => {
        await lockBlocks(tx);
        const f = await syncCalendarFill(tx, from, to);
        return { ...f, auto_committed: await applyAutoRules(tx, from, to) };
      });
      if (fill.auto_committed > 0) scheduleRecompute();
      return { ...stats, fill };
    },
  },
  {
    name: "tasks_poll",
    everyS: env.LOR_TASKS_POLL_S,
    run: async () => {
      const { touched, ...stats } = await syncTasks(sql, googleTasks);
      if (touched.length > 0) scheduleRecompute();
      return stats;
    },
  },
  {
    name: "score_tick",
    everyS: 3600,
    run: async () => ({ woken: await wakeSnoozed(sql), ...(await recomputeScores(sql)) }),
  },
];

/** A job is stale when its last successful run is older than this. */
export function staleAfterMs(job: Pick<Job, "everyS" | "dailyAt">): number {
  if (job.everyS) return job.everyS * 3 * 1000;
  return 26 * 3600_000;
}
