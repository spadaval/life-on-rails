import { useState } from "react";
import { Link, createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { Ago } from "../components/Ago.js";
import { Markdown } from "../components/Markdown.js";
import {
  createPageFn, getMemoryFn, revertPageFn, savePageFn, setInDigestFn, setPersonFn,
} from "../server/functions/memory.js";

const Search = z.object({
  tab: z.enum(["pages", "people", "timeline"]).optional(),
  slug: z.string().optional(),
  version: z.coerce.number().int().optional(),
  q: z.string().optional(),
  date: z.string().optional(),
  obs: z.string().optional(),
  filter: z.enum(["important", "all", "keep-in-touch", "overdue", "birthdays", "groups"]).optional(),
  pq: z.string().optional(),
});

export const Route = createFileRoute("/memory")({
  validateSearch: (search) => Search.parse(search),
  loaderDeps: ({ search }) => ({ tab: search.tab, slug: search.slug, version: search.version, q: search.q, date: search.date, filter: search.filter, pq: search.pq }),
  loader: ({ deps }) => getMemoryFn({ data: deps }),
  component: Memory,
});

type Data = Awaited<ReturnType<typeof getMemoryFn>>;
type Act = (fn: () => Promise<unknown>, done?: string) => Promise<boolean>;

const TABS = [
  { id: "pages", label: "Pages" },
  { id: "people", label: "People" },
  { id: "timeline", label: "Timeline" },
] as const;

function Memory() {
  const data = Route.useLoaderData();
  const search = Route.useSearch();
  const router = useRouter();
  const navigate = useNavigate({ from: "/memory" });
  const [q, setQ] = useState(data.q);
  const [notice, setNotice] = useState<string | null>(null);

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

  return (
    <div className="space-y-5">
      <h1 className="text-3xl font-bold">Memory</h1>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void navigate({ search: (s) => ({ ...s, q: q.trim() || undefined }) });
        }}
        className="flex gap-2"
      >
        <input className="input" placeholder="Search everything the agent knows…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search" />
        <button className="btn" type="submit">
          Search
        </button>
      </form>

      {data.results ? <Results results={data.results} timezone={data.timezone} onClear={() => { setQ(""); void navigate({ search: (s) => ({ ...s, q: undefined }) }); }} /> : null}

      <nav className="flex gap-1">
        {TABS.map((t) => (
          <Link key={t.id} to="/memory" search={{ tab: t.id }} className={`rounded-lg px-3 py-2 font-medium ${data.tab === t.id ? "bg-black/10 dark:bg-white/15" : ""}`}>
            {t.label}
          </Link>
        ))}
      </nav>

      {notice ? (
        <p className="card text-sm" role="status">
          {notice}{" "}
          <button className="underline" onClick={() => setNotice(null)}>
            dismiss
          </button>
        </p>
      ) : null}

      {data.tab === "pages" ? <Pages data={data} act={act} /> : null}
      {data.tab === "people" ? <People data={data} act={act} /> : null}
      {data.tab === "timeline" ? <Timeline data={data} highlight={search.obs} /> : null}
    </div>
  );
}

