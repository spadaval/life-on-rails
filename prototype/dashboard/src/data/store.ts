// The prototype's one store: the snapshot, a scene's clock, and every change made since, in
// memory only. Nothing here talks to LoR or saves anything; a reload starts over. Every
// direction reads and changes the same store, so a task ticked in one is ticked in the next.
//
// Every action pushes the state before it onto `history`, so `undo()` takes back any change.
// A direction decides which of them to offer an Undo for (the brief: those LoR can reverse).
import { useEffect, useState, useSyncExternalStore } from "react";
import { addDays, localTimeOnDay, localYmd } from "../../../../src/shared/localTime.js";
import raw from "./snapshot.json";
import type { BlockView, Day, Hub, IntentStatus, IntentType, Node, Page, Person, Snapshot } from "./types.js";

const snapshot = raw as unknown as Snapshot;
export const TZ = snapshot.timezone;
export const DAY = snapshot.day;

// ---- scenes: where the clock starts and what the data looks like then ----

export const SCENES = {
  afternoon: "15:00, a timer running, two flags",
  morning: "09:00, the brief, three flags",
  quiet: "15:00, nothing wants you",
  empty: "09:00, nothing recorded yet",
} as const;
export type SceneId = keyof typeof SCENES;
export const isScene = (s: string | null | undefined): s is SceneId => !!s && s in SCENES;

export interface State {
  scene: SceneId;
  /** The scene's clock, in ms. Now is this plus the time since the scene began, unless still. */
  sceneAt: number;
  day: Day;
  /** The day's actual blocks; the running one has t_end null. */
  blocks: BlockView[];
  recent: { id: string; title: string }[];
  brief: string | null;
  unwell: string[] | null;
  nodes: Node[];
  habitDone: { intent_id: string; t: string }[];
  metricValues: { intent_id: string; value: number; t: string }[];
  system: { id: string; title: string }[];
  entities: { id: string; name: string }[];
  people: Person[];
  peopleLists: Record<string, string[]>;
  hubs: Record<string, Hub>;
  pages: Page[];
  /** What was sent to the agent from the dashboard. It goes nowhere. */
  sent: { text: string; about: string | null; at: string }[];
}

interface Store {
  state: State;
  /** Before each change, newest last, with what the change was called. */
  history: { label: string; state: State }[];
  /** Real ms when the scene began; with `still`, the clock does not move. */
  began: number;
  still: boolean;
}

const clone = <T,>(x: T): T => structuredClone(x);

function sceneState(id: SceneId): State {
  const base = id === "morning" || id === "empty" ? snapshot.scenes.morning : snapshot.scenes.afternoon;
  const sceneAt = Date.parse(base.at);
  const { actual, running: _running, ...day } = clone(base.today);
  // The demo set writes the whole day's phone and place time up front. A scene draws what has
  // happened by its clock: later blocks are dropped and one across now is cut at now.
  let blocks = actual
    .filter((b) => Date.parse(b.t_start) < sceneAt)
    .map((b) => (b.t_end && Date.parse(b.t_end) > sceneAt ? { ...b, t_end: new Date(sceneAt).toISOString() } : b));
  const s: State = {
    scene: id,
    sceneAt,
    day,
    blocks,
    recent: clone(base.recent),
    brief: base.brief,
    unwell: base.unwell,
    nodes: clone(snapshot.nodes),
    habitDone: clone(snapshot.habitDone),
    metricValues: [],
    system: clone(snapshot.system),
    entities: clone(snapshot.entities),
    people: clone(snapshot.people),
    peopleLists: clone(snapshot.peopleLists),
    hubs: clone(snapshot.hubs),
    pages: clone(snapshot.pages),
    sent: [],
  };
  const nowIso = new Date(sceneAt).toISOString();
  if (id === "afternoon") {
    // The brief's afternoon has two flagged items; the demo set has three.
    const sam = s.nodes.find((n) => n.title === "Organise Sam's birthday dinner");
    if (sam) Object.assign(sam, { flagged_at: null, flag_note: null });
  }
  if (id === "quiet") {
    // Nothing wants him: no flags, no timer, nothing due today or before.
    blocks = blocks.filter((b) => b.t_end !== null);
    s.blocks = blocks;
    for (const n of s.nodes) {
      if (n.flagged_at) Object.assign(n, { flagged_at: null, flag_note: null });
      if (n.type === "task" && n.due_local && n.due_local <= DAY && n.status !== "done") {
        Object.assign(n, { due_at: null, due_local: null, overdue: false, updated_at: nowIso });
      }
    }
  }
  if (id === "empty") s.blocks = [];
  refresh(s, sceneAt);
  return s;
}

