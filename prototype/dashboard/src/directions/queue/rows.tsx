// Queue's rows. Every row is one thing with its own action on it (tick, Done, Seen) and a title
// that opens it in place: the row grows to show its fields, and nothing navigates away.
import { useEffect, useRef, useState } from "react";
import { deleteIntent, setDone, setParent, startTimer, unflag, updateIntent, useStore } from "../../data/store.js";
import type { Node } from "../../data/types.js";
import { act, dueWords, parentTitle, say, setLocal, shortDay } from "./lib.js";
import { revealOnOpen } from "./lib.js";
import { Button, Check, cx, Icon } from "./ui.js";
import { addDays } from "../../../../../src/shared/localTime.js";

export const googleNote = "Lives in Google Tasks: tick it off there";

export function tick(n: Node, done: boolean) {
  act(done ? `Done: ${n.title}` : `Back on the list: ${n.title}`, () => setDone(n.id, done), { undoable: true });
}

/** A task as a row: tick, title (opens it here), when it is due and what it counts toward. */
export function TaskRow({
  node,
  open,
  onOpen,
  stale,
  agentRow,
  showDone,
}: {
  node: Node;
  open: boolean;
  onOpen: (open: boolean) => void;
  stale?: boolean;
  agentRow?: boolean;
  showDone?: boolean;
}) {
  const s = useStore();
  const ref = useRef<HTMLLIElement>(null);
  useScrollWhenOpened(ref, open);
  const due = dueWords(node.due_local, s.day.date);
  const parent = parentTitle(node);
  const done = node.status === "done";
  const meta = [
    node.status === "waiting" ? `waiting on ${node.waiting_on ?? "someone"}` : null,
    done && showDone ? "done" : due,
    parent,
  ].filter(Boolean);
  return (
    <li ref={ref} className={cx("q-row", open && "is-open", done && "is-done", agentRow && "is-agent")}>
      <div className="q-row__head">
        {agentRow ? (
          <span className="q-row__agent" title="The agent is doing this" aria-hidden="true" />
        ) : (
          <Check
            checked={done}
            label={done ? `Mark “${node.title}” not done` : `Tick “${node.title}”`}
            disabled={stale || node.google_task_id !== null}
            title={stale ? "LoR is not answering; try again when it is back" : node.google_task_id ? googleNote : undefined}
            onChange={(v) => tick(node, v)}
          />
        )}
        <button type="button" className="q-row__title" aria-expanded={open} onClick={() => onOpen(!open)}>
          <span className="q-row__name">{node.title}</span>
          {meta.length ? <span className={cx("q-row__meta", node.overdue && "is-late")}>{meta.join(" · ")}</span> : null}
        </button>
        <button type="button" className="q-row__open" aria-label={open ? "Close" : "Open"} aria-expanded={open} onClick={() => onOpen(!open)}>
          <Icon name="chev" />
        </button>
      </div>
      {open ? <TaskFields node={node} stale={stale} /> : null}
    </li>
  );
}

/** One of the agent's flags as a row: why it flagged it, and Done and Seen on the row. */
export function FlagRow({
  node,
  note,
  made,
  open,
  onOpen,
  stale,
}: {
  node: Node;
  note: string;
  made?: boolean;
  open: boolean;
  onOpen: (open: boolean) => void;
  stale?: boolean;
}) {
  const ref = useRef<HTMLLIElement>(null);
  useScrollWhenOpened(ref, open);
  return (
    <li ref={ref} className={cx("q-row", "q-row--flag", open && "is-open")}>
      <div className="q-row__head">
        <span className="q-row__flagdot" aria-label="Flagged by the agent" />
        <button type="button" className="q-row__title" aria-expanded={open} onClick={() => onOpen(!open)}>
          <span className="q-row__name">{node.title}</span>
          <span className="q-row__note">{note}</span>
        </button>
        <div className="q-row__acts">
          <FlagActions node={node} made={made} stale={stale} />
        </div>
      </div>
      {open ? <TaskFields node={node} stale={stale} /> : null}
    </li>
  );
}

export function FlagActions({ node, made, stale, big }: { node: Node; made?: boolean; stale?: boolean; big?: boolean }) {
  const off = stale ? "LoR is not answering; try again when it is back" : undefined;
  const g = node.google_task_id !== null;
  return (
    <>
      <Button
        tone={big ? "primary" : "quiet"}
        disabled={stale || g}
        title={off ?? (g ? googleNote : undefined)}
        onClick={() => {
          if (made) setLocal((l) => ({ ...l, seen: new Set(l.seen).add(node.id) }));
          act(`Done: ${node.title}`, () => setDone(node.id, true), { undoable: true });
        }}
      >
        Done
      </Button>
      <Button
        tone={big ? "quiet" : "ghost"}
        disabled={stale}
        title={off ?? "Take the flag down without doing it"}
        onClick={() => {
          if (made) {
            setLocal((l) => ({ ...l, seen: new Set(l.seen).add(node.id) }));
            act("Seen", () => {}, { localUndo: () => setLocal((l) => { const seen = new Set(l.seen); seen.delete(node.id); return { ...l, seen }; }) });
          } else act("Seen", () => unflag(node.id), { undoable: true });
        }}
      >
        Seen
      </Button>
    </>
  );
}

