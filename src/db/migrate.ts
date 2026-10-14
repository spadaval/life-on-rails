// Applies the generated SQL in ./drizzle, then the idempotent seed.
// Run: pnpm db:migrate
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { drizzle } from "drizzle-orm/postgres-js";
import { migrate } from "drizzle-orm/postgres-js/migrator";
import { connect } from "./client.js";
import { ensureSeed } from "./seed.js";
import { env } from "../server/env.js";

const MIGRATIONS = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../drizzle");

export async function runMigrations(url: string): Promise<void> {
  const client = connect(url, 1);
  try {
    await migrate(drizzle(client), { migrationsFolder: MIGRATIONS });
    await ensureSeed(client);
  } finally {
    await client.end();
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await runMigrations(env.DATABASE_URL);
  console.log(`migrate ok: ${env.DATABASE_URL}`);
  process.exit(0);
}
