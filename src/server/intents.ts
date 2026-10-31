// The world model: goals, projects, habits, tasks, metrics, and the links
// between them. The UI and the agent both write through here. What the agent
// writes is stamped (via, source_ref) so the UI can badge it and undo it.
import type { Sql, Tx } from "../db/client.js";
import { uuidv7 } from "../shared/ids.js";
import { type Ymd, localTimeOnDay, localYmd } from "../shared/localTime.js";
import { env } from "./env.js";
import { BadInput, NotFound, Refused } from "./errors.js";
import { scheduleRecompute } from "./scoring/recompute.js";

export const INTENT_TYPES = ["goal", "project", "habit", "task", "metric"] as const;
export type IntentType = (typeof INTENT_TYPES)[number];
export const INTENT_STATUSES = ["active", "waiting", "paused", "later", "done", "archived"] as const;
export type IntentStatus = (typeof INTENT_STATUSES)[number];
export const LINK_KINDS = ["measures", "blocks", "derived_from"] as const;
export type LinkKind = (typeof LINK_KINDS)[number];

export interface Provenance {
  via: "ui" | "agent" | "job";
  sourceRef?: string | null;
}

export interface IntentView {
  id: string;
  type: IntentType;
  title: string;
  description: string | null;
  status: IntentStatus;
  weight: number;
  weekly_hour_target: number | null;
  frequency: number | null;
  frequency_horizon: "day" | "week" | null;
  completion_weight: number | null;
  goal_mix: number | null;
  system: boolean;
  metric_kind: string | null;
  metric_min: number | null;
  metric_max: number | null;
  estimate_hours: number | null;
  due_at: string | null;
  snoozed_until: string | null;
  waiting_on_entity_id: string | null;
  waiting_on: string | null;
  waiting_since: string | null;
  google_task_id: string | null;
  assignee: "owner" | "agent";
  thread_ref: string | null;
  /** Tasks the agent flagged for the owner's attention, and why. */
  flagged_at: string | null;
  flag_note: string | null;
  via: string;
  source_ref: string | null;
  created_at: string;
  updated_at: string;
}

interface IntentRow extends Omit<IntentView, "due_at" | "snoozed_until" | "waiting_since" | "flagged_at" | "created_at" | "updated_at"> {
  due_at: Date | null;
  flagged_at: Date | null;
  snoozed_until: Date | null;
  waiting_since: Date | null;
  created_at: Date;
  updated_at: Date;
}

const iso = (d: Date | null) => d?.toISOString() ?? null;

function toView(r: IntentRow): IntentView {
  return {
    id: r.id, type: r.type, title: r.title, description: r.description, status: r.status, weight: r.weight,
    weekly_hour_target: r.weekly_hour_target, frequency: r.frequency, frequency_horizon: r.frequency_horizon,
    completion_weight: r.completion_weight, goal_mix: r.goal_mix, system: r.system, metric_kind: r.metric_kind,
    metric_min: r.metric_min, metric_max: r.metric_max, estimate_hours: r.estimate_hours,
    due_at: iso(r.due_at), snoozed_until: iso(r.snoozed_until), waiting_on_entity_id: r.waiting_on_entity_id,
    waiting_on: r.waiting_on, waiting_since: iso(r.waiting_since), google_task_id: r.google_task_id,
    assignee: r.assignee, thread_ref: r.thread_ref, flagged_at: iso(r.flagged_at), flag_note: r.flag_note, via: r.via, source_ref: r.source_ref,
    created_at: r.created_at.toISOString(), updated_at: r.updated_at.toISOString(),
  };
}

const SELECT_INTENT = (tx: Tx) => tx`
  SELECT i.id, i.type, i.title, i.description, i.status, i.weight, i.weekly_hour_target, i.frequency,
         i.frequency_horizon, i.completion_weight, i.goal_mix, i.system, i.metric_kind, i.metric_min, i.metric_max,
         i.estimate_hours, i.due_at, i.snoozed_until, i.waiting_on_entity_id, e.name AS waiting_on, i.waiting_since,
         i.google_task_id, i.assignee, i.thread_ref, i.flagged_at, i.flag_note, i.via, i.source_ref, i.created_at, i.updated_at
  FROM intent i LEFT JOIN entity e ON e.id = i.waiting_on_entity_id`;

