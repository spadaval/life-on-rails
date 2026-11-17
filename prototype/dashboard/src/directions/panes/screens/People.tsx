// People: the list, filtered, and a person in the detail pane: when you were last in touch and
// how, what waits on them, the latest messages and mail, their page, and Ask before a call.
// Their cadence and priority are under More. A task waiting on them stacks over the person.
import { useState } from "react";
import { PEOPLE_FILTERS, peopleIn } from "../../../data/derive.js";
import { askAgent, setPerson, useNow, useStore } from "../../../data/store.js";
import type { Person } from "../../../data/types.js";
import type { Route } from "../../../shell/nav.js";
import { taskDetail } from "../details.js";
import { act, ago, closeHere, findNode, firstName, hm, initials, modeOf, openHere, phref, STALE } from "../lib.js";
import { type Detail, ListHead, More, Panes } from "../panes.js";
import { Button, cx, Group, Icon, Prose } from "../ui.js";

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
const OPEN = ["id", "task"];

function cadence(p: Person): { text: string; late: boolean } | null {
  if (p.birthday_in !== null && p.birthday_in <= 30) return { text: p.birthday_in === 0 ? "birthday today" : `birthday in ${p.birthday_in} days`, late: false };
  if (p.keep_in_touch_days === null) return null;
  if (p.overdue_by !== null && p.overdue_by > 0) return { text: p.days_since === null ? "due a first hello" : `${p.overdue_by} days past`, late: true };
  return null;
}

export function People({ route }: { route: Route }) {
  const s = useStore();
  const p = route.params;
  const stale = modeOf(p) === "stale";
  const [q, setQ] = useState("");
  const target = p.id ? (s.people.find((x) => x.id === p.id) ?? s.people.find((x) => x.name === p.id)) : undefined;
  const filter = p.filter ?? (target && !peopleIn(s, "important").some((x) => x.id === target.id) ? "all" : "important");
  const people = peopleIn(s, filter, q);
  const task = target ? findNode(s.nodes, p.task, (n) => n.type === "task") : undefined;

  const detail: Detail | null = target
    ? {
        key: `person-${target.id}`,
        kicker: target.kind === "person" ? "Person" : target.kind,
        title: target.name,
        close: () => closeHere(OPEN),
        actions: (
          <Button tone="quiet" disabled={stale} title={stale ? STALE : "The agent answers on your phone"} onClick={() => act("Sent to the agent. It answers on your phone.", () => askAgent(`What should I know before I talk to ${target.name}?`, target.id))}>
            <Icon name="spark" /> Ask the agent before a call
          </Button>
        ),
        body: <PersonBody p={target} stale={stale} openTask={(id) => openHere({ task: id }, ["task"], { sub: true })} />,
      }
    : null;
  const sub = target && task ? taskDetail(task, () => closeHere(["task"], { sub: true }), { stale, backLabel: target.name }) : null;

  const head = (
    <ListHead title="People" sub={`${peopleIn(s, "overdue").length} past their cadence, ${peopleIn(s, "birthdays").length} birthdays this month`}>
      <input className="p-input p-list__filter" type="search" placeholder="Filter by name" aria-label="Filter people by name" value={q} onChange={(e) => setQ(e.target.value)} />
      <nav className="p-filter" aria-label="Which people">
        {PEOPLE_FILTERS.map((f) => (
          <a key={f} className={cx("p-filter__a", filter === f && "is-on")} aria-current={filter === f ? "true" : undefined} href={phref("/people", { filter: f === "important" ? undefined : f, id: p.id })}>
            {NAMES[f]}
            <span className="p-count">{peopleIn(s, f).length}</span>
          </a>
        ))}
      </nav>
    </ListHead>
  );

  const list = people.length ? (
    <ul className="p-rows">
      {people.map((x) => (
        <PersonRow key={x.id} p={x} sel={target?.id === x.id} onOpen={() => openHere({ id: x.id }, OPEN)} />
      ))}
    </ul>
  ) : (
    <p className="p-empty">No one here{q ? ` called “${q}”` : ""}.</p>
  );

  const overdue = peopleIn(s, "overdue").slice(0, 4);
  const soon = peopleIn(s, "birthdays").slice(0, 3);
  const rest = (
    <div className="p-overview">
      <p className="p-kicker">Who to reach</p>
      {overdue.length ? (
        <Group label="Past their cadence" count={peopleIn(s, "overdue").length}>
          <ul className="p-mini">
            {overdue.map((x) => (
              <li key={x.id}>
                <a href={phref("/people", { id: x.id, filter })}>{x.name}</a>
                <span className="p-mini__meta is-late">{x.overdue_by} days past</span>
              </li>
            ))}
          </ul>
        </Group>
      ) : null}
      {soon.length ? (
        <Group label="Birthdays soon">
          <ul className="p-mini">
            {soon.map((x) => (
              <li key={x.id}>
                <a href={phref("/people", { id: x.id, filter })}>{x.name}</a>
                <span className="p-mini__meta">{x.birthday_in === 0 ? "today" : `in ${x.birthday_in} days`}</span>
              </li>
            ))}
          </ul>
        </Group>
      ) : null}
      <p className="p-overview__hint">Open someone before a call to see the latest you heard from them.</p>
    </div>
  );

  return <Panes label="People" head={head} list={list} detail={detail} sub={sub} rest={rest} sheet={p.sheet} restInList />;
}

