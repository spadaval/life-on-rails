import { useEffect, useMemo, useRef, useState } from "react";
import { Link, createFileRoute, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { DayArc } from "../components/DayArc.js";
import { useNow } from "../components/clock.js";
import { duration, shortDate, splitBrief } from "../components/format.js";
import { localTimeOnDay } from "../shared/localTime.js";
import { TaskCheck } from "../components/IntentEditor.js";
import { IntentPicker, type PickIntent } from "../components/IntentPicker.js";
import { Markdown } from "../components/Markdown.js";
import { Empty, Meter, Notice, PageHead, Panel, Star } from "../components/ui.js";
import { metricFn, resetVisibilityFn, taskDoneFn, unflagFn } from "../server/functions/graph.js";
import {
  askAgentFn, confirmFn, deleteBlockFn, editBlockFn, getTodayFn, recategorizeFn, splitBlockFn, timerFn, uncommitFn,
} from "../server/functions/today.js";
import { GlobalActions } from "./__root.js";

export const Route = createFileRoute("/")({
  // `block=` selects a block by link: its id, or a local time like 14:30 that it covers. `brief=1` opens the brief.
  validateSearch: (search) =>
    z
      .object({ date: z.string().optional(), obs: z.string().uuid().optional(), block: z.coerce.string().optional(), brief: z.coerce.boolean().optional() })
      .parse(search),
  loaderDeps: ({ search }) => ({ date: search.date, obs: search.obs }),
  loader: ({ deps }) => getTodayFn({ data: { date: deps.date, obs: deps.obs } }),
  component: Today,
});

type Data = Awaited<ReturnType<typeof getTodayFn>>;
type Block = Data["today"]["actual"][number];
type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;

/** Phone or place time nobody has labelled: shown, never asked about. */
const isBare = (b: Block) => (b.origin === "auto_activity" || b.origin === "place") && !b.intent && !b.committed && !b.proposal;
const blockName = (b: Block) => b.intent ?? (b.origin === "auto_activity" ? "Phone" : b.origin === "place" ? (b.title ?? "Somewhere") : "Unlabelled");
const fromWhere = (origin: string) =>
  origin === "calendar" ? "from the calendar" : origin === "auto_activity" ? "from your phone" : origin === "place" ? "from where you were" : "timer";

function Today() {
  const data = Route.useLoaderData();
  const { today: t } = data;
  const search = Route.useSearch();
  const router = useRouter();
  const now = useNow(20_000, t.now);
  const linked = useMemo(() => linkedBlock(t, search.block), [t, search.block]);
  const [selected, setSelected] = useState<string | null>(linked);
  useEffect(() => setSelected(linked), [linked]);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // The timer card's "Something else…" picker. It opens under the arc, not in it: laid over the picture it grew past
  // the arc's bottom and under the lanes, which then took its clicks.
  const [picking, setPicking] = useState(false);
  const [timerTitle, setTimerTitle] = useState("");
  // Either way of starting, a recent intent in the card or any intent in the picker, takes the picker's title and closes it.
  const startTimer = (intentId: string) =>
    act(() => timerFn({ data: { action: "start", intentId, title: timerTitle.trim() || null } })).then(() => {
      setPicking(false);
      setTimerTitle("");
    });

  const hm24 = useMemo(() => new Intl.DateTimeFormat("en-GB", { timeZone: t.timezone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }), [t.timezone]);
  const hm = (ms: number) => hm24.format(new Date(ms));
  const clock = (iso: string) => hm24.format(new Date(iso));

  const act: Act = async (fn, done) => {
    if (busy) return;
    setBusy(true);
    try {
      await fn();
      setNotice(done ?? null);
    } catch (err) {
      setNotice(err instanceof Error ? err.message : String(err));
    }
    await router.invalidate();
    setBusy(false);
  };

  // Keep the page honest without a tap: new calendar fill, a timer started from the phone.
  useEffect(() => {
    const id = setInterval(() => void router.invalidate(), 60_000);
    return () => clearInterval(id);
  }, [router]);

  // `r` jumps to relabelling. Never while typing.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = e.target as HTMLElement | null;
      if (el && (el.tagName === "INPUT" || el.tagName === "TEXTAREA" || el.tagName === "SELECT" || el.tagName === "BUTTON")) return;
      if (e.key === "r") {
        const target = selected ?? t.running?.id ?? null;
        if (!target) return;
        e.preventDefault();
        setSelected(target);
        setTimeout(() => document.getElementById("relabel-search")?.focus(), 0);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  const selectedBlock = t.actual.find((b) => b.id === selected) ?? (t.running?.id === selected ? t.running : null);
  const dateLabel = new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "long", day: "numeric", month: "long" }).format(new Date(`${t.date}T12:00:00Z`));
  const wakeStart = new Date(t.wake_start).getTime();
  const wakeEnd = new Date(t.wake_end).getTime();
  const remaining = t.is_today ? Math.max(0, (wakeEnd - Math.max(now, wakeStart)) / 1000) : 0;
  // Everything the actual lane holds, labelled or not.
  const recorded = t.actual.reduce((s, b) => {
    const a = Math.max(new Date(b.t_start).getTime(), new Date(t.day_start).getTime());
    const z = Math.min(b.t_end ? new Date(b.t_end).getTime() : now, new Date(t.day_end).getTime());
    return s + Math.max(0, z - a) / 1000;
  }, 0);
  const timed = t.planned.filter((e) => !e.all_day);
  const next = t.is_today ? timed.find((e) => new Date(e.t_start).getTime() > now) : undefined;

  return (
    <div className="space-y-5 pb-4">
      <PageHead title={t.is_today ? "Today" : "Day"} sub={dateLabel}>
        <Link to="/" search={{ date: t.prev }} className="btn btn-secondary btn-sm" aria-label="Previous day">
          Earlier
        </Link>
        {!t.is_today ? (
          <Link to="/" search={{}} className="btn btn-secondary btn-sm">
            Today
          </Link>
        ) : null}
        <Link to="/" search={{ date: t.next }} className="btn btn-secondary btn-sm" aria-label="Next day">
          Later
        </Link>
        <GlobalActions timerChip={!t.is_today} />
      </PageHead>

      <Notice text={notice} onClose={() => setNotice(null)} />

      <DayArc
        wakeStart={t.wake_start}
        wakeEnd={t.wake_end}
        now={now}
        isToday={t.is_today}
        blocks={t.actual.map((b) => ({ t_start: b.t_start, t_end: b.t_end, committed: b.committed, bare: isBare(b) }))}
        events={timed.map((e) => ({ key: `${e.calendar_id}/${e.event_id}`, title: e.title ?? "(no title)", t_start: e.t_start }))}
        hm={hm}
        below={t.is_today && !t.running && picking ? <TimerPicker data={data} title={timerTitle} onTitle={setTimerTitle} onStart={startTimer} /> : null}
      >
        {t.is_today ? (
          <>
            <p className="cond text-[56px] leading-none font-semibold tracking-[0.03em] md:text-[62px]">
              {remaining > 0 ? duration(remaining) : now < wakeStart ? "Not yet" : duration(recorded)}
              {remaining > 0 || now >= wakeStart ? (
                <em className="ml-3 text-[20px] font-medium tracking-[0.08em] text-or not-italic">{remaining > 0 ? "remaining" : "recorded"}</em>
              ) : null}
            </p>
            <p className="mt-2 text-body text-mute">
              {remaining > 0 ? "of the waking day" : now < wakeStart ? `The day starts at ${clock(t.wake_start)}` : "The waking day is over"}
              {next ? ` · next ${clock(next.t_start)} ${next.title ?? ""}, in ${duration((new Date(next.t_start).getTime() - now) / 1000)}` : ""}
            </p>
            <TimerCard
              data={data}
              now={now}
              busy={busy}
              act={act}
              onStart={startTimer}
              picking={picking}
              onTogglePicker={() => setPicking((v) => !v)}
              onRelabel={() => t.running && setSelected(t.running.id)}
            />
          </>
        ) : (
          <>
            <p className="cond text-[56px] leading-none font-semibold tracking-[0.03em]">{duration(recorded)}</p>
            <p className="mt-2 text-body text-mute">recorded on this day</p>
          </>
        )}
      </DayArc>

      <Lanes t={t} now={now} selected={selected} onSelect={setSelected} hm={hm} clock={clock} />

      {selectedBlock ? (
        <BlockPanel key={selectedBlock.id} block={selectedBlock} data={data} busy={busy} act={act} clock={clock} onClose={() => setSelected(null)} />
      ) : null}

      <div className="grid gap-[22px] lg:grid-cols-[1.22fr_1fr_1.08fr]">
        <NeedsYou data={data} busy={busy} act={act} />
        <DueSoon data={data} busy={busy} act={act} />
        <Goals goals={data.goals} />
      </div>

      {data.brief ? <Brief body={data.brief} opened={!!search.brief} /> : null}

      {data.record && t.date <= data.yesterday ? <DayRecord record={data.record} timezone={t.timezone} highlight={search.obs} /> : null}

      <AskAgent about={`Today, ${t.date}`} busy={busy} act={act} setNotice={setNotice} />

      {data.metricPrompt && (!t.is_today || data.localHour >= 17) ? (
        <Panel title={`${data.metricPrompt.title}?`}>
          <div className="flex flex-wrap gap-2">
            {[1, 2, 3, 4, 5].map((v) => (
              <button key={v} className="btn btn-secondary min-w-10" disabled={busy} onClick={() => act(() => metricFn({ data: { id: data.metricPrompt!.id, value: v } }), "Noted.")}>
                {v}
              </button>
            ))}
          </div>
        </Panel>
      ) : null}

      {/* The day's two measures, calendar confirmed and time accounted for, are on /status. */}
      <footer className="flex justify-end border-t border-line pt-3">
        <button
          className="btn btn-quiet btn-sm"
          onClick={() =>
            act(async () => {
              const r = await resetVisibilityFn({ data: {} });
              setNotice(r.moved === 0 ? "Nothing overdue to clear." : `Moved ${r.moved} overdue to Later. Nothing was deleted.`);
            })
          }
        >
          Too much? Reset visibility
        </button>
      </footer>
    </div>
  );
}

