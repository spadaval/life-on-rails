// The bar's other pages, for managing rather than working: All tasks (grouped by the context each
// lives in), Goals, People and Notes. Opening a goal or a person turns the page into its context.
import { useState } from "react";
import { isOpen, PEOPLE_FILTERS, peopleIn } from "../../../data/derive.js";
import { dayAfter, today, useNow, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import type { Route } from "../../../shell/nav.js";
import { Icon, kindIcon, Prose } from "../components/bits.js";
import { Layer } from "../components/Layer.js";
import { TaskRow } from "../components/TaskRow.js";
import { type Ctx, ctxPath, homeOf, LOOSE, members, sameCtx } from "../lib/model.js";
import { href, navigate, setParams } from "../lib/nav.js";
import { ago } from "../lib/time.js";
import { usePhone } from "../lib/ui.js";
import { WeekBar } from "./ContextView.js";

const FILTERS = [
  { id: "open", name: "Open" },
  { id: "due", name: "Due this week" },
  { id: "waiting", name: "Waiting" },
  { id: "agent", name: "The agent’s" },
  { id: "later", name: "Later" },
  { id: "done", name: "Done" },
] as const;

export function Tasks({ route }: { route: Route }) {
  const s = useStore();
  const p = route.params;
  const f = FILTERS.find((x) => x.id === p.show)?.id ?? "open";
  const week = dayAfter(today(), 7);
  const keep = (n: Node) => {
    if (n.type !== "task") return false;
    switch (f) {
      case "due": return isOpen(n) && !!n.due_local && n.due_local <= week;
      case "waiting": return n.status === "waiting";
      case "agent": return isOpen(n) && n.assignee === "agent";
      case "later": return n.status === "later";
      case "done": return n.status === "done";
      default: return isOpen(n);
    }
  };
  const shown = s.nodes.filter(keep);
  const groups = new Map<string, { ctx: Ctx; items: Node[] }>();
  for (const n of shown) {
    const c = homeOf(s, n);
    const g = groups.get(c.key) ?? { ctx: c, items: [] };
    g.items.push(n);
    groups.set(c.key, g);
  }
  const ordered = [...groups.values()].sort((a, b) => Number(a.ctx.kind === "loose") - Number(b.ctx.kind === "loose") || b.items.length - a.items.length || a.ctx.title.localeCompare(b.ctx.title));
  for (const g of ordered) g.items.sort((a, b) => (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title));
  return (
    <div className="cx-page">
      <header className="cx-page-head">
        <div>
          <h1 className="cx-page-title">All tasks</h1>
          <p className="cx-page-sub">{shown.length} {f === "open" ? "open" : FILTERS.find((x) => x.id === f)!.name.toLowerCase()}, by the context each lives in</p>
        </div>
        <div className="cx-seg" role="group" aria-label="Show">
          {FILTERS.map((x) => (
            <button key={x.id} type="button" aria-pressed={f === x.id} onClick={() => setParams({ show: x.id === "open" ? undefined : x.id, id: undefined })}>
              {x.name}
            </button>
          ))}
        </div>
      </header>
      <div className="cx-groups">
        {ordered.map((g) => (
          <section key={g.ctx.key} className="cx-group">
            <header className="cx-group-head">
              <a href={href(ctxPath(g.ctx).path, ctxPath(g.ctx).params)} className="cx-group-link">
                <Icon name={kindIcon(g.ctx.kind)} size={15} />
                <span>{g.ctx.title}</span>
                <Icon name="chevron" size={14} />
              </a>
              <span className="cx-count">{g.items.length}</span>
            </header>
            <ul className="cx-tasks">
              {g.items.map((n) => (
                <TaskRow key={n.id} node={n} open={p.id === n.title} param="id" />
              ))}
            </ul>
          </section>
        ))}
        {!ordered.length ? <p className="cx-empty-line">Nothing here.</p> : null}
      </div>
    </div>
  );
}

export function GoalsIndex() {
  const s = useStore();
  const goals = s.nodes.filter((n) => n.type === "goal" && n.status === "active").sort((a, b) => (b.week_hours ?? 0) - (a.week_hours ?? 0) || a.title.localeCompare(b.title));
  return (
    <div className="cx-page">
      <header className="cx-page-head">
        <div>
          <h1 className="cx-page-title">Goals</h1>
          <p className="cx-page-sub">Each goal opens as its own context: its projects, tasks, people and time.</p>
        </div>
      </header>
      <ul className="cx-cards">
        {goals.map((g) => {
          const m = members(s, { kind: "goal", key: g.id, title: g.title, linkId: g.title, node: g });
          const open = m.filter((n) => n.type === "task" && isOpen(n)).length;
          const projects = m.filter((n) => n.type === "project" && n.status === "active");
          return (
            <li key={g.id}>
              <a className="cx-goalcard" href={href("/goals", { id: g.title })}>
                <span className="cx-goalcard-title">{g.title}</span>
                <WeekBar goal={g.title} hrs={g.week_hours ?? 0} target={g.weekly_hour_target} self />
                <span className="cx-goalcard-meta">
                  {projects.length ? `${projects.map((x) => x.title).join(", ")} · ` : ""}
                  {open} open {open === 1 ? "task" : "tasks"}
                </span>
              </a>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export function PeopleIndex({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(60_000);
  const filter = route.params.list ?? "important";
  const [q, setQ] = useState("");
  const list = peopleIn(s, filter, q);
  const NAMES: Record<string, string> = { important: "Important", all: "Everyone", "keep-in-touch": "Keep in touch", overdue: "Overdue", birthdays: "Birthdays", groups: "Groups" };
  return (
    <div className="cx-page">
      <header className="cx-page-head">
        <div>
          <h1 className="cx-page-title">People</h1>
          <p className="cx-page-sub">Each person opens as a context: what you said lately, what names them, their page.</p>
        </div>
        <label className="cx-field cx-field-search cx-field-narrow">
          <Icon name="search" size={16} />
          <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Filter by name" aria-label="Filter people by name" />
        </label>
      </header>
      <div className="cx-seg" role="group" aria-label="List">
        {PEOPLE_FILTERS.map((x) => (
          <button key={x} type="button" aria-pressed={filter === x} onClick={() => setParams({ list: x === "important" ? undefined : x })}>
            {NAMES[x]}
          </button>
        ))}
      </div>
      <ul className="cx-plist">
        {list.map((p) => (
          <li key={p.id}>
            <a className="cx-prow" href={href("/people", { id: p.name })}>
              <span className="cx-avatar" data-priority={p.priority || undefined} aria-hidden="true">
                {p.name.split(/\s+/).slice(0, 2).map((w) => w[0]).join("").toUpperCase()}
              </span>
              <span className="cx-prow-text">
                <span className="cx-prow-name">{p.name}</span>
                <span className="cx-prow-meta">
                  {p.last.at ? `In touch ${ago(Date.parse(p.last.at), now)}` : "Not in touch yet"}
                  {p.keep_in_touch_days ? ` · every ${p.keep_in_touch_days} days` : ""}
                </span>
              </span>
              {p.overdue_by !== null && p.overdue_by > 0 ? <span className="cx-chip" data-late>{p.overdue_by} days over</span> : null}
              {p.birthday_in !== null && p.birthday_in <= 30 ? <span className="cx-chip cx-chip-amber">Birthday in {p.birthday_in} days</span> : null}
              <Icon name="chevron" size={16} />
            </a>
          </li>
        ))}
        {!list.length ? <li className="cx-empty-line">Nobody on this list.</li> : null}
      </ul>
    </div>
  );
}

export function Notes({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(60_000);
  const sel = s.pages.find((p) => p.slug === route.params.id) ?? null;
  const groups = new Map<string, typeof s.pages>();
  for (const p of s.pages) {
    const g = p.slug.includes("/") ? p.slug.split("/")[0]! : "pages";
    groups.set(g, [...(groups.get(g) ?? []), p]);
  }
  return (
    <div className="cx-page cx-notes" data-open={sel ? true : undefined}>
      <nav className="cx-notes-list" aria-label="Pages">
        <h1 className="cx-page-title">Notes</h1>
        {[...groups.entries()].map(([g, pages]) => (
          <section key={g}>
            <h2 className="cx-label">{g}</h2>
            <ul>
              {pages.map((p) => (
                <li key={p.slug}>
                  <a href={href("/notes", { id: p.slug })} aria-current={sel?.slug === p.slug ? "page" : undefined}>
                    {p.title}
                  </a>
                </li>
              ))}
            </ul>
          </section>
        ))}
      </nav>
      {sel ? (
        <article className="cx-card cx-reader">
          <header className="cx-reader-head">
            <p className="cx-label">{sel.slug} · edited {ago(Date.parse(sel.updated_at), now)}</p>
            <h2 className="cx-page-title">{sel.title}</h2>
          </header>
          <Prose body={sel.body} onLink={(slug) => navigate("/notes", { id: slug })} />
        </article>
      ) : (
        <p className="cx-empty-line cx-reader-empty">Pick a page.</p>
      )}
    </div>
  );
}

/** A context's page, in the right half beside it (a sheet at phone width). */
export function PageReader({ slug, here }: { slug: string; here: Ctx }) {
  const s = useStore();
  const now = useNow(60_000);
  const phone = usePhone();
  const pg = s.pages.find((p) => p.slug === slug);
  const close = () => setParams({ page: undefined });
  if (!pg) return null;
  const onLink = (to: string) => {
    if (to.startsWith("people/")) {
      const person = s.people.find((p) => p.page === to);
      if (person) return navigate("/people", { id: person.name });
    }
    if (s.pages.some((p) => p.slug === to)) setParams({ page: to });
  };
  const body = (
    <>
      <p className="cx-label">{pg.slug} · edited {ago(Date.parse(pg.updated_at), now)}{sameCtx(here, LOOSE) ? "" : ` · from ${here.title}`}</p>
      <Prose body={pg.body} onLink={onLink} />
      <a className="cx-btn cx-btn-sm cx-btn-ghost" href={href("/notes", { id: pg.slug })}>
        <Icon name="notes" size={14} /> Open in Notes
      </a>
    </>
  );
  if (phone) {
    return (
      <Layer label={pg.title} title={<span className="cx-float-name">{pg.title}</span>} onClose={close}>
        {body}
      </Layer>
    );
  }
  return (
    <aside className="cx-card cx-reader cx-reader-side" aria-label={`Page: ${pg.title}`}>
      <header className="cx-reader-head">
        <h2 className="cx-page-title">{pg.title}</h2>
        <button type="button" className="cx-iconbtn" onClick={close} aria-label="Close the page">
          <Icon name="close" size={16} />
        </button>
      </header>
      {body}
    </aside>
  );
}

