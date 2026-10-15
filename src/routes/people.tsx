// People: who matters and when you were last in touch, and one hub per person
// with their page, what was said lately, and what waits on them.
import { useState } from "react";
import { Link, createFileRoute, useNavigate, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { Ago } from "../components/Ago.js";
import { dueLabel } from "../components/format.js";
import { Markdown } from "../components/Markdown.js";
import { Empty, Notice, PageHead, Panel } from "../components/ui.js";
import { getPeopleFn, getPersonFn, setPersonFn } from "../server/functions/memory.js";
import { GlobalActions } from "./__root.js";

const Search = z.object({
  id: z.string().uuid().optional(),
  filter: z.enum(["important", "all", "keep-in-touch", "overdue", "birthdays", "groups"]).optional(),
  q: z.string().optional(),
});

export const Route = createFileRoute("/people")({
  validateSearch: (search) => Search.parse(search),
  loaderDeps: ({ search }) => ({ id: search.id, filter: search.filter, q: search.q }),
  loader: async ({ deps }) =>
    deps.id ? { person: await getPersonFn({ data: { id: deps.id } }), list: null } : { person: null, list: await getPeopleFn({ data: { filter: deps.filter, q: deps.q } }) },
  component: People,
});

type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;
type List = NonNullable<Awaited<ReturnType<typeof getPeopleFn>>>;
type Hub = Awaited<ReturnType<typeof getPersonFn>>;

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
const bday = (b: string) => new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", timeZone: "UTC" }).format(new Date(`2000-${b.slice(-5)}T12:00:00Z`));

function People() {
  const { person, list } = Route.useLoaderData();
  const router = useRouter();
  const [notice, setNotice] = useState<string | null>(null);
  const act: Act = async (fn, done) => {
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err));
    }
    await router.invalidate();
  };

  return (
    <div className="space-y-5 pb-4">
      <PageHead title="People" sub={person ? person.entity.name : list ? `${list.total} shown` : undefined}>
        <GlobalActions />
      </PageHead>
      <Notice text={notice} onClose={() => setNotice(null)} />
      {person ? <PersonHub hub={person} act={act} /> : list ? <PeopleList list={list} act={act} /> : null}
    </div>
  );
}

function Cadence({ id, name, days, act }: { id: string; name: string; days: number | null; act: Act }) {
  return (
    <select
      className="input w-[150px] py-1"
      aria-label={`Keep in touch with ${name}`}
      value={days ?? ""}
      onChange={(e) => act(() => setPersonFn({ data: { id, keepInTouchDays: e.target.value ? Number(e.target.value) : null } }))}
    >
      {CADENCES.map(([label, d]) => (
        <option key={label} value={d ?? ""}>
          {label}
        </option>
      ))}
      {days !== null && !CADENCES.some(([, d]) => d === days) ? <option value={days}>Every {days} days</option> : null}
    </select>
  );
}