function TimerCard({ data, now, busy, act, onStart, picking, onTogglePicker, onRelabel }: {
  data: Data; now: number; busy: boolean; act: Act; onStart: (intentId: string) => void; picking: boolean; onTogglePicker: () => void; onRelabel: () => void;
}) {
  const running = data.today.running;

  if (running) {
    const paused = running.paused_at !== null;
    const until = paused ? new Date(running.paused_at!).getTime() : now;
    const elapsed = Math.max(0, (until - new Date(running.t_start).getTime()) / 1000);
    return (
      <div className="mt-[18px] inline-flex flex-wrap items-center justify-center gap-3 rounded-sm border border-edge bg-panel/90 py-1.5 pr-2 pl-4">
        <span className={`size-1.5 rounded-full bg-or ${paused ? "opacity-40" : ""}`} />
        <button className="max-w-[260px] truncate text-left font-medium hover:text-or" onClick={onRelabel} title="Change label">
          {running.intent ?? "Uncategorized"}
          {running.title ? <span className="text-mute"> — {running.title}</span> : null}
        </button>
        <span className="cond min-w-[42px] text-[21px] leading-none font-semibold text-or">{duration(elapsed)}</span>
        <button className="btn btn-quiet btn-sm" disabled={busy} onClick={() => act(() => timerFn({ data: { action: paused ? "resume" : "pause" } }))}>
          {paused ? "Resume" : "Pause"}
        </button>
        <button className="btn btn-secondary btn-sm" disabled={busy} onClick={() => act(() => timerFn({ data: { action: "stop" } }))}>
          Stop
        </button>
      </div>
    );
  }

  return (
    <div className="mt-[18px] flex w-full flex-col items-center gap-2">
      <div className="flex flex-wrap justify-center gap-2">
        {data.recent.slice(0, 3).map((r) => (
          <button
            key={r.id}
            className="btn btn-secondary btn-sm max-w-[200px] truncate"
            disabled={busy}
            onClick={() => onStart(r.id)}
          >
            {r.title}
          </button>
        ))}
        <button className="btn btn-quiet btn-sm" aria-expanded={picking} aria-controls="timer-picker" onClick={onTogglePicker}>
          {picking ? "Close" : "Something else…"}
        </button>
      </div>
    </div>
  );
}

