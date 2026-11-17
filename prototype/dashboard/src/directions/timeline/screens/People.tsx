// People: the list, and one person as a page. A person's recent contact is drawn as a short
// route of its own, newest at the top, so "when did we last talk" reads the way Today does.
import { PEOPLE_FILTERS, peopleIn } from "../../../data/derive.js";
import { setPerson, useNow, useStore } from "../../../data/store.js";
import type { Person } from "../../../data/types.js";
import { href, setParams, type Route } from "../../../shell/nav.js";
import { Icon, Prose } from "../components/bits.js";
import { useMode } from "../lib/ctx.js";
import { ago, hm, shortDate, ymd } from "../lib/time.js";
import { reversible } from "../lib/ui.js";

const FILTER_NAMES: Record<string, string> = {
  important: "Important",
  all: "Everyone",
  "keep-in-touch": "Keep in touch",
  overdue: "Overdue",
  birthdays: "Birthdays",
  groups: "Groups",
};

const VIA: Record<string, string> = { mail_in: "mail", mail_out: "mail", phone: "phone", meeting: "a meeting", whatsapp: "WhatsApp", sms: "text" };
const via = (v: string | null) => (v ? ` by ${VIA[v] ?? v}` : "");

const initials = (name: string) =>
  name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();

function cadence(p: Person): { text: string; late: boolean } | null {
  if (p.keep_in_touch_days === null) return null;
  if (p.overdue_by !== null && p.overdue_by > 0) return { text: p.days_since === null ? `Never in touch · every ${p.keep_in_touch_days} days` : `${p.overdue_by} days past every ${p.keep_in_touch_days}`, late: true };
  return { text: `Every ${p.keep_in_touch_days} days`, late: false };
}

