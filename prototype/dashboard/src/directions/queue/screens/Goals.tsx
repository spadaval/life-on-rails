// Goals: each goal is a row with its week; it opens in place to its projects, habits, open tasks
// and weekly aim. A goal with no committed time this week says "No data yet", never "0 of 3 h".
import { habitDoneOn, isOpen } from "../../../data/derive.js";
import { habitDone, updateIntent, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import { setParams, type Route } from "../../../shell/nav.js";
import { act, modeOf, qhref } from "../lib.js";
import { revealOnOpen } from "../lib.js";
import { TaskRow } from "../rows.js";
import { Button, cx, Icon } from "../ui.js";
import { fmtH } from "./Today.js";
import { useEffect, useRef, useState } from "react";

export function Goals({ route }: { route: Route }) {
  const s = useStore();
  const stale = modeOf(route.params) === "stale";
  const goals = s.nodes.filter((n) => n.type === "goal" && n.status === "active").sort((a, b) => b.weight - a.weight || a.title.localeCompare(b.title));
  const asked = route.params.id ?? null;
  const askedNode = asked ? s.nodes.find((n) => n.title === asked) : undefined;
  const openGoal = askedNode ? (askedNode.type === "goal" ? askedNode : topGoal(s.nodes, askedNode)) : undefined;
  const nodata = goals.filter((g) => !(g.week_hours ?? 0)).length;
  return (
    <div className="q-col">
      <header className="q-page">
        <h1 className="q-page__title">Goals</h1>
        <p className="q-page__sub">
          This week, {goals.length} goals{nodata ? `, ${nodata} with no time recorded yet` : ""}.
        </p>
      </header>
      <ul className="q-list q-list--goals">
        {goals.map((g) => (
          <GoalRow
            key={g.id}
            g={g}
            open={openGoal?.id === g.id}
            focus={askedNode && askedNode.id !== g.id ? askedNode.id : null}
            onOpen={(v) => setParams({ id: v ? g.title : undefined })}
            stale={stale}
          />
        ))}
      </ul>
    </div>
  );
}

function topGoal(nodes: Node[], n: Node): Node | undefined {
  let cur: Node | undefined = n;
  for (let i = 0; cur && i < 6; i++) {
    if (cur.type === "goal") return cur;
    const pid: string | undefined = cur.parents[0]?.id;
    cur = pid ? nodes.find((x) => x.id === pid) : undefined;
  }
  return undefined;
}

function Week({ g }: { g: Node }) {
  const h = g.week_hours ?? 0;
  const t = g.weekly_hour_target;
  if (!h) return <span className="q-goal__nodata">{t ? `No data yet · aim ${fmtH(t)} h` : "No data yet"}</span>;
  return (
    <span className="q-goal__week">
      {t ? (
        <span className="q-meter" aria-hidden="true">
          <span className="q-meter__fill" style={{ width: `${Math.min(100, (h / t) * 100)}%` }} />
        </span>
      ) : null}
      <span className="q-goal__num">{t ? `${fmtH(h)} of ${fmtH(t)} h` : `${fmtH(h)} h`}</span>
    </span>
  );
}

function GoalRow({ g, open, focus, onOpen, stale }: { g: Node; open: boolean; focus: string | null; onOpen: (v: boolean) => void; stale?: boolean }) {
  const s = useStore();
  const ref = useRef<HTMLLIElement>(null);
  useEffect(() => {
    if (open) revealOnOpen(ref.current);
  }, [open]);
  const kids = s.nodes.filter((n) => n.parents.some((p) => p.id === g.id) && n.status !== "archived");
  const projects = kids.filter((n) => n.type === "project");
  const habits = kids.filter((n) => n.type === "habit");
  const under = new Set([g.id, ...projects.map((p) => p.id)]);
  const tasks = s.nodes.filter((n) => n.type === "task" && isOpen(n) && n.parents.some((p) => under.has(p.id)));
  const [openTask, setOpenTask] = useState<string | null>(null);
  const summary = [projects.length ? `${projects.length} project${projects.length > 1 ? "s" : ""}` : null, habits.length ? `${habits.length} habit${habits.length > 1 ? "s" : ""}` : null, tasks.length ? `${tasks.length} open task${tasks.length > 1 ? "s" : ""}` : null]
    .filter(Boolean)
    .join(" · ");
  return (
    <li ref={ref} className={cx("q-goal", open && "is-open")}>
      <button type="button" className="q-goal__head" aria-expanded={open} onClick={() => onOpen(!open)}>
        <span className="q-goal__main">
          <span className="q-goal__title">{g.title}</span>
          <span className="q-goal__sum">{summary || "Nothing under it yet"}</span>
        </span>
        <Week g={g} />
        <Icon name="chev" />
      </button>
      {open ? (
        <div className="q-goal__body">
          {g.description ? <p className="q-goal__desc">{g.description}</p> : null}
          {!(g.week_hours ?? 0) ? (
            <p className="q-note">
              No time has been counted toward this goal this week. Label a block on Today, or start a timer on one of its tasks, and it shows here.
            </p>
          ) : null}
          {projects.length ? (
            <div className="q-sub">
              <h3 className="q-label">Projects</h3>
              <ul className="q-mini">
                {projects.map((p) => {
                  const n = s.nodes.filter((t) => t.type === "task" && isOpen(t) && t.parents.some((x) => x.id === p.id)).length;
                  return (
                    <li key={p.id} className={cx(focus === p.id && "is-focus")}>
                      <span>{p.title}</span>
                      <span className="q-mini__meta">{p.week_hours ? `${fmtH(p.week_hours)} h this week · ` : ""}{n} open</span>
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : null}
          {habits.length ? (
            <div className="q-sub">
              <h3 className="q-label">Habits</h3>
              <ul className="q-mini">
                {habits.map((h) => {
                  const done = habitDoneOn(s, h.id, s.day.date);
                  return (
                    <li key={h.id} className={cx(focus === h.id && "is-focus")}>
                      <span>{h.title}</span>
                      {done ? (
                        <span className="q-mini__meta">Done today</span>
                      ) : (
                        <Button size="sm" tone="quiet" disabled={stale} onClick={() => act(`Logged ${h.title}`, () => habitDone(h.id), { undoable: true })}>
                          Done today
                        </Button>
                      )}
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : null}
          {tasks.length ? (
            <div className="q-sub">
              <h3 className="q-label">Open tasks</h3>
              <ul className="q-list q-list--inner">
                {tasks.slice(0, 6).map((t) => (
                  <TaskRow key={t.id} node={t} open={openTask === t.id} onOpen={(v) => setOpenTask(v ? t.id : null)} stale={stale} agentRow={t.assignee === "agent"} />
                ))}
              </ul>
              {tasks.length > 6 ? (
                <a className="q-link" href={qhref("/tasks")}>
                  All {tasks.length} on Tasks
                </a>
              ) : null}
            </div>
          ) : null}
          <Aim g={g} stale={stale} />
        </div>
      ) : null}
    </li>
  );
}

function Aim({ g, stale }: { g: Node; stale?: boolean }) {
  const [v, setV] = useState(g.weekly_hour_target?.toString() ?? "");
  useEffect(() => setV(g.weekly_hour_target?.toString() ?? ""), [g.weekly_hour_target]);
  const save = () => {
    const n = v.trim() === "" ? null : Number(v);
    if (n !== null && !(n >= 0)) return;
    if (n === g.weekly_hour_target) return;
    act(n ? `Aim: ${n} h a week` : "No weekly aim", () => updateIntent(g.id, { weeklyHourTarget: n }, "Weekly aim"), { undoable: true });
  };
  return (
    <div className="q-field q-field--inline">
      <label htmlFor={`aim-${g.id}`}>Weekly aim</label>
      <span className="q-aim">
        <input id={`aim-${g.id}`} className="q-input q-input--num" inputMode="decimal" disabled={stale} value={v} placeholder="none" onChange={(e) => setV(e.target.value)} onBlur={save} onKeyDown={(e) => e.key === "Enter" && save()} />
        <span className="q-aim__unit">hours</span>
      </span>
    </div>
  );
}