/** "Something else…": a title and any intent to start the timer on. */
function TimerPicker({ data, title, onTitle, onStart }: {
  data: Data; title: string; onTitle: (title: string) => void; onStart: (intentId: string) => void;
}) {
  return (
    <div id="timer-picker" className="w-full max-w-[560px] space-y-2 rounded-sm bg-panel p-3 text-left">
      <input className="input" placeholder="What are you doing? (optional)" value={title} onChange={(e) => onTitle(e.target.value)} />
      <IntentPicker intents={data.intents} recent={data.recent} onPick={(i) => onStart(i.id)} />
    </div>
  );
}

/** The block a link names: by id, or the actual block covering a local HH:MM on the day shown. */
function linkedBlock(t: Data["today"], ref: string | undefined): string | null {
  if (!ref) return null;
  const blocks = t.running ? [...t.actual, t.running] : t.actual;
  if (!/^\d{2}:\d{2}$/.test(ref)) return blocks.find((b) => b.id === ref)?.id ?? null;
  const at = localTimeOnDay(t.timezone, t.date, ref).getTime();
  const covers = (b: Block) => new Date(b.t_start).getTime() <= at && (b.t_end ? new Date(b.t_end).getTime() : Infinity) > at;
  return t.actual.find(covers)?.id ?? null;
}

