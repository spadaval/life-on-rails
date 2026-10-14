import { useState } from "react";
import { createFileRoute, useRouter } from "@tanstack/react-router";
import { pct } from "../components/format.js";
import {
  archiveIntentFn, assignFn, explainFn, getGraphFn, habitFn, linkFn, metricFn, resetVisibilityFn, saveIntentFn,
  taskDoneFn, undoIntentFn, unlinkFn,
} from "../server/functions/graph.js";

export const Route = createFileRoute("/graph")({
  loader: () => getGraphFn(),
  component: Graph,
});

type Data = Awaited<ReturnType<typeof getGraphFn>>;
type Node = Data["nodes"][number];
type IntentType = Node["type"];

const SECTIONS: { type: IntentType; title: string }[] = [
  { type: "goal", title: "Goals" },
  { type: "project", title: "Projects" },
  { type: "habit", title: "Habits" },
  { type: "metric", title: "Metrics" },
  { type: "task", title: "Tasks" },
];

const numOrNull = (s: string): number | null => (s.trim() === "" || Number.isNaN(Number(s)) ? null : Number(s));

function Graph() {
  const { nodes, entities } = Route.useLoaderData();
  const router = useRouter();
  const [open, setOpen] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [showHidden, setShowHidden] = useState(false);

  /** Run a write, then reload. A refusal becomes a notice instead of a crash. */
  const act = async (fn: () => Promise<unknown>, done?: string) => {
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err));
    }
    await router.invalidate();
  };

  const visible = (n: Node) => showHidden || (n.status !== "done" && n.status !== "later");
  const hidden = nodes.filter((n) => n.status === "done" || n.status === "later").length;

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-3xl font-bold">Graph</h1>
        <div className="flex gap-2">
          <button className="btn" onClick={() => setShowHidden((v) => !v)}>
            {showHidden ? "Hide done and later" : `Show done and later (${hidden})`}
          </button>
          <button
            className="btn"
            onClick={() =>
              act(async () => {
                const r = await resetVisibilityFn({ data: {} });
                setNotice(r.moved === 0 ? "Nothing overdue to clear." : `Moved ${r.moved} overdue to Later. Nothing was deleted.`);
              })
            }
          >
            Reset visibility
          </button>
        </div>
      </header>

      {notice ? (
        <p className="card text-sm" role="status">
          {notice}{" "}
          <button className="underline" onClick={() => setNotice(null)}>
            dismiss
          </button>
        </p>
      ) : null}

      <NewIntent nodes={nodes} onSave={(data) => act(() => saveIntentFn({ data }))} />

      {SECTIONS.map(({ type, title }) => {
        const rows = nodes.filter((n) => n.type === type && visible(n));
        if (rows.length === 0) return null;
        return (
          <section key={type}>
            <h2 className="mb-2 text-xl font-semibold">{title}</h2>
            <ul className="space-y-2">
              {rows.map((n) => (
                <li key={n.id} className="card">
                  <Row node={n} expanded={open === n.id} onToggle={() => setOpen(open === n.id ? null : n.id)} act={act} />
                  {open === n.id ? <Editor node={n} nodes={nodes} entities={entities} act={act} /> : null}
                </li>
              ))}
            </ul>
          </section>
        );
      })}
      {nodes.length === 0 ? <p className="muted">Nothing here yet. Add a goal to start.</p> : null}
    </div>
  );
}

type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;

