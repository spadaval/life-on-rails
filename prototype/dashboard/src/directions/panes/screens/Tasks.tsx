// Tasks: every task, by when it is due, in a list pane with a filter; a task opens in the detail
// pane beside it, where its due date, what it counts toward and its notes change.
import { addDays } from "../../../../../../src/shared/localTime.js";
import { isOpen } from "../../../data/derive.js";
import { useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import type { Route } from "../../../shell/nav.js";
import { taskDetail } from "../details.js";
import { closeHere, flags, modeOf, openHere, phref, useLocal } from "../lib.js";
import { openField } from "../omnibox.js";
import { ListHead, Panes } from "../panes.js";
import { TaskRow } from "../rows.js";
import { Button, cx, Group, Icon } from "../ui.js";

const FILTERS = [
  ["open", "Open"],
  ["waiting", "Waiting"],
  ["agent", "The agent's"],
  ["later", "Later"],
  ["done", "Done"],
] as const;
type Filter = (typeof FILTERS)[number][0];
const OPEN = ["id"];

function inFilter(n: Node, f: Filter): boolean {
  if (n.type !== "task") return false;
  if (f === "open") return isOpen(n) && n.assignee === "owner" && n.status === "active";
  if (f === "waiting") return n.status === "waiting";
  if (f === "agent") return isOpen(n) && n.assignee === "agent";
  if (f === "later") return n.status === "later";
  return n.status === "done";
}

export function Tasks({ route }: { route: Route }) {
  const s = useStore();
  const l = useLocal();
  const p = route.params;
  const stale = modeOf(p) === "stale";
  const asked = p.id ? s.nodes.find((n) => n.title === p.id && n.type === "task") : undefined;
  const filter: Filter = (FILTERS.some(([f]) => f === p.filter) ? p.filter : asked && !inFilter(asked, "open") ? FILTERS.find(([f]) => inFilter(asked, f))?.[0] : "open") as Filter;
  const tasks = s.nodes.filter((n) => inFilter(n, filter)).sort((a, b) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title));
  const day = s.day.date;
  const close = () => closeHere(OPEN);
  const fl = flags(s, modeOf(p), l);
  const detail = asked ? taskDetail(asked, close, { flag: fl.find((f) => f.id === asked.id), stale }) : null;

  const groups: [string, Node[]][] =
    filter === "open"
      ? [
          ["Late", tasks.filter((n) => n.due_local && n.due_local < day)],
          ["Today", tasks.filter((n) => n.due_local === day)],
          ["This week", tasks.filter((n) => n.due_local && n.due_local > day && n.due_local <= addDays(day, 7))],
          ["Later on", tasks.filter((n) => n.due_local && n.due_local > addDays(day, 7))],
          ["No date", tasks.filter((n) => !n.due_local)],
        ]
      : [[FILTERS.find(([f]) => f === filter)![1], tasks]];

  const head = (
    <ListHead
      title="Tasks"
      sub={`${s.nodes.filter((n) => inFilter(n, "open")).length} open, ${s.nodes.filter((n) => inFilter(n, "waiting")).length} waiting on someone`}
      tools={
        <Button tone="quiet" size="sm" disabled={stale} onClick={() => openField("add")} title="Add a task (q)">
          <Icon name="plus" /> Add
        </Button>
      }
    >
      <nav className="p-filter" aria-label="Which tasks">
        {FILTERS.map(([f, name]) => (
          <a key={f} className={cx("p-filter__a", filter === f && "is-on")} aria-current={filter === f ? "true" : undefined} href={phref("/tasks", { filter: f === "open" ? undefined : f, id: p.id })}>
            {name}
            <span className="p-count">{s.nodes.filter((n) => inFilter(n, f)).length}</span>
          </a>
        ))}
      </nav>
    </ListHead>
  );

  const list = tasks.length ? (
    groups
      .filter(([, g]) => g.length)
      .map(([label, g]) => (
        <Group key={label} label={label} count={g.length}>
          <ul className="p-rows">
            {g.map((n) => (
              <TaskRow key={n.id} node={n} sel={asked?.id === n.id} onOpen={() => openHere({ id: n.title }, OPEN)} stale={stale} showDone={filter === "done"} />
            ))}
          </ul>
        </Group>
      ))
  ) : (
    <p className="p-empty">Nothing here.</p>
  );

  const rest = (
    <div className="p-overview">
      <p className="p-kicker">Tasks</p>
      <p className="p-overview__lead">Open a task to change when it is due, what it counts toward, and its notes.</p>
      <ul className="p-overview__facts">
        <li>
          <strong>{tasks.filter((n) => n.overdue).length}</strong> late
        </li>
        <li>
          <strong>{tasks.filter((n) => n.due_local === day).length}</strong> due today
        </li>
        <li>
          <strong>{tasks.filter((n) => n.due_local && n.due_local > day && n.due_local <= addDays(day, 7)).length}</strong> this week
        </li>
      </ul>
      <p className="p-overview__hint">
        Tick a task on its row; nothing opens. Press <kbd className="p-kbd">q</kbd> to add one from anywhere.
      </p>
    </div>
  );

  return <Panes label="Tasks" head={head} list={list} detail={detail} rest={rest} sheet={p.sheet} />;
}
