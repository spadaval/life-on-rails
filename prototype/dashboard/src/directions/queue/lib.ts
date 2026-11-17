// Queue's own helpers: links that keep this direction's parameters, times, the queue's ranking,
// the one thing, the two states the store has no scene for, and the toast that offers Undo only
// where the brief allows it. Imports nothing from another direction.
import { useSyncExternalStore } from "react";
import { localHm } from "../../../../../src/shared/localTime.js";
import { isOpen, needsYou } from "../../data/derive.js";
import { dayAfter, getState, subscribe, TZ, undo, type State } from "../../data/store.js";
import type { Node } from "../../data/types.js";
import { href, navigate, parse, type Params } from "../../shell/nav.js";

// ---- links ----

/**
 * Queue's own parameters that stay on every link, like the shell's: `state` is `stale` (LoR has
 * not answered for a while; what shows is its last data) or `crowded` (twelve things waiting).
 */
const KEEP = ["state"] as const;
const kept = (): Params => {
  const cur = parse(location.hash).params;
  return Object.fromEntries(KEEP.map((k) => [k, cur[k]]));
};
export const qhref = (path: string, params: Params = {}) => href(path, { ...kept(), ...params });
export const go = (path: string, params: Params = {}, opts: { replace?: boolean } = {}) => navigate(path, { ...kept(), ...params }, opts);

export type Mode = "stale" | "crowded" | null;
export const modeOf = (params: Record<string, string>): Mode => (params.state === "stale" || params.state === "crowded" ? params.state : null);

// ---- times ----

export const hm = (iso: string | number) => localHm(TZ, new Date(iso));
const WD = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MON = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const ymdDate = (ymd: string) => new Date(`${ymd}T12:00:00Z`);

/** A due date in words: "today", "tomorrow", "Fri 21 Nov", "was due Thu 13". */
export function dueWords(ymd: string | null, now: string): string | null {
  if (!ymd) return null;
  const d = ymdDate(ymd);
  const label = `${WD[d.getUTCDay()]} ${d.getUTCDate()} ${MON[d.getUTCMonth()]}`;
  if (ymd === now) return "due today";
  if (ymd === dayAfter(now)) return "due tomorrow";
  if (ymd < now) return `was due ${WD[d.getUTCDay()]} ${d.getUTCDate()}`;
  return `due ${label}`;
}
export const shortDay = (ymd: string) => {
  const d = ymdDate(ymd);
  return `${WD[d.getUTCDay()]} ${d.getUTCDate()} ${MON[d.getUTCMonth()]}`;
};

/** 47:12, or 1:03:09 past an hour. */
export function readout(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const p = (n: number) => String(n).padStart(2, "0");
  return h ? `${h}:${p(m)}:${p(sec)}` : `${p(m)}:${p(sec)}`;
}
export const minutes = (ms: number) => {
  const m = Math.round(ms / 60000);
  if (m < 1) return "under a minute";
  return m < 60 ? `${m} min` : `${Math.floor(m / 60)} h ${m % 60 ? `${m % 60} min` : ""}`.trim();
};
export function ago(iso: string | null, nowMs: number): string {
  if (!iso) return "never";
  const d = Math.floor((nowMs - Date.parse(iso)) / 86_400_000);
  if (d <= 0) return "today";
  if (d === 1) return "yesterday";
  if (d < 14) return `${d} days ago`;
  if (d < 60) return `${Math.round(d / 7)} weeks ago`;
  return `${Math.round(d / 30)} months ago`;
}

// ---- the one thing ----

/**
 * The agent's one thing for the day. The snapshot does not record it, so Queue names it here, by
 * title (ids change with every snapshot): the first line of the day's brief is about it. In the
 * quiet scene the agent named nothing, which is what "nothing wants you" means.
 */
const ONE_THING = {
  title: "Draft the offline maps spec",
  why: "Finish it before tomorrow's design review. Lena is waiting on the icon sizes.",
};

// ---- the queue ----

export type Item =
  | { kind: "one"; id: string; title: string; why: string; node: Node }
  | { kind: "flag"; id: string; title: string; note: string; node: Node; made?: boolean }
  | { kind: "task"; id: string; title: string; node: Node };

/**
 * For the crowded state: more of the agent's flags than any real day has, made up over open tasks
 * so the queue holds twelve. Drawn only with `state=crowded`; nothing is written to the store.
 */
const CROWD: [string, string][] = [
  ["Organise Sam's birthday dinner", "Sam's birthday is Friday and the table for eight isn't booked yet."],
  ["Book a sports massage", "Your legs are heaviest this week. Thursday has a free hour at 17:00."],
  ["Invite twenty beta testers", "Beta 0.4 goes out Friday and nobody is invited yet."],
  ["Choose the kitchen paint colour", "The painter holds the slot only if the colour is in by Monday."],
];

/** Direction-local state: flags made for the crowded state that he marked Seen, and "Not today". */
interface Local {
  seen: Set<string>;
  aside: boolean;
  scene: string;
  /** How many made-up flags the crowded state adds: fixed at first draw, so clearing one does not bring in another. */
  crowd: number | null;
}
let local: Local = { seen: new Set(), aside: false, scene: "", crowd: null };
const localListeners = new Set<() => void>();
const emitLocal = () => localListeners.forEach((l) => l());
export function setLocal(fn: (l: Local) => Local) {
  local = fn(local);
  emitLocal();
}
export function useLocal(): Local {
  const scene = getState().scene;
  if (local.scene !== scene) local = { seen: new Set(), aside: false, scene, crowd: null };
  return useSyncExternalStore(
    (l) => {
      localListeners.add(l);
      return () => localListeners.delete(l);
    },
    () => local,
  );
}

