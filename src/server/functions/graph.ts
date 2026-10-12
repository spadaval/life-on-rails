import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { dto } from "../dto.js";
import { uuidv7 } from "../../shared/ids.js";

const IntentInput = z.object({
  id: z.string().uuid().optional(),
  type: z.enum(["goal", "project", "habit", "task", "metric"]),
  title: z.string().min(1).max(200),
  description: z.string().nullable().optional(),
  status: z.enum(["active", "paused", "later", "done", "archived"]).optional(),
  weight: z.number().min(0).optional(),
  weeklyHourTarget: z.number().nullable().optional(),
  frequency: z.number().int().nullable().optional(),
  frequencyHorizon: z.enum(["day", "week"]).optional(),
  completionWeight: z.number().min(0).max(1).optional(),
  goalMix: z.number().min(0).max(1).optional(),
});

export const getGraph = createServerFn({ method: "GET" })
  .validator((d: unknown) => z.object({}).parse(d))
  .handler(async () => {
    const nodes = dto(await sql`SELECT * FROM intent WHERE archived_at IS NULL ORDER BY type, title`);
    const links = dto(await sql`SELECT * FROM intent_link`);
    const scores = dto(await sql`SELECT DISTINCT ON (intent_id) intent_id, score, as_of FROM score_snapshot ORDER BY intent_id, as_of DESC`);
    return { nodes, links, scores };
  });

export const upsertIntent = createServerFn({ method: "POST" })
  .validator((d: unknown) => IntentInput.parse(d))
  .handler(async ({ data }) => {
    if (data.id) {
      await sql`UPDATE intent SET title = ${data.title}, description = ${data.description ?? null},
        status = ${data.status ?? "active"}, weight = ${data.weight ?? 1},
        weekly_hour_target = ${data.weeklyHourTarget ?? null}, frequency = ${data.frequency ?? null},
        updated_at = now() WHERE id = ${data.id} AND system = false`;
      return { id: data.id };
    }
    const id = uuidv7();
    await sql`INSERT INTO intent (id, type, title, description, status, weight, weekly_hour_target, frequency)
      VALUES (${id}, ${data.type}, ${data.title}, ${data.description ?? null},
        ${data.status ?? "active"}, ${data.weight ?? 1}, ${data.weeklyHourTarget ?? null}, ${data.frequency ?? null})`;
    return { id };
  });

export const linkIntents = createServerFn({ method: "POST" })
  .validator((d: unknown) =>
    z.object({ from: z.string().uuid(), to: z.string().uuid(), kind: z.enum(["measures", "blocks", "derived_from"]), weight: z.number().min(0).optional() }).parse(d),
  )
  .handler(async ({ data }) => {
    if (data.from === data.to) throw new Error("cannot link intent to itself");
    const id = uuidv7();
    await sql`INSERT INTO intent_link (id, from_id, to_id, kind, weight)
      VALUES (${id}, ${data.from}, ${data.to}, ${data.kind}, ${data.weight ?? 1})
      ON CONFLICT (from_id, to_id, kind) DO UPDATE SET weight = EXCLUDED.weight`;
    return { id };
  });

export const archiveIntent = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({ id: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    await sql`UPDATE intent SET status = 'archived', archived_at = now()
      WHERE id = ${data.id} AND system = false`;
    return { ok: true };
  });

/** Reset Visibility: active overdue/open tasks+projects → later. Never touches evidence. */
export const resetVisibility = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({ until: z.string().datetime({ offset: true }).optional() }).parse(d))
  .handler(async ({ data }) => {
    const rows = (await sql`UPDATE intent SET status = 'later', snoozed_until = ${data.until ? new Date(data.until) : null}
      WHERE status = 'active' AND system = false AND type IN ('task', 'project')
        AND (due_at IS NOT NULL AND due_at < now())
      RETURNING id`) as { id: string }[];
    return { moved: rows.length };
  });
