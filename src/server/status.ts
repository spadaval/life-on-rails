// What `/health`, the Capture cockpit, and `lor status` all report.
import { sql } from "../db/client.js";
import type { JsonObject } from "../shared/json.js";
import { type GoogleAuthState, authState } from "./google/auth.js";
import { JOBS, staleAfterMs } from "./jobs/registry.js";

export interface JobStatus {
  name: string;
  schedule: string;
  state: "ok" | "stale" | "error" | "never";
  lastOk: string | null;
  lastRun: string | null;
  lastError: string | null;
  stats: JsonObject;
}

export interface SourceStatus {
  source: string;
  lastArrival: string;
  count: number;
}

export interface Status {
  ok: boolean;
  db: boolean;
  now: string;
  jobs: JobStatus[];
  sources: SourceStatus[];
  google: GoogleAuthState;
}

export async function getStatus(): Promise<Status> {
  const now = new Date();
  try {
    const last = await sql<{ name: string; status: string; started_at: Date; error: string | null; stats: JsonObject }[]>`
      SELECT DISTINCT ON (name) name, status, started_at, error, stats
      FROM job_run ORDER BY name, started_at DESC`;
    const ok = await sql<{ name: string; last: Date }[]>`
      SELECT name, max(started_at) AS last FROM job_run WHERE status = 'ok' GROUP BY name`;
    const sources = await sql<{ source: string; last: Date; n: number }[]>`
      SELECT source, max(ingested_at) AS last, count(*)::int AS n FROM observation GROUP BY source ORDER BY source`;
    const lastBy = new Map(last.map((r) => [r.name, r]));
    const okBy = new Map(ok.map((r) => [r.name, r.last]));
    const jobs: JobStatus[] = JOBS.map((job) => {
      const run = lastBy.get(job.name);
      const lastOk = okBy.get(job.name) ?? null;
      let state: JobStatus["state"] = "never";
      if (run?.status === "error") state = "error";
      else if (lastOk) state = now.getTime() - lastOk.getTime() > staleAfterMs(job) ? "stale" : "ok";
      return {
        name: job.name,
        schedule: job.everyS ? `every ${job.everyS}s` : job.dailyAt ? `daily ${job.dailyAt}` : "on demand",
        state,
        lastOk: lastOk?.toISOString() ?? null,
        lastRun: run?.started_at.toISOString() ?? null,
        lastError: run?.status === "error" ? run.error : null,
        stats: run?.stats ?? {},
      };
    });
    return {
      ok: jobs.every((j) => j.state === "ok" || j.state === "never"),
      db: true,
      now: now.toISOString(),
      jobs,
      sources: sources.map((s) => ({ source: s.source, lastArrival: s.last.toISOString(), count: s.n })),
      google: authState(),
    };
  } catch {
    return { ok: false, db: false, now: now.toISOString(), jobs: [], sources: [], google: authState() };
  }
}
