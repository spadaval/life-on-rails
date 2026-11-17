// A task as a row: its box, its title (which opens it in place), and what it is waiting on. Opened,
// it grows in the list to show its due date, notes and a timer, and nothing leaves the screen.
import { addDays } from "../../../../../../src/shared/localTime.js";
import { setDone, startTimer, switchTimer, today as todayOf, updateIntent, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import { ctxPath, homeOf, runningBlock } from "../lib/model.js";
import { href, setParams } from "../lib/nav.js";
import { dueWords, shortDate, weekday } from "../lib/time.js";
import { oneWay, reversible, useOff } from "../lib/ui.js";
import { Check, Icon, kindIcon } from "./bits.js";

export function TaskRow({ node: n, open, param = "task", showHome, lead }: { node: Node; open: boolean; param?: string; showHome?: boolean; lead?: boolean }) {
  const s = useStore();
  const off = useOff();
  const today = todayOf();
  const done = n.status === "done";
  const r = runningBlock(s);
  const timing = r?.intent_id === n.id;
  const home = homeOf(s, n);
  const google = n.google_task_id !== null;
  const toggle = () => setParams({ [param]: open ? undefined : n.title, blk: undefined, block: undefined });

  const tick = (v: boolean) => reversible(v ? `Done: ${n.title}` : `Not done: ${n.title}`, () => setDone(n.id, v));
  const due = (ymd: string | null) =>
    reversible(ymd ? `Due ${dueWords(ymd, today)}: ${n.title}` : `No due date: ${n.title}`, () => updateIntent(n.id, { due: ymd }, ymd ? `Due ${ymd}` : "No due date"));

  const chips: { label: string; ymd: string | null }[] = [
    { label: "Today", ymd: today },
    { label: "Tomorrow", ymd: addDays(today, 1) },
    ...[2, 3, 4].map((d) => ({ label: weekday(addDays(today, d)), ymd: addDays(today, d) })),
    { label: "In a week", ymd: addDays(today, 7) },
    { label: "No date", ymd: null },
  ];

  return (
    <li className="cx-task" data-open={open || undefined} data-done={done || undefined} data-lead={lead || undefined} data-timing={timing || undefined}>
      <div className="cx-task-row">
        <Check checked={done} onChange={tick} label={done ? `Mark ${n.title} not done` : `Mark ${n.title} done`} disabled={off || google} title={google ? "Lives in Google Tasks: tick it there" : undefined} />
        <button type="button" className="cx-task-title" onClick={toggle} aria-expanded={open}>
          <span className="cx-task-name">{n.title}</span>
          <span className="cx-task-meta">
            {timing ? <span className="cx-chip cx-chip-amber"><span className="cx-live-dot" /> Timing</span> : null}
            {n.flagged_at ? <span className="cx-chip cx-chip-flag"><Icon name="flag" size={12} /> Flagged</span> : null}
            {n.assignee === "agent" ? <span className="cx-chip cx-chip-cyan"><Icon name="agent" size={12} /> Agent</span> : null}
            {n.status === "waiting" ? <span className="cx-chip">Waiting on {n.waiting_on ?? "someone"}</span> : null}
            {n.status === "later" ? <span className="cx-chip">Later</span> : null}
            {n.due_local && !done ? <span className="cx-chip" data-late={n.overdue || undefined}>{dueWords(n.due_local, today)}</span> : null}
            {showHome ? <span className="cx-task-home">{home.kind === "loose" ? "Loose ends" : home.title}</span> : null}
          </span>
        </button>
        <Icon name="down" size={16} />
      </div>
      {open ? (
        <div className="cx-task-edit">
          {n.flag_note ? (
            <p className="cx-task-flag">
              <Icon name="flag" size={14} /> {n.flag_note}
            </p>
          ) : null}
          {n.description ? <p className="cx-task-desc">{n.description}</p> : null}
          <div className="cx-field-row">
            <span className="cx-label">Due</span>
            <div className="cx-chips" role="group" aria-label="Due date">
              {chips.map((c) => (
                <button
                  key={c.label}
                  type="button"
                  className="cx-chipbtn"
                  aria-pressed={n.due_local === c.ymd}
                  disabled={off || done}
                  onClick={() => due(c.ymd)}
                  title={c.ymd ? shortDate(c.ymd) : "Take the date off"}
                >
                  {c.label}
                </button>
              ))}
              <input
                type="date"
                className="cx-date"
                aria-label="Pick a due date"
                value={n.due_local ?? ""}
                disabled={off || done}
                onChange={(e) => e.target.value && due(e.target.value)}
              />
            </div>
          </div>
          <div className="cx-task-actions">
            {!done && n.assignee !== "agent" ? (
              timing ? (
                <span className="cx-label cx-label-amber">The timer is on this</span>
              ) : (
                <button
                  type="button"
                  className="cx-btn cx-btn-sm"
                  disabled={off}
                  onClick={() =>
                    r
                      ? oneWay(`Switched to ${n.title}`, () => switchTimer({ intentId: n.id }))
                      : oneWay(`Timer started on ${n.title}`, () => startTimer({ intentId: n.id }))
                  }
                >
                  <Icon name={r ? "switch" : "play"} size={15} /> {r ? "Switch the timer to this" : "Start a timer"}
                </button>
              )
            ) : null}
            {!done && n.status !== "later" ? (
              <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" disabled={off} onClick={() => reversible(`Later: ${n.title}`, () => updateIntent(n.id, { status: "later" }, "Later"))}>
                Later
              </button>
            ) : null}
            {n.status === "later" ? (
              <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" disabled={off} onClick={() => reversible(`Back: ${n.title}`, () => updateIntent(n.id, { status: "active" }, "Back"))}>
                Bring back
              </button>
            ) : null}
            {showHome ? (
              <a className="cx-btn cx-btn-sm cx-btn-ghost" href={href(ctxPath(home).path, { ...ctxPath(home).params, task: n.title })}>
                <Icon name={kindIcon(home.kind)} size={15} /> Open {home.kind === "loose" ? "Loose ends" : home.title}
              </a>
            ) : null}
            <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost cx-push" onClick={toggle}>
              Close
            </button>
          </div>
        </div>
      ) : null}
    </li>
  );
}
