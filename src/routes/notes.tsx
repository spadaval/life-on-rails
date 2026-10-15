// Notes: the wiki the agent and the owner both write. An index by folder, the
// reader with what links here, and the whole wiki as a star map.
import { useState } from "react";
import { Link, createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { Ago } from "../components/Ago.js";
import { Markdown } from "../components/Markdown.js";
import { StarMap } from "../components/StarMap.js";
import { Empty, Notice, PageHead, Panel, Star } from "../components/ui.js";
import { createPageFn, getNotesFn, revertPageFn, savePageFn, setInDigestFn } from "../server/functions/memory.js";
import { folderOf } from "../shared/wiki.js";
import { GlobalActions } from "./__root.js";

const Search = z.object({
  slug: z.string().optional(),
  version: z.coerce.number().int().optional(),
  view: z.enum(["graph"]).optional(),
});

export const Route = createFileRoute("/notes")({
  validateSearch: (search) => Search.parse(search),
  loaderDeps: ({ search }) => ({ slug: search.slug, version: search.version, graph: search.view === "graph" }),
  loader: ({ deps }) => getNotesFn({ data: deps }),
  component: Notes,
});

type Data = Awaited<ReturnType<typeof getNotesFn>>;
type Page = Data["pages"][number];
type Act = (fn: () => Promise<unknown>, done?: string) => Promise<boolean>;

/** Folders whose pages are dated: newest first, and folded away unless open. */
const DATED = new Set(["days"]);

function Notes() {
  const data = Route.useLoaderData();
  const search = Route.useSearch();
  const router = useRouter();
  const navigate = useNavigate({ from: "/notes" });
  const [notice, setNotice] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const act: Act = async (fn, done) => {
    let ok = true;
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      ok = false;
      setNotice(err instanceof Error ? err.message : String(err));
    }
    // Reload only after a success: a refused save must keep what was typed.
    if (ok) await router.invalidate();
    return ok;
  };
  const graph = search.view === "graph";

  return (
    <div className="space-y-5 pb-4">
      <PageHead title="Notes" sub={`${data.pages.length} pages`}>
        <GlobalActions />
      </PageHead>

      <div className="flex flex-wrap items-center gap-3">
        <nav className="flex border border-edge" aria-label="Show">
          <Link to="/notes" search={{ slug: search.slug }} className={`px-4 py-1 text-[14px] ${!graph ? "row-sel text-txt" : "text-mute hover:text-txt"}`}>
            Pages
          </Link>
          <Link to="/notes" search={{ slug: search.slug, view: "graph" }} className={`border-l border-edge px-4 py-1 text-[14px] ${graph ? "row-sel text-txt" : "text-mute hover:text-txt"}`}>
            Graph
          </Link>
        </nav>
        <button className="btn" onClick={() => setCreating((v) => !v)}>
          {creating ? "Close" : "New page"}
        </button>
      </div>

      {creating ? (
        <NewPage
          onCancel={() => setCreating(false)}
          onCreate={(input) =>
            act(() => createPageFn({ data: input })).then((ok) => {
              if (!ok) return;
              setCreating(false);
              void navigate({ search: { slug: input.slug } });
            })
          }
        />
      ) : null}

      <Notice text={notice} onClose={() => setNotice(null)} />

      {graph ? <GraphView data={data} selected={search.slug ?? null} /> : <PagesView data={data} act={act} />}
    </div>
  );
}

function GraphView({ data, selected }: { data: Data; selected: string | null }) {
  const navigate = useNavigate({ from: "/notes" });
  const page = data.pages.find((p) => p.slug === selected) ?? null;
  const linksIn = page ? data.pages.filter((p) => p.links.includes(page.slug)) : [];
  const linksOut = page ? page.links.map((l) => data.pages.find((p) => p.slug === l)!).filter(Boolean) : [];
  return (
    <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1fr)_360px]">
      <div className="panel h-[62vh] xl:h-[calc(100dvh-196px)] xl:min-h-[560px]">
        <div className="face">
          <StarMap
            pages={data.pages}
            layout={data.layout ?? {}}
            selected={selected}
            onSelect={(slug) => void navigate({ search: { view: "graph", slug: slug ?? undefined }, replace: true })}
            onOpen={(slug) => void navigate({ search: { slug } })}
          />
        </div>
      </div>
      <aside className="xl:sticky xl:top-4">
        {page ? (
          <Panel title={page.title} aside={<Link to="/notes" search={{ slug: page.slug }} className="btn btn-sm border-or text-or">Read</Link>}>
            <p className="text-[13px] text-dim">
              {page.slug} · last changed by {page.last_via} <Ago iso={page.updated_at} />
            </p>
            <LinkList title="Links here" pages={linksIn} graph />
            <LinkList title="Links to" pages={linksOut} graph />
          </Panel>
        ) : (
          <Panel title="Nothing selected">
            <Empty>Pick a page to light up what it links to and what links to it. Pick it again to read it. Drag to pan, scroll to zoom.</Empty>
          </Panel>
        )}
      </aside>
    </div>
  );
}

