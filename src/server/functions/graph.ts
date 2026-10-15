import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { isYmd, localTimeOnDay, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { BadInput } from "../errors.js";
import { getGraph } from "../graph.js";
import {
  INTENT_STATUSES, INTENT_TYPES, LINK_KINDS, assignTask, commitHabit, commitMetric, completeTask, createIntent,
  deleteIntent, getIntent, linkIntents, reopenTask, resetVisibility, unflagTask, unlinkIntents, updateIntent,
} from "../intents.js";
import { entityNames } from "../memory/entities.js";
import { explainScore, recomputeScores } from "../scoring/recompute.js";

const ui = { via: "ui" as const };
const Id = z.object({ id: z.string().uuid() });
const num = z.number().nullable().optional();

export const getGraphFn = createServerFn({ method: "GET" }).handler(async () => {
  await recomputeScores(sql);
  const [graph, entities] = await Promise.all([getGraph(sql), entityNames(sql)]);
  return { nodes: graph.nodes, entities: entities.map((e) => ({ id: e.id, name: e.name })), today: localYmd(env.LOR_TZ, new Date()) };
});

/** What quick add needs from any screen: today, what a task can count toward, and who it can wait on. */
export const getQuickAddFn = createServerFn({ method: "GET" }).handler(async () => {
  const [parents, entities] = await Promise.all([
    sql<{ id: string; name: string; type: string }[]>`
      SELECT id, title AS name, type FROM intent
      WHERE NOT system AND type IN ('goal', 'project', 'habit') AND status IN ('active', 'waiting') ORDER BY lower(title)`,
    entityNames(sql),
  ]);
  return {
    today: localYmd(env.LOR_TZ, new Date()),
    parents: parents.map((p) => ({ ...p })),
    people: entities.map((e) => ({ id: e.id, name: e.name })),
  };
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
  assignee: z.enum(["owner", "agent"]).optional(),
  parentId: z.string().uuid().nullable().optional(),
});

function checkYmd(value: string): string {
  if (!isYmd(value)) throw new BadInput("a date must be YYYY-MM-DD");
  return value;
}

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

export const deleteIntentFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => Id.parse(d))
  .handler(async ({ data }) => deleteIntent(sql, data.id));

const BulkOp = z.discriminatedUnion("op", [
  z.object({ op: z.literal("done") }),
  z.object({ op: z.literal("due"), due: z.string().nullable() }),
  z.object({ op: z.literal("later"), until: z.string().nullable() }),
  z.object({ op: z.literal("active") }),
  z.object({ op: z.literal("assign"), assignee: z.enum(["owner", "agent"]) }),
  z.object({ op: z.literal("parent"), parentId: z.string().uuid().nullable() }),
  z.object({ op: z.literal("delete") }),
]);

/**
 * One action over several tasks. What Google Tasks owns (done, due, deletion)
 * is skipped for tasks imported from it, and counted as skipped.
 */
export const bulkTasksFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ ids: z.array(z.string().uuid()).min(1), action: BulkOp }).parse(d))
  .handler(async ({ data }) => {
    const a = data.action;
    let changed = 0;
    let skipped = 0;
    for (const id of data.ids) {
      const t = await getIntent(sql, id);
      if (t.type !== "task" || (t.google_task_id && (a.op === "done" || a.op === "due" || a.op === "delete"))) {
        skipped++;
        continue;
      }
      if (a.op === "done") {
        if (t.status !== "done") await completeTask(sql, id, ui);
      } else if (a.op === "due") await updateIntent(sql, id, { dueAt: parseDue(a.due) ?? null });
      else if (a.op === "later") await updateIntent(sql, id, { status: "later", snoozedUntil: a.until ? localTimeOnDay(env.LOR_TZ, checkYmd(a.until), "00:00") : null });
      else if (a.op === "active") await updateIntent(sql, id, { status: t.waiting_on_entity_id ? "waiting" : "active" });
      else if (a.op === "assign") await assignTask(sql, id, a.assignee);
      else if (a.op === "parent") {
        const links = await sql<{ id: string }[]>`
          SELECT l.id FROM intent_link l JOIN intent p ON p.id = l.to_id WHERE l.from_id = ${id} AND l.kind = 'measures'`;
        for (const l of links) await unlinkIntents(sql, l.id);
        if (a.parentId) await linkIntents(sql, { fromId: id, toId: a.parentId, kind: "measures" }, ui);
      } else await deleteIntent(sql, id);
      changed++;
    }
    return { changed, skipped };
  });

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

export const unflagFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ id: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    await unflagTask(sql, data.id);
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
