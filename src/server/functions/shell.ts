import { createServerFn } from "@tanstack/react-start";
import { sql } from "../../db/client.js";
import { clockNow, fixedNow } from "../clock.js";
import { env } from "../env.js";
import { phoneSilentMs } from "../../shared/phoneSilence.js";
import { jobStatuses, phoneLastReport } from "../status.js";

/** What every screen's frame shows: the running timer, open tasks, whether the jobs are healthy and the phone is reporting, and the clock. */
export const getShellFn = createServerFn({ method: "GET" }).handler(async () => {
  const [running] = await sql<{ id: string; t_start: Date; paused_at: Date | null; title: string | null; intent: string | null }[]>`
    SELECT b.id, b.t_start, b.paused_at, b.title, i.title AS intent
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id WHERE b.t_end IS NULL`;
  const [tasks] = await sql<{ n: number }[]>`
    SELECT count(*)::int AS n FROM intent WHERE type = 'task' AND status IN ('active', 'waiting') AND NOT system`;
  const jobs = await jobStatuses().catch(() => null);
  const unwell = jobs ? jobs.filter((j) => j.state === "stale" || j.state === "error").map((j) => j.name) : null;
  const phoneLast = await phoneLastReport().catch(() => null);
  return {
    running: running
      ? { id: running.id, t_start: running.t_start.toISOString(), paused_at: running.paused_at?.toISOString() ?? null, title: running.title, intent: running.intent }
      : null,
    openTasks: tasks?.n ?? 0,
    unwell,
    /** How long the phone has been silent past the threshold, or null (reporting, or never has). */
    phoneSilentMs: phoneSilentMs(phoneLast, clockNow().getTime()),
    /** The fixed time the page reads instead of its own clock (the test database only), or null. */
    clock: fixedNow(),
    /** LoR's time zone (LOR_TZ): the frame's clock reads in it on the server and the page alike. */
    timezone: env.LOR_TZ,
  };
});
