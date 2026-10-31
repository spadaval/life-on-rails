// Idempotent seed: rows the app cannot run without. Called after every
// migration and at server boot.
import { readFileSync } from "node:fs";
import path from "node:path";
import type { Tx } from "./client.js";
import { uuidv7 } from "../shared/ids.js";

// The pages every run depends on. Ordinary pages once created: the owner and
// the agent edit them like any other, and the seed never overwrites them.
// Bodies are read from src/db/pages/ at the repo root (the service's working
// directory; scripts/serve.mjs sets LOR_ROOT).
const SEED_PAGES = [
  { slug: "agent", title: "The agent", file: "agent.md", inDigest: true },
  { slug: "me", title: "Sumeet", file: "me.md", inDigest: true },
  { slug: "messaging-rules", title: "When to message Sumeet", file: "messaging-rules.md", inDigest: true },
  { slug: "inbox", title: "Inbox", file: "inbox.md", inDigest: false },
];

function seedBody(file: string): string {
  return readFileSync(path.join(process.env.LOR_ROOT ?? process.cwd(), "src/db/pages", file), "utf8");
}

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
  for (const p of SEED_PAGES) {
    const existing = await tx`SELECT id FROM page WHERE slug = ${p.slug} LIMIT 1`;
    if (existing.length > 0) continue;
    const id = uuidv7();
    const body = seedBody(p.file);
    await tx`INSERT INTO page (id, slug, title, head_version, body, in_digest)
      VALUES (${id}, ${p.slug}, ${p.title}, 1, ${body}, ${p.inDigest})`;
    await tx`INSERT INTO page_version (page_id, version, title, body, via) VALUES (${id}, 1, ${p.title}, ${body}, 'seed')`;
  }
}

export async function systemIntentId(tx: Tx, title: (typeof SYSTEM_INTENTS)[number]["title"]): Promise<string> {
  const rows = await tx<{ id: string }[]>`SELECT id FROM intent WHERE system AND title = ${title} LIMIT 1`;
  const id = rows[0]?.id;
  if (!id) throw new Error(`system intent ${title} is missing; run pnpm db:migrate`);
  return id;
}