function Lanes({ t, now, selected, onSelect, hm, clock }: {
  t: Data["today"]; now: number; selected: string | null; onSelect: (id: string) => void; hm: (ms: number) => string; clock: (iso: string) => string;
}) {
  const from = new Date(t.day_start).getTime();
  const to = new Date(t.day_end).getTime();
  const x = (ms: number) => `${((Math.min(Math.max(ms, from), to) - from) / (to - from)) * 100}%`;
  const share = (a: number, b: number) => Math.max(0.0015, (Math.min(b, to) - Math.max(a, from)) / (to - from));
  const w = (a: number, b: number) => `${share(a, b) * 100}%`;
  // A label only where it fits: about 7.5px a character on a lane roughly 1100px wide.
  const fits = (a: number, b: number, text: string | null) => share(a, b) * 1100 > Math.min(text?.length ?? 0, 14) * 7.5 + 8;
  const ticks: number[] = [];
  for (let h = from; h <= to; h += 4 * 3_600_000) ticks.push(h);
  const timed = t.planned.filter((e) => !e.all_day);
  const allDay = t.planned.filter((e) => e.all_day);
  // One free slot: the next stretch from now that a task fits. The server gives each stretch its own task.
  const slot = t.gaps.find((g) => g.fits);
  const unlabelled = t.actual.some((b) => !b.intent);
  const showNow = t.is_today && now >= from && now <= to;

  return (
    <section aria-label="Planned and actual" className="space-y-1.5">
      {allDay.length > 0 ? (
        <p className="flex flex-wrap gap-2 pl-[66px]">
          {allDay.map((e) => (
            <span key={e.event_id} className="chip border-cy/50 text-cy">
              {e.title ?? "(no title)"}
            </span>
          ))}
        </p>
      ) : null}
      <div className="grid grid-cols-[66px_1fr] items-center gap-y-1.5">
        <span />
        <div className="relative h-4 text-small text-dim">
          {ticks.map((h, i) => (
            <span
              key={h}
              className="absolute"
              style={{ left: x(h), transform: i === 0 ? "none" : i === ticks.length - 1 ? "translateX(-100%)" : "translateX(-50%)" }}
            >
              {hm(h).slice(0, 2)}
            </span>
          ))}
        </div>

        <span className="text-small text-mute">Planned</span>
        <Track showNow={showNow} nowX={x(now)}>
          {timed.map((e) => (
            <a
              key={`${e.calendar_id}/${e.event_id}`}
              href={e.html_link ?? undefined}
              target="_blank"
              rel="noreferrer"
              className={`absolute top-1 bottom-1 overflow-hidden px-1 text-small leading-4 whitespace-nowrap text-cy ${e.fills ? "border border-cy/80 bg-cy/12" : "border border-dashed border-cy/40"}`}
              style={{ left: x(new Date(e.t_start).getTime()), width: w(new Date(e.t_start).getTime(), new Date(e.t_end).getTime()) }}
              title={`${clock(e.t_start)}–${clock(e.t_end)} ${e.title ?? ""}`}
            >
              {fits(new Date(e.t_start).getTime(), new Date(e.t_end).getTime(), e.title) ? e.title : null}
            </a>
          ))}
        </Track>

        <span className="text-small text-mute">Actual</span>
        <Track showNow={showNow} nowX={x(now)}>
          {t.actual.map((b) => {
            const a = new Date(b.t_start).getTime();
            const z = b.t_end ? new Date(b.t_end).getTime() : b.paused_at ? new Date(b.paused_at).getTime() : now;
            const look = b.committed
              ? "bg-or-deep text-txt"
              : !b.t_end
                ? "border border-or bg-or/15 text-or"
                : isBare(b) || b.rejected
                  ? "border border-dashed border-[#7a5530] text-dim"
                  : "border border-dashed border-or/80 text-mute";
            return (
              <button
                key={b.id}
                className={`absolute top-1 bottom-1 overflow-hidden px-1 text-left text-small leading-4 whitespace-nowrap ${look} ${selected === b.id ? "outline-2 outline-offset-1 outline-txt" : ""}`}
                style={{ left: x(a), width: w(a, z) }}
                onClick={() => onSelect(b.id)}
                title={`${clock(b.t_start)}–${b.t_end ? clock(b.t_end) : "now"} ${blockName(b)}${b.title && b.title !== blockName(b) ? ` · ${b.title}` : ""}`}
              >
                {fits(a, z, blockName(b)) ? blockName(b) : null}
              </button>
            );
          })}
        </Track>
      </div>
      <div className="flex flex-wrap justify-between gap-x-6 gap-y-1 pl-[66px] text-small text-mute">
        <span className="flex items-center gap-2">
          <LegendMark />
          {unlabelled ? <span>Click a block to label it.</span> : null}
        </span>
        {t.phone ? (
          <span>
            Phone today <b className="font-medium text-txt">{duration(t.phone.screen_s)}</b> on screen · <b className="font-medium text-txt">{t.phone.pickups}</b> pickups ·{" "}
            <b className="font-medium text-txt">{t.phone.glances}</b> glances
            {t.phone.top.length > 0 ? ` · most ${t.phone.top.map((a) => a.name).join(", ")}` : ""}
          </span>
        ) : null}
      </div>
      {slot ? (
        <p className="pl-[66px]">
          <span className="chip max-w-full flex-wrap whitespace-normal" data-free-slot>
            {duration(slot.minutes * 60)} free at {clock(slot.t_start)}
            <span className="text-dim">· fits {slot.fits}</span>
          </span>
        </p>
      ) : null}
    </section>
  );
}