function useScrollWhenOpened(ref: React.RefObject<HTMLElement | null>, open: boolean) {
  const first = useRef(true);
  useEffect(() => {
    if (open && first.current) revealOnOpen(ref.current);
    first.current = false;
  }, [open, ref]);
}

/** A task's fields, opened in place under its row. Each change saves at once and offers Undo. */
export function TaskFields({ node, stale }: { node: Node; stale?: boolean }) {
  const s = useStore();
  const [title, setTitle] = useState(node.title);
  const [notes, setNotes] = useState(node.description ?? "");
  useEffect(() => setTitle(node.title), [node.title]);
  useEffect(() => setNotes(node.description ?? ""), [node.description]);
  const day = s.day.date;
  const parents = s.nodes.filter((n) => (n.type === "goal" || n.type === "project") && n.status === "active").sort((a, b) => a.title.localeCompare(b.title));
  const setDue = (ymd: string | null) =>
    act(ymd ? `Due ${ymd === day ? "today" : shortDay(ymd)}` : "No due date", () => updateIntent(node.id, { due: ymd }, "Due date"), { undoable: true });
  const quick: [string, string | null][] = [
    ["Today", day],
    ["Tomorrow", addDays(day, 1)],
    ["Next week", addDays(day, 7)],
    ["No date", null],
  ];
  const id = `f-${node.id.slice(0, 8)}`;
  return (
    <fieldset className="q-fields" disabled={stale}>
      <div className="q-field">
        <label htmlFor={`${id}-t`}>Title</label>
        <input
          id={`${id}-t`}
          className="q-input"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          onBlur={() => title.trim() && title !== node.title && act("Renamed", () => updateIntent(node.id, { title }, "Renamed"), { undoable: true })}
          onKeyDown={(e) => e.key === "Enter" && (e.target as HTMLInputElement).blur()}
        />
      </div>
      <div className="q-field">
        <label htmlFor={`${id}-d`}>Due</label>
        <div className="q-due">
          <div className="q-seg" role="group" aria-label="Due">
            {quick.map(([label, ymd]) => (
              <button key={label} type="button" className={cx("q-seg__btn", node.due_local === ymd && "is-on")} aria-pressed={node.due_local === ymd} onClick={() => setDue(ymd)}>
                {label}
              </button>
            ))}
          </div>
          <input id={`${id}-d`} className="q-input q-input--date" type="date" value={node.due_local ?? ""} onChange={(e) => setDue(e.target.value || null)} />
        </div>
      </div>
      <div className="q-field">
        <label htmlFor={`${id}-p`}>Counts toward</label>
        <select
          id={`${id}-p`}
          className="q-input q-select"
          value={node.parents[0]?.id ?? ""}
          onChange={(e) => {
            const to = e.target.value || null;
            const name = parents.find((p) => p.id === to)?.title;
            act(name ? `Counts toward ${name}` : "Counts toward nothing", () => setParent([node.id], to), { undoable: true });
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
      <div className="q-field">
        <label htmlFor={`${id}-n`}>Notes</label>
        <textarea
          id={`${id}-n`}
          className="q-input q-textarea"
          rows={2}
          placeholder="Anything to remember"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          onBlur={() => notes !== (node.description ?? "") && act("Notes saved", () => updateIntent(node.id, { description: notes || null }, "Notes"), { undoable: true })}
        />
      </div>
      {node.status === "waiting" ? <p className="q-fields__aside">Waiting on {node.waiting_on ?? "someone"}.</p> : null}
      {node.google_task_id ? <p className="q-fields__aside">{googleNote}.</p> : null}
      <div className="q-fields__foot">
        <Button
          tone="quiet"
          onClick={() => act(`Timer started on ${node.title}`, () => startTimer({ intentId: node.id }))}
        >
          <Icon name="play" /> Start a timer on it
        </Button>
        <Button
          tone="ghost"
          className="q-btn--del"
          onClick={() => {
            if (node.google_task_id) return say(googleNote, true);
            act(`Deleted “${node.title}”`, () => deleteIntent(node.id), { undoable: true });
          }}
        >
          Delete
        </Button>
      </div>
    </fieldset>
  );
}
