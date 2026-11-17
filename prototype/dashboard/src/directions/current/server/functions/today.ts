// Stand-ins for src/server/functions/today.ts over the in-memory store.
import { agentTasks, doneOn, metricPrompt, needsYou, openTasks, pickable, weekGoals } from "../../../../data/derive.js";
import * as store from "../../../../data/store.js";
import { getState, now, running, today, TZ } from "../../../../data/store.js";
import { addDays, localYmd } from "../../shared/localTime.js";

type Data<T> = { data: T };
const run = <T,>(fn: () => T): Promise<T> => {
  try {
    return Promise.resolve(fn());
  } catch (err) {
    return Promise.reject(err);
  }
};

/** What Today's loader returns, for the scene's day. The prototype has one day: earlier and later days are not in it. */
export function getTodayFn(_: { data: { date?: string; obs?: string } }) {
  const s = getState();
  const at = now();
  const wakeEnd = Date.parse(s.day.wake_end);
  const wakeStart = Date.parse(s.day.wake_start);
  const remaining = Math.max(0, (wakeEnd - Math.max(at, wakeStart)) / 1000);
  const r = running(s);
  const date = today();
  return {
    today: {
      ...s.day,
      now: new Date(at).toISOString(),
      remaining_s: Math.round(remaining),
      remaining_fraction: remaining / ((wakeEnd - wakeStart) / 1000),
      running: r,
      actual: s.blocks,
      to_confirm: s.blocks.filter((b) => b.proposal && b.t_end && Date.parse(b.t_end) <= at).length,
    },
    intents: pickable(s),
    recent: s.recent,
    goals: weekGoals(s),
    tasks: [...openTasks(s), ...doneOn(s, date)],
    brief: s.brief,
    needsYou: needsYou(s),
    agentTasks: agentTasks(s),
    metricPrompt: metricPrompt(s, date),
    localHour: Number(new Intl.DateTimeFormat("en-GB", { timeZone: TZ, hour: "2-digit", hourCycle: "h23" }).format(new Date(at))),
    yesterday: addDays(localYmd(TZ, new Date(at)), -1),
    record: null,
  };
}

export function timerFn({ data }: Data<{ action: "start" | "stop" | "pause" | "resume"; intentId?: string | null; title?: string | null }>) {
  return run(() => {
    if (data.action === "start") store.startTimer({ intentId: data.intentId ?? null, title: data.title ?? null });
    else if (data.action === "stop") store.stopTimer();
    else if (data.action === "pause") store.pauseTimer();
    else store.resumeTimer();
  });
}

export const recategorizeFn = ({ data }: Data<{ blockId: string; intentId: string; rememberTitle?: boolean; remember?: boolean }>) =>
  run(() => store.relabel(data.blockId, data.intentId));

export function confirmFn({ data }: Data<{ date: string; accept?: string[]; reject?: string[] }>) {
  return run(() => {
    const s = getState();
    for (const [ids, yes] of [[data.accept ?? [], true], [data.reject ?? [], false]] as const) {
      for (const pid of ids) {
        const b = s.blocks.find((x) => x.proposal?.id === pid);
        if (b) store.answerProposal(b.id, yes);
      }
    }
  });
}

export const uncommitFn = ({ data }: Data<{ blockId: string }>) => run(() => store.uncommit(data.blockId));
export const editBlockFn = ({ data }: Data<{ blockId: string; date: string; start: string; end: string }>) => run(() => store.retime(data.blockId, data.start, data.end));
export const splitBlockFn = ({ data }: Data<{ blockId: string; date: string; at: string }>) => run(() => store.split(data.blockId, data.at));
export const deleteBlockFn = ({ data }: Data<{ blockId: string }>) => run(() => store.deleteBlock(data.blockId));
export const askAgentFn = ({ data }: Data<{ text: string; about?: string }>) => run(() => store.askAgent(data.text, data.about ?? null));