// ---- the store and its clock ----

let store: Store = { state: sceneState("afternoon"), history: [], began: Date.now(), still: false };
const listeners = new Set<() => void>();
const emit = () => listeners.forEach((l) => l());

export function subscribe(l: () => void): () => void {
  listeners.add(l);
  return () => listeners.delete(l);
}

/** Start a scene over from the snapshot. Changes made before are dropped. */
export function resetScene(id: SceneId, still = store.still): void {
  store = { state: sceneState(id), history: [], began: Date.now(), still };
  emit();
}

/** Freeze or free the clock (`still=1` in the link: for screenshots). */
export function setStill(still: boolean): void {
  if (store.still === still) return;
  // Keep now where it is, so freezing or freeing does not jump the clock.
  const at = now();
  store = { ...store, still, began: Date.now(), state: { ...store.state, sceneAt: at } };
  emit();
}

/** Now, in ms, as the data reads it. */
export function now(): number {
  return store.still ? store.state.sceneAt : store.state.sceneAt + (Date.now() - store.began);
}
const nowIso = () => new Date(now()).toISOString();
/** Today's local date (the scene's), YYYY-MM-DD. */
export const today = () => localYmd(TZ, new Date(now()));

export const getState = () => store.state;

/** The whole state, re-rendering on every change. */
export function useStore(): State {
  return useSyncExternalStore(subscribe, getState, getState);
}

/** What `undo()` would take back, or null. */
export function useUndo(): string | null {
  return useSyncExternalStore(subscribe, () => store.history.at(-1)?.label ?? null, () => null);
}

/** Take back the last change. Returns what it was called, or null when there is nothing to undo. */
export function undo(): string | null {
  const last = store.history.at(-1);
  if (!last) return null;
  store = { ...store, state: last.state, history: store.history.slice(0, -1) };
  emit();
  return last.label;
}

/** Apply a change to a copy of the state, keeping the old one for undo. */
function change<T>(label: string, fn: (s: State) => T): T {
  const before = store.state;
  const next = clone(before);
  const out = fn(next);
  refresh(next, now());
  store = { ...store, state: next, history: [...store.history, { label, state: before }].slice(-50) };
  emit();
  return out;
}

/** Fields that follow from others and from the clock: overdue, the local due date, parents' titles. */
function refresh(s: State, at: number): void {
  const byId = new Map(s.nodes.map((n) => [n.id, n]));
  for (const n of s.nodes) {
    n.due_local = n.due_at ? localYmd(TZ, new Date(n.due_at)) : null;
    n.overdue = n.due_at !== null && Date.parse(n.due_at) < at && n.status !== "done";
    n.parents = n.parents.filter((p) => byId.has(p.id)).map((p) => ({ ...p, title: byId.get(p.id)!.title }));
    n.waiting_on = n.waiting_on_entity_id ? (s.entities.find((e) => e.id === n.waiting_on_entity_id)?.name ?? null) : null;
  }
}

const uuid = () => crypto.randomUUID();
const find = (s: State, id: string) => {
  const n = s.nodes.find((x) => x.id === id);
  if (!n) throw new Error(`nothing with id ${id}`);
  return n;
};
/** A due date (YYYY-MM-DD) as LoR stores it: the end of that local day. */
const dueAt = (ymd: string | null) => (ymd ? localTimeOnDay(TZ, ymd, "23:59").toISOString() : null);

// ---- tasks, goals, projects, habits ----

