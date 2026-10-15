import { createServerFn } from "@tanstack/react-start";
import { sql } from "../../db/client.js";
import { jobStatuses } from "../status.js";

/** What every screen's frame shows: the running timer, open tasks, and whether the jobs are healthy. */
export const getShellFn = createServerFn({ method: "GET" }).handler(async () => {
  const [running] = await sql<{ id: string; t_start: Date; paused_at: Date | null; title: string | null; intent: string | null }[]>`
    SELECT b.id, b.t_start, b.paused_at, b.title, i.title AS intent
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id WHERE b.t_end IS NULL`;
  const [tasks] = await sql<{ n: number }[]>`
    SELECT count(*)::int AS n FROM intent WHERE type = 'task' AND status IN ('active', 'waiting') AND NOT system`;
  const jobs = await jobStatuses().catch(() => null);
  const unwell = jobs ? jobs.filter((j) => j.state === "stale" || j.state === "error").map((j) => j.name) : null;
  return {
    running: running
      ? { id: running.id, t_start: running.t_start.toISOString(), paused_at: running.paused_at?.toISOString() ?? null, title: running.title, intent: running.intent }
      : null,
    openTasks: tasks?.n ?? 0,
    unwell,
  };
});
