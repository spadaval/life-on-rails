// This direction's own state beside the shared store: the notice at the bottom (with Undo
// only where LoR can reverse the change), tasks planned onto the route, and the two states
// the shared scenes do not have (LoR unreachable, a crowded queue), which are links here.
import { useSyncExternalStore } from "react";
import { getState, subscribe as storeSubscribe, undo as storeUndo } from "../../../data/store.js";

type Listener = () => void;
const listeners = new Set<Listener>();
const emit = () => listeners.forEach((l) => l());
const sub = (l: Listener) => {
  listeners.add(l);
  return () => listeners.delete(l);
};

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

let notice: Notice | null = null;
let seq = 0;
let timer: ReturnType<typeof setTimeout> | null = null;

export function say(text: string, opts: Omit<Notice, "id" | "text"> & { ms?: number } = {}): void {
  if (timer) clearTimeout(timer);
  const id = ++seq;
  notice = { id, text, tone: opts.tone, undo: opts.undo, cancel: opts.cancel };
  emit();
  timer = setTimeout(() => {
    if (notice?.id === id) {
      notice = null;
      emit();
    }
  }, opts.ms ?? (opts.undo ? 8000 : 4000));
}
export const dismiss = () => {
  notice = null;
  emit();
};
export const useNotice = () => useSyncExternalStore(sub, () => notice, () => null);

/**
 * Make a change in the shared store and offer Undo for it. The offer lasts only while that
 * change is the newest one: any later change (a timer, another tick) withdraws it, so Undo
 * never takes back something other than what it names.
 */
export function reversible(text: string, fn: () => void, steps = 1): void {
  try {
    fn();
  } catch (err) {
    say(err instanceof Error ? err.message : String(err), { tone: "error" });
    return;
  }
  const mine = getState();
  say(text, {
    undo: () => {
      if (getState() === mine) for (let i = 0; i < steps; i++) storeUndo();
      dismiss();
    },
  });
  const stop = storeSubscribe(() => {
    if (getState() !== mine) {
      stop();
      if (notice?.text === text && notice.undo) {
        notice = { ...notice, undo: undefined };
        emit();
      }
    }
  });
}

/** A change with no Undo (a timer start or stop): say what happened, or the error. */
export function oneWay(text: string, fn: () => void): void {
  try {
    fn();
    say(text);
  } catch (err) {
    say(err instanceof Error ? err.message : String(err), { tone: "error" });
  }
}

/** ⌘Z / Ctrl+Z: the offered Undo, and nothing else. */
export function undoOffered(): boolean {
  if (!notice?.undo) return false;
  notice.undo();
  return true;
}

// ---- tasks planned onto the route: a dashed block in a free gap, put there only by him ----

export interface Plan {
  scene: string;
  taskId: string;
  title: string;
  start: number;
  minutes: number;
}
let plans: Plan[] = [];

export function usePlans(): Plan[] {
  const scene = useSyncExternalStore(storeSubscribe, () => getState().scene);
  const all = useSyncExternalStore(sub, () => plans);
  return all.filter((p) => p.scene === scene);
}

export function plan(taskId: string, title: string, start: number, minutes: number): void {
  const before = plans;
  plans = [...plans.filter((p) => p.taskId !== taskId), { scene: getState().scene, taskId, title, start, minutes }];
  emit();
  say(`Planned “${title}”`, {
    undo: () => {
      plans = before;
      dismiss();
      emit();
    },
  });
}

export function unplan(taskId: string): void {
  const before = plans;
  const p = plans.find((x) => x.taskId === taskId);
  plans = plans.filter((x) => x.taskId !== taskId);
  emit();
  if (p)
    say(`Took “${p.title}” off the route`, {
      undo: () => {
        plans = before;
        dismiss();
        emit();
      },
    });
}

// ---- the crowded queue: twelve waiting, as a link (state=crowded) ----

let crowdSeen: string[] = [];
export const useCrowdSeen = () => useSyncExternalStore(sub, () => crowdSeen);
export function crowdSee(id: string, title: string): void {
  const before = crowdSeen;
  crowdSeen = [...crowdSeen, id];
  emit();
  say(`Seen: ${title}`, {
    undo: () => {
      crowdSeen = before;
      dismiss();
      emit();
    },
  });
}

// ---- asking the agent: a moment to cancel, then it is sent and cannot be taken back ----

export function sendLater(text: string, send: () => void): void {
  let live = true;
  const t = setTimeout(() => {
    if (!live) return;
    send();
    say("Sent to the agent. Its answer arrives on your phone.", { tone: "ok" });
  }, 3000);
  say(`Sending to the agent: “${text.length > 48 ? `${text.slice(0, 46)}…` : text}”`, {
    ms: 3200,
    cancel: () => {
      live = false;
      clearTimeout(t);
      say("Not sent.");
    },
  });
}