export interface IntentInput {
  type?: IntentType;
  title: string;
  parentId?: string | null;
  /** YYYY-MM-DD, or null for none. */
  due?: string | null;
  status?: IntentStatus;
  description?: string | null;
  waitingOnEntityId?: string | null;
  weight?: number;
  weeklyHourTarget?: number | null;
  frequency?: number | null;
  frequencyHorizon?: "day" | "week";
  estimateHours?: number | null;
  assignee?: "owner" | "agent";
  /** Later until this date (YYYY-MM-DD), or null until brought back. */
  snoozedUntil?: string | null;
}

/** Add a task (or any intent). Returns its id. */
export function addIntent(input: IntentInput): string {
  return change(`Added “${input.title}”`, (s) => {
    const t = nowIso();
    const id = uuid();
    const parent = input.parentId ? find(s, input.parentId) : null;
    s.nodes.push({
      id, type: input.type ?? "task", title: input.title.trim(), short_title: null, description: input.description ?? null,
      status: input.status ?? "active", weight: input.weight ?? 1, weekly_hour_target: input.weeklyHourTarget ?? null,
      frequency: input.frequency ?? null, frequency_horizon: input.frequencyHorizon ?? "day", completion_weight: null, goal_mix: null,
      system: false, metric_kind: input.type === "metric" ? "scale" : null, metric_min: null, metric_max: null,
      estimate_hours: input.estimateHours ?? null, due_at: dueAt(input.due ?? null), snoozed_until: null,
      waiting_on_entity_id: input.waitingOnEntityId ?? null, waiting_on: null, waiting_since: input.waitingOnEntityId ? t : null,
      google_task_id: null, assignee: input.assignee ?? "owner", thread_ref: null, flagged_at: null, flag_note: null,
      via: "ui", source_ref: null, created_at: t, updated_at: t, score: null, confidence: null, week_hours: null,
      starving: false, overdue: false, due_local: null, tasks_done: 0, tasks_total: 0,
      parents: parent ? [{ link_id: uuid(), id: parent.id, title: parent.title, kind: "measures", via: "ui" }] : [],
      children: 0, done_at: null,
    });
    return id;
  });
}

export type IntentPatch = Partial<Omit<IntentInput, "parentId" | "type">>;

/** Change fields of one intent. */
export function updateIntent(id: string, patch: IntentPatch, label = "Saved"): void {
  change(label, (s) => patchNode(find(s, id), patch));
}

function patchNode(n: Node, p: IntentPatch): void {
  if (p.title !== undefined) n.title = p.title.trim() || n.title;
  if (p.description !== undefined) n.description = p.description;
  if (p.status !== undefined) {
    if (p.status === "done" && n.status !== "done") n.done_at = nowIso();
    n.status = p.status;
  }
  if (p.due !== undefined) n.due_at = dueAt(p.due);
  if (p.waitingOnEntityId !== undefined) {
    n.waiting_on_entity_id = p.waitingOnEntityId;
    n.waiting_since = p.waitingOnEntityId ? nowIso() : null;
  }
  if (p.weight !== undefined) n.weight = p.weight;
  if (p.weeklyHourTarget !== undefined) n.weekly_hour_target = p.weeklyHourTarget;
  if (p.frequency !== undefined) n.frequency = p.frequency;
  if (p.frequencyHorizon !== undefined) n.frequency_horizon = p.frequencyHorizon;
  if (p.estimateHours !== undefined) n.estimate_hours = p.estimateHours;
  if (p.assignee !== undefined) n.assignee = p.assignee;
  if (p.snoozedUntil !== undefined) n.snoozed_until = p.snoozedUntil ? localTimeOnDay(TZ, p.snoozedUntil, "00:00").toISOString() : null;
  n.updated_at = nowIso();
}

/** Tick a task, or untick it. A done task leaves Needs you. */
export function setDone(id: string, done: boolean): void {
  const title = getState().nodes.find((n) => n.id === id)?.title ?? "task";
  change(done ? `Done: ${title}` : `Not done: ${title}`, (s) => {
    const n = find(s, id);
    if (n.google_task_id) throw new Error("Lives in Google Tasks: check it off there");
    if (done) {
      n.status = "done";
      n.done_at = nowIso();
      n.flagged_at = null;
      n.flag_note = null;
    } else {
      n.status = n.waiting_on_entity_id ? "waiting" : "active";
      n.done_at = null;
    }
    n.updated_at = nowIso();
  });
}

