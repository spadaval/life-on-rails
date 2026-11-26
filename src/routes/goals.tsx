// Goals: what the time is for. Each goal with its week's hours against target,
// and the projects and habits under it. Tasks live in Tasks; here they are
// counts that open Tasks filtered to the one thing.
import { type ReactNode, useState } from "react";
import { Link, createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { type GraphNode, IntentEditor, numOrNull, parentChoices } from "../components/IntentEditor.js";
import { shortDate } from "../components/format.js";
import { byRef } from "../components/links.js";
import { Empty, Meter, Notice, PageHead, Panel, Star } from "../components/ui.js";
import { getGraphFn, habitFn, saveIntentFn } from "../server/functions/graph.js";
import { GlobalActions } from "./__root.js";

const Search = z.object({
  /** A goal, project or habit by id or title. */
  id: z.coerce.string().optional(),
  done: z.boolean().optional(),
  /** Opens the selected one's editor, by link. */
  edit: z.coerce.boolean().optional(),
});

export const Route = createFileRoute("/goals")({
  validateSearch: (search) => Search.parse(search),
  loader: () => getGraphFn(),
  component: Goals,
});

type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;
type IntentType = GraphNode["type"];

const TYPE_NAME: Record<IntentType, string> = { goal: "Goal", project: "Project", habit: "Habit", task: "Task", metric: "Metric" };
const round1 = (n: number) => String(Math.round(n * 10) / 10);
const isOpen = (n: GraphNode) => n.status === "active" || n.status === "waiting";
const tasks = (n: number) => `${n} ${n === 1 ? "task" : "tasks"}`;

/** " · 1.2h over" when the week's hours passed the target, else nothing. */
const overBy = (hours: number | null, target: number | null) =>
  target && (hours ?? 0) - target >= 0.05 ? ` · ${round1((hours ?? 0) - target)}h over` : "";

/** A goal's hours this week against its target, saying how far over when it is. */
const weekHours = (hours: number | null, target: number | null): string | undefined =>
  target ? `${round1(hours ?? 0)} / ${round1(target)}h this week${overBy(hours, target)}` : hours ? `${round1(hours)}h this week` : undefined;

function Goals() {
  const { nodes, entities } = Route.useLoaderData();
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/goals" });
  const router = useRouter();
  const [notice, setNotice] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
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

  const live = nodes.filter((n) => n.status !== "archived");
  const shown = live.filter((n) => n.type !== "task" && (showDone || isOpen(n) || n.status === "paused"));
  const ids = new Set(shown.map((n) => n.id));
  const top = shown.filter((n) => !n.parents.some((p) => ids.has(p.id)));
  const goals = top.filter((n) => n.type === "goal").sort((a, b) => (b.weekly_hour_target ?? 0) - (a.weekly_hour_target ?? 0) || a.title.localeCompare(b.title));
  const loose = top.filter((n) => n.type !== "goal");
  const openTasks = useOpenTasks(live);
  const selected = byRef(live, search.id, (n) => n.title);
  const count = (t: IntentType) => live.filter((n) => n.type === t && isOpen(n)).length;

  return (
    <div className="flex flex-col gap-4 pb-4">
      <PageHead title="Goals" sub={`${count("goal")} goals · ${count("project")} projects · ${count("habit")} habits`}>
        <GlobalActions />
      </PageHead>
      <Notice text={notice} onClose={() => setNotice(null)} />

      <div className="flex flex-wrap items-center gap-3">
        <button className="btn btn-secondary" onClick={() => setAdding((v) => !v)}>
          {adding ? "Close" : "New goal, project or habit"}
        </button>
        <label className="flex items-center gap-2 text-small text-mute">
          <input type="checkbox" checked={showDone} onChange={(e) => set({ done: e.target.checked || undefined })} />
          Show done, paused and later
        </label>
      </div>

      {adding ? <NewIntent nodes={live} defaultParent={selected} onDone={() => setAdding(false)} act={act} /> : null}

      <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1fr)_420px]">
        {/* Cards in a row stretch to the tallest, so every row starts level. */}
        <div className="grid gap-[22px] lg:grid-cols-2">
          {goals.length === 0 && loose.length === 0 ? (
            <Panel title="No goals yet">
              <Empty>Add a goal, give it hours a week, and put projects and habits under it.</Empty>
            </Panel>
          ) : null}
          {goals.map((g) => (
            <GoalCard key={g.id} goal={g} kids={shown.filter((k) => k.parents.some((p) => p.id === g.id))} shown={shown} openTasks={openTasks} ownTasks={live.filter((t) => t.type === "task" && isOpen(t) && t.parents.some((p) => p.id === g.id)).length} selected={selected?.id ?? null} onSelect={(id) => set({ id, edit: undefined })} act={act} />
          ))}
          {loose.length > 0 ? (
            <Panel title="Not under a goal">
              <ul className="-mx-2">
                {loose.map((n) => (
                  <Row key={n.id} n={n} shown={shown} depth={0} openTasks={openTasks} selected={selected?.id ?? null} onSelect={(id) => set({ id, edit: undefined })} act={act} />
                ))}
              </ul>
            </Panel>
          ) : null}
        </div>
        <aside className="xl:sticky xl:top-4">
          {selected ? (
            <Details key={selected.id + selected.updated_at} node={selected} edit={!!search.edit} nodes={live} entities={entities} openTasks={openTasks} act={act} onSelect={(id) => set({ id, edit: undefined })} onClose={() => set({ id: undefined, edit: undefined })} />
          ) : (
            <Panel title="Nothing selected">
              <Empty>Pick a goal, project or habit to see it and change it.</Empty>
            </Panel>
          )}
        </aside>
      </div>
    </div>
  );
}

