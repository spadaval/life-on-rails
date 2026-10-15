// Notes: the wiki the agent and the owner both write. An index by folder, the
// reader with what links here, and the whole wiki as a star map.
import { useEffect, useRef, useState } from "react";
import { Link, createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { Ago } from "../components/Ago.js";
import { Markdown } from "../components/Markdown.js";
import { StarMap } from "../components/StarMap.js";
import { Empty, Notice, PageHead, Panel, Star } from "../components/ui.js";
import { createPageFn, getNotesFn, pageHeadFn, revertPageFn, savePageFn, setInDigestFn, takeBackFn } from "../server/functions/memory.js";
import { type Merge, carry, join, merge3 } from "../shared/merge.js";
import { NoteEditor } from "../components/NoteEditor.js";
import { folderOf, titleFromSlug } from "../shared/wiki.js";
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

  // Following a link to a page nobody has written yet starts it.
  const follow = async (slug: string, label: string | null) => {
    if (!data.pages.some((p) => p.slug === slug)) {
      const title = label ?? titleFromSlug(slug);
      if (!(await act(() => createPageFn({ data: { slug, title, body: `# ${title}\n\n` } }), `Started “${title}”.`))) return;
    }
    void navigate({ search: { slug } });
  };

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

      {graph ? <GraphView data={data} selected={search.slug ?? null} /> : <PagesView data={data} act={act} onFollow={follow} />}
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

function PagesView({ data, act, onFollow }: { data: Data; act: Act; onFollow: (slug: string, label: string | null) => void }) {
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
        <PageView
          key={`${data.page.view.slug}@${data.page.view.version === data.page.view.head_version ? "head" : data.page.view.version}`}
          page={data.page}
          pages={data.pages}
          act={act}
          onFollow={onFollow}
        />
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

function PageView({ page, pages, act, onFollow }: { page: NonNullable<Data["page"]>; pages: Page[]; act: Act; onFollow: (slug: string, label: string | null) => void }) {
  const { view, history, diff, backlinks, missing } = page;
  const navigate = useNavigate({ from: "/notes" });
  const [showHistory, setShowHistory] = useState(view.version !== view.head_version);
  const isHead = view.version === view.head_version;
  const linksOut = (pages.find((p) => p.slug === view.slug)?.links ?? []).map((l) => pages.find((p) => p.slug === l)!).filter(Boolean);

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

          {isHead ? (
            <LiveBody key={view.slug} view={view} authors={page.authors} pages={pages} onFollow={onFollow} />
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
          {isHead ? (
            <div className="mt-2 flex flex-wrap items-center gap-2 border-t border-line pt-3">
              <button className="btn" onClick={() => setShowHistory((v) => !v)}>
                {showHistory ? "Hide history" : `History (${history.length})`}
              </button>
              {history.length > 1 && history[0]!.via === "agent" ? (
                <button className="btn" onClick={() => act(() => takeBackFn({ data: { slug: view.slug, version: history[0]!.version } }), "Took back the agent's last edit.")}>
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
                  <span className="flex items-baseline gap-3 text-[13px] text-mute">
                    <span>
                      {h.via}
                      {h.note ? ` · ${h.note}` : ""} · <Ago iso={h.created_at} />
                    </span>
                    {h.version !== history[history.length - 1]!.version ? (
                      <button
                        className="text-dim underline decoration-dim hover:text-txt"
                        title="Undo what this version changed, and keep every edit since"
                        onClick={() => act(() => takeBackFn({ data: { slug: view.slug, version: h.version } }), `Took back version ${h.version}.`)}
                      >
                        take back
                      </button>
                    ) : null}
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

type View = NonNullable<Data["page"]>["view"];
type SaveState = "saved" | "unsaved" | "saving" | "clash" | "error";
interface Head {
  version: number;
  body: string;
  via: string;
  authors: number[];
}
type Pick = "mine" | "theirs" | "both";

/**
 * The page as you write it. Saves a moment after you stop typing (a run of typing is one
 * version). When someone else wrote meanwhile, their edit and yours are merged line by
 * line; only the passages you both changed are put to you.
 */
function LiveBody({ view, authors, pages, onFollow }: { view: View; authors: number[]; pages: Page[]; onFollow: (slug: string, label: string | null) => void }) {
  const router = useRouter();
  const base = useRef(view.head_version);
  /** The text of the base version: what both your typing and any other edit started from. */
  const saved = useRef(view.body);
  const current = useRef(view.body);
  const marks = useRef(authors);
  const timer = useRef<number | undefined>(undefined);
  const inflight = useRef(false);
  const [state, setState] = useState<SaveState>("saved");
  const [problem, setProblem] = useState<string | null>(null);
  const [merged, setMerged] = useState<string | null>(null);
  const [clash, setClash] = useState<{ merge: Merge; head: Head; picks: (Pick | null)[] } | null>(null);
  const [external, setExternal] = useState({ text: view.body, n: 0, authors });
  const clashOpen = useRef(false);
  clashOpen.current = clash !== null;
  const targets = useMemoPages(pages);

  const schedule = () => {
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => void save(), 1200);
  };

  const show = (text: string, head: Head) => {
    marks.current = carry(head.authors, head.body, text, 0);
    setExternal((e) => ({ text, n: e.n + 1, authors: marks.current }));
  };

  /** Bring a newer version in under what is typed here. */
  const mergeWith = (head: Head) => {
    if (head.version <= base.current) return;
    const m = merge3(saved.current, current.current, head.body);
    const who = head.via === "agent" ? "the agent's" : "another";
    if (m.text === null) {
      clashOpen.current = true;
      setClash({ merge: m, head, picks: m.chunks.filter((c) => c.kind === "conflict").map(() => null) });
      setState("clash");
      return;
    }
    const mine = m.text !== head.body;
    base.current = head.version;
    saved.current = head.body;
    current.current = m.text;
    show(m.text, head);
    if (mine) {
      setMerged(`Merged with ${who} edit.`);
      setState("unsaved");
      schedule();
    } else {
      setState("saved");
    }
  };

  const save = async () => {
    window.clearTimeout(timer.current);
    if (inflight.current) return schedule();
    const body = current.current;
    // Nothing is saved while a clash is open; settling it saves.
    if (body === saved.current || clashOpen.current) return;
    inflight.current = true;
    setState("saving");
    try {
      const r = await savePageFn({ data: { slug: view.slug, base: base.current, body } });
      base.current = r.version;
      saved.current = body;
      setProblem(null);
      if (current.current === body) setState("saved");
      else {
        setState("unsaved");
        schedule();
      }
      void router.invalidate();
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      if (/is at version/.test(msg)) {
        inflight.current = false;
        mergeWith(await pageHeadFn({ data: { slug: view.slug } }));
      } else {
        setState("error");
        setProblem(msg);
      }
    } finally {
      inflight.current = false;
    }
  };

  const settle = () => {
    if (!clash || clash.picks.some((p) => p === null)) return;
    const text = join(clash.merge.chunks, (c, i) => (clash.picks[i] === "mine" ? c.mine : clash.picks[i] === "theirs" ? c.theirs : [...c.mine, ...c.theirs]));
    base.current = clash.head.version;
    saved.current = clash.head.body;
    current.current = text;
    show(text, clash.head);
    clashOpen.current = false;
    setClash(null);
    setState("unsaved");
    void save();
  };

  // A newer version arrived with the page (the agent, a take-back, another window).
  useEffect(() => {
    if (!inflight.current) mergeWith({ version: view.head_version, body: view.body, via: view.via, authors });
  }, [view.head_version]);

  // Coming back to the tab is when an agent edit is most likely waiting.
  useEffect(() => {
    const onFocus = () => void router.invalidate();
    const onLeave = (e: BeforeUnloadEvent) => {
      if (current.current !== saved.current) e.preventDefault();
    };
    window.addEventListener("focus", onFocus);
    window.addEventListener("beforeunload", onLeave);
    return () => {
      window.removeEventListener("focus", onFocus);
      window.removeEventListener("beforeunload", onLeave);
      window.clearTimeout(timer.current);
      // Leaving the page with typing not yet saved: save it on the way out.
      if (current.current !== saved.current && !inflight.current) {
        void savePageFn({ data: { slug: view.slug, base: base.current, body: current.current } }).catch(() => undefined);
      }
    };
  }, []);

  return (
    <div>
      {clash ? <Clash clash={clash} onPick={(i, p) => setClash({ ...clash, picks: clash.picks.map((x, n) => (n === i ? p : x)) })} onDone={settle} /> : null}
      {merged && !clash ? (
        <p role="status" className="row-sel mb-3 flex items-center justify-between px-3 py-1.5 text-[13.5px]">
          {merged}
          <button className="text-[13px] text-mute underline hover:text-txt" onClick={() => setMerged(null)}>
            dismiss
          </button>
        </p>
      ) : null}
      <NoteEditor
        initial={view.body}
        authors={authors}
        external={external}
        readOnly={clash !== null}
        pages={targets}
        onChange={(text) => {
          current.current = text;
          setState("unsaved");
          schedule();
        }}
        onFollow={onFollow}
        onSave={() => void save()}
      />
      <p className="mt-2 text-[12.5px] text-dim" aria-live="polite">
        {state === "saving" ? "Saving…" : state === "unsaved" ? "Not saved yet" : state === "error" ? `Not saved: ${problem}` : state === "clash" ? "Not saved" : "Saved"}
        <span className="ml-3">
          <code>[[</code> links a page · a link opens on a click, or Ctrl-click while you type it ·{" "}
          <span className="border-l-2 border-cy/55 pl-1.5">the agent wrote this</span>
        </span>
      </p>
    </div>
  );
}

/** The passages both you and someone else changed, one choice each. Everything else is already merged. */
function Clash({ clash, onPick, onDone }: {
  clash: { merge: Merge; head: Head; picks: (Pick | null)[] };
  onPick: (i: number, p: Pick) => void;
  onDone: () => void;
}) {
  const theirs = clash.head.via === "agent" ? "The agent's" : "The other edit";
  const conflicts = clash.merge.chunks.filter((c): c is Extract<Merge["chunks"][number], { kind: "conflict" }> => c.kind === "conflict");
  const block = (lines: string[]) => <pre className="font-sans text-[14px] whitespace-pre-wrap">{lines.join("\n") || "(removed)"}</pre>;
  return (
    <section className="mb-4 border border-or/50 bg-or/5 p-3" aria-label="Settle the clash">
      <p className="mb-3 text-[14px]">
        {clash.head.via === "agent" ? "The agent" : "Someone else"} changed this page while you were writing. Everything else is merged;{" "}
        {conflicts.length === 1 ? "one passage was" : `${conflicts.length} passages were`} changed by both of you.
      </p>
      {conflicts.map((c, i) => (
        <div key={i} className="mb-3 border-t border-line pt-2">
          <div className="grid gap-3 md:grid-cols-2">
            <div>
              <h4 className="cond mb-1 text-[12.5px] font-semibold text-mute">Yours</h4>
              {block(c.mine)}
            </div>
            <div>
              <h4 className="cond mb-1 text-[12.5px] font-semibold text-cy">{theirs}</h4>
              {block(c.theirs)}
            </div>
          </div>
          <div className="mt-2 flex gap-2">
            {(["mine", "theirs", "both"] as const).map((p) => (
              <button key={p} className={`btn btn-sm ${clash.picks[i] === p ? "border-or text-or" : ""}`} onClick={() => onPick(i, p)}>
                {p === "mine" ? "Keep yours" : p === "theirs" ? `Keep ${theirs.toLowerCase()}` : "Keep both"}
              </button>
            ))}
          </div>
        </div>
      ))}
      <button className="btn border-or text-or" disabled={clash.picks.some((p) => p === null)} onClick={onDone}>
        Done
      </button>
    </section>
  );
}

function useMemoPages(pages: Page[]) {
  const key = pages.map((p) => `${p.slug}\t${p.title}`).join("\n");
  const ref = useRef<{ key: string; list: { slug: string; title: string }[] }>({ key: "", list: [] });
  if (ref.current.key !== key) ref.current = { key, list: pages.map((p) => ({ slug: p.slug, title: p.title })) };
  return ref.current.list;
}
