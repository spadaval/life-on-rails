// Applies migrations to the test database once per run.
export default async function setup(): Promise<void> {
  const url = process.env.LOR_TEST_DATABASE_URL;
  if (!url) return;
  if (!/test/.test(url)) throw new Error(`refusing to run database tests against ${url}: the name must contain "test"`);
  process.env.DATABASE_URL = url;
  const { runMigrations } = await import("../../src/db/migrate.js");
  await runMigrations(url);
}
