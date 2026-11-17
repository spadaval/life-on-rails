// Tasks: for managing, not looking. The same rows as Today, grouped by when they are due, in the
// same single column; a task opens in place, so the list stays where it was.
import { useRef } from "react";
import { addDays } from "../../../../../../src/shared/localTime.js";
import { isOpen } from "../../../data/derive.js";
import { useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import { setParams, type Route } from "../../../shell/nav.js";
import { modeOf, qhref } from "../lib.js";
import { TaskRow } from "../rows.js";
import { cx } from "../ui.js";

const VIEWS = [
  ["mine", "Mine"],
  ["waiting", "Waiting"],
  ["agent", "The agent's"],
  ["done", "Done"],
] as const;
type View = (typeof VIEWS)[number][0];

export function Tasks({ route }: { route: Route }) {
  const s = useStore();
  const stale = modeOf(route.params) === "stale";
  const openTitle = route.params.id ?? null;
  const target = openTitle ? s.nodes.find((n) => n.type === "task" && n.title === openTitle) : undefined;
  // A link to a task opens the view it is in.
  const asked = (route.params.view as View | undefined) ?? null;
  // Fixed when the link changes, so ticking the open task does not move the page to Done.
  const linked = useRef<{ title: string | null; view: View } | null>(null);
  if (!linked.current || linked.current.title !== openTitle) linked.current = { title: openTitle, view: target ? viewOf(target) : "mine" };
  const view: View = asked ?? linked.current.view;
  const day = s.day.date;
  const tasks = s.nodes.filter((n) => n.type === "task");
  const by = (a: Node, b: Node) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title);

  let groups: [string, Node[]][];
  if (view === "mine") {
    const mine = tasks.filter((n) => n.status === "active" && n.assignee === "owner").sort(by);
    const week = addDays(day, 7);
    groups = [
      ["Before today", mine.filter((n) => n.due_local && n.due_local < day)],
      ["Today", mine.filter((n) => n.due_local === day)],
      ["Tomorrow", mine.filter((n) => n.due_local === addDays(day, 1))],
      ["This week", mine.filter((n) => n.due_local && n.due_local > addDays(day, 1) && n.due_local <= week)],
      ["Later", mine.filter((n) => n.due_local && n.due_local > week)],
      ["No date", mine.filter((n) => !n.due_local)],
    ];
  } else if (view === "waiting") {
    groups = [["Waiting on someone", tasks.filter((n) => n.status === "waiting").sort(by)]];
  } else if (view === "agent") {
    groups = [["The agent is doing", tasks.filter((n) => isOpen(n) && n.assignee === "agent").sort(by)]];
  } else {
    groups = [["Done", tasks.filter((n) => n.status === "done").sort((a, b) => (b.done_at ?? "").localeCompare(a.done_at ?? ""))]];
  }
  const count = (v: View) =>
    v === "mine"
      ? tasks.filter((n) => n.status === "active" && n.assignee === "owner").length
      : v === "waiting"
        ? tasks.filter((n) => n.status === "waiting").length
        : v === "agent"
          ? tasks.filter((n) => isOpen(n) && n.assignee === "agent").length
          : tasks.filter((n) => n.status === "done").length;

  return (
    <div className="q-col">
      <header className="q-page">
        <h1 className="q-page__title">Tasks</h1>
        <p className="q-page__sub">
          Press <kbd className="q-kbd">q</kbd> anywhere to add one.
        </p>
      </header>
      <nav className="q-tabs" aria-label="Which tasks">
        {VIEWS.map(([id, label]) => (
          <a key={id} className={cx("q-tab", view === id && "is-on")} aria-current={view === id ? "page" : undefined} href={qhref("/tasks", { view: id === "mine" ? undefined : id })}>
            {label} <span className="q-count">{count(id)}</span>
          </a>
        ))}
      </nav>
      {groups.every(([, list]) => list.length === 0) ? <p className="q-empty">Nothing here.</p> : null}
      {groups
        .filter(([, list]) => list.length)
        .map(([name, list]) => (
          <section key={name} className="q-section">
            <h2 className="q-label">
              {name} <span className="q-count">{list.length}</span>
            </h2>
            <ul className="q-list">
              {list.map((n) => (
                <TaskRow
                  key={n.id}
                  node={n}
                  agentRow={n.assignee === "agent" && n.status !== "done"}
                  showDone={view === "done"}
                  open={openTitle === n.title}
                  onOpen={(v) => setParams({ id: v ? n.title : undefined })}
                  stale={stale}
                />
              ))}
            </ul>
          </section>
        ))}
    </div>
  );
}

function viewOf(n: Node): View {
  if (n.status === "done") return "done";
  if (n.status === "waiting") return "waiting";
  if (n.assignee === "agent") return "agent";
  return "mine";
}
