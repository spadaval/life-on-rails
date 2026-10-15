import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { ago, dueLabel, longDate } from "../components/format.js";
import { type GraphNode, TaskCheck, parentChoices } from "../components/IntentEditor.js";
import { Key, QuickAdd, type QuickAddOptions, typing } from "../components/QuickAdd.js";
import { Empty, Notice, PageHead, Panel, Star } from "../components/ui.js";
import {
  bulkTasksFn, deleteIntentFn, getGraphFn, resetVisibilityFn, saveIntentFn, taskDoneFn, unflagFn,
} from "../server/functions/graph.js";
import { addDays } from "../shared/localTime.js";
import { GlobalActions } from "./__root.js";

const VIEWS = [
  { id: "open", label: "Open" },
  { id: "waiting", label: "Waiting" },
  { id: "agent", label: "With the agent" },
  { id: "later", label: "Later" },
  { id: "done", label: "Done" },
] as const;
type View = (typeof VIEWS)[number]["id"];

const DUE = [
  { id: "overdue", label: "Past due" },
  { id: "today", label: "Due today" },
  { id: "week", label: "Due this week" },
  { id: "dated", label: "Has a date" },
  { id: "none", label: "No date" },
] as const;
const FROM = [
  { id: "agent", label: "Added by the agent" },
  { id: "you", label: "Added by you" },
  { id: "google", label: "From Google Tasks" },
] as const;
const GROUPS = [
  { id: "due", label: "Due date" },
  { id: "for", label: "What it counts toward" },
  { id: "status", label: "Status" },
  { id: "none", label: "No grouping" },
] as const;
const SORTS = [
  { id: "due", label: "Due date" },
  { id: "created", label: "Newest first" },
  { id: "updated", label: "Recently changed" },
  { id: "title", label: "Title" },
] as const;

const Search = z.object({
  view: z.enum(["open", "waiting", "agent", "later", "done"]).optional(),
  for: z.string().optional(),
  due: z.enum(["overdue", "today", "week", "dated", "none"]).optional(),
  from: z.enum(["agent", "you", "google"]).optional(),
  flagged: z.boolean().optional(),
  q: z.string().optional(),
  group: z.enum(["due", "for", "status", "none"]).optional(),
  sort: z.enum(["due", "created", "updated", "title"]).optional(),
  id: z.string().optional(),
});
type Filters = z.infer<typeof Search>;

export const Route = createFileRoute("/tasks")({
  validateSearch: (search) => Search.parse(search),
  loader: () => getGraphFn(),
  component: Tasks,
});

type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;
interface Group {
  key: string;
  title: string;
  rows: GraphNode[];
}