function Row({ node: n, expanded, onToggle, act }: { node: Node; expanded: boolean; onToggle: () => void; act: Act }) {
  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
      <button className="min-w-0 flex-1 text-left text-lg font-medium" onClick={onToggle} aria-expanded={expanded}>
        <span className={n.status === "done" ? "line-through opacity-60" : ""}>{n.title}</span>
        {n.parents.length > 0 ? <span className="muted text-sm"> → {n.parents.map((p) => p.title).join(", ")}</span> : null}
      </button>
      {n.starving ? (
        <span className="chip border-bad text-bad">
          starving · {n.week_hours ?? 0}/{n.weekly_hour_target}h
        </span>
      ) : n.type === "goal" && n.weekly_hour_target ? (
        <span className="chip">
          {n.week_hours ?? 0}/{n.weekly_hour_target}h this week
        </span>
      ) : null}
      {n.overdue ? <span className="chip border-warn text-warn">overdue</span> : null}
      {n.status !== "active" ? <span className="chip">{n.status === "waiting" ? `waiting on ${n.waiting_on ?? "someone"}` : n.status}</span> : null}
      {n.assignee === "agent" ? <span className="chip">with the agent</span> : null}
      {n.google_task_id ? <span className="chip">Google Tasks</span> : null}
      {n.via === "agent" ? (
        <span className="chip gap-1">
          agent
          <button className="underline" onClick={() => act(() => undoIntentFn({ data: { id: n.id } }), `Removed “${n.title}”.`)}>
            undo
          </button>
        </span>
      ) : null}
      {n.type !== "task" ? (
        <span className="chip font-semibold" title={`confidence: ${n.confidence ?? "none"}`}>
          {pct(n.score)}
          {n.confidence && n.confidence !== "high" ? <span className="muted">&nbsp;· {n.confidence}</span> : null}
        </span>
      ) : null}
      {n.type === "task" && n.status !== "done" ? (
        n.google_task_id ? (
          <span className="muted text-sm">complete in Google</span>
        ) : (
          <button className="btn" onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: true } }))}>
            Done
          </button>
        )
      ) : null}
      {n.type === "habit" ? (
        <button className="btn" onClick={() => act(() => habitFn({ data: { id: n.id } }), `Logged “${n.title}”.`)}>
          Did it
        </button>
      ) : null}
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block text-sm">
      <span className="muted">{label}</span>
      {children}
    </label>
  );
}

function Editor({ node: n, nodes, entities, act }: { node: Node; nodes: Node[]; entities: Data["entities"]; act: Act }) {
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
    act(() =>
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
    );

  const candidates = nodes.filter((p) => p.id !== n.id && p.type !== "task" && p.type !== "metric" && !n.parents.some((x) => x.id === p.id));

  return (
    <div className="mt-3 space-y-3 border-t border-black/10 pt-3 dark:border-white/10">
      <div className="grid gap-3 sm:grid-cols-2">
        <Field label="Title">
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="Status">
          <select className="input" value={status} onChange={(e) => setStatus(e.target.value as Node["status"])}>
            {["active", "waiting", "paused", "later", "done"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </Field>
        <Field label="Weight">
          <input className="input" inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} />
        </Field>
        {n.type === "goal" || n.type === "project" ? (
          <Field label="Weekly hour target">
            <input className="input" inputMode="decimal" value={hours} onChange={(e) => setHours(e.target.value)} />
          </Field>
        ) : null}
        {n.type === "habit" ? (
          <Field label="How often">
            <span className="flex gap-2">
              <input className="input" inputMode="numeric" value={frequency} onChange={(e) => setFrequency(e.target.value)} />
              <select className="input" value={horizon} onChange={(e) => setHorizon(e.target.value as "day" | "week")}>
                <option value="day">per day</option>
                <option value="week">per week</option>
              </select>
            </span>
          </Field>
        ) : null}
        {n.type === "project" || n.type === "task" ? (
          <>
            <Field label="Estimate (hours)">
              <input className="input" inputMode="decimal" value={estimate} onChange={(e) => setEstimate(e.target.value)} />
            </Field>
            <Field label="Due">
              <input className="input" type="date" value={due} onChange={(e) => setDue(e.target.value)} />
            </Field>
          </>
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
      </div>
      <Field label="Notes">
        <textarea className="input" rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </Field>

      {n.type === "metric" ? (
        <div className="flex flex-wrap items-center gap-2">
          <span className="muted text-sm">Log a value:</span>
          {[1, 2, 3, 4, 5].map((v) => (
            <button key={v} className="btn" onClick={() => act(() => metricFn({ data: { id: n.id, value: v } }), `Logged ${v} for “${n.title}”.`)}>
              {v}
            </button>
          ))}
        </div>
      ) : null}

      <div className="space-y-1 text-sm">
        <span className="muted">Counts toward:</span>
        {n.parents.length === 0 ? <span className="muted"> nothing yet</span> : null}
        {n.parents.map((p) => (
          <span key={p.link_id} className="chip ml-2 gap-1">
            {p.title}
            {p.via === "agent" ? <span className="muted">(agent)</span> : null}
            <button className="underline" onClick={() => act(() => unlinkFn({ data: { id: p.link_id } }))}>
              remove
            </button>
          </span>
        ))}
        <span className="mt-2 flex gap-2">
          <select className="input" value={parent} onChange={(e) => setParent(e.target.value)}>
            <option value="">add a parent…</option>
            {candidates.map((p) => (
              <option key={p.id} value={p.id}>
                {p.type}: {p.title}
              </option>
            ))}
          </select>
          <button
            className="btn"
            disabled={!parent}
            onClick={() => act(() => linkFn({ data: { fromId: n.id, toId: parent, kind: "measures" } })).then(() => setParent(""))}
          >
            Link
          </button>
        </span>
      </div>

      <div className="flex flex-wrap gap-2">
        <button className="btn btn-primary" onClick={save}>
          Save
        </button>
        {n.type === "task" ? (
          <>
            {n.status === "done" && !n.google_task_id ? (
              <button className="btn" onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: false } }))}>
                Reopen
              </button>
            ) : null}
            <button
              className="btn"
              onClick={() => act(() => assignFn({ data: { id: n.id, assignee: n.assignee === "agent" ? "owner" : "agent" } }))}
            >
              {n.assignee === "agent" ? "Take it back" : "Give to the agent"}
            </button>
          </>
        ) : (
          <button className="btn" onClick={async () => setExplain((await explainFn({ data: { id: n.id } })).json)}>
            Explain score
          </button>
        )}
        <button className="btn btn-danger" onClick={() => act(() => archiveIntentFn({ data: { id: n.id } }), `Archived “${n.title}”.`)}>
          Archive
        </button>
      </div>
      {explain ? <pre className="overflow-x-auto rounded-lg bg-black/5 p-3 text-xs dark:bg-white/10">{explain}</pre> : null}
    </div>
  );
}