function LinkList({ title, pages, graph = false }: { title: string; pages: { slug: string; title: string }[]; graph?: boolean }) {
  return (
    <section className="mt-4">
      <h4 className="cond flex justify-between border-b border-line pb-1 text-[13px] font-semibold text-mute">
        {title}
        <span className="font-sans text-[12.5px] font-normal text-dim">{pages.length}</span>
      </h4>
      {pages.length === 0 ? <p className="py-1.5 text-[13.5px] text-dim">None.</p> : null}
      <ul className="mt-1">
        {pages.map((p) => (
          <li key={p.slug} className="py-1">
            <Link to="/notes" search={graph ? { view: "graph", slug: p.slug } : { slug: p.slug }} className="hover:text-or">
              {p.title}
            </Link>
            {folderOf(p.slug) ? <span className="ml-2 text-[12.5px] text-dim">{folderOf(p.slug)}</span> : null}
          </li>
        ))}
      </ul>
    </section>
  );
}

function PagesView({ data, act }: { data: Data; act: Act }) {
  const current = data.page?.view.slug ?? null;
  const [filter, setFilter] = useState("");
  const f = filter.trim().toLowerCase();
  const match = (p: Page) => !f || p.title.toLowerCase().includes(f) || p.slug.includes(f);
  const pinned = data.pages.filter((p) => p.in_digest && match(p));
  const folders = new Map<string, Page[]>();
  for (const p of data.pages.filter((p) => !p.in_digest && match(p))) {
    const k = folderOf(p.slug);
    folders.set(k, [...(folders.get(k) ?? []), p]);
  }
  const order = [...folders.keys()].sort((a, b) => (a === "" ? 1 : b === "" ? -1 : Number(DATED.has(a)) - Number(DATED.has(b)) || a.localeCompare(b)));
  const recent = [...data.pages].sort((a, b) => b.updated_at.localeCompare(a.updated_at)).slice(0, 12);

  const item = (p: Page, sub?: string) => (
    <li key={p.slug}>
      <Link to="/notes" search={{ slug: p.slug }} className={`flex items-baseline justify-between gap-3 px-2 py-1 ${current === p.slug ? "row-sel" : "hover:bg-raised"}`}>
        <span className="min-w-0 truncate">{p.title}</span>
        {sub ? <span className="shrink-0 text-[12.5px] text-dim">{sub}</span> : null}
      </Link>
    </li>
  );

  return (
    <div className="grid items-start gap-[22px] lg:grid-cols-[300px_minmax(0,1fr)]">
      <Panel className={`lg:sticky lg:top-4 ${data.page ? "hidden lg:block" : ""}`} bodyClass="max-h-[calc(100dvh-140px)] overflow-y-auto">
        <input className="input mb-3" placeholder="Filter pages" value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter pages" />
        {pinned.length > 0 ? (
          <section className="mb-3">
            <h3 className="cond mb-1 flex items-center gap-1.5 text-[12.5px] font-medium text-dim">
              <Star size={8} className="text-or" /> Loaded into every agent run
            </h3>
            <ul className="-mx-2">{pinned.map((p) => item(p))}</ul>
          </section>
        ) : null}
        {order.map((k) => {
          const list = folders.get(k)!;
          const dated = DATED.has(k);
          const sorted = dated ? [...list].sort((a, b) => b.slug.localeCompare(a.slug)) : list;
          const open = !!f || !dated || (current !== null && folderOf(current) === k);
          return (
            <details key={k} open={open} className="mb-2 group">
              <summary className="cond flex cursor-pointer list-none items-center justify-between py-1 text-[12.5px] font-medium text-dim hover:text-txt">
                <span>
                  <span className="mr-1.5 inline-block transition-transform group-open:rotate-90">›</span>
                  {k || "Other"}
                </span>
                <span className="font-sans font-normal">{list.length}</span>
              </summary>
              <ul className="-mx-2">{sorted.map((p) => item(p))}</ul>
            </details>
          );
        })}
        {pinned.length === 0 && order.length === 0 ? <Empty>No page matches.</Empty> : null}
      </Panel>

      {data.page ? (
        <PageView key={`${data.page.view.slug}@${data.page.view.version}`} page={data.page} pages={data.pages} act={act} />
      ) : (
        <Panel title="Recently changed">
          <ul>
            {recent.map((p) => (
              <li key={p.slug} className="flex flex-wrap items-baseline justify-between gap-2 border-t border-line py-2 first:border-t-0">
                <span>
                  <Link to="/notes" search={{ slug: p.slug }} className="link text-[15.5px] font-medium">
                    {p.title}
                  </Link>
                  <span className="ml-2 text-[12.5px] text-dim">{p.slug}</span>
                </span>
                <span className="text-[13px] text-mute">
                  {p.last_via} · <Ago iso={p.updated_at} />
                </span>
              </li>
            ))}
          </ul>
        </Panel>
      )}
    </div>
  );
}

