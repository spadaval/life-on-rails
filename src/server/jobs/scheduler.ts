// In-process scheduler (spec Decision 13). Owns calendar_poll, overnight_close,
// score_tick inside `pnpm start` / lor-web.service. pnpm dev is UI-only.
// Every run takes pg_advisory_xact_lock(hashtext(name)) first so the systemd
// watchdog POST /internal/jobs/tick racing the timer exits `skipped`.
export interface Job {
  name: "calendar_poll" | "overnight_close" | "score_tick";
  /** Seconds between runs. */
  everyS: number;
  run: () => Promise<Record<string, unknown>>;
}

export interface JobStore {
  /** Returns true when the lock + running-row were acquired. */
  acquire(name: string): Promise<boolean>;
  finish(name: string, status: "ok" | "error", stats: Record<string, unknown>, error?: string): Promise<void>;
}

export function dueJobs(lastRun: Map<string, number>, nowMs: number, jobs: Job[]): Job[] {
  return jobs.filter((j) => nowMs - (lastRun.get(j.name) ?? 0) >= j.everyS * 1000);
}

export async function runJob(store: JobStore, job: Job): Promise<{ ran: boolean; stats?: Record<string, unknown> }> {
  if (!(await store.acquire(job.name))) return { ran: false };
  try {
    const stats = await job.run();
    await store.finish(job.name, "ok", stats);
    return { ran: true, stats };
  } catch (err) {
    await store.finish(job.name, "error", {}, (err as Error).message);
    throw err;
  }
}

export const ACQUIRE_SQL = `
SELECT pg_try_advisory_xact_lock(hashtext($1)) AS locked`;

export const JOB_RUN_INSERT_SQL = `
INSERT INTO job_run (id, name, status, stats)
VALUES (gen_random_uuid(), $1, 'running', '{}')
RETURNING id`;
