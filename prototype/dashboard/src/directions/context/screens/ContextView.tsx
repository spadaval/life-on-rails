// The context surface: one raised surface with everything about the thing being worked on. Its
// title and the timer, then the next task (what leads), then its regions on their own tones:
// tasks, the agent's work, its page, its people, its time. Uncommon things (cadence, targets)
// sit folded at the bottom. Every kind of context uses this one surface.
import { type ReactNode } from "react";
import { doneOn, isOpen } from "../../../data/derive.js";
import { answerProposal, habitDone, link, relabel as relabelBlock, setPerson, startTimer, switchTimer, today as todayOf, useNow, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import type { Route } from "../../../shell/nav.js";
import { Check, Icon, kindIcon, Prose } from "../components/bits.js";
import { anchorFrom } from "../components/Layer.js";
import { Picker } from "../components/Picker.js";
import { TaskRow } from "../components/TaskRow.js";
import { runningLabel, stop, useElapsed } from "../components/Timer.js";
import {
  blockName, blockSource, type Ctx, ctxPath, eventNow, homeOf, members, nextEvent, ONE_THING, pageOf, pagesNaming, peopleOf, runningBlock, sameCtx, blockCtx, SUGGEST, timeOf, type Why,
} from "../lib/model.js";
import { href, navigate, setParams } from "../lib/nav.js";
import { ago, clock, dueWords, dur, hm, hours } from "../lib/time.js";
import { oneWay, reversible, useOff } from "../lib/ui.js";

const WHY: Record<Why, string> = {
  timer: "The timer is running here",
  stopped: "You just stopped here",
  one: "The one thing, from this morning’s brief",
  recent: "Where you left off",
  none: "Nothing is chosen",
};

export function ContextView({ ctx, why, focus, route, isNow }: { ctx: Ctx; why?: Why; focus?: Node | null; route: Route; isNow?: boolean }) {
  const s = useStore();
  const now = useNow(1000);
  const p = route.params;
  const today = todayOf();
  const nodes = members(s, ctx);
  const tasks = nodes.filter((n) => n.type === "task");
  const open = tasks
    .filter((n) => isOpen(n) && n.assignee !== "agent")
    .sort((a, b) => Number(!a.flagged_at) - Number(!b.flagged_at) || (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title));
  const lead = (focus && isOpen(focus) && open.find((n) => n.id === focus.id)) || open[0] || null;
  const rest = open.filter((n) => n !== lead);
  const later = tasks.filter((n) => n.status === "later");
  const agent = tasks.filter((n) => n.assignee === "agent" && isOpen(n));
  const done = doneOn(s, today).filter((t) => tasks.some((n) => n.id === t.id));
  const habits = nodes.filter((n) => n.type === "habit" && n.status === "active");
  const sub = nodes.filter((n) => n.type === "project" && n.status === "active");
  const page = pageOf(s, ctx);
  const people = peopleOf(s, ctx, nodes);
  const time = timeOf(s, ctx, nodes, now);
  const r = runningBlock(s);
  const runHere = r ? sameCtx(blockCtx(s, r), ctx) || (ctx.kind === "unnamed" && !r.intent_id) : false;
  const parent = ctx.node?.parents[0];
  const openPage = (slug: string) => setParams({ page: slug });
  const taskParam = p.task;

  return (
    <article className="cx-surface" data-kind={ctx.kind} aria-labelledby="cx-ctx-title" key={`${ctx.kind}:${ctx.key}`}>
      <header className="cx-head">
        <div className="cx-head-text">
          <p className="cx-kicker">
            <span className="cx-kind">
              <Icon name={kindIcon(ctx.kind)} size={14} />
              {ctx.kind === "project" ? "Project" : ctx.kind === "goal" ? "Goal" : ctx.kind === "person" ? "Person" : ctx.kind === "slot" ? "Next fixed event" : ctx.kind === "loose" ? "Under nothing yet" : "Timer"}
            </span>
            {parent ? (
              <span className="cx-kicker-part">
                <span className="cx-sep" aria-hidden="true">·</span>
                <a className="cx-kicker-link" href={href("/goals", { id: parent.title })}>
                  {parent.title}
                </a>
              </span>
            ) : null}
            {isNow && why ? (
              <span className="cx-kicker-part">
                <span className="cx-sep" aria-hidden="true">·</span>
                <span className="cx-why" data-why={why}>{WHY[why]}</span>
              </span>
            ) : null}
          </p>
          <h1 id="cx-ctx-title" className="cx-title">{ctx.title}</h1>
          {ctx.kind === "person" && ctx.person ? <PersonLine ctx={ctx} /> : null}
          {ctx.kind === "slot" && ctx.event ? (
            <p className="cx-subline cx-num">
              {hm(ctx.event.t_start)}–{hm(ctx.event.t_end)} · {Date.parse(ctx.event.t_start) > now ? `in ${dur(Date.parse(ctx.event.t_start) - now)}` : "now"}
            </p>
          ) : null}
        </div>
        <TimerHead ctx={ctx} lead={lead} runHere={runHere} />
      </header>

      {ctx.kind === "unnamed" ? <UnnamedBody /> : null}

      {lead ? (
        <section className="cx-lead" aria-label="Next">
          <div className="cx-lead-head">
            <h2 className="cx-label">{why === "one" && lead.title === ONE_THING.title ? "The one thing" : lead.flagged_at ? "Next · flagged by the agent" : "Next"}</h2>
            {ctx.node && !rest.length ? <AddHere /> : null}
          </div>
          <ul className="cx-tasks cx-tasks-lead">
            <TaskRow node={lead} open={taskParam === lead.title} lead />
          </ul>
          {why === "one" && lead.title === ONE_THING.title ? <p className="cx-lead-why">{ONE_THING.why}</p> : null}
        </section>
      ) : ctx.kind !== "unnamed" && ctx.kind !== "slot" ? (
        <section className="cx-lead cx-lead-empty">
          <p>{ctx.person ? `No open task names ${ctx.person.name.split(" ")[0]}.` : <>Nothing open here. {ctx.node ? "Add the next step with q, or let it rest." : null}</>}</p>
        </section>
      ) : null}

      <div className="cx-regions">
        {ctx.kind === "slot" && ctx.event ? <SlotRegion ctx={ctx} /> : null}

        {rest.length || sub.length || later.length || done.length || (ctx.node && !lead) || ctx.kind === "loose" ? (
          <Region title="Tasks" count={rest.length} wide action={ctx.node ? <AddHere /> : null}>
            {rest.length ? (
              <ul className="cx-tasks">
                {rest.map((n) => (
                  <div key={n.id} className="cx-task-wrap">
                    <TaskRow node={n} open={taskParam === n.title} showHome={ctx.kind === "person" || ctx.kind === "slot"} />
                    {ctx.kind === "loose" && SUGGEST[n.title] ? <Suggest node={n} goal={SUGGEST[n.title]!} /> : null}
                  </div>
                ))}
              </ul>
            ) : (
              <p className="cx-empty-line">{lead ? "Only the next one." : "No open tasks."}</p>
            )}
            {sub.length ? (
              <div className="cx-subctx">
                <span className="cx-label">Projects in it</span>
                <div className="cx-chips">
                  {sub.map((n) => (
                    <a key={n.id} className="cx-ctxchip" href={href("/project", { id: n.title })}>
                      <Icon name="project" size={14} /> {n.title}
                    </a>
                  ))}
                </div>
              </div>
            ) : null}
            {later.length ? (
              <details className="cx-fold">
                <summary>{later.length} for later</summary>
                <ul className="cx-tasks">
                  {later.map((n) => (
                    <TaskRow key={n.id} node={n} open={taskParam === n.title} />
                  ))}
                </ul>
              </details>
            ) : null}
            {done.length ? (
              <div className="cx-donelist">
                <span className="cx-label">Done today</span>
                <ul className="cx-tasks">
                  {done.map((t) => {
                    const n = s.nodes.find((x) => x.id === t.id)!;
                    return <TaskRow key={n.id} node={n} open={taskParam === n.title} />;
                  })}
                </ul>
              </div>
            ) : null}
          </Region>
        ) : null}

        {agent.length ? (
          <Region title="Given to the agent" count={agent.length} tone="cyan">
            <ul className="cx-agent">
              {agent.map((n) => (
                <li key={n.id}>
                  <span className="cx-agent-dot" aria-hidden="true" />
                  <span className="cx-agent-text">
                    <span className="cx-agent-title">{n.title}</span>
                    <span className="cx-agent-meta">{n.thread_ref ? "Working in a T3 thread" : "Queued, no thread yet"}{n.due_local ? ` · due ${dueWords(n.due_local, today)}` : ""}</span>
                  </span>
                  {n.thread_ref ? (
                    <a className="cx-btn cx-btn-sm cx-btn-ghost" href={`t3://thread/${encodeURIComponent(n.thread_ref)}`} onClick={(e) => e.preventDefault()} title="The thread stays in T3: it opens there, on the phone or the desktop app">
                      Thread in T3 <Icon name="out" size={14} />
                    </a>
                  ) : null}
                </li>
              ))}
            </ul>
          </Region>
        ) : null}

        {ctx.kind !== "unnamed" && ctx.kind !== "loose" ? (
          <Region title="Page" tone="page" action={page ? <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" onClick={() => openPage(page.slug)} aria-pressed={p.page === page.slug}>Open <Icon name="chevron" size={14} /></button> : null}>
            {page ? (
              <button type="button" className="cx-pagecard" onClick={() => openPage(page.slug)}>
                <span className="cx-pagecard-title">{page.title}</span>
                <Prose body={page.body} limit={5} />
                <span className="cx-pagecard-meta">Notes · {page.slug} · edited {ago(Date.parse(page.updated_at), now)}</span>
              </button>
            ) : (
              <p className="cx-empty-line">No page in Notes for this yet. The agent writes one when there is something to keep.</p>
            )}
          </Region>
        ) : null}

        {ctx.kind !== "unnamed" && ctx.kind !== "loose" && (people.length || ctx.kind !== "person") ? (
          <Region title="People" count={people.length}>
            {people.length ? (
              <ul className="cx-people">
                {people.map((l) => (
                  <li key={l.person.id}>
                    <a className="cx-person" href={href("/people", { id: l.person.name })} data-waiting={l.waiting || undefined}>
                      <span className="cx-avatar" aria-hidden="true">{initials(l.person.name)}</span>
                      <span className="cx-person-text">
                        <span className="cx-person-name">{l.person.name}</span>
                        <span className="cx-person-why">{l.why}</span>
                      </span>
                      {l.waiting ? <span className="cx-chip cx-chip-amber">Waiting</span> : null}
                    </a>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="cx-empty-line">Nobody it waits on or names.</p>
            )}
          </Region>
        ) : null}

        {ctx.kind === "person" && ctx.person ? <PersonRegions ctx={ctx} /> : null}

        {habits.length ? (
          <Region title="Habits">
            <ul className="cx-habits">
              {habits.map((h) => {
                const n = s.habitDone.filter((x) => x.intent_id === h.id && x.t.slice(0, 10) >= today.slice(0, 10)).length;
                return (
                  <li key={h.id}>
                    <span>{h.title}</span>
                    <span className="cx-habit-n cx-num">{n ? `${n} today` : h.frequency ? `${h.frequency} a ${h.frequency_horizon}` : ""}</span>
                    <HabitButton id={h.id} title={h.title} />
                  </li>
                );
              })}
            </ul>
          </Region>
        ) : null}

        {ctx.node || ctx.kind === "label" ? <TimeRegion ctx={ctx} time={time} /> : null}

        {ctx.node ? <AboutNode node={ctx.node} /> : null}
      </div>
    </article>
  );
}

function initials(name: string) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();
}

export function Region({ title, count, children, tone, action, wide }: { title: string; count?: number; children: ReactNode; tone?: string; action?: ReactNode; wide?: boolean }) {
  return (
    <section className="cx-region" data-tone={tone} data-wide={wide || undefined}>
      <header className="cx-region-head">
        <h2 className="cx-label">
          {title}
          {count ? <span className="cx-count">{count}</span> : null}
        </h2>
        {action}
      </header>
      {children}
    </section>
  );
}

function AddHere() {
  const off = useOff();
  return (
    <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" disabled={off} onClick={() => window.dispatchEvent(new Event("cx:add"))} title="Add a task here (q)">
      <Icon name="plus" size={14} /> Add <kbd className="cx-kbd">q</kbd>
    </button>
  );
}

function HabitButton({ id, title }: { id: string; title: string }) {
  const off = useOff();
  return (
    <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" disabled={off} onClick={() => reversible(`Logged ${title}`, () => habitDone(id))}>
      <Icon name="check" size={14} /> Did it
    </button>
  );
}

/** The header's timer: the clock when it runs here, a way to bring it here when it runs elsewhere. */
function TimerHead({ ctx, lead, runHere }: { ctx: Ctx; lead: Node | null; runHere: boolean }) {
  const s = useStore();
  const off = useOff();
  const elapsed = useElapsed();
  const r = runningBlock(s);
  const target = lead?.id ?? (ctx.node && ctx.node.status === "active" ? ctx.node.id : null);
  const targetTitle = lead?.title ?? ctx.node?.title ?? null;
  if (r && runHere) {
    return (
      <div className="cx-timer" data-live>
        <span className="cx-timer-on">
          <span className="cx-live-dot" aria-hidden="true" /> {r.paused_at ? "Paused" : r.intent_id ? `On ${runningLabel(r)}` : "Running, no label"} since {hm(r.t_start)}
        </span>
        <span className="cx-num cx-timer-clock" aria-label={`Elapsed ${clock(elapsed)}`}>{clock(elapsed)}</span>
        <div className="cx-timer-actions">
          <button type="button" className="cx-btn cx-btn-stop" onClick={stop} disabled={off}>
            <Icon name="stop" size={15} /> Stop
          </button>
          <button
            type="button"
            className="cx-btn cx-btn-ghost"
            disabled={off}
            onClick={(e) => {
              anchorFrom(e);
              setParams({ timer: "switch" });
            }}
          >
            <Icon name="switch" size={15} /> Switch
          </button>
        </div>
      </div>
    );
  }
  if (!target || ctx.kind === "person" || ctx.kind === "slot") return null;
  return (
    <div className="cx-timer">
      {r ? <span className="cx-timer-on cx-timer-else">Timer on {runningLabel(r)}</span> : <span className="cx-timer-on cx-timer-else">No timer running</span>}
      <button
        type="button"
        className={r ? "cx-btn cx-btn-quiet" : "cx-btn cx-btn-primary"}
        disabled={off}
        onClick={() => {
          if (r) oneWay(`Switched to ${targetTitle}`, () => switchTimer({ intentId: target }));
          else oneWay(`Timer started on ${targetTitle}`, () => startTimer({ intentId: target }));
          navigate("/", {}, { replace: true });
        }}
      >
        <Icon name={r ? "switch" : "play"} size={15} /> {r ? "Switch here" : "Start"}
        <span className="cx-btn-sub">{targetTitle}</span>
      </button>
    </div>
  );
}

/** A timer with no label: say what it is, here, without leaving. */
function UnnamedBody() {
  const s = useStore();
  const off = useOff();
  const r = runningBlock(s);
  if (!r) return null;
  return (
    <section className="cx-unnamed">
      <p className="cx-unnamed-say">
        It belongs to no context yet, so it counts toward nothing. Name it now, or keep going: when it stops, it waits beside this under “Time in no context”.
      </p>
      <Picker
        disabled={off}
        onPick={(c) => reversible(`The timer since ${hm(r.t_start)} is now ${c.title}`, () => relabelBlock(r.id, c.id))}
      />
    </section>
  );
}

/** The agent's suggested link for a task under nothing. */
function Suggest({ node, goal }: { node: Node; goal: string }) {
  const s = useStore();
  const off = useOff();
  const g = s.nodes.find((n) => n.title === goal);
  if (!g) return null;
  return (
    <div className="cx-suggest">
      <Icon name="spark" size={14} />
      <span>
        The agent thinks this belongs to <strong>{g.title}</strong>.
      </span>
      <button type="button" className="cx-btn cx-btn-sm cx-btn-cyan" disabled={off} onClick={() => reversible(`${node.title} is now under ${g.title}`, () => link(node.id, g.id))}>
        <Icon name="link" size={14} /> Link
      </button>
    </div>
  );
}

function TimeRegion({ ctx, time }: { ctx: Ctx; time: ReturnType<typeof timeOf> }) {
  const off = useOff();
  const { blocks, guesses, todayMs, week } = time;
  return (
    <Region title="Time" tone="time">
      <div className="cx-time-sum">
        <div>
          <span className="cx-num cx-time-big">{todayMs ? dur(todayMs) : "None"}</span>
          <span className="cx-time-cap">today{ctx.kind === "project" || ctx.kind === "goal" ? ` on ${ctx.title}` : ""}</span>
        </div>
        {week ? <WeekBar goal={week.goal.title} hrs={week.hours} target={week.target} self={ctx.kind === "goal"} /> : null}
      </div>
      {blocks.length ? (
        <ul className="cx-blocks">
          {blocks.map((b) => (
            <li key={b.id} data-live={b.t_end === null || undefined}>
              <span className="cx-num cx-block-at">{hm(b.t_start)}–{b.t_end ? hm(b.t_end) : "now"}</span>
              <span className="cx-block-name">{blockName(b)}</span>
              <span className="cx-block-src">{blockSource(b)}</span>
              {b.t_end ? (
                <button
                  type="button"
                  className="cx-linkbtn"
                  disabled={off}
                  onClick={(e) => {
                    anchorFrom(e);
                    setParams({ blk: b.id, block: undefined });
                  }}
                >
                  Relabel
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : (
        <p className="cx-empty-line">Nothing recorded here today. Start the timer on it, or LoR fills in phone and calendar time as the day goes.</p>
      )}
      {guesses.length ? (
        <div className="cx-guesses">
          <span className="cx-label">The agent thinks these were {ctx.title}</span>
          <ul className="cx-blocks">
            {guesses.map((b) => <Guess key={b.id} b={b} />)}
          </ul>
        </div>
      ) : null}
    </Region>
  );
}

export function Guess({ b }: { b: ReturnType<typeof timeOf>["guesses"][number] }) {
  const off = useOff();
  const range = `${hm(b.t_start)}–${hm(b.t_end!)}`;
  return (
    <li data-guess>
      <span className="cx-num cx-block-at">{range}</span>
      <span className="cx-block-name">{b.intent}?</span>
      <span className="cx-block-src">{blockSource(b)}</span>
      <span className="cx-guess-acts">
        <button type="button" className="cx-iconbtn cx-iconbtn-sm cx-iconbtn-ok" aria-label={`Yes, ${range} was ${b.intent}`} title="Yes" disabled={off} onClick={() => reversible(`${range} counted as ${b.intent}`, () => answerProposal(b.id, true))}>
          <Icon name="check" size={14} />
        </button>
        <button
          type="button"
          className="cx-linkbtn"
          disabled={off}
          onClick={(e) => {
            anchorFrom(e);
            setParams({ blk: b.id, block: undefined });
          }}
        >
          Something else
        </button>
      </span>
    </li>
  );
}

export function WeekBar({ goal, hrs, target, self }: { goal: string; hrs: number; target: number | null; self?: boolean }) {
  const none = hrs <= 0;
  const pct = target ? Math.min(100, (hrs / target) * 100) : 0;
  return (
    <div className="cx-week" data-none={none || undefined}>
      <span className="cx-week-label">{self ? "This week" : `${goal}, this week`}</span>
      {none ? (
        <span className="cx-week-val cx-nodata">No data yet</span>
      ) : (
        <span className="cx-week-val cx-num">
          {hours(hrs)}
          {target ? <span className="cx-week-of"> of {target} h</span> : null}
        </span>
      )}
      {target && !none ? (
        <span className="cx-meter" aria-hidden="true">
          <span style={{ width: `${pct}%` }} data-over={hrs > target || undefined} />
        </span>
      ) : null}
    </div>
  );
}

function AboutNode({ node }: { node: Node }) {
  return (
    <details className="cx-fold cx-about">
      <summary>About this {node.type}</summary>
      <dl>
        <dt>Kind</dt>
        <dd>{node.type === "goal" ? "Goal" : "Project"}{node.parents[0] ? `, under ${node.parents[0].title}` : ""}</dd>
        <dt>Weekly target</dt>
        <dd>{node.weekly_hour_target ? `${node.weekly_hour_target} h` : "None"}</dd>
        <dt>Weight</dt>
        <dd>{node.weight}</dd>
        <dt>Made</dt>
        <dd>{node.created_at.slice(0, 10)} · {node.via === "agent" ? "by the agent" : "by you"}</dd>
      </dl>
      <p className="cx-fold-note">Editing targets, weights and links is on the full Goals editor, which this prototype leaves as it is.</p>
    </details>
  );
}

// ---- the person context ----

function PersonLine({ ctx }: { ctx: Ctx }) {
  const s = useStore();
  const now = useNow(60_000);
  const off = useOff();
  const p = s.people.find((x) => x.id === ctx.key) ?? ctx.person!;
  const last = p.last.at ? Date.parse(p.last.at) : null;
  return (
    <p className="cx-subline">
      {last ? <>In touch {ago(last, now)}{p.last.via ? `, by ${p.last.via}` : ""}</> : "Not in touch yet"}
      {p.keep_in_touch_days ? <> · every {p.keep_in_touch_days} days{p.overdue_by && p.overdue_by > 0 ? <span className="cx-late"> · {p.overdue_by} days over</span> : null}</> : null}
      <button
        type="button"
        className="cx-star"
        aria-pressed={p.priority}
        disabled={off}
        onClick={() => reversible(p.priority ? `${p.name} is no longer a priority` : `${p.name} is a priority`, () => setPerson(p.id, { priority: !p.priority }))}
        title={p.priority ? "A priority: shown first" : "Make a priority"}
      >
        <Icon name="star" size={15} /> {p.priority ? "Priority" : "Not a priority"}
      </button>
    </p>
  );
}

function PersonRegions({ ctx }: { ctx: Ctx }) {
  const s = useStore();
  const now = useNow(60_000);
  const off = useOff();
  const hub = s.hubs[ctx.key];
  const p = s.people.find((x) => x.id === ctx.key) ?? ctx.person!;
  const msgs = hub?.entity.recent_messages ?? [];
  const mail = hub?.entity.recent_mail ?? [];
  const partOf = p.page ? pagesNaming(s, p.page) : [];
  const stream = [
    ...msgs.map((m) => ({ at: m.at, icon: "message", who: m.direction === "in" ? (m.from ?? p.name) : "You", text: m.text ?? "", via: m.kind })),
    ...mail.map((m) => ({ at: m.at, icon: "mail", who: m.direction === "in" ? p.name : "You", text: m.subject ?? "(no subject)", via: "mail" })),
  ].sort((a, b) => b.at.localeCompare(a.at));
  const cadences = [null, 7, 14, 30, 60, 90];
  return (
    <>
      <Region title="Lately" count={stream.length} wide>
        {stream.length ? (
          <ul className="cx-stream">
            {stream.slice(0, 6).map((x, i) => (
              <li key={i} data-out={x.who === "You" || undefined}>
                <Icon name={x.icon} size={15} />
                <span className="cx-stream-text">
                  <span className="cx-stream-who">{x.who}</span> {x.text}
                </span>
                <span className="cx-stream-at">{ago(Date.parse(x.at), now)}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="cx-empty-line">No mail or messages LoR has seen.</p>
        )}
      </Region>
      {partOf.length ? (
        <Region title="Part of">
          <div className="cx-chips">
            {partOf.map((pg) => (
              <a key={pg.slug} className="cx-ctxchip" href={href("/notes", { id: pg.slug })}>
                <Icon name="notes" size={14} /> {pg.title}
              </a>
            ))}
          </div>
        </Region>
      ) : null}
      <details className="cx-fold cx-about">
        <summary>Cadence and contact details</summary>
        <div className="cx-field-row">
          <span className="cx-label">Keep in touch</span>
          <div className="cx-chips" role="group" aria-label="Keep in touch every">
            {cadences.map((d) => (
              <button key={String(d)} type="button" className="cx-chipbtn" aria-pressed={p.keep_in_touch_days === d} disabled={off} onClick={() => reversible(d ? `Every ${d} days for ${p.name}` : `No cadence for ${p.name}`, () => setPerson(p.id, { keepInTouchDays: d }))}>
                {d ? `${d} days` : "No cadence"}
              </button>
            ))}
          </div>
        </div>
        <dl>
          {p.identifiers.map((i) => (
            <div key={i.value} className="cx-dl-row">
              <dt>{i.kind}</dt>
              <dd>{i.value}</dd>
            </div>
          ))}
        </dl>
        <p className="cx-fold-note">Mail routing for a person stays on the shipped People page.</p>
      </details>
    </>
  );
}

// ---- the slot context ----

function SlotRegion({ ctx }: { ctx: Ctx }) {
  const s = useStore();
  const now = useNow(1000);
  const e = ctx.event!;
  const start = Date.parse(e.t_start);
  const during = eventNow(s, now);
  const free = Math.max(0, start - Math.max(now, during ? Date.parse(during.t_end) : now));
  const cur = nextEvent(s, now);
  return (
    <Region title="Before it" tone="cyan" wide>
      <p className="cx-slot-gap">
        <span className="cx-num cx-time-big">{dur(free)}</span>
        <span className="cx-time-cap">free from {hm(Math.max(now, during ? Date.parse(during.t_end) : now))} until {hm(start)}{cur?.event.event_id === e.event_id ? "" : " (another event comes first)"}</span>
      </p>
      <p className="cx-empty-line">Nothing is put here unasked. Pick from Next in the context you are in, or from what waits beside it.</p>
    </Region>
  );
}

/** Open a context link: the path and params for it. */
export const ctxHref = (c: Ctx, extra: Record<string, string> = {}) => href(ctxPath(c).path, { ...ctxPath(c).params, ...extra });
export const homeHref = (s: ReturnType<typeof useStore>, n: Node, extra: Record<string, string> = {}) => ctxHref(homeOf(s, n), extra);
export { Check };
