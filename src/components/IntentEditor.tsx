// Edit one intent: its fields, what it counts toward, and the actions its
// type allows. Shared by Tasks and Graph.
import { type ReactNode, useState } from "react";
import {
  assignFn, deleteIntentFn, explainFn, type getGraphFn, linkFn, metricFn, saveIntentFn, taskDoneFn, unlinkFn,
} from "../server/functions/graph.js";

type GraphData = Awaited<ReturnType<typeof getGraphFn>>;
export type GraphNode = GraphData["nodes"][number];
export type Entity = GraphData["entities"][number];
export type Act = (fn: () => Promise<unknown>, done?: string) => Promise<unknown>;

export const numOrNull = (s: string): number | null => (s.trim() === "" || Number.isNaN(Number(s)) ? null : Number(s));

function Field({ label, children, wide = false }: { label: string; children: ReactNode; wide?: boolean }) {
  return (
    <label className={`block text-[13px] text-mute ${wide ? "sm:col-span-2" : ""}`}>
      <span className="mb-1 block">{label}</span>
      {children}
    </label>
  );
}

export function IntentEditor({ node: n, nodes, entities, act }: { node: GraphNode; nodes: GraphNode[]; entities: Entity[]; act: Act }) {
  const [title, setTitle] = useState(n.title);
  const [description, setDescription] = useState(n.description ?? "");
  const [weight, setWeight] = useState(String(n.weight));
  const [hours, setHours] = useState(n.weekly_hour_target?.toString() ?? "");
  const [frequency, setFrequency] = useState(n.frequency?.toString() ?? "");
  const [horizon, setHorizon] = useState<"day" | "week">(n.frequency_horizon ?? "day");
  const [estimate, setEstimate] = useState(n.estimate_hours?.toString() ?? "");
  const [due, setDue] = useState(n.due_local ?? "");
  const [status, setStatus] = useState(n.status);
  const [waitingOn, setWaitingOn] = useState(n.waiting_on_entity_id ?? "");
  const [parent, setParent] = useState("");
  const [explain, setExplain] = useState<string | null>(null);

  const save = () =>
    act(
      () =>
        saveIntentFn({
          data: {
            id: n.id,
            type: n.type,
            title,
            description: description || null,
            status,
            weight: Number(weight) || 0,
            weeklyHourTarget: numOrNull(hours),
            frequency: numOrNull(frequency),
            frequencyHorizon: horizon,
            estimateHours: numOrNull(estimate),
            dueAt: due || null,
            waitingOnEntityId: waitingOn || null,
          },
        }),
      "Saved.",
    );

  const candidates = nodes.filter(
    (p) => p.id !== n.id && p.type !== "task" && p.type !== "metric" && p.status !== "done" && !n.parents.some((x) => x.id === p.id),
  );

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Title" wide>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="Status">
          <select className="input" value={status} onChange={(e) => setStatus(e.target.value as GraphNode["status"])}>
            {["active", "waiting", "paused", "later", "done"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </Field>
        {n.type === "task" ? (
          <Field label="Due">
            <input className="input" type="date" value={due} onChange={(e) => setDue(e.target.value)} />
          </Field>
        ) : (
          <Field label="Weight">
            <input className="input" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} />
          </Field>
        )}
        {n.type === "goal" || n.type === "project" ? (
          <Field label="Hours a week">
            <input className="input" inputMode="decimal" value={hours} onChange={(e) => setHours(e.target.value)} />
          </Field>
        ) : null}
        {n.type === "habit" ? (
          <Field label="How often">
            <span className="flex gap-2">
              <input className="input w-20" inputMode="numeric" value={frequency} onChange={(e) => setFrequency(e.target.value)} />
              <select className="input" value={horizon} onChange={(e) => setHorizon(e.target.value as "day" | "week")}>
                <option value="day">per day</option>
                <option value="week">per week</option>
              </select>
            </span>
          </Field>
        ) : null}
        {n.type === "project" ? (
          <Field label="Due">
            <input className="input" type="date" value={due} onChange={(e) => setDue(e.target.value)} />
          </Field>
        ) : null}
        {n.type === "project" || n.type === "task" ? (
          <Field label="Estimate (hours)">
            <input className="input" inputMode="decimal" value={estimate} onChange={(e) => setEstimate(e.target.value)} />
          </Field>
        ) : null}
        {n.type === "task" ? (
          <Field label="Waiting on">
            <select className="input" value={waitingOn} onChange={(e) => setWaitingOn(e.target.value)}>
              <option value="">nobody</option>
              {entities.map((e) => (
                <option key={e.id} value={e.id}>
                  {e.name}
                </option>
              ))}
            </select>
          </Field>
        ) : null}
        <Field label="Notes" wide>
          <textarea className="input" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
      </div>

      {n.type === "metric" ? (
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-[13px] text-mute">Log a value</span>
          {[1, 2, 3, 4, 5].map((v) => (
            <button key={v} className="btn btn-sm min-w-8" onClick={() => act(() => metricFn({ data: { id: n.id, value: v } }), `Logged ${v} for “${n.title}”.`)}>
              {v}
            </button>
          ))}
        </div>
      ) : null}

      <div className="space-y-2">
        <p className="text-[13px] text-mute">Counts toward</p>
        <div className="flex flex-wrap gap-2">
          {n.parents.length === 0 ? <span className="text-[14px] text-dim">Nothing yet.</span> : null}
          {n.parents.map((p) => (
            <span key={p.link_id} className="chip text-txt">
              {p.title}
              {p.via === "agent" ? <span className="text-cy">agent</span> : null}
              <button className="text-mute underline hover:text-txt" onClick={() => act(() => unlinkFn({ data: { id: p.link_id } }))}>
                remove
              </button>
            </span>
          ))}
        </div>
        <span className="flex gap-2">
          <select className="input" value={parent} onChange={(e) => setParent(e.target.value)} aria-label="Add a parent">
            <option value="">Add a parent…</option>
            {candidates.map((p) => (
              <option key={p.id} value={p.id}>
                {p.type}: {p.title}
              </option>
            ))}
          </select>
          <button className="btn" disabled={!parent} onClick={() => act(() => linkFn({ data: { fromId: n.id, toId: parent, kind: "measures" } })).then(() => setParent(""))}>
            Link
          </button>
        </span>
      </div>

      <div className="flex flex-wrap gap-2 border-t border-line pt-3">
        <button className="btn border-or text-or" onClick={save}>
          Save
        </button>
        {n.type === "task" ? (
          <>
            {n.status === "done" && !n.google_task_id ? (
              <button className="btn" onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: false } }), "Reopened.")}>
                Reopen
              </button>
            ) : null}
            <button className="btn" onClick={() => act(() => assignFn({ data: { id: n.id, assignee: n.assignee === "agent" ? "owner" : "agent" } }))}>
              {n.assignee === "agent" ? "Take it back" : "Give to the agent"}
            </button>
          </>
        ) : (
          <button className="btn" onClick={async () => setExplain((await explainFn({ data: { id: n.id } })).json)}>
            Explain score
          </button>
        )}
        <button
          className="btn btn-danger ml-auto"
          disabled={n.google_task_id !== null}
          title={n.google_task_id ? "Lives in Google Tasks: delete it there" : "Deleted, or archived if time or check-ins were recorded against it"}
          onClick={() => confirm(`Delete “${n.title}”?`) && act(() => deleteIntentFn({ data: { id: n.id } }), `Deleted “${n.title}”.`)}
        >
          Delete
        </button>
      </div>
      {explain ? <pre className="overflow-x-auto border border-line bg-well p-3 text-xs text-mute">{explain}</pre> : null}
    </div>
  );
}

