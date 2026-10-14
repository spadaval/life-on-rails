// In-process scheduler. Started once, from the production server entry only.
// One process owns the clock, so an in-memory flag is all the exclusion a job
// needs; if the process dies, systemd restarts it.
import { sql } from "../../db/client.js";
import { uuidv7 } from "../../shared/ids.js";
import { localTimeOnDay, localYmd, addDays } from "../../shared/localTime.js";
import { env } from "../env.js";
import { errorMessage, log } from "../log.js";

export interface Job {
  name: string;
  /** Run every N seconds ... */
  everyS?: number;
  /** ... or once a day at this local wall-clock time ("01:30"). */
  dailyAt?: string;
  /**
   * Checked before a scheduled run; false means "nothing to do", and no run is
   * recorded. A job with neither everyS nor dailyAt runs only when asked.
   */
  shouldRun?: () => Promise<boolean>;
  run: () => Promise<Record<string, unknown>>;
}

const TICK_MS = 15_000;
const DAILY_RETRY_MS = 15 * 60_000;

const jobs = new Map<string, Job>();
const running = new Set<string>();
const lastOk = new Map<string, number>();
const lastAttempt = new Map<string, number>();
const lastFailure = new Map<string, string>();
let timer: NodeJS.Timeout | null = null;

/** The most recent instant a daily job was scheduled for, at or before `now`. */
export function lastScheduledInstant(tz: string, hm: string, now: Date): Date {
  const today = localTimeOnDay(tz, localYmd(tz, now), hm);
  return today.getTime() <= now.getTime() ? today : localTimeOnDay(tz, addDays(localYmd(tz, now), -1), hm);
}

export function isDue(
  job: Pick<Job, "everyS" | "dailyAt">,
  now: Date,
  state: { lastOk?: number; lastAttempt?: number },
  tz: string,
): boolean {
  const attempt = state.lastAttempt ?? 0;
  if (job.dailyAt) {
    const scheduled = lastScheduledInstant(tz, job.dailyAt, now).getTime();
    if ((state.lastOk ?? 0) >= scheduled) return false;
    return now.getTime() - attempt >= DAILY_RETRY_MS;
  }
  if (job.everyS) return now.getTime() - attempt >= job.everyS * 1000;
  return false;
}

export type RunOutcome = "ok" | "error" | "skipped";

/** Run a job now. A run already in flight makes this a no-op (`skipped`). */
export async function runJob(name: string): Promise<RunOutcome> {
  const job = jobs.get(name);
  if (!job) throw new Error(`unknown job: ${name}`);
  if (running.has(name)) return "skipped";
  running.add(name);
  const id = uuidv7();
  const started = Date.now();
  lastAttempt.set(name, started);
  try {
    await sql`INSERT INTO job_run (id, name, status) VALUES (${id}, ${name}, 'running')`;
    const stats = await job.run();
    await sql`UPDATE job_run SET status = 'ok', finished_at = now(), stats = ${sql.json(stats as never)} WHERE id = ${id}`;
    lastOk.set(name, started);
    lastFailure.delete(name);
    return "ok";
  } catch (err) {
    const message = errorMessage(err);
    // The same failure every five minutes is one line in the log, not hundreds.
    if (lastFailure.get(name) !== message) {
      log("error", "job failed", { job: name, err: message, duration_ms: Date.now() - started });
      lastFailure.set(name, message);
    }
    await sql`UPDATE job_run SET status = 'error', finished_at = now(), error = ${message} WHERE id = ${id}`.catch(() => {});
    return "error";
  } finally {
    running.delete(name);
  }
}

async function tick(): Promise<void> {
  const now = new Date();
  for (const job of jobs.values()) {
    const state = { lastOk: lastOk.get(job.name), lastAttempt: lastAttempt.get(job.name) };
    if (running.has(job.name) || !isDue(job, now, state, env.LOR_TZ)) continue;
    if (job.shouldRun) {
      const go = await job.shouldRun().catch(() => false);
      if (!go) {
        // Not a failure and not a run: look again at the next interval.
        if (!job.dailyAt) lastAttempt.set(job.name, now.getTime());
        continue;
      }
    }
    void runJob(job.name);
  }
}

export function registerJobs(list: Job[]): void {
  for (const job of list) jobs.set(job.name, job);
}

export function registeredJobs(): Job[] {
  return [...jobs.values()];
}

export async function startScheduler(): Promise<void> {
  if (timer) return;
  // One process: a row still marked running belongs to a process that died.
  await sql`UPDATE job_run SET status = 'error', finished_at = now(), error = 'abandoned'
    WHERE status = 'running'`;
  const rows = await sql<{ name: string; last: Date }[]>`
    SELECT name, max(started_at) AS last FROM job_run WHERE status = 'ok' GROUP BY name`;
  for (const r of rows) {
    lastOk.set(r.name, r.last.getTime());
    lastAttempt.set(r.name, r.last.getTime());
  }
  timer = setInterval(() => void tick(), TICK_MS);
  timer.unref?.();
  void tick();
  log("info", "scheduler started", { jobs: [...jobs.keys()] });
}

export function stopScheduler(): void {
  if (timer) clearInterval(timer);
  timer = null;
}
