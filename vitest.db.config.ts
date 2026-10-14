import { existsSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";
import { defineConfig } from "vitest/config";

// `pnpm test:db`: write paths against the real test database. The invariants
// live in Postgres, so a mock store could not fail any of them.
const envFile = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");
if (!process.env.LOR_TEST_DATABASE_URL && existsSync(envFile)) process.loadEnvFile(envFile);

const url = process.env.LOR_TEST_DATABASE_URL ?? "";
if (!url) {
  console.warn("\n*** LOR_TEST_DATABASE_URL is not set: every database test will be SKIPPED ***\n");
}

export default defineConfig({
  test: {
    include: ["tests/db/**/*.test.ts"],
    // One database, so one file at a time.
    fileParallelism: false,
    globalSetup: ["tests/db/globalSetup.ts"],
    setupFiles: ["tests/db/setup.ts"],
    // The app's client reads DATABASE_URL at import; point it at the test database.
    env: { DATABASE_URL: url, LOR_TEST_DATABASE_URL: url, LOR_TZ: "America/Los_Angeles", LOR_AGENT_TOKEN: "test-agent-token-000", LOR_SYNC_RECOMPUTE: "off" },
  },
});
