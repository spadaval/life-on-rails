// The timer: a row at the top of Today's list, and a chip in the rail (in the top bar at phone
// width) that stops or switches it from any page. One vocabulary: a timer without a label is "No
// label yet". Starting, stopping and switching have no Undo: time has already moved on.
import { useRef, useState } from "react";
import { running, startTimer, stopTimer, switchTimer, useNow, useStore } from "../../data/store.js";
import { act, hm, minutes, readout, say, STALE } from "./lib.js";
import { Button, cx, Floating, Icon, LabelPicker } from "./ui.js";

/** Every timer write, refused while LoR is not answering, however the control was reached. */
export function useTimer(stale?: boolean) {
  const s = useStore();
  const r = running(s);
  const held = () => {
    if (stale) say(STALE, true);
    return Boolean(stale);
  };
  return {
    r,
    parent: r?.intent_id ? (s.nodes.find((n) => n.id === r.intent_id)?.parents[0]?.title ?? null) : null,
    stop: (nowMs: number) => {
      if (!r || held()) return;
      act(`Stopped: ${minutes(nowMs - Date.parse(r.t_start))} on ${r.intent ?? "no label yet"}`, stopTimer);
    },
    start: (id: string | null, title: string) => !held() && act(id ? `Timer started on ${title}` : "Timer started, no label yet", () => startTimer({ intentId: id })),
    switchTo: (id: string | null, title: string) => !held() && act(`Switched to ${title}`, () => switchTimer({ intentId: id })),
  };
}

/** The switch picker, floating from the button that opened it. */
export function SwitchFloat({ anchor, onClose, stale }: { anchor: () => HTMLElement | null; onClose: () => void; stale?: boolean }) {
  const now = useNow(1000);
  const { r, switchTo } = useTimer(stale);
  if (!r) return null;
  return (
    <Floating anchor={anchor} onClose={onClose} label="Switch the timer">
      <h3 className="p-float__title">Switch to</h3>
      <p className="p-float__sub">
        Ends {r.intent ?? "this timer"} at {minutes(now - Date.parse(r.t_start))} and starts the new one from now.
      </p>
      <LabelPicker
        current={r.intent_id}
        onPick={(id, title) => {
          onClose();
          switchTo(id, title);
        }}
      />
    </Floating>
  );
}

export function StartFloat({ anchor, onClose, stale }: { anchor: () => HTMLElement | null; onClose: () => void; stale?: boolean }) {
  const { start } = useTimer(stale);
  return (
    <Floating anchor={anchor} onClose={onClose} label="Start a timer">
      <h3 className="p-float__title">Start a timer</h3>
      <LabelPicker
        onPick={(id, title) => {
          onClose();
          start(id, title);
        }}
        extra={
          <button type="button" className="p-option p-option--plain" onClick={() => (onClose(), start(null, ""))}>
            Start without a label <span className="p-option__kind">label it later</span>
          </button>
        }
      />
    </Floating>
  );
}

/** Today's first row: what runs, for how long, Switch and Stop on the row; or how to start one. */
export function TimerRow({ stale, selected, onOpen, initial }: { stale?: boolean; selected: boolean; onOpen: () => void; initial?: string }) {
  const s = useStore();
  const now = useNow(1000);
  const { r, parent, stop, start } = useTimer(stale);
  const [pick, setPick] = useState<"start" | "switch" | null>(initial === "switch" || initial === "start" ? initial : null);
  const anchor = useRef<HTMLButtonElement>(null);
  if (!r) {
    const recent = s.recent.slice(0, 3);
    return (
      <div className="p-timerow is-idle">
        <span className="p-timerow__icon" aria-hidden="true">
          <Icon name="clock" />
        </span>
        <span className="p-timerow__idle">No timer running</span>
        <Button ref={anchor} tone="quiet" size="sm" className="p-timerow__start" disabled={stale} title={stale ? STALE : "Pick what it is for"} onClick={() => setPick("start")}>
          <Icon name="play" /> Start…
        </Button>
        {recent.length ? (
          <div className="p-timerow__recents" role="group" aria-label="Start on something recent">
            {recent.map((x) => (
              <button key={x.id} type="button" className="p-chip" disabled={stale} title={stale ? STALE : `Start a timer on ${x.title}`} onClick={() => start(x.id, x.title)}>
                {x.title}
              </button>
            ))}
          </div>
        ) : null}
        {pick === "start" && !stale ? <StartFloat anchor={() => anchor.current} onClose={() => setPick(null)} stale={stale} /> : null}
      </div>
    );
  }
  return (
    <div className={cx("p-timerow", selected && "is-sel", stale && "is-stale")} data-sel={selected}>
      <span className="p-live" aria-hidden="true" />
      <button type="button" className="p-timerow__what" onClick={onOpen} aria-expanded={selected}>
        {r.intent ?? r.title ?? "No label yet"}
      </button>
      <span className="p-readout" aria-label="Running for">
        {readout(now - Date.parse(r.t_start))}
      </span>
      <span className="p-timerow__meta">
        {parent ? `${parent} · ` : ""}since {hm(r.t_start)}
      </span>
      <div className="p-timerow__acts">
        <Button ref={anchor} tone="quiet" size="sm" disabled={stale} title={stale ? STALE : "Stop this and start another from now"} onClick={() => setPick("switch")}>
          <Icon name="swap" /> Switch
        </Button>
        <Button tone="quiet" size="sm" disabled={stale} title={stale ? STALE : undefined} onClick={() => stop(now)}>
          <Icon name="stop" /> Stop
        </Button>
      </div>
      {pick === "switch" && !stale ? <SwitchFloat anchor={() => anchor.current} onClose={() => setPick(null)} stale={stale} /> : null}
    </div>
  );
}

/** The running timer, on every page: in the rail, or in the top bar at phone width. */
export function TimerChip({ stale, initial }: { stale?: boolean; initial?: string }) {
  const now = useNow(1000);
  const { r, stop } = useTimer(stale);
  const [pick, setPick] = useState(initial === "switch");
  const anchor = useRef<HTMLButtonElement>(null);
  if (!r) return null;
  return (
    <div className={cx("p-tchip", stale && "is-stale")} role="group" aria-label="Timer">
      <span className="p-live" aria-hidden="true" />
      <span className="p-tchip__what">
        <span className="p-tchip__label">{r.intent ?? "No label yet"}</span>
        <span className="p-readout p-readout--sm">{readout(now - Date.parse(r.t_start))}</span>
      </span>
      <span className="p-tchip__btns">
        <button ref={anchor} type="button" className="p-tchip__btn" disabled={stale} title={stale ? STALE : "Switch to something else from now"} onClick={() => setPick(true)}>
          <Icon name="swap" />
          <span>Switch</span>
        </button>
        <button type="button" className="p-tchip__btn" disabled={stale} title={stale ? STALE : "Stop the timer"} onClick={() => stop(now)}>
          <Icon name="stop" />
          <span>Stop</span>
        </button>
      </span>
      {pick && !stale ? <SwitchFloat anchor={() => anchor.current} onClose={() => setPick(false)} stale={stale} /> : null}
    </div>
  );
}
