// What opens in the detail pane: a task (or a flag on one), a stretch of time to label, the
// running timer, a calendar event. The pane is for what the row cannot do: its common fields
// first, saving as they change, and what is uncommon under More, collapsed.
import { useEffect, useState } from "react";
import { addDays } from "../../../../../src/shared/localTime.js";
import { answerProposal, deleteBlock, deleteIntent, relabel, running, setParent, split, startTimer, updateIntent, useNow, useStore } from "../../data/store.js";
import type { BlockView, Node, PlannedEvent } from "../../data/types.js";
import { act, ago, dueWords, type Flag, hm, minutes, readout, say, shortDay, STALE } from "./lib.js";
import { type Detail, More } from "./panes.js";
import { FlagActions, GOOGLE, tick, where } from "./rows.js";
import { useTimer } from "./timer.js";
import { Button, cx, Icon, LabelPicker } from "./ui.js";

// ---- a task ----

export function taskDetail(n: Node, close: () => void, opts: { flag?: Flag; stale?: boolean; backLabel?: string } = {}): Detail {
  const { flag, stale } = opts;
  const done = n.status === "done";
  const kicker = flag ? (flag.made ? "Flagged by the agent" : "Flagged by the agent") : n.assignee === "agent" ? "Task · the agent is doing it" : done ? "Task · done" : "Task";
  return {
    key: `task-${n.id}`,
    kicker,
    title: n.title,
    tone: flag ? "agent" : undefined,
    close,
    backLabel: opts.backLabel,
    actions: flag ? <FlagActions flag={flag} stale={stale} big /> : <TaskActions n={n} stale={stale} />,
    body: <TaskBody key={n.id} n={n} flag={flag} stale={stale} />,
  };
}

function TaskActions({ n, stale }: { n: Node; stale?: boolean }) {
  const s = useStore();
  const r = running(s);
  const done = n.status === "done";
  const g = n.google_task_id !== null;
  const onIt = r?.intent_id === n.id;
  return (
    <>
      <Button tone={done ? "quiet" : "primary"} disabled={stale || g} title={stale ? STALE : g ? GOOGLE : undefined} onClick={() => tick(n, !done)}>
        {done ? "Mark not done" : "Done"}
      </Button>
      {onIt ? (
        <span className="p-pill is-live">
          <span className="p-live" aria-hidden="true" /> Timer running on it
        </span>
      ) : (
        <Button tone="quiet" disabled={stale} title={stale ? STALE : "Start a timer on this task"} onClick={() => act(`Timer started on ${n.title}`, () => startTimer({ intentId: n.id }))}>
          <Icon name="play" /> Start a timer
        </Button>
      )}
    </>
  );
}

