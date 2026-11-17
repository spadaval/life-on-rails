// Today: one ranked list of what wants him, read top to bottom. The timer bar and the day strip
// sit above it; the head of the list is the lead card in the "now" reticle; clearing it slides
// the next one up into its place. Every other row has its own action and opens in place.
import { useState } from "react";
import { doneOn, weekGoals } from "../../../data/derive.js";
import { running, startTimer, useNow, useStore } from "../../../data/store.js";
import { localHm } from "../../../../../../src/shared/localTime.js";
import { TZ } from "../../../data/store.js";
import type { Route } from "../../../shell/nav.js";
import { setParams } from "../../../shell/nav.js";
import { act, dueWords, hm, type Item, modeOf, parentTitle, qhref, queue, setLocal, useLocal } from "../lib.js";
import { FlagActions, FlagRow, TaskFields, TaskRow, tick } from "../rows.js";
import { DayStrip } from "../strip.js";
import { TimerBar } from "../timer.js";
import { Button, cx, Icon, Prose, Reticle } from "../ui.js";

const SHOWN = 7;

export function Today({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(30_000);
  const l = useLocal();
  const mode = modeOf(route.params);
  const stale = mode === "stale";
  const items = queue(s, mode, l, now);
  const [lead, ...rest] = items;
  const openId = route.params.id ?? null;
  const isOpen = (it: Item) => openId === it.title;
  const setOpen = (it: Item, v: boolean) => setParams({ id: v ? it.title : undefined });
  const [more, setMore] = useState(false);
  const hiddenOpen = rest.slice(SHOWN).some(isOpen);
  const shown = more || hiddenOpen || rest.length <= SHOWN + 1 ? rest : rest.slice(0, SHOWN);
  const morning = Number(localHm(TZ, new Date(now)).slice(0, 2)) < 12;
  const next = s.day.planned.find((p) => !p.all_day && Date.parse(p.t_start) > now) ?? null;

  return (
    <div className="q-col q-today">
      <TimerBar stale={stale} initial={route.params.timer} />
      <DayStrip stale={stale} initial={route.params.block} />

      {lead ? (
        <Lead item={lead} open={isOpen(lead)} onOpen={(v) => setOpen(lead, v)} stale={stale} next={next ? `${hm(next.t_start)} ${next.title ?? "an event"}` : null} />
      ) : (
        <Clear next={next ? `${hm(next.t_start)} ${next.title ?? "an event"}` : null} />
      )}

      {s.brief && morning ? <Brief body={s.brief} open={route.params.brief === "1"} /> : null}

      {rest.length ? (
        <section className="q-section" aria-labelledby="q-then">
          <h2 className="q-label" id="q-then">
            Then <span className="q-count">{rest.length}</span>
          </h2>
          <ul className="q-list">
            {shown.map((it) =>
              it.kind === "task" ? (
                <TaskRow key={it.id} node={it.node} open={isOpen(it)} onOpen={(v) => setOpen(it, v)} stale={stale} />
              ) : (
                <FlagRow key={it.id} node={it.node} note={it.kind === "flag" ? it.note : it.why} made={it.kind === "flag" && it.made} open={isOpen(it)} onOpen={(v) => setOpen(it, v)} stale={stale} />
              ),
            )}
          </ul>
          {shown.length < rest.length ? (
            <button type="button" className="q-more" onClick={() => setMore(true)}>
              {rest.length - shown.length} more waiting
              <Icon name="chev" />
            </button>
          ) : null}
        </section>
      ) : null}

      <DoneToday stale={stale} />
      {s.brief && !morning ? <Brief body={s.brief} open={route.params.brief === "1"} afternoon /> : null}
      <Week />
    </div>
  );
}

function Lead({ item, open, onOpen, stale, next }: { item: Item; open: boolean; onOpen: (v: boolean) => void; stale?: boolean; next: string | null }) {
  const s = useStore();
  const r = running(s);
  const n = item.node;
  const onIt = r?.intent_id === n.id;
  const label = item.kind === "one" ? "The one thing" : item.kind === "flag" ? "Flagged by the agent" : n.overdue ? "Waiting since" : "Due today";
  const due = dueWords(n.due_local, s.day.date);
  const parent = parentTitle(n);
  const off = stale ? "LoR is not answering; try again when it is back" : undefined;
  return (
    <article className={cx("q-lead", item.kind === "flag" && "is-agent")} aria-label="Next up" key={item.id}>
      <div className="q-lead__body">
        <header className="q-lead__top">
          <span className={cx("q-cap", item.kind === "flag" && "q-cap--agent")}>
            {label}
            {item.kind === "task" && n.overdue && due ? ` ${due.replace("was due ", "")}` : null}
          </span>
          {onIt ? <span className="q-cap q-cap--live">Timer running on it</span> : null}
        </header>
        <h1 className="q-lead__title">{n.title}</h1>
        <p className="q-lead__why">
          {item.kind === "one" ? item.why : item.kind === "flag" ? item.note : [due, parent].filter(Boolean).join(" · ")}
          {next && item.kind === "one" ? <span className="q-lead__next"> Next on the calendar: {next}.</span> : null}
        </p>
        {item.kind !== "task" && (due || parent) ? <p className="q-lead__meta">{[due, parent].filter(Boolean).join(" · ")}</p> : null}
        <div className="q-lead__acts">
          {item.kind === "flag" ? (
            <FlagActions node={n} made={item.made} stale={stale} big />
          ) : (
            <Button tone="primary" disabled={stale || n.google_task_id !== null} title={off ?? (n.google_task_id ? "Lives in Google Tasks: tick it off there" : undefined)} onClick={() => tick(n, true)}>
              Done
            </Button>
          )}
          {!onIt ? (
            <Button tone="quiet" disabled={stale} title={off} onClick={() => act(`Timer started on ${n.title}`, () => startTimer({ intentId: n.id }))}>
              <Icon name="play" /> Start timer
            </Button>
          ) : null}
          {item.kind === "one" ? (
            <Button
              tone="ghost"
              disabled={stale}
              title={off ?? "Put it aside for today; it stays on Tasks"}
              onClick={() => {
                setLocal((x) => ({ ...x, aside: true }));
                act("Set aside for today", () => {}, { localUndo: () => setLocal((x) => ({ ...x, aside: false })) });
              }}
            >
              Not today
            </Button>
          ) : null}
          <Button tone="ghost" aria-expanded={open} onClick={() => onOpen(!open)}>
            {open ? "Close" : "Open"}
          </Button>
        </div>
        {open ? <TaskFields node={n} stale={stale} /> : null}
      </div>
      <Reticle live={onIt} />
    </article>
  );
}

function Clear({ next }: { next: string | null }) {
  const s = useStore();
  const soon = s.nodes
    .filter((n) => n.type === "task" && n.status === "active" && n.assignee === "owner" && n.due_local && n.due_local > s.day.date)
    .sort((a, b) => a.due_at!.localeCompare(b.due_at!))[0];
  return (
    <article className="q-lead q-lead--clear" aria-label="Next up">
      <div className="q-lead__body">
        <header className="q-lead__top">
          <span className="q-cap">All clear</span>
        </header>
        <h1 className="q-lead__title">Nothing wants you.</h1>
        <p className="q-lead__why">
          No flags and nothing due today.
          {next ? ` Next on the calendar: ${next}.` : " Nothing else on the calendar today."}
        </p>
        {soon ? (
          <p className="q-lead__meta">
            When you want something: <a href={qhref("/tasks", { id: soon.title })}>{soon.title}</a>, {dueWords(soon.due_local, s.day.date)}.
          </p>
        ) : null}
      </div>
      <Reticle />
    </article>
  );
}

function Brief({ body, open, afternoon }: { body: string; open: boolean; afternoon?: boolean }) {
  const [shown, setShown] = useState(open);
  const first = body.split("\n\n")[0] ?? body;
  return (
    <section className={cx("q-brief", afternoon && "q-brief--late")} aria-labelledby="q-brief">
      <h2 className="q-label" id="q-brief">
        {afternoon ? "This morning's brief" : "The brief"}
      </h2>
      {shown ? <Prose source={body} /> : <p className="q-brief__first">{first}</p>}
      <button type="button" className="q-link" aria-expanded={shown} onClick={() => setShown(!shown)}>
        {shown ? "Show less" : "Read it all"}
      </button>
    </section>
  );
}

function DoneToday({ stale }: { stale?: boolean }) {
  const s = useStore();
  const now = useNow(60_000);
  // The snapshot's done tasks carry the afternoon's times; at 09:00 only those done by now count.
  const done = doneOn(s, s.day.date).filter((t) => {
    const at = s.nodes.find((x) => x.id === t.id)?.done_at;
    return at && Date.parse(at) <= now;
  });
  const [open, setOpen] = useState(false);
  if (!done.length) return null;
  return (
    <section className="q-section q-done">
      <button type="button" className="q-label q-label--btn" aria-expanded={open} onClick={() => setOpen(!open)}>
        Done today <span className="q-count">{done.length}</span>
        <Icon name="chev" />
      </button>
      {open ? (
        <ul className="q-list q-list--done">
          {done.map((t) => {
            const n = s.nodes.find((x) => x.id === t.id)!;
            return <TaskRow key={t.id} node={n} open={false} onOpen={() => setParams({ id: undefined })} stale={stale} showDone />;
          })}
        </ul>
      ) : null}
    </section>
  );
}

function Week() {
  const s = useStore();
  const goals = weekGoals(s);
  if (!goals.length) return null;
  return (
    <footer className="q-week">
      <a className="q-label q-label--link" href={qhref("/goals")}>
        This week
      </a>
      <ul className="q-week__list">
        {goals.map((g) => (
          <li key={g.id}>
            <a href={qhref("/goals", { id: g.title })}>
              <span className="q-week__name">{g.title}</span>
              <span className="q-week__num">{g.hours > 0 ? `${fmtH(g.hours)} of ${fmtH(g.target)} h` : "no data yet"}</span>
            </a>
          </li>
        ))}
      </ul>
    </footer>
  );
}

export const fmtH = (h: number) => (Math.round(h * 10) / 10).toString();
