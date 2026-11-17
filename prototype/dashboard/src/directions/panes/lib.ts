// Panes' own helpers: links that keep this direction's parameters, opening and closing the
// detail pane with an honest Back, the layer stack Escape walks, the width the layout is in,
// times, the crowded state, and the toast that offers Undo only where the brief allows it.
// Imports nothing from another direction.
import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { localHm } from "../../../../../src/shared/localTime.js";
import { isOpen, needsYou } from "../../data/derive.js";
import { dayAfter, getState, subscribe, TZ, undo, type State } from "../../data/store.js";
import type { Node } from "../../data/types.js";
import { href, parse, type Params } from "../../shell/nav.js";

// ---- links ----

/** Panes' parameters that stay on every link, like the shell's: `state` is `stale` or `crowded`. */
const KEEP = ["state"] as const;
const kept = (): Params => {
  const cur = parse(location.hash).params;
  return Object.fromEntries(KEEP.map((k) => [k, cur[k]]));
};
export const phref = (path: string, params: Params = {}) => href(path, { ...kept(), ...params });

export type Mode = "stale" | "crowded" | null;
export const modeOf = (params: Record<string, string>): Mode => (params.state === "stale" || params.state === "crowded" ? params.state : null);
export const useMode = (): Mode => modeOf(parse(useSyncExternalStore(hashSub, readHash, readHash)).params);
const readHash = () => location.hash;
const hashSub = (l: () => void) => {
  addEventListener("hashchange", l);
  return () => removeEventListener("hashchange", l);
};

/**
 * What this tab's history knows about the panes: `panes` is how many entries opening panes has
 * pushed since the list (0 when a link opened them), `sub` that this entry was pushed by opening a
 * stacked pane over another.
 */
interface PaneState {
  panes: number;
  sub: boolean;
}
const paneState = (): PaneState => ({ panes: Number(history.state?.panes ?? 0), sub: Boolean(history.state?.sub) });

/** Write a hash and tell the app. */
function write(next: string, how: "push" | "replace", state: PaneState | null) {
  if (next === location.hash && how === "replace") {
    history.replaceState(state, "", next);
    return;
  }
  if (how === "push") history.pushState(state, "", next);
  else history.replaceState(state, "", next);
  dispatchEvent(new HashChangeEvent("hashchange"));
}

/** Go back `steps` entries, then run `fn` on the entry that lands. */
function backThen(steps: number, fn: () => void) {
  if (steps <= 0) return fn();
  addEventListener("popstate", () => fn(), { once: true });
  history.go(-steps);
}

/** A task, goal or project from a link: by id, or by title for hand-written links. */
export function findNode<T extends { id: string; title: string }>(nodes: T[], key: string | undefined, ok: (n: T) => boolean = () => true): T | undefined {
  if (!key) return undefined;
  return nodes.find((n) => n.id === key && ok(n)) ?? nodes.find((n) => n.title === key && ok(n));
}

/** Go to a page (a new history entry), keeping Panes' parameters. */
export function go(path: string, params: Params = {}) {
  write(phref(path, params), "push", null);
}

/**
 * Open something in this page's detail pane (`sub`: a pane stacked over the open one). From
 * nothing open it adds a history entry, so Back closes it. With something already open it replaces
 * the entry, so Back never walks through every row he looked at; and when that drops a stacked
 * pane pushed over the old item, its entry goes too, so Close and Back return to the list.
 */
export function openHere(patch: Params, openKeys: string[], opts: { sub?: boolean } = {}) {
  const st = paneState();
  const target = () => {
    const c = parse(location.hash);
    const params: Params = { ...c.params };
    for (const k of openKeys) delete params[k];
    Object.assign(params, patch);
    return href(c.path, params);
  };
  const wasOpen = openKeys.some((k) => parse(location.hash).params[k] !== undefined);
  if (!wasOpen) return write(target(), "push", { panes: st.panes + 1, sub: Boolean(opts.sub) });
  if (!opts.sub && st.sub) return backThen(1, () => write(target(), "replace", { ...paneState(), sub: false }));
  write(target(), "replace", st);
}

/**
 * Close the detail pane (`sub`: only the stacked pane in front). What was opened in this tab
 * closes by going Back over its entries, so Back afterwards does not reopen it; what a link
 * opened is taken off its entry, so Back leaves the prototype as it came.
 */
