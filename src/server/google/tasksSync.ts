// Google Tasks → `task` intents, one way. Google is where tasks are captured
// and ticked off; LoR is the linked record. Ticking a task in Google is the
// owner's own confirmation, so this is the one place a job writes a commitment.
import type { Sql, Tx } from "../../db/client.js";
import { uuidv7 } from "../../shared/ids.js";
import { localTimeOnDay } from "../../shared/localTime.js";
import { env } from "../env.js";
import type { GTask, TasksPort } from "./client.js";

/** Google's `due` is a date (midnight UTC of the due day). Due = the end of that local day. */
export function dueAt(due: string | undefined, tz: string): Date | null {
  if (!due) return null;
  const ymd = due.slice(0, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(ymd) ? localTimeOnDay(tz, ymd, "23:59") : null;
}

export interface TasksSyncStats {
  lists: number;
  tasks: number;
  created: number;
  updated: number;
  completed: number;
  reopened: number;
  archived: number;
  [key: string]: unknown;
}

interface Existing {
  id: string;
  title: string;
  description: string | null;
  status: string;
  due_at: Date | null;
  list: string | null;
}

const sourceRef = (taskId: string) => `google_task:${taskId}`;

async function syncOne(tx: Tx, listId: string, task: GTask, stats: TasksSyncStats): Promise<string | null> {
  const title = task.title.trim();
  const rows = await tx<Existing[]>`
    SELECT id, title, description, status, due_at, meta->>'google_task_list' AS list
    FROM intent WHERE google_task_id = ${task.id} FOR UPDATE`;
  let existing = rows[0];

  if (task.deleted) {
    if (existing && existing.status !== "archived") {
      await tx`UPDATE intent SET status = 'archived', archived_at = now(), updated_at = now() WHERE id = ${existing.id}`;
      stats.archived++;
      return existing.id;
    }
    return null;
  }
  if (!existing && !title) return null; // an empty row someone left in Google

  const due = dueAt(task.due, env.LOR_TZ);
  const notes = task.notes?.trim() || null;
  let touched = false;

  if (!existing) {
    const id = uuidv7();
    await tx`INSERT INTO intent (id, type, title, description, due_at, google_task_id, via, source_ref, meta)
      VALUES (${id}, 'task', ${title}, ${notes}, ${due}, ${task.id}, 'job', ${sourceRef(task.id)},
        ${tx.json({ google_task_list: listId })})`;
    existing = { id, title, description: notes, status: "active", due_at: due, list: listId };
    stats.created++;
    touched = true;
  } else if (
    existing.title !== title ||
    (existing.description ?? null) !== notes ||
    (existing.due_at?.getTime() ?? null) !== (due?.getTime() ?? null) ||
    existing.list !== listId
  ) {
    await tx`UPDATE intent SET title = ${title || existing.title}, description = ${notes}, due_at = ${due},
        meta = meta || ${tx.json({ google_task_list: listId })}, updated_at = now()
      WHERE id = ${existing.id}`;
    stats.updated++;
    touched = true;
  }

  const done = await tx<{ id: string }[]>`
    SELECT id FROM commitment WHERE kind = 'task_done' AND intent_id = ${existing.id} AND source_ref = ${sourceRef(task.id)}`;
  if (task.status === "completed") {
    if (done.length === 0) {
      const when = task.completed ? new Date(task.completed) : new Date();
      await tx`INSERT INTO commitment (id, kind, intent_id, t, via, source_ref)
        VALUES (${uuidv7()}, 'task_done', ${existing.id}, ${when}, 'job', ${sourceRef(task.id)})`;
      touched = true;
    }
    if (existing.status !== "done" && existing.status !== "archived") {
      await tx`UPDATE intent SET status = 'done', updated_at = now() WHERE id = ${existing.id}`;
      stats.completed++;
      touched = true;
    }
  } else {
    if (done.length > 0) {
      await tx`DELETE FROM commitment WHERE id = ANY(${done.map((d) => d.id)}::uuid[])`;
      touched = true;
    }
    // Reopened in Google. `later` and `waiting` are LoR's own shades of "not done"; leave them.
    if (existing.status === "done" || existing.status === "archived") {
      await tx`UPDATE intent SET status = 'active', archived_at = NULL, updated_at = now() WHERE id = ${existing.id}`;
      stats.reopened++;
      touched = true;
    }
  }
  return touched ? existing.id : null;
}

export async function syncTasks(db: Sql, port: TasksPort): Promise<TasksSyncStats & { touched: string[] }> {
  const stats: TasksSyncStats = { lists: 0, tasks: 0, created: 0, updated: 0, completed: 0, reopened: 0, archived: 0 };
  const touched: string[] = [];
  const lists = await port.listTaskLists();
  stats.lists = lists.length;
  for (const list of lists) {
    const tasks = await port.listTasks(list.id);
    stats.tasks += tasks.length;
    await db.begin(async (tx) => {
      for (const task of tasks) {
        const id = await syncOne(tx, list.id, task, stats);
        if (id) touched.push(id);
      }
    });
  }
  // A whole list deleted in Google: its tasks never come back flagged, they just stop arriving.
  const orphaned = await db<{ id: string }[]>`
    UPDATE intent SET status = 'archived', archived_at = now(), updated_at = now()
    WHERE google_task_id IS NOT NULL AND status <> 'archived'
      AND NOT (coalesce(meta->>'google_task_list', '') = ANY(${lists.map((l) => l.id)}::text[]))
    RETURNING id`;
  stats.archived += orphaned.length;
  return { ...stats, touched: [...touched, ...orphaned.map((o) => o.id)] };
}
