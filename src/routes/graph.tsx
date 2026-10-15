import { useEffect, useState } from "react";
import { createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { type GraphNode, IntentEditor, TaskCheck, numOrNull, parentChoices } from "../components/IntentEditor.js";
import { type MapFilter, StarMap } from "../components/StarMap.js";
import { AgentTag, Empty, Meter, Notice, PageHead, Panel, Star } from "../components/ui.js";
import { getGraphFn, habitFn, saveIntentFn, taskDoneFn, undoIntentFn } from "../server/functions/graph.js";
import { GlobalActions } from "./__root.js";
import { shortDate } from "./index.js";

const Search = z.object({
  view: z.enum(["map", "outline"]).optional(),
  f: z.enum(["all", "goal", "project", "habit", "task"]).optional(),
  id: z.string().optional(),
  done: z.boolean().optional(),
});

export const Route = createFileRoute("/graph")({
  validateSearch: (search) => Search.parse(search),
  loader: () => getGraphFn(),
  component: Graph,
});

type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;
type IntentType = GraphNode["type"];

const FILTERS: { id: MapFilter; label: string }[] = [
  { id: "all", label: "All" },
  { id: "goal", label: "Goals" },
  { id: "project", label: "Projects" },
  { id: "habit", label: "Habits" },
  { id: "task", label: "Tasks" },
];

const TYPE_NAME: Record<IntentType, string> = { goal: "Goal", project: "Project", habit: "Habit", task: "Task", metric: "Metric" };

function Graph() {
  const { nodes, entities } = Route.useLoaderData();
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/graph" });
  const router = useRouter();
  const [notice, setNotice] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const view = search.view ?? "map";
  const filter = search.f ?? "all";
  const showDone = search.done ?? false;

  const act: Act = async (fn, done) => {
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err));
    }
    await router.invalidate();
  };
  const set = (patch: Partial<z.infer<typeof Search>>) => void navigate({ search: (s) => ({ ...s, ...patch }), replace: true });

  // A phone is too narrow for the map: it opens on the outline unless asked.
  useEffect(() => {
    if (!search.view && window.matchMedia("(max-width: 767px)").matches) set({ view: "outline" });
  }, []);

  const live = nodes.filter((n) => n.status !== "archived");
  const counts = (t: IntentType) => live.filter((n) => n.type === t && n.status !== "done").length;
  const selected = live.find((n) => n.id === search.id) ?? null;

  return (
    <div className="flex flex-col gap-4 pb-4">
      <PageHead title="Graph" sub={`${counts("goal")} goals · ${counts("project")} projects · ${counts("habit")} habits · ${counts("task")} open tasks`}>
        <GlobalActions />
      </PageHead>
      <Notice text={notice} onClose={() => setNotice(null)} />

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex border border-edge" role="tablist" aria-label="Show">
          {FILTERS.map((f) => (
            <button
              key={f.id}
              role="tab"
              aria-selected={filter === f.id}
              onClick={() => set({ f: f.id === "all" ? undefined : f.id })}
              className={`border-l border-edge px-3.5 py-1 text-[13.5px] first:border-l-0 ${filter === f.id ? "row-sel text-txt" : "text-mute hover:text-txt"}`}
            >
              {f.label}
            </button>
          ))}
        </div>
        <label className="flex items-center gap-2 text-[13.5px] text-mute">
          <input type="checkbox" checked={showDone} onChange={(e) => set({ done: e.target.checked || undefined })} />
          Show done and later
        </label>
        <button className="btn" onClick={() => setAdding((v) => !v)}>
          {adding ? "Close" : "New…"}
        </button>
        <div className="ml-auto flex border border-edge" role="tablist" aria-label="View">
          {(["map", "outline"] as const).map((v) => (
            <button
              key={v}
              role="tab"
              aria-selected={view === v}
              onClick={() => set({ view: v === "map" ? undefined : v })}
              className={`border-l border-edge px-3.5 py-1 text-[13.5px] capitalize first:border-l-0 ${view === v ? "row-sel text-txt" : "text-mute hover:text-txt"}`}
            >
              {v}
            </button>
          ))}
        </div>
      </div>

      {adding ? <NewIntent nodes={live} defaultParent={selected} onDone={() => setAdding(false)} act={act} /> : null}

      <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1fr)_400px]">
        <div className="xl:sticky xl:top-4">
          {view === "map" ? (
            <div className="panel h-[62vh] xl:h-[calc(100dvh-196px)] xl:min-h-[560px]">
              <div className="face">
                <StarMap nodes={live} selected={selected?.id ?? null} onSelect={(id) => set({ id: id ?? undefined })} filter={filter} showDone={showDone} />
              </div>
            </div>
          ) : (
            <Outline nodes={live} filter={filter} showDone={showDone} selected={selected?.id ?? null} onSelect={(id) => set({ id })} act={act} />
          )}
        </div>
        <aside>
          {selected ? (
            <SystemInfo key={selected.id + selected.updated_at} node={selected} nodes={live} entities={entities} act={act} onSelect={(id) => set({ id })} onClose={() => set({ id: undefined })} />
          ) : (
            <Panel title="Nothing selected">
              <Empty>Pick a star to see what it holds. Drag to pan, scroll to zoom.</Empty>
            </Panel>
          )}
        </aside>
      </div>
    </div>
  );
}

