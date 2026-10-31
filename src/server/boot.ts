// Runs once when the server bundle is loaded by the production entry
// (scripts/serve.mjs sets LOR_RUN_JOBS=1). Under `vite dev` this does nothing,
// so a dev server can never own the clock.
import { sql } from "../db/client.js";
import { ensureSeed } from "../db/seed.js";
import { channelConfigured, runChannelLoop } from "./channel/channel.js";
import { JOBS } from "./jobs/registry.js";
import { registerJobs, startScheduler } from "./jobs/scheduler.js";
import { errorMessage, log } from "./log.js";

declare global {
  // eslint-disable-next-line no-var
  var __lorReady: Promise<void> | undefined;
}

async function start(): Promise<void> {
  try {
    await sql`SELECT 1`;
    await ensureSeed(sql);
  } catch (err) {
    log("error", "database unavailable at boot", { err: errorMessage(err) });
    process.exit(1);
  }
  registerJobs(JOBS);
  await startScheduler();
  if (channelConfigured()) void runChannelLoop(sql);
}

export function boot(): void {
  if (process.env.LOR_RUN_JOBS !== "1" || globalThis.__lorReady) return;
  globalThis.__lorReady = start();
}
