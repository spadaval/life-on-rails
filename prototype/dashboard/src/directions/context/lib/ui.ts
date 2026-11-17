// Context's own state beside the shared store: the notice (with Undo only where LoR can reverse
// the change), the two link-only states (LoR unreachable, a crowded queue), the contexts he
// visited (the bar's tabs), and the media hook.
import { createContext, useContext, useEffect, useState, useSyncExternalStore } from "react";
import { askAgent, getState, subscribe as storeSubscribe, undo as storeUndo } from "../../../data/store.js";

type L = () => void;
function signal<T>(initial: T) {
  let value = initial;
  const ls = new Set<L>();
  return {
    get: () => value,
    set: (v: T) => {
      value = v;
      ls.forEach((l) => l());
    },
    use: () =>
      useSyncExternalStore(
        (l) => {
          ls.add(l);
          return () => ls.delete(l);
        },
        () => value,
        () => value,
      ),
  };
}

// ---- the notice ----

export interface Notice {
  id: number;
  text: string;
  tone?: "ok" | "error";
  /** Present only when the change can be taken back. */
  undo?: () => void;
  /** A moment to stop something that cannot be taken back once sent. */
  cancel?: () => void;
}

const notice = signal<Notice | null>(null);
let seq = 0;
let hide: ReturnType<typeof setTimeout> | null = null;

export function say(text: string, opts: Omit<Notice, "id" | "text"> & { ms?: number } = {}): number {
  if (hide) clearTimeout(hide);
  const id = ++seq;
  notice.set({ id, text, tone: opts.tone, undo: opts.undo, cancel: opts.cancel });
  hide = setTimeout(() => {
    if (notice.get()?.id === id) notice.set(null);
  }, opts.ms ?? (opts.undo || opts.cancel ? 7000 : 3500));
  return id;
}
export const dismiss = () => notice.set(null);
export const useNotice = notice.use;

const fail = (err: unknown) => say(err instanceof Error ? err.message : String(err), { tone: "error" });

/** The state right after the last reversible change: Undo stays offered only while it is current. */
let offered: { after: unknown; noticeId: number } | null = null;
storeSubscribe(() => {
  if (offered && getState() !== offered.after) {
    const id = offered.noticeId;
    offered = null;
    const n = notice.get();
    // A later change withdraws the offer, so Undo never takes back something it does not name.
    if (n?.id === id && n.undo) notice.set({ ...n, undo: undefined });
  }
});

/** A change LoR can take back (a tick, a relabel, a due date, Seen): done now, with Undo. */
export function reversible(text: string, fn: () => void): void {
  try {
    fn();
  } catch (err) {
    fail(err);
    return;
  }
  const after = getState();
  const id = say(text, {
    undo: () => {
      if (getState() !== after) return;
      offered = null;
      storeUndo();
      say("Undone");
    },
  });
  offered = { after, noticeId: id };
}

/** A change with no Undo (timer start, stop and switch: nothing in LoR reopens a closed block). */
export function oneWay(text: string, fn: () => void): void {
  try {
    fn();
  } catch (err) {
    fail(err);
    return;
  }
  say(text);
}

/** ⌘Z: the Undo on show, if any. Returns whether it did something. */
export function undoOffered(): boolean {
  const n = notice.get();
  if (!n?.undo) return false;
  n.undo();
  return true;
}

/** Ask the agent: three seconds to cancel, then it is sent, with no Undo after. */
export function ask(text: string, about: string | null): void {
  let gone = false;
  const send = setTimeout(() => {
    if (gone) return;
    askAgent(text, about);
    say("Sent to the agent. Its answer arrives in T3 on your phone.");
  }, 3000);
  say(`Asking the agent: “${text}”`, {
    ms: 3000,
    cancel: () => {
      gone = true;
      clearTimeout(send);
      say("Not sent");
    },
  });
}

// ---- the two link-only states ----

export interface Mode {
  /** When LoR last answered (state=stale), or null when it is reachable. */
  staleSince: number | null;
  /** Twelve waiting (state=crowded). */
  crowded: boolean;
}
export const ModeCtx = createContext<Mode>({ staleSince: null, crowded: false });
export const useMode = () => useContext(ModeCtx);
/** True when changes are off (LoR unreachable). */
export const useOff = () => useMode().staleSince !== null;

/** Made-up flags (crowded state) marked Seen. Kept here: the store has no way to raise one. */
const crowdSeen = signal<string[]>([]);
export const useCrowdSeen = crowdSeen.use;
export const crowdSee = (id: string) => crowdSeen.set([...crowdSeen.get(), id]);
export const crowdUnsee = (id: string) => crowdSeen.set(crowdSeen.get().filter((x) => x !== id));

// ---- contexts visited, newest first: the bar's tabs ----

export interface Visit {
  kind: "project" | "goal" | "person" | "slot" | "loose";
  /** The title or name the link carries. */
  id: string;
}
const visits = signal<Visit[]>([]);
export const useVisits = visits.use;
export function visit(v: Visit): void {
  const cur = visits.get();
  if (cur[0]?.kind === v.kind && cur[0]?.id === v.id) return;
  visits.set([v, ...cur.filter((x) => !(x.kind === v.kind && x.id === v.id))].slice(0, 8));
}
export function forget(v: Visit): void {
  visits.set(visits.get().filter((x) => !(x.kind === v.kind && x.id === v.id)));
}

// ---- layout ----

/** True below `px` wide. */
export function useBelow(px: number): boolean {
  const q = `(max-width: ${px - 0.02}px)`;
  const [m, setM] = useState(() => window.matchMedia(q).matches);
  useEffect(() => {
    const mq = window.matchMedia(q);
    const on = () => setM(mq.matches);
    on();
    mq.addEventListener("change", on);
    return () => mq.removeEventListener("change", on);
  }, [q]);
  return m;
}
export const usePhone = () => useBelow(760);
