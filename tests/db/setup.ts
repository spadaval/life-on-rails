// Every test starts from empty tables plus the seed. Truncation, not a
// rolled-back transaction: the code under test opens its own transactions,
// and some cases need two connections that both commit.
import { afterAll, beforeEach } from "vitest";

export const hasDb = Boolean(process.env.LOR_TEST_DATABASE_URL);

if (hasDb) {
  const { sql } = await import("../../src/db/client.js");
  const { ensureSeed } = await import("../../src/db/seed.js");

  beforeEach(async () => {
    const tables = await sql<{ tablename: string }[]>`SELECT tablename FROM pg_tables WHERE schemaname = 'public'`;
    if (tables.length > 0) {
      await sql.unsafe(`TRUNCATE ${tables.map((t) => `"${t.tablename}"`).join(", ")} RESTART IDENTITY CASCADE`);
    }
    await ensureSeed(sql);
  });

  afterAll(async () => {
    await sql.end({ timeout: 5 });
  });
}