/** Seen: take a flag down without doing the task. */
export function unflag(id: string): void {
  change("Seen", (s) => Object.assign(find(s, id), { flagged_at: null, flag_note: null, updated_at: nowIso() }));
}

/** The same change to several tasks. */
export function bulkUpdate(ids: string[], patch: IntentPatch): void {
  change(`Changed ${ids.length} tasks`, (s) => ids.forEach((id) => patchNode(find(s, id), patch)));
}

/** Delete an intent and its links. */
export function deleteIntent(id: string): void {
  const title = getState().nodes.find((n) => n.id === id)?.title ?? "it";
  change(`Deleted “${title}”`, (s) => {
    s.nodes = s.nodes.filter((n) => n.id !== id);
  });
}

/** Make `fromId` count toward `toId`. */
export function link(fromId: string, toId: string): void {
  change("Linked", (s) => {
    const from = find(s, fromId);
    const to = find(s, toId);
    if (!from.parents.some((p) => p.id === toId)) from.parents.push({ link_id: uuid(), id: to.id, title: to.title, kind: "measures", via: "ui" });
  });
}

/** Put an intent under one parent instead of its first one (null: under nothing). */
export function setParent(ids: string[], parentId: string | null): void {
  change("Moved", (s) => {
    const to = parentId ? find(s, parentId) : null;
    for (const id of ids) {
      const n = find(s, id);
      n.parents = n.parents.slice(1);
      if (to && !n.parents.some((p) => p.id === to.id)) n.parents.unshift({ link_id: uuid(), id: to.id, title: to.title, kind: "measures", via: "ui" });
      n.updated_at = nowIso();
    }
  });
}

export function unlink(linkId: string): void {
  change("Unlinked", (s) => s.nodes.forEach((n) => (n.parents = n.parents.filter((p) => p.link_id !== linkId))));
}

/** Move every overdue task to Later. Returns how many moved. */
export function resetVisibility(): number {
  return change("Reset visibility", (s) => {
    const overdue = s.nodes.filter((n) => n.type === "task" && n.overdue && (n.status === "active" || n.status === "waiting"));
    overdue.forEach((n) => Object.assign(n, { status: "later", updated_at: nowIso() }));
    return overdue.length;
  });
}

/** Check in a habit for today. */
export function habitDone(id: string): void {
  const title = getState().nodes.find((n) => n.id === id)?.title ?? "habit";
  change(`Logged “${title}”`, (s) => s.habitDone.push({ intent_id: id, t: nowIso() }));
}

/** Log a metric's value (1 to 5) for today. */
export function logMetric(id: string, value: number): void {
  change(`Logged ${value}`, (s) => s.metricValues.push({ intent_id: id, value, t: nowIso() }));
}

// ---- the timer and the day's blocks ----

export interface TimerLabel {
  intentId?: string | null;
  /** A note on what it is, beside or instead of an intent. */
  title?: string | null;
}

const intentTitle = (s: State, id: string | null | undefined) =>
  id ? (s.nodes.find((n) => n.id === id)?.title ?? s.system.find((x) => x.id === id)?.title ?? null) : null;

function newBlock(s: State, label: TimerLabel, at: string): BlockView {
  return {
    id: uuid(), t_start: at, t_end: null, paused_at: null, origin: "manual", title: label.title?.trim() || null,
    intent_id: label.intentId ?? null, intent: intentTitle(s, label.intentId), committed: false, committed_via: null,
    commitment_id: null, proposal: null, rejected: false, mismatch: null,
  } as BlockView;
}

/** The running block, or null. */
export const running = (s: State = getState()) => s.blocks.find((b) => b.t_end === null) ?? null;