function NewPage({ onCreate, onCancel }: { onCreate: (input: { slug: string; title: string; body: string }) => void; onCancel: () => void }) {
  const [slug, setSlug] = useState("topics/");
  const [title, setTitle] = useState("");
  const [touched, setTouched] = useState(false);
  const derived = (t: string) => `${slug.includes("/") ? slug.slice(0, slug.lastIndexOf("/") + 1) : ""}${t.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")}`;
  return (
    <form
      className="flex flex-wrap items-center gap-2 border border-edge bg-panel p-3"
      onSubmit={(e) => {
        e.preventDefault();
        if (title.trim()) onCreate({ slug: slug.trim(), title: title.trim(), body: `# ${title.trim()}\n` });
      }}
    >
      <input
        className="input min-w-[240px] flex-1"
        placeholder="Title"
        value={title}
        onChange={(e) => {
          setTitle(e.target.value);
          if (!touched) setSlug(derived(e.target.value));
        }}
        aria-label="Title"
        autoFocus
      />
      <input
        className="input w-[280px]"
        placeholder="slug, e.g. topics/home-network"
        value={slug}
        onChange={(e) => {
          setTouched(true);
          setSlug(e.target.value);
        }}
        aria-label="Slug"
      />
      <button className="btn border-or text-or" type="submit" disabled={!title.trim()}>
        Create
      </button>
      <button className="btn" type="button" onClick={onCancel}>
        Cancel
      </button>
    </form>
  );
}