function Tasks() {
  const { nodes, entities, today } = Route.useLoaderData();
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/tasks" });
  const router = useRouter();
  const [notice, setNotice] = useState<string | null>(null);
  const [cursor, setCursor] = useState<string | null>(null);
  const [picked, setPicked] = useState<ReadonlySet<string>>(new Set());
  const filterBox = useRef<HTMLInputElement>(null);

  const view: View = search.view ?? "open";
  const group = search.group ?? (view === "done" ? "none" : "due");
  const sort = search.sort ?? (view === "done" ? "updated" : "due");
  const set = (patch: Partial<Filters>) => void navigate({ search: (s) => ({ ...s, ...patch }), replace: true });

  const act: Act = async (fn, done) => {
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err));
    }
    await router.invalidate();
  };

  const byId = useMemo(() => new Map(nodes.map((n) => [n.id, n])), [nodes]);
  const tasks = nodes.filter((n) => n.type === "task");
  const parents = parentChoices(nodes).sort(byTypeThenTitle);
  const quick: QuickAddOptions = useMemo(
    () => ({ today, parents: parents.map((p) => ({ id: p.id, name: p.title, type: p.type })), people: entities }),
    [today, nodes, entities],
  );

  /** Everything a task counts toward, all the way up, so filtering by a goal finds its projects' tasks. */
  const ancestors = (n: GraphNode): Set<string> => {
    const seen = new Set<string>();
    const walk = (x: GraphNode) => {
      for (const p of x.parents) {
        if (seen.has(p.id)) continue;
        seen.add(p.id);
        const up = byId.get(p.id);
        if (up) walk(up);
      }
    };
    walk(n);
    return seen;
  };

  const q = search.q?.trim().toLowerCase() ?? "";
  const filtered = tasks.filter(
    (n) =>
      inView(view, n) &&
      (!search.for || ancestors(n).has(search.for)) &&
      (!search.due || dueMatches(search.due, n.due_local, today)) &&
      (!search.from || fromMatches(search.from, n)) &&
      (!search.flagged || n.flagged_at !== null) &&
      (!q || `${n.title} ${n.description ?? ""} ${n.parents.map((p) => p.title).join(" ")} ${n.waiting_on ?? ""}`.toLowerCase().includes(q)),
  );
  const sorted = [...filtered].sort(sorter(sort));
  const groups = grouped(sorted, group, today, view);
  const order = groups.flatMap((g) => g.rows);
  const count = (v: View) => tasks.filter((n) => inView(v, n)).length;
  const selected = tasks.find((n) => n.id === search.id) ?? null;
  const open = (id: string | null) => set({ id: id ?? undefined });
  const pickedIds = [...picked].filter((id) => order.some((n) => n.id === id));
  const filtering = !!(search.for || search.due || search.from || search.flagged || q);

  // Keyboard, Linear-style. Single keys only when no field has focus.
  const keys = useRef<(e: KeyboardEvent) => void>(() => {});
  keys.current = (e) => {
    if (e.metaKey || e.ctrlKey || e.altKey || typing(e.target)) return;
    const i = cursor ? order.findIndex((n) => n.id === cursor) : -1;
    const at = i >= 0 ? order[i] : undefined;
    const move = (d: number) => {
      const next = order[Math.max(0, Math.min(order.length - 1, i < 0 ? 0 : i + d))];
      if (!next) return;
      setCursor(next.id);
      document.getElementById(`task-${next.id}`)?.scrollIntoView({ block: "nearest" });
      if (search.id) open(next.id);
    };
    const handled = () => e.preventDefault();
    if (e.key === "j" || e.key === "ArrowDown") (handled(), move(1));
    else if (e.key === "k" || e.key === "ArrowUp") (handled(), move(-1));
    else if ((e.key === "Enter" || e.key === "o") && at) (handled(), open(at.id));
    else if (e.key === "x" && at) {
      handled();
      const next = new Set(picked);
      if (next.has(at.id)) next.delete(at.id);
      else next.add(at.id);
      setPicked(next);
    } else if (e.key === "d" && (pickedIds.length || at)) {
      handled();
      if (pickedIds.length) void act(() => bulkTasksFn({ data: { ids: pickedIds, action: { op: "done" } } }), `Done: ${pickedIds.length}.`).then(() => setPicked(new Set()));
      else if (at && !at.google_task_id) void act(() => taskDoneFn({ data: { id: at.id, done: at.status !== "done" } }));
    } else if (e.key === "/") (handled(), filterBox.current?.focus());
    else if (e.key === "Escape") {
      if (search.id) open(null);
      else if (picked.size) setPicked(new Set());
    }
  };
  useEffect(() => {
    const h = (e: KeyboardEvent) => keys.current(e);
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, []);

  return (
    <div className="space-y-4 pb-4">
      <PageHead title="Tasks" sub={`${count("open")} open${count("waiting") ? ` · ${count("waiting")} waiting` : ""}`}>
        <GlobalActions />
      </PageHead>
      <Notice text={notice} onClose={() => setNotice(null)} />

      <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1fr)_440px]">
        <div className="min-w-0 space-y-3">
          <QuickAdd options={quick} />

          <nav className="flex flex-wrap border-b border-line" aria-label="Which tasks">
            {VIEWS.map((v) => (
              <button
                key={v.id}
                onClick={() => {
                  setPicked(new Set());
                  set({ view: v.id === "open" ? undefined : v.id, group: undefined, sort: undefined });
                }}
                className={`-mb-px border-b-2 px-3.5 py-2 text-[14px] ${view === v.id ? "border-or text-txt" : "border-transparent text-mute hover:text-txt"}`}
              >
                {v.label}
                <span className="ml-1.5 text-[12.5px] text-dim">{count(v.id)}</span>
              </button>
            ))}
          </nav>

          <div className="flex flex-wrap items-center gap-2">
            <input
              ref={filterBox}
              className="input w-[180px] py-1"
              placeholder="Filter…  /"
              aria-label="Filter by text"
              value={search.q ?? ""}
              onChange={(e) => set({ q: e.target.value || undefined })}
              onKeyDown={(e) => e.key === "Escape" && (e.currentTarget.blur(), set({ q: undefined }))}
            />
            <Choice label="For" value={search.for} onChange={(v) => set({ for: v })} options={parents.map((p) => ({ id: p.id, label: `${p.title}`, hint: p.type }))} />
            <Choice label="Due" value={search.due} onChange={(v) => set({ due: v as Filters["due"] })} options={DUE.map((d) => ({ id: d.id, label: d.label }))} />
            <Choice label="From" value={search.from} onChange={(v) => set({ from: v as Filters["from"] })} options={FROM.map((d) => ({ id: d.id, label: d.label }))} />
            <button className={`btn ${search.flagged ? "border-or/70 text-or" : "text-mute"}`} aria-pressed={!!search.flagged} onClick={() => set({ flagged: search.flagged ? undefined : true })}>
              <Star size={9} /> Needs you
            </button>
            {filtering ? (
              <button className="text-[13px] text-mute underline decoration-dim hover:text-txt" onClick={() => set({ for: undefined, due: undefined, from: undefined, flagged: undefined, q: undefined })}>
                Clear
              </button>
            ) : null}
            <span className="ml-auto flex items-center gap-2">
              <Choice label="Group" value={group} always onChange={(v) => set({ group: (v ?? "none") as Filters["group"] })} options={GROUPS.map((g) => ({ id: g.id, label: g.label }))} />
              <Choice label="Sort" value={sort} always onChange={(v) => set({ sort: (v ?? "due") as Filters["sort"] })} options={SORTS.map((g) => ({ id: g.id, label: g.label }))} />
            </span>
          </div>

          {pickedIds.length ? <BulkBar ids={pickedIds} parents={parents} today={today} act={act} onClear={() => setPicked(new Set())} /> : null}

          {order.length === 0 ? (
            <Empty>{filtering ? "Nothing matches these filters." : view === "open" ? "Nothing open. Press q to add one." : "Nothing here."}</Empty>
          ) : null}
          {groups.map((g) =>
            g.rows.length === 0 ? null : (
              <section key={g.key}>
                {group !== "none" ? (
                  <h2 className="h-sec mt-2 mb-1 text-[13.5px] text-mute">
                    {g.title} <span className="font-sans text-[12.5px] font-normal tracking-normal text-dim">{g.rows.length}</span>
                  </h2>
                ) : null}
                <ul className="border-t border-line">
                  {g.rows.map((n) => (
                    <TaskRow
                      key={n.id}
                      node={n}
                      today={today}
                      showParent={group !== "for"}
                      open={selected?.id === n.id}
                      cursor={cursor === n.id}
                      picked={picked.has(n.id)}
                      onOpen={() => {
                        setCursor(n.id);
                        open(selected?.id === n.id ? null : n.id);
                      }}
                      onPick={() => {
                        const next = new Set(picked);
                        if (next.has(n.id)) next.delete(n.id);
                        else next.add(n.id);
                        setCursor(n.id);
                        setPicked(next);
                      }}
                      act={act}
                    />
                  ))}
                </ul>
              </section>
            ),
          )}

          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 pt-2 text-[12.5px] text-dim">
            <span>
              <Key>j</Key> <Key>k</Key> move · <Key>Enter</Key> open · <Key>x</Key> select · <Key>d</Key> done · <Key>/</Key> filter · <Key>q</Key> add · <Key>Esc</Key> close
            </span>
            {view === "open" ? (
              <span className="ml-auto">
                Too much?{" "}
                <button
                  className="underline decoration-dim hover:text-txt"
                  onClick={() =>
                    act(async () => {
                      const r = await resetVisibilityFn({ data: {} });
                      setNotice(r.moved === 0 ? "Nothing past due to clear." : `Moved ${r.moved} past due to Later. Nothing was deleted.`);
                    })
                  }
                >
                  Move everything past due to Later
                </button>
              </span>
            ) : null}
          </div>
        </div>

        <aside className="xl:sticky xl:top-4">
          {selected ? (
            <TaskPanel key={selected.id + selected.updated_at} node={selected} parents={parents} entities={entities} today={today} act={act} onClose={() => open(null)} />
          ) : (
            <Panel title="Task">
              <Empty>Pick a task, or move with j and k and press Enter. Select several with x, or by clicking their edge, to change them together.</Empty>
            </Panel>
          )}
        </aside>
      </div>
    </div>
  );
}

