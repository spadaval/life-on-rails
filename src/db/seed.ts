// Seed: system intents (Uncategorized, Meetings) + user_profile boot defaults.
// Env = boot defaults; user_profile row = runtime truth (spec precedence).
// Re-runnable: upserts only. --refresh-profile rewrites the profile from env.
import { sql } from "./client.js";
import { uuidv7 } from "../shared/ids.js";

const SYSTEM_INTENTS = [
  { title: "Uncategorized", description: "Default for manual/activity blocks without a rule match" },
  { title: "Meetings", description: "Default for calendar blocks without a rule match" },
];

async function main() {
  const refresh = process.argv.includes("--refresh-profile");
  for (const s of SYSTEM_INTENTS) {
    // Bucket type is 'task' (finite, completable-n/a); callers match on
    // system=true + title, never on type. Not deletable (see Graph guards).
    // Title has no unique constraint (user may name a task "Meetings"), so
    // select-then-insert on (system, title).
    const existing = await sql`SELECT id FROM intent WHERE system = true AND title = ${s.title} LIMIT 1`;
    if (existing.length === 0) {
      await sql`
        INSERT INTO intent (id, type, title, description, status, system)
        VALUES (${uuidv7()}, 'task', ${s.title}, ${s.description}, 'active', true)`;
    }
  }
  const profile = {
    timezone: process.env.LOR_TZ ?? "America/Los_Angeles",
    wakeStart: process.env.LOR_WAKE_START ?? "08:00",
    wakeEnd: process.env.LOR_WAKE_END ?? "22:00",
    idleThresholdS: Number(process.env.LOR_IDLE_THRESHOLD_S ?? 180),
    xaiModel: process.env.XAI_MODEL ?? "grok-4.7",
  };
  if (refresh) {
    await sql`
      INSERT INTO user_profile (id, timezone, wake_start, wake_end, idle_threshold_s, xai_model)
      VALUES (true, ${profile.timezone}, ${profile.wakeStart}, ${profile.wakeEnd}, ${profile.idleThresholdS}, ${profile.xaiModel})
      ON CONFLICT (id) DO UPDATE SET
        timezone = EXCLUDED.timezone, wake_start = EXCLUDED.wake_start,
        wake_end = EXCLUDED.wake_end, idle_threshold_s = EXCLUDED.idle_threshold_s,
        xai_model = EXCLUDED.xai_model`;
  } else {
    await sql`
      INSERT INTO user_profile (id, timezone, wake_start, wake_end, idle_threshold_s, xai_model)
      VALUES (true, ${profile.timezone}, ${profile.wakeStart}, ${profile.wakeEnd}, ${profile.idleThresholdS}, ${profile.xaiModel})
      ON CONFLICT (id) DO NOTHING`;
  }
  console.log("seed ok (system intents + user_profile)");
  await sql.end();
}

await main();
