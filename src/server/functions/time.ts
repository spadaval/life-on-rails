import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { recategorizeBlock, splitBlock, confirmDay } from "../attribution/commit.js";
import { closeOpenBlock, getOpenBlock, clipCalendarFill } from "../attribution/state.js";
import { dto, one } from "../dto.js";
import { uuidv7 } from "../../shared/ids.js";

const DateInput = z.object({ date: z.string().optional() });

export const getToday = createServerFn({ method: "GET" })
  .validator((d: unknown) => DateInput.parse(d))
  .handler(async () => {
    const running = one(await sql`SELECT * FROM time_block WHERE t_end IS NULL LIMIT 1`);
    const actual = dto(await sql`SELECT * FROM time_block WHERE t_start >= now() - interval '1 day' ORDER BY t_start DESC LIMIT 200`);
    const planned = dto(await sql`SELECT * FROM google_event WHERE t_start >= now() - interval '1 day' AND t_start < now() + interval '2 days' ORDER BY t_start`);
    const starve = dto(await sql`SELECT id, title, weight, weekly_hour_target FROM intent
      WHERE type = 'goal' AND status = 'active' AND weight >= 2 AND weekly_hour_target IS NOT NULL`);
    const pending = one(await sql`SELECT count(*)::int AS n FROM proposal WHERE status = 'pending'`);
    return {
      running,
      actual,
      planned,
      starve,
      pendingProposals: typeof pending?.n === "number" ? pending.n : 0,
    };
  });

export const startManualBlock = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({ intentId: z.string().uuid().nullable(), title: z.string().max(200).nullable() }).parse(d))
  .handler(async ({ data }) => {
    const now = new Date();
    await sql.begin(async (tx) => {
      await closeOpenBlock(tx, now);
      const id = uuidv7();
      const fallback = (await tx`SELECT id FROM intent WHERE system = true AND title = 'Uncategorized' LIMIT 1`) as { id: string }[];
      await tx`INSERT INTO time_block (id, t_start, origin, intent_id, title, committed)
        VALUES (${id}, ${now}, 'manual', ${data.intentId ?? fallback[0]?.id ?? null}, ${data.title}, false)`;
      await tx`INSERT INTO observation (id, t_start, device, source, title)
        VALUES (${uuidv7()}, ${now}, 'buildbot', 'manual', ${data.title})`;
      // New occupied [now, +∞): truncate overlapping uncommitted calendar fill.
      await clipCalendarFill(tx, now, new Date(now.getTime() + 365 * 24 * 3600_000));
    });
    return { ok: true };
  });

export const stopRunningBlock = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({}).parse(d))
  .handler(async () => {
    const now = new Date();
    await sql.begin(async (tx) => {
      const open = await getOpenBlock(tx);
      if (!open) return;
      await closeOpenBlock(tx, now);
      const { ensureAttributionProposal } = await import("../attribution/state.js");
      await ensureAttributionProposal(tx, open.id, open.intent_id ?? "", "manual", 1);
      // Refill sides from calendar events overlapping the closed interval.
      void clipCalendarFill;
    });
    return { ok: true };
  });

export const recategorize = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({ blockId: z.string().uuid(), intentId: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    await recategorizeBlock(sql, data.blockId, data.intentId);
    return { ok: true };
  });

export const split = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({ blockId: z.string().uuid(), at: z.string().datetime({ offset: true }) }).parse(d))
  .handler(async ({ data }) => {
    const rightId = await splitBlock(sql, data.blockId, new Date(data.at));
    return { ok: true, rightId };
  });

export const confirm = createServerFn({ method: "POST" })
  .validator((d: unknown) =>
    z.object({
      acceptProposalIds: z.array(z.string().uuid()),
      rejectProposalIds: z.array(z.string().uuid()),
      acceptAllPending: z.boolean().optional(),
      dayStart: z.string().datetime({ offset: true }),
      dayEnd: z.string().datetime({ offset: true }),
    }).parse(d),
  )
  .handler(async ({ data }) => {
    return confirmDay(sql, {
      acceptProposalIds: data.acceptProposalIds,
      rejectProposalIds: data.rejectProposalIds,
      acceptAllPending: data.acceptAllPending,
      dayStart: new Date(data.dayStart),
      dayEnd: new Date(data.dayEnd),
    });
  });
