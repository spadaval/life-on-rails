// The day as a compact strip above the queue: what was recorded, the running timer, the rest of
// the calendar, and now. A block is relabelled here, in a popover anchored to it; the popover
// steps through the day's blocks, so a ten-minute block is reachable at phone width too.
import { useMemo, useRef, useState } from "react";
import { blockName, isBare } from "../../data/derive.js";
import { answerProposal, relabel, useNow, useStore } from "../../data/store.js";
import type { BlockView } from "../../data/types.js";
import { act, hm, minutes } from "./lib.js";
import { Button, cx, Floating, Icon, LabelPicker } from "./ui.js";

const HOUR = 3_600_000;
const wants = (b: BlockView) => b.t_end !== null && !b.committed;

function where(b: BlockView): string {
  if (b.origin === "manual") return b.t_end ? "Timer" : "Timer, running";
  if (b.origin === "auto_activity") return `Phone${b.title ? ` · ${b.title}` : ""}`;
  if (b.origin === "place") return b.title ?? "A place";
  if (b.origin === "calendar") return `Calendar${b.title ? ` · ${b.title}` : ""}`;
  return b.origin;
}

export function DayStrip({ stale, initial }: { stale?: boolean; initial?: string }) {
  const s = useStore();
  const now = useNow(30_000);
  const blocks = s.blocks;
  const startMs = Math.floor(Math.min(Date.parse(s.day.wake_start), ...blocks.map((b) => Date.parse(b.t_start))) / HOUR) * HOUR;
  const endMs = Math.ceil(Math.max(Date.parse(s.day.wake_end), now) / HOUR) * HOUR;
  const span = endMs - startMs;
  const pct = (ms: number) => `${((Math.min(Math.max(ms, startMs), endMs) - startMs) / span) * 100}%`;
  const width = (a: number, z: number) => `${(Math.max(z - a, 0) / span) * 100}%`;
  const planned = s.day.planned.filter((p) => !p.all_day && p.fills && Date.parse(p.t_end) > now);
  const toLabel = blocks.filter(wants);
  const ticks = useMemo(() => {
    const out: number[] = [];
    for (let t = startMs; t <= endMs; t += HOUR) if (Number(hm(t).slice(0, 2)) % 3 === 0) out.push(t);
    return out;
  }, [startMs, endMs]);

  const initialId = initial ? (blocks.find((b) => hm(b.t_start) === initial)?.id ?? null) : null;
  const [sel, setSel] = useState<string | null>(initialId);
  const refs = useRef(new Map<string, HTMLButtonElement>());
  const strip = useRef<HTMLDivElement>(null);
  const selected = blocks.find((b) => b.id === sel) ?? null;
  const order = blocks;
  const step = (dir: 1 | -1) => {
    if (!selected) return;
    const i = order.indexOf(selected);
    const next = order[i + dir];
    if (next) setSel(next.id);
  };

  return (
    <section className="q-strip" aria-label="Today's time">
      <div className="q-strip__head">
        <span className="q-label">Today so far</span>
        {toLabel.length ? (
          <button type="button" className="q-strip__ask" onClick={() => setSel(toLabel[0]!.id)}>
            {toLabel.length} to label
            <Icon name="right" />
          </button>
        ) : blocks.length ? (
          <span className="q-strip__ok">All labelled</span>
        ) : (
          <span className="q-strip__ok">Nothing recorded yet. A timer, your phone and the calendar fill this in.</span>
        )}
      </div>
      <div className="q-strip__track" ref={strip}>
        {planned.map((p) => (
          <span
            key={p.event_id}
            className="q-strip__plan"
            style={{ left: pct(Date.parse(p.t_start)), width: width(Math.max(Date.parse(p.t_start), now), Date.parse(p.t_end)) }}
            title={`${hm(p.t_start)} ${p.title ?? "Event"}`}
          />
        ))}
        {blocks.map((b) => {
          const a = Date.parse(b.t_start);
          const z = b.t_end ? Date.parse(b.t_end) : now;
          return (
            <button
              key={b.id}
              ref={(el) => {
                if (el) refs.current.set(b.id, el);
                else refs.current.delete(b.id);
              }}
              type="button"
              className={cx(
                "q-strip__block",
                !b.t_end && "is-running",
                b.t_end && b.committed && "is-labelled",
                wants(b) && (b.proposal || b.intent) && "is-suggested",
                isBare(b) && "is-bare",
                sel === b.id && "is-selected",
              )}
              style={{ left: pct(a), width: width(a, z) }}
              aria-label={`${hm(a)} to ${b.t_end ? hm(z) : "now"}, ${blockName(b)}${wants(b) ? ", to label" : ""}`}
              onClick={() => setSel(sel === b.id ? null : b.id)}
            />
          );
        })}
        <span className="q-strip__now" style={{ left: pct(now) }} aria-hidden="true" />
      </div>
      <div className="q-strip__ticks" aria-hidden="true">
        {ticks.map((t) => (
          <span key={t} style={{ left: pct(t) }}>
            {hm(t)}
          </span>
        ))}
      </div>
      {selected ? (
        <Floating anchor={() => refs.current.get(selected.id) ?? strip.current} onClose={() => setSel(null)} label="Label this time" width={380}>
          <BlockPanel b={selected} stale={stale} now={now} onStep={step} first={order[0]?.id === selected.id} last={order.at(-1)?.id === selected.id} onDone={() => setSel(null)} />
        </Floating>
      ) : null}
    </section>
  );
}