function TaskBody({ n, flag, stale }: { n: Node; flag?: Flag; stale?: boolean }) {
  const s = useStore();
  const now = useNow(60_000);
  const [title, setTitle] = useState(n.title);
  const [notes, setNotes] = useState(n.description ?? "");
  useEffect(() => setTitle(n.title), [n.title]);
  useEffect(() => setNotes(n.description ?? ""), [n.description]);
  const day = s.day.date;
  const parents = s.nodes.filter((x) => (x.type === "goal" || x.type === "project") && x.status === "active").sort((a, b) => a.title.localeCompare(b.title));
  const setDue = (ymd: string | null) => act(ymd ? `Due ${ymd === day ? "today" : shortDay(ymd)}` : "No due date", () => updateIntent(n.id, { due: ymd }, "Due date"), { undoable: true });
  const quick: [string, string | null][] = [
    ["Today", day],
    ["Tomorrow", addDays(day, 1)],
    ["Next week", addDays(day, 7)],
    ["No date", null],
  ];
  const id = `f-${n.id.slice(0, 8)}`;
  const due = dueWords(n.due_local, day);
  const people = s.entities.filter((e) => s.people.some((p) => p.id === e.id && p.kind === "person")).sort((a, b) => a.name.localeCompare(b.name));
  return (
    <>
      {flag ? (
        <div className="p-note is-agent">
          <span className="p-note__mark" aria-hidden="true">
            <Icon name="flag" />
          </span>
          <div>
            <p className="p-note__text">{flag.note}</p>
            <p className="p-note__meta">{flag.made ? "From the agent" : `From the agent, ${ago(flag.node.flagged_at, now)}`}</p>
          </div>
        </div>
      ) : null}
      <fieldset className="p-fields" disabled={stale}>
        <div className="p-field">
          <label htmlFor={`${id}-d`}>Due {due ? <span className={cx("p-field__now", n.overdue && "is-late")}>{due}</span> : null}</label>
          <div className="p-due">
            <div className="p-seg" role="group" aria-label="Due">
              {quick.map(([label, ymd]) => (
                <button key={label} type="button" className={cx("p-seg__btn", n.due_local === ymd && "is-on")} aria-pressed={n.due_local === ymd} onClick={() => setDue(ymd)}>
                  {label}
                </button>
              ))}
            </div>
            <input id={`${id}-d`} className="p-input p-input--date" type="date" aria-label="Due on a date" value={n.due_local ?? ""} onChange={(e) => setDue(e.target.value || null)} />
          </div>
        </div>
        <div className="p-field">
          <label htmlFor={`${id}-p`}>Counts toward</label>
          <select
            id={`${id}-p`}
            className="p-input p-select"
            value={n.parents[0]?.id ?? ""}
            onChange={(e) => {
              const to = e.target.value || null;
              const name = parents.find((p) => p.id === to)?.title;
              act(name ? `Counts toward ${name}` : "Counts toward nothing", () => setParent([n.id], to), { undoable: true });
            }}
          >
            <option value="">Nothing</option>
            {parents.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title}
              </option>
            ))}
          </select>
        </div>
        <div className="p-field">
          <label htmlFor={`${id}-n`}>Notes</label>
          <textarea
            id={`${id}-n`}
            className="p-input p-textarea"
            rows={3}
            placeholder="Anything to remember about it"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            onBlur={() => notes !== (n.description ?? "") && act("Notes saved", () => updateIntent(n.id, { description: notes || null }, "Notes"), { undoable: true })}
          />
        </div>
        {n.status === "waiting" ? <p className="p-aside">Waiting on {n.waiting_on ?? "someone"} since {n.waiting_since ? shortDay(n.waiting_since.slice(0, 10)) : "a while"}.</p> : null}
        {n.google_task_id ? <p className="p-aside">{GOOGLE}.</p> : null}
      </fieldset>
      <More>
        <fieldset className="p-fields" disabled={stale}>
          <div className="p-field">
            <label htmlFor={`${id}-t`}>Title</label>
            <input
              id={`${id}-t`}
              className="p-input"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onBlur={() => title.trim() && title !== n.title && act("Renamed", () => updateIntent(n.id, { title }, "Renamed"), { undoable: true })}
              onKeyDown={(e) => e.key === "Enter" && (e.target as HTMLInputElement).blur()}
            />
          </div>
          <div className="p-field">
            <label htmlFor={`${id}-w`}>Waiting on</label>
            <select
              id={`${id}-w`}
              className="p-input p-select"
              value={n.waiting_on_entity_id ?? ""}
              onChange={(e) => {
                const to = e.target.value || null;
                const who = people.find((p) => p.id === to)?.name;
                act(who ? `Waiting on ${who}` : "Not waiting on anyone", () => updateIntent(n.id, { waitingOnEntityId: to, status: to ? "waiting" : "active" }, "Waiting"), { undoable: true });
              }}
            >
              <option value="">No one</option>
              {people.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
          <p className="p-aside">
            Added {shortDay(n.created_at.slice(0, 10))} {n.via === "agent" ? "by the agent" : ""}, last changed {ago(n.updated_at, now)}.
          </p>
          <div className="p-more__acts">
            {n.status !== "later" ? (
              <Button tone="ghost" onClick={() => act("Moved to Later", () => updateIntent(n.id, { status: "later" }, "Later"), { undoable: true })}>
                Later
              </Button>
            ) : null}
            <Button
              tone="ghost"
              className="p-btn--danger"
              onClick={() => {
                if (n.google_task_id) return say(GOOGLE, true);
                act(`Deleted “${n.title}”`, () => deleteIntent(n.id), { undoable: true });
              }}
            >
              Delete
            </Button>
          </div>
        </fieldset>
      </More>
    </>
  );
}

