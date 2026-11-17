// A block on the route, opened in place: what it was, the agent's guess if there is one, and
// a label with one press. Splitting, retiming and deleting are under More, because they are rare.
import { useState } from "react";
import { answerProposal, deleteBlock, relabel, retime, split, switchTimer, useNow, useStore } from "../../../data/store.js";
import { blockSource, blockTitle, toneOf } from "../lib/day.js";
import { useMode } from "../lib/ctx.js";
import { clock, dur, hm } from "../lib/time.js";
import { oneWay, reversible } from "../lib/ui.js";
import { Icon } from "./bits.js";
import { LabelPicker } from "./LabelPicker.js";

export function BlockEditor({ id, onDone, startMode }: { id: string; onDone: () => void; startMode?: "switch" | "relabel" }) {
  const s = useStore();
  const now = useNow(1000);
  const { staleSince } = useMode();
  const b = s.blocks.find((x) => x.id === id);
  const [mode, setMode] = useState<"switch" | "relabel">(startMode ?? "switch");
  const [cut, setCut] = useState("");
  const [from, setFrom] = useState(b ? hm(b.t_start) : "");
  const [to, setTo] = useState(b?.t_end ? hm(b.t_end) : "");
  if (!b) return <p className="tl-muted">This block is gone.</p>;
  const off = staleSince !== null;
  const tone = toneOf(b);
  const start = Date.parse(b.t_start);
  const end = b.t_end ? Date.parse(b.t_end) : now;
  const live = b.t_end === null;

  if (live && mode === "switch") {
    return (
      <div className="tl-editor">
        <p className="tl-kicker">
          Running since {hm(start)} · <span className="tl-num">{clock(end - start)}</span>
        </p>
        <h2 className="tl-editor-title">Switch from {b.intent ?? "Unlabelled"}</h2>
        <p className="tl-muted">This block ends now and a new one starts on what you pick.</p>
        {off ? <p className="tl-note">LoR can’t be reached, so the timer can’t change until it answers.</p> : (
          <LabelPicker
            current={b.intent_id}
            allowNone
            onPick={(lid, title) => {
              oneWay(`Switched to ${title}`, () => switchTimer({ intentId: lid }));
              onDone();
            }}
          />
        )}
        <button type="button" className="tl-link" onClick={() => setMode("relabel")}>
          Relabel this block instead, from {hm(start)} <Icon name="chevron" size={14} />
        </button>
      </div>
    );
  }

  return (
    <div className="tl-editor">
      <p className="tl-kicker">
        {hm(start)} to {live ? "now" : hm(end)} · {dur(end - start)}
      </p>
      <h2 className="tl-editor-title">{tone === "bare" ? `${dur(end - start)}, unlabelled` : blockTitle(b)}</h2>
      <p className="tl-muted">
        <Icon name={b.origin === "auto_activity" ? "phone" : b.origin === "place" ? "pin" : b.origin === "calendar" ? "calendar" : "today"} size={14} /> {blockSource(b)}
        {tone === "tracked" && b.intent ? ` · counted toward ${b.intent}` : ""}
      </p>
      {off ? <p className="tl-note">LoR can’t be reached. You can look, but labels wait until it answers.</p> : null}
      {tone === "proposed" && !off ? (
        <div className="tl-suggest">
          <p>
            <Icon name="spark" size={14} /> The agent thinks this was <strong>{b.intent}</strong>.
          </p>
          <div className="tl-row-actions">
            <button type="button" className="tl-btn tl-btn-primary" data-autofocus onClick={() => { reversible(`Confirmed: ${b.intent}`, () => answerProposal(b.id, true)); onDone(); }}>
              <Icon name="check" size={16} /> Yes, {b.intent}
            </button>
            <button type="button" className="tl-btn" onClick={() => reversible("Turned the suggestion down", () => answerProposal(b.id, false))}>
              No
            </button>
          </div>
        </div>
      ) : null}
      {!off ? (
        <section className="tl-field">
          <h3 className="tl-label">{tone === "bare" ? "Label it" : "Label it as something else"}</h3>
          <LabelPicker
            current={b.committed ? b.intent_id : null}
            onPick={(lid, title) => {
              if (!lid) return;
              reversible(`Relabelled ${hm(start)} as ${title}`, () => relabel(b.id, lid));
              onDone();
            }}
          />
        </section>
      ) : null}
      {!off && !live ? (
        <details className="tl-more">
          <summary>
            Split, change times, delete <Icon name="down" size={14} />
          </summary>
          <div className="tl-more-body">
            <form className="tl-inline-form" onSubmit={(e) => { e.preventDefault(); if (cut) reversible(`Split at ${cut}`, () => split(b.id, cut)); }}>
              <label>
                Split at <input type="time" className="tl-input" value={cut} min={hm(start)} max={hm(end)} onChange={(e) => setCut(e.target.value)} />
              </label>
              <button type="submit" className="tl-btn" disabled={!cut}>Split</button>
            </form>
            {b.origin === "manual" ? (
              <form className="tl-inline-form" onSubmit={(e) => { e.preventDefault(); reversible(`Times changed to ${from}–${to}`, () => retime(b.id, from, to)); }}>
                <label>
                  From <input type="time" className="tl-input" value={from} onChange={(e) => setFrom(e.target.value)} />
                </label>
                <label>
                  to <input type="time" className="tl-input" value={to} onChange={(e) => setTo(e.target.value)} />
                </label>
                <button type="submit" className="tl-btn">Save</button>
              </form>
            ) : (
              <p className="tl-muted">Times come from {blockSource(b).toLowerCase()}; split it to label part of it.</p>
            )}
            <button type="button" className="tl-btn tl-btn-danger" onClick={() => { reversible("Deleted the block", () => deleteBlock(b.id)); onDone(); }}>
              Delete this block
            </button>
          </div>
        </details>
      ) : null}
    </div>
  );
}
