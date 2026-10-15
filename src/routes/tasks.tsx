import { useState } from "react";
import { createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { type GraphNode, IntentEditor, TaskCheck, parentChoices } from "../components/IntentEditor.js";
import { AgentTag, Empty, Notice, PageHead, Panel, Star } from "../components/ui.js";
import { getGraphFn, resetVisibilityFn, saveIntentFn, undoIntentFn } from "../server/functions/graph.js";
import { GlobalActions } from "./__root.js";
import { shortDate } from "./index.js";

const VIEWS = [
  { id: "open", label: "Open" },
  { id: "waiting", label: "Waiting" },
  { id: "agent", label: "With the agent" },
  { id: "later", label: "Later" },
  { id: "done", label: "Done" },
] as const;
type View = (typeof VIEWS)[number]["id"];

export const Route = createFileRoute("/tasks")({
  validateSearch: (search) => z.object({ view: z.enum(["open", "waiting", "agent", "later", "done"]).optional(), id: z.string().optional() }).parse(search),
  loader: () => getGraphFn(),
  component: Tasks,
});

const todayYmd = () => new Intl.DateTimeFormat("en-CA", { year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
const plusDays = (ymd: string, n: number) => {
  const d = new Date(`${ymd}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().slice(0, 10);
};

function Tasks() {
  const { nodes, entities } = Route.useLoaderData();
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/tasks" });
  const router = useRouter();
  const [notice, setNotice] = useState<string | null>(null);
  const view: View = search.view ?? "open";

  const act = async (fn: () => Promise<unknown>, done?: string) => {
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err));
    }
    await router.invalidate();
  };

  const tasks = nodes.filter((n) => n.type === "task");
  const open = tasks.filter((n) => n.status === "active" || n.status === "waiting");
  const rows = tasks.filter((n) => inView(view, n));
  const count = (v: View) => tasks.filter((n) => inView(v, n)).length;

  const groups = view === "open" ? groupOpen(rows) : view === "done" ? [{ title: "Done", rows: [...rows].sort((a, b) => b.updated_at.localeCompare(a.updated_at)).slice(0, 60) }] : [{ title: VIEWS.find((v) => v.id === view)!.label, rows: byDue(rows) }];
  const selected = tasks.find((n) => n.id === search.id) ?? null;
  const select = (id: string | null) => void navigate({ search: (s) => ({ ...s, id: id ?? undefined }), replace: true });

  return (
    <div className="space-y-5 pb-4">
      <PageHead title="Tasks" sub={`${open.length} open`}>
        <GlobalActions />
      </PageHead>
      <Notice text={notice} onClose={() => setNotice(null)} />

      <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1fr)_440px]">
        <div className="min-w-0 space-y-4">
          <NewTask nodes={nodes} act={act} />

          <nav className="flex flex-wrap border-b border-line" aria-label="Which tasks">
            {VIEWS.map((v) => (
              <button
                key={v.id}
                onClick={() => void navigate({ search: { view: v.id === "open" ? undefined : v.id } })}
                className={`-mb-px border-b-2 px-3.5 py-2 text-[14px] ${view === v.id ? "border-or text-txt" : "border-transparent text-mute hover:text-txt"}`}
              >
                {v.label}
                <span className="ml-1.5 text-[12.5px] text-dim">{count(v.id)}</span>
              </button>
            ))}
          </nav>

          {rows.length === 0 ? <Empty>{view === "open" ? "Nothing open. Add one above, or ask the agent." : "Nothing here."}</Empty> : null}
          {groups.map((g) =>
            g.rows.length === 0 ? null : (
              <section key={g.title}>
                <h2 className="h-sec mb-1 text-[13.5px] text-mute">
                  {g.title} <span className="font-sans text-[12.5px] font-normal tracking-normal text-dim">{g.rows.length}</span>
                </h2>
                <ul className="border-t border-line">
                  {g.rows.map((n) => (
                    <TaskRow key={n.id} node={n} selected={selected?.id === n.id} onSelect={() => select(selected?.id === n.id ? null : n.id)} act={act} />
                  ))}
                </ul>
              </section>
            ),
          )}

          {view === "open" ? (
            <p className="text-[13px] text-mute">
              Too much?{" "}
              <button
                className="underline decoration-dim hover:text-txt"
                onClick={() =>
                  act(async () => {
                    const r = await resetVisibilityFn({ data: {} });
                    setNotice(r.moved === 0 ? "Nothing overdue to clear." : `Moved ${r.moved} overdue to Later. Nothing was deleted.`);
                  })
                }
              >
                Move everything past due to Later
              </button>
              . Nothing is deleted.
            </p>
          ) : null}
        </div>

        <aside className="xl:sticky xl:top-4">
          {selected ? (
            <Panel title="Task" aside={<button className="btn btn-sm" onClick={() => select(null)}>Close</button>}>
              <IntentEditor key={selected.id + selected.updated_at} node={selected} nodes={nodes} entities={entities} act={act} />
            </Panel>
          ) : (
            <Panel title="Task">
              <Empty>Pick a task to edit its due date, notes, what it counts toward, or who is doing it.</Empty>
            </Panel>
          )}
        </aside>
      </div>
    </div>
  );
}

function inView(view: View, n: GraphNode): boolean {
  const live = n.status === "active" || n.status === "waiting";
  if (view === "open") return n.status === "active" && n.assignee !== "agent";
  if (view === "waiting") return n.status === "waiting" && n.assignee !== "agent";
  if (view === "agent") return live && n.assignee === "agent";
  if (view === "later") return n.status === "later" || n.status === "paused";
  return n.status === "done";
}

function byDue(rows: GraphNode[]) {
  return [...rows].sort((a, b) => (a.due_local ?? "9999").localeCompare(b.due_local ?? "9999") || a.title.localeCompare(b.title));
}

function groupOpen(rows: GraphNode[]) {
  const today = todayYmd();
  const week = plusDays(today, 7);
  const sorted = byDue(rows);
  const flagged = sorted.filter((n) => n.flagged_at);
  const rest = sorted.filter((n) => !n.flagged_at);
  return [
    { title: "Needs you", rows: flagged },
    { title: "Past due", rows: rest.filter((n) => n.due_local && n.due_local < today) },
    { title: "This week", rows: rest.filter((n) => n.due_local && n.due_local >= today && n.due_local <= week) },
    { title: "Later on", rows: rest.filter((n) => n.due_local && n.due_local > week) },
    { title: "No date", rows: rest.filter((n) => !n.due_local) },
  ];
}

function TaskRow({ node: n, selected, onSelect, act }: { node: GraphNode; selected: boolean; onSelect: () => void; act: (fn: () => Promise<unknown>, done?: string) => Promise<void> }) {
  const done = n.status === "done";
  const google = n.google_task_id !== null;
  return (
    <li className={`flex items-start gap-3 border-b border-line px-2 py-2.5 ${selected ? "row-sel" : "hover:bg-raised"}`}>
      <TaskCheck id={n.id} title={n.title} done={done} google={google} act={act} />
      <button className="min-w-0 flex-1 text-left" onClick={onSelect} aria-expanded={selected}>
        <span className={`flex items-baseline gap-2 ${done ? "text-dim line-through" : ""}`}>
          {n.flagged_at ? <Star size={9} className="translate-y-[-1px] text-or" /> : null}
          {n.title}
        </span>
        <small className="block text-[13px] text-mute">
          {[
            n.due_local ? `due ${shortDate(n.due_local)}` : null,
            n.status === "waiting" ? `waiting on ${n.waiting_on ?? "someone"}` : null,
            n.parents.map((p) => p.title).join(", ") || null,
            google ? "Google Tasks" : null,
            n.flag_note,
          ]
            .filter(Boolean)
            .join(" · ")}
        </small>
      </button>
      {n.via === "agent" ? <AgentTag onUndo={() => act(() => undoIntentFn({ data: { id: n.id } }), `Removed “${n.title}”.`)} /> : null}
    </li>
  );
}

function NewTask({ nodes, act }: { nodes: GraphNode[]; act: (fn: () => Promise<unknown>, done?: string) => Promise<void> }) {
  const [title, setTitle] = useState("");
  const [parent, setParent] = useState("");
  const [due, setDue] = useState("");
  const parents = parentChoices(nodes).sort((a, b) => (a.type === b.type ? a.title.localeCompare(b.title) : a.type === "project" ? -1 : 1));

  return (
    <form
      className="flex flex-wrap gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        if (!title.trim()) return;
        void act(
          () => saveIntentFn({ data: { type: "task", title: title.trim(), parentId: parent || null, dueAt: due || null, weight: 1 } }),
          `Added “${title.trim()}”.`,
        ).then(() => {
          setTitle("");
          setDue("");
        });
      }}
    >
      <input className="input min-w-[240px] flex-[3]" placeholder="New task…" value={title} onChange={(e) => setTitle(e.target.value)} aria-label="New task" />
      <select className="input min-w-[230px] flex-[2]" value={parent} onChange={(e) => setParent(e.target.value)} aria-label="Counts toward">
        <option value="">Counts toward nothing</option>
        {parents.map((p) => (
          <option key={p.id} value={p.id}>
            {p.type}: {p.title}
          </option>
        ))}
      </select>
      <input className="input w-[150px]" type="date" value={due} onChange={(e) => setDue(e.target.value)} aria-label="Due" />
      <button className="btn border-or text-or" type="submit" disabled={!title.trim()}>
        Add task
      </button>
    </form>
  );
}
