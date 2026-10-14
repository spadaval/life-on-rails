import { defineConfig } from "vitest/config";

// `pnpm test`: pure functions only. No database, no network.
export default defineConfig({
  test: {
    include: ["tests/**/*.test.ts"],
    exclude: ["tests/db/**", "node_modules/**", "archive/**"],
  },
});