function PersonRow({ p, sel, onOpen }: { p: Person; sel: boolean; onOpen: () => void }) {
  const now = useNow(60_000);
  const c = cadence(p);
  const via = p.last.via?.replace(/_(in|out)$/, "");
  const last = p.last.at ? `${ago(p.last.at, now)}${via ? ` by ${via}` : ""}` : "never in touch";
  return (
    <li className={cx("p-row", "p-row--person", sel && "is-sel")} data-sel={sel}>
      <span className={cx("p-avatar", p.priority && "is-priority")} aria-hidden="true">
        {initials(p.name)}
      </span>
      <button type="button" className="p-row__open" aria-current={sel || undefined} onClick={onOpen}>
        <span className="p-row__title">{p.name}</span>
        <span className="p-row__meta">{p.kind === "person" ? last : `${p.kind} · ${last}`}</span>
      </button>
      {c ? <span className={cx("p-tag", c.late && "is-late")}>{c.text}</span> : <Icon name="right" />}
    </li>
  );
}

function PersonBody({ p, stale, openTask }: { p: Person; stale?: boolean; openTask: (id: string) => void }) {
  const s = useStore();
  const now = useNow(60_000);
  const hub = s.hubs[p.id];
  const e = hub?.entity;
  const waiting = s.nodes.filter((n) => n.waiting_on_entity_id === p.id && n.status === "waiting");
  const day = (iso: string) => `${new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", timeZone: s.day.timezone })} ${hm(iso)}`;
  const lately = [
    ...(e?.recent_messages ?? []).map((m) => ({ at: m.at, out: m.direction === "out", text: m.text ?? "", via: m.kind })),
    ...(e?.recent_mail ?? []).map((m) => ({ at: m.at, out: m.direction === "sent", text: m.subject ?? "(no subject)", via: "mail" })),
  ]
    .sort((a, b) => b.at.localeCompare(a.at))
    .slice(0, 6);
  const via = p.last.via?.replace(/_(in|out)$/, "");
  const name = firstName(p.name);
  return (
    <>
      <dl className="p-facts">
        <dt>Last in touch</dt>
        <dd>{p.last.at ? `${ago(p.last.at, now)}${via ? `, by ${via}` : ""}` : "Never, that LoR knows of"}</dd>
        {p.keep_in_touch_days ? (
          <>
            <dt>Keep in touch</dt>
            <dd className={cx(p.overdue_by !== null && p.overdue_by > 0 && "is-late")}>
              every {p.keep_in_touch_days} days{p.overdue_by !== null && p.overdue_by > 0 ? `, ${p.overdue_by} days past` : ""}
            </dd>
          </>
        ) : null}
        {p.birthday_in !== null && p.birthday_in <= 60 ? (
          <>
            <dt>Birthday</dt>
            <dd>{p.birthday_in === 0 ? "today" : `in ${p.birthday_in} days`}{p.turning ? `, turning ${p.turning}` : ""}</dd>
          </>
        ) : null}
        {p.identifiers.length ? (
          <>
            <dt>Reach them</dt>
            <dd className="p-facts__ids">
              {p.identifiers.slice(0, 3).map((i) => (
                <span key={i.value} className="p-tag is-plain">
                  {i.value}
                </span>
              ))}
            </dd>
          </>
        ) : null}
      </dl>
      {waiting.length ? (
        <Group label={`Waiting on ${name}`} count={waiting.length}>
          <ul className="p-mini">
            {waiting.map((t) => (
              <li key={t.id}>
                <button type="button" className="p-link" onClick={() => openTask(t.id)}>
                  {t.title}
                </button>
                <span className="p-mini__meta">since {t.waiting_since ? day(t.waiting_since) : "a while"}</span>
              </li>
            ))}
          </ul>
        </Group>
      ) : null}
      <Group label="Lately">
        {lately.length ? (
          <ul className="p-thread">
            {lately.map((m, i) => (
              <li key={i} className={cx("p-msg", m.out && "is-out")}>
                <span className="p-msg__text">{m.text}</span>
                <span className="p-msg__meta">
                  {m.out ? "You" : name} · {m.via} · {day(m.at)}
                </span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="p-empty">Nothing heard from or sent to {name} that LoR knows of.</p>
        )}
      </Group>
      {hub?.page ? (
        <Group label="Their page" aside={<a className="p-link" href={phref("/notes", { id: hub.page.slug })}>Open in Notes</a>}>
          <div className="p-pagecard">
            <Prose source={hub.page.body} />
          </div>
        </Group>
      ) : null}
      <More label="Cadence and priority">
        <fieldset className="p-fields" disabled={stale}>
          <div className="p-field">
            <label htmlFor={`cad-${p.id}`}>Keep in touch</label>
            <select
              id={`cad-${p.id}`}
              className="p-input p-select"
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
          <label className="p-switch">
            <input type="checkbox" checked={p.priority} onChange={(ev) => act(ev.target.checked ? `${p.name} is a priority` : `${p.name} is not a priority`, () => setPerson(p.id, { priority: ev.target.checked }), { undoable: true })} />
            <span className="p-switch__track" aria-hidden="true" />
            Priority: their mail reaches the agent without the usual wait
          </label>
        </fieldset>
      </More>
    </>
  );
}