const childrenOf = (id: string, nodes: GraphNode[]) => nodes.filter((n) => n.parents.some((p) => p.id === id));
const visibleNode = (n: GraphNode, showDone: boolean) => showDone || (n.status !== "done" && n.status !== "later");
const round1 = (n: number) => String(Math.round(n * 10) / 10);

function SystemInfo({ node: n, nodes, entities, act, onSelect, onClose }: {
  node: GraphNode; nodes: GraphNode[]; entities: { id: string; name: string }[]; act: Act; onSelect: (id: string) => void; onClose: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const kids = childrenOf(n.id, nodes).filter((k) => visibleNode(k, false) || k.status === "done");
  const projects = kids.filter((k) => k.type === "project");
  const habits = kids.filter((k) => k.type === "habit");
  const tasks = kids.filter((k) => k.type === "task");
  const facts: [string, string][] = [
    ["Class", TYPE_NAME[n.type]],
    ...(n.type === "goal" || n.weekly_hour_target ? [["This week", `${round1(n.week_hours ?? 0)} / ${round1(n.weekly_hour_target ?? 0)}h${n.starving ? " · under target" : ""}`] as [string, string]] : []),
    ["Status", n.status === "waiting" ? `waiting on ${n.waiting_on ?? "someone"}` : n.status],
    ...(n.due_local ? [["Due", shortDate(n.due_local)] as [string, string]] : []),
    ...(n.parents.length ? [["Counts toward", n.parents.map((p) => p.title).join(", ")] as [string, string]] : []),
    ...(n.type === "project" && n.tasks_total ? [["Tasks", `${n.tasks_done} of ${n.tasks_total} done`] as [string, string]] : []),
    ...(n.type === "habit" && n.frequency ? [["How often", `${n.frequency} per ${n.frequency_horizon ?? "day"}`] as [string, string]] : []),
  ];

  return (
    <Panel title={n.title} aside={<button className="btn btn-sm" onClick={onClose}>Close</button>}>
      {n.via === "agent" ? (
        <p className="mt-1.5">
          <AgentTag onUndo={() => act(() => undoIntentFn({ data: { id: n.id } }), `Removed “${n.title}”.`).then(onClose)} />
        </p>
      ) : null}
      <dl className="mt-3 grid grid-cols-[110px_1fr] gap-x-3 gap-y-1.5 text-[14px]">
        {facts.map(([k, v]) => (
          <div key={k} className="contents">
            <dt className="cond text-[12.5px] font-medium text-dim">{k}</dt>
            <dd>{v}</dd>
          </div>
        ))}
      </dl>
      {n.type === "goal" && n.weekly_hour_target ? (
        <div className="mt-2">
          <Meter value={n.week_hours ?? 0} target={n.weekly_hour_target} />
        </div>
      ) : null}
      {n.description ? <p className="mt-3 text-[14px] whitespace-pre-line text-mute">{n.description}</p> : null}

      <div className="mt-3 flex flex-wrap gap-2">
        {n.type === "habit" ? (
          <button className="btn border-or text-or" onClick={() => act(() => habitFn({ data: { id: n.id } }), `Logged “${n.title}”.`)}>
            Did it
          </button>
        ) : null}
        {n.type === "task" ? (
          <button className="btn border-or text-or" disabled={n.google_task_id !== null} onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: n.status !== "done" } }))}>
            {n.status === "done" ? "Not done after all" : "Done"}
          </button>
        ) : null}
        <button className="btn" onClick={() => setEditing((v) => !v)}>
          {editing ? "Stop editing" : "Edit"}
        </button>
      </div>

      {editing ? (
        <div className="mt-4 border-t border-line pt-4">
          <IntentEditor node={n} nodes={nodes} entities={entities} act={act} />
        </div>
      ) : null}

      {projects.length > 0 ? (
        <Section title="Projects" count={projects.length}>
          {projects.map((p) => {
            const pt = childrenOf(p.id, nodes).filter((t) => t.type === "task" && t.status !== "later");
            return (
              <li key={p.id} className="py-1.5">
                <button className="flex w-full items-baseline justify-between gap-2 text-left hover:text-or" onClick={() => onSelect(p.id)}>
                  <span>{p.title}</span>
                  <span className="shrink-0 text-[13px] text-dim">{pt.length ? `${pt.length} ${pt.length === 1 ? "task" : "tasks"}` : "no tasks"}</span>
                </button>
                {pt.length > 0 ? (
                  <ul className="mt-1 ml-3 space-y-1">
                    {pt.map((t) => (
                      <TaskLine key={t.id} t={t} act={act} onSelect={onSelect} />
                    ))}
                  </ul>
                ) : null}
              </li>
            );
          })}
        </Section>
      ) : null}
      {habits.length > 0 ? (
        <Section title="Habits" count={habits.length}>
          {habits.map((h) => (
            <li key={h.id} className="flex items-center justify-between gap-2 py-1.5">
              <button className="text-left hover:text-or" onClick={() => onSelect(h.id)}>
                {h.title}
              </button>
              <button className="btn btn-sm" onClick={() => act(() => habitFn({ data: { id: h.id } }), `Logged “${h.title}”.`)}>
                Did it
              </button>
            </li>
          ))}
        </Section>
      ) : null}
      {tasks.length > 0 || n.type === "project" || n.type === "goal" ? (
        <Section title="Tasks" count={tasks.length}>
          {tasks.map((t) => (
            <TaskLine key={t.id} t={t} act={act} onSelect={onSelect} />
          ))}
          {n.type !== "task" && n.type !== "metric" ? <AddTask parent={n} act={act} /> : null}
        </Section>
      ) : null}
    </Panel>
  );
}

