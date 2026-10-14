// pnpm db:seed seeds/<name>.json
//
// Loads a graph seed: nodes, links, and attribution rules, in one transaction.
// Safe to run again: a node whose type and title already exist is reused.
import { readFileSync } from "node:fs";
import { z } from "zod";
import { sql } from "../src/db/client.js";
import { systemIntentId } from "../src/db/seed.js";
import { uuidv7 } from "../src/shared/ids.js";
import { validateTitleRegex } from "../src/server/attribution/suggest.js";
import { recomputeScores } from "../src/server/scoring/recompute.js";

const Seed = z.object({
  name: z.string(),
  nodes: z.array(
    z.object({
      key: z.string(),
      type: z.enum(["goal", "project", "habit", "task", "metric"]),
      title: z.string(),
      weight: z.number().optional(),
      weekly_hour_target: z.number().optional(),
      frequency: z.number().int().optional(),
      frequency_horizon: z.enum(["day", "week"]).optional(),
      metric_kind: z.string().optional(),
      metric_min: z.number().optional(),
      metric_max: z.number().optional(),
      estimate_hours: z.number().optional(),
    }),
  ),
  links: z.array(z.object({ from: z.string(), to: z.string(), kind: z.enum(["measures", "blocks", "derived_from"]), weight: z.number().optional() })).default([]),
  rules: z.array(z.object({ match_app: z.string().optional(), match_title: z.string().optional(), match_url: z.string().optional(), intent_key: z.string(), auto_apply: z.boolean().optional() })).default([]),
});

const file = process.argv[2];
if (!file) {
  console.error("usage: pnpm db:seed seeds/<name>.json");
  process.exit(2);
}
const seed = Seed.parse(JSON.parse(readFileSync(file, "utf8")));

await sql.begin(async (tx) => {
  const ids = new Map<string, string>([
    ["meetings", await systemIntentId(tx, "Meetings")],
    ["uncategorized", await systemIntentId(tx, "Uncategorized")],
  ]);
  for (const n of seed.nodes) {
    const existing = await tx<{ id: string }[]>`
      SELECT id FROM intent WHERE type = ${n.type} AND lower(title) = lower(${n.title}) AND status <> 'archived' LIMIT 1`;
    if (existing[0]) {
      ids.set(n.key, existing[0].id);
      continue;
    }
    const id = uuidv7();
    await tx`INSERT INTO intent (id, type, title, weight, weekly_hour_target, frequency, frequency_horizon, metric_kind, metric_min, metric_max, estimate_hours)
      VALUES (${id}, ${n.type}, ${n.title}, ${n.weight ?? 1}, ${n.weekly_hour_target ?? null}, ${n.frequency ?? null},
        ${n.frequency_horizon ?? "day"}, ${n.metric_kind ?? null}, ${n.metric_min ?? null}, ${n.metric_max ?? null}, ${n.estimate_hours ?? null})`;
    ids.set(n.key, id);
  }
  const idOf = (key: string) => {
    const id = ids.get(key);
    if (!id) throw new Error(`seed refers to unknown key ${key}`);
    return id;
  };
  for (const l of seed.links) {
    await tx`INSERT INTO intent_link (id, from_id, to_id, kind, weight) VALUES (${uuidv7()}, ${idOf(l.from)}, ${idOf(l.to)}, ${l.kind}, ${l.weight ?? 1})
      ON CONFLICT (from_id, to_id, kind) DO NOTHING`;
  }
  for (const r of seed.rules) {
    if (r.match_title && !validateTitleRegex(r.match_title)) throw new Error(`bad match_title: ${r.match_title}`);
    const dup = await tx`SELECT id FROM attribution_rule WHERE intent_id = ${idOf(r.intent_key)}
      AND match_app IS NOT DISTINCT FROM ${r.match_app ?? null} AND match_title IS NOT DISTINCT FROM ${r.match_title ?? null}
      AND match_url IS NOT DISTINCT FROM ${r.match_url ?? null}`;
    if (dup.length > 0) continue;
    await tx`INSERT INTO attribution_rule (id, match_app, match_title, match_url, intent_id, auto_apply)
      VALUES (${uuidv7()}, ${r.match_app ?? null}, ${r.match_title ?? null}, ${r.match_url ?? null}, ${idOf(r.intent_key)}, ${r.auto_apply ?? false})`;
  }
});
await recomputeScores(sql);
console.log(`seeded "${seed.name}": ${seed.nodes.length} nodes, ${seed.links.length} links, ${seed.rules.length} rules`);
await sql.end();