/** What the lanes' looks mean, behind a help mark: shown on hover, on focus, and held open by a click. */
function LegendMark() {
  const [open, setOpen] = useState(false);
  // Escape hides it even while hovered or focused, until the pointer or focus leaves.
  const [dismissed, setDismissed] = useState(false);
  const reset = () => {
    setOpen(false);
    setDismissed(false);
  };
  const ref = useRef<HTMLSpanElement>(null);
  // On the window: a pointer resting on the mark leaves the keyboard's focus elsewhere.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = ref.current;
      if (e.key !== "Escape" || !el || !(el.matches(":hover") || el.contains(document.activeElement) || open)) return;
      setOpen(false);
      setDismissed(true);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open]);
  return (
    <span ref={ref} className="group relative inline-flex" onMouseLeave={() => setDismissed(false)}>
      <button
        type="button"
        className="inline-grid size-5 place-content-center rounded-full border border-edge text-small leading-none text-mute hover:border-or/70 hover:text-txt"
        aria-label="What the lanes mean"
        aria-expanded={open}
        aria-controls="lanes-legend"
        onClick={() => {
          setDismissed(false);
          setOpen((v) => !v);
        }}
        onBlur={reset}
      >
        ?
      </button>
      <span
        id="lanes-legend"
        role="note"
        className={`absolute bottom-full left-0 z-20 mb-1.5 w-max max-w-[min(520px,80vw)] rounded-sm border border-edge bg-raised px-2.5 py-1.5 text-small text-mute shadow-lg ${open ? "block" : dismissed ? "hidden" : "hidden group-hover:block group-has-[:focus-visible]:block"}`}
      >
        Solid: counted · dashed: a suggestion · faint: unlabelled phone or place · outlined: running.
      </span>
    </span>
  );
}

function Track({ children, showNow, nowX }: { children: React.ReactNode; showNow: boolean; nowX: string }) {
  return (
    <div
      className="relative h-[26px] bg-well"
      style={{ backgroundImage: "repeating-linear-gradient(90deg, transparent 0 calc(100% / 24 - 1px), #171920 calc(100% / 24 - 1px) calc(100% / 24))" }}
    >
      {children}
      {showNow ? <span className="pointer-events-none absolute -top-1 -bottom-1 w-px bg-or" style={{ left: nowX }} /> : null}
    </div>
  );
}

/**
 * The brief's first sentence on one line; the rest opens below it. The button also shows when the sentence alone
 * is cut off at this width, so a one-sentence brief can still be read in full by touch or keyboard.
 */