const byTypeThenTitle = (a: GraphNode, b: GraphNode) => {
  const rank = { goal: 0, project: 1, habit: 2, task: 3, metric: 4 };
  return rank[a.type] - rank[b.type] || a.title.localeCompare(b.title);
};

function inView(view: View, n: GraphNode): boolean {
  if (view === "open") return n.status === "active" && n.assignee !== "agent";
  if (view === "waiting") return n.status === "waiting" && n.assignee !== "agent";
  if (view === "agent") return (n.status === "active" || n.status === "waiting") && n.assignee === "agent";
  if (view === "later") return n.status === "later" || n.status === "paused";
  return n.status === "done";
}

function dueMatches(f: NonNullable<Filters["due"]>, due: string | null, today: string): boolean {
  if (f === "none") return due === null;
  if (due === null) return false;
  if (f === "overdue") return due < today;
  if (f === "today") return due === today;
  if (f === "week") return due >= today && due <= addDays(today, 6);
  return true;
}

function fromMatches(f: NonNullable<Filters["from"]>, n: GraphNode): boolean {
  if (f === "google") return n.google_task_id !== null;
  if (f === "agent") return n.via === "agent";
  return n.via === "ui";
}

function sorter(sort: NonNullable<Filters["sort"]>) {
  return (a: GraphNode, b: GraphNode) => {
    if (sort === "created") return b.created_at.localeCompare(a.created_at);
    if (sort === "updated") return b.updated_at.localeCompare(a.updated_at);
    if (sort === "title") return a.title.localeCompare(b.title);
    return (a.due_local ?? "9999").localeCompare(b.due_local ?? "9999") || a.title.localeCompare(b.title);
  };
}