export function closeHere(openKeys: string[], opts: { sub?: boolean } = {}) {
  const st = paneState();
  const strip = () => {
    const c = parse(location.hash);
    if (![...openKeys, "sheet"].some((k) => c.params[k] !== undefined)) return;
    const params: Params = { ...c.params };
    for (const k of [...openKeys, ...(opts.sub ? [] : ["sheet"])]) delete params[k];
    write(href(c.path, params), "replace", paneState());
  };
  if (opts.sub) return st.sub ? history.back() : strip();
  backThen(st.panes, strip);
}

/** Change parameters in place (a filter, the sheet's height), without a new history entry. */
export function setHere(patch: Params) {
  const cur = parse(location.hash);
  write(href(cur.path, { ...cur.params, ...patch }), "replace", paneState());
}

// ---- the layer stack: Escape closes only the one in front ----

const layers: { id: number; close: () => void }[] = [];
let layerSeq = 0;
addEventListener("keydown", (e) => {
  if (e.key !== "Escape" || !layers.length) return;
  e.preventDefault();
  layers.at(-1)!.close();
});

/** While mounted and `on`, this layer is the one Escape closes, until a newer one opens. */
export function useLayer(on: boolean, close: () => void) {
  const fn = useRef(close);
  fn.current = close;
  useEffect(() => {
    if (!on) return;
    const me = { id: ++layerSeq, close: () => fn.current() };
    layers.push(me);
    return () => {
      const i = layers.findIndex((l) => l.id === me.id);
      if (i >= 0) layers.splice(i, 1);
    };
  }, [on]);
}

// ---- width ----

/** wide: list and detail side by side (1280 and up). mid: the detail opens over the list. narrow: phone width, a sheet. */
export type Fit = "wide" | "mid" | "narrow";
const fitNow = (): Fit => (innerWidth >= 1280 ? "wide" : innerWidth >= 720 ? "mid" : "narrow");
export function useFit(): Fit {
  const [fit, set] = useState(fitNow);
  useEffect(() => {
    const on = () => set(fitNow());
    addEventListener("resize", on);
    return () => removeEventListener("resize", on);
  }, []);
  return fit;
}

// ---- times ----

export const hm = (iso: string | number) => localHm(TZ, new Date(iso));
const WD = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MON = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const ymdDate = (ymd: string) => new Date(`${ymd}T12:00:00Z`);
export const shortDay = (ymd: string) => {
  const d = ymdDate(ymd);
  return `${WD[d.getUTCDay()]} ${d.getUTCDate()} ${MON[d.getUTCMonth()]}`;
};
export const longDay = (ymd: string) => {
  const d = ymdDate(ymd);
  return `${["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"][d.getUTCDay()]} ${d.getUTCDate()} ${["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"][d.getUTCMonth()]}`;
};

/** A due date in a few words, against today: "today", "tomorrow", "Fri 20 Nov", "3 days late". */
export function dueWords(ymd: string | null, today: string): string | null {
  if (!ymd) return null;
  if (ymd === today) return "today";
  if (ymd === dayAfter(today)) return "tomorrow";
  if (ymd < today) {
    const days = Math.round((Date.parse(`${today}T12:00:00Z`) - Date.parse(`${ymd}T12:00:00Z`)) / 86_400_000);
    return days === 1 ? "yesterday" : `${days} days late`;
  }
  return shortDay(ymd);
}

/** 47:12, or 1:03:09 past an hour. */
export function readout(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const p = (n: number) => String(n).padStart(2, "0");
  return h ? `${h}:${p(m)}:${p(s % 60)}` : `${p(m)}:${p(s % 60)}`;
}
export function minutes(ms: number): string {
  const m = Math.round(ms / 60000);
  if (m < 1) return "under a minute";
  if (m < 60) return `${m} min`;
  return `${Math.floor(m / 60)} h${m % 60 ? ` ${m % 60} min` : ""}`;
}
export function ago(iso: string | null, nowMs: number): string {
  if (!iso) return "never";
  const mins = Math.floor((nowMs - Date.parse(iso)) / 60000);
  if (mins < 60) return mins <= 1 ? "just now" : `${mins} min ago`;
  if (mins < 24 * 60) return `${Math.floor(mins / 60)} h ago`;
  const d = Math.floor(mins / (24 * 60));
  if (d === 1) return "yesterday";
  if (d < 14) return `${d} days ago`;
  if (d < 60) return `${Math.round(d / 7)} weeks ago`;
  return `${Math.round(d / 30)} months ago`;
}
export const fmtH = (h: number) => (Math.round(h * 10) / 10).toString();

