// Search everything: notes, people, goals and tasks, and the record of what happened.
import { useState } from "react";
import { Link, createFileRoute, useNavigate } from "../router.js";
import { z } from "zod";
import { Empty, PageHead, Panel } from "../components/ui.js";
import { searchFn } from "../server/functions/memory.js";
import { GlobalActions } from "./__root.js";

export const Route = createFileRoute("/search")({
  validateSearch: (search) => z.object({ q: z.string().optional() }).parse(search),
  loaderDeps: ({ search }) => ({ q: search.q ?? "" }),
  loader: ({ deps }) => searchFn({ data: { q: deps.q } }),
  component: SearchPage,
});

// What each kind of result is called. The prototype searches the snapshot (src/data/derive.ts), not
// messages, mail or the record of what happened.
const KIND: Record<string, string> = { page: "note", person: "person" };

function SearchPage() {
  const { results } = Route.useLoaderData();
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/search" });
  const [q, setQ] = useState(search.q ?? "");

  return (
    <div className="space-y-5 pb-4">
      <PageHead title="Search" sub={search.q ? `“${search.q}”` : undefined}>
        <GlobalActions />
      </PageHead>
      <form
        className="flex max-w-[640px] gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          void navigate({ search: { q: q.trim() || undefined } });
        }}
      >
        <input className="input" placeholder="Notes, people, tasks, messages, mail…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search" autoFocus />
        <button className="btn" type="submit">
          Search
        </button>
      </form>
      {search.q ? (
        <Panel title={results.length === 0 ? "Nothing found" : `${results.length} found`}>
          {results.length === 0 ? <Empty>Try fewer words, or another spelling.</Empty> : null}
          <ul className="grid gap-x-8 lg:grid-cols-2">
            {results.map((r) => (
              <li key={`${r.kind}:${r.id}`} className="border-t border-line py-2">
                <span className="chip mr-2">{KIND[r.kind] ?? r.kind}</span>
                {r.kind === "page" ? (
                  <Link to="/notes" search={{ slug: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                ) : r.kind === "person" ? (
                  <Link to="/people" search={{ id: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                ) : r.kind === "task" ? (
                  <Link to="/tasks" search={{ id: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                ) : (
                  <Link to="/goals" search={{ id: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                )}
                {r.detail ? <p className="mt-0.5 line-clamp-2 text-[13.5px] text-mute">{r.detail}</p> : null}
              </li>
            ))}
          </ul>
        </Panel>
      ) : null}
    </div>
  );
}