function grouped(rows: GraphNode[], by: NonNullable<Filters["group"]>, today: string, view: View): Group[] {
  if (by === "none") return [{ key: "all", title: "", rows: view === "done" ? rows.slice(0, 100) : rows }];
  if (by === "status") {
    const label: Record<string, string> = { active: "Open", waiting: "Waiting", later: "Later", paused: "Later", done: "Done" };
    return bucket(rows, (n) => label[n.status] ?? n.status, ["Open", "Waiting", "Later", "Done"]);
  }
  if (by === "for") {
    const groups = bucket(rows, (n) => n.parents[0]?.title ?? "Not under anything");
    return [...groups.filter((g) => g.key !== "Not under anything"), ...groups.filter((g) => g.key === "Not under anything")];
  }
  const week = addDays(today, 6);
  const flagged = rows.filter((n) => n.flagged_at);
  const rest = rows.filter((n) => !n.flagged_at);
  return [
    { key: "flagged", title: "Needs you", rows: flagged },
    { key: "past", title: "Past due", rows: rest.filter((n) => n.due_local && n.due_local < today) },
    { key: "today", title: "Today", rows: rest.filter((n) => n.due_local === today) },
    { key: "week", title: "This week", rows: rest.filter((n) => n.due_local && n.due_local > today && n.due_local <= week) },
    { key: "later", title: "Later on", rows: rest.filter((n) => n.due_local && n.due_local > week) },
    { key: "none", title: "No date", rows: rest.filter((n) => !n.due_local) },
  ];
}

