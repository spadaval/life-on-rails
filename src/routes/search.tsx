// Search everything: notes, people, goals and tasks, and the record of what happened.
import { useState } from "react";
import { Link, createFileRoute, useNavigate } from "@tanstack/react-router";
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

// An intent's snippet starts with its type, which also picks where it opens.
const KIND: Record<string, string> = { page: "note", entity: "person", obs: "record" };

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
                <span className="chip mr-2">{r.kind === "intent" ? r.snippet.split(" ")[0] : KIND[r.kind]}</span>
                {r.kind === "page" ? (
                  <Link to="/notes" search={{ slug: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                ) : r.kind === "entity" ? (
                  <Link to="/people" search={{ id: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                ) : r.kind === "obs" ? (
                  <Link to="/" search={{ obs: r.id }} className="link font-medium">
                    {r.title || "(untitled)"}
                  </Link>
                ) : r.snippet.startsWith("task") ? (
                  <Link to="/tasks" search={{ id: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                ) : (
                  <Link to="/goals" search={{ id: r.id }} className="link font-medium">
                    {r.title}
                  </Link>
                )}
                <p className="mt-0.5 line-clamp-2 text-[13.5px] text-mute">{r.snippet}</p>
              </li>
            ))}
          </ul>
        </Panel>
      ) : null}
    </div>
  );
}
