// People: the list, filtered, and a person opened in place: when you were last in touch and
// how, their page, recent messages and mail, what waits on them, and their cadence.
import { useEffect, useRef, useState } from "react";
import { PEOPLE_FILTERS, peopleIn } from "../../../data/derive.js";
import { askAgent, setPerson, useNow, useStore } from "../../../data/store.js";
import type { Person } from "../../../data/types.js";
import { setParams, type Route } from "../../../shell/nav.js";
import { act, ago, hm, initials, modeOf, qhref } from "../lib.js";
import { revealOnOpen } from "../lib.js";
import { Button, cx, Icon, Prose } from "../ui.js";

const NAMES: Record<string, string> = { important: "Important", all: "Everyone", "keep-in-touch": "Keep in touch", overdue: "Overdue", birthdays: "Birthdays", groups: "Groups" };
const CADENCES: [string, number | null][] = [
  ["No cadence", null],
  ["Every week", 7],
  ["Every 2 weeks", 14],
  ["Every 3 weeks", 21],
  ["Every month", 30],
  ["Every 6 weeks", 45],
  ["Every 2 months", 60],
  ["Every 3 months", 90],
];

function cadence(p: Person): { text: string; late: boolean } | null {
  if (p.birthday_in !== null && p.birthday_in <= 30) return { text: p.birthday_in === 0 ? "birthday today" : `birthday in ${p.birthday_in} days`, late: false };
  if (p.keep_in_touch_days === null) return null;
  if (p.overdue_by !== null && p.overdue_by > 0) return { text: p.days_since === null ? `every ${p.keep_in_touch_days} days` : `${p.overdue_by} days past`, late: true };
  return { text: `every ${p.keep_in_touch_days} days`, late: false };
}

export function People({ route }: { route: Route }) {
  const s = useStore();
  const stale = modeOf(route.params) === "stale";
  const [q, setQ] = useState("");
  const openName = route.params.id ?? null;
  const target = openName ? s.people.find((p) => p.name === openName) : undefined;
  const filter = route.params.filter ?? (target && !peopleIn(s, "important").some((p) => p.id === target.id) ? "all" : "important");
  const list = peopleIn(s, filter, q);
  return (
    <div className="q-col">
      <header className="q-page">
        <h1 className="q-page__title">People</h1>
        <input className="q-input q-page__filter" type="search" placeholder="Filter by name" aria-label="Filter people by name" value={q} onChange={(e) => setQ(e.target.value)} />
      </header>
      <nav className="q-tabs" aria-label="Which people">
        {PEOPLE_FILTERS.map((f) => (
          <a key={f} className={cx("q-tab", filter === f && "is-on")} aria-current={filter === f ? "page" : undefined} href={qhref("/people", { filter: f === "important" ? undefined : f })}>
            {NAMES[f]} <span className="q-count">{peopleIn(s, f).length}</span>
          </a>
        ))}
      </nav>
      {list.length === 0 ? <p className="q-empty">No one here{q ? ` called “${q}”` : ""}.</p> : null}
      <ul className="q-list">
        {list.map((p) => (
          <PersonRow key={p.id} p={p} open={openName === p.name} onOpen={(v) => setParams({ id: v ? p.name : undefined })} stale={stale} />
        ))}
      </ul>
    </div>
  );
}

function PersonRow({ p, open, onOpen, stale }: { p: Person; open: boolean; onOpen: (v: boolean) => void; stale?: boolean }) {
  const now = useNow(60_000);
  const ref = useRef<HTMLLIElement>(null);
  useEffect(() => {
    if (open) revealOnOpen(ref.current);
  }, [open]);
  const c = cadence(p);
  const via = p.last.via?.replace(/_(in|out)$/, "");
  const last = p.last.at ? `in touch ${ago(p.last.at, now)}${via ? ` by ${via}` : ""}` : "never in touch";
  return (
    <li ref={ref} className={cx("q-row", "q-person", open && "is-open")}>
      <div className="q-row__head">
        <span className={cx("q-avatar", p.priority && "is-priority")} aria-hidden="true">
          {initials(p.name)}
        </span>
        <button type="button" className="q-row__title" aria-expanded={open} onClick={() => onOpen(!open)}>
          <span className="q-row__name">{p.name}</span>
          <span className="q-row__meta">{p.kind === "person" ? last : `${p.kind} · ${last}`}</span>
        </button>
        {c ? <span className={cx("q-person__cad", c.late && "is-late")}>{c.text}</span> : null}
        <button type="button" className="q-row__open" aria-label={open ? "Close" : "Open"} aria-expanded={open} onClick={() => onOpen(!open)}>
          <Icon name="chev" />
        </button>
      </div>
      {open ? <PersonBody p={p} stale={stale} /> : null}
    </li>
  );
}

