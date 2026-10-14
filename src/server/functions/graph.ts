import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { isYmd, localTimeOnDay } from "../../shared/localTime.js";
import { env } from "../env.js";
import { BadInput } from "../errors.js";
import { getGraph } from "../graph.js";
import {
  INTENT_STATUSES, INTENT_TYPES, LINK_KINDS, archiveIntent, assignTask, commitHabit, commitMetric, completeTask,
  createIntent, linkIntents, reopenTask, resetVisibility, undoIntent, unlinkIntents, updateIntent,
} from "../intents.js";
import { listEntities } from "../memory/entities.js";
import { explainScore, recomputeScores } from "../scoring/recompute.js";

const ui = { via: "ui" as const };
const Id = z.object({ id: z.string().uuid() });
const num = z.number().nullable().optional();

export const getGraphFn = createServerFn({ method: "GET" }).handler(async () => {
  await recomputeScores(sql);
  const [graph, entities] = await Promise.all([getGraph(sql), listEntities(sql)]);
  return { nodes: graph.nodes, entities: entities.map((e) => ({ id: e.id, name: e.name })) };
});

const IntentForm = z.object({
  id: z.string().uuid().optional(),
  type: z.enum(INTENT_TYPES),
  title: z.string(),
  description: z.string().nullable().optional(),
  status: z.enum(INTENT_STATUSES).optional(),
  weight: z.number().optional(),
  weeklyHourTarget: num,
  frequency: num,
  frequencyHorizon: z.enum(["day", "week"]).optional(),
  metricMin: num,
  metricMax: num,
  estimateHours: num,
  /** A local date, "2026-10-20", or empty. */
  dueAt: z.string().nullable().optional(),
  waitingOnEntityId: z.string().uuid().nullable().optional(),
  parentId: z.string().uuid().nullable().optional(),
});

/** A local calendar date means the end of that day in the owner's zone. */
function parseDue(value: string | null | undefined): Date | null | undefined {
  if (value === undefined) return undefined;
  if (!value) return null;
  if (!isYmd(value)) throw new BadInput("due date must be YYYY-MM-DD");
  return localTimeOnDay(env.LOR_TZ, value, "23:59");
}

export const saveIntentFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => IntentForm.parse(d))
  .handler(async ({ data }) => {
    const { id, type, parentId, dueAt, ...rest } = data;
    const due = parseDue(dueAt);
    if (id) {
      const saved = await updateIntent(sql, id, { ...rest, ...(due !== undefined ? { dueAt: due } : {}) });
      return { id: saved.id };
    }
    const created = await createIntent(sql, { type, ...rest, dueAt: due ?? null, parentId }, ui);
    return { id: created.id };
  });

export const archiveIntentFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => Id.parse(d))
  .handler(async ({ data }) => {
    await archiveIntent(sql, data.id);
    return { ok: true };
  });

export const undoIntentFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => Id.parse(d))
  .handler(async ({ data }) => undoIntent(sql, data.id));

export const linkFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ fromId: z.string().uuid(), toId: z.string().uuid(), kind: z.enum(LINK_KINDS) }).parse(d))
  .handler(async ({ data }) => linkIntents(sql, data, ui));

export const unlinkFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => Id.parse(d))
  .handler(async ({ data }) => {
    await unlinkIntents(sql, data.id);
    return { ok: true };
  });

export const taskDoneFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ id: z.string().uuid(), done: z.boolean() }).parse(d))
  .handler(async ({ data }) => {
    if (data.done) await completeTask(sql, data.id, ui);
    else await reopenTask(sql, data.id);
    return { ok: true };
  });

export const assignFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ id: z.string().uuid(), assignee: z.enum(["owner", "agent"]) }).parse(d))
  .handler(async ({ data }) => {
    await assignTask(sql, data.id, data.assignee);
    return { ok: true };
  });

export const habitFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => Id.parse(d))
  .handler(async ({ data }) => commitHabit(sql, data.id, ui));

export const metricFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ id: z.string().uuid(), value: z.number() }).parse(d))
  .handler(async ({ data }) => commitMetric(sql, data.id, data.value, ui));

export const resetVisibilityFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ taskIds: z.array(z.string().uuid()).optional() }).parse(d))
  .handler(async ({ data }) => resetVisibility(sql, { alsoTaskIds: data.taskIds }));

export const explainFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => Id.parse(d))
  .handler(async ({ data }) => {
    const s = await explainScore(sql, data.id);
    return { json: s ? JSON.stringify(s.parts, null, 2) : "No score yet." };
  });
