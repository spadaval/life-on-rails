// Notes, read only. The real page (src/routes/notes.tsx) also edits, keeps history and draws the
// graph; Notes is not one of the screens the concepts are compared on, so the prototype only
// lists the pages and shows one, so that links from People and search land somewhere.
import { createFileRoute, Link } from "../router.js";
import { z } from "zod";
import { getState } from "../../../data/store.js";
import { Ago } from "../components/Ago.js";
import { Markdown } from "../components/Markdown.js";
import { Empty, PageHead, Panel } from "../components/ui.js";
import { GlobalActions } from "./__root.js";

export const Route = createFileRoute("/notes")({
  validateSearch: (search) => z.object({ slug: z.string().optional() }).parse(search),
  loaderDeps: ({ search }) => ({ slug: search.slug }),
  loader: ({ deps }) => {
    const pages = getState().pages;
    return { pages, page: deps.slug ? (pages.find((p) => p.slug === deps.slug) ?? null) : null };
  },
  component: Notes,
});

function Notes() {
  const { pages, page } = Route.useLoaderData();
  const search = Route.useSearch();
  return (
    <div className="space-y-5 pb-4">
      <PageHead title="Notes" sub={page ? page.slug : `${pages.length} pages`}>
        <GlobalActions />
      </PageHead>
      <div className="grid items-start gap-[22px] xl:grid-cols-[300px_minmax(0,1fr)]">
        <Panel title="Pages" aside={pages.length}>
          <ul className="text-[14px]">
            {pages.map((p) => (
              <li key={p.slug} className={`py-1 ${p.slug === page?.slug ? "text-or" : ""}`}>
                <Link to="/notes" search={{ slug: p.slug }} className="hover:text-or">
                  {p.title}
                </Link>
              </li>
            ))}
          </ul>
        </Panel>
        {page ? (
          <Panel title={page.title} aside={<span>read only in the prototype</span>} bodyClass="px-7 py-5">
            <Markdown source={page.body} />
            <p className="mt-4 text-[13px] text-dim">
              last written by {page.last_via} · <Ago iso={page.updated_at} />
            </p>
          </Panel>
        ) : (
          <Panel title={search.slug ? "No such page" : "Nothing open"}>
            <Empty>Pick a page.</Empty>
          </Panel>
        )}
      </div>
    </div>
  );
}