// ---- a stretch of time ----

export function blockDetail(b: BlockView, close: () => void, opts: { stale?: boolean; onStep?: (d: 1 | -1) => void; first?: boolean; last?: boolean }): Detail {
  return {
    key: `block-${b.id}`,
    kicker: b.committed ? "Time · labelled" : "Time · to label",
    title: (
      <span className="p-when">
        {hm(b.t_start)} – {b.t_end ? hm(b.t_end) : "now"}
      </span>
    ),
    close,
    actions: opts.onStep ? (
      <div className="p-stepper" role="group" aria-label="The day's other stretches">
        <Button tone="ghost" size="sm" disabled={opts.first} onClick={() => opts.onStep!(-1)}>
          <Icon name="left" /> Earlier
        </Button>
        <Button tone="ghost" size="sm" disabled={opts.last} onClick={() => opts.onStep!(1)}>
          Later <Icon name="right" />
        </Button>
      </div>
    ) : undefined,
    body: <BlockBody key={b.id} b={b} stale={opts.stale} />,
  };
}

function BlockBody({ b, stale }: { b: BlockView; stale?: boolean }) {
  const now = useNow(30_000);
  const a = Date.parse(b.t_start);
  const z = b.t_end ? Date.parse(b.t_end) : now;
  const [at, setAt] = useState(hm(a + (z - a) / 2));
  const status = !b.t_end ? "Running now" : b.committed ? `Counted as ${b.intent}` : b.proposal ? null : b.intent ? `Guessed: ${b.intent}, not counted yet` : "Not labelled, so it counts toward nothing";
  return (
    <>
      <dl className="p-facts">
        <dt>How long</dt>
        <dd>{minutes(z - a)}</dd>
        <dt>From</dt>
        <dd>{where(b)}</dd>
        {status ? (
          <>
            <dt>Now</dt>
            <dd>{status}</dd>
          </>
        ) : null}
      </dl>
      {b.proposal && b.intent ? (
        <div className="p-note is-agent">
          <span className="p-note__mark" aria-hidden="true">
            <Icon name="spark" />
          </span>
          <div>
            <p className="p-note__text">The agent thinks this was {b.intent}.</p>
            {b.proposal.reason ? <p className="p-note__meta">{b.proposal.reason}</p> : null}
          </div>
          <Button tone="agent" size="sm" disabled={stale} title={stale ? STALE : undefined} onClick={() => act(`Counted as ${b.intent}`, () => answerProposal(b.id, true), { undoable: true })}>
            Yes
          </Button>
        </div>
      ) : null}
      <section className="p-sec">
        <h3 className="p-sec__head">{b.committed || !b.t_end ? "Relabel as" : "Label as"}</h3>
        <LabelPicker
          autoFocus={false}
          disabled={stale}
          current={b.intent_id}
          onPick={(id, title) => {
            if (!id) return;
            act(`Relabelled ${hm(a)}–${b.t_end ? hm(z) : "now"} as ${title}`, () => relabel(b.id, id), { undoable: true });
          }}
        />
        {stale ? <p className="p-aside">{STALE}.</p> : null}
      </section>
      {b.t_end ? (
        <More>
          <fieldset className="p-fields" disabled={stale}>
            <div className="p-field">
              <label htmlFor={`split-${b.id}`}>Split it at</label>
              <div className="p-inline">
                <input id={`split-${b.id}`} className="p-input p-input--time" type="time" value={at} onChange={(e) => setAt(e.target.value)} />
                <Button tone="quiet" size="sm" onClick={() => act(`Split at ${at}`, () => split(b.id, at), { undoable: true })}>
                  Split
                </Button>
              </div>
            </div>
            <div className="p-more__acts">
              <Button tone="ghost" className="p-btn--danger" onClick={() => act(`Deleted ${hm(a)}–${hm(z)}`, () => deleteBlock(b.id), { undoable: true })}>
                Delete this stretch
              </Button>
            </div>
          </fieldset>
        </More>
      ) : null}
    </>
  );
}