export function People({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(60_000);
  const person = route.params.id ? s.people.find((p) => p.name === route.params.id) : undefined;
  if (person) return <PersonPage p={person} />;
  const filter = route.params.filter ?? "important";
  const list = peopleIn(s, filter);
  return (
    <div className="tl-page">
      <header className="tl-pagehead">
        <div>
          <h1>People</h1>
          <p className="tl-muted">{list.length} {FILTER_NAMES[filter]?.toLowerCase() ?? filter}</p>
        </div>
      </header>
      <div className="tl-filters" role="group" aria-label="Show">
        {PEOPLE_FILTERS.map((f) => (
          <button key={f} type="button" className="tl-chip" aria-pressed={f === filter} data-on={f === filter || undefined} onClick={() => setParams({ filter: f === "important" ? undefined : f })}>
            {FILTER_NAMES[f]}
          </button>
        ))}
      </div>
      <ul className="tl-people">
        {list.map((p) => {
          const c = cadence(p);
          return (
            <li key={p.id}>
              <a className="tl-person-row" href={href("/people", { id: p.name })}>
                <span className="tl-avatar" data-priority={p.priority || undefined} aria-hidden="true">
                  {initials(p.name)}
                </span>
                <span className="tl-row-main">
                  <span className="tl-row-title">{p.name}</span>
                  <span className="tl-row-meta">
                    {p.last.at ? `${ago(Date.parse(p.last.at), now)}${via(p.last.via)}` : "No contact recorded"}
                    {p.birthday_in !== null && p.birthday_in <= 30 ? ` · birthday ${p.birthday_in === 0 ? "today" : `in ${p.birthday_in} days`}` : ""}
                  </span>
                  {c ? (
                    <span className="tl-row-meta tl-narrow-only" data-late={c.late || undefined}>
                      {c.text}
                    </span>
                  ) : null}
                </span>
                {c ? (
                  <span className="tl-tag tl-wide-only" data-late={c.late || undefined}>
                    {c.text}
                  </span>
                ) : null}
                <Icon name="chevron" size={16} />
              </a>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function PersonPage({ p }: { p: Person }) {
  const s = useStore();
  const now = useNow(60_000);
  const { staleSince } = useMode();
  const off = staleSince !== null;
  const hub = s.hubs[p.id];
  const c = cadence(p);
  const contact = [
    ...(hub?.entity.recent_messages ?? []).map((m) => ({ at: m.at, kind: m.kind === "whatsapp" ? "WhatsApp" : m.kind === "sms" ? "Text" : m.kind, out: m.direction === "out", text: m.text })),
    ...(hub?.entity.recent_mail ?? []).map((m) => ({ at: m.at, kind: "Mail", out: m.direction === "sent", text: m.subject })),
  ].sort((a, b) => b.at.localeCompare(a.at));
  const waiting = s.nodes.filter((n) => n.waiting_on_entity_id === p.id && n.status !== "done");
  const first = p.name.split(/\s+/)[0]!;
  const mentions = s.nodes.filter((n) => n.type === "task" && n.status !== "done" && n.title.includes(first) && n.waiting_on_entity_id !== p.id);
  let lastDay = "";
  return (
    <div className="tl-page">
      <a className="tl-back" href={href("/people")}>
        <Icon name="back" size={16} /> People
      </a>
      <header className="tl-pagehead tl-personhead">
        <span className="tl-avatar tl-avatar-lg" data-priority={p.priority || undefined} aria-hidden="true">
          {initials(p.name)}
        </span>
        <div>
          <h1>{p.name}</h1>
          <p className="tl-muted">
            {p.last.at ? `Last in touch ${ago(Date.parse(p.last.at), now)}${via(p.last.via)}` : "No contact recorded yet"}
            {c ? <> · <span data-late={c.late || undefined}>{c.text}</span></> : null}
          </p>
        </div>
      </header>
      <div className="tl-personpage">
        <div className="tl-personmain">
          {waiting.length || mentions.length ? (
            <section className="tl-section">
              <h2 className="tl-section-title">Open with {first}</h2>
              <ul className="tl-rows">
                {[...waiting, ...mentions].map((n) => (
                  <li key={n.id} className="tl-row">
                    <a className="tl-row-main" href={href("/tasks", { id: n.title })}>
                      <span className="tl-row-title">{n.title}</span>
                      <span className="tl-row-meta">{n.waiting_on_entity_id === p.id ? `Waiting on ${first}` : (n.parents[0]?.title ?? "Task")}</span>
                    </a>
                    <Icon name="chevron" size={16} />
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
          <section className="tl-section">
            <h2 className="tl-section-title">Recent contact</h2>
            {contact.length ? (
              <ol className="tl-contact">
                {contact.map((m, i) => {
                  const day = ymd(m.at);
                  const showDay = day !== lastDay;
                  lastDay = day;
                  return (
                    <li key={i} data-out={m.out || undefined}>
                      {showDay ? <span className="tl-contact-day">{shortDate(day)}</span> : null}
                      <span className="tl-contact-dot" aria-hidden="true" />
                      <span className="tl-contact-body">
                        <span className="tl-contact-k">
                          <Icon name={m.kind === "Mail" ? "mail" : "message"} size={13} /> {m.kind} · {m.out ? "you" : first} · <span className="tl-num">{hm(m.at)}</span>
                        </span>
                        <span>{m.text}</span>
                      </span>
                    </li>
                  );
                })}
              </ol>
            ) : (
              <p className="tl-muted">No messages, mail or calls recorded with {first}.</p>
            )}
          </section>
          {hub?.page ? (
            <section className="tl-section">
              <h2 className="tl-section-title">
                Notes <a className="tl-link" href={href("/notes", { slug: hub.page.slug })}>open in Notes</a>
              </h2>
              <Prose text={hub.page.body} className="tl-prose tl-card" />
            </section>
          ) : null}
        </div>
        <aside className="tl-personside">
          <section className="tl-card tl-facts">
            <h2 className="tl-section-title">Keep in touch</h2>
            <select
              className="tl-input"
              aria-label="Keep in touch"
              value={p.keep_in_touch_days ?? ""}
              disabled={off}
              onChange={(e) => {
                const d = e.target.value ? Number(e.target.value) : null;
                reversible(d ? `${first}: every ${d} days` : `${first}: no cadence`, () => setPerson(p.id, { keepInTouchDays: d }));
              }}
            >
              <option value="">No cadence</option>
              {[7, 14, 30, 45, 90].map((d) => (
                <option key={d} value={d}>
                  Every {d} days
                </option>
              ))}
            </select>
            <label className="tl-toggle">
              <input type="checkbox" checked={p.priority} disabled={off} onChange={(e) => reversible(e.target.checked ? `${first} marked important` : `${first} no longer important`, () => setPerson(p.id, { priority: e.target.checked }))} />
              <span>Important</span>
            </label>
            <h2 className="tl-section-title">Reach</h2>
            <ul className="tl-facts-list">
              {p.identifiers.map((i) => (
                <li key={i.value}>
                  <small>{i.kind === "email" ? "Email" : i.kind === "phone" ? "Phone" : i.kind}</small>
                  <span className={i.kind === "phone" ? "tl-num" : undefined}>{i.value}</span>
                </li>
              ))}
              {p.birthday ? (
                <li>
                  <small>Birthday</small>
                  <span>
                    {p.birthday}
                    {p.birthday_in !== null ? ` · in ${p.birthday_in} days` : ""}
                  </span>
                </li>
              ) : null}
            </ul>
          </section>
        </aside>
      </div>
    </div>
  );
}
