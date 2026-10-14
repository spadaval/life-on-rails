import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { type Ymd, addDays, localDayBounds, localTimeOnDay, localYmd } from "../../shared/localTime.js";
import {
  confirmDay, deleteBlock, editBlock, pauseRunningBlock, recategorizeBlock, resumeRunningBlock, splitBlock,
  startManualBlock, stopRunningBlock, uncommitBlock,
} from "../attribution/blocks.js";
import { env } from "../env.js";
import { BadInput } from "../errors.js";
import { getGraph } from "../graph.js";
import { needsOwner } from "../intents.js";
import { resolveDate } from "../memory/day.js";
import { getToday } from "../today.js";

const ui = { via: "ui" as const };
const Hm = z.string().regex(/^\d{2}:\d{2}$/);
const DateInput = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);

/** A wall-clock time on the day being viewed; an early-morning time before the day's blocks means the next day. */
function onDay(date: Ymd, hm: string): Date {
  return localTimeOnDay(env.LOR_TZ, date, hm);
}

export const getTodayFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => z.object({ date: z.string().optional() }).parse(d ?? {}))
  .handler(async ({ data }) => {
    const now = new Date();
    const date = resolveDate(data.date, now);
    const [today, graph] = await Promise.all([getToday(sql, date, now), getGraph(sql, now)]);
    const { start, end } = localDayBounds(env.LOR_TZ, date);
    const recent = await sql<{ id: string; title: string }[]>`
      SELECT i.id, i.title FROM intent i
      JOIN (SELECT intent_id, max(t_start) AS last FROM time_block WHERE origin <> 'calendar' OR committed GROUP BY intent_id) b ON b.intent_id = i.id
      WHERE NOT i.system AND i.status IN ('active', 'waiting') ORDER BY b.last DESC LIMIT 5`;
    const prompt = await sql<{ id: string; title: string }[]>`
      SELECT i.id, i.title FROM intent i
      WHERE i.type = 'metric' AND i.status = 'active'
        AND NOT EXISTS (SELECT 1 FROM commitment c WHERE c.intent_id = i.id AND c.kind = 'metric_value' AND c.t >= ${start} AND c.t < ${end})
      ORDER BY i.weight DESC, i.created_at LIMIT 1`;
    const system = await sql<{ id: string; title: string }[]>`SELECT id, title FROM intent WHERE system ORDER BY title`;
    // Time goes to goals, projects, habits, and tasks; a metric is a measurement, not somewhere time goes.
    const pickable = graph.nodes.filter((n) => (n.status === "active" || n.status === "waiting") && n.type !== "metric");
    return {
      today,
      intents: [
        ...pickable.map((n) => ({ id: n.id, title: n.title, type: n.type as string })),
        ...system.map((s) => ({ id: s.id, title: s.title, type: "system" })),
      ],
      recent: recent.map((r) => ({ id: r.id, title: r.title })),
      starving: graph.nodes.filter((n) => n.starving).map((n) => ({ id: n.id, title: n.title, hours: n.week_hours ?? 0, target: n.weekly_hour_target ?? 0 })),
      needsYou: (await needsOwner(sql)).map((n) => ({ id: n.id, title: n.title, note: n.flag_note ?? "", google: n.google_task_id !== null })),
      agentTasks: graph.nodes
        .filter((n) => n.type === "task" && n.assignee === "agent" && (n.status === "active" || n.status === "waiting"))
        .map((n) => ({ id: n.id, title: n.title, status: n.status as string })),
      metricPrompt: prompt[0] ? { id: prompt[0].id, title: prompt[0].title } : null,
      localHour: Number(new Intl.DateTimeFormat("en-GB", { timeZone: env.LOR_TZ, hour: "2-digit", hourCycle: "h23" }).format(now)),
      yesterday: addDays(localYmd(env.LOR_TZ, now), -1),
    };
  });

export const timerFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) =>
    z.object({ action: z.enum(["start", "stop", "pause", "resume"]), intentId: z.string().uuid().nullable().optional(), title: z.string().max(200).nullable().optional() }).parse(d),
  )
  .handler(async ({ data }) => {
    if (data.action === "start") await startManualBlock(sql, { intentId: data.intentId, title: data.title }, ui);
    else if (data.action === "stop") await stopRunningBlock(sql, ui);
    else if (data.action === "pause") await pauseRunningBlock(sql);
    else await resumeRunningBlock(sql, ui);
    return { ok: true };
  });

export const recategorizeFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ blockId: z.string().uuid(), intentId: z.string().uuid(), rememberTitle: z.boolean().optional() }).parse(d))
  .handler(async ({ data }) => {
    await recategorizeBlock(sql, data.blockId, data.intentId, ui, { rememberTitle: data.rememberTitle });
    return { ok: true };
  });

export const confirmFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) =>
    z.object({ date: DateInput, all: z.boolean().optional(), accept: z.array(z.string().uuid()).optional(), reject: z.array(z.string().uuid()).optional() }).parse(d),
  )
  .handler(async ({ data }) => confirmDay(sql, data, ui));

export const uncommitFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ blockId: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    await uncommitBlock(sql, data.blockId);
    return { ok: true };
  });

export const editBlockFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ blockId: z.string().uuid(), date: DateInput, start: Hm.optional(), end: Hm.optional() }).parse(d))
  .handler(async ({ data }) => {
    const tStart = data.start ? onDay(data.date, data.start) : undefined;
    let tEnd = data.end ? onDay(data.date, data.end) : undefined;
    // "23:00 to 00:30" ends on the next day.
    if (tStart && tEnd && tEnd.getTime() <= tStart.getTime()) tEnd = onDay(addDays(data.date, 1), data.end!);
    if (!tStart && !tEnd) throw new BadInput("nothing to change");
    await editBlock(sql, data.blockId, { tStart, tEnd });
    return { ok: true };
  });

export const splitBlockFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ blockId: z.string().uuid(), date: DateInput, at: Hm }).parse(d))
  .handler(async ({ data }) => splitBlock(sql, data.blockId, onDay(data.date, data.at)));

export const deleteBlockFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ blockId: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    await deleteBlock(sql, data.blockId);
    return { ok: true };
  });