function Brief({ body, opened }: { body: string; opened: boolean }) {
  const [open, setOpen] = useState(opened);
  const { lead, rest } = splitBrief(body);
  const leadRef = useRef<HTMLParagraphElement>(null);
  const [clipped, setClipped] = useState(false);
  // Measured only while closed: open, the sentence wraps and never overflows.
  useEffect(() => {
    const el = leadRef.current;
    if (!el || open) return;
    const check = () => setClipped(el.scrollWidth > el.clientWidth);
    check();
    const ro = new ResizeObserver(check);
    ro.observe(el);
    return () => ro.disconnect();
  }, [open, lead]);
  return (
    <Panel bodyClass="py-3">
      <div className="flex items-baseline gap-3">
        <h2 className="h-sec shrink-0">This morning</h2>
        <p id="brief-lead" ref={leadRef} className={`min-w-0 flex-1 text-body ${open ? "" : "truncate"}`} title={open ? undefined : lead}>
          {lead}
        </p>
        {rest || clipped ? (
          <button
            className="btn btn-quiet btn-sm shrink-0"
            aria-expanded={open}
            aria-controls={rest ? "brief-lead brief-rest" : "brief-lead"}
            onClick={() => setOpen((v) => !v)}
          >
            {open ? "Less" : rest ? "The rest" : "More"}
          </button>
        ) : null}
      </div>
      {rest ? (
        <div id="brief-rest" hidden={!open} className="mt-2 whitespace-pre-wrap text-body leading-relaxed">
          {rest}
          <p className="mt-2 text-small text-mute">From the agent's morning run.</p>
        </div>
      ) : null}
    </Panel>
  );
}

/** Goes to the agent's thread; the answer comes to WhatsApp. */
function AskAgent({ about, busy, act, setNotice }: { about: string; busy: boolean; act: Act; setNotice: (s: string) => void }) {
  const [text, setText] = useState("");
  const send = () => {
    const question = text.trim();
    if (!question) return;
    void act(async () => {
      await askAgentFn({ data: { text: question, about } });
      setText("");
      setNotice("Sent. The answer will come to WhatsApp.");
    });
  };
  return (
    <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); send(); }}>
      <input
        className="input min-w-0 flex-1"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Ask the agent, or hand it something"
        aria-label="Ask the agent"
      />
      <button className="btn btn-secondary" disabled={busy || !text.trim()}>Send</button>
    </form>
  );
}