export interface NewIntentData {
  type: GraphNode["type"];
  title: string;
  parentId: string | null;
  weeklyHourTarget: number | null;
  frequency: number | null;
  frequencyHorizon: "day" | "week";
  weight: number;
  dueAt?: string | null;
}

/** Parents an intent of this type may hang under: anything but tasks and metrics that is still open. */
export const parentChoices = (nodes: GraphNode[]) =>
  nodes.filter((p) => p.type !== "task" && p.type !== "metric" && (p.status === "active" || p.status === "waiting"));

/** A task's checkbox. It flips at once; the reload that follows confirms it. */
export function TaskCheck({ id, title, done, google, act, className = "mt-[5px] size-[15px]" }: {
  id: string; title: string; done: boolean; google: boolean; act: Act; className?: string;
}) {
  const [checked, setChecked] = useState(done);
  const [seen, setSeen] = useState(done);
  if (seen !== done) {
    setSeen(done);
    setChecked(done);
  }
  return (
    <input
      type="checkbox"
      className={`shrink-0 ${className}`}
      checked={checked}
      disabled={google}
      title={google ? "Lives in Google Tasks: check it off there" : checked ? "Not done after all" : "Done"}
      onChange={(e) => {
        const next = e.target.checked;
        setChecked(next);
        void act(() => taskDoneFn({ data: { id, done: next } }));
      }}
      aria-label={title}
    />
  );
}
