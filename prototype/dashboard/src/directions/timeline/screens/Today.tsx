// Today: the day as a route from waking to sleep, with now a third of the way down. Past blocks
// above the now line, calendar events and his own plans below it, and what has no time in a
// column beside it. Everything on the route is edited where it is, in a popover.
import { type CSSProperties, useLayoutEffect, useMemo, useRef, useState } from "react";
import { doneOn, isOpen, taskRow } from "../../../data/derive.js";
import { answerProposal, running, setDone, startTimer, stopTimer, today as todayOf, unflag, useNow, useStore } from "../../../data/store.js";
import { setParams } from "../lib/nav.js";
import type { Route } from "../../../shell/nav.js";
import { BlockEditor } from "../components/BlockEditor.js";
import { Check, Icon, Prose, useNarrow } from "../components/bits.js";
import { LabelPicker } from "../components/LabelPicker.js";
import { Popover } from "../components/Popover.js";
import { TaskEditor } from "../components/TaskEditor.js";
import { useMode } from "../lib/ctx.js";
import { blockSource, blockTitle, buildRoute, freeGaps, type RouteModel, type Span, toneOf, type Waiting, waitingOn } from "../lib/day.js";
import { ago, at, clock, dueWords, dur, hm, longDate } from "../lib/time.js";
import { crowdSee, oneWay, plan, reversible, unplan, useCrowdSeen, usePlans } from "../lib/ui.js";