interface NewIntentData {
  type: IntentType;
  title: string;
  parentId: string | null;
  weeklyHourTarget: number | null;
  frequency: number | null;
  frequencyHorizon: "day" | "week";
  weight: number;
}

function NewIntent({ nodes, onSave }: { nodes: Node[]; onSave: (data: NewIntentData) => Promise<void> }) {
  const [type, setType] = useState<IntentType>("task");
  const [title, setTitle] = useState("");
  const [parent, setParent] = useState("");
  const [hours, setHours] = useState("");
  const [frequency, setFrequency] = useState("1");
  const [horizon, setHorizon] = useState<"day" | "week">("day");

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    await onSave({
      type,
      title,
      parentId: parent || null,
      weeklyHourTarget: type === "goal" || type === "project" ? numOrNull(hours) : null,
      frequency: type === "habit" ? numOrNull(frequency) : null,
      frequencyHorizon: horizon,
      // A goal that has an hour target matters enough to be flagged when it starves.
      weight: type === "goal" && numOrNull(hours) ? 2 : 1,
    });
    setTitle("");
  };

  return (
    <form className="card grid gap-2 sm:grid-cols-[8rem_1fr]" onSubmit={submit}>
      <select className="input" value={type} onChange={(e) => setType(e.target.value as IntentType)} aria-label="Type">
        {SECTIONS.map((s) => (
          <option key={s.type} value={s.type}>
            {s.type}
          </option>
        ))}
      </select>
      <input className="input" placeholder={`New ${type}…`} value={title} onChange={(e) => setTitle(e.target.value)} aria-label="Title" />
      <span className="sm:col-start-2 flex flex-wrap gap-2">
        {type !== "goal" ? (
          <select className="input sm:w-auto" value={parent} onChange={(e) => setParent(e.target.value)} aria-label="Parent">
            <option value="">no parent</option>
            {nodes
              .filter((p) => p.type !== "task" && p.type !== "metric" && p.status === "active")
              .map((p) => (
                <option key={p.id} value={p.id}>
                  {p.type}: {p.title}
                </option>
              ))}
          </select>
        ) : null}
        {type === "goal" || type === "project" ? (
          <input className="input sm:w-40" inputMode="decimal" placeholder="hours / week" value={hours} onChange={(e) => setHours(e.target.value)} />
        ) : null}
        {type === "habit" ? (
          <>
            <input className="input sm:w-20" inputMode="numeric" value={frequency} onChange={(e) => setFrequency(e.target.value)} aria-label="Times" />
            <select className="input sm:w-auto" value={horizon} onChange={(e) => setHorizon(e.target.value as "day" | "week")}>
              <option value="day">per day</option>
              <option value="week">per week</option>
            </select>
          </>
        ) : null}
        <button className="btn btn-primary" type="submit">
          Add
        </button>
      </span>
    </form>
  );
}
