// Notes: LoR's pages by folder; a page opens in place to read. Not one of the brief's required
// screens, but it is in the top bar, so it is here in Queue's look.
import { useEffect, useRef } from "react";
import { useStore } from "../../../data/store.js";
import type { Page } from "../../../data/types.js";
import { setParams, type Route } from "../../../shell/nav.js";
import { revealOnOpen } from "../lib.js";
import { cx, Icon, Prose } from "../ui.js";

const FOLDERS: Record<string, string> = { briefs: "Briefs", days: "Days", goals: "Goals", people: "People", places: "Places", projects: "Projects", reading: "Reading", recipes: "Recipes", topics: "Topics" };

export function Notes({ route }: { route: Route }) {
  const s = useStore();
  const open = route.params.id ?? null;
  const groups = new Map<string, Page[]>();
  for (const p of [...s.pages].sort((a, b) => a.title.localeCompare(b.title))) {
    const f = p.slug.includes("/") ? p.slug.split("/")[0]! : "";
    groups.set(f, [...(groups.get(f) ?? []), p]);
  }
  return (
    <div className="q-col">
      <header className="q-page">
        <h1 className="q-page__title">Notes</h1>
        <p className="q-page__sub">{s.pages.length} pages. Search finds words inside them.</p>
      </header>
      {[...groups.entries()]
        .sort(([a], [b]) => (a === "" ? -1 : b === "" ? 1 : a.localeCompare(b)))
        .map(([f, pages]) => (
          <section key={f} className="q-section">
            <h2 className="q-label">{FOLDERS[f] ?? (f || "Top level")}</h2>
            <ul className="q-list">
              {pages.map((p) => (
                <NoteRow key={p.slug} p={p} open={open === p.slug} onOpen={(v) => setParams({ id: v ? p.slug : undefined })} />
              ))}
            </ul>
          </section>
        ))}
    </div>
  );
}

function NoteRow({ p, open, onOpen }: { p: Page; open: boolean; onOpen: (v: boolean) => void }) {
  const ref = useRef<HTMLLIElement>(null);
  useEffect(() => {
    if (open) revealOnOpen(ref.current);
  }, [open]);
  const first = p.body.split("\n").find((l) => l.trim() && !l.startsWith("#"))?.slice(0, 120) ?? "";
  return (
    <li ref={ref} className={cx("q-row", open && "is-open")}>
      <div className="q-row__head">
        <button type="button" className="q-row__title" aria-expanded={open} onClick={() => onOpen(!open)}>
          <span className="q-row__name">{p.title}</span>
          {!open && first ? <span className="q-row__meta">{first}</span> : null}
        </button>
        <button type="button" className="q-row__open" aria-label={open ? "Close" : "Open"} aria-expanded={open} onClick={() => onOpen(!open)}>
          <Icon name="chev" />
        </button>
      </div>
      {open ? (
        <div className="q-notebody">
          <Prose source={p.body} />
        </div>
      ) : null}
    </li>
  );
}
