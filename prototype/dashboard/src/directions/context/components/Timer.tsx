// The timer, in three places: the context's own header (when it runs here), a chip in the bar on
// every other page (so Stop and Switch are never a page away), and the Switch picker.
import { relabel, running, startTimer, stopTimer, switchTimer, useNow, useStore } from "../../../data/store.js";
import { blockCtx, runningBlock } from "../lib/model.js";
import { setParams } from "../lib/nav.js";
import { clock, hm } from "../lib/time.js";
import { oneWay, reversible, useMode, useOff } from "../lib/ui.js";
import { Icon } from "./bits.js";
import { anchorFrom, Layer } from "./Layer.js";
import { Picker } from "./Picker.js";

export const runningLabel = (b: { intent: string | null; title: string | null }) => b.intent ?? b.title ?? "No label yet";

/** Elapsed on the running block; frozen at LoR's last word when it is unreachable. */
export function useElapsed(): number {
  const s = useStore();
  const now = useNow(1000);
  const { staleSince } = useMode();
  const r = runningBlock(s);
  if (!r) return 0;
  const end = r.paused_at ? Date.parse(r.paused_at) : (staleSince ?? now);
  return end - Date.parse(r.t_start);
}

export function stop() {
  const r = running();
  oneWay(r?.intent_id ? `Stopped. ${runningLabel(r)} has its time.` : "Stopped. The block waits for a name.", stopTimer);
}

/** The chip in the bar: the running label and clock, with Stop and Switch, from any page. */
export function TimerChip({ onOpen }: { onOpen: () => void }) {
  const s = useStore();
  const off = useOff();
  const elapsed = useElapsed();
  const r = runningBlock(s);
  if (!r) return null;
  const c = blockCtx(s, r);
  return (
    <div className="cx-timerchip" role="group" aria-label="Running timer">
      <button type="button" className="cx-timerchip-main" onClick={onOpen} title={c ? `Open ${c.title}` : "Open the timer"}>
        <span className="cx-live-dot" aria-hidden="true" />
        <span className="cx-timerchip-label">{runningLabel(r)}</span>
        <span className="cx-num cx-timerchip-clock">{clock(elapsed)}</span>
      </button>
      <button
        type="button"
        className="cx-iconbtn cx-iconbtn-sm"
        aria-label="Switch the timer to something else"
        title="Switch"
        disabled={off}
        onClick={(e) => {
          anchorFrom(e);
          setParams({ timer: "switch" });
        }}
      >
        <Icon name="switch" size={15} />
      </button>
      <button type="button" className="cx-iconbtn cx-iconbtn-sm cx-iconbtn-stop" aria-label="Stop the timer" title="Stop" disabled={off} onClick={stop}>
        <Icon name="stop" size={15} />
      </button>
    </div>
  );
}

/** Switch (or start) from a picker. Opened by `timer=switch` or `timer=start`. */
export function TimerLayer({ mode, onDone }: { mode: string; onDone: (intentId: string) => void }) {
  const s = useStore();
  const off = useOff();
  const now = useNow(1000);
  const r = runningBlock(s);
  const close = () => setParams({ timer: undefined });
  const switching = mode === "switch" && r;
  return (
    <Layer
      label={switching ? "Switch the timer" : "Start a timer"}
      onClose={close}
      title={
        switching ? (
          <>
            <span className="cx-label">Switch from</span>
            <span className="cx-float-name">{runningLabel(r)}</span>
          </>
        ) : (
          <span className="cx-float-name">Start a timer on…</span>
        )
      }
    >
      {switching ? <p className="cx-float-note">The block on {runningLabel(r)} ends now and the new one starts from {hm(now)}.</p> : null}
      <Picker
        current={r?.intent_id ?? null}
        disabled={off}
        onPick={(c) => {
          close();
          if (switching) {
            if (c.id === r.intent_id) return;
            oneWay(`Switched to ${c.title}`, () => switchTimer({ intentId: c.id }));
          } else oneWay(`Timer started on ${c.title}`, () => startTimer({ intentId: c.id }));
          onDone(c.id);
        }}
      />
      {switching ? (
        <p className="cx-float-foot">
          Or{" "}
          <button type="button" className="cx-linkbtn" onClick={() => setParams({ timer: undefined, blk: r.id })}>
            relabel the whole block since {hm(r.t_start)}
          </button>{" "}
          instead.
        </p>
      ) : null}
    </Layer>
  );
}

/** Relabel one block of time, by id (`blk=`) or by its start (`block=HH:MM`, for links). */
export function BlockLayer({ blockId }: { blockId: string }) {
  const s = useStore();
  const off = useOff();
  const b = s.blocks.find((x) => x.id === blockId);
  const close = () => setParams({ blk: undefined, block: undefined });
  if (!b) return null;
  const range = `${hm(b.t_start)}–${b.t_end ? hm(b.t_end) : "now"}`;
  return (
    <Layer
      label="Relabel this time"
      onClose={close}
      title={
        <>
          <span className="cx-label cx-num">{range}</span>
          <span className="cx-float-name">{b.intent ?? (b.origin === "manual" ? "A timer with no label" : (b.title ?? "Unnamed time"))}</span>
        </>
      }
    >
      <p className="cx-float-note">
        {b.origin === "auto_activity" ? `Phone: ${b.title}.` : b.origin === "place" ? `${b.title}.` : b.origin === "calendar" ? `Calendar: ${b.title}.` : "From the timer."}{" "}
        {b.proposal && !b.committed ? "The agent guessed this label; pick another or confirm it from the context." : "Pick what it was. It moves to that context’s time."}
      </p>
      <Picker
        current={b.intent_id}
        disabled={off}
        onPick={(c) => {
          close();
          reversible(`${range} is now ${c.title}`, () => relabel(b.id, c.id));
        }}
      />
    </Layer>
  );
}