function NeedsYou({ data, busy, act }: { data: Data; busy: boolean; act: Act }) {
  return (
    <Panel title="Needs you" aside={data.needsYou.length > 0 ? `${data.needsYou.length} flagged by the agent` : undefined}>
      {data.needsYou.length === 0 ? <Empty>Nothing flagged. The agent will put things here when it needs you.</Empty> : null}
      <ul>
        {data.needsYou.map((n) => (
          <li key={n.id} className="py-2.5">
            <p className="flex items-baseline gap-2.5 font-medium">
              <Star size={9} className="translate-y-[-1px] text-or" />
              {n.title}
            </p>
            {n.note ? <p className="mt-0.5 mb-2 ml-[19px] text-body text-mute">{n.note}</p> : null}
            <div className="ml-[19px] flex items-center gap-2">
              {n.google ? (
                <span className="text-small text-dim">Complete it in Google Tasks</span>
              ) : (
                <button className="btn btn-primary btn-sm" disabled={busy} onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: true } }))}>
                  Done
                </button>
              )}
              <button className="btn btn-quiet btn-sm" disabled={busy} onClick={() => act(() => unflagFn({ data: { id: n.id } }))}>
                Seen
              </button>
            </div>
          </li>
        ))}
      </ul>
      {data.agentTasks.length > 0 ? (
        <div className="mt-auto border-t border-line pt-2">
          <span className="tag-agent">with the agent</span>
          <ul className="mt-1 text-small">
            {data.agentTasks.map((a) => (
              <li key={a.id} className="py-0.5">
                <Link to="/tasks" search={{ view: "agent", id: a.id }} className="text-mute hover:text-or">
                  {a.title}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </Panel>
  );
}

function DueSoon({ data, busy, act }: { data: Data; busy: boolean; act: Act }) {
  const open = data.tasks.filter((x) => !x.done);
  const shown = [...open.slice(0, 6), ...data.tasks.filter((x) => x.done)];
  return (
    <Panel
      title="Due soon"
      aside={
        <Link to="/tasks" className="hover:text-txt">
          All {open.length} tasks
        </Link>
      }
    >
      {shown.length === 0 ? <Empty>No open tasks.</Empty> : null}
      <ul>
        {shown.map((task) => (
          <li key={task.id} className="flex items-start gap-3 py-2">
            <TaskCheck id={task.id} title={task.title} done={task.done} google={task.google} act={act} />
            <span className="min-w-0">
              <span className={task.done ? "text-dim line-through" : ""}>{task.title}</span>
              <small className="block text-small text-mute">
                {[task.done ? "done today" : task.due ? `due ${shortDate(task.due)}` : null, task.waiting_on ? `waiting on ${task.waiting_on}` : null, task.parent, task.google ? "Google Tasks" : null]
                  .filter(Boolean)
                  .join(" · ")}
              </small>
            </span>
          </li>
        ))}
      </ul>
    </Panel>
  );
}

function Goals({ goals }: { goals: Data["goals"] }) {
  return (
    <Panel title="Goals this week" aside="hours / target">
      {goals.length === 0 ? <Empty>No goals with an hour target yet. Set one in Goals.</Empty> : null}
      <ul>
        {goals.map((g) => (
          <li key={g.id} className="grid grid-cols-[1fr_60px_auto] items-center gap-3 py-[7px]">
            <Link to="/goals" search={{ id: g.id }} className={`hover:text-or ${g.hours > 0 ? "text-txt" : "text-mute"}`}>
              {g.title}
            </Link>
            <Meter value={g.hours} target={g.target} />
            <span className={`text-right text-small whitespace-nowrap ${g.hours > 0 ? "text-txt" : "text-mute"}`}>
              {round1(g.hours)} / {round1(g.target)}h
            </span>
          </li>
        ))}
      </ul>
      {goals.some((g) => g.hours === 0) ? <p className="mt-2 text-small text-dim">Quiet rows have no hours yet this week.</p> : null}
    </Panel>
  );
}

const round1 = (n: number) => String(Math.round(n * 10) / 10);

const SOURCE: Record<string, string> = { android: "phone", gmail: "mail", calendar: "calendar", agent: "agent", manual: "you", google_tasks: "tasks", system: "system" };

/** A day gone by: what the nightly run wrote about it, and everything recorded on it. */
function DayRecord({ record, timezone, highlight }: { record: NonNullable<Data["record"]>; timezone: string; highlight?: string }) {
  const clock = (iso: string) => new Intl.DateTimeFormat("en-GB", { timeZone: timezone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(iso));
  return (
    <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <Panel
        title="The day, written up"
        aside={
          record.page ? (
            <Link to="/notes" search={{ slug: record.page.slug }} className="btn btn-secondary btn-sm">
              Open in Notes
            </Link>
          ) : undefined
        }
        bodyClass="px-7 py-5"
      >
        {record.page ? <Markdown source={record.page.body} /> : <Empty>The nightly run did not write this day up.</Empty>}
      </Panel>
      <Panel title="Recorded" aside={record.timeline.length ? `${record.timeline.length}` : undefined}>
        {record.timeline.length === 0 ? <Empty>Nothing was recorded on this day.</Empty> : null}
        <ul>
          {record.timeline.map((o) => (
            <li
              key={o.id}
              className={`grid grid-cols-[48px_72px_minmax(0,1fr)] items-baseline gap-x-3 border-t border-line py-1.5 first:border-t-0 ${o.bulk ? "opacity-50" : ""} ${highlight && o.ids.includes(highlight) ? "row-sel" : ""}`}
            >
              <span className="text-small text-mute">{clock(o.t)}</span>
              <span className={`text-small ${o.source === "agent" ? "text-cy" : "text-dim"}`}>{SOURCE[o.source] ?? o.source}</span>
              <span className="min-w-0">
                {o.title ?? "(untitled)"}
                {o.detail ? <span className="text-mute"> · {o.detail}</span> : null}
                {o.bulk ? <span className="text-dim"> · bulk</span> : null}
              </span>
            </li>
          ))}
        </ul>
      </Panel>
    </div>
  );
}

function BlockPanel({ block: b, data, busy, act, clock, onClose }: {
  block: Block; data: Data; busy: boolean; act: Act; clock: (iso: string) => string; onClose: () => void;
}) {
  const date = data.today.date;
  const [remember, setRemember] = useState(false);
  const [start, setStart] = useState(clock(b.t_start));
  const [end, setEnd] = useState(b.t_end ? clock(b.t_end) : "");
  const [splitAt, setSplitAt] = useState("");
  const calendar = b.origin === "calendar";
  const fill = calendar || b.origin === "auto_activity" || b.origin === "place";
  const closed = b.t_end !== null;

  return (
    <Panel
      framed
      title={
        <>
          {blockName(b)}
          {b.title && b.title !== blockName(b) ? <span className="ml-2 font-sans text-body font-normal tracking-normal text-mute normal-case">{b.title}</span> : null}
        </>
      }
      aside={
        <button className="btn btn-quiet btn-sm" onClick={onClose}>
          Close
        </button>
      }
    >
      <p className="text-body text-mute">
        {clock(b.t_start)}–{b.t_end ? clock(b.t_end) : "now"} · {fromWhere(b.origin)} ·{" "}
        {b.committed
          ? `confirmed${b.committed_via && b.committed_via !== "ui" ? ` by ${b.committed_via}` : ""}`
          : b.proposal
            ? `suggested${b.proposal.via === "agent" ? " by the agent" : ""}${b.proposal.reason ? `: ${b.proposal.reason}` : ""}`
            : b.rejected
              ? "you said this label was wrong; pick the right one, or leave it"
              : isBare(b)
                ? "nothing to confirm; label it if it was for something"
                : "not confirmed"}
        {b.mismatch ? <span className="ml-2 chip border-or/60 text-or">{b.mismatch}</span> : null}
      </p>

      <div className="mt-3 grid gap-5 lg:grid-cols-[1fr_auto]">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-body font-medium">{b.committed ? "Change it to" : "It was"}</span>
            {b.proposal && closed ? (
              <>
                <button className="btn btn-primary btn-sm" disabled={busy} onClick={() => act(() => confirmFn({ data: { date, accept: [b.proposal!.id] } }))}>
                  Yes, {b.intent}
                </button>
                <button className="btn btn-secondary btn-sm" disabled={busy} onClick={() => act(() => confirmFn({ data: { date, reject: [b.proposal!.id] } }))}>
                  Not this
                </button>
              </>
            ) : null}
            {b.committed ? (
              <button className="btn btn-secondary btn-sm" disabled={busy} onClick={() => act(() => uncommitFn({ data: { blockId: b.id } }), "Unconfirmed.")}>
                Undo confirm
              </button>
            ) : null}
          </div>
          <IntentPicker
            inputId="relabel-search"
            intents={data.intents}
            recent={data.recent}
            onPick={(i: PickIntent) => act(() => recategorizeFn({ data: { blockId: b.id, intentId: i.id, rememberTitle: calendar && remember, remember: !calendar && remember } })).then(onClose)}
          />
          {(calendar && b.title) || b.origin === "auto_activity" || b.origin === "place" ? (
            <label className="flex items-center gap-2 text-body text-mute">
              <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
              {calendar
                ? `Label every “${b.title}” this way from now on`
                : b.origin === "place"
                  ? `Label time ${b.title ? b.title.replace(/^At /, "at ") : "here"} this way from now on`
                  : "Label this app this way from now on"}
            </label>
          ) : null}
        </div>

        {closed ? (
          <div className="flex flex-wrap items-end gap-2 lg:max-w-[360px] lg:border-l lg:border-line lg:pl-5">
            {!fill ? (
              <>
                <label className="flex flex-col gap-1 text-small text-mute">
                  Start
                  <input className="input w-[136px]" type="time" value={start} onChange={(e) => setStart(e.target.value)} />
                </label>
                <label className="flex flex-col gap-1 text-small text-mute">
                  End
                  <input className="input w-[136px]" type="time" value={end} onChange={(e) => setEnd(e.target.value)} />
                </label>
                <button className="btn btn-secondary" disabled={busy} onClick={() => act(() => editBlockFn({ data: { blockId: b.id, date, start, end } }), "Times changed.")}>
                  Save times
                </button>
              </>
            ) : null}
            <label className="flex flex-col gap-1 text-small text-mute">
              Split at
              <input className="input w-[136px]" type="time" value={splitAt} onChange={(e) => setSplitAt(e.target.value)} />
            </label>
            <button
              className="btn btn-secondary"
              disabled={busy || !splitAt}
              onClick={() => act(() => splitBlockFn({ data: { blockId: b.id, date, at: splitAt } }), fill ? "Split. Label one half now, or the next sync will join them again." : "Split.")}
            >
              Split
            </button>
            {!fill ? (
              <button className="btn btn-secondary btn-danger" disabled={busy} onClick={() => act(() => deleteBlockFn({ data: { blockId: b.id } }), "Deleted.").then(onClose)}>
                Delete
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
    </Panel>
  );
}