function bucket(rows: GraphNode[], key: (n: GraphNode) => string, order: string[] = []): Group[] {
  const m = new Map<string, GraphNode[]>(order.map((k) => [k, []]));
  for (const n of rows) m.set(key(n), [...(m.get(key(n)) ?? []), n]);
  return [...m.entries()].map(([k, rows]) => ({ key: k, title: k, rows }));
}

/** A filter or display choice: a quiet button that opens a list; the chosen value shows on the button. */
function Choice({ label, value, options, onChange, always = false }: {
  label: string;
  value: string | undefined;
  options: { id: string; label: string; hint?: string }[];
  onChange: (v: string | undefined) => void;
  always?: boolean;
}) {
  const current = options.find((o) => o.id === value);
  return (
    <label className={`relative inline-flex items-center border text-[13.5px] ${current && !always ? "border-or/70 text-txt" : "border-edge text-mute"} hover:border-or/70`}>
      <span className="pointer-events-none flex items-center gap-1.5 py-1 pr-6 pl-3 whitespace-nowrap">
        {label}
        {current ? <span className="max-w-[200px] truncate text-txt">{current.label}</span> : null}
      </span>
      <select
        className="absolute inset-0 cursor-pointer opacity-0"
        aria-label={label}
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value || undefined)}
      >
        {always ? null : <option value="">Any</option>}
        {options.map((o) => (
          <option key={o.id} value={o.id}>
            {o.hint ? `${o.label} (${o.hint})` : o.label}
          </option>
        ))}
      </select>
      <svg className="pointer-events-none absolute right-2 text-dim" width="8" height="5" viewBox="0 0 8 5" aria-hidden="true">
        <path d="M0 0h8L4 5z" fill="currentColor" />
      </svg>
    </label>
  );
}

function TaskRow({ node: n, today, showParent, open, cursor, picked, onOpen, onPick, act }: {
  node: GraphNode; today: string; showParent: boolean; open: boolean; cursor: boolean; picked: boolean;
  onOpen: () => void; onPick: () => void; act: Act;
}) {
  const done = n.status === "done";
  const past = !done && n.due_local !== null && n.due_local < today;
  return (
    <li
      id={`task-${n.id}`}
      className={`group relative flex items-center gap-3 border-b border-line py-[7px] pr-2 pl-3 ${open || picked ? "row-sel" : "hover:bg-raised"}`}
    >
      <button
        className={`absolute inset-y-0 left-0 w-[5px] ${picked ? "bg-or" : cursor ? "bg-or/60" : "group-hover:bg-edge"}`}
        onClick={onPick}
        aria-label={picked ? `Unselect ${n.title}` : `Select ${n.title}`}
        aria-pressed={picked}
      />
      <TaskCheck id={n.id} title={n.title} done={done} google={n.google_task_id !== null} act={act} className="size-[15px]" />
      <button
        className="flex min-w-0 flex-1 items-center gap-2 text-left"
        onClick={(e) => (e.shiftKey || e.metaKey || e.ctrlKey ? onPick() : onOpen())}
        aria-expanded={open}
      >
        {n.flagged_at ? <Star size={9} className="text-or" /> : null}
        <span className={`truncate ${done ? "text-dim line-through" : ""}`} title={n.flag_note ?? undefined}>
          {n.title}
        </span>
      </button>
      <span className="flex shrink-0 items-center gap-2 text-[12.5px]">
        {n.status === "waiting" ? <span className="text-mute">waiting on {n.waiting_on ?? "someone"}</span> : null}
        {n.assignee === "agent" ? <span className="text-cy/85">agent</span> : null}
        {showParent && n.parents[0] ? <span className="chip max-w-[190px] truncate">{n.parents[0].title}</span> : null}
        {n.google_task_id ? <span className="text-dim" title="From Google Tasks">G</span> : null}
        <span className={`w-[68px] text-right ${n.due_local === today && !done ? "text-or" : past ? "text-txt" : "text-mute"}`}>
          {n.due_local ? dueLabel(n.due_local, today) : ""}
        </span>
      </span>
    </li>
  );
}

