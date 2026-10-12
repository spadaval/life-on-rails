import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { dto, one, type Row } from "../dto.js";

export const searchObservations = createServerFn({ method: "GET" })
  .validator((d: unknown) =>
    z.object({
      q: z.string().max(200).optional(),
      from: z.string().datetime({ offset: true }).optional(),
      to: z.string().datetime({ offset: true }).optional(),
      source: z.string().optional(),
      limit: z.number().int().min(1).max(200).optional(),
    }).parse(d),
  )
  .handler(async ({ data }) => {
    const limit = data.limit ?? 50;
    // LIKE search only in v1 (embeddings arrive PR 8).
    const rows = dto(await sql`SELECT id, t_start, t_end, device, source, app, title
      FROM observation
      WHERE (${data.q ?? null} IS NULL OR title ILIKE '%' || ${data.q ?? ""} || '%')
        AND (${data.source ?? null} IS NULL OR source = ${data.source ?? ""})
        AND (${data.from ?? null} IS NULL OR t_start >= ${data.from ? new Date(data.from) : null})
        AND (${data.to ?? null} IS NULL OR t_start < ${data.to ? new Date(data.to) : null})
      ORDER BY t_start DESC LIMIT ${limit}`);
    return rows;
  });

export const getDay = createServerFn({ method: "GET" })
  .validator((d: unknown) => z.object({ date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/) }).parse(d))
  .handler(async ({ data }) => {
    const start = new Date(`${data.date}T00:00:00Z`);
    const end = new Date(start.getTime() + 24 * 3600_000);
    const blocks = dto(await sql`SELECT * FROM time_block WHERE t_start >= ${start} AND t_start < ${end} ORDER BY t_start`);
    const commitments = dto(await sql`SELECT * FROM commitment WHERE t >= ${start} AND t < ${end} ORDER BY t`);
    return { blocks, commitments };
  });

export const getTimeReport = createServerFn({ method: "GET" })
  .validator((d: unknown) =>
    z.object({ from: z.string().datetime({ offset: true }), to: z.string().datetime({ offset: true }) }).parse(d),
  )
  .handler(async ({ data }) => {
    const rows = dto(await sql`SELECT c.intent_id, i.title,
        sum(EXTRACT(EPOCH FROM (c.t_end - c.t))) AS seconds
      FROM commitment c JOIN intent i ON i.id = c.intent_id
      WHERE c.kind = 'time_spent' AND c.t >= ${new Date(data.from)} AND c.t < ${new Date(data.to)}
      GROUP BY c.intent_id, i.title ORDER BY seconds DESC`);
    const unatt = one(await sql`SELECT sum(EXTRACT(EPOCH FROM (t_end - t_start))) AS s
      FROM time_block WHERE committed = false
        AND t_start >= ${new Date(data.from)} AND t_start < ${new Date(data.to)}`);
    const byIntent = rows.map((r) => ({ intentId: String(r.intent_id), title: String(r.title), seconds: Number(r.seconds) }));
    return { byIntent, unattributedS: Number(unatt?.s ?? 0) };
  });

export const explainScore = createServerFn({ method: "GET" })
  .validator((d: unknown) => z.object({ intentId: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    const rows = (await sql`SELECT score, parts, as_of FROM score_snapshot
      WHERE intent_id = ${data.intentId} ORDER BY as_of DESC LIMIT 1`) as unknown as {
      score: number; parts: unknown; as_of: Date;
    }[];
    const row = rows[0];
    if (!row) return { score: null as number | null, asOf: null as string | null, partsJson: "{}" };
    return { score: row.score, asOf: row.as_of.toISOString(), partsJson: JSON.stringify(row.parts ?? {}) };
  });
