import { describe } from "vitest";

/** `describe` that skips (loudly, see vitest.db.config.ts) when no test database is configured. */
export const dbDescribe = process.env.LOR_TEST_DATABASE_URL ? describe : describe.skip;