/** Open tasks under each intent, counting everything beneath it. */
function useOpenTasks(nodes: GraphNode[]): Map<string, number> {
  const out = new Map<string, number>();
  const byId = new Map(nodes.map((n) => [n.id, n]));
  for (const t of nodes) {
    if (t.type !== "task" || !isOpen(t)) continue;
    const seen = new Set<string>();
    const stack = t.parents.map((p) => p.id);
    while (stack.length) {
      const id = stack.pop()!;
      if (seen.has(id)) continue;
      seen.add(id);
      out.set(id, (out.get(id) ?? 0) + 1);
      stack.push(...(byId.get(id)?.parents.map((p) => p.id) ?? []));
    }
  }
  return out;
}

function TaskCount({ id, n }: { id: string; n: number }) {
  return (
    <Link to="/tasks" search={{ for: id }} className="shrink-0 text-small text-dim hover:text-or" title="Open these in Tasks">
      {n === 0 ? "no tasks" : tasks(n)}
    </Link>
  );
}

/**
 * One goal: its week's hours, and the projects and habits under it. Each project row carries its
 * own task count; the foot counts only the tasks filed on the goal itself, so no task is said twice.
 */
function GoalCard({ goal: g, kids, shown, openTasks, ownTasks, selected, onSelect, act }: {
  goal: GraphNode; kids: GraphNode[]; shown: GraphNode[]; openTasks: Map<string, number>; ownTasks: number; selected: string | null; onSelect: (id: string) => void; act: Act;
}) {
  const order = (n: GraphNode) => (n.type === "goal" ? 0 : n.type === "project" ? 1 : n.type === "habit" ? 2 : 3);
  return (
    <Panel
      title={
        <button className={`flex items-center gap-2 text-left hover:text-or ${selected === g.id ? "text-or" : ""}`} onClick={() => onSelect(g.id)}>
          <Star size={11} className={g.week_hours ? "text-or" : "text-dim"} />
          {g.title}
        </button>
      }
      aside={<span className="whitespace-nowrap">{weekHours(g.week_hours, g.weekly_hour_target)}</span>}
    >
      {g.weekly_hour_target ? (
        <div className="mb-2">
          <Meter value={g.week_hours ?? 0} target={g.weekly_hour_target} />
        </div>
      ) : null}
      {kids.length === 0 ? <Empty>Nothing under this goal yet.</Empty> : null}
      <ul className="-mx-2">
        {[...kids].sort((a, b) => order(a) - order(b) || a.title.localeCompare(b.title)).map((k) => (
          <Row key={k.id} n={k} shown={shown} depth={0} openTasks={openTasks} selected={selected} onSelect={onSelect} act={act} />
        ))}
      </ul>
      {ownTasks > 0 ? (
        <p className="mt-auto flex justify-end pt-2">
          <Link to="/tasks" search={{ for: g.id, group: "for" }} className="text-small text-dim hover:text-or" title="Open this goal's tasks in Tasks, grouped by what each counts toward">
            {tasks(ownTasks)} on the goal itself
          </Link>
        </p>
      ) : null}
    </Panel>
  );
}