function PersonBody({ p, stale }: { p: Person; stale?: boolean }) {
  const s = useStore();
  const hub = s.hubs[p.id];
  const e = hub?.entity;
  const waiting = s.nodes.filter((n) => n.waiting_on_entity_id === p.id && n.status === "waiting");
  const day = (iso: string) => {
    const d = new Date(iso);
    return `${d.toLocaleDateString("en-GB", { day: "numeric", month: "short", timeZone: s.day.timezone })} ${hm(iso)}`;
  };
  const messages = e?.recent_messages ?? [];
  const mail = e?.recent_mail ?? [];
  return (
    <div className="q-personbody">
      <div className="q-personbody__ids">
        {p.identifiers.map((i) => (
          <span key={i.value} className="q-tag q-tag--plain">
            {i.value}
          </span>
        ))}
      </div>
      {hub?.page ? (
        <div className="q-sub">
          <h3 className="q-label">
            Their page <a className="q-link" href={qhref("/notes", { id: hub.page.slug })}>Open in Notes</a>
          </h3>
          <Prose source={hub.page.body} />
        </div>
      ) : null}
      {waiting.length ? (
        <div className="q-sub">
          <h3 className="q-label">Waiting on {p.name.split(" ")[0]}</h3>
          <ul className="q-mini">
            {waiting.map((t) => (
              <li key={t.id}>
                <a href={qhref("/tasks", { id: t.title })}>{t.title}</a>
                <span className="q-mini__meta">since {t.waiting_since ? day(t.waiting_since) : "a while"}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      {messages.length || mail.length ? (
        <div className="q-sub">
          <h3 className="q-label">Lately</h3>
          <ul className="q-thread">
            {[
              ...messages.map((m) => ({ at: m.at, out: m.direction === "out", text: m.text, via: m.kind })),
              ...mail.map((m) => ({ at: m.at, out: m.direction === "sent", text: m.subject, via: "mail" })),
            ]
              .sort((a, b) => b.at.localeCompare(a.at))
              .slice(0, 6)
              .map((m, i) => (
                <li key={i} className={cx("q-msg", m.out && "is-out")}>
                  <span className="q-msg__text">{m.text}</span>
                  <span className="q-msg__meta">
                    {m.out ? "You" : p.name.split(" ")[0]} · {m.via} · {day(m.at)}
                  </span>
                </li>
              ))}
          </ul>
        </div>
      ) : (
        <p className="q-note">Nothing heard from or sent to {p.name.split(" ")[0]} that LoR knows of.</p>
      )}
      <div className="q-personbody__set">
        <div className="q-field q-field--inline">
          <label htmlFor={`cad-${p.id}`}>Keep in touch</label>
          <select
            id={`cad-${p.id}`}
            className="q-input q-select"
            disabled={stale}
            value={p.keep_in_touch_days ?? ""}
            onChange={(ev) => {
              const d = ev.target.value === "" ? null : Number(ev.target.value);
              act(d ? `Keep in touch every ${d} days` : "No cadence", () => setPerson(p.id, { keepInTouchDays: d }), { undoable: true });
            }}
          >
            {CADENCES.map(([label, d]) => (
              <option key={label} value={d ?? ""}>
                {label}
              </option>
            ))}
            {p.keep_in_touch_days !== null && !CADENCES.some(([, d]) => d === p.keep_in_touch_days) ? <option value={p.keep_in_touch_days}>Every {p.keep_in_touch_days} days</option> : null}
          </select>
        </div>
        <label className="q-switch">
          <input type="checkbox" disabled={stale} checked={p.priority} onChange={(ev) => act(ev.target.checked ? `${p.name} is a priority` : `${p.name} is not a priority`, () => setPerson(p.id, { priority: ev.target.checked }), { undoable: true })} />
          <span className="q-switch__track" aria-hidden="true" />
          Priority
        </label>
        <Button tone="quiet" disabled={stale} onClick={() => act("Sent to the agent. It answers on your phone.", () => askAgent(`What should I know before I talk to ${p.name}?`, p.id))}>
          <Icon name="spark" /> Ask the agent before a call
        </Button>
      </div>
    </div>
  );
}