/**
 * What wants him, ranked: the one thing (the agent chose it for the day), then LoR's flags,
 * oldest first, then his tasks due today or before, by due time. T3's cards are not here: they
 * stay on the phone.
 */
export function queue(s: State, mode: Mode, l: Local): Item[] {
  const out: Item[] = [];
  const taken = new Set<string>();
  const one = s.scene === "quiet" || l.aside ? null : s.nodes.find((n) => n.title === ONE_THING.title && n.type === "task" && isOpen(n));
  if (one) {
    out.push({ kind: "one", id: one.id, title: one.title, why: ONE_THING.why, node: one });
    taken.add(one.id);
  }
  for (const f of needsYou(s)) {
    if (taken.has(f.id)) continue;
    out.push({ kind: "flag", id: f.id, title: f.title, note: f.note, node: s.nodes.find((n) => n.id === f.id)! });
    taken.add(f.id);
  }
  if (mode === "crowded") {
    const due = s.nodes.filter((n) => n.type === "task" && n.status === "active" && n.assignee === "owner" && n.due_local && n.due_local <= s.day.date && !taken.has(n.id)).length;
    // Twelve waiting in all, counted once when the state first draws.
    l.crowd ??= Math.max(0, 12 - out.length - due);
    for (const [title, note] of CROWD.slice(0, l.crowd)) {
      const n = s.nodes.find((x) => x.title === title && isOpen(x));
      if (!n || taken.has(n.id) || l.seen.has(n.id)) continue;
      out.push({ kind: "flag", id: n.id, title: n.title, note, node: n, made: true });
      taken.add(n.id);
    }
  }
  const due = s.nodes
    .filter((n) => n.type === "task" && n.status === "active" && n.assignee === "owner" && n.due_local && n.due_local <= s.day.date && !taken.has(n.id))
    .sort((a, b) => a.due_at!.localeCompare(b.due_at!) || a.title.localeCompare(b.title));
  for (const n of due) out.push({ kind: "task", id: n.id, title: n.title, node: n });
  return out;
}

// ---- the toast, and Undo only where the brief allows it ----

export interface Toast {
  id: number;
  text: string;
  /** Present only for a change LoR can reverse, and only until another change lands. */
  undo?: () => void;
  action?: { label: string; run: () => void };
  error?: boolean;
}
let toast: Toast | null = null;
let seq = 0;
let mine = false;
const toastListeners = new Set<() => void>();
const emitToast = () => toastListeners.forEach((l) => l());
// Any change to the store that this module did not make takes Undo off the toast: undo() takes
// back the store's last change, which would no longer be the one the toast names.
subscribe(() => {
  if (mine || !toast?.undo) return;
  toast = { ...toast, undo: undefined };
  emitToast();
});
let timer: ReturnType<typeof setTimeout> | undefined;
function show(t: Omit<Toast, "id">) {
  toast = { ...t, id: ++seq };
  clearTimeout(timer);
  timer = setTimeout(() => {
    toast = null;
    emitToast();
  }, t.undo ? 8000 : 5000);
  emitToast();
}
export const dismissToast = () => {
  toast = null;
  emitToast();
};
export const useToast = () =>
  useSyncExternalStore(
    (l) => {
      toastListeners.add(l);
      return () => toastListeners.delete(l);
    },
    () => toast,
  );

/**
 * Make a change and say so. `undoable` offers Undo, for the changes LoR can reverse (a tick, a
 * relabel, a due date, Seen, an added task); never for what went outside it (a message to the
 * agent) or what time has already overtaken (a timer started or stopped).
 */
export function act(text: string, fn: () => void, opts: { undoable?: boolean; localUndo?: () => void; action?: Toast["action"] } = {}): boolean {
  mine = true;
  try {
    fn();
  } catch (err) {
    mine = false;
    show({ text: err instanceof Error ? err.message : String(err), error: true });
    return false;
  }
  mine = false;
  const revert = opts.localUndo
    ? opts.localUndo
    : opts.undoable
      ? () => {
          mine = true;
          undo();
          mine = false;
        }
      : undefined;
  show({
    text,
    action: opts.action,
    undo: revert
      ? () => {
          revert();
          show({ text: "Undone" });
        }
      : undefined,
  });
  return true;
}
export const say = (text: string, error = false) => show({ text, error });

// ---- small things ----

export const initials = (name: string) =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]!.toUpperCase())
    .join("");
export const parentTitle = (n: Node) => n.parents[0]?.title ?? null;
export const ONE_THING_TITLE = ONE_THING.title;

/**
 * Bring a row opened by a link into view, except with a still clock (`still=1`, for screenshots):
 * a full-page capture of a scrolled page draws the sticky top bar in the middle of it.
 */
export function revealOnOpen(el: HTMLElement | null) {
  if (parse(location.hash).sticky.still === "1") return;
  el?.scrollIntoView({ block: "nearest" });
}