function Results({ results, timezone, onClear }: { results: NonNullable<Data["results"]>; timezone: string; onClear: () => void }) {
  return (
    <section className="card space-y-2">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold">{results.length === 0 ? "Nothing found" : `${results.length} found`}</h2>
        <button className="underline" onClick={onClear}>
          clear
        </button>
      </div>
      <ul className="divide-y divide-black/10 dark:divide-white/10">
        {results.map((r) => (
          <li key={`${r.kind}:${r.id}`} className="py-2">
            <span className="chip mr-2">{r.kind}</span>
            {r.kind === "page" ? (
              <Link to="/memory" search={{ slug: r.id }} className="font-medium underline">
                {r.title}
              </Link>
            ) : r.kind === "entity" ? (
              <Link to="/memory" search={{ tab: "people" }} className="font-medium underline">
                {r.title}
              </Link>
            ) : r.kind === "obs" ? (
              <Link to="/memory" search={{ tab: "timeline", date: localDate(r.t, timezone), obs: r.id }} className="font-medium underline">
                {r.title || "(untitled)"}
              </Link>
            ) : (
              <Link to="/graph" className="font-medium underline">
                {r.title}
              </Link>
            )}
            <p className="muted text-sm">{r.snippet}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}

/** The local calendar date of an instant, for a link into the Timeline. */
function localDate(iso: string, timeZone: string): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date(iso));
}

function Pages({ data, act }: { data: Data; act: Act }) {
  const navigate = useNavigate({ from: "/memory" });
  const [creating, setCreating] = useState(false);
  const pinned = data.pages.filter((p) => p.in_digest);
  const rest = data.pages.filter((p) => !p.in_digest);
  const groups = new Map<string, typeof rest>();
  for (const p of rest) {
    const prefix = p.slug.includes("/") ? p.slug.slice(0, p.slug.indexOf("/")) : "";
    groups.set(prefix, [...(groups.get(prefix) ?? []), p]);
  }

  if (data.page) return <PageView key={`${data.page.view.slug}@${data.page.view.version}`} page={data.page} act={act} />;

  return (
    <div className="space-y-5">
      <section>
        <h2 className="mb-1 text-lg font-semibold">Loaded into every agent run</h2>
        <ul className="space-y-2">
          {pinned.map((p) => (
            <li key={p.slug} className="card flex flex-wrap items-baseline justify-between gap-2">
              <Link to="/memory" search={{ slug: p.slug }} className="text-lg font-medium underline">
                {p.title}
              </Link>
              <span className="muted text-sm">
                v{p.head_version} · last changed by {p.last_via} <Ago iso={p.updated_at} />
              </span>
            </li>
          ))}
        </ul>
      </section>

      {[...groups.entries()].map(([prefix, list]) => (
        <section key={prefix}>
          <h2 className="mb-1 text-lg font-semibold">{prefix || "Other"}</h2>
          <ul className="divide-y divide-black/10 dark:divide-white/10">
            {list.map((p) => (
              <li key={p.slug} className="flex flex-wrap items-baseline justify-between gap-2 py-2">
                <Link to="/memory" search={{ slug: p.slug }} className="font-medium underline">
                  {p.title}
                </Link>
                <span className="muted text-sm">
                  {p.slug} · {p.last_via} <Ago iso={p.updated_at} />
                </span>
              </li>
            ))}
          </ul>
        </section>
      ))}

      {creating ? (
        <NewPage onCancel={() => setCreating(false)} onCreate={(input) => act(() => createPageFn({ data: input })).then((ok) => { if (ok) void navigate({ search: { slug: input.slug } }); })} />
      ) : (
        <button className="btn" onClick={() => setCreating(true)}>
          New page
        </button>
      )}
    </div>
  );
}

function NewPage({ onCreate, onCancel }: { onCreate: (input: { slug: string; title: string; body: string }) => void; onCancel: () => void }) {
  const [slug, setSlug] = useState("topics/");
  const [title, setTitle] = useState("");
  return (
    <form
      className="card space-y-2"
      onSubmit={(e) => {
        e.preventDefault();
        onCreate({ slug: slug.trim(), title: title.trim(), body: `# ${title.trim()}\n` });
      }}
    >
      <input className="input" placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} aria-label="Title" />
      <input className="input" placeholder="slug, e.g. topics/home-network" value={slug} onChange={(e) => setSlug(e.target.value)} aria-label="Slug" />
      <div className="flex gap-2">
        <button className="btn btn-primary" type="submit">
          Create
        </button>
        <button className="btn" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

function PageView({ page, act }: { page: NonNullable<Data["page"]>; act: Act }) {
  const { view, history, diff } = page;
  const navigate = useNavigate({ from: "/memory" });
  const [editing, setEditing] = useState(false);
  const [body, setBody] = useState(view.body);
  const [showHistory, setShowHistory] = useState(view.version !== view.head_version);
  const isHead = view.version === view.head_version;

  const [conflict, setConflict] = useState(false);
  const save = async (force = false) => {
    const ok = await act(() => savePageFn({ data: { slug: view.slug, base: view.head_version, body, force } }), "Saved.");
    if (ok) setEditing(false);
    else setConflict(true);
  };

  return (
    <article className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Link to="/memory" search={{ tab: "pages" }} className="underline">
          All pages
        </Link>
        <span className="muted text-sm">
          {view.slug} · version {view.version} of {view.head_version} · {view.via}
          {view.source_ref ? ` · thread ${view.source_ref}` : ""} · <Ago iso={view.written_at} />
        </span>
      </div>

      {!isHead ? (
        <p className="card flex flex-wrap items-center justify-between gap-2 border-warn text-sm">
          <span>You are looking at an old version.</span>
          <span className="flex gap-2">
            <button
              className="btn btn-primary"
              onClick={() => act(() => revertPageFn({ data: { slug: view.slug, version: view.version } }), `Reverted to version ${view.version}.`).then(() => navigate({ search: { slug: view.slug } }))}
            >
              Revert to this version
            </button>
            <Link to="/memory" search={{ slug: view.slug }} className="btn">
              Back to current
            </Link>
          </span>
        </p>
      ) : null}

      {editing ? (
        <div className="space-y-2">
          <textarea className="input min-h-[60vh] font-mono text-sm" value={body} onChange={(e) => setBody(e.target.value)} aria-label="Page body" />
          <div className="flex gap-2">
            <button className="btn btn-primary" onClick={() => save()}>
              Save
            </button>
            {conflict ? (
              <button className="btn" onClick={() => save(true)}>
                Save mine on top
              </button>
            ) : null}
            <button className="btn" onClick={() => { setBody(view.body); setEditing(false); }}>
              Cancel
            </button>
          </div>
          <p className="muted text-sm">
            {conflict
              ? "The page changed while you were editing. Your text is still here. “Save mine on top” makes it the newest version; the other change stays in the history."
              : "If the agent saves a change while you are editing, your save is refused and your text stays here."}
          </p>
        </div>
      ) : (
        <Markdown source={view.body} />
      )}

      {isHead && !editing ? (
        <div className="flex flex-wrap items-center gap-2 border-t border-black/10 pt-3 dark:border-white/10">
          <button className="btn btn-primary" onClick={() => setEditing(true)}>
            Edit
          </button>
          <button className="btn" onClick={() => setShowHistory((v) => !v)}>
            {showHistory ? "Hide history" : `History (${history.length})`}
          </button>
          {history.length > 1 && history[0]!.via === "agent" ? (
            <button
              className="btn"
              onClick={() => act(() => revertPageFn({ data: { slug: view.slug, version: view.head_version - 1 } }), "Undid the agent's last edit.")}
            >
              Undo the agent's last edit
            </button>
          ) : null}
          <label className="ml-auto flex items-center gap-2 text-sm">
            <input type="checkbox" checked={view.in_digest} onChange={(e) => act(() => setInDigestFn({ data: { slug: view.slug, value: e.target.checked } }))} />
            Load into every agent run
          </label>
        </div>
      ) : null}

      {showHistory ? (
        <section className="space-y-2">
          <h2 className="text-lg font-semibold">History</h2>
          <ul className="divide-y divide-black/10 dark:divide-white/10">
            {history.map((h) => (
              <li key={h.version} className="flex flex-wrap items-baseline justify-between gap-2 py-2">
                <Link to="/memory" search={{ slug: view.slug, version: h.version }} className={`underline ${h.version === view.version ? "font-bold" : ""}`}>
                  version {h.version}
                </Link>
                <span className="muted text-sm">
                  {h.via}
                  {h.note ? ` · ${h.note}` : ""} · <Ago iso={h.created_at} />
                </span>
              </li>
            ))}
          </ul>
          {diff ? (
            <>
              <h3 className="font-semibold">What version {view.version} changed</h3>
              <pre className="overflow-x-auto rounded-lg bg-black/5 p-3 text-xs dark:bg-white/10">
                {diff.split("\n").map((line, i) => (
                  <span key={i} className={line.startsWith("+") ? "text-rail" : line.startsWith("-") ? "text-bad" : ""}>
                    {line}
                    {"\n"}
                  </span>
                ))}
              </pre>
            </>
          ) : null}
        </section>
      ) : null}
    </article>
  );
}

const CADENCES: [string, number | null][] = [
  ["no cadence", null],
  ["every week", 7],
  ["every 2 weeks", 14],
  ["every month", 30],
  ["every 2 months", 60],
  ["every 3 months", 90],
  ["every 6 months", 182],
  ["every year", 365],
];

const FILTERS = [
  { id: "important", label: "Who matters" },
  { id: "keep-in-touch", label: "Keeping in touch" },
  { id: "overdue", label: "Overdue" },
  { id: "birthdays", label: "Birthdays soon" },
  { id: "all", label: "Everyone" },
  { id: "groups", label: "Groups" },
] as const;

const VIA: Record<string, string> = { mail_in: "they emailed", mail_out: "you emailed", meeting: "you met", phone: "call, text or WhatsApp" };

function People({ data, act }: { data: Data; act: Act }) {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/memory" });
  const [pq, setPq] = useState(search.pq ?? "");
  const filter = search.filter ?? "important";
  const { people, total } = data.people;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <Link key={f.id} to="/memory" search={{ tab: "people", filter: f.id, pq: search.pq }} className={`chip py-1 text-sm ${filter === f.id ? "bg-rail text-white" : ""}`}>
            {f.label}
          </Link>
        ))}
      </div>
      <form
        className="flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          void navigate({ search: (s) => ({ ...s, tab: "people", pq: pq.trim() || undefined }) });
        }}
      >
        <input className="input" placeholder="Find a person by name, email, or number…" value={pq} onChange={(e) => setPq(e.target.value)} aria-label="Find a person" />
        <button className="btn" type="submit">
          Find
        </button>
      </form>
      <p className="muted text-sm">
        {total === 0
          ? filter === "important" && !search.pq
            ? "No one yet. People arrive from Google Contacts once LoR is signed in, and the agent adds people who matter."
            : "No one matches."
          : `${total} ${filter === "groups" ? (total === 1 ? "group" : "groups") : total === 1 ? "person" : "people"}${total > people.length ? `, showing ${people.length}` : ""}`}
      </p>
      <ul className="space-y-2">
        {people.map((p) => (
          <li key={p.id} className="card space-y-2">
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <span className="text-lg font-medium">
                {p.page ? (
                  <Link to="/memory" search={{ slug: p.page }} className="underline">
                    {p.name}
                  </Link>
                ) : (
                  p.name
                )}{" "}
                {p.kind !== "person" ? <span className="chip">{p.kind}</span> : null}
                {p.from_google ? <span className="chip ml-1">contacts</span> : null}
              </span>
              {p.overdue_by !== null && p.overdue_by > 0 ? (
                <span className="chip border-warn text-warn">overdue by {p.overdue_by} days</span>
              ) : null}
            </div>
            <p className="text-sm">
              {p.last.at ? (
                <>
                  Last in touch <Ago iso={p.last.at} />
                  {p.last.via ? <span className="muted"> · {VIA[p.last.via]}</span> : null}
                </>
              ) : (
                <span className="muted">Never in touch that LoR has seen.</span>
              )}
              {p.birthday ? (
                <span className="muted">
                  {" "}
                  · birthday {new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", timeZone: "UTC" }).format(new Date(`2000-${p.birthday.slice(-5)}T12:00:00Z`))}
                  {p.birthday_in !== null && p.birthday_in <= 30 ? ` (${p.birthday_in === 0 ? "today" : `in ${p.birthday_in} days`}${p.turning ? `, turning ${p.turning}` : ""})` : ""}
                </span>
              ) : null}
            </p>
            <div className="flex flex-wrap items-center gap-3 text-sm">
              <select
                className="input w-auto"
                aria-label="Keep in touch"
                value={p.keep_in_touch_days ?? ""}
                onChange={(e) => act(() => setPersonFn({ data: { id: p.id, keepInTouchDays: e.target.value ? Number(e.target.value) : null } }))}
              >
                {CADENCES.map(([label, days]) => (
                  <option key={label} value={days ?? ""}>
                    {label}
                  </option>
                ))}
                {p.keep_in_touch_days !== null && !CADENCES.some(([, d]) => d === p.keep_in_touch_days) ? (
                  <option value={p.keep_in_touch_days}>every {p.keep_in_touch_days} days</option>
                ) : null}
              </select>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={p.priority} onChange={(e) => act(() => setPersonFn({ data: { id: p.id, priority: e.target.checked } }))} />
                Their mail reaches the agent at once
              </label>
            </div>
            {p.identifiers.length > 0 ? <p className="muted text-xs">{p.identifiers.map((i) => i.value).join(" · ")}</p> : null}
          </li>
        ))}
      </ul>
    </div>
  );
}

