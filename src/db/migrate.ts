// Applies drizzle-generated SQL + the raw-SQL tail the ORM cannot express:
// pgvector extension, HNSW index, and the proposal/time_block/commitment FK
// cycle (spec FK delete matrix). Run: pnpm db:migrate (needs the host runbook
// Postgres 18 + pgvector over the Unix socket; peer auth, no password).
import postgres from "postgres";
import { drizzle } from "drizzle-orm/postgres-js";
import { migrate } from "drizzle-orm/postgres-js/migrator";
import { parseDatabaseUrl } from "./client.js";

const opts = parseDatabaseUrl();
const migrationClient = postgres({
  host: opts.host,
  database: opts.database,
  ...(opts.username ? { username: opts.username } : {}),
  max: 1,
});

const POST_DDL: string[] = [
  // The one step that needs the postgres superuser when runbook hasn't:
  // CREATE EXTENSION IF NOT EXISTS vector;
  `CREATE INDEX IF NOT EXISTS title_embedding_hnsw
     ON title_embedding USING hnsw (embedding vector_cosine_ops)`,
  `ALTER TABLE observation DROP CONSTRAINT IF EXISTS observation_intent_hint_fkey`,
  `ALTER TABLE observation ADD CONSTRAINT observation_intent_hint_fkey
     FOREIGN KEY (intent_hint) REFERENCES intent(id) ON DELETE SET NULL`,
  `ALTER TABLE proposal DROP CONSTRAINT IF EXISTS proposal_time_block_fkey`,
  `ALTER TABLE proposal ADD CONSTRAINT proposal_time_block_fkey
     FOREIGN KEY (time_block_id) REFERENCES time_block(id) ON DELETE CASCADE`,
  `ALTER TABLE proposal DROP CONSTRAINT IF EXISTS proposal_intent_fkey`,
  `ALTER TABLE proposal ADD CONSTRAINT proposal_intent_fkey
     FOREIGN KEY (intent_id) REFERENCES intent(id) ON DELETE SET NULL`,
  `ALTER TABLE commitment DROP CONSTRAINT IF EXISTS commitment_intent_fkey`,
  `ALTER TABLE commitment ADD CONSTRAINT commitment_intent_fkey
     FOREIGN KEY (intent_id) REFERENCES intent(id) ON DELETE RESTRICT`,
  `ALTER TABLE commitment DROP CONSTRAINT IF EXISTS commitment_proposal_fkey`,
  `ALTER TABLE commitment ADD CONSTRAINT commitment_proposal_fkey
     FOREIGN KEY (proposal_id) REFERENCES proposal(id) ON DELETE SET NULL`,
  `ALTER TABLE commitment DROP CONSTRAINT IF EXISTS commitment_block_fkey`,
  `ALTER TABLE commitment ADD CONSTRAINT commitment_block_fkey
     FOREIGN KEY (time_block_id) REFERENCES time_block(id) ON DELETE RESTRICT`,
  `ALTER TABLE time_block DROP CONSTRAINT IF EXISTS time_block_intent_fkey`,
  `ALTER TABLE time_block ADD CONSTRAINT time_block_intent_fkey
     FOREIGN KEY (intent_id) REFERENCES intent(id) ON DELETE RESTRICT`,
  `ALTER TABLE time_block DROP CONSTRAINT IF EXISTS time_block_commitment_fkey`,
  `ALTER TABLE time_block ADD CONSTRAINT time_block_commitment_fkey
     FOREIGN KEY (commitment_id) REFERENCES commitment(id) ON DELETE SET NULL`,
];

async function main() {
  try {
    await migrationClient`CREATE EXTENSION IF NOT EXISTS vector`;
  } catch (err) {
    console.error(
      "CREATE EXTENSION vector failed (run as the postgres superuser once — see spec host runbook):",
      (err as Error).message,
    );
    process.exitCode = 1;
    await migrationClient.end();
    return;
  }
  const db = drizzle(migrationClient);
  await migrate(db, { migrationsFolder: "./drizzle" });
  for (const stmt of POST_DDL) {
    await migrationClient.unsafe(stmt);
  }
  console.log("migrate ok (drizzle + post-DDL)");
  await migrationClient.end();
}

await main();
