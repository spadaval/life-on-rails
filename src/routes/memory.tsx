import { useState } from "react";
import { Link, createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { Ago } from "../components/Ago.js";
import { Markdown } from "../components/Markdown.js";
import { Empty, Notice, PageHead, Panel } from "../components/ui.js";
import {
  createPageFn, getMemoryFn, revertPageFn, savePageFn, setInDigestFn, setPersonFn,
} from "../server/functions/memory.js";
import { GlobalActions } from "./__root.js";

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
    <div className="space-y-5 pb-4">
      <PageHead title="Memory" sub="what the agent knows">
        <GlobalActions />
      </PageHead>

      <div className="flex flex-wrap items-center gap-3">
        <nav className="flex border border-edge" aria-label="Memory">
          {TABS.map((t) => (
            <Link
              key={t.id}
              to="/memory"
              search={{ tab: t.id }}
              className={`border-l border-edge px-4 py-1 text-[14px] first:border-l-0 ${data.tab === t.id ? "row-sel text-txt" : "text-mute hover:text-txt"}`}
            >
              {t.label}
            </Link>
          ))}
        </nav>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void navigate({ search: (s) => ({ ...s, q: q.trim() || undefined }) });
          }}
          className={`ml-auto w-full max-w-[520px] gap-2 ${data.tab === "people" && !data.results ? "hidden" : "flex"}`}
        >
          <input className="input" placeholder="Search pages, people, messages, mail, tasks…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search memory" />
          <button className="btn" type="submit">
            Search
          </button>
        </form>
      </div>

      <Notice text={notice} onClose={() => setNotice(null)} />

      {data.results ? (
        <Results
          results={data.results}
          timezone={data.timezone}
          onClear={() => {
            setQ("");
            void navigate({ search: (s) => ({ ...s, q: undefined }) });
          }}
        />
      ) : null}

      {data.tab === "pages" ? <Pages data={data} act={act} /> : null}
      {data.tab === "people" ? <People data={data} act={act} /> : null}
      {data.tab === "timeline" ? <Timeline data={data} highlight={search.obs} /> : null}
    </div>
  );
}

