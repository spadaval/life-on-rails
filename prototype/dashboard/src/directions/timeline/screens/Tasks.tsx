// Tasks: an ordinary list, ordered by when, as the rest of this direction is. A task opens in a
// popover beside its row (a sheet at phone width); there is no detail pane.
import { useState } from "react";
import { doneOn, isOpen, taskRow, type TaskRow } from "../../../data/derive.js";
import { setDone, today as todayOf, useStore } from "../../../data/store.js";
import { setParams, type Route } from "../../../shell/nav.js";
import { Check, Icon } from "../components/bits.js";
import { Popover } from "../components/Popover.js";
import { TaskEditor } from "../components/TaskEditor.js";
import { useMode } from "../lib/ctx.js";
import { addDays, dueWords } from "../lib/time.js";
import { reversible } from "../lib/ui.js";

export const askToAdd = () => window.dispatchEvent(new CustomEvent("tl:add"));

export function Tasks({ route }: { route: Route }) {
  const s = useStore();
  const { staleSince } = useMode();
  const off = staleSince !== null;
  const today = todayOf();
  const target = route.params.id ? s.nodes.find((n) => n.title === route.params.id && n.type === "task") : undefined;
  const who = route.params.who === "agent" || (!route.params.who && target?.assignee === "agent") ? "agent" : "owner";
  const [showLaterState, setShowLater] = useState(false);
  const showLater = showLaterState || target?.status === "later";
  const open = s.nodes.filter((n) => n.type === "task" && isOpen(n) && n.assignee === who).map(taskRow);
  const week = addDays(today, 7);
  const groups: { key: string; title: string; rows: TaskRow[] }[] = [
    { key: "late", title: "Late", rows: open.filter((t) => t.due && t.due < today && !t.waiting_on) },
    { key: "today", title: "Today", rows: open.filter((t) => t.due === today && !t.waiting_on) },
    { key: "tomorrow", title: "Tomorrow", rows: open.filter((t) => t.due === addDays(today, 1) && !t.waiting_on) },
    { key: "week", title: "This week", rows: open.filter((t) => t.due && t.due > addDays(today, 1) && t.due <= week && !t.waiting_on) },
    { key: "after", title: "After that", rows: open.filter((t) => t.due && t.due > week && !t.waiting_on) },
    { key: "none", title: "No date", rows: open.filter((t) => !t.due && !t.waiting_on) },
    { key: "waiting", title: "Waiting on someone", rows: open.filter((t) => t.waiting_on) },
  ].map((g) => ({ ...g, rows: g.rows.sort((a, b) => (a.due ?? "").localeCompare(b.due ?? "") || a.title.localeCompare(b.title)) }));
  const later = s.nodes.filter((n) => n.type === "task" && n.status === "later" && n.assignee === who).map(taskRow);
  const done = doneOn(s, today).filter((t) => s.nodes.find((n) => n.id === t.id)?.assignee === who);
  const openId = target?.id;
  // A task opened from search that no group lists (done on another day) shows on its own at the top.
  const listed = new Set([...groups.flatMap((g) => g.rows), ...later, ...done].map((t) => t.id));
  if (target && !listed.has(target.id)) groups.unshift({ key: "opened", title: "Opened", rows: [taskRow(target)] });
  const openRow = (title: string) => setParams({ id: route.params.id === title ? undefined : title });

  const row = (t: TaskRow) => (
    <li key={t.id} className="tl-row" data-done={t.done || undefined} data-selected={openId === t.id || undefined}>
      <Check
        checked={t.done}
        disabled={off || t.google}
        title={t.google ? "Lives in Google Tasks: tick it there" : undefined}
        label={t.done ? `Not done: ${t.title}` : `Done: ${t.title}`}
        onChange={(v) => reversible(v ? `Done: ${t.title}` : `Not done: ${t.title}`, () => setDone(t.id, v))}
      />
      <button type="button" id={`task-${t.id}`} className="tl-row-main" onClick={() => openRow(t.title)}>
        <span className="tl-row-title">
          {t.flagged ? <Icon name="flag" size={13} /> : null} {t.title}
        </span>
        <span className="tl-row-meta">{[t.parent, t.waiting_on ? `waiting on ${t.waiting_on}` : null, t.google ? "Google Tasks" : null].filter(Boolean).join(" · ") || "No goal or project"}</span>
      </button>
      <span className="tl-row-due" data-late={(t.due && t.due < today && !t.done) || undefined}>
        {t.due ? dueWords(t.due, today) : ""}
      </span>
    </li>
  );

  return (
    <div className="tl-page">
      <header className="tl-pagehead">
        <div>
          <h1>Tasks</h1>
          <p className="tl-muted">
            {open.length} open{who === "agent" ? ", the agent is doing these" : ""} · ordered by when they are due
          </p>
        </div>
        <div className="tl-pagehead-actions">
          <div className="tl-seg" role="group" aria-label="Whose">
            <button type="button" aria-pressed={who === "owner"} onClick={() => setParams({ who: undefined, id: undefined })}>
              Mine
            </button>
            <button type="button" aria-pressed={who === "agent"} onClick={() => setParams({ who: "agent", id: undefined })}>
              The agent’s
            </button>
          </div>
          <button type="button" className="tl-btn tl-btn-primary" disabled={off} onClick={askToAdd}>
            <Icon name="plus" size={16} /> Add a task <kbd>q</kbd>
          </button>
        </div>
      </header>
      <div className="tl-list" data-pop-root>
        {groups
          .filter((g) => g.rows.length)
          .map((g) => (
            <section key={g.key} className="tl-group" data-group={g.key}>
              <h2 className="tl-section-title">
                {g.title} <span className="tl-count">{g.rows.length}</span>
              </h2>
              <ul className="tl-rows">{g.rows.map(row)}</ul>
            </section>
          ))}
        {done.length ? (
          <section className="tl-group">
            <h2 className="tl-section-title">
              Done today <span className="tl-count">{done.length}</span>
            </h2>
            <ul className="tl-rows">{done.map(row)}</ul>
          </section>
        ) : null}
        {later.length ? (
          <section className="tl-group">
            <button type="button" className="tl-section-toggle" aria-expanded={showLater} onClick={() => setShowLater((v) => !v)}>
              <h2 className="tl-section-title">
                Put off for later <span className="tl-count">{later.length}</span>
              </h2>
              <Icon name={showLater ? "down" : "chevron"} size={16} />
            </button>
            {showLater ? <ul className="tl-rows">{later.map(row)}</ul> : null}
          </section>
        ) : null}
        {openId ? (
          <Popover anchorId={`task-${openId}`} label={route.params.id!} onClose={() => setParams({ id: undefined })} width={400}>
            <TaskEditor id={openId} gaps={undefined} />
          </Popover>
        ) : null}
      </div>
    </div>
  );
}