export async function getIntent(tx: Tx, id: string): Promise<IntentView> {
  const rows = await tx<IntentRow[]>`${SELECT_INTENT(tx)} WHERE i.id = ${id}`;
  if (!rows[0]) throw new NotFound(`no intent ${id}`);
  return toView(rows[0]);
}

export async function listIntents(tx: Tx, opts: { includeArchived?: boolean } = {}): Promise<IntentView[]> {
  const rows = opts.includeArchived
    ? await tx<IntentRow[]>`${SELECT_INTENT(tx)} ORDER BY i.type, lower(i.title)`
    : await tx<IntentRow[]>`${SELECT_INTENT(tx)} WHERE i.status <> 'archived' ORDER BY i.type, lower(i.title)`;
  return rows.map(toView);
}

export interface IntentInput {
  type: IntentType;
  title: string;
  description?: string | null;
  status?: IntentStatus;
  weight?: number;
  weeklyHourTarget?: number | null;
  frequency?: number | null;
  frequencyHorizon?: "day" | "week" | null;
  completionWeight?: number | null;
  goalMix?: number | null;
  metricKind?: string | null;
  metricMin?: number | null;
  metricMax?: number | null;
  estimateHours?: number | null;
  dueAt?: Date | null;
  /** With status `later`: when it comes back by itself. */
  snoozedUntil?: Date | null;
  waitingOnEntityId?: string | null;
  assignee?: "owner" | "agent";
  threadRef?: string | null;
  /** Shorthand for a `measures` link from this intent to a parent. */
  parentId?: string | null;
}

function checkInput(i: Partial<IntentInput>): void {
  if (i.title !== undefined && !i.title.trim()) throw new BadInput("an intent needs a title");
  if (i.weight !== undefined && !(i.weight >= 0)) throw new BadInput("weight must be 0 or more");
  for (const [name, v] of [["completion weight", i.completionWeight], ["goal mix", i.goalMix]] as const) {
    if (v !== undefined && v !== null && !(v >= 0 && v <= 1)) throw new BadInput(`${name} must be between 0 and 1`);
  }
}

export async function createIntent(db: Sql, input: IntentInput, who: Provenance): Promise<IntentView> {
  checkInput(input);
  if (!INTENT_TYPES.includes(input.type)) throw new BadInput(`type must be one of ${INTENT_TYPES.join(", ")}`);
  const id = uuidv7();
  const waiting = Boolean(input.waitingOnEntityId);
  const status = input.status ?? (waiting ? "waiting" : "active");
  await db.begin(async (tx) => {
    await tx`
      INSERT INTO intent (id, type, title, description, status, weight, weekly_hour_target, frequency,
        frequency_horizon, completion_weight, goal_mix, metric_kind, metric_min, metric_max, estimate_hours,
        due_at, waiting_on_entity_id, waiting_since, assignee, thread_ref, via, source_ref)
      VALUES (${id}, ${input.type}, ${input.title.trim()}, ${input.description ?? null}, ${status},
        ${input.weight ?? 1}, ${input.weeklyHourTarget ?? null}, ${input.frequency ?? null},
        ${input.frequencyHorizon ?? "day"}, ${input.completionWeight ?? 0.7}, ${input.goalMix ?? 0.7},
        ${input.metricKind ?? (input.type === "metric" ? "five_point" : null)}, ${input.metricMin ?? null},
        ${input.metricMax ?? null}, ${input.estimateHours ?? null}, ${input.dueAt ?? null},
        ${input.waitingOnEntityId ?? null}, ${status === "waiting" ? new Date() : null},
        ${input.assignee ?? "owner"}, ${input.threadRef ?? null}, ${who.via}, ${who.sourceRef ?? null})`;
    if (input.parentId) await linkInTx(tx, { fromId: id, toId: input.parentId, kind: "measures" }, who);
  });
  scheduleRecompute();
  return getIntent(db, id);
}

export type IntentPatch = Partial<Omit<IntentInput, "type" | "parentId">>;