const QUICK_DATES = (today: string) => [
  { label: "Today", ymd: today },
  { label: "Tomorrow", ymd: addDays(today, 1) },
  { label: "Next week", ymd: addDays(today, ((8 - new Date(`${today}T12:00:00Z`).getUTCDay()) % 7) || 7) },
];

type BulkAction =
  | { op: "done" } | { op: "due"; due: string | null } | { op: "later"; until: string | null } | { op: "active" }
  | { op: "assign"; assignee: "owner" | "agent" } | { op: "parent"; parentId: string | null } | { op: "delete" };

function BulkBar({ ids, parents, today, act, onClear }: { ids: string[]; parents: GraphNode[]; today: string; act: Act; onClear: () => void }) {
  const run = (action: BulkAction, said: string) =>
    act(async () => {
      const r = await bulkTasksFn({ data: { ids, action } });
      if (r.skipped) throw new Error(`${said}: ${r.changed}. ${r.skipped} skipped (Google Tasks owns those).`);
    }, `${said}: ${ids.length}.`).then(action.op === "delete" || action.op === "done" ? onClear : () => undefined);
  const dates = QUICK_DATES(today);
  return (
    <div className="sticky top-0 z-20 flex flex-wrap items-center gap-2 border border-or/50 bg-panel px-3 py-2 text-[13.5px]">
      <span className="mr-1 font-medium">{ids.length} selected</span>
      <button className="btn btn-sm" onClick={() => run({ op: "done" }, "Done")}>Done</button>
      <Choice label="Due" value={undefined} always onChange={(v) => v && run({ op: "due", due: v === "none" ? null : v }, "Rescheduled")} options={[...dates.map((d) => ({ id: d.ymd, label: d.label })), { id: "none", label: "No date" }]} />
      <Choice label="Later" value={undefined} always onChange={(v) => v && run({ op: "later", until: v === "none" ? null : v }, "Moved to Later")} options={[...dates.slice(1).map((d) => ({ id: d.ymd, label: `until ${d.label.toLowerCase()}` })), { id: "none", label: "until I bring it back" }]} />
      <Choice label="For" value={undefined} always onChange={(v) => v && run({ op: "parent", parentId: v === "none" ? null : v }, "Moved")} options={[...parents.map((p) => ({ id: p.id, label: p.title, hint: p.type })), { id: "none", label: "Nothing" }]} />
      <button className="btn btn-sm" onClick={() => run({ op: "assign", assignee: "agent" }, "Given to the agent")}>Give to the agent</button>
      <button className="btn btn-sm" onClick={() => run({ op: "assign", assignee: "owner" }, "Taken back")}>Take back</button>
      <button className="btn btn-sm btn-danger" onClick={() => confirm(`Delete ${ids.length} tasks?`) && run({ op: "delete" }, "Deleted")}>Delete</button>
      <button className="ml-auto text-[13px] text-mute underline decoration-dim hover:text-txt" onClick={onClear}>Clear</button>
    </div>
  );
}