function BlockPanel({ b, stale, now, onStep, first, last, onDone }: { b: BlockView; stale?: boolean; now: number; onStep: (d: 1 | -1) => void; first: boolean; last: boolean; onDone: () => void }) {
  const a = Date.parse(b.t_start);
  const z = b.t_end ? Date.parse(b.t_end) : now;
  const state = !b.t_end ? "Running" : b.committed ? `Counted as ${b.intent}` : b.proposal ? null : b.intent ? `Guessed: ${b.intent}, not counted yet` : "Not labelled";
  return (
    <div className="q-block">
      <header className="q-block__head">
        <button type="button" className="q-iconbtn" aria-label="Earlier block" disabled={first} onClick={() => onStep(-1)}>
          <Icon name="left" />
        </button>
        <div className="q-block__when">
          <span className="q-block__time">
            {hm(a)} – {b.t_end ? hm(z) : "now"}
          </span>
          <span className="q-block__len">{minutes(z - a)}</span>
        </div>
        <button type="button" className="q-iconbtn" aria-label="Later block" disabled={last} onClick={() => onStep(1)}>
          <Icon name="right" />
        </button>
      </header>
      <p className="q-block__where">{where(b)}</p>
      {state ? <p className="q-block__state">{state}</p> : null}
      {b.proposal && b.intent ? (
        <div className="q-suggest">
          <span className="q-suggest__mark" aria-hidden="true" />
          <div className="q-suggest__text">
            <span>The agent thinks this was {b.intent}</span>
            {b.proposal.reason ? <span className="q-suggest__why">{b.proposal.reason}</span> : null}
          </div>
          <Button
            tone="quiet"
            size="sm"
            disabled={stale}
            onClick={() => {
              act(`Counted as ${b.intent}`, () => answerProposal(b.id, true), { undoable: true });
              onDone();
            }}
          >
            Yes
          </Button>
        </div>
      ) : null}
      <fieldset className="q-block__pick" disabled={stale}>
        <legend>{b.committed || !b.t_end ? "Relabel as" : "Label as"}</legend>
        <LabelPicker
          autoFocus={false}
          current={b.intent_id}
          onPick={(id, title) => {
            if (!id) return;
            act(`Relabelled ${hm(a)}–${b.t_end ? hm(z) : "now"} as ${title}`, () => relabel(b.id, id), { undoable: true });
            onDone();
          }}
        />
      </fieldset>
      {stale ? <p className="q-block__state">LoR is not answering, so labels wait until it is back.</p> : null}
    </div>
  );
}