function Results({ results, timezone, onClear }: { results: NonNullable<Data["results"]>; timezone: string; onClear: () => void }) {
  return (
    <Panel
      title={results.length === 0 ? "Nothing found" : `${results.length} found`}
      aside={
        <button className="btn btn-sm" onClick={onClear}>
          Clear
        </button>
      }
    >
      <ul className="grid gap-x-8 lg:grid-cols-2">
        {results.map((r) => (
          <li key={`${r.kind}:${r.id}`} className="border-t border-line py-2">
            <span className="chip mr-2">{r.kind === "obs" ? "record" : r.kind === "entity" ? "person" : r.kind === "intent" ? "graph" : r.kind}</span>
            {r.kind === "page" ? (
              <Link to="/memory" search={{ slug: r.id }} className="link font-medium">
                {r.title}
              </Link>
            ) : r.kind === "entity" ? (
              <Link to="/memory" search={{ tab: "people", pq: r.title }} className="link font-medium">
                {r.title}
              </Link>
            ) : r.kind === "obs" ? (
              <Link to="/memory" search={{ tab: "timeline", date: localDate(r.t, timezone), obs: r.id }} className="link font-medium">
                {r.title || "(untitled)"}
              </Link>
            ) : (
              <Link to="/graph" search={{ id: r.id }} className="link font-medium">
                {r.title}
              </Link>
            )}
            <p className="mt-0.5 line-clamp-2 text-[13.5px] text-mute">{r.snippet}</p>
          </li>
        ))}
      </ul>
    </Panel>
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
  const current = data.page?.view.slug ?? null;
  const item = (p: (typeof rest)[number], sub: string) => (
    <li key={p.slug}>
      <Link
        to="/memory"
        search={{ slug: p.slug }}
        className={`flex items-baseline justify-between gap-3 px-2 py-1.5 ${current === p.slug ? "row-sel" : "hover:bg-raised"}`}
      >
        <span className="min-w-0 truncate">{p.title}</span>
        <span className="shrink-0 text-[12.5px] text-dim">{sub}</span>
      </Link>
    </li>
  );

  return (
    <div className="grid items-start gap-[22px] lg:grid-cols-[320px_minmax(0,1fr)]">
      <Panel title="Pages" aside={`${data.pages.length}`} className={data.page ? "hidden lg:block" : ""}>
        <h3 className="cond mt-1 mb-1 text-[12.5px] font-medium text-dim">Loaded into every agent run</h3>
        <ul className="-mx-2">{pinned.map((p) => item(p, `v${p.head_version}`))}</ul>
        {[...groups.entries()].map(([prefix, list]) => (
          <section key={prefix} className="mt-4">
            <h3 className="cond mb-1 text-[12.5px] font-medium text-dim">{prefix || "Other"}</h3>
            <ul className="-mx-2">{list.map((p) => item(p, p.last_via))}</ul>
          </section>
        ))}
        <div className="mt-4 border-t border-line pt-3">
          {creating ? (
            <NewPage
              onCancel={() => setCreating(false)}
              onCreate={(input) =>
                act(() => createPageFn({ data: input })).then((ok) => {
                  if (ok) void navigate({ search: { slug: input.slug } });
                })
              }
            />
          ) : (
            <button className="btn" onClick={() => setCreating(true)}>
              New page
            </button>
          )}
        </div>
      </Panel>

      {data.page ? (
        <PageView key={`${data.page.view.slug}@${data.page.view.version}`} page={data.page} act={act} />
      ) : (
        <Panel title="Loaded into every agent run">
          <p className="mb-3 text-[14px] text-mute">These pages go into every agent session. Only you decide which; the agent may still edit them, and its edits show here with who made them.</p>
          <ul>
            {pinned.map((p) => (
              <li key={p.slug} className="flex flex-wrap items-baseline justify-between gap-2 border-t border-line py-2.5">
                <Link to="/memory" search={{ slug: p.slug }} className="link text-[16px] font-medium">
                  {p.title}
                </Link>
                <span className="text-[13px] text-mute">
                  v{p.head_version} · last changed by {p.last_via} <Ago iso={p.updated_at} />
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
  return (
    <form
      className="space-y-2"
      onSubmit={(e) => {
        e.preventDefault();
        onCreate({ slug: slug.trim(), title: title.trim(), body: `# ${title.trim()}\n` });
      }}
    >
      <input className="input" placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} aria-label="Title" autoFocus />
      <input className="input" placeholder="slug, e.g. topics/home-network" value={slug} onChange={(e) => setSlug(e.target.value)} aria-label="Slug" />
      <div className="flex gap-2">
        <button className="btn border-or text-or" type="submit">
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
  const [conflict, setConflict] = useState(false);
  const isHead = view.version === view.head_version;

  const save = async (force = false) => {
    const ok = await act(() => savePageFn({ data: { slug: view.slug, base: view.head_version, body, force } }), "Saved.");
    if (ok) setEditing(false);
    else setConflict(true);
  };

  return (
    <div className="space-y-[22px]">
      <Panel
        title={
          <Link to="/memory" search={{ tab: "pages" }} className="text-mute hover:text-txt lg:hidden">
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
              <Link to="/memory" search={{ slug: view.slug }} className="btn btn-sm">
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
                : "If the agent saves a change while you are editing, your save is refused and your text stays here."}
            </p>
          </div>
        ) : (
          <Markdown source={view.body} />
        )}

        <p className="mt-6 text-[13px] text-dim">
          {view.slug} · version {view.version} of {view.head_version} · last written by {view.via}
          {view.source_ref ? ` · thread ${view.source_ref.slice(0, 8)}` : ""} · <Ago iso={view.written_at} />
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
              <button className="btn" onClick={() => act(() => revertPageFn({ data: { slug: view.slug, version: view.head_version - 1 } }), "Undid the agent's last edit.")}>
                Undo the agent's last edit
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
                <Link to="/memory" search={{ slug: view.slug, version: h.version }} className={`link ${h.version === view.version ? "font-semibold text-or" : ""}`}>
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
  );
}

const CADENCES: [string, number | null][] = [
  ["No cadence", null],
  ["Every week", 7],
  ["Every 2 weeks", 14],
  ["Every month", 30],
  ["Every 2 months", 60],
  ["Every 3 months", 90],
  ["Every 6 months", 182],
  ["Every year", 365],
];

const FILTERS = [
  { id: "important", label: "Who matters" },
  { id: "keep-in-touch", label: "Keeping in touch" },
  { id: "overdue", label: "Due a hello" },
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
  const bday = (b: string) => new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", timeZone: "UTC" }).format(new Date(`2000-${b.slice(-5)}T12:00:00Z`));

  return (
    <Panel
      title="People"
      aside={
        total === 0
          ? undefined
          : `${total} ${filter === "groups" ? (total === 1 ? "group" : "groups") : total === 1 ? "person" : "people"}${total > people.length ? `, showing ${people.length}` : ""}`
      }
    >
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <div className="flex flex-wrap border border-edge">
          {FILTERS.map((f) => (
            <Link
              key={f.id}
              to="/memory"
              search={{ tab: "people", filter: f.id, pq: search.pq }}
              className={`border-l border-edge px-3 py-1 text-[13.5px] first:border-l-0 ${filter === f.id ? "row-sel text-txt" : "text-mute hover:text-txt"}`}
            >
              {f.label}
            </Link>
          ))}
        </div>
        <form
          className="ml-auto flex w-full max-w-[380px] gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            void navigate({ search: (s) => ({ ...s, tab: "people", pq: pq.trim() || undefined }) });
          }}
        >
          <input className="input" placeholder="Find by name, email, or number" value={pq} onChange={(e) => setPq(e.target.value)} aria-label="Find a person" />
          <button className="btn" type="submit">
            Find
          </button>
        </form>
      </div>

      {total === 0 ? (
        <Empty>
          {filter === "important" && !search.pq
            ? "No one yet. People arrive from Google Contacts once LoR is signed in, and the agent adds people who matter."
            : "No one matches."}
        </Empty>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[860px] text-left text-[14px]">
            <thead>
              <tr className="cond border-b border-line text-[12.5px] text-dim">
                <th className="py-2 pr-4 font-medium">Name</th>
                <th className="py-2 pr-4 font-medium">Last in touch</th>
                <th className="py-2 pr-4 font-medium">Birthday</th>
                <th className="py-2 pr-4 font-medium">Keep in touch</th>
                <th className="py-2 font-medium">Mail goes straight to the agent</th>
              </tr>
            </thead>
            <tbody>
              {people.map((p) => (
                <tr key={p.id} className="border-b border-line align-top hover:bg-raised">
                  <td className="py-2.5 pr-4">
                    <span className="font-medium">
                      {p.page ? (
                        <Link to="/memory" search={{ slug: p.page }} className="link">
                          {p.name}
                        </Link>
                      ) : (
                        p.name
                      )}
                    </span>
                    {p.kind !== "person" ? <span className="chip ml-2">{p.kind}</span> : null}
                    {p.identifiers.length > 0 ? <span className="block max-w-[340px] truncate text-[12.5px] text-dim">{p.identifiers.map((i) => i.value).join(" · ")}</span> : null}
                  </td>
                  <td className="py-2.5 pr-4">
                    {p.last.at ? (
                      <>
                        <Ago iso={p.last.at} />
                        {p.last.via ? <span className="block text-[12.5px] text-dim">{VIA[p.last.via]}</span> : null}
                      </>
                    ) : (
                      <span className="text-dim">Not seen yet</span>
                    )}
                    {p.overdue_by !== null && p.overdue_by > 0 ? <span className="block text-[12.5px] text-or">{p.overdue_by} days past the cadence</span> : null}
                  </td>
                  <td className="py-2.5 pr-4 text-mute">
                    {p.birthday ? (
                      <>
                        {bday(p.birthday)}
                        {p.birthday_in !== null && p.birthday_in <= 30 ? (
                          <span className="block text-[12.5px] text-or">
                            {p.birthday_in === 0 ? "today" : `in ${p.birthday_in} days`}
                            {p.turning ? `, turning ${p.turning}` : ""}
                          </span>
                        ) : null}
                      </>
                    ) : (
                      <span className="text-dim">—</span>
                    )}
                  </td>
                  <td className="py-2 pr-4">
                    <select
                      className="input w-[150px] py-1"
                      aria-label={`Keep in touch with ${p.name}`}
                      value={p.keep_in_touch_days ?? ""}
                      onChange={(e) => act(() => setPersonFn({ data: { id: p.id, keepInTouchDays: e.target.value ? Number(e.target.value) : null } }))}
                    >
                      {CADENCES.map(([label, days]) => (
                        <option key={label} value={days ?? ""}>
                          {label}
                        </option>
                      ))}
                      {p.keep_in_touch_days !== null && !CADENCES.some(([, d]) => d === p.keep_in_touch_days) ? (
                        <option value={p.keep_in_touch_days}>Every {p.keep_in_touch_days} days</option>
                      ) : null}
                    </select>
                  </td>
                  <td className="py-2.5">
                    <input
                      type="checkbox"
                      className="size-[15px]"
                      checked={p.priority}
                      onChange={(e) => act(() => setPersonFn({ data: { id: p.id, priority: e.target.checked } }))}
                      aria-label={`${p.name}'s mail reaches the agent at once`}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}

const SOURCE: Record<string, string> = { android: "phone", gmail: "mail", calendar: "calendar", agent: "agent", manual: "you", google_tasks: "tasks", system: "system" };

function Timeline({ data, highlight }: { data: Data; highlight?: string }) {
  const clock = (iso: string) => new Intl.DateTimeFormat("en-GB", { timeZone: data.timezone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(iso));
  const label = new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "long", day: "numeric", month: "long" }).format(new Date(`${data.date}T12:00:00Z`));
  return (
    <Panel
      title={label}
      aside={
        <span className="flex gap-2">
          <Link to="/memory" search={{ tab: "timeline", date: data.prev }} className="btn btn-sm">
            Earlier
          </Link>
          <Link to="/memory" search={{ tab: "timeline", date: data.next }} className="btn btn-sm">
            Later
          </Link>
        </span>
      }
    >
      {data.timeline.length === 0 ? <Empty>Nothing was recorded on this day.</Empty> : null}
      <ul>
        {data.timeline.map((o) => (
          <li
            key={o.id}
            className={`grid grid-cols-[56px_84px_minmax(0,1fr)] items-baseline gap-x-3 border-t border-line py-1.5 first:border-t-0 ${o.bulk ? "opacity-50" : ""} ${highlight && o.ids.includes(highlight) ? "row-sel" : ""}`}
          >
            <span className="text-[13.5px] text-mute">{clock(o.t)}</span>
            <span className={`text-[12.5px] ${o.source === "agent" ? "text-cy" : "text-dim"}`}>{SOURCE[o.source] ?? o.source}</span>
            <span className="min-w-0">
              {o.title ?? "(untitled)"}
              {o.detail ? <span className="text-mute"> · {o.detail}</span> : null}
              {o.bulk ? <span className="text-dim"> · bulk</span> : null}
            </span>
          </li>
        ))}
      </ul>
    </Panel>
  );
}