function PageView({ page, pages, act }: { page: NonNullable<Data["page"]>; pages: Page[]; act: Act }) {
  const { view, history, diff, backlinks, missing } = page;
  const navigate = useNavigate({ from: "/notes" });
  const [editing, setEditing] = useState(false);
  const [body, setBody] = useState(view.body);
  const [showHistory, setShowHistory] = useState(view.version !== view.head_version);
  const [conflict, setConflict] = useState(false);
  const isHead = view.version === view.head_version;
  const linksOut = (pages.find((p) => p.slug === view.slug)?.links ?? []).map((l) => pages.find((p) => p.slug === l)!).filter(Boolean);

  const save = async (force = false) => {
    const ok = await act(() => savePageFn({ data: { slug: view.slug, base: view.head_version, body, force } }), "Saved.");
    if (ok) setEditing(false);
    else setConflict(true);
  };

  return (
    <div className="grid items-start gap-[22px] 2xl:grid-cols-[minmax(0,1fr)_300px]">
      <div className="min-w-0 space-y-[22px]">
        <Panel
          title={
            <Link to="/notes" className="text-mute hover:text-txt lg:hidden">
              All pages
            </Link>
          }
          bodyClass="px-7 py-5"
        >
          {!isHead ? (
            <div className="row-sel mb-4 flex flex-wrap items-center justify-between gap-2 px-3 py-2 text-[14px]">
              <span>You are looking at an old version.</span>
              <span className="flex gap-2">
                <button
                  className="btn btn-sm border-or text-or"
                  onClick={() =>
                    act(() => revertPageFn({ data: { slug: view.slug, version: view.version } }), `Reverted to version ${view.version}.`).then(() => navigate({ search: { slug: view.slug } }))
                  }
                >
                  Revert to this version
                </button>
                <Link to="/notes" search={{ slug: view.slug }} className="btn btn-sm">
                  Back to current
                </Link>
              </span>
            </div>
          ) : null}

          {editing ? (
            <div className="space-y-2">
              <textarea className="input min-h-[60vh] font-mono text-[13.5px] leading-relaxed" value={body} onChange={(e) => setBody(e.target.value)} aria-label="Page body" />
              <div className="flex flex-wrap items-center gap-2">
                <button className="btn border-or text-or" onClick={() => save()}>
                  Save
                </button>
                {conflict ? (
                  <button className="btn" onClick={() => save(true)}>
                    Save mine on top
                  </button>
                ) : null}
                <button
                  className="btn"
                  onClick={() => {
                    setBody(view.body);
                    setEditing(false);
                  }}
                >
                  Cancel
                </button>
              </div>
              <p className="text-[13px] text-mute">
                {conflict
                  ? "The page changed while you were editing. Your text is still here. “Save mine on top” makes it the newest version; the other change stays in the history."
                  : "Link another page with [[slug]] or [[slug|words]]. If the agent saves a change while you are editing, your save is refused and your text stays here."}
              </p>
            </div>
          ) : (
            <Markdown source={view.body} />
          )}

          <p className="mt-6 text-[13px] text-dim">
            {view.slug} · version {view.version} of {view.head_version} · last written by {view.via}
            {view.source_ref ? ` · thread ${view.source_ref.slice(0, 8)}` : ""} · <Ago iso={view.written_at} />
            {view.entity_id ? (
              <>
                {" · "}
                <Link to="/people" search={{ id: view.entity_id }} className="link">
                  their page in People
                </Link>
              </>
            ) : null}
          </p>
          {isHead && !editing ? (
            <div className="mt-2 flex flex-wrap items-center gap-2 border-t border-line pt-3">
              <button className="btn" onClick={() => setEditing(true)}>
                Edit
              </button>
              <button className="btn" onClick={() => setShowHistory((v) => !v)}>
                {showHistory ? "Hide history" : `History (${history.length})`}
              </button>
              {history.length > 1 && history[0]!.via === "agent" ? (
                <button className="btn" onClick={() => act(() => revertPageFn({ data: { slug: view.slug, version: view.head_version - 1 } }), "Took back the agent's last edit.")}>
                  Take back the agent's last edit
                </button>
              ) : null}
              <label className="ml-auto flex items-center gap-2 text-[14px] text-mute">
                <input type="checkbox" checked={view.in_digest} onChange={(e) => act(() => setInDigestFn({ data: { slug: view.slug, value: e.target.checked } }))} />
                Load into every agent run
              </label>
            </div>
          ) : null}
        </Panel>

        {showHistory ? (
          <Panel title="History" aside={`${history.length} versions`}>
            <ul>
              {history.map((h) => (
                <li key={h.version} className="flex flex-wrap items-baseline justify-between gap-2 border-t border-line py-1.5 first:border-t-0">
                  <Link to="/notes" search={{ slug: view.slug, version: h.version }} className={`link ${h.version === view.version ? "font-semibold text-or" : ""}`}>
                    Version {h.version}
                  </Link>
                  <span className="text-[13px] text-mute">
                    {h.via}
                    {h.note ? ` · ${h.note}` : ""} · <Ago iso={h.created_at} />
                  </span>
                </li>
              ))}
            </ul>
            {diff ? (
              <>
                <h3 className="cond mt-4 mb-1 text-[13px] font-semibold text-mute">What version {view.version} changed</h3>
                <pre className="overflow-x-auto border border-line bg-well p-3 text-[12.5px] leading-relaxed">
                  {diff.split("\n").map((line, i) => (
                    <span key={i} className={line.startsWith("+") ? "text-or" : line.startsWith("-") ? "text-dim line-through" : "text-mute"}>
                      {line}
                      {"\n"}
                    </span>
                  ))}
                </pre>
              </>
            ) : null}
          </Panel>
        ) : null}
      </div>

      <Panel title="Connections">
        <LinkList title="Links here" pages={backlinks} />
        <LinkList title="Links to" pages={linksOut} />
        {missing.length > 0 ? (
          <section className="mt-4">
            <h4 className="cond border-b border-line pb-1 text-[13px] font-semibold text-mute">Not written yet</h4>
            <ul className="mt-1 text-[13.5px] text-dim">
              {missing.map((m) => (
                <li key={m} className="py-0.5">
                  {m}
                </li>
              ))}
            </ul>
          </section>
        ) : null}
        <p className="mt-4 text-[13px]">
          <Link to="/notes" search={{ view: "graph", slug: view.slug }} className="link">
            See it on the graph
          </Link>
        </p>
      </Panel>
    </div>
  );
}
