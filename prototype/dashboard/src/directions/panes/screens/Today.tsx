// Today: the day's list. The timer's row first, then what wants him (the agent's flags, his
// tasks due), the morning's brief, the time still to label, the rest of the calendar, and what
// is done. Each row's common action is on the row; anything opened goes to the detail pane. At
// rest from 1280px the detail side holds the day's overview: the brief in the morning, the day
// so far and the week in the afternoon.
import { useState } from "react";
import { doneOn, weekGoals } from "../../../data/derive.js";
import { useNow, useStore } from "../../../data/store.js";
import type { Route } from "../../../shell/nav.js";
import { blockDetail, eventDetail, taskDetail, timerDetail } from "../details.js";
import { closeHere, dueNow, flags, fmtH, hm, longDay, minutes, modeOf, openHere, phref, useFit, useLocal } from "../lib.js";
import { openField } from "../omnibox.js";
import { type Detail, ListHead, Panes } from "../panes.js";
import { BlockRow, EventRow, FlagRow, TaskRow, wantsLabel } from "../rows.js";
import { TimerRow } from "../timer.js";
import { Button, cx, Group, Icon, Prose } from "../ui.js";

const OPEN = ["id", "block", "timer", "event", "brief"];

export function Today({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(30_000);
  const l = useLocal();
  const fit = useFit();
  const p = route.params;
  const stale = modeOf(p) === "stale";
  const close = () => closeHere(OPEN);
  const open = (patch: Record<string, string>) => openHere(patch, OPEN);

  const fl = flags(s, modeOf(p), l);
  const due = dueNow(s, new Set(fl.map((f) => f.id)));
  const morning = Number(hm(now).slice(0, 2)) < 12;
  const later = s.day.planned.filter((e) => !e.all_day && Date.parse(e.t_end) > now);
  const [allTime, setAllTime] = useState(false);
  const toLabel = s.blocks.filter(wantsLabel);
  const timeRows = allTime ? s.blocks : toLabel;
  const doneToday = doneOn(s, s.day.date).filter((t) => {
    const at = s.nodes.find((x) => x.id === t.id)?.done_at;
    return at && Date.parse(at) <= now;
  });
  const [showDone, setShowDone] = useState(false);
  const quiet = fl.length === 0 && due.length === 0;

  // What is open, found in the store (not in the visible list), so it stays open when a change
  // moves its row: a task given a later due date leaves Due but its pane stays.
  let detail: Detail | null = null;
  if (p.id) {
    const n = s.nodes.find((x) => x.title === p.id);
    if (n) detail = taskDetail(n, close, { flag: fl.find((f) => f.id === n.id), stale });
  } else if (p.block) {
    const list = s.blocks.some((b) => hm(b.t_start) === p.block && wantsLabel(b)) && !allTime ? toLabel : s.blocks;
    const i = list.findIndex((b) => hm(b.t_start) === p.block);
    const b = list[i];
    if (b) {
      const step = (d: 1 | -1) => {
        const next = list[i + d];
        if (next) open({ block: hm(next.t_start) });
      };
      detail = blockDetail(b, close, { stale, onStep: step, first: i === 0, last: i === list.length - 1 });
    }
  } else if (p.timer) detail = timerDetail(close, stale);
  else if (p.event) {
    const e = s.day.planned.find((x) => x.title === p.event);
    if (e) detail = eventDetail(e, close, stale);
  } else if (p.brief && s.brief) {
    detail = { key: "brief", kicker: morning ? "The brief · this morning" : "This morning's brief", title: longDay(s.day.date), close, body: <Prose source={s.brief} /> };
  }

  const head = (
    <ListHead
      title="Today"
      sub={`${longDay(s.day.date)} · ${hm(now)}`}
      tools={
        <Button tone="quiet" size="sm" disabled={stale} onClick={() => openField("add")} title="Add a task (q)">
          <Icon name="plus" /> Add
        </Button>
      }
    />
  );

  const list = (
    <>
      <TimerRow stale={stale} selected={Boolean(p.timer)} onOpen={() => open({ timer: "1" })} initial={p.switch === "1" ? "switch" : undefined} />

      {quiet ? (
        <div className="p-calm" role="status">
          <h2 className="p-calm__title">Nothing wants you.</h2>
          <p className="p-calm__text">
            No flags and nothing due today.
            {later[0] ? ` Next on the calendar: ${hm(later[0].t_start)} ${later[0].title ?? "an event"}.` : " Nothing else on the calendar today."}
          </p>
        </div>
      ) : null}

      {fl.length ? (
        <Group label="Needs you" count={fl.length} aside={modeOf(p) === "crowded" ? "the agent flagged these" : undefined}>
          <ul className="p-rows">
            {fl.map((f) => (
              <FlagRow key={f.id} flag={f} sel={p.id === f.title} onOpen={() => open({ id: f.title })} stale={stale} />
            ))}
          </ul>
        </Group>
      ) : null}

      {s.brief && morning ? <BriefRow body={s.brief} sel={Boolean(p.brief)} onOpen={() => open({ brief: "1" })} /> : null}

      {due.length ? (
        <Group label="Due" count={due.length}>
          <ul className="p-rows">
            {due.map((n) => (
              <TaskRow key={n.id} node={n} sel={p.id === n.title} onOpen={() => open({ id: n.title })} stale={stale} />
            ))}
          </ul>
        </Group>
      ) : null}

      <Group
        label={allTime ? "Today's time" : "Time to label"}
        count={allTime ? s.blocks.length : toLabel.length}
        aside={
          s.blocks.length ? (
            <button type="button" className="p-link" onClick={() => setAllTime(!allTime)} aria-pressed={allTime}>
              {allTime ? "Only what needs a label" : "Show all"}
            </button>
          ) : undefined
        }
      >
        {s.blocks.length === 0 ? (
          <p className="p-empty">Nothing recorded yet. A timer, your phone and the calendar fill this in as the day goes.</p>
        ) : timeRows.length === 0 ? (
          <p className="p-empty">All of today is labelled.</p>
        ) : (
          <ul className="p-rows">
            {timeRows.map((b) => (
              <BlockRow key={b.id} b={b} sel={p.block === hm(b.t_start)} onOpen={() => open({ block: hm(b.t_start) })} stale={stale} />
            ))}
          </ul>
        )}
      </Group>

      {later.length ? (
        <Group label="Later today" count={later.length}>
          <ul className="p-rows">
            {later.map((e) => (
              <EventRow key={e.event_id} p={e} sel={p.event === e.title} onOpen={() => open({ event: e.title ?? "" })} />
            ))}
          </ul>
        </Group>
      ) : null}

      {doneToday.length ? (
        <section className="p-group">
          <h3 className="p-group__label">
            <button type="button" className="p-group__toggle" aria-expanded={showDone} onClick={() => setShowDone(!showDone)}>
              <Icon name="chev" /> Done today <span className="p-count">{doneToday.length}</span>
            </button>
          </h3>
          {showDone ? (
            <ul className="p-rows">
              {doneToday.map((t) => {
                const n = s.nodes.find((x) => x.id === t.id)!;
                return <TaskRow key={t.id} node={n} sel={p.id === n.title} onOpen={() => open({ id: n.title })} stale={stale} showDone />;
              })}
            </ul>
          ) : null}
        </section>
      ) : null}

      {s.brief && !morning ? <BriefRow body={s.brief} sel={Boolean(p.brief)} onOpen={() => open({ brief: "1" })} afternoon /> : null}
    </>
  );

  return <Panes label="Today" head={head} list={list} detail={detail} rest={<Overview inList={fit !== "wide"} morning={morning} toLabel={toLabel.length} onLabel={() => toLabel[0] && open({ block: hm(toLabel[0].t_start) })} />} sheet={p.sheet} restInList />;
}

function BriefRow({ body, sel, onOpen, afternoon }: { body: string; sel: boolean; onOpen: () => void; afternoon?: boolean }) {
  const first = (body.split("\n\n").find((x) => !x.startsWith("#")) ?? body).replace(/[*_#>[\]]/g, "");
  return (
    <section className="p-group" aria-label={afternoon ? "This morning's brief" : "The brief"}>
      <h3 className="p-group__label">
        <span>{afternoon ? "This morning's brief" : "The brief"}</span>
      </h3>
      <button type="button" className={cx("p-brief", sel && "is-sel", afternoon && "is-late")} data-sel={sel} aria-current={sel || undefined} onClick={onOpen}>
        <Icon name="brief" />
        <span className="p-brief__text">{first}</span>
        <span className="p-brief__more">Read it</span>
      </button>
    </section>
  );
}

/**
 * The day's overview: what the detail side shows when nothing is open, from 1280px. Below that it
 * sits at the foot of the list, without the brief, which the list already has.
 */
function Overview({ inList, morning, toLabel, onLabel }: { inList: boolean; morning: boolean; toLabel: number; onLabel: () => void }) {
  const s = useStore();
  const now = useNow(60_000);
  const goals = weekGoals(s);
  const recorded = s.blocks.reduce((m, b) => m + (b.t_end ? Date.parse(b.t_end) : now) - Date.parse(b.t_start), 0);
  const labelled = s.blocks.filter((b) => b.committed || !b.t_end).reduce((m, b) => m + (b.t_end ? Date.parse(b.t_end) : now) - Date.parse(b.t_start), 0);
  const next = s.day.planned.find((e) => !e.all_day && Date.parse(e.t_start) > now);
  const left = Math.max(0, Date.parse(s.day.wake_end) - now);
  return (
    <div className={cx("p-overview", inList && "is-inlist")}>
      {!inList ? <p className="p-kicker">{morning && s.brief ? "The brief" : "The day so far"}</p> : null}
      {!inList && morning && s.brief ? (
        <div className="p-overview__brief">
          <Prose source={s.brief} />
        </div>
      ) : null}
      <div className="p-stats" role="group" aria-label="The day in numbers">
        <div className="p-stat">
          <span className="p-stat__num">{recorded ? minutes(recorded) : "None yet"}</span>
          <span className="p-stat__label">recorded</span>
        </div>
        <div className="p-stat">
          <span className="p-stat__num">{recorded ? `${Math.round((labelled / recorded) * 100)}%` : "–"}</span>
          <span className="p-stat__label">labelled</span>
        </div>
        <div className="p-stat">
          <span className="p-stat__num">{minutes(left)}</span>
          <span className="p-stat__label">awake left</span>
        </div>
      </div>
      {toLabel ? (
        <button type="button" className="p-overview__ask" onClick={onLabel}>
          <span>
            {toLabel} stretch{toLabel > 1 ? "es" : ""} of time to label
          </span>
          <Icon name="right" />
        </button>
      ) : null}
      {next ? (
        <p className="p-overview__line">
          <Icon name="cal" /> Next: {hm(next.t_start)} {next.title ?? "an event"}, in {minutes(Date.parse(next.t_start) - now)}
        </p>
      ) : null}
      {goals.length ? (
        <section className="p-week" aria-label="This week">
          <h3 className="p-group__label">
            <a href={phref("/goals")}>This week</a>
          </h3>
          <ul>
            {goals.map((g) => (
              <li key={g.id}>
                <a href={phref("/goals", { id: g.title })} className="p-week__row">
                  <span className="p-week__name">{g.title}</span>
                  {g.hours > 0 ? (
                    <>
                      <span className="p-meter" aria-hidden="true">
                        <span className="p-meter__fill" style={{ width: `${Math.min(100, (g.hours / g.target) * 100)}%` }} />
                      </span>
                      <span className="p-week__num">
                        {fmtH(g.hours)} of {fmtH(g.target)} h
                      </span>
                    </>
                  ) : (
                    <span className="p-week__nodata">no data yet</span>
                  )}
                </a>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
      {!inList ? <p className="p-overview__hint">Open anything on the left to see it here.</p> : null}
    </div>
  );
}

