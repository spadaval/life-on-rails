// Goals: what the week's time went toward. A goal with nothing committed this week reads
// "No data yet" (lor-thnc), never as a failure. One goal opens as its own page.
import { habitDoneOn, isOpen, taskRow } from "../../../data/derive.js";
import { running, setDone, startTimer, today as todayOf, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import { href, type Route } from "../../../shell/nav.js";
import { Check, Icon, Prose } from "../components/bits.js";
import { useMode } from "../lib/ctx.js";
import { dueWords } from "../lib/time.js";
import { oneWay, reversible } from "../lib/ui.js";

const fmtH = (h: number) => (h < 1 ? `${Math.round(h * 60)}m` : `${Math.round(h * 10) / 10}h`);

function Week({ n }: { n: Node }) {
  const h = n.week_hours ?? 0;
  const t = n.weekly_hour_target;
  if (h <= 0) {
    return (
      <span className="tl-week" data-none>
        <span className="tl-nodata">No data yet</span>
        {t ? <span className="tl-muted">aim {fmtH(t)} a week</span> : null}
      </span>
    );
  }
  if (!t) {
    return (
      <span className="tl-week">
        <span className="tl-num tl-week-h">{fmtH(h)}</span> <span className="tl-muted">this week, no aim set</span>
      </span>
    );
  }
  const pct = Math.min(1, h / t);
  return (
    <span className="tl-week">
      <span className="tl-num tl-week-h">{fmtH(h)}</span>
      <span className="tl-muted">of {fmtH(t)}</span>
      <span className="tl-bar" aria-hidden="true">
        <span style={{ width: `${pct * 100}%` }} data-over={h > t || undefined} />
      </span>
    </span>
  );
}

export function Goals({ route }: { route: Route }) {
  const s = useStore();
  const target = route.params.id ? s.nodes.find((n) => n.title === route.params.id && n.type !== "task" && n.type !== "metric") : undefined;
  if (target) return <GoalPage n={target} />;
  const goals = s.nodes.filter((n) => n.type === "goal" && n.status === "active").sort((a, b) => (b.week_hours ?? 0) - (a.week_hours ?? 0) || a.title.localeCompare(b.title));
  const under = (id: string) => s.nodes.filter((n) => n.parents[0]?.id === id && isOpen(n) && (n.type === "project" || n.type === "habit"));
  const total = goals.reduce((a, g) => a + (g.week_hours ?? 0), 0);
  const none = goals.filter((g) => !(g.week_hours ?? 0)).length;
  return (
    <div className="tl-page">
      <header className="tl-pagehead">
        <div>
          <h1>Goals</h1>
          <p className="tl-muted">
            This week: <span className="tl-num">{fmtH(total)}</span> committed across {goals.length - none} goals{none ? `; ${none} with no data yet` : ""}
          </p>
        </div>
      </header>
      <ul className="tl-goals">
        {goals.map((g) => (
          <li key={g.id} className="tl-card tl-goal">
            <a className="tl-goal-main" href={href("/goals", { id: g.title })}>
              <span className="tl-goal-title">{g.title}</span>
              <Week n={g} />
            </a>
            <span className="tl-goal-under">
              {under(g.id).map((c) => (
                <a key={c.id} className="tl-tag" href={href("/goals", { id: c.title })}>
                  {c.type === "habit" ? "Habit · " : ""}
                  {c.title}
                </a>
              ))}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

function GoalPage({ n }: { n: Node }) {
  const s = useStore();
  const { staleSince } = useMode();
  const off = staleSince !== null;
  const today = todayOf();
  const children = s.nodes.filter((c) => c.parents.some((p) => p.id === n.id) && c.status !== "archived");
  const projects = children.filter((c) => c.type === "project");
  const habits = children.filter((c) => c.type === "habit");
  const tasks = children.filter((c) => c.type === "task" && isOpen(c)).map(taskRow);
  const page = s.pages.find((p) => p.title === n.title || p.slug.endsWith(n.title.toLowerCase().replace(/\s+/g, "-")));
  const r = running(s);
  const parent = n.parents[0];
  return (
    <div className="tl-page">
      <a className="tl-back" href={parent ? href("/goals", { id: parent.title }) : href("/goals")}>
        <Icon name="back" size={16} /> {parent ? parent.title : "Goals"}
      </a>
      <header className="tl-pagehead">
        <div>
          <p className="tl-kicker">{n.type === "goal" ? "Goal" : n.type === "project" ? "Project" : "Habit"}</p>
          <h1>{n.title}</h1>
        </div>
        <div className="tl-pagehead-actions">
          {r?.intent_id === n.id ? (
            <span className="tl-tag tl-tag-live">
              <span className="tl-live-dot" /> Timer running on this
            </span>
          ) : (
            <button type="button" className="tl-btn" disabled={off} onClick={() => oneWay(`Started: ${n.title}`, () => startTimer({ intentId: n.id }))}>
              <Icon name="play" size={14} /> Start a timer on this
            </button>
          )}
        </div>
      </header>
      <div className="tl-goalpage">
        <section className="tl-card tl-goal-week">
          <h2 className="tl-section-title">This week</h2>
          <Week n={n} />
          {!(n.week_hours ?? 0) ? (
            <p className="tl-muted">
              Nothing has been committed toward this since Monday, so there is nothing to measure yet. Label a block on Today’s route or start a timer on it, and its time shows here.
            </p>
          ) : null}
          {n.type === "habit" && n.frequency ? (
            <p className="tl-muted">
              {habitDoneOn(s, n.id, today)} of {n.frequency} today
            </p>
          ) : null}
        </section>
        {projects.length || habits.length ? (
          <section className="tl-section">
            <h2 className="tl-section-title">Under it</h2>
            <ul className="tl-rows">
              {[...projects, ...habits].map((c) => (
                <li key={c.id} className="tl-row">
                  <a className="tl-row-main" href={href("/goals", { id: c.title })}>
                    <span className="tl-row-title">{c.title}</span>
                    <span className="tl-row-meta">{c.type === "project" ? `Project · ${c.tasks_done} of ${c.tasks_total} tasks done` : `Habit · ${c.frequency ?? 1} a ${c.frequency_horizon}`}</span>
                  </a>
                  <Icon name="chevron" size={16} />
                </li>
              ))}
            </ul>
          </section>
        ) : null}
        {tasks.length ? (
          <section className="tl-section">
            <h2 className="tl-section-title">Open tasks</h2>
            <ul className="tl-rows">
              {tasks.map((t) => (
                <li key={t.id} className="tl-row">
                  <Check checked={false} label={`Done: ${t.title}`} disabled={off || t.google} onChange={() => reversible(`Done: ${t.title}`, () => setDone(t.id, true))} />
                  <a className="tl-row-main" href={href("/tasks", { id: t.title })}>
                    <span className="tl-row-title">{t.title}</span>
                    <span className="tl-row-meta">{t.due ? dueWords(t.due, today) : "no date"}</span>
                  </a>
                </li>
              ))}
            </ul>
          </section>
        ) : null}
        {page ? (
          <section className="tl-section">
            <h2 className="tl-section-title">
              Its page <a className="tl-link" href={href("/notes", { slug: page.slug })}>open in Notes</a>
            </h2>
            <Prose text={page.body} className="tl-prose tl-card" />
          </section>
        ) : null}
      </div>
    </div>
  );
}