/** Fill (phone and place time) gives way to a timer: cut what it overlaps. */
function yieldFill(s: State, from: number, to: number): void {
  const out: BlockView[] = [];
  for (const b of s.blocks) {
    const fill = (b.origin === "auto_activity" || b.origin === "place") && !b.committed;
    const a = Date.parse(b.t_start);
    const z = b.t_end ? Date.parse(b.t_end) : Infinity;
    if (!fill || z <= from || a >= to) {
      out.push(b);
      continue;
    }
    if (a < from) out.push({ ...b, t_end: new Date(from).toISOString() });
    if (z > to) out.push({ ...b, id: uuid(), t_start: new Date(to).toISOString() });
  }
  s.blocks = out.sort((x, y) => x.t_start.localeCompare(y.t_start));
}

/** Count a closed, committed block's hours toward its intent and everything above it this week. */
function countHours(s: State, b: BlockView, sign = 1): void {
  if (!b.intent_id || !b.t_end || !b.committed) return;
  const hours = (sign * (Date.parse(b.t_end) - Date.parse(b.t_start))) / 3_600_000;
  const seen = new Set<string>();
  const stack = [b.intent_id];
  while (stack.length) {
    const id = stack.pop()!;
    if (seen.has(id)) continue;
    seen.add(id);
    const n = s.nodes.find((x) => x.id === id);
    if (!n) continue;
    if (n.type === "goal" || n.type === "project" || n.week_hours !== null) n.week_hours = Math.max(0, Math.round(((n.week_hours ?? 0) + hours) * 100) / 100);
    stack.push(...n.parents.map((p) => p.id));
  }
}

function closeRunning(s: State, at: number): BlockView | null {
  const r = running(s);
  if (!r) return null;
  const end = r.paused_at ? Date.parse(r.paused_at) : at;
  r.t_end = new Date(Math.max(end, Date.parse(r.t_start) + 60_000)).toISOString();
  r.paused_at = null;
  // A timer with a label counts; one without waits to be labelled.
  r.committed = r.intent_id !== null;
  r.committed_via = r.committed ? "ui" : null;
  countHours(s, r);
  return r;
}

/** Start a timer. A running one stops first. */
export function startTimer(label: TimerLabel = {}): void {
  change("Started a timer", (s) => {
    const at = now();
    closeRunning(s, at);
    yieldFill(s, at, Infinity);
    s.blocks.push(newBlock(s, label, new Date(at).toISOString()));
    if (label.intentId) bumpRecent(s, label.intentId);
  });
}

/** Stop the running timer. */
export function stopTimer(): void {
  change("Stopped the timer", (s) => {
    closeRunning(s, now());
  });
}

/** Stop the running timer and start one on something else, from this moment. */
export function switchTimer(label: TimerLabel): void {
  change("Switched the timer", (s) => {
    const at = now();
    closeRunning(s, at);
    s.blocks.push(newBlock(s, label, new Date(at).toISOString()));
    if (label.intentId) bumpRecent(s, label.intentId);
  });
}

export function pauseTimer(): void {
  change("Paused", (s) => {
    const r = running(s);
    if (r && !r.paused_at) r.paused_at = nowIso();
  });
}

export function resumeTimer(): void {
  change("Resumed", (s) => {
    const r = running(s);
    if (!r?.paused_at) return;
    // A pause is never inside a block: the paused stretch ends this block and a new one starts now.
    const label = { intentId: r.intent_id, title: r.title };
    closeRunning(s, now());
    s.blocks.push(newBlock(s, label, nowIso()));
  });
}

function bumpRecent(s: State, id: string): void {
  const title = intentTitle(s, id);
  if (!title) return;
  s.recent = [{ id, title }, ...s.recent.filter((r) => r.id !== id)].slice(0, 5);
}

const block = (s: State, id: string) => {
  const b = s.blocks.find((x) => x.id === id);
  if (!b) throw new Error(`no block ${id}`);
  return b;
};

/** Label a block of time (or the running timer) and count it. Undo takes it back. */
export function relabel(blockId: string, intentId: string): void {
  const title = intentTitle(getState(), intentId) ?? "it";
  change(`Relabelled as ${title}`, (s) => {
    const b = block(s, blockId);
    countHours(s, b, -1);
    Object.assign(b, { intent_id: intentId, intent: intentTitle(s, intentId), proposal: null, rejected: false, mismatch: null });
    if (b.t_end) Object.assign(b, { committed: true, committed_via: "ui" });
    countHours(s, b);
    bumpRecent(s, intentId);
  });
}

