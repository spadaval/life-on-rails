// Goals: each goal is a row with its week; it opens in the detail pane to its week, projects,
// habits and open tasks. A task opened from there stacks a second pane over the goal. A goal with
// no committed time this week says "No data yet", never "0 of 3 h".
import { useState } from "react";
import { habitDoneOn, isOpen } from "../../../data/derive.js";
import { habitDone, updateIntent, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import type { Route } from "../../../shell/nav.js";
import { taskDetail } from "../details.js";
import { act, closeHere, fmtH, modeOf, openHere, STALE } from "../lib.js";
import { type Detail, ListHead, More, Panes } from "../panes.js";
import { TaskRow } from "../rows.js";
import { Button, cx, Group, Icon } from "../ui.js";

const OPEN = ["id", "task"];

function topGoal(nodes: Node[], n: Node): Node | undefined {
  let cur: Node | undefined = n;
  for (let i = 0; cur && i < 6; i++) {
    if (cur.type === "goal") return cur;
    const pid: string | undefined = cur.parents[0]?.id;
    cur = pid ? nodes.find((x) => x.id === pid) : undefined;
  }
  return undefined;
}

export function Goals({ route }: { route: Route }) {
  const s = useStore();
  const p = route.params;
  const stale = modeOf(p) === "stale";
  const goals = s.nodes.filter((n) => n.type === "goal" && n.status === "active").sort((a, b) => b.weight - a.weight || a.title.localeCompare(b.title));
  const asked = p.id ? s.nodes.find((n) => n.title === p.id) : undefined;
  const goal = asked ? (asked.type === "goal" ? asked : topGoal(s.nodes, asked)) : undefined;
  const task = p.task ? s.nodes.find((n) => n.title === p.task && n.type === "task") : undefined;
  const nodata = goals.filter((g) => !(g.week_hours ?? 0));

  const detail: Detail | null = goal
    ? {
        key: `goal-${goal.id}`,
        kicker: "Goal · this week",
        title: goal.title,
        close: () => closeHere(OPEN),
        body: <GoalBody g={goal} stale={stale} openTask={(t) => openHere({ id: goal.title, task: t.title }, ["task"])} selTask={task?.id ?? null} />,
      }
    : null;
  const sub = goal && task ? taskDetail(task, () => closeHere(["task"]), { stale, backLabel: goal.title }) : null;

  const head = <ListHead title="Goals" sub={`This week: ${goals.length} goals, ${nodata.length} with no time recorded yet`} />;
  const list = (
    <ul className="p-rows p-rows--goals">
      {goals.map((g) => (
        <GoalRow key={g.id} g={g} sel={goal?.id === g.id} onOpen={() => openHere({ id: g.title }, OPEN)} />
      ))}
    </ul>
  );
  const rest = (
    <div className="p-overview">
      <p className="p-kicker">The week</p>
      <p className="p-overview__lead">
        {fmtH(goals.reduce((h, g) => h + (g.week_hours ?? 0), 0))} hours have counted toward goals since Monday.
      </p>
      <p className="p-overview__hint">“No data yet” means nothing was recorded against a goal this week. It is not a score of zero.</p>
    </div>
  );
  return <Panes label="Goals" head={head} list={list} detail={detail} sub={sub} rest={rest} sheet={p.sheet} />;
}

function Week({ g }: { g: Node }) {
  const h = g.week_hours ?? 0;
  const t = g.weekly_hour_target;
  if (!h) return <span className="p-goal__nodata">{t ? `No data yet · aim ${fmtH(t)} h` : "No data yet"}</span>;
  return (
    <span className="p-goal__week">
      {t ? (
        <span className="p-meter" aria-hidden="true">
          <span className={cx("p-meter__fill", h >= t && "is-full")} style={{ width: `${Math.min(100, (h / t) * 100)}%` }} />
        </span>
      ) : null}
      <span className="p-goal__num">{t ? `${fmtH(h)} of ${fmtH(t)} h` : `${fmtH(h)} h`}</span>
    </span>
  );
}

function GoalRow({ g, sel, onOpen }: { g: Node; sel: boolean; onOpen: () => void }) {
  const s = useStore();
  const kids = s.nodes.filter((n) => n.parents.some((p) => p.id === g.id) && n.status !== "archived");
  const projects = kids.filter((n) => n.type === "project").length;
  const habits = kids.filter((n) => n.type === "habit").length;
  const summary = [projects ? `${projects} project${projects > 1 ? "s" : ""}` : null, habits ? `${habits} habit${habits > 1 ? "s" : ""}` : null].filter(Boolean).join(" · ");
  return (
    <li className={cx("p-row", "p-row--goal", sel && "is-sel")} data-sel={sel}>
      <button type="button" className="p-row__open" aria-current={sel || undefined} onClick={onOpen}>
        <span className="p-row__title">{g.title}</span>
        <span className="p-row__meta">{summary || "Nothing under it yet"}</span>
      </button>
      <Week g={g} />
      <Icon name="right" />
    </li>
  );
}

function GoalBody({ g, stale, openTask, selTask }: { g: Node; stale?: boolean; openTask: (t: Node) => void; selTask: string | null }) {
  const s = useStore();
  const kids = s.nodes.filter((n) => n.parents.some((p) => p.id === g.id) && n.status !== "archived");
  const projects = kids.filter((n) => n.type === "project");
  const habits = kids.filter((n) => n.type === "habit");
  const under = new Set([g.id, ...projects.map((p) => p.id)]);
  const tasks = s.nodes.filter((n) => n.type === "task" && isOpen(n) && n.parents.some((p) => under.has(p.id)));
  const h = g.week_hours ?? 0;
  const t = g.weekly_hour_target;
  const [aim, setAim] = useState(t ? String(t) : "");
  return (
    <>
      <div className={cx("p-weekcard", !h && "is-nodata")}>
        {h ? (
          <>
            <span className="p-weekcard__num">{fmtH(h)} h</span>
            <span className="p-weekcard__label">{t ? `of an aim of ${fmtH(t)} h this week` : "this week, with no aim set"}</span>
            {t ? (
              <span className="p-meter p-meter--lg" aria-hidden="true">
                <span className={cx("p-meter__fill", h >= t && "is-full")} style={{ width: `${Math.min(100, (h / t) * 100)}%` }} />
              </span>
            ) : null}
          </>
        ) : (
          <>
            <span className="p-weekcard__num">No data yet</span>
            <span className="p-weekcard__label">Nothing recorded has counted toward it since Monday. Label time or start a timer on one of its tasks and it shows here.</span>
          </>
        )}
      </div>
      {projects.length ? (
        <Group label="Projects" count={projects.length}>
          <ul className="p-mini">
            {projects.map((pr) => {
              const open = s.nodes.filter((n) => n.type === "task" && isOpen(n) && n.parents.some((x) => x.id === pr.id)).length;
              return (
                <li key={pr.id}>
                  <span>{pr.title}</span>
                  <span className="p-mini__meta">{open ? `${open} open` : "nothing open"}</span>
                </li>
              );
            })}
          </ul>
        </Group>
      ) : null}
      {habits.length ? (
        <Group label="Habits">
          <ul className="p-mini">
            {habits.map((hb) => {
              const today = habitDoneOn(s, hb.id, s.day.date);
              return (
                <li key={hb.id}>
                  <span>{hb.title}</span>
                  {today ? (
                    <span className="p-mini__meta is-done">done today</span>
                  ) : (
                    <Button tone="quiet" size="sm" disabled={stale} title={stale ? STALE : undefined} onClick={() => act(`Logged ${hb.title}`, () => habitDone(hb.id), { undoable: true })}>
                      Done today
                    </Button>
                  )}
                </li>
              );
            })}
          </ul>
        </Group>
      ) : null}
      <Group label="Open tasks" count={tasks.length}>
        {tasks.length ? (
          <ul className="p-rows">
            {tasks.map((n) => (
              <TaskRow key={n.id} node={n} sel={selTask === n.id} onOpen={() => openTask(n)} stale={stale} />
            ))}
          </ul>
        ) : (
          <p className="p-empty">Nothing open under it.</p>
        )}
      </Group>
      <More label="Change the weekly aim">
        <fieldset className="p-fields" disabled={stale}>
          <div className="p-field">
            <label htmlFor={`aim-${g.id}`}>Hours a week</label>
            <div className="p-inline">
              <input id={`aim-${g.id}`} className="p-input p-input--num" type="number" min={0} step={0.5} value={aim} onChange={(e) => setAim(e.target.value)} />
              <Button
                tone="quiet"
                size="sm"
                onClick={() => {
                  const v = aim.trim() ? Number(aim) : null;
                  act(v ? `Aim: ${fmtH(v)} h a week` : "No weekly aim", () => updateIntent(g.id, { weeklyHourTarget: v }, "Aim"), { undoable: true });
                }}
              >
                Save
              </Button>
            </div>
          </div>
        </fieldset>
      </More>
    </>
  );
}
