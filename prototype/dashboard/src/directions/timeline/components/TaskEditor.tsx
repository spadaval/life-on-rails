// A task, opened: tick it, its flag, its due date, and planning it onto today's route.
// The same body in a popover on the desktop and a sheet at phone width.
import { setDone, today as todayOf, unflag, updateIntent, useNow, useStore } from "../../../data/store.js";
import { href } from "../lib/nav.js";
import { useMode } from "../lib/ctx.js";
import { addDays, dueWords, dur, hm, shortDate, weekday } from "../lib/time.js";
import { plan, reversible, unplan, usePlans } from "../lib/ui.js";
import { Check, Icon } from "./bits.js";

export function TaskEditor({ id, gaps, showOpen }: { id: string; gaps?: { start: number; end: number }[]; showOpen?: boolean }) {
  const s = useStore();
  const now = useNow(30_000);
  const { staleSince } = useMode();
  const plans = usePlans();
  const n = s.nodes.find((x) => x.id === id);
  if (!n) return <p className="tl-muted">This task is gone.</p>;
  const today = todayOf();
  const done = n.status === "done";
  const google = n.google_task_id !== null;
  const off = staleSince !== null;
  const planned = plans.find((p) => p.taskId === id);
  const nextMon = addDays(today, ((8 - weekday(today)) % 7) || 7);
  const setDue = (d: string | null) =>
    reversible(d ? `Due ${dueWords(d, today)}: ${n.title}` : `No due date: ${n.title}`, () => updateIntent(id, { due: d }, d ? `Due ${d}` : "No due date"));
  const minutes = Math.round((n.estimate_hours ?? 0.5) * 60);
  return (
    <div className="tl-editor">
      <div className="tl-editor-head">
        <Check
          checked={done}
          disabled={off || google}
          title={google ? "Lives in Google Tasks: tick it there" : undefined}
          label={done ? "Mark not done" : "Mark done"}
          onChange={(v) => reversible(v ? `Done: ${n.title}` : `Not done: ${n.title}`, () => setDone(id, v))}
        />
        <div>
          <p className="tl-kicker">{n.parents[0]?.title ?? "No goal or project"}</p>
          <h2 className="tl-editor-title" data-done={done || undefined}>
            {n.title}
          </h2>
        </div>
      </div>
      {google ? <p className="tl-note tl-note-quiet">Lives in Google Tasks. Tick it there; LoR picks it up within a few minutes.</p> : null}
      {n.flagged_at ? (
        <div className="tl-flagnote">
          <p>
            <Icon name="flag" size={14} /> {n.flag_note || "The agent flagged this for you."}
          </p>
          <div className="tl-row-actions">
            <button type="button" className="tl-btn tl-btn-primary" disabled={off || google} onClick={() => reversible(`Done: ${n.title}`, () => setDone(id, true))}>
              <Icon name="check" size={16} /> Done
            </button>
            <button type="button" className="tl-btn" disabled={off} onClick={() => reversible(`Seen: ${n.title}`, () => unflag(id))}>
              Seen
            </button>
          </div>
        </div>
      ) : null}
      {n.status === "waiting" ? <p className="tl-note tl-note-quiet">Waiting on {n.waiting_on ?? "someone"}.</p> : null}

      <section className="tl-field">
        <h3 className="tl-label">
          Due <span className="tl-label-v">{n.due_local ? `${shortDate(n.due_local)} · ${dueWords(n.due_local, today)}` : "No date"}</span>
        </h3>
        <div className="tl-chips">
          {[
            { d: today, t: "Today" },
            { d: addDays(today, 1), t: "Tomorrow" },
            { d: nextMon, t: `Mon ${nextMon.slice(8).replace(/^0/, "")}` },
          ].map((o) => (
            <button key={o.t} type="button" className="tl-chip" data-on={n.due_local === o.d || undefined} disabled={off} onClick={() => setDue(o.d)}>
              {o.t}
            </button>
          ))}
          <input
            type="date"
            className="tl-input tl-input-date"
            aria-label="Due date"
            value={n.due_local ?? ""}
            disabled={off}
            onChange={(e) => e.target.value && setDue(e.target.value)}
          />
          {n.due_local ? (
            <button type="button" className="tl-chip tl-chip-ghost" disabled={off} onClick={() => setDue(null)}>
              No date
            </button>
          ) : null}
        </div>
      </section>

      {gaps && !done ? (
        <section className="tl-field">
          <h3 className="tl-label">
            Today’s route{" "}
            <span className="tl-label-v">{planned ? `planned ${hm(planned.start)} to ${hm(planned.start + planned.minutes * 60_000)}` : "not planned"}</span>
          </h3>
          {planned ? (
            <button type="button" className="tl-btn" disabled={off} onClick={() => unplan(id)}>
              Take it off the route
            </button>
          ) : gaps.length ? (
            <div className="tl-chips">
              {gaps.slice(0, 4).map((g) => (
                <button key={g.start} type="button" className="tl-chip tl-chip-plan" disabled={off} onClick={() => plan(id, n.title, Math.max(g.start, now), Math.min(minutes, Math.round((g.end - Math.max(g.start, now)) / 60_000)))}>
                  {hm(Math.max(g.start, now))} <small>{dur(g.end - Math.max(g.start, now))} free</small>
                </button>
              ))}
            </div>
          ) : (
            <p className="tl-muted">No free time left today.</p>
          )}
        </section>
      ) : null}

      {showOpen ? (
        <a className="tl-link" href={href("/tasks", { id: n.title })}>
          Open in Tasks <Icon name="chevron" size={14} />
        </a>
      ) : null}
    </div>
  );
}