// ---- the running timer ----

export function timerDetail(close: () => void, stale?: boolean): Detail {
  return { key: "timer", kicker: "Timer · running", title: <TimerTitle />, close, actions: <TimerActs stale={stale} />, body: <TimerBody stale={stale} /> };
}

function TimerTitle() {
  const s = useStore();
  const r = running(s);
  return <>{r ? (r.intent ?? r.title ?? "No label yet") : "No timer running"}</>;
}

function TimerActs({ stale }: { stale?: boolean }) {
  const now = useNow(1000);
  const { r, stop } = useTimer(stale);
  if (!r) return null;
  return (
    <>
      <span className="p-readout p-readout--lg">{readout(now - Date.parse(r.t_start))}</span>
      <Button tone="quiet" disabled={stale} title={stale ? STALE : undefined} onClick={() => stop(now)}>
        <Icon name="stop" /> Stop
      </Button>
    </>
  );
}

function TimerBody({ stale }: { stale?: boolean }) {
  const { r, parent, switchTo } = useTimer(stale);
  const s = useStore();
  if (!r) return <p className="p-aside">It stopped. Start another from Today's first row.</p>;
  return (
    <>
      <dl className="p-facts">
        <dt>Started</dt>
        <dd>{hm(r.t_start)}</dd>
        {parent ? (
          <>
            <dt>Counts toward</dt>
            <dd>{parent}</dd>
          </>
        ) : null}
        <dt>Today so far</dt>
        <dd>{minutes(s.blocks.filter((b) => b.intent_id && b.intent_id === r.intent_id && b.t_end).reduce((m, b) => m + Date.parse(b.t_end!) - Date.parse(b.t_start), 0)) } earlier on it</dd>
      </dl>
      <section className="p-sec">
        <h3 className="p-sec__head">Switch to</h3>
        <p className="p-aside">Ends this one now and starts the new one from this moment.</p>
        <LabelPicker autoFocus={false} disabled={stale} current={r.intent_id} onPick={(id, title) => switchTo(id, title)} />
      </section>
    </>
  );
}

// ---- a calendar event ----

export function eventDetail(p: PlannedEvent, close: () => void, stale?: boolean): Detail {
  return {
    key: `event-${p.event_id}`,
    kicker: "Calendar",
    title: p.title ?? "An event",
    close,
    actions: (
      <Button tone="quiet" disabled={stale} title={stale ? STALE : "A timer named for this event, from now"} onClick={() => act(`Timer started: ${p.title ?? "event"}`, () => startTimer({ title: p.title }))}>
        <Icon name="play" /> Start a timer for it
      </Button>
    ),
    body: <EventBody p={p} />,
  };
}

function EventBody({ p }: { p: PlannedEvent }) {
  const now = useNow(60_000);
  const inMs = Date.parse(p.t_start) - now;
  return (
    <>
      <dl className="p-facts">
        <dt>When</dt>
        <dd>
          {hm(p.t_start)} – {hm(p.t_end)}, {minutes(Date.parse(p.t_end) - Date.parse(p.t_start))}
        </dd>
        <dt>Starts</dt>
        <dd>{inMs > 0 ? `in ${minutes(inMs)}` : "now"}</dd>
      </dl>
      <p className="p-aside">Calendar events fill the day on their own once they happen; nothing to do here unless the plan changes.</p>
    </>
  );
}