export async function updateIntent(db: Sql, id: string, patch: IntentPatch): Promise<IntentView> {
  checkInput(patch);
  const current = await getIntent(db, id);
  if (current.system) throw new Refused("system-intent", `${current.title} is a system intent and cannot be edited`);
  const has = <K extends keyof IntentPatch>(k: K) => Object.prototype.hasOwnProperty.call(patch, k);
  const status = patch.status ?? current.status;
  const waitingOn = has("waitingOnEntityId") ? (patch.waitingOnEntityId ?? null) : current.waiting_on_entity_id;
  const becameWaiting = status === "waiting" && current.status !== "waiting";
  await db`
    UPDATE intent SET
      title = ${has("title") ? patch.title!.trim() : current.title},
      description = ${has("description") ? (patch.description ?? null) : current.description},
      status = ${status},
      weight = ${patch.weight ?? current.weight},
      weekly_hour_target = ${has("weeklyHourTarget") ? (patch.weeklyHourTarget ?? null) : current.weekly_hour_target},
      frequency = ${has("frequency") ? (patch.frequency ?? null) : current.frequency},
      frequency_horizon = ${patch.frequencyHorizon ?? current.frequency_horizon},
      completion_weight = ${has("completionWeight") ? (patch.completionWeight ?? null) : current.completion_weight},
      goal_mix = ${has("goalMix") ? (patch.goalMix ?? null) : current.goal_mix},
      metric_kind = ${has("metricKind") ? (patch.metricKind ?? null) : current.metric_kind},
      metric_min = ${has("metricMin") ? (patch.metricMin ?? null) : current.metric_min},
      metric_max = ${has("metricMax") ? (patch.metricMax ?? null) : current.metric_max},
      estimate_hours = ${has("estimateHours") ? (patch.estimateHours ?? null) : current.estimate_hours},
      due_at = ${has("dueAt") ? (patch.dueAt ?? null) : current.due_at ? new Date(current.due_at) : null},
      snoozed_until = ${status !== "later" ? null : has("snoozedUntil") ? (patch.snoozedUntil ?? null) : current.snoozed_until ? new Date(current.snoozed_until) : null},
      waiting_on_entity_id = ${waitingOn},
      waiting_since = ${status === "waiting" ? (becameWaiting ? new Date() : current.waiting_since ? new Date(current.waiting_since) : new Date()) : null},
      assignee = ${patch.assignee ?? current.assignee},
      thread_ref = ${has("threadRef") ? (patch.threadRef ?? null) : current.thread_ref},
      archived_at = ${status === "archived" ? new Date() : null},
      updated_at = now()
    WHERE id = ${id}`;
  scheduleRecompute();
  return getIntent(db, id);
}

export async function archiveIntent(db: Sql, id: string): Promise<void> {
  const current = await getIntent(db, id);
  if (current.system) throw new Refused("system-intent", `${current.title} is a system intent and cannot be archived`);
  await db`UPDATE intent SET status = 'archived', archived_at = now(), updated_at = now() WHERE id = ${id}`;
  scheduleRecompute();
}

// ---- links ----

export interface LinkView {
  id: string;
  from_id: string;
  to_id: string;
  kind: LinkKind;
  weight: number;
  via: string;
  source_ref: string | null;
}

export async function listLinks(tx: Tx): Promise<LinkView[]> {
  const rows = await tx<LinkView[]>`SELECT id, from_id, to_id, kind, weight, via, source_ref FROM intent_link`;
  return rows.map((r) => ({ ...r }));
}

interface LinkInput {
  fromId: string;
  toId: string;
  kind: LinkKind;
  weight?: number;
}