function Section({ title, count, children }: { title: string; count: number; children: React.ReactNode }) {
  return (
    <section className="mt-5">
      <h4 className="cond flex justify-between border-b border-line pb-1 text-[13px] font-semibold text-mute">
        {title}
        <span className="font-sans text-[12.5px] font-normal text-dim">{count}</span>
      </h4>
      <ul className="mt-1">{children}</ul>
    </section>
  );
}

function TaskLine({ t, act, onSelect }: { t: GraphNode; act: Act; onSelect: (id: string) => void }) {
  const done = t.status === "done";
  return (
    <li className="flex items-start gap-2.5 py-1 text-[14px]">
      <TaskCheck id={t.id} title={t.title} done={t.status === "done"} google={t.google_task_id !== null} act={act} className="mt-[5px] size-[14px]" />
      <button className={`text-left hover:text-or ${done ? "text-dim line-through" : ""}`} onClick={() => onSelect(t.id)}>
        {t.flagged_at ? <Star size={8} className="mr-1.5 translate-y-[-1px] text-or" /> : null}
        {t.title}
        {t.due_local && !done ? <span className="ml-2 text-[12.5px] text-dim">due {shortDate(t.due_local)}</span> : null}
      </button>
    </li>
  );
}

function AddTask({ parent, act }: { parent: GraphNode; act: Act }) {
  const [title, setTitle] = useState("");
  return (
    <li className="pt-2">
      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          if (!title.trim()) return;
          void act(() => saveIntentFn({ data: { type: "task", title: title.trim(), parentId: parent.id, weight: 1 } }), `Added “${title.trim()}”.`).then(() => setTitle(""));
        }}
      >
        <input className="input py-1" placeholder={`New task for ${parent.title.length > 24 ? `${parent.title.slice(0, 23)}…` : parent.title}`} value={title} onChange={(e) => setTitle(e.target.value)} aria-label="New task" />
        <button className="btn border-or text-or" type="submit" disabled={!title.trim()}>
          Add task
        </button>
      </form>
    </li>
  );
}

