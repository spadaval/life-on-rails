// Beside the context, flat on the ground: the morning brief (before noon), the next context (the
// next fixed event and the gap before it), what waits on him elsewhere, and time nobody named.
// Each thing here opens in its own context.
import { useState } from "react";
import { setDone, today as todayOf, unflag, useNow, useStore } from "../../../data/store.js";
import type { Route } from "../../../shell/nav.js";
import { Check, Icon, kindIcon, Prose } from "../components/bits.js";
import { anchorFrom } from "../components/Layer.js";
import { type Ctx, ctxPath, eventNow, homeOf, nextEvent, unnamedTime, type Waiting, waitingElsewhere, blockSource } from "../lib/model.js";
import { href, setParams } from "../lib/nav.js";
import { at, dueWords, dur, hm } from "../lib/time.js";
import { crowdSee, crowdUnsee, reversible, say, useCrowdSeen, useMode, useOff, usePhone } from "../lib/ui.js";
import { Guess } from "./ContextView.js";

export function Side({ here, route }: { here: Ctx; route: Route }) {
  const s = useStore();
  const now = useNow(1000);
  const { crowded } = useMode();
  const seen = useCrowdSeen();
  const today = todayOf();
  const waiting = waitingElsewhere(s, here, today, crowded, seen);
  const morning = now < at(today, "12:00");
  return (
    <aside className="cx-side" aria-label="Beside it">
      {s.brief ? <Brief text={s.brief} open={morning} /> : null}
      <NextCard here={here} />
      <WaitingList items={waiting} crowded={crowded} />
      <Unnamed route={route} />
    </aside>
  );
}

function Brief({ text, open }: { text: string; open: boolean }) {
  const [shown, setShown] = useState(open);
  const first = text.split("\n").find((l) => l.trim()) ?? "";
  return (
    <section className="cx-card cx-brief" data-open={shown || undefined}>
      <header className="cx-card-head">
        <h2 className="cx-label cx-label-cyan">
          <Icon name="agent" size={14} /> This morning’s brief
        </h2>
        <button type="button" className="cx-linkbtn" onClick={() => setShown(!shown)} aria-expanded={shown}>
          {shown ? "Fold" : "Read"}
        </button>
      </header>
      {shown ? <Prose body={text} /> : <p className="cx-brief-line">{first}</p>}
    </section>
  );
}

function NextCard({ here }: { here: Ctx }) {
  const s = useStore();
  const now = useNow(1000);
  const nx = nextEvent(s, now);
  const during = eventNow(s, now);
  if (!nx) {
    return (
      <section className="cx-card cx-next">
        <h2 className="cx-label cx-label-cyan">Next</h2>
        <p className="cx-next-none">Nothing else fixed today. The rest of the day is yours.</p>
      </section>
    );
  }
  const e = nx.event;
  const isHere = here.kind === "slot" && here.key === e.event_id;
  return (
    <section className="cx-card cx-next" data-here={isHere || undefined}>
      <h2 className="cx-label cx-label-cyan">Next context</h2>
      <a className="cx-next-main" href={href("/slot", { id: e.title ?? e.event_id })} aria-current={isHere ? "page" : undefined}>
        <span className="cx-next-at cx-num">{hm(e.t_start)}</span>
        <span className="cx-next-text">
          <span className="cx-next-title">{e.title}</span>
          <span className="cx-next-meta">
            in {dur(nx.gapMs)} · until {hm(e.t_end)}
          </span>
        </span>
        <Icon name="chevron" size={16} />
      </a>
      {during ? (
        <p className="cx-next-during">
          <Icon name="calendar" size={14} /> Now: {during.title} until {hm(during.t_end)}
        </p>
      ) : null}
      <p className="cx-next-free">
        <span className="cx-num">{dur(nx.gapMs)}</span> free before it. Nothing is put there unasked.
      </p>
    </section>
  );
}

const SHOW = 4;