async function linkInTx(tx: Tx, input: LinkInput, who: Provenance): Promise<string> {
  if (input.fromId === input.toId) throw new BadInput("an intent cannot link to itself");
  if (!LINK_KINDS.includes(input.kind)) throw new BadInput(`kind must be one of ${LINK_KINDS.join(", ")}`);
  const ends = await tx<{ id: string }[]>`SELECT id FROM intent WHERE id IN (${input.fromId}, ${input.toId})`;
  if (ends.length !== 2) throw new NotFound("both ends of a link must be existing intents");
  const rows = await tx<{ id: string }[]>`
    INSERT INTO intent_link (id, from_id, to_id, kind, weight, via, source_ref)
    VALUES (${uuidv7()}, ${input.fromId}, ${input.toId}, ${input.kind}, ${input.weight ?? 1}, ${who.via}, ${who.sourceRef ?? null})
    ON CONFLICT (from_id, to_id, kind) DO UPDATE SET weight = EXCLUDED.weight
    RETURNING id`;
  return rows[0]!.id;
}

/** from = child or source, to = parent or target. */
export async function linkIntents(db: Sql, input: LinkInput, who: Provenance): Promise<{ id: string }> {
  const id = await linkInTx(db, input, who);
  scheduleRecompute();
  return { id };
}

export async function unlinkIntents(db: Sql, linkId: string): Promise<void> {
  await db`DELETE FROM intent_link WHERE id = ${linkId}`;
  scheduleRecompute();
}

/** Remove the link between two intents; refused if there is none. */
export async function unlinkEnds(db: Sql, ends: { fromId: string; toId: string; kind: string }): Promise<{ removed: string }> {
  const r = await db<{ id: string }[]>`
    DELETE FROM intent_link WHERE from_id = ${ends.fromId} AND to_id = ${ends.toId} AND kind = ${ends.kind} RETURNING id`;
  if (!r[0]) throw new NotFound(`no ${ends.kind} link from ${ends.fromId} to ${ends.toId}`);
  scheduleRecompute();
  return { removed: r[0].id };
}

// ---- tasks ----

export async function assignTask(db: Sql, id: string, assignee: "owner" | "agent", threadRef?: string | null): Promise<IntentView> {
  const current = await getIntent(db, id);
  if (current.type !== "task") throw new BadInput("only a task can be assigned");
  await db`UPDATE intent SET assignee = ${assignee}, thread_ref = ${assignee === "agent" ? (threadRef ?? current.thread_ref) : null},
    queued_at = NULL, updated_at = now() WHERE id = ${id}`;
  return getIntent(db, id);
}

/** Give a task to the agent to do on its own: the work run starts a thread on the oldest one. */
export async function queueTask(db: Sql, id: string): Promise<IntentView> {
  const current = await getIntent(db, id);
  if (current.type !== "task") throw new BadInput("only a task can be queued");
  if (current.status !== "active" && current.status !== "waiting") throw new BadInput(`this task is ${current.status}`);
  await db`UPDATE intent SET assignee = 'agent', queued_at = now(), updated_at = now() WHERE id = ${id}`;
  return getIntent(db, id);
}

/** The oldest queued task nobody has started. */
export async function nextQueuedTask(db: Sql): Promise<{ id: string; title: string } | null> {
  const rows = await db<{ id: string; title: string }[]>`
    SELECT id, title FROM intent WHERE queued_at IS NOT NULL AND status IN ('active', 'waiting') ORDER BY queued_at LIMIT 1`;
  return rows[0] ?? null;
}

/** A worker thread took it. */
export async function claimTask(db: Sql, id: string, threadRef: string): Promise<IntentView> {
  await db`UPDATE intent SET queued_at = NULL, thread_ref = ${threadRef}, updated_at = now() WHERE id = ${id}`;
  return getIntent(db, id);
}

/** Mark a task done. The memo is the outcome (what the agent did, or the owner's note). */
export async function completeTask(db: Sql, id: string, opts: { memo?: string | null } & Provenance): Promise<IntentView> {
  const current = await getIntent(db, id);
  if (current.type !== "task") throw new BadInput("only a task can be marked done");
  if (current.system) throw new Refused("system-intent", "system intents are not tasks to complete");
  if (current.google_task_id) {
    throw new Refused("google-task", "this task lives in Google Tasks: complete it there and the next poll marks it done here");
  }
  await db.begin(async (tx) => {
    const existing = await tx`SELECT id FROM commitment WHERE kind = 'task_done' AND intent_id = ${id}`;
    if (existing.length === 0) {
      await tx`INSERT INTO commitment (id, kind, intent_id, t, memo, via, source_ref)
        VALUES (${uuidv7()}, 'task_done', ${id}, now(), ${opts.memo ?? null}, ${opts.via}, ${opts.sourceRef ?? null})`;
    } else if (opts.memo) {
      await tx`UPDATE commitment SET memo = ${opts.memo} WHERE id = ${existing[0]!.id}`;
    }
    await tx`UPDATE intent SET status = 'done', flagged_at = NULL, flag_note = NULL, updated_at = now() WHERE id = ${id}`;
  });
  scheduleRecompute();
  return getIntent(db, id);
}

