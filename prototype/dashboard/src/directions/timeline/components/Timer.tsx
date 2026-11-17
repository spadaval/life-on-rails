// The timer away from Today: a chip in the top bar on every other page (and at phone width),
// with Stop and Switch, so a timer never has to be chased back to the route to end it.
import { running, startTimer, stopTimer, useNow, useStore } from "../../../data/store.js";
import { setParams } from "../lib/nav.js";
import { useMode } from "../lib/ctx.js";
import { clock } from "../lib/time.js";
import { oneWay } from "../lib/ui.js";
import { Icon } from "./bits.js";
import { BlockEditor } from "./BlockEditor.js";
import { LabelPicker } from "./LabelPicker.js";
import { Popover } from "./Popover.js";

export function TimerChip({ params }: { params: Record<string, string> }) {
  const s = useStore();
  const now = useNow(1000);
  const { staleSince } = useMode();
  const r = running(s);
  const off = staleSince !== null;
  if (!r) {
    return (
      <div className="tl-timerchip" data-idle>
        <button type="button" id="tl-chip-start" className="tl-btn tl-btn-quiet" disabled={off} onClick={() => setParams({ start: params.start ? undefined : 1 })}>
          <Icon name="play" size={14} /> Start a timer
        </button>
        {params.start ? (
          <Popover anchorId="tl-chip-start" label="Start a timer" onClose={() => setParams({ start: undefined })}>
            <div className="tl-editor">
              <p className="tl-kicker">Starts now</p>
              <h2 className="tl-editor-title">What are you doing?</h2>
              <LabelPicker
                allowNone
                onPick={(id, title) => {
                  oneWay(`Started: ${title}`, () => startTimer({ intentId: id }));
                  setParams({ start: undefined });
                }}
              />
            </div>
          </Popover>
        ) : null}
      </div>
    );
  }
  const since = Date.parse(r.t_start);
  return (
    <div className="tl-timerchip" role="group" aria-label="Timer">
      <span className="tl-live-dot" aria-hidden="true" />
      <span className="tl-timerchip-label">{r.intent ?? "Unlabelled"}</span>
      <span className="tl-num tl-timerchip-clock">{clock((staleSince ?? now) - since)}</span>
      <button type="button" id="tl-chip-switch" className="tl-btn tl-btn-quiet tl-btn-sm" disabled={off} onClick={() => setParams({ switch: params.switch ? undefined : 1 })}>
        <Icon name="switch" size={14} /> Switch
      </button>
      <button type="button" className="tl-btn tl-btn-sm" disabled={off} onClick={() => oneWay(`Stopped: ${r.intent ?? "Unlabelled"}, ${clock(now - since)}`, stopTimer)}>
        <Icon name="stop" size={14} /> Stop
      </button>
      {params.switch ? (
        <Popover anchorId="tl-chip-switch" label="Switch the timer" onClose={() => setParams({ switch: undefined })}>
          <BlockEditor id={r.id} onDone={() => setParams({ switch: undefined })} />
        </Popover>
      ) : null}
    </div>
  );
}