function Outline({ nodes, filter, showDone, selected, onSelect, act }: {
  nodes: GraphNode[]; filter: MapFilter; showDone: boolean; selected: string | null; onSelect: (id: string) => void; act: Act;
}) {
  const shown = nodes.filter((n) => visibleNode(n, showDone));
  const goals = shown.filter((n) => n.type === "goal").sort((a, b) => a.title.localeCompare(b.title));
  const ids = new Set(shown.map((n) => n.id));
  const unattached = shown.filter((n) => n.type !== "goal" && !n.parents.some((p) => ids.has(p.id)));
  const wants = (t: IntentType) => filter === "all" || filter === t || (filter === "task" && t !== "goal");

  const Row = ({ n, depth }: { n: GraphNode; depth: number }) => {
    const kids = shown.filter((k) => k.parents.some((p) => p.id === n.id) && wants(k.type));
    const done = n.status === "done";
    return (
      <li>
        <div className={`flex items-center gap-2.5 py-1.5 pr-2 ${selected === n.id ? "row-sel" : "hover:bg-raised"}`} style={{ paddingLeft: 8 + depth * 22 }}>
          {n.type === "task" ? (
            <TaskCheck id={n.id} title={n.title} done={n.status === "done"} google={n.google_task_id !== null} act={act} className="size-[14px]" />
          ) : n.type === "habit" ? (
            <span className="size-2 shrink-0 border border-mute" />
          ) : (
            <span className="size-1.5 shrink-0 rounded-full bg-mute" />
          )}
          <button className={`min-w-0 flex-1 truncate text-left ${done ? "text-dim line-through" : ""}`} onClick={() => onSelect(n.id)}>
            {n.flagged_at ? <Star size={8} className="mr-1.5 translate-y-[-1px] text-or" /> : null}
            {n.title}
          </button>
          {n.due_local && !done ? <span className="shrink-0 text-[12.5px] text-dim">due {shortDate(n.due_local)}</span> : null}
          {n.type === "habit" ? (
            <button className="btn btn-sm" onClick={() => act(() => habitFn({ data: { id: n.id } }), `Logged “${n.title}”.`)}>
              Did it
            </button>
          ) : null}
        </div>
        {kids.length > 0 ? (
          <ul>
            {kids.map((k) => (
              <Row key={k.id} n={k} depth={depth + 1} />
            ))}
          </ul>
        ) : null}
      </li>
    );
  };

  return (
    <div className="grid gap-[22px] lg:grid-cols-2">
      {goals.map((g) => {
        const kids = shown.filter((k) => k.parents.some((p) => p.id === g.id) && wants(k.type));
        return (
          <Panel
            key={g.id}
            title={
              <button className={`text-left hover:text-or ${selected === g.id ? "text-or" : ""}`} onClick={() => onSelect(g.id)}>
                {g.title}
              </button>
            }
            aside={g.weekly_hour_target ? `${round1(g.week_hours ?? 0)} / ${round1(g.weekly_hour_target)}h` : undefined}
          >
            {g.weekly_hour_target ? (
              <div className="mb-2">
                <Meter value={g.week_hours ?? 0} target={g.weekly_hour_target} />
              </div>
            ) : null}
            {kids.length === 0 ? <Empty>Nothing under this goal yet.</Empty> : null}
            <ul className="-mx-2">
              {kids.map((k) => (
                <Row key={k.id} n={k} depth={0} />
              ))}
            </ul>
          </Panel>
        );
      })}
      {unattached.filter((n) => wants(n.type)).length > 0 ? (
        <Panel title="Not under a goal">
          <ul className="-mx-2">
            {unattached
              .filter((n) => wants(n.type))
              .map((n) => (
                <Row key={n.id} n={n} depth={0} />
              ))}
          </ul>
        </Panel>
      ) : null}
    </div>
  );
}

