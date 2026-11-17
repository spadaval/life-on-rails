// Panes' rows. A row is one thing with its common action on it (tick, Done, Seen, Yes), which
// opens nothing; the rest of the row opens it in the detail pane. The open row wears amber.
import { blockName } from "../../data/derive.js";
import { answerProposal, setDone, unflag, useNow, useStore } from "../../data/store.js";
import type { BlockView, Node, PlannedEvent } from "../../data/types.js";
import { act, dueWords, type Flag, hm, minutes, parentTitle, setLocal, STALE } from "./lib.js";
import { Button, Check, cx, Icon } from "./ui.js";

export const GOOGLE = "Lives in Google Tasks: tick it off there";

export function tick(n: Node, done: boolean) {
  act(done ? `Done: ${n.title}` : `Back on the list: ${n.title}`, () => setDone(n.id, done), { undoable: true });
}

/** A task: tick, title, when it is due and what it counts toward. */
export function TaskRow({ node, sel, onOpen, stale, showDone }: { node: Node; sel: boolean; onOpen: () => void; stale?: boolean; showDone?: boolean }) {
  const s = useStore();
  const due = dueWords(node.due_local, s.day.date);
  const done = node.status === "done";
  const late = node.overdue && !done;
  const meta = [node.status === "waiting" ? `waiting on ${node.waiting_on ?? "someone"}` : null, done && showDone ? "done" : late ? null : due, parentTitle(node)].filter(Boolean);
  const agent = node.assignee === "agent";
  return (
    <li className={cx("p-row", sel && "is-sel", done && "is-done")} data-sel={sel}>
      {agent ? (
        <span className="p-row__mark is-agent" title="The agent is doing this" aria-label="The agent is doing this">
          <Icon name="spark" />
        </span>
      ) : (
        <Check
          checked={done}
          label={done ? `Mark “${node.title}” not done` : `Tick “${node.title}”`}
          disabled={stale || node.google_task_id !== null}
          title={stale ? STALE : node.google_task_id ? GOOGLE : undefined}
          onChange={(v) => tick(node, v)}
        />
      )}
      <button type="button" className="p-row__open" aria-current={sel || undefined} onClick={onOpen}>
        <span className="p-row__title">{node.title}</span>
        {meta.length || late ? (
          <span className="p-row__meta">
            {late ? <span className="p-late">{due}</span> : null}
            {late && meta.length ? " · " : null}
            {meta.join(" · ")}
          </span>
        ) : null}
      </button>
      <Icon name="right" />
    </li>
  );
}

/** One of the agent's flags: why it flagged it, and Done and Seen on the row. */
export function FlagRow({ flag, sel, onOpen, stale }: { flag: Flag; sel: boolean; onOpen: () => void; stale?: boolean }) {
  return (
    <li className={cx("p-row", "p-row--flag", sel && "is-sel")} data-sel={sel}>
      <span className="p-row__mark is-agent" aria-label="Flagged by the agent">
        <Icon name="flag" />
      </span>
      <button type="button" className="p-row__open" aria-current={sel || undefined} onClick={onOpen}>
        <span className="p-row__title">{flag.title}</span>
        <span className="p-row__note">{flag.note}</span>
      </button>
      <div className="p-row__acts">
        <FlagActions flag={flag} stale={stale} />
      </div>
    </li>
  );
}

export function FlagActions({ flag, stale, big }: { flag: Flag; stale?: boolean; big?: boolean }) {
  const g = flag.node.google_task_id !== null;
  return (
    <>
      <Button
        tone={big ? "primary" : "quiet"}
        size={big ? undefined : "sm"}
        disabled={stale || g}
        title={stale ? STALE : g ? GOOGLE : "Done: tick the task and take the flag down"}
        onClick={() => act(`Done: ${flag.title}`, () => setDone(flag.id, true), { undoable: true })}
      >
        Done
      </Button>
      <Button
        tone={big ? "quiet" : "ghost"}
        size={big ? undefined : "sm"}
        disabled={stale}
        title={stale ? STALE : "Take the flag down without doing it"}
        onClick={() => {
          if (flag.made) {
            setLocal((l) => ({ ...l, seen: new Set(l.seen).add(flag.id) }));
            act("Seen", () => {}, {
              localUndo: () =>
                setLocal((l) => {
                  const seen = new Set(l.seen);
                  seen.delete(flag.id);
                  return { ...l, seen };
                }),
            });
          } else act("Seen", () => unflag(flag.id), { undoable: true });
        }}
      >
        Seen
      </Button>
    </>
  );
}

export const wantsLabel = (b: BlockView) => b.t_end !== null && !b.committed;

export function where(b: BlockView): string {
  if (b.origin === "manual") return b.t_end ? "Timer" : "Timer, running";
  if (b.origin === "auto_activity") return `Phone${b.title ? `: ${b.title}` : ""}`;
  if (b.origin === "place") return b.title ?? "A place";
  if (b.origin === "calendar") return `Calendar: ${b.title ?? "an event"}`;
  return b.origin;
}

/** A stretch of the day: when, how long, where it came from; the agent's guess with Yes on the row. */
export function BlockRow({ b, sel, onOpen, stale }: { b: BlockView; sel: boolean; onOpen: () => void; stale?: boolean }) {
  const now = useNow(60_000);
  const a = Date.parse(b.t_start);
  const z = b.t_end ? Date.parse(b.t_end) : now;
  const guess = !b.committed && b.intent ? b.intent : null;
  return (
    <li className={cx("p-row", "p-row--block", sel && "is-sel", b.committed && "is-labelled")} data-sel={sel}>
      <span className={cx("p-row__time")}>{hm(a)}</span>
      <button type="button" className="p-row__open" aria-current={sel || undefined} onClick={onOpen}>
        <span className="p-row__title">{b.committed ? blockName(b) : where(b)}</span>
        <span className={cx("p-row__meta", guess && "is-agent")}>
          {minutes(z - a)} · {b.committed ? where(b) : guess ? `the agent thinks ${guess}` : "not labelled"}
        </span>
      </button>
      {guess && b.proposal ? (
        <div className="p-row__acts">
          <Button tone="agent" size="sm" disabled={stale} title={stale ? STALE : `Yes, it was ${guess}`} onClick={() => act(`Counted as ${guess}`, () => answerProposal(b.id, true), { undoable: true })}>
            Yes
          </Button>
        </div>
      ) : (
        <Icon name="right" />
      )}
    </li>
  );
}

/** A calendar event later today. */
export function EventRow({ p, sel, onOpen }: { p: PlannedEvent; sel: boolean; onOpen: () => void }) {
  const now = useNow(60_000);
  const inMs = Date.parse(p.t_start) - now;
  return (
    <li className={cx("p-row", "p-row--event", sel && "is-sel")} data-sel={sel}>
      <span className="p-row__time is-cal">{hm(p.t_start)}</span>
      <button type="button" className="p-row__open" aria-current={sel || undefined} onClick={onOpen}>
        <span className="p-row__title">{p.title ?? "An event"}</span>
        <span className="p-row__meta">{inMs > 0 ? `in ${minutes(inMs)}` : "now"} · until {hm(p.t_end)}</span>
      </button>
      <Icon name="right" />
    </li>
  );
}
