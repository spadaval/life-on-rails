// Read-side helpers over the store: the same lists LoR's loaders build (src/server/functions/
// today.ts, src/server/graph.ts), so a direction does not have to rebuild them. Pure functions.
import { localYmd } from "../../../../src/shared/localTime.js";
import { TZ } from "./store.js";
import type { State } from "./store.js";
import type { BlockView, Node } from "./types.js";

/** A task as Today's Due soon lists it (src/server/graph.ts taskRow). */
export interface TaskRow {
  id: string;
  title: string;
  due: string | null;
  overdue: boolean;
  waiting_on: string | null;
  parent: string | null;
  flagged: boolean;
  agent: boolean;
  google: boolean;
  done: boolean;
}

export const isOpen = (n: Node) => n.status === "active" || n.status === "waiting";

export function taskRow(n: Node): TaskRow {
  return {
    id: n.id, title: n.title, due: n.due_local, overdue: n.overdue, waiting_on: n.status === "waiting" ? (n.waiting_on ?? "someone") : null,
    parent: n.parents[0]?.title ?? null, flagged: n.flagged_at !== null, agent: n.via === "agent", google: n.google_task_id !== null,
    done: n.status === "done",
  };
}

/** Open tasks by due date, undated last, then by title. */
export function openTasks(s: State): TaskRow[] {
  return s.nodes
    .filter((n) => n.type === "task" && isOpen(n))
    .sort((a, b) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title))
    .map(taskRow);
}

/** Tasks marked done on the local day `ymd`. */
export function doneOn(s: State, ymd: string): TaskRow[] {
  return s.nodes.filter((n) => n.type === "task" && n.status === "done" && n.done_at && localYmd(TZ, new Date(n.done_at)) === ymd).map(taskRow);
}

/** What the agent flagged for him, oldest flag first. */
export function needsYou(s: State) {
  return s.nodes
    .filter((n) => n.flagged_at !== null && isOpen(n))
    .sort((a, b) => a.flagged_at!.localeCompare(b.flagged_at!))
    .map((n) => ({ id: n.id, title: n.title, note: n.flag_note ?? "", google: n.google_task_id !== null, flagged_at: n.flagged_at! }));
}

/** Tasks the agent is doing. */
export const agentTasks = (s: State) =>
  s.nodes.filter((n) => n.type === "task" && n.assignee === "agent" && isOpen(n)).map((n) => ({ id: n.id, title: n.title, status: n.status, thread: n.thread_ref }));

/** Active goals with an hour target, and their hours this week. */
export const weekGoals = (s: State) =>
  s.nodes
    .filter((n) => n.type === "goal" && n.status === "active" && n.weekly_hour_target)
    .map((n) => ({ id: n.id, title: n.title, hours: n.week_hours ?? 0, target: n.weekly_hour_target ?? 0, starving: n.starving }));

/** What a block of time can be labelled with: open goals, projects, habits and tasks, then the system labels. */
export const pickable = (s: State) => [
  ...s.nodes.filter((n) => isOpen(n) && n.type !== "metric").map((n) => ({ id: n.id, title: n.title, type: n.type as string })),
  ...s.system.map((x) => ({ id: x.id, title: x.title, type: "system" })),
];

/** The one metric not yet logged today (Energy, 1 to 5), or null. */
export function metricPrompt(s: State, ymd: string) {
  const m = s.nodes.filter((n) => n.type === "metric" && n.status === "active").sort((a, b) => b.weight - a.weight)[0];
  if (!m) return null;
  return s.metricValues.some((v) => v.intent_id === m.id && localYmd(TZ, new Date(v.t)) === ymd) ? null : { id: m.id, title: m.title };
}

/** Habit check-ins on the local day `ymd`. */
export const habitDoneOn = (s: State, id: string, ymd: string) => s.habitDone.filter((h) => h.intent_id === id && localYmd(TZ, new Date(h.t)) === ymd).length;

/** Phone or place time nobody has labelled. */
export const isBare = (b: BlockView) => (b.origin === "auto_activity" || b.origin === "place") && !b.intent && !b.committed && !b.proposal;
export const blockName = (b: BlockView) => b.intent ?? (b.origin === "auto_activity" ? "Phone" : b.origin === "place" ? (b.title ?? "Somewhere") : "Unlabelled");

// ---- search: one field, everything ----

export interface Hit {
  kind: "task" | "goal" | "project" | "habit" | "person" | "page";
  id: string;
  title: string;
  /** A line of context: the parent, a due date, an email, a matching line of the page. */
  detail: string | null;
}

/** Plain-words search over tasks, goals, people and pages, best matches first. */
export function search(s: State, q: string, limit = 40): Hit[] {
  const needle = q.trim().toLowerCase();
  if (!needle) return [];
  const words = needle.split(/\s+/);
  const has = (text: string) => words.every((w) => text.toLowerCase().includes(w));
  const score = (title: string) => (title.toLowerCase().startsWith(needle) ? 0 : title.toLowerCase().includes(needle) ? 1 : 2);
  const hits: (Hit & { rank: number })[] = [];
  for (const n of s.nodes) {
    if (n.status === "archived" || n.type === "metric") continue;
    if (has(`${n.title} ${n.description ?? ""}`)) {
      const detail = n.type === "task" ? [n.status === "done" ? "done" : n.due_local ? `due ${n.due_local}` : null, n.parents[0]?.title].filter(Boolean).join(" · ") : n.type;
      hits.push({ kind: n.type as Hit["kind"], id: n.id, title: n.title, detail: detail || null, rank: score(n.title) });
    }
  }
  for (const p of s.people) {
    const ids = p.identifiers.map((i) => i.value).join(" ");
    if (has(`${p.name} ${ids}`)) hits.push({ kind: "person", id: p.id, title: p.name, detail: p.identifiers[0]?.value ?? null, rank: score(p.name) });
  }
  for (const p of s.pages) {
    if (has(`${p.title} ${p.slug} ${p.body}`)) {
      const line = p.body.split("\n").find((l) => words.some((w) => l.toLowerCase().includes(w)) && !l.startsWith("#")) ?? null;
      hits.push({ kind: "page", id: p.slug, title: p.title, detail: line ? line.trim().slice(0, 140) : p.slug, rank: score(p.title) + 0.5 });
    }
  }
  return hits.sort((a, b) => a.rank - b.rank || a.title.localeCompare(b.title)).slice(0, limit).map(({ rank: _r, ...h }) => h);
}