function Timeline({ data, highlight }: { data: Data; highlight?: string }) {
  const clock = (iso: string) => new Intl.DateTimeFormat("en-US", { timeZone: data.timezone, hour: "numeric", minute: "2-digit" }).format(new Date(iso));
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <Link to="/memory" search={{ tab: "timeline", date: data.prev }} className="btn">
          Earlier
        </Link>
        <span className="font-semibold">{data.date}</span>
        <Link to="/memory" search={{ tab: "timeline", date: data.next }} className="btn">
          Later
        </Link>
      </div>
      {data.timeline.length === 0 ? <p className="muted">Nothing was recorded on this day.</p> : null}
      <ul className="divide-y divide-black/10 dark:divide-white/10">
        {data.timeline.map((o) => (
          <li key={o.id} className={`flex flex-wrap items-baseline gap-x-3 py-2 ${o.bulk ? "opacity-50" : ""} ${highlight && o.ids.includes(highlight) ? "bg-warn/15" : ""}`}>
            <span className="muted w-20 shrink-0 text-sm tabular-nums">{clock(o.t)}</span>
            <span className="chip">{o.source}</span>
            <span className="min-w-0 flex-1">
              {o.title ?? "(untitled)"}
              {o.detail ? <span className="muted"> · {o.detail}</span> : null}
              {o.bulk ? <span className="muted"> · bulk</span> : null}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
