// Idempotent seed: rows the app cannot run without. Called after every
// migration and at server boot.
import type { Tx } from "./client.js";
import { uuidv7 } from "../shared/ids.js";

export const SYSTEM_INTENTS = [
  { title: "Uncategorized", description: "Default for manual and activity blocks with no rule match" },
  { title: "Meetings", description: "Default for calendar blocks with no rule match" },
] as const;

export async function ensureSeed(tx: Tx): Promise<void> {
  for (const s of SYSTEM_INTENTS) {
    // Titles are not unique (the owner may name a task "Meetings"), so this is
    // select-then-insert on (system, title).
    const existing = await tx`SELECT id FROM intent WHERE system AND title = ${s.title} LIMIT 1`;
    if (existing.length === 0) {
      await tx`INSERT INTO intent (id, type, title, description, system)
        VALUES (${uuidv7()}, 'task', ${s.title}, ${s.description}, true)`;
    }
  }
}

export async function systemIntentId(tx: Tx, title: (typeof SYSTEM_INTENTS)[number]["title"]): Promise<string> {
  const rows = await tx<{ id: string }[]>`SELECT id FROM intent WHERE system AND title = ${title} LIMIT 1`;
  const id = rows[0]?.id;
  if (!id) throw new Error(`system intent ${title} is missing; run pnpm db:migrate`);
  return id;
}