export async function reopenTask(db: Sql, id: string): Promise<IntentView> {
  const current = await getIntent(db, id);
  if (current.google_task_id) throw new Refused("google-task", "this task lives in Google Tasks: reopen it there");
  await db.begin(async (tx) => {
    await tx`DELETE FROM commitment WHERE kind = 'task_done' AND intent_id = ${id}`;
    await tx`UPDATE intent SET status = 'active', updated_at = now() WHERE id = ${id}`;
  });
  scheduleRecompute();
  return getIntent(db, id);
}

// ---- needs you ----

/**
 * The agent asks for the owner's attention on a task: it shows under "Needs
 * you" on Today and in the digest, and the channel sends it once. Flagging again
 * (with a new reason) sends it again.
 */
export async function flagTask(db: Sql, id: string, note: string): Promise<IntentView> {
  const current = await getIntent(db, id);
  if (current.type !== "task") throw new BadInput("only a task can be flagged");
  if (current.status !== "active" && current.status !== "waiting") throw new BadInput(`this task is ${current.status}`);
  if (!note.trim()) throw new BadInput("say why it needs Sumeet");
  await db`UPDATE intent SET flagged_at = now(), flag_note = ${note.trim()}, notified_at = NULL, assignee = 'owner',
    updated_at = now() WHERE id = ${id}`;
  return getIntent(db, id);
}

/** The owner has seen it. The task stays. */
export async function unflagTask(db: Sql, id: string): Promise<void> {
  await db`UPDATE intent SET flagged_at = NULL, flag_note = NULL, updated_at = now() WHERE id = ${id}`;
}

export async function needsOwner(tx: Tx): Promise<IntentView[]> {
  const rows = await tx<IntentRow[]>`${SELECT_INTENT(tx)}
    WHERE i.flagged_at IS NOT NULL AND i.status IN ('active', 'waiting') ORDER BY i.flagged_at`;
  return rows.map(toView);
}


/** Outcome memos on a task, newest first (what `lor done --memo` recorded). */
export async function taskOutcome(tx: Tx, id: string): Promise<{ memo: string | null; at: string; via: string; source_ref: string | null } | null> {
  const rows = await tx<{ memo: string | null; t: Date; via: string; source_ref: string | null }[]>`
    SELECT memo, t, via, source_ref FROM commitment WHERE kind = 'task_done' AND intent_id = ${id} ORDER BY t DESC LIMIT 1`;
  return rows[0] ? { memo: rows[0].memo, at: rows[0].t.toISOString(), via: rows[0].via, source_ref: rows[0].source_ref } : null;
}

// ---- habits and metrics: the owner's evidence ----

/** The instant a dated entry is filed at: now for today, local noon for another day. */
function entryTime(date: Ymd | undefined, now = new Date()): Date {
  if (!date || date === localYmd(env.LOR_TZ, now)) return now;
  return localTimeOnDay(env.LOR_TZ, date, "12:00");
}

export async function commitHabit(db: Sql, id: string, opts: { date?: Ymd; memo?: string | null } & Provenance): Promise<{ id: string }> {
  const intent = await getIntent(db, id);
  if (intent.type !== "habit") throw new BadInput(`${intent.title} is a ${intent.type}, not a habit`);
  const cid = uuidv7();
  await db`INSERT INTO commitment (id, kind, intent_id, t, memo, via, source_ref)
    VALUES (${cid}, 'habit_completion', ${id}, ${entryTime(opts.date)}, ${opts.memo ?? null}, ${opts.via}, ${opts.sourceRef ?? null})`;
  scheduleRecompute();
  return { id: cid };
}