// ---- what wants him: the agent's flags, and in the crowded state twelve of them ----

/**
 * For the crowded state: flags made up over open tasks so Needs you holds twelve. Drawn only with
 * `state=crowded`; nothing is written to the store, and Seen on one hides it in this tab.
 */
const CROWD: [string, string][] = [
  ["Book a sports massage", "Your legs are heaviest this week. Thursday has a free hour at 17:00."],
  ["Invite twenty beta testers", "Beta 0.4 goes out Friday and nobody is invited yet."],
  ["Choose the kitchen paint colour", "The painter holds the slot only if the colour is in by Monday."],
  ["Write release notes for beta 0.4", "Lena needs them to write the store listing tomorrow morning."],
  ["Benchmark tile cache sizes", "The spec review on Thursday will ask which size; there is no number yet."],
  ["Return the library books", "Due tomorrow. The library is on the way to the dentist at 16:30."],
  ["Plan the long run route for Sunday", "Coach Dana asked for the route by Friday so the club can join."],
  ["Gather 1099s and receipts", "Northwind Accounting sends the engagement letter once these are in."],
  ["Book flights to Lisbon", "Fares for the April dates went up twice this week."],
  ["Send Noor the photos from the hike", "Noor asked again yesterday on WhatsApp."],
];

export interface Flag {
  id: string;
  title: string;
  note: string;
  node: Node;
  /** Made up for the crowded state. */
  made?: boolean;
}

interface Local {
  seen: Set<string>;
  scene: string;
}
let local: Local = { seen: new Set(), scene: "" };
const localListeners = new Set<() => void>();
export function setLocal(fn: (l: Local) => Local) {
  local = fn(local);
  localListeners.forEach((l) => l());
}
export function useLocal(): Local {
  const scene = getState().scene;
  if (local.scene !== scene) local = { seen: new Set(), scene };
  return useSyncExternalStore(
    (l) => {
      localListeners.add(l);
      return () => localListeners.delete(l);
    },
    () => local,
  );
}

/** The agent's flags, oldest first; in the crowded state, made-up ones until there are twelve. */
export function flags(s: State, mode: Mode, l: Local): Flag[] {
  const out: Flag[] = needsYou(s).map((f) => ({ id: f.id, title: f.title, note: f.note, node: s.nodes.find((n) => n.id === f.id)! }));
  if (mode !== "crowded") return out;
  const taken = new Set(out.map((f) => f.id));
  const want = Math.max(0, 12 - out.length);
  let added = 0;
  for (const [title, note] of CROWD) {
    if (added >= want) break;
    const n = s.nodes.find((x) => x.title === title);
    if (!n || taken.has(n.id)) continue;
    added++;
    if (!isOpen(n) || l.seen.has(n.id)) continue;
    out.push({ id: n.id, title: n.title, note, node: n, made: true });
  }
  return out;
}

/** His open tasks due today or before, by due time. */
export const dueNow = (s: State, skip: Set<string>) =>
  s.nodes
    .filter((n) => n.type === "task" && n.status === "active" && n.assignee === "owner" && n.due_local && n.due_local <= s.day.date && !skip.has(n.id))
    .sort((a, b) => a.due_at!.localeCompare(b.due_at!) || a.title.localeCompare(b.title));

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
// Any change this module did not make takes Undo off the toast: undo() takes back the store's
// last change, which would no longer be the one the toast names.
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
 * Make a change and say so. `undoable` offers Undo, for what LoR can reverse (a tick, a relabel, a
 * due date, Seen, an added task); never for what left it (a message to the agent) or what time has
 * overtaken (a timer started, stopped or switched).
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

/** Why a write control is off while LoR is not answering. */
export const STALE = "LoR is not answering; this waits until it is back";

// ---- small things ----

export const initials = (name: string) =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]!.toUpperCase())
    .join("");
export const parentTitle = (n: Node) => n.parents[0]?.title ?? null;
export const firstName = (name: string) => name.split(" ")[0] ?? name;
export const isStill = () => parse(location.hash).sticky.still === "1";