/** Who made a task, in words. */
function madeBy(n: GraphNode): string {
  if (n.google_task_id) return "Imported from Google Tasks";
  if (n.via === "agent") return n.source_ref?.startsWith("claude:") ? "Added by the agent in an unattended run" : "Added by the agent in a T3 thread";
  if (n.via === "ui") return "Added by you";
  return "Added by LoR";
}

function TaskPanel({ node: n, parents, entities, today, act, onClose }: {
  node: GraphNode; parents: GraphNode[]; entities: { id: string; name: string }[]; today: string; act: Act; onClose: () => void;
}) {
  const google = n.google_task_id !== null;
  const done = n.status === "done";
  const [title, setTitle] = useState(n.title);
  const [notes, setNotes] = useState(n.description ?? "");
  const save = (patch: Record<string, unknown>, said?: string) => act(() => saveIntentFn({ data: { id: n.id, type: "task", ...patch } }), said);
  const later = n.status === "later" || n.status === "paused";

  return (
    <Panel title="Task" aside={<button className="btn btn-sm" onClick={onClose}>Close</button>}>
      <div className="mt-1 flex items-start gap-3">
        <TaskCheck id={n.id} title={n.title} done={done} google={google} act={act} className="mt-[9px] size-[16px]" />
        <textarea
          className="min-h-0 w-full resize-none border border-transparent bg-transparent px-1 py-1 text-[18px] leading-snug font-medium text-txt hover:border-edge focus:border-or/70 focus:outline-none disabled:hover:border-transparent"
          rows={Math.max(1, Math.ceil(title.length / 34))}
          value={title}
          disabled={google}
          aria-label="Title"
          onChange={(e) => setTitle(e.target.value.replace(/\n/g, ""))}
          onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), e.currentTarget.blur())}
          onBlur={() => title.trim() && title !== n.title && void save({ title: title.trim() })}
        />
      </div>

      {n.flagged_at ? (
        <div className="mt-3 border-l-2 border-or bg-or/5 px-3 py-2 text-[14px]">
          <p className="mb-1 flex items-center gap-1.5 text-[12.5px] text-or">
            <Star size={9} /> The agent flagged this for you <span className="text-dim">· {ago(n.flagged_at)}</span>
          </p>
          {n.flag_note ? <p className="text-mute">{n.flag_note}</p> : null}
          <button className="btn btn-sm mt-2" onClick={() => act(() => unflagFn({ data: { id: n.id } }), "Marked seen.")}>
            Seen
          </button>
        </div>
      ) : null}

      <dl className="mt-4 grid grid-cols-[96px_1fr] items-center gap-x-3 gap-y-2 text-[14px]">
        <Prop label="Status">
          {done ? (
            <span className="text-mute">Done</span>
          ) : (
            <select
              className="input py-1"
              value={later ? "later" : n.status}
              onChange={(e) => save({ status: e.target.value })}
            >
              <option value="active">Open</option>
              <option value="waiting">Waiting</option>
              <option value="later">Later</option>
            </select>
          )}
        </Prop>
        {later ? (
          <Prop label="Back on">
            <input
              className="input py-1"
              type="date"
              value={n.snoozed_until ? n.snoozed_until.slice(0, 10) : ""}
              onChange={(e) => act(() => bulkTasksFn({ data: { ids: [n.id], action: { op: "later", until: e.target.value || null } } }))}
            />
          </Prop>
        ) : null}
        <Prop label="Due">
          {google ? (
            <span className="text-mute">{n.due_local ? longDate(n.due_local) : "No date"} · set in Google</span>
          ) : (
            <span className="flex flex-wrap items-center gap-1.5">
              <input className="input w-[150px] py-1" type="date" value={n.due_local ?? ""} onChange={(e) => save({ dueAt: e.target.value || null })} aria-label="Due" />
              {QUICK_DATES(today).map((d) => (
                <button key={d.label} className="btn btn-sm text-mute" onClick={() => save({ dueAt: d.ymd })}>
                  {d.label}
                </button>
              ))}
            </span>
          )}
        </Prop>
        <Prop label="For">
          <select
            className="input py-1"
            value={n.parents[0]?.id ?? ""}
            onChange={(e) => act(() => bulkTasksFn({ data: { ids: [n.id], action: { op: "parent", parentId: e.target.value || null } } }))}
          >
            <option value="">Nothing</option>
            {parents.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title} ({p.type})
              </option>
            ))}
          </select>
          {n.parents.length > 1 ? <span className="mt-1 block text-[12.5px] text-dim">Also: {n.parents.slice(1).map((p) => p.title).join(", ")}</span> : null}
        </Prop>
        <Prop label="Whose">
          <span className="flex border border-edge">
            {(["owner", "agent"] as const).map((a) => (
              <button
                key={a}
                className={`flex-1 border-l border-edge px-3 py-1 text-[13.5px] first:border-l-0 ${n.assignee === a ? "row-sel text-txt" : "text-mute hover:text-txt"}`}
                onClick={() => n.assignee !== a && act(() => bulkTasksFn({ data: { ids: [n.id], action: { op: "assign", assignee: a } } }))}
              >
                {a === "owner" ? "Mine" : "The agent's"}
              </button>
            ))}
          </span>
        </Prop>
        <Prop label="Waiting on">
          <select
            className="input py-1"
            value={n.waiting_on_entity_id ?? ""}
            onChange={(e) =>
              save({
                waitingOnEntityId: e.target.value || null,
                status: e.target.value ? "waiting" : n.status === "waiting" ? "active" : n.status,
              })
            }
          >
            <option value="">Nobody</option>
            {entities.map((e) => (
              <option key={e.id} value={e.id}>
                {e.name}
              </option>
            ))}
          </select>
        </Prop>
      </dl>

      <label className="mt-4 block text-[13px] text-mute">
        <span className="mb-1 block">Notes{google ? " · from Google Tasks" : ""}</span>
        <textarea
          className="input leading-relaxed"
          rows={Math.min(14, Math.max(4, notes.split("\n").length + Math.ceil(notes.length / 60)))}
          value={notes}
          disabled={google}
          placeholder="Anything worth remembering about it"
          onChange={(e) => setNotes(e.target.value)}
          onBlur={() => notes !== (n.description ?? "") && void save({ description: notes || null })}
        />
      </label>

      <ul className="mt-4 space-y-0.5 border-t border-line pt-3 text-[13px] text-dim">
        <li>
          {madeBy(n)} · {ago(n.created_at)}
        </li>
        {n.updated_at !== n.created_at ? <li>Last changed {ago(n.updated_at)}</li> : null}
        {n.status === "waiting" && n.waiting_since ? <li>Waiting since {ago(n.waiting_since)}</li> : null}
        {n.thread_ref ? <li>Worked on in thread {n.thread_ref}</li> : null}
      </ul>

      <div className="mt-4 flex flex-wrap gap-2">
        {done && !google ? (
          <button className="btn" onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: false } }), "Reopened.")}>
            Reopen
          </button>
        ) : null}
        <button
          className="btn btn-danger ml-auto"
          disabled={google}
          title={google ? "Lives in Google Tasks: delete it there" : undefined}
          onClick={() =>
            confirm(`Delete “${n.title}”?`) &&
            act(async () => {
              const r = await deleteIntentFn({ data: { id: n.id } });
              onClose();
              return r;
            }, `Deleted “${n.title}”.`)
          }
        >
          Delete
        </button>
      </div>
    </Panel>
  );
}

function Prop({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="contents">
      <dt className="cond text-[12.5px] font-medium text-dim">{label}</dt>
      <dd className="min-w-0">{children}</dd>
    </div>
  );
}