function NewIntent({ nodes, defaultParent, onDone, act }: { nodes: GraphNode[]; defaultParent: GraphNode | null; onDone: () => void; act: Act }) {
  const [type, setType] = useState<IntentType>("project");
  const [title, setTitle] = useState("");
  const [parent, setParent] = useState(defaultParent && defaultParent.type !== "task" && defaultParent.type !== "metric" ? defaultParent.id : "");
  const [hours, setHours] = useState("");
  const [frequency, setFrequency] = useState("1");
  const [horizon, setHorizon] = useState<"day" | "week">("week");

  return (
    <form
      className="flex flex-wrap items-center gap-2 border border-edge bg-panel p-3"
      onSubmit={(e) => {
        e.preventDefault();
        if (!title.trim()) return;
        void act(
          () =>
            saveIntentFn({
              data: {
                type,
                title: title.trim(),
                parentId: type === "goal" ? null : parent || null,
                weeklyHourTarget: type === "goal" || type === "project" ? numOrNull(hours) : null,
                frequency: type === "habit" ? numOrNull(frequency) : null,
                frequencyHorizon: horizon,
                // A goal that has an hour target matters enough to be flagged when it starves.
                weight: type === "goal" && numOrNull(hours) ? 2 : 1,
              },
            }),
          `Added “${title.trim()}”.`,
        ).then(onDone);
      }}
    >
      <select className="input w-[120px]" value={type} onChange={(e) => setType(e.target.value as IntentType)} aria-label="Type">
        {(["goal", "project", "habit", "task", "metric"] as const).map((t) => (
          <option key={t} value={t}>
            {TYPE_NAME[t]}
          </option>
        ))}
      </select>
      <input className="input min-w-[220px] flex-1" placeholder={`New ${type}…`} value={title} onChange={(e) => setTitle(e.target.value)} aria-label="Title" autoFocus />
      {type !== "goal" ? (
        <select className="input w-[240px]" value={parent} onChange={(e) => setParent(e.target.value)} aria-label="Counts toward">
          <option value="">Counts toward nothing</option>
          {parentChoices(nodes).map((p) => (
            <option key={p.id} value={p.id}>
              {p.type}: {p.title}
            </option>
          ))}
        </select>
      ) : null}
      {type === "goal" || type === "project" ? (
        <input className="input w-[130px]" inputMode="decimal" placeholder="hours a week" value={hours} onChange={(e) => setHours(e.target.value)} aria-label="Hours a week" />
      ) : null}
      {type === "habit" ? (
        <>
          <input className="input w-16" inputMode="numeric" value={frequency} onChange={(e) => setFrequency(e.target.value)} aria-label="Times" />
          <select className="input w-[120px]" value={horizon} onChange={(e) => setHorizon(e.target.value as "day" | "week")} aria-label="Per">
            <option value="day">per day</option>
            <option value="week">per week</option>
          </select>
        </>
      ) : null}
      <button className="btn border-or text-or" type="submit" disabled={!title.trim()}>
        Add
      </button>
    </form>
  );
}
