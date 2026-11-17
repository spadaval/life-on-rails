// Notes: LoR's pages, newest first, and one page to read. No direction changes how Notes is
// organised (the brief), so this is the plain version in this direction's look.
import { useStore } from "../../../data/store.js";
import { href } from "../lib/nav.js";
import type { Route } from "../../../shell/nav.js";
import { Icon, Prose } from "../components/bits.js";
import { shortDate, ymd } from "../lib/time.js";

export function Notes({ route }: { route: Route }) {
  const s = useStore();
  const page = route.params.slug ? s.pages.find((p) => p.slug === route.params.slug) : undefined;
  if (page) {
    return (
      <div className="tl-page tl-reading">
        <a className="tl-back" href={href("/notes")}>
          <Icon name="back" size={16} /> Notes
        </a>
        <header className="tl-pagehead">
          <div>
            <p className="tl-kicker">{page.slug}</p>
            <h1>{page.title}</h1>
          </div>
        </header>
        <Prose text={page.body} className="tl-prose tl-card" />
      </div>
    );
  }
  const groups = new Map<string, typeof s.pages>();
  for (const p of s.pages) {
    const k = p.slug.includes("/") ? p.slug.split("/")[0]! : "pages";
    groups.set(k, [...(groups.get(k) ?? []), p]);
  }
  return (
    <div className="tl-page">
      <header className="tl-pagehead">
        <div>
          <h1>Notes</h1>
          <p className="tl-muted">{s.pages.length} pages</p>
        </div>
      </header>
      {[...groups].map(([k, ps]) => (
        <section key={k} className="tl-group">
          <h2 className="tl-section-title">{k}</h2>
          <ul className="tl-rows">
            {ps.map((p) => (
              <li key={p.slug} className="tl-row">
                <a className="tl-row-main" href={href("/notes", { slug: p.slug })}>
                  <span className="tl-row-title">{p.title}</span>
                  <span className="tl-row-meta">Updated {shortDate(ymd(p.updated_at))}</span>
                </a>
                <Icon name="chevron" size={16} />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
