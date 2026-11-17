// Notes: the pages, by folder, and the open page beside them.
import { useState } from "react";
import { useNow, useStore } from "../../../data/store.js";
import type { Route } from "../../../shell/nav.js";
import { ago, closeHere, openHere } from "../lib.js";
import { type Detail, ListHead, Panes } from "../panes.js";
import { cx, Group, Icon, Prose } from "../ui.js";

const OPEN = ["id"];

export function Notes({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(60_000);
  const p = route.params;
  const [q, setQ] = useState("");
  const page = p.id ? s.pages.find((x) => x.slug === p.id) : undefined;
  const shown = s.pages.filter((x) => !q.trim() || `${x.title} ${x.slug}`.toLowerCase().includes(q.trim().toLowerCase()));
  const folders = new Map<string, typeof shown>();
  for (const x of shown) {
    const f = x.slug.includes("/") ? x.slug.split("/")[0]! : "pages";
    folders.set(f, [...(folders.get(f) ?? []), x]);
  }
  const detail: Detail | null = page
    ? { key: `page-${page.slug}`, kicker: `Note · ${page.slug}`, title: page.title, close: () => closeHere(OPEN), body: <><p className="p-aside">Changed {ago(page.updated_at, now)} by {page.last_via === "agent" ? "the agent" : "you"}.</p><Prose source={page.body} /></> }
    : null;
  const head = (
    <ListHead title="Notes" sub={`${s.pages.length} pages`}>
      <input className="p-input p-list__filter" type="search" placeholder="Filter pages" aria-label="Filter pages" value={q} onChange={(e) => setQ(e.target.value)} />
    </ListHead>
  );
  const list = [...folders].map(([f, pages]) => (
    <Group key={f} label={f} count={pages.length}>
      <ul className="p-rows">
        {pages.map((x) => (
          <li key={x.slug} className={cx("p-row", page?.slug === x.slug && "is-sel")} data-sel={page?.slug === x.slug}>
            <span className="p-row__mark" aria-hidden="true">
              <Icon name="notes" />
            </span>
            <button type="button" className="p-row__open" aria-current={page?.slug === x.slug || undefined} onClick={() => openHere({ id: x.slug }, OPEN)}>
              <span className="p-row__title">{x.title}</span>
              <span className="p-row__meta">{ago(x.updated_at, now)}</span>
            </button>
            <Icon name="right" />
          </li>
        ))}
      </ul>
    </Group>
  ));
  const rest = (
    <div className="p-overview">
      <p className="p-kicker">Notes</p>
      <p className="p-overview__lead">The agent's memory and yours: people, places, topics, the briefs and the days.</p>
      <p className="p-overview__hint">Open a page to read it here; links inside it open their page in the same place.</p>
    </div>
  );
  return <Panes label="Notes" head={head} list={list} detail={detail} rest={rest} sheet={p.sheet} />;
}
