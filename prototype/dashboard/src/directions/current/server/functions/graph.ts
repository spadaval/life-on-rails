// Stand-ins for src/server/functions/graph.ts: the same names and inputs, over the in-memory
// store. Reads return at once; writes return a promise, as the real server functions do.
import { getState, today, type IntentPatch } from "../../../../data/store.js";
import * as store from "../../../../data/store.js";
import type { Node } from "../../../../data/types.js";

type Data<T> = { data: T };
const ok = <T,>(v: T) => Promise.resolve(v);
const run = <T,>(fn: () => T): Promise<T> => {
  try {
    return Promise.resolve(fn());
  } catch (err) {
    return Promise.reject(err);
  }
};

export function getGraphFn() {
  const s = getState();
  return { nodes: s.nodes, entities: s.entities, today: today() };
}

export function getQuickAddFn() {
  const s = getState();
  return Promise.resolve({
    today: today(),
    parents: s.nodes
      .filter((n) => ["goal", "project", "habit"].includes(n.type) && (n.status === "active" || n.status === "waiting"))
      .sort((a, b) => a.title.toLowerCase().localeCompare(b.title.toLowerCase()))
      .map((n) => ({ id: n.id, name: n.title, type: n.type })),
    people: s.entities,
  });
}

export interface IntentForm {
  id?: string;
  type: Node["type"];
  title?: string;
  description?: string | null;
  status?: Node["status"];
  weight?: number;
  weeklyHourTarget?: number | null;
  frequency?: number | null;
  frequencyHorizon?: "day" | "week";
  estimateHours?: number | null;
  dueAt?: string | null;
  waitingOnEntityId?: string | null;
  assignee?: "owner" | "agent";
  parentId?: string | null;
}

export function saveIntentFn({ data }: Data<IntentForm>) {
  return run(() => {
    const { id, type, parentId, dueAt, ...rest } = data;
    const patch: IntentPatch = { ...rest, ...(dueAt !== undefined ? { due: dueAt || null } : {}) };
    if (id) {
      store.updateIntent(id, patch);
      return { id };
    }
    return { id: store.addIntent({ ...patch, type, title: data.title ?? "", parentId: parentId ?? null }) };
  });
}

export const deleteIntentFn = ({ data }: Data<{ id: string }>) => run(() => store.deleteIntent(data.id));
export const taskDoneFn = ({ data }: Data<{ id: string; done: boolean }>) => run(() => store.setDone(data.id, data.done));
export const unflagFn = ({ data }: Data<{ id: string }>) => run(() => store.unflag(data.id));
export const assignFn = ({ data }: Data<{ id: string; assignee: "owner" | "agent" }>) => run(() => store.updateIntent(data.id, { assignee: data.assignee }));
export const habitFn = ({ data }: Data<{ id: string }>) => run(() => store.habitDone(data.id));
export const metricFn = ({ data }: Data<{ id: string; value: number }>) => run(() => store.logMetric(data.id, data.value));
export const linkFn = ({ data }: Data<{ fromId: string; toId: string; kind: string }>) => run(() => store.link(data.fromId, data.toId));
export const unlinkFn = ({ data }: Data<{ id: string }>) => run(() => store.unlink(data.id));
export const resetVisibilityFn = (_: Data<{ taskIds?: string[] }>) => run(() => ({ moved: store.resetVisibility() }));

export function explainFn({ data }: Data<{ id: string }>) {
  const n = getState().nodes.find((x) => x.id === data.id);
  // The real one prints the score's parts; the snapshot keeps only the score and its confidence.
  return ok({ json: n?.score === null || !n ? "No score yet." : JSON.stringify({ score: n.score, confidence: n.confidence, week_hours: n.week_hours }, null, 2) });
}

type BulkAction =
  | { op: "done" } | { op: "due"; due: string | null } | { op: "later"; until: string | null } | { op: "active" }
  | { op: "assign"; assignee: "owner" | "agent" } | { op: "parent"; parentId: string | null } | { op: "delete" };

export function bulkTasksFn({ data }: Data<{ ids: string[]; action: BulkAction }>) {
  return run(() => {
    const s = getState();
    const mine = data.ids.filter((id) => !s.nodes.find((n) => n.id === id)?.google_task_id);
    const skipped = data.ids.length - mine.length;
    const a = data.action;
    if (a.op === "done") store.bulkUpdate(mine, { status: "done" });
    else if (a.op === "due") store.bulkUpdate(mine, { due: a.due });
    else if (a.op === "later") store.bulkUpdate(mine, { status: "later", snoozedUntil: a.until });
    else if (a.op === "active") store.bulkUpdate(mine, { status: "active", snoozedUntil: null });
    else if (a.op === "assign") store.bulkUpdate(data.ids, { assignee: a.assignee });
    else if (a.op === "parent") store.setParent(data.ids, a.parentId);
    else if (a.op === "delete") mine.forEach((id) => store.deleteIntent(id));
    return { changed: a.op === "assign" || a.op === "parent" ? data.ids.length : mine.length, skipped: a.op === "assign" || a.op === "parent" ? 0 : skipped };
  });
}