/** Say yes or no to the agent's suggestion on a block. */
export function answerProposal(blockId: string, accept: boolean): void {
  change(accept ? "Confirmed" : "Turned down", (s) => {
    const b = block(s, blockId);
    if (accept) {
      Object.assign(b, { proposal: null, committed: true, committed_via: "ui" });
      countHours(s, b);
    } else Object.assign(b, { proposal: null, rejected: true, intent: null, intent_id: null });
  });
}

/** Take a block's confirmation back: it stays labelled, uncounted. */
export function uncommit(blockId: string): void {
  change("Unconfirmed", (s) => {
    const b = block(s, blockId);
    countHours(s, b, -1);
    Object.assign(b, { committed: false, committed_via: null });
  });
}

const onDay = (hm: string) => localTimeOnDay(TZ, getState().day.date, hm).getTime();

/** New start and end times (HH:MM on the day shown) for a timer block. */
export function retime(blockId: string, start: string, end: string): void {
  change("Times changed", (s) => {
    const b = block(s, blockId);
    const a = onDay(start);
    const z = onDay(end);
    if (!(z > a)) throw new Error("The end has to be after the start.");
    countHours(s, b, -1);
    Object.assign(b, { t_start: new Date(a).toISOString(), t_end: new Date(z).toISOString() });
    countHours(s, b);
    yieldFill(s, a, z);
  });
}

/** Cut a block in two at HH:MM. */
export function split(blockId: string, at: string): void {
  change("Split", (s) => {
    const b = block(s, blockId);
    const t = onDay(at);
    const end = b.t_end ? Date.parse(b.t_end) : now();
    if (!(t > Date.parse(b.t_start) && t < end)) throw new Error("Split inside the block.");
    const second = { ...b, id: uuid(), t_start: new Date(t).toISOString() };
    b.t_end = new Date(t).toISOString();
    s.blocks.splice(s.blocks.indexOf(b) + 1, 0, second);
  });
}

export function deleteBlock(blockId: string): void {
  change("Deleted the block", (s) => {
    countHours(s, block(s, blockId), -1);
    s.blocks = s.blocks.filter((b) => b.id !== blockId);
  });
}

// ---- people, the agent ----

export function setPerson(id: string, patch: { priority?: boolean; keepInTouchDays?: number | null }): void {
  change("Saved", (s) => {
    const p = s.people.find((x) => x.id === id);
    const hub = s.hubs[id];
    if (patch.priority !== undefined) {
      if (p) p.priority = patch.priority;
      if (hub) hub.entity.priority = patch.priority;
    }
    if (patch.keepInTouchDays !== undefined) {
      const d = patch.keepInTouchDays;
      if (p) {
        p.keep_in_touch_days = d;
        p.overdue_by = d !== null && p.days_since !== null ? p.days_since - d : null;
      }
      if (hub) hub.entity.keep_in_touch_days = d;
      const kit = (s.peopleLists["keep-in-touch"] ??= []);
      s.peopleLists["keep-in-touch"] = d === null ? kit.filter((x) => x !== id) : kit.includes(id) ? kit : [...kit, id];
    }
  });
}

/** Ask the agent, or hand it something. In the prototype it is only recorded. */
export function askAgent(text: string, about: string | null = null): void {
  change("Sent to the agent", (s) => s.sent.push({ text, about, at: nowIso() }));
}

// ---- small helpers directions may want ----

export const dayAfter = (ymd: string, n = 1) => addDays(ymd, n);

/** Now in ms, re-read every `everyMs` (and on every change). Stops with `still`. */
export function useNow(everyMs = 1000): number {
  const [, tick] = useState(0);
  useStore();
  useEffect(() => {
    if (store.still) return;
    const id = setInterval(() => tick((x) => x + 1), everyMs);
    return () => clearInterval(id);
  }, [everyMs, store.still]);
  return now();
}