function WaitingList({ items, crowded }: { items: Waiting[]; crowded: boolean }) {
  const phone = usePhone();
  const [all, setAll] = useState(false);
  const limit = phone ? 3 : SHOW;
  const shown = all ? items : items.slice(0, limit);
  const flags = items.filter((w) => w.kind === "flag").length;
  return (
    <section className="cx-card cx-waiting" data-crowded={crowded || undefined}>
      <header className="cx-card-head">
        <h2 className="cx-label">
          Waiting elsewhere {items.length ? <span className="cx-count" data-loud={items.length > 6 || undefined}>{items.length}</span> : null}
        </h2>
        {flags ? <span className="cx-card-meta">{flags} flagged by the agent</span> : null}
      </header>
      {items.length ? (
        <>
          <ul className="cx-wait-list">
            {shown.map((w) => (
              <WaitingRow key={w.id} w={w} />
            ))}
          </ul>
          {items.length > limit ? (
            <button type="button" className="cx-morebtn" onClick={() => setAll(!all)} aria-expanded={all}>
              {all ? "Show fewer" : `${items.length - limit} more waiting, oldest first`}
              <Icon name="down" size={14} />
            </button>
          ) : null}
        </>
      ) : (
        <p className="cx-quiet">
          <Icon name="check" size={16} /> Nothing waits on you elsewhere. No flags, nothing due today.
        </p>
      )}
    </section>
  );
}

function WaitingRow({ w }: { w: Waiting }) {
  const off = useOff();
  const today = todayOf();
  const n = w.node;
  const ctxLink = href(ctxPath(w.home).path, { ...ctxPath(w.home).params, task: n.title });
  const done = () => reversible(`Done: ${n.title}`, () => setDone(n.id, true));
  const seenIt = () => {
    if (w.madeUp) {
      crowdSee(n.id);
      say(`Seen: ${n.title}`, { undo: () => { crowdUnsee(n.id); say("Undone"); } });
    } else reversible(`Seen: ${n.title}`, () => unflag(n.id));
  };
  return (
    <li className="cx-wait" data-kind={w.kind}>
      {w.kind === "due" ? <Check checked={false} onChange={(v) => v && done()} label={`Mark ${n.title} done`} disabled={off || n.google_task_id !== null} /> : <span className="cx-wait-flag" aria-hidden="true"><Icon name="flag" size={14} /></span>}
      <div className="cx-wait-body">
        <a className="cx-wait-title" href={ctxLink}>
          {n.title}
        </a>
        {w.note ? <p className="cx-wait-note">{w.note}</p> : null}
        <p className="cx-wait-meta">
          <Icon name={kindIcon(w.home.kind)} size={12} /> {w.home.kind === "loose" ? "Loose ends" : w.home.title}
          {n.due_local ? <span data-late={n.overdue || undefined}> · {dueWords(n.due_local, today)}</span> : null}
        </p>
        {w.kind === "flag" ? (
          <div className="cx-wait-acts">
            <button type="button" className="cx-btn cx-btn-sm" disabled={off} onClick={done}>
              <Icon name="check" size={14} /> Done
            </button>
            <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" disabled={off} onClick={seenIt}>
              Seen
            </button>
          </div>
        ) : null}
      </div>
    </li>
  );
}

/** Time nobody named and the agent's guesses: time in no context yet. */
function Unnamed({ route }: { route: Route }) {
  const s = useStore();
  const off = useOff();
  const u = unnamedTime(s);
  const [open, setOpen] = useState(route.params.time === "open");
  if (!u.bare.length && !u.guesses.length) return null;
  return (
    <section className="cx-card cx-unnamedtime">
      <header className="cx-card-head">
        <h2 className="cx-label">Time in no context</h2>
        <button type="button" className="cx-linkbtn" onClick={() => setOpen(!open)} aria-expanded={open}>
          {open ? "Fold" : "Name it"}
        </button>
      </header>
      <p className="cx-unnamed-sum">
        {u.bare.length ? <><span className="cx-num">{dur(u.bareMs)}</span> unnamed in {u.bare.length} stretches</> : null}
        {u.bare.length && u.guesses.length ? " · " : null}
        {u.guesses.length ? <><span className="cx-num">{u.guesses.length}</span> guesses to confirm</> : null}
      </p>
      {open ? (
        <ul className="cx-blocks">
          {u.bare.map((b) => (
            <li key={b.id}>
              <span className="cx-num cx-block-at">{hm(b.t_start)}–{hm(b.t_end!)}</span>
              <span className="cx-block-name">{b.title ?? "Unnamed"}</span>
              <span className="cx-block-src">{blockSource(b) === b.title ? (b.origin === "auto_activity" ? "Phone" : "Place") : blockSource(b)}</span>
              <button
                type="button"
                className="cx-linkbtn"
                disabled={off}
                onClick={(e) => {
                  anchorFrom(e);
                  setParams({ blk: b.id, block: undefined });
                }}
              >
                Name it
              </button>
            </li>
          ))}
          {u.guesses.map((b) => (
            <Guess key={b.id} b={b} />
          ))}
        </ul>
      ) : null}
    </section>
  );
}