export function Today({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(1000);
  const narrow = useNarrow();
  const { staleSince, crowded } = useMode();
  const plans = usePlans();
  const seen = useCrowdSeen();
  const p = route.params;
  const idle = running(s) === null && staleSince === null;
  const model = useMemo(() => buildRoute(s, now, plans, staleSince, idle ? 56 : 0), [s, now, plans, staleSince, idle]);
  const waiting = waitingOn(s, crowded, seen);
  const today = todayOf();
  const planned = new Set(plans.map((x) => x.taskId));
  const due = s.nodes
    .filter((n) => n.type === "task" && isOpen(n) && n.due_local !== null && n.due_local <= today && n.flagged_at === null && !planned.has(n.id))
    .filter((n) => !(crowded && waiting.some((w) => w.id === n.id)))
    .sort((a, b) => a.due_local!.localeCompare(b.due_local!) || a.title.localeCompare(b.title))
    .map(taskRow);
  const done = doneOn(s, today);
  const gaps = freeGaps(model);
  const r = running(s);
  const next = s.day.planned.filter((e) => !e.all_day && Date.parse(e.t_start) > now).sort((a, b) => a.t_start.localeCompare(b.t_start))[0];
  const allDay = s.day.planned.filter((e) => e.all_day);
  const wakeEnd = Date.parse(s.day.wake_end ?? s.day.day_end);
  const left = Math.max(0, wakeEnd - now);
  const morning = now < at(today, "12:00");
  const toConfirm = s.blocks.filter((b) => b.t_end && toneOf(b) === "proposed").length;
  const bare = s.blocks.filter((b) => b.t_end && toneOf(b) === "bare").length;

  const openTask = p.task ? s.nodes.find((n) => n.title === p.task) : undefined;
  const closeTask = () => setParams({ task: undefined });

  const column = (
    <Untimed
      waiting={waiting}
      due={due}
      done={done}
      today={today}
      now={now}
      brief={s.brief}
      morning={morning}
      next={next ? { title: next.title ?? "An event", at: Date.parse(next.t_start) } : null}
      narrow={narrow}
      params={p}
    />
  );

  return (
    <div className="tl-today" data-narrow={narrow || undefined}>
      <header className="tl-dayhead">
        <div className="tl-dayhead-date">
          <h1>{longDate(today)}</h1>
          <div className="tl-dayhead-tags">
            {allDay.map((e) => (
              <span key={e.event_id} className="tl-tag tl-tag-plan">
                <Icon name="calendar" size={12} /> {e.title}
              </span>
            ))}
            {toConfirm || bare ? (
              <span className="tl-tag tl-tag-ask" title="Blocks on the route waiting for a label">
                {toConfirm ? `${toConfirm} to confirm` : ""}
                {toConfirm && bare ? " · " : ""}
                {bare ? `${bare} unlabelled` : ""}
              </span>
            ) : null}
            {toConfirm > 1 && staleSince === null ? (
              <button
                type="button"
                className="tl-btn tl-btn-quiet tl-btn-sm"
                onClick={() =>
                  reversible(
                    `Confirmed ${toConfirm} suggestions`,
                    () => {
                      for (const b of s.blocks.filter((x) => x.t_end && toneOf(x) === "proposed")) answerProposal(b.id, true);
                    },
                    toConfirm,
                  )
                }
              >
                <Icon name="check" size={14} /> Confirm all {toConfirm}
              </button>
            ) : null}
          </div>
        </div>
        <div className="tl-dayhead-now">
          <p className="tl-readout">
            <span className="tl-num">{dur(left).replace(/m$/, "")}</span>
            {dur(left).endsWith("m") ? <small>m</small> : null}
            <small className="tl-readout-k">left today</small>
          </p>
          <p className="tl-muted">{next ? <>Next: {next.title} at {hm(next.t_start)}, in {dur(Date.parse(next.t_start) - now)}</> : "Nothing else on the calendar"}</p>
        </div>
      </header>

      {narrow ? column : null}

      <RouteView model={model} now={now} narrow={narrow} params={p} running={Boolean(r)} />

      {narrow ? null : column}

      {openTask ? (
        <Popover anchorId={`task-${openTask.id}`} label={openTask.title} onClose={closeTask}>
          <TaskEditor id={openTask.id} gaps={gaps} showOpen />
        </Popover>
      ) : null}
    </div>
  );
}

// ---- the route ----

function RouteView({ model, now, narrow, params, running: isRunning }: { model: RouteModel; now: number; narrow: boolean; params: Record<string, string>; running: boolean }) {
  const s = useStore();
  const scroller = useRef<HTMLDivElement>(null);
  const { staleSince } = useMode();
  const [earlier, setEarlier] = useState(false);
  const off = staleSince !== null;

  // At phone width the morning folds into one line, so now stays near the top of the route.
  const foldBefore = narrow && !earlier ? now - 2 * 3_600_000 : -Infinity;
  const folded = model.spans.filter((sp) => sp.end <= foldBefore && sp.end <= now);
  const shift = folded.length ? folded[folded.length - 1]!.top + folded[folded.length - 1]!.height : 0;
  const foldH = folded.length ? 56 : 0;
  const spans = model.spans.filter((sp) => !folded.includes(sp));
  const y = (v: number) => v - shift + foldH;

  // Now sits a third of the way down the route's own scroll on the desktop.
  useLayoutEffect(() => {
    const el = scroller.current;
    if (!el || narrow) return;
    el.scrollTop = Math.max(0, y(model.nowY) - el.clientHeight / 3);
    // Only when the scene or the width changes, not on every tick.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [narrow, s.scene]);

  const hours: { t: number; y: number }[] = [];
  for (let t = model.first; t <= model.last; t += 3_600_000) {
    const v = model.yOf(t);
    // The now line carries its own time, so an hour tick right beside it is left out.
    // Squeezed stretches would stack ticks on top of each other: keep one in every 24px.
    const prev = hours[hours.length - 1];
    if (v !== null && v >= shift && Math.abs(t - now) >= 25 * 60_000 && Math.abs(y(v) - y(model.nowY)) > 22 && (!prev || y(v) - prev.y >= 24)) hours.push({ t, y: y(v) });
  }

  // A press names the block by its id (`blk=`), which no other block shares. `block=HH:MM` is
  // the readable form for links and screenshots: the block that starts in that minute (the
  // newest, if a timer was switched in it), else the one the minute falls inside.
  const blockAt = params.blk
    ? spans.find((sp) => sp.block?.id === params.blk)
    : params.block
      ? (spans.filter((sp) => sp.block && hm(sp.start) === params.block).at(-1) ??
        spans.find((sp) => sp.block && sp.start <= at(todayOf(), params.block!) && at(todayOf(), params.block!) < Math.max(sp.end, sp.start + 60_000)))
      : undefined;
  const closeBlock = () => setParams({ block: undefined, blk: undefined });
  const gapAt = params.gap ? spans.find((sp) => sp.kind === "free" && hm(sp.start) === params.gap) : undefined;
  const toFold = folded.filter((sp) => sp.block);
  const foldAsk = toFold.filter((sp) => toneOf(sp.block!) !== "tracked").length;

  return (
    <section className="tl-routewrap" aria-label="The day">
      <div className="tl-route-scroll" ref={scroller}>
        <div className="tl-route" style={{ height: y(model.height) + 24 }}>
          {hours.map((h) => (
            <span key={h.t} className="tl-hour" style={{ top: h.y }}>
              <span className="tl-num">{hm(h.t)}</span>
            </span>
          ))}
          {folded.length ? (
            <button type="button" className="tl-fold" style={{ top: 0, "--h": `${foldH - 4}px` } as CSSProperties} onClick={() => setEarlier(true)}>
              <span className="tl-fold-main">
                <span className="tl-num">
                  Earlier, {hm(folded[0]!.start)}–{hm(folded[folded.length - 1]!.end)}
                </span>
                <span>
                  {toFold.length} block{toFold.length === 1 ? "" : "s"}{foldAsk ? ` · ${foldAsk} want a label` : ""}
                </span>
              </span>
              <span className="tl-fold-show">
                Show <Icon name="down" size={14} />
              </span>
            </button>
          ) : null}
          {spans.map((sp) => (
            <SpanView key={sp.key} sp={{ ...sp, top: y(sp.top) }} now={now} params={params} off={off} empty={s.blocks.length === 0} selected={blockAt?.key === sp.key} />
          ))}
          <NowLine top={y(model.nowY)} now={now} running={isRunning} params={params} off={off} />
          {blockAt?.block ? (
            <Popover anchorId={`blk-${blockAt.block.id}`} label="A block of time" onClose={closeBlock}>
              <BlockEditor key={blockAt.block.id} id={blockAt.block.id} startMode={blockAt.kind === "running" ? "switch" : "relabel"} onDone={closeBlock} />
            </Popover>
          ) : null}
          {gapAt ? (
            <Popover anchorId={`gap-${gapAt.start}`} label="Plan a task" onClose={() => setParams({ gap: undefined })}>
              <GapPlanner start={Math.max(gapAt.start, now)} end={gapAt.end} onDone={() => setParams({ gap: undefined })} />
            </Popover>
          ) : null}
        </div>
      </div>
    </section>
  );
}

function SpanView({ sp, now, params, off, empty, selected }: { sp: Span; now: number; params: Record<string, string>; off: boolean; empty: boolean; selected: boolean }) {
  const { staleSince } = useMode();
  const [over, setOver] = useState(false);
  const style = { top: sp.top, height: sp.height };
  const box = { top: sp.top, "--h": `${sp.height}px` } as CSSProperties;
  const mins = (sp.end - sp.start) / 60_000;
  const spine = <span className="tl-spine" data-kind={sp.kind} data-tone={sp.block ? toneOf(sp.block) : undefined} style={style} aria-hidden="true" />;

  if (sp.kind === "untracked") {
    return (
      <>
        {spine}
        {sp.height >= 22 ? (
          <div className="tl-gapnote" style={box}>
            {empty && sp.end >= now ? "Nothing recorded yet today. Start a timer, or LoR fills this in from your phone and calendar as the day goes." : `${dur(sp.end - sp.start)}, nothing recorded`}
          </div>
        ) : null}
      </>
    );
  }
  if (sp.kind === "silent") {
    return (
      <>
        {spine}
        <div className="tl-silent" style={box}>
          <Icon name="cloud" size={16} /> No word from LoR since {hm(sp.start)}. What happened after is not shown yet.
        </div>
      </>
    );
  }
  if (sp.kind === "event") {
    const e = sp.event!;
    return (
      <>
        {spine}
        <div className="tl-blk" data-kind="event" style={box}>
          <span className="tl-blk-main">
            <span className="tl-blk-title">{e.title}</span>
            <span className="tl-blk-meta">
              <span className="tl-num">{hm(e.t_start)}–{hm(e.t_end)}</span> · Calendar
            </span>
          </span>
        </div>
      </>
    );
  }
  if (sp.kind === "plan") {
    const pl = sp.plan!;
    return (
      <>
        {spine}
        <div className="tl-blk" data-kind="plan" style={box}>
          <span className="tl-blk-main">
            <span className="tl-blk-title">{pl.title}</span>
            <span className="tl-blk-meta">
              <span className="tl-num">{hm(sp.start)}–{hm(sp.end)}</span> · planned by you
            </span>
          </span>
          <button type="button" className="tl-btn tl-btn-quiet tl-btn-sm" disabled={off} onClick={() => unplan(pl.taskId)}>
            Take off
          </button>
        </div>
      </>
    );
  }
  if (sp.kind === "free") {
    const begin = Math.max(sp.start, now);
    const free = sp.end - begin;
    if (free < 5 * 60_000) return spine;
    return (
      <>
        {spine}
        <div
          className="tl-free"
          data-over={over || undefined}
          style={box}
          onDragOver={(e) => {
            if (off || !e.dataTransfer.types.includes("text/lor-task")) return;
            e.preventDefault();
            setOver(true);
          }}
          onDragLeave={() => setOver(false)}
          onDrop={(e) => {
            setOver(false);
            const raw = e.dataTransfer.getData("text/lor-task");
            if (!raw) return;
            const { id, title, minutes } = JSON.parse(raw) as { id: string; title: string; minutes: number };
            const frac = Math.max(0, Math.min(1, (e.nativeEvent.offsetY || 0) / sp.height));
            const q = 15 * 60_000;
            let start = Math.ceil((begin + frac * free) / q) * q;
            const length = Math.min(minutes, Math.round(free / 60_000));
            if (start + length * 60_000 > sp.end) start = sp.end - length * 60_000;
            plan(id, title, Math.max(begin, start), length);
          }}
        >
          <span className="tl-num">{dur(free)}</span> free
          {mins >= 10 && !off ? (
            <button type="button" id={`gap-${sp.start}`} className="tl-free-plan" onClick={() => setParams({ gap: params.gap ? undefined : hm(sp.start) })}>
              <Icon name="plus" size={14} /> Plan a task here
            </button>
          ) : null}
        </div>
      </>
    );
  }

  // A recorded block, or the running one.
  const b = sp.block!;
  const tone = toneOf(b);
  const open = () => setParams({ block: undefined, blk: selected ? undefined : b.id });
  if (sp.kind === "running") {
    return (
      <>
        {spine}
        <div className="tl-blk" data-kind="running" data-selected={selected || undefined} style={box}>
          <button type="button" id={`blk-${b.id}`} className="tl-blk-main" onClick={open} aria-label={`Running: ${b.intent ?? "Unlabelled"}. Open`}>
            <span className="tl-blk-k">
              <span className="tl-live-dot" aria-hidden="true" /> Running since {hm(b.t_start)}
            </span>
            <span className="tl-blk-title">{b.intent ?? "Unlabelled"}</span>
          </button>
          <span className="tl-num tl-blk-clock">{clock((staleSince ?? now) - Date.parse(b.t_start))}</span>
          <span className="tl-blk-actions">
            <button type="button" className="tl-btn tl-btn-quiet" disabled={off} onClick={() => setParams({ block: undefined, blk: b.id })}>
              <Icon name="switch" size={16} /> Switch
            </button>
            <button type="button" className="tl-btn" disabled={off} onClick={() => oneWay(`Stopped: ${b.intent ?? "Unlabelled"}, ${clock(now - Date.parse(b.t_start))}`, stopTimer)}>
              <Icon name="stop" size={16} /> Stop
            </button>
          </span>
        </div>
      </>
    );
  }
  const short = sp.height < 52;
  return (
    <>
      {spine}
      <div className="tl-blk" data-kind="block" data-tone={tone} data-short={short || undefined} data-selected={selected || undefined} style={box}>
        <button type="button" id={`blk-${b.id}`} className="tl-blk-main" onClick={open}>
          <span className="tl-blk-title">{tone === "bare" ? blockSource(b) : blockTitle(b)}</span>
          <span className="tl-blk-meta">
            <span className="tl-num">{hm(b.t_start)}–{hm(b.t_end!)}</span> · {dur(sp.end - sp.start)}
            {tone === "tracked" ? ` · ${b.origin === "manual" ? "timer" : b.origin === "calendar" ? `${b.intent} · Calendar` : blockSource(b)}` : ""}
            {tone === "proposed" ? ` · ${blockSource(b)}` : ""}
          </span>
        </button>
        {tone === "proposed" && !off ? (
          <span className="tl-blk-ask">
            <span className="tl-blk-guess">{b.intent}?</span>
            <button type="button" className="tl-iconbtn tl-iconbtn-ok" aria-label={`Yes, ${b.intent}`} title={`Yes, ${b.intent}`} onClick={() => reversible(`Confirmed: ${b.intent}`, () => answerProposal(b.id, true))}>
              <Icon name="check" size={16} />
            </button>
          </span>
        ) : null}
        {tone === "bare" && !off ? (
          <button type="button" className="tl-blk-name" onClick={open}>
            Name it
          </button>
        ) : null}
      </div>
    </>
  );
}

function NowLine({ top, now, running: isRunning, params, off }: { top: number; now: number; running: boolean; params: Record<string, string>; off: boolean }) {
  const { staleSince } = useMode();
  return (
    <div className="tl-now" style={{ top }}>
      <span className="tl-now-label">
        <span className="tl-num">{hm(now)}</span> now
      </span>
      <span className="tl-reticle" aria-hidden="true" />
      {!isRunning && staleSince === null ? (
        <>
          <button type="button" id="tl-now-start" className="tl-btn tl-btn-primary tl-now-start" disabled={off} onClick={() => setParams({ start: params.start ? undefined : 1 })}>
            <Icon name="play" size={14} /> Start a timer here
          </button>
          {params.start ? (
            <Popover anchorId="tl-now-start" label="Start a timer" onClose={() => setParams({ start: undefined })}>
              <div className="tl-editor">
                <p className="tl-kicker">Starts at {hm(now)}, grows down from the now line</p>
                <h2 className="tl-editor-title">What are you doing?</h2>
                <LabelPicker
                  allowNone
                  onPick={(id, title) => {
                    oneWay(`Started: ${title}`, () => startTimer({ intentId: id }));
                    setParams({ start: undefined });
                  }}
                />
              </div>
            </Popover>
          ) : null}
        </>
      ) : null}
    </div>
  );
}

function GapPlanner({ start, end, onDone }: { start: number; end: number; onDone: () => void }) {
  const s = useStore();
  const { staleSince } = useMode();
  const today = todayOf();
  const plans = usePlans();
  const taken = new Set(plans.map((x) => x.taskId));
  const cands = s.nodes
    .filter((n) => n.type === "task" && isOpen(n) && n.assignee === "owner" && !taken.has(n.id))
    .sort((a, b) => (a.due_local ?? "9999").localeCompare(b.due_local ?? "9999"))
    .slice(0, 6);
  return (
    <div className="tl-editor">
      <p className="tl-kicker">
        {hm(start)} to {hm(end)} · {dur(end - start)} free
      </p>
      <h2 className="tl-editor-title">Plan something here</h2>
      {staleSince !== null ? (
        <p className="tl-note">LoR can’t be reached, so planning waits until it answers.</p>
      ) : (
        <p className="tl-muted">Nothing goes on the route unless you put it there.</p>
      )}
      <ul className="tl-planlist">
        {cands.map((n) => {
          const minutes = Math.min(Math.round((n.estimate_hours ?? 0.5) * 60), Math.round((end - start) / 60_000));
          return (
            <li key={n.id}>
              <button
                type="button"
                disabled={staleSince !== null}
                onClick={() => {
                  plan(n.id, n.title, start, minutes);
                  onDone();
                }}
              >
                <span>{n.title}</span>
                <small>
                  {n.due_local ? dueWords(n.due_local, today) : "no date"} · {minutes} min
                </small>
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

// ---- what has no time: waiting on him, due with no time, done today ----

function Untimed({ waiting, due, done, today, now, brief, morning, next, narrow, params }: {
  waiting: Waiting[];
  due: ReturnType<typeof taskRow>[];
  done: ReturnType<typeof taskRow>[];
  today: string;
  now: number;
  brief: string | null;
  morning: boolean;
  next: { title: string; at: number } | null;
  narrow: boolean;
  params: Record<string, string>;
}) {
  const { staleSince, crowded } = useMode();
  const off = staleSince !== null;
  const [allWaiting, setAllWaiting] = useState(false);
  const [showDue, setShowDue] = useState(!narrow);
  const [showBrief, setShowBrief] = useState(morning && !narrow);
  const cap = narrow ? 2 : 4;
  const shown = allWaiting || waiting.length <= cap + 1 ? waiting : waiting.slice(0, cap);
  const openTask = (title: string) => setParams({ task: params.task === title ? undefined : title });
  const quiet = waiting.length === 0 && due.length === 0;

  return (
    <aside className="tl-untimed" aria-label="No time yet">
      {brief ? (
        <section className="tl-card tl-brief" data-open={showBrief || undefined}>
          <button type="button" className="tl-brief-head" aria-expanded={showBrief} onClick={() => setShowBrief((v) => !v)}>
            <Icon name="spark" size={16} />
            <span className="tl-section-title">This morning’s brief</span>
            <span className="tl-muted tl-num">07:00</span>
            <Icon name="down" size={16} />
          </button>
          {showBrief ? <Prose text={brief} /> : <p className="tl-brief-line">{brief.split("\n")[0]}</p>}
        </section>
      ) : null}

      <section className="tl-section" aria-label="Waiting on you">
        <h2 className="tl-section-title">
          Waiting on you {waiting.length ? <span className="tl-count" data-loud={crowded || undefined}>{waiting.length}</span> : null}
        </h2>
        {quiet ? (
          <div className="tl-calm">
            <p className="tl-calm-title">Nothing is waiting on you.</p>
            <p className="tl-muted">
              No flags, nothing due. {next ? <>Next on the route: {next.title} at {hm(next.at)}.</> : "The rest of the day is free."}
            </p>
          </div>
        ) : waiting.length === 0 ? (
          <p className="tl-muted tl-empty-line">No flags from the agent.</p>
        ) : (
          <ul className="tl-cards">
            {shown.map((w) => (
              <li key={w.id} className="tl-card tl-flag">
                <button type="button" id={`task-${w.id}`} className="tl-flag-main" onClick={() => openTask(w.title)}>
                  <span className="tl-flag-title">{w.title}</span>
                  <span className="tl-flag-note">{w.note}</span>
                  <span className="tl-flag-when">Flagged {ago(Date.parse(w.flaggedAt), now)}</span>
                </button>
                <span className="tl-row-actions">
                  <button
                    type="button"
                    className="tl-btn tl-btn-primary tl-btn-sm"
                    disabled={off || w.google}
                    title={w.google ? "Lives in Google Tasks: tick it there" : undefined}
                    onClick={() => reversible(`Done: ${w.title}`, () => setDone(w.id, true))}
                  >
                    <Icon name="check" size={14} /> Done
                  </button>
                  <button
                    type="button"
                    className="tl-btn tl-btn-quiet tl-btn-sm"
                    disabled={off}
                    onClick={() => (w.crowd ? crowdSee(w.id, w.title) : reversible(`Seen: ${w.title}`, () => unflag(w.id)))}
                  >
                    Seen
                  </button>
                </span>
              </li>
            ))}
            {shown.length < waiting.length ? (
              <li>
                <button type="button" className="tl-more-btn" onClick={() => setAllWaiting(true)}>
                  {waiting.length - shown.length} more waiting, oldest first <Icon name="down" size={14} />
                </button>
              </li>
            ) : null}
          </ul>
        )}
      </section>

      {due.length || done.length ? (
        <section className="tl-section" aria-label="Due, no time yet">
          <button type="button" className="tl-section-toggle" aria-expanded={showDue} onClick={() => setShowDue((v) => !v)}>
            <h2 className="tl-section-title">
              Due, no time yet {due.length ? <span className="tl-count">{due.length}</span> : null}
            </h2>
            <Icon name={showDue ? "down" : "chevron"} size={16} />
          </button>
          {showDue ? (
            <>
              {due.length ? <p className="tl-hint">Drag one onto a free gap to plan it, or open it.</p> : null}
              <ul className="tl-rows">
                {due.map((t) => (
                  <li
                    key={t.id}
                    className="tl-row"
                    draggable={!off}
                    onDragStart={(e) => {
                      const n = { id: t.id, title: t.title, minutes: 30 };
                      e.dataTransfer.setData("text/lor-task", JSON.stringify(n));
                      e.dataTransfer.effectAllowed = "move";
                    }}
                  >
                    <Check
                      checked={false}
                      disabled={off || t.google}
                      title={t.google ? "Lives in Google Tasks: tick it there" : undefined}
                      label={`Done: ${t.title}`}
                      onChange={() => reversible(`Done: ${t.title}`, () => setDone(t.id, true))}
                    />
                    <button type="button" id={`task-${t.id}`} className="tl-row-main" onClick={() => openTask(t.title)}>
                      <span className="tl-row-title">{t.title}</span>
                      <span className="tl-row-meta" data-late={t.overdue && t.due !== today ? true : undefined}>
                        {dueWords(t.due, today)}
                        {t.parent ? ` · ${t.parent}` : ""}
                        {t.google ? " · Google Tasks" : ""}
                      </span>
                    </button>
                    <span className="tl-grip" aria-hidden="true" />
                  </li>
                ))}
              </ul>
              {done.length ? (
                <details className="tl-done">
                  <summary>
                    Done today <span className="tl-count">{done.length}</span>
                  </summary>
                  <ul className="tl-rows">
                    {done.map((t) => (
                      <li key={t.id} className="tl-row" data-done>
                        <Check checked label={`Not done: ${t.title}`} disabled={off} onChange={() => reversible(`Not done: ${t.title}`, () => setDone(t.id, false))} />
                        <span className="tl-row-main">
                          <span className="tl-row-title">{t.title}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                </details>
              ) : null}
            </>
          ) : null}
        </section>
      ) : null}
    </aside>
  );
}
