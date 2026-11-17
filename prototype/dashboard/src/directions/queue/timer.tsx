// The timer: a bar at the top of Today, and a chip in the top bar on every other page, each with
// Switch and Stop. One vocabulary: a timer without a label is "No label yet".
import { useRef, useState } from "react";
import { running, startTimer, stopTimer, switchTimer, useNow, useStore } from "../../data/store.js";
import { act, minutes, readout, say } from "./lib.js";
import { Button, cx, Floating, Icon, LabelPicker } from "./ui.js";

const off = "LoR is not answering; try again when it is back";

/** Every timer write, refused while LoR is not answering (`stale`), however the control was reached. */
function useTimerActions(stale?: boolean) {
  const s = useStore();
  const r = running(s);
  const held = () => {
    if (stale) say(off, true);
    return Boolean(stale);
  };
  return {
    r,
    parent: r?.intent_id ? (s.nodes.find((n) => n.id === r.intent_id)?.parents[0]?.title ?? null) : null,
    stop: (nowMs: number) => {
      if (!r || held()) return;
      const took = minutes(nowMs - Date.parse(r.t_start));
      act(`Stopped: ${took} on ${r.intent ?? "no label yet"}`, stopTimer);
    },
    start: (id: string | null, title: string) => !held() && act(id ? `Timer started on ${title}` : "Timer started, no label yet", () => startTimer({ intentId: id })),
    switchTo: (id: string | null, title: string) => !held() && act(`Switched to ${title}`, () => switchTimer({ intentId: id })),
  };
}

/** Today's timer bar: what runs, for how long, Switch and Stop; or how to start one. */
export function TimerBar({ stale, initial }: { stale?: boolean; initial?: string }) {
  const s = useStore();
  const now = useNow(1000);
  const { r, parent, stop, start, switchTo } = useTimerActions(stale);
  const [pick, setPick] = useState<"start" | "switch" | null>(initial === "switch" || initial === "start" ? initial : null);
  const anchor = useRef<HTMLButtonElement>(null);
  const startAnchor = useRef<HTMLButtonElement>(null);
  if (!r) {
    const recent = s.recent.slice(0, 3);
    return (
      <section className="q-timer q-timer--idle" aria-label="Timer">
        <span className="q-timer__idle">No timer running</span>
        <div className="q-timer__recent">
          {recent.map((x) => (
            <Button key={x.id} tone="ghost" size="sm" disabled={stale} title={stale ? off : `Start a timer on ${x.title}`} onClick={() => start(x.id, x.title)}>
              <Icon name="play" /> {x.title}
            </Button>
          ))}
        </div>
        <Button ref={startAnchor} tone="quiet" disabled={stale} title={stale ? off : undefined} onClick={() => setPick("start")}>
          Start…
        </Button>
        {pick === "start" && !stale ? (
          <Floating anchor={() => startAnchor.current} onClose={() => setPick(null)} label="Start a timer">
            <h3 className="q-float__title">Start a timer</h3>
            <LabelPicker
              onPick={(id, title) => {
                setPick(null);
                start(id, title);
              }}
              extra={
                <button type="button" className="q-option q-option--plain" onClick={() => (setPick(null), start(null, ""))}>
                  Start without a label <span className="q-option__kind">label it later</span>
                </button>
              }
            />
          </Floating>
        ) : null}
      </section>
    );
  }
  return (
    <section className="q-timer" aria-label="Timer">
      <span className="q-timer__dot" aria-hidden="true" />
      <div className="q-timer__what">
        <span className="q-timer__label">{r.intent ?? r.title ?? "No label yet"}</span>
        {parent ? <span className="q-timer__parent">{parent}</span> : null}
      </div>
      <span className="q-readout" aria-label="Running for">
        {readout(now - Date.parse(r.t_start))}
      </span>
      <div className="q-timer__acts">
        <Button ref={anchor} tone="quiet" disabled={stale} title={stale ? off : "Stop this and start another from now"} onClick={() => setPick("switch")}>
          <Icon name="swap" /> Switch
        </Button>
        <Button tone="quiet" disabled={stale} title={stale ? off : undefined} onClick={() => stop(now)}>
          <Icon name="stop" /> Stop
        </Button>
      </div>
      {pick === "switch" && !stale ? (
        <Floating anchor={() => anchor.current} onClose={() => setPick(null)} label="Switch the timer">
          <h3 className="q-float__title">Switch to</h3>
          <p className="q-float__sub">
            Ends {r.intent ?? "this timer"} at {minutes(now - Date.parse(r.t_start))} and starts the new one from now.
          </p>
          <LabelPicker
            current={r.intent_id}
            onPick={(id, title) => {
              setPick(null);
              switchTo(id, title);
            }}
          />
        </Floating>
      ) : null}
    </section>
  );
}

/** The running timer on every page but Today: its label, time, Switch and Stop. */
export function TimerChip({ stale }: { stale?: boolean }) {
  const now = useNow(1000);
  const { r, stop, switchTo } = useTimerActions(stale);
  const [pick, setPick] = useState(false);
  const anchor = useRef<HTMLButtonElement>(null);
  if (!r) return null;
  return (
    <div className="q-tchip" role="group" aria-label="Timer">
      <span className="q-timer__dot" aria-hidden="true" />
      <span className="q-tchip__label">{r.intent ?? "No label yet"}</span>
      <span className="q-readout q-readout--sm">{readout(now - Date.parse(r.t_start))}</span>
      <button ref={anchor} type="button" className={cx("q-tchip__btn")} disabled={stale} title={stale ? off : "Switch"} aria-label="Switch" onClick={() => setPick(true)}>
        <Icon name="swap" />
      </button>
      <button type="button" className="q-tchip__btn" disabled={stale} title={stale ? off : "Stop"} aria-label="Stop" onClick={() => stop(now)}>
        <Icon name="stop" />
      </button>
      {pick && !stale ? (
        <Floating anchor={() => anchor.current} onClose={() => setPick(false)} label="Switch the timer">
          <h3 className="q-float__title">Switch to</h3>
          <LabelPicker
            current={r.intent_id}
            onPick={(id, title) => {
              setPick(false);
              switchTo(id, title);
            }}
          />
        </Floating>
      ) : null}
    </div>
  );
}