export async function commitMetric(db: Sql, id: string, value: number, opts: { date?: Ymd; memo?: string | null } & Provenance): Promise<{ id: string }> {
  const intent = await getIntent(db, id);
  if (intent.type !== "metric") throw new BadInput(`${intent.title} is a ${intent.type}, not a metric`);
  if (!Number.isFinite(value)) throw new BadInput("a metric value must be a number");
  const cid = uuidv7();
  await db`INSERT INTO commitment (id, kind, intent_id, t, value, memo, via, source_ref)
    VALUES (${cid}, 'metric_value', ${id}, ${entryTime(opts.date)}, ${value}, ${opts.memo ?? null}, ${opts.via}, ${opts.sourceRef ?? null})`;
  scheduleRecompute();
  return { id: cid };
}

// ---- delete ----

/**
 * Delete an intent, whoever wrote it. With history (time or commitments on it)
 * it is archived instead, because history cannot vanish. A Google task is
 * deleted in Google: the next poll would bring it back.
 */
export async function deleteIntent(db: Sql, id: string): Promise<{ removed: "deleted" | "archived" }> {
  const current = await getIntent(db, id);
  if (current.system) throw new Refused("system-intent", "system intents cannot be removed");
  if (current.google_task_id) throw new Refused("google-task", `“${current.title}” lives in Google Tasks: delete it there`);
  const history = await db<{ n: number }[]>`
    SELECT (SELECT count(*) FROM commitment WHERE intent_id = ${id})::int
         + (SELECT count(*) FROM time_block WHERE intent_id = ${id})::int AS n`;
  if ((history[0]?.n ?? 0) > 0) {
    await archiveIntent(db, id);
    return { removed: "archived" };
  }
  await db`DELETE FROM intent WHERE id = ${id}`;
  scheduleRecompute();
  return { removed: "deleted" };
}

/** Delete a commitment (an agent-written habit, metric, or done mark) and put its task back. */
export async function undoCommitment(db: Sql, commitmentId: string): Promise<void> {
  await db.begin(async (tx) => {
    const rows = await tx<{ kind: string; intent_id: string; time_block_id: string | null }[]>`
      SELECT kind, intent_id, time_block_id FROM commitment WHERE id = ${commitmentId} FOR UPDATE`;
    const c = rows[0];
    if (!c) throw new NotFound(`no commitment ${commitmentId}`);
    if (c.time_block_id) {
      await tx`UPDATE time_block SET committed = false, commitment_id = NULL, updated_at = now() WHERE id = ${c.time_block_id}`;
    }
    await tx`DELETE FROM commitment WHERE id = ${commitmentId}`;
    if (c.kind === "task_done") {
      await tx`UPDATE intent SET status = 'active', updated_at = now() WHERE id = ${c.intent_id} AND status = 'done'`;
    }
  });
  scheduleRecompute();
}

// ---- Reset Visibility ----

/**
 * Move overdue active tasks and projects (and any tasks named) out of sight:
 * status `later`, optionally snoozed until a date. Goals, habits, and metrics
 * are never moved, and nothing is deleted.
 */
export async function resetVisibility(db: Sql, opts: { until?: Date | null; alsoTaskIds?: string[] } = {}): Promise<{ moved: number }> {
  const also = opts.alsoTaskIds ?? [];
  const rows = await db`
    UPDATE intent SET status = 'later', snoozed_until = ${opts.until ?? null}, updated_at = now()
    WHERE NOT system AND status = 'active'
      AND ((type IN ('task', 'project') AND due_at IS NOT NULL AND due_at < now())
        OR (type = 'task' AND id = ANY(${also}::uuid[])))
    RETURNING id`;
  return { moved: rows.length };
}

/** Bring snoozed intents back when their date arrives. */
export async function wakeSnoozed(db: Tx): Promise<number> {
  const rows = await db`
    UPDATE intent SET status = 'active', snoozed_until = NULL, updated_at = now()
    WHERE status = 'later' AND snoozed_until IS NOT NULL AND snoozed_until <= now() RETURNING id`;
  return rows.length;
}