function Row({ n, shown, depth, openTasks, selected, onSelect, act }: {
  n: GraphNode; shown: GraphNode[]; depth: number; openTasks: Map<string, number>; selected: string | null; onSelect: (id: string) => void; act: Act;
}) {
  const kids = shown.filter((k) => k.parents.some((p) => p.id === n.id));
  const quiet = !isOpen(n);
  return (
    <li>
      <div className={`flex items-center gap-2.5 py-1.5 pr-2 ${selected === n.id ? "row-sel" : "hover:bg-raised"}`} style={{ paddingLeft: 8 + depth * 20 }}>
        <span className={`cond w-[54px] shrink-0 text-small tracking-[0.06em] uppercase ${n.type === "habit" ? "text-or-deep" : "text-dim"}`}>{TYPE_NAME[n.type]}</span>
        <button className={`min-w-0 flex-1 truncate text-left ${quiet ? "text-dim" : ""}`} onClick={() => onSelect(n.id)}>
          {n.title}
          {quiet ? <span className="ml-2 text-small">{n.status}</span> : null}
        </button>
        {n.due_local && !quiet ? <span className="shrink-0 text-small text-dim">due {shortDate(n.due_local)}</span> : null}
        {n.weekly_hour_target ? <span className="shrink-0 text-small text-mute">{round1(n.week_hours ?? 0)} / {round1(n.weekly_hour_target)}h</span> : null}
        {n.type === "habit" && n.frequency ? <span className="shrink-0 text-small text-mute">{n.frequency} per {n.frequency_horizon ?? "day"}</span> : null}
        {n.type === "habit" ? (
          <button className="btn btn-primary btn-sm" onClick={() => act(() => habitFn({ data: { id: n.id } }), `Logged “${n.title}”.`)}>
            Did it
          </button>
        ) : n.type === "project" || n.type === "goal" ? (
          <TaskCount id={n.id} n={openTasks.get(n.id) ?? 0} />
        ) : null}
      </div>
      {kids.length > 0 && depth < 4 ? (
        <ul>
          {kids.map((k) => (
            <Row key={k.id} n={k} shown={shown} depth={depth + 1} openTasks={openTasks} selected={selected} onSelect={onSelect} act={act} />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

function Details({ node: n, edit, nodes, entities, openTasks, act, onSelect, onClose }: {
  node: GraphNode; edit: boolean; nodes: GraphNode[]; entities: { id: string; name: string }[]; openTasks: Map<string, number>; act: Act; onSelect: (id: string) => void; onClose: () => void;
}) {
  const [editing, setEditing] = useState(edit);
  const kids = nodes.filter((k) => k.type !== "task" && k.parents.some((p) => p.id === n.id) && isOpen(k));
  const facts: [string, ReactNode][] = [
    ["Kind", TYPE_NAME[n.type]],
    ...(n.type === "goal" || n.weekly_hour_target ? [["This week", `${round1(n.week_hours ?? 0)} / ${round1(n.weekly_hour_target ?? 0)}h${n.starving ? " · under target" : overBy(n.week_hours, n.weekly_hour_target)}`] as [string, string]] : []),
    ["Status", n.status],
    ...(n.due_local ? [["Due", shortDate(n.due_local)] as [string, string]] : []),
    ...(n.parents.length
      ? [["Counts toward", n.parents.map((p, i) => (
          <span key={p.id}>
            {i > 0 ? ", " : ""}
            <button className="link" onClick={() => onSelect(p.id)}>{p.title}</button>
          </span>
        ))] as [string, ReactNode]]
      : []),
    ...(n.type === "habit" && n.frequency ? [["How often", `${n.frequency} per ${n.frequency_horizon ?? "day"}`] as [string, string]] : []),
    ...(n.type !== "habit" && n.type !== "metric" ? [["Tasks", <TaskCount key="t" id={n.id} n={openTasks.get(n.id) ?? 0} />] as [string, ReactNode]] : []),
  ];

  return (
    <Panel framed title={n.title} aside={<button className="btn btn-quiet btn-sm" onClick={onClose}>Close</button>}>
      {n.via === "agent" ? (
        <p className="mt-1.5">
          <span className="tag-agent">added by the agent</span>
        </p>
      ) : null}
      <dl className="mt-3 grid grid-cols-[110px_1fr] gap-x-3 gap-y-1.5 text-body">
        {facts.map(([k, v]) => (
          <div key={k} className="contents">
            <dt className="cond text-small font-medium text-dim">{k}</dt>
            <dd>{v}</dd>
          </div>
        ))}
      </dl>
      {n.weekly_hour_target ? (
        <div className="mt-2">
          <Meter value={n.week_hours ?? 0} target={n.weekly_hour_target} />
        </div>
      ) : null}
      {n.description ? <p className="mt-3 text-body whitespace-pre-line text-mute">{n.description}</p> : null}

      <div className="mt-3 flex flex-wrap gap-2">
        {n.type === "habit" ? (
          <button className={`btn ${editing ? "btn-secondary" : "btn-primary"}`} onClick={() => act(() => habitFn({ data: { id: n.id } }), `Logged “${n.title}”.`)}>
            Did it
          </button>
        ) : null}
        <button className="btn btn-secondary" onClick={() => setEditing((v) => !v)}>
          {editing ? "Stop editing" : "Edit"}
        </button>
      </div>

      {editing ? (
        <div className="mt-4 border-t border-line pt-4">
          <IntentEditor node={n} nodes={nodes} entities={entities} act={act} />
        </div>
      ) : null}

      {kids.length > 0 ? (
        <section className="mt-5">
          <h4 className="cond flex justify-between border-b border-line pb-1 text-small font-semibold text-mute">
            Under it
            <span className="font-sans text-small font-normal text-dim">{kids.length}</span>
          </h4>
          <ul className="mt-1">
            {kids.map((k) => (
              <li key={k.id} className="flex items-baseline justify-between gap-2 py-1">
                <button className="text-left hover:text-or" onClick={() => onSelect(k.id)}>
                  {k.title}
                </button>
                <span className="text-small text-dim">{TYPE_NAME[k.type].toLowerCase()}</span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </Panel>
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
      className="flex flex-wrap items-center gap-2 rounded-sm bg-panel p-3"
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
      <select className="input w-[120px]" value={type} onChange={(e) => setType(e.target.value as IntentType)} aria-label="Kind">
        {(["goal", "project", "habit", "metric"] as const).map((t) => (
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
      <button className="btn btn-primary" type="submit" disabled={!title.trim()}>
        Add
      </button>
    </form>
  );
}
