import { defineConfig } from "drizzle-kit";

export default defineConfig({
  schema: "./src/db/schema.ts",
  out: "./drizzle",
  dialect: "postgresql",
  dbCredentials: {
    // Generate-only in CI/dev without a live DB; migrate.ts uses DATABASE_URL at runtime.
    url: process.env.DATABASE_URL ?? "postgres:///life_on_rails?host=/var/run/postgresql",
  },
});