function PeopleList({ list, act }: { list: List; act: Act }) {
  const search = Route.useSearch();
  const navigate = useNavigate({ from: "/people" });
  const [q, setQ] = useState(search.q ?? "");
  const filter = search.filter ?? "important";
  const { people, total } = list;

  return (
    <Panel
      title={filter === "groups" ? "Groups" : "People"}
      aside={total === 0 ? undefined : `${total} ${filter === "groups" ? (total === 1 ? "group" : "groups") : total === 1 ? "person" : "people"}${total > people.length ? `, showing ${people.length}` : ""}`}
    >
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <div className="flex flex-wrap border border-edge">
          {FILTERS.map((f) => (
            <Link
              key={f.id}
              to="/people"
              search={{ filter: f.id === "important" ? undefined : f.id, q: search.q }}
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
            void navigate({ search: (s) => ({ ...s, q: q.trim() || undefined }) });
          }}
        >
          <input className="input" placeholder="Find by name, email, or number" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Find a person" />
          <button className="btn" type="submit">
            Find
          </button>
        </form>
      </div>

      {total === 0 ? (
        <Empty>{filter === "important" && !search.q ? "No one yet. People arrive from Google Contacts once LoR is signed in, and the agent adds people who matter." : "No one matches."}</Empty>
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
                    <Link to="/people" search={{ id: p.id }} className="link font-medium">
                      {p.name}
                    </Link>
                    {p.kind !== "person" ? <span className="chip ml-2">{p.kind}</span> : null}
                    {p.page ? <span className="ml-2 text-[12.5px] text-dim">has a page</span> : null}
                    {p.identifiers.length > 0 ? <span className="block max-w-[340px] truncate text-[12.5px] text-dim">{p.identifiers.map((i) => i.value).join(" · ")}</span> : null}
                  </td>
                  <td className="py-2.5 pr-4">
                    <LastTouch p={p} />
                  </td>
                  <td className="py-2.5 pr-4 text-mute">
                    <Birthday p={p} />
                  </td>
                  <td className="py-2 pr-4">
                    <Cadence id={p.id} name={p.name} days={p.keep_in_touch_days} act={act} />
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

type Person = List["people"][number];

function LastTouch({ p }: { p: Person }) {
  return (
    <>
      {p.last.at ? (
        <>
          <Ago iso={p.last.at} />
          {p.last.via ? <span className="block text-[12.5px] text-dim">{VIA[p.last.via]}</span> : null}
        </>
      ) : (
        <span className="text-dim">Not seen yet</span>
      )}
      {p.overdue_by !== null && p.overdue_by > 0 ? <span className="block text-[12.5px] text-or">{p.overdue_by} days past the cadence</span> : null}
    </>
  );
}

function Birthday({ p }: { p: Person }) {
  if (!p.birthday) return <span className="text-dim">—</span>;
  return (
    <>
      {bday(p.birthday)}
      {p.birthday_in !== null && p.birthday_in <= 30 ? (
        <span className="block text-[12.5px] text-or">
          {p.birthday_in === 0 ? "today" : `in ${p.birthday_in} days`}
          {p.turning ? `, turning ${p.turning}` : ""}
        </span>
      ) : null}
    </>
  );
}

function PersonHub({ hub, act }: { hub: Hub; act: Act }) {
  const { entity: e, person: p, page, waiting, today } = hub;
  const clock = (iso: string) =>
    new Intl.DateTimeFormat("en-GB", { timeZone: hub.timezone, day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(iso));
  const channels = e.last_contact
    ? (["mail_in", "mail_out", "meeting", "phone"] as const).filter((c) => e.last_contact![c]).map((c) => [VIA[c]!, e.last_contact![c]!] as const)
    : [];

  return (
    <div className="space-y-[22px]">
      <p className="text-[14px]">
        <Link to="/people" className="text-mute hover:text-txt">
          ← Everyone
        </Link>
      </p>
      <div className="grid items-start gap-[22px] xl:grid-cols-[380px_minmax(0,1fr)]">
        <div className="space-y-[22px]">
          <Panel title={e.name} aside={e.kind !== "person" ? e.kind : undefined}>
            {e.aliases.length > 0 ? <p className="text-[13px] text-dim">also {e.aliases.join(", ")}</p> : null}
            <ul className="mt-1 text-[13.5px] text-mute">
              {e.identifiers.map((i) => (
                <li key={`${i.kind}:${i.value}`} className="truncate">
                  {i.value}
                </li>
              ))}
            </ul>
            <dl className="mt-4 grid grid-cols-[120px_1fr] items-baseline gap-x-3 gap-y-2 text-[14px]">
              <dt className="cond text-[12.5px] font-medium text-dim">Last in touch</dt>
              <dd>{p ? <LastTouch p={p} /> : <span className="text-dim">Not seen yet</span>}</dd>
              {channels.length > 1
                ? channels.map(([label, at]) => (
                    <div key={label} className="contents text-[13px] text-mute">
                      <dt className="pl-2">{label}</dt>
                      <dd>
                        <Ago iso={at} />
                      </dd>
                    </div>
                  ))
                : null}
              <dt className="cond text-[12.5px] font-medium text-dim">Birthday</dt>
              <dd>{p ? <Birthday p={p} /> : "—"}</dd>
              <dt className="cond text-[12.5px] font-medium text-dim">Keep in touch</dt>
              <dd>
                <Cadence id={e.id} name={e.name} days={e.keep_in_touch_days} act={act} />
              </dd>
              <dt className="cond text-[12.5px] font-medium text-dim">Their mail</dt>
              <dd>
                <label className="flex items-center gap-2 text-mute">
                  <input type="checkbox" checked={e.priority} onChange={(ev) => act(() => setPersonFn({ data: { id: e.id, priority: ev.target.checked } }))} />
                  goes straight to the agent
                </label>
              </dd>
            </dl>
          </Panel>

          <Panel title="Waiting on them" aside={waiting.length || undefined}>
            {waiting.length === 0 ? <Empty>Nothing is waiting on {e.name.split(" ")[0]}.</Empty> : null}
            <ul>
              {waiting.map((w) => (
                <li key={w.id} className="flex items-baseline justify-between gap-2 border-t border-line py-1.5 first:border-t-0">
                  <Link to="/tasks" search={{ view: w.status === "later" ? "later" : w.status === "waiting" ? "waiting" : undefined, id: w.id }} className="hover:text-or">
                    {w.title}
                  </Link>
                  {w.due ? <span className={`shrink-0 text-[12.5px] ${w.due === today ? "text-or" : "text-dim"}`}>{dueLabel(w.due, today)}</span> : null}
                </li>
              ))}
            </ul>
          </Panel>

          {e.members.length > 0 || e.groups.length > 0 ? (
            <Panel title={e.kind === "group" ? "Members" : "Groups"} aside={e.members.length || e.groups.length}>
              <ul className="text-[14px]">
                {e.kind === "group"
                  ? e.members.map((m) => (
                      <li key={m.identifier} className="py-0.5">
                        {m.entity_id ? (
                          <Link to="/people" search={{ id: m.entity_id }} className="link">
                            {m.name}
                          </Link>
                        ) : (
                          <span className="text-mute">{m.identifier}</span>
                        )}
                      </li>
                    ))
                  : e.groups.map((g) => (
                      <li key={g.id} className="py-0.5">
                        <Link to="/people" search={{ id: g.id }} className="link">
                          {g.name}
                        </Link>
                      </li>
                    ))}
              </ul>
            </Panel>
          ) : null}
        </div>

        <div className="min-w-0 space-y-[22px]">
          <Panel
            title={page ? "Their page" : "No page yet"}
            aside={
              page ? (
                <Link to="/notes" search={{ slug: page.slug }} className="btn btn-sm">
                  Open in Notes
                </Link>
              ) : undefined
            }
            bodyClass="px-7 py-5"
          >
            {page ? (
              <>
                <Markdown source={page.body} />
                <p className="mt-4 text-[13px] text-dim">
                  last written by {page.via} · <Ago iso={page.written_at} />
                </p>
              </>
            ) : (
              <Empty>The agent writes a page for someone once there is something worth keeping about them.</Empty>
            )}
          </Panel>

          {e.recent_messages.length > 0 ? (
            <Panel title="Messages" aside="latest">
              <ul>
                {e.recent_messages.map((m, i) => (
                  <li key={i} className="grid grid-cols-[110px_minmax(0,1fr)] gap-x-3 border-t border-line py-1.5 text-[14px] first:border-t-0">
                    <span className="text-[13px] text-mute">{clock(m.at)}</span>
                    <span className="min-w-0">
                      <span className={`text-[12.5px] ${m.direction === "out" ? "text-mute" : "text-cy"}`}>
                        {m.direction === "out" ? "you" : (m.from ?? e.name)}
                        {m.group ? ` in ${m.group}` : ""}
                        {m.kind === "whatsapp" ? " · WhatsApp" : " · text"}
                      </span>
                      <span className="block break-words">{m.text ?? <span className="text-dim">(no text)</span>}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </Panel>
          ) : null}

          {e.recent_mail.length > 0 ? (
            <Panel title="Mail" aside="latest, headers only">
              <ul>
                {e.recent_mail.map((m) => (
                  <li key={m.gmail_id} className="grid grid-cols-[110px_60px_minmax(0,1fr)] gap-x-3 border-t border-line py-1.5 text-[14px] first:border-t-0">
                    <span className="text-[13px] text-mute">{clock(m.at)}</span>
                    <span className="text-[12.5px] text-dim">{m.direction === "sent" ? "you sent" : "they sent"}</span>
                    <a href={`https://mail.google.com/mail/u/0/#all/${m.gmail_id}`} target="_blank" rel="noreferrer" className="truncate hover:text-or">
                      {m.subject ?? "(no subject)"}
                    </a>
                  </li>
                ))}
              </ul>
            </Panel>
          ) : null}
        </div>
      </div>
    </div>
  );
}
