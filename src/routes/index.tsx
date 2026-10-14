import { useEffect, useMemo, useRef, useState } from "react";
import { Link, createFileRoute, useRouter } from "@tanstack/react-router";
import { z } from "zod";
import { duration, pct } from "../components/format.js";
import { IntentPicker, type PickIntent } from "../components/IntentPicker.js";
import { metricFn, resetVisibilityFn, taskDoneFn, unflagFn } from "../server/functions/graph.js";
import {
  confirmFn, deleteBlockFn, editBlockFn, getTodayFn, recategorizeFn, splitBlockFn, timerFn, uncommitFn,
} from "../server/functions/today.js";

export const Route = createFileRoute("/")({
  validateSearch: (search) => z.object({ date: z.string().optional() }).parse(search),
  loaderDeps: ({ search }) => ({ date: search.date }),
  loader: ({ deps }) => getTodayFn({ data: { date: deps.date } }),
  component: Today,
});

type Data = Awaited<ReturnType<typeof getTodayFn>>;
type Block = Data["today"]["actual"][number];
type Planned = Data["today"]["planned"][number];

/** Phone or place time nobody has labelled: shown, never asked about. */
const isBare = (b: Block) => (b.origin === "auto_activity" || b.origin === "place") && !b.intent && !b.committed && !b.proposal;
const blockName = (b: Block) => b.intent ?? (b.origin === "auto_activity" ? "Phone" : b.origin === "place" ? (b.title ?? "Somewhere") : "Unlabelled");
const fromWhere = (origin: string) =>
  origin === "calendar" ? "from the calendar" : origin === "auto_activity" ? "from your phone" : origin === "place" ? "from where you were" : "timer";

const PX_PER_MIN = 0.9;
const MIN_BLOCK_PX = 22;

function useNow(everyMs: number, initial: string): number {
  const [now, setNow] = useState(() => new Date(initial).getTime());
  useEffect(() => {
    setNow(Date.now());
    const id = setInterval(() => setNow(Date.now()), everyMs);
    return () => clearInterval(id);
  }, [everyMs]);
  return now;
}

function Today() {
  const data = Route.useLoaderData();
  const { today: t } = data;
  const router = useRouter();
  const now = useNow(20_000, t.now);
  const [selected, setSelected] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const fmt = useMemo(() => new Intl.DateTimeFormat("en-US", { timeZone: t.timezone, hour: "numeric", minute: "2-digit" }), [t.timezone]);
  const hm24 = useMemo(() => new Intl.DateTimeFormat("en-GB", { timeZone: t.timezone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }), [t.timezone]);
  const clock = (iso: string) => fmt.format(new Date(iso));

  const act = async (fn: () => Promise<unknown>, done?: string) => {
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

  // A block belongs to the day it starts, the same rule "Accept all" uses.
  const toConfirm = t.actual.filter(
    (b) => b.proposal && b.t_end && new Date(b.t_end).getTime() <= now && new Date(b.t_start).getTime() >= new Date(t.day_start).getTime(),
  );
  const acceptAll = () => act(() => confirmFn({ data: { date: t.date, all: true } }), "Confirmed.");

  // Enter confirms everything labelled; `r` jumps to relabelling. Never while typing.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = e.target as HTMLElement | null;
      if (el && (el.tagName === "INPUT" || el.tagName === "TEXTAREA" || el.tagName === "SELECT" || el.tagName === "BUTTON")) return;
      if (e.key === "Enter" && toConfirm.length > 0) {
        e.preventDefault();
        void acceptAll();
      }
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
  const dateLabel = new Intl.DateTimeFormat("en-US", { timeZone: "UTC", weekday: "long", month: "short", day: "numeric" }).format(new Date(`${t.date}T12:00:00Z`));
  const remaining = t.is_today ? Math.max(0, (new Date(t.wake_end).getTime() - Math.max(now, new Date(t.wake_start).getTime())) / 1000) : 0;
  const wakeSeconds = (new Date(t.wake_end).getTime() - new Date(t.wake_start).getTime()) / 1000;

  return (
    <div className="space-y-5">
      <header className="flex items-center justify-between gap-2">
        <Link to="/" search={{ date: t.prev }} className="btn" aria-label="Previous day">
          Earlier
        </Link>
        <div className="text-center">
          <h1 className="text-2xl font-bold">{t.is_today ? "Today" : dateLabel}</h1>
          {t.is_today ? <p className="muted text-sm">{dateLabel}</p> : (
            <Link to="/" search={{}} className="text-sm underline">
              back to today
            </Link>
          )}
        </div>
        <Link to="/" search={{ date: t.next }} className="btn" aria-label="Next day">
          Later
        </Link>
      </header>

      {t.is_today ? (
        <section aria-label="Time left today">
          <div className="h-4 overflow-hidden rounded-full bg-black/10 dark:bg-white/15">
            <div className="h-full rounded-full bg-rail" style={{ width: `${Math.min(100, (remaining / wakeSeconds) * 100)}%` }} />
          </div>
          <p className="mt-1 text-sm">
            <span className="font-semibold">{remaining > 0 ? `${duration(remaining)} left today` : "The day is done."}</span>
            <span className="muted"> · until {clock(t.wake_end)}</span>
          </p>
        </section>
      ) : null}

      {notice ? (
        <p className="card text-sm" role="status">
          {notice}{" "}
          <button className="underline" onClick={() => setNotice(null)}>
            dismiss
          </button>
        </p>
      ) : null}

      {data.needsYou.length > 0 ? (
        <section className="card space-y-2">
          <h2 className="text-lg font-semibold">Needs you</h2>
          <ul className="divide-y divide-black/10 dark:divide-white/10">
            {data.needsYou.map((n) => (
              <li key={n.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2">
                <div className="min-w-0 flex-1">
                  <p className="font-medium">{n.title}</p>
                  <p className="muted text-sm">{n.note}</p>
                </div>
                {n.google ? (
                  <span className="muted text-sm">complete in Google</span>
                ) : (
                  <button className="btn" disabled={busy} onClick={() => act(() => taskDoneFn({ data: { id: n.id, done: true } }))}>
                    Done
                  </button>
                )}
                <button className="btn" disabled={busy} onClick={() => act(() => unflagFn({ data: { id: n.id } }))}>
                  Seen
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {t.is_today ? <Timer data={data} now={now} busy={busy} act={act} onRelabel={() => t.running && setSelected(t.running.id)} clock={clock} /> : null}

      {toConfirm.length > 0 ? (
        <section className="card space-y-3">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">
              {toConfirm.length} to confirm
            </h2>
            <button className="btn btn-primary" disabled={busy} onClick={acceptAll}>
              Accept all
            </button>
          </div>
          <ul className="divide-y divide-black/10 dark:divide-white/10">
            {toConfirm.map((b) => (
              <li key={b.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2">
                <span className="muted w-32 shrink-0 text-sm tabular-nums">
                  {clock(b.t_start)}–{clock(b.t_end!)}
                </span>
                <button className="min-w-0 flex-1 text-left" onClick={() => setSelected(b.id)}>
                  <span className="font-medium">{b.intent ?? "Unlabelled"}</span>
                  {b.title ? <span className="muted"> · {b.title}</span> : null}
                  {b.proposal?.via === "agent" ? <span className="chip ml-2">agent suggests</span> : null}
                </button>
                <button className="btn" disabled={busy} onClick={() => act(() => confirmFn({ data: { date: t.date, accept: [b.proposal!.id] } }))}>
                  Yes
                </button>
                <button className="btn" disabled={busy} onClick={() => act(() => confirmFn({ data: { date: t.date, reject: [b.proposal!.id] } }))}>
                  No
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {t.gaps.length > 0 ? (
        <section className="flex flex-wrap gap-2" aria-label="Free stretches">
          {t.gaps.slice(0, 4).map((g) => (
            <span key={g.t_start} className="chip py-1 text-sm">
              {duration(g.minutes * 60)} free at {clock(g.t_start)}
              {g.fits ? <span className="muted">&nbsp;· fits: {g.fits}</span> : null}
            </span>
          ))}
        </section>
      ) : null}

      {t.phone ? (
        <p className="muted text-sm">
          Phone: {duration(t.phone.screen_s)} on screen · {t.phone.pickups} pickups · {t.phone.glances} glances
          {t.phone.top.length > 0 ? ` · most: ${t.phone.top.map((a) => a.name).join(", ")}` : ""}
        </p>
      ) : null}

      {data.starving.length > 0 || data.agentTasks.length > 0 ? (
        <section className="space-y-1 text-sm">
          {data.starving.map((g) => (
            <p key={g.id}>
              <span className="chip border-bad text-bad">starving</span> {g.title}{" "}
              <span className="muted">
                {g.hours}/{g.target}h this week
              </span>
            </p>
          ))}
          {data.agentTasks.length > 0 ? (
            <p>
              <span className="chip">with the agent</span> {data.agentTasks.map((a) => a.title).join(" · ")}
            </p>
          ) : null}
        </section>
      ) : null}

      <Timeline t={t} now={now} selected={selected} onSelect={setSelected} clock={clock} />

      {selectedBlock ? (
        <BlockPanel
          key={selectedBlock.id}
          block={selectedBlock}
          data={data}
          busy={busy}
          act={act}
          clock={clock}
          hm={(iso) => hm24.format(new Date(iso))}
          onClose={() => setSelected(null)}
        />
      ) : null}

      {data.metricPrompt && (!t.is_today || data.localHour >= 17) ? (
        <section className="card flex flex-wrap items-center gap-2">
          <span className="font-medium">{data.metricPrompt.title}?</span>
          {[1, 2, 3, 4, 5].map((v) => (
            <button key={v} className="btn" disabled={busy} onClick={() => act(() => metricFn({ data: { id: data.metricPrompt!.id, value: v } }), "Noted.")}>
              {v}
            </button>
          ))}
        </section>
      ) : null}

      <footer className="muted flex flex-wrap items-center justify-between gap-2 pt-4 text-sm">
        <span>
          Calendar confirmed: {pct(t.confirm_rate)}
          {t.coverage !== null ? ` · time accounted for: ${pct(t.coverage)}` : ""}
        </span>
        <button
          className="underline"
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

type Act = (fn: () => Promise<unknown>, done?: string) => Promise<void>;

function Timer({ data, now, busy, act, onRelabel, clock }: { data: Data; now: number; busy: boolean; act: Act; onRelabel: () => void; clock: (iso: string) => string }) {
  const running = data.today.running;
  const [picking, setPicking] = useState(false);
  const [title, setTitle] = useState("");
  const start = (intentId: string | null) =>
    act(() => timerFn({ data: { action: "start", intentId, title: title.trim() || null } })).then(() => {
      setPicking(false);
      setTitle("");
    });

  if (running) {
    const paused = running.paused_at !== null;
    const until = paused ? new Date(running.paused_at!).getTime() : now;
    const elapsed = Math.max(0, (until - new Date(running.t_start).getTime()) / 1000);
    return (
      <section className="card space-y-3 border-rail">
        <div>
          <p className="muted text-sm">
            {paused ? "Paused" : "Running"} since {clock(running.t_start)}
          </p>
          <p className="text-2xl font-semibold">
            {running.intent ?? "Uncategorized"}
            {running.title ? <span className="muted text-lg"> · {running.title}</span> : null}
          </p>
          <p className="text-4xl font-bold tabular-nums">{duration(elapsed)}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button className="btn btn-primary" disabled={busy} onClick={() => act(() => timerFn({ data: { action: "stop" } }))}>
            Stop
          </button>
          <button className="btn" disabled={busy} onClick={() => act(() => timerFn({ data: { action: paused ? "resume" : "pause" } }))}>
            {paused ? "Resume" : "Pause"}
          </button>
          <button className="btn" onClick={onRelabel}>
            Change label
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="card space-y-3">
      <h2 className="text-lg font-semibold">Start a timer</h2>
      <div className="flex flex-wrap gap-2">
        {data.recent.map((r) => (
          <button key={r.id} className="btn" disabled={busy} onClick={() => start(r.id)}>
            {r.title}
          </button>
        ))}
        <button className="btn" disabled={busy} onClick={() => start(null)}>
          Just start
        </button>
        <button className="btn" onClick={() => setPicking((v) => !v)}>
          {picking ? "Close" : "Something else…"}
        </button>
      </div>
      {picking ? (
        <div className="space-y-2">
          <input className="input" placeholder="What are you doing? (optional)" value={title} onChange={(e) => setTitle(e.target.value)} />
          <IntentPicker intents={data.intents} recent={data.recent} onPick={(i) => start(i.id)} />
        </div>
      ) : null}
    </section>
  );
}

function Timeline({ t, now, selected, onSelect, clock }: { t: Data["today"]; now: number; selected: string | null; onSelect: (id: string) => void; clock: (iso: string) => string }) {
  const timed = t.planned.filter((e) => !e.all_day);
  const allDay = t.planned.filter((e) => e.all_day);
  const ms = (iso: string) => new Date(iso).getTime();

  // The window drawn: the waking day, stretched to hold anything outside it.
  const HOUR = 3_600_000;
  let from = ms(t.wake_start);
  let to = ms(t.wake_end);
  for (const e of timed) {
    from = Math.min(from, ms(e.t_start));
    to = Math.max(to, ms(e.t_end));
  }
  for (const b of t.actual) {
    from = Math.min(from, ms(b.t_start));
    to = Math.max(to, b.t_end ? ms(b.t_end) : now);
  }
  from = Math.floor(from / HOUR) * HOUR;
  to = Math.ceil(to / HOUR) * HOUR;
  // A block that began yesterday or runs past midnight is drawn only for this day's part.
  const y = (at: number) => ((Math.min(Math.max(at, from), to) - from) / 60_000) * PX_PER_MIN;
  const hours: number[] = [];
  for (let h = from; h <= to; h += HOUR) hours.push(h);

  // Planned events that overlap sit side by side.
  const columns = new Map<string, number>();
  const ends: number[] = [];
  for (const e of [...timed].sort((a, b) => ms(a.t_start) - ms(b.t_start))) {
    let col = ends.findIndex((end) => end <= ms(e.t_start));
    if (col === -1) col = ends.length;
    ends[col] = ms(e.t_end);
    columns.set(e.event_id, col);
  }
  const columnCount = Math.max(1, ends.length);

  const box = (start: number, end: number) => ({ top: y(start), height: Math.max(MIN_BLOCK_PX, y(end) - y(start) - 2) });

  return (
    <section aria-label="Timeline">
      {allDay.length > 0 ? (
        <p className="mb-2 flex flex-wrap gap-2 text-sm">
          {allDay.map((e) => (
            <span key={e.event_id} className="chip">
              {e.title ?? "(no title)"}
            </span>
          ))}
        </p>
      ) : null}
      <div className="grid grid-cols-[3.25rem_1fr_1fr] gap-x-2 text-sm">
        <span />
        <span className="muted pb-1 font-medium">Planned</span>
        <span className="muted pb-1 font-medium">Actual</span>
        <div className="relative" style={{ height: y(to) }}>
          {hours.map((h) => (
            <span key={h} className="muted absolute -translate-y-1/2 text-xs tabular-nums" style={{ top: y(h) }}>
              {new Intl.DateTimeFormat("en-US", { timeZone: t.timezone, hour: "numeric" }).format(new Date(h))}
            </span>
          ))}
        </div>
        <div className="relative border-l border-black/10 dark:border-white/15" style={{ height: y(to) }}>
          {hours.map((h) => (
            <span key={h} className="absolute left-0 right-0 border-t border-black/5 dark:border-white/10" style={{ top: y(h) }} />
          ))}
          {timed.map((e: Planned) => {
            const col = columns.get(e.event_id) ?? 0;
            return (
              <div
                key={`${e.calendar_id}/${e.event_id}`}
                className={`absolute overflow-hidden rounded-md border px-1.5 py-0.5 leading-tight ${e.fills ? "border-black/20 bg-black/5 dark:border-white/25 dark:bg-white/10" : "border-dashed border-black/20 opacity-60 dark:border-white/25"}`}
                style={{ ...box(ms(e.t_start), ms(e.t_end)), left: `${(col / columnCount) * 100}%`, width: `calc(${100 / columnCount}% - 2px)` }}
                title={`${clock(e.t_start)}–${clock(e.t_end)}`}
              >
                {e.title ?? "(no title)"}
              </div>
            );
          })}
          {t.is_today ? <span className="absolute left-0 right-0 border-t-2 border-bad" style={{ top: y(now) }} /> : null}
        </div>
        <div className="relative border-l border-black/10 dark:border-white/15" style={{ height: y(to) }}>
          {hours.map((h) => (
            <span key={h} className="absolute left-0 right-0 border-t border-black/5 dark:border-white/10" style={{ top: y(h) }} />
          ))}
          {t.actual.map((b: Block) => {
            const end = b.t_end ? ms(b.t_end) : b.paused_at ? ms(b.paused_at) : now;
            const look = b.committed
              ? "border-rail bg-rail text-white"
              : isBare(b)
                ? "border-dashed border-black/15 bg-black/5 opacity-70 dark:border-white/20 dark:bg-white/10"
              : b.rejected
                ? "border-dashed border-black/25 opacity-50 dark:border-white/30"
                : b.t_end
                  ? "border-dashed border-rail bg-rail/10"
                  : "border-rail bg-rail/25";
            return (
              <button
                key={b.id}
                className={`absolute left-0 right-0 overflow-hidden rounded-md border px-1.5 py-0.5 text-left leading-tight ${look} ${selected === b.id ? "ring-2 ring-warn" : ""}`}
                style={box(ms(b.t_start), end)}
                onClick={() => onSelect(b.id)}
              >
                {blockName(b)}
                {b.title && b.title !== blockName(b) ? <span className="opacity-75"> · {b.title}</span> : null}
                {b.mismatch ? <span className="ml-1 rounded bg-warn px-1 text-xs text-white">{b.mismatch}</span> : null}
                {b.committed && b.committed_via === "agent" ? <span className="ml-1 rounded bg-white/25 px-1 text-xs">agent</span> : null}
              </button>
            );
          })}
          {t.is_today ? <span className="absolute left-0 right-0 border-t-2 border-bad" style={{ top: y(now) }} /> : null}
        </div>
      </div>
      {t.actual.length === 0 && timed.length === 0 ? <p className="muted mt-2 text-sm">Nothing planned and nothing tracked.</p> : null}
    </section>
  );
}

function BlockPanel({ block: b, data, busy, act, clock, hm, onClose }: {
  block: Block; data: Data; busy: boolean; act: Act; clock: (iso: string) => string; hm: (iso: string) => string; onClose: () => void;
}) {
  const date = data.today.date;
  const [remember, setRemember] = useState(false);
  const [start, setStart] = useState(hm(b.t_start));
  const [end, setEnd] = useState(b.t_end ? hm(b.t_end) : "");
  const [splitAt, setSplitAt] = useState("");
  const ref = useRef<HTMLElement>(null);
  useEffect(() => {
    // Newer Chrome returns a promise from scrollIntoView; an effect must not return it.
    void ref.current?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }, []);
  const calendar = b.origin === "calendar";
  const fill = calendar || b.origin === "auto_activity" || b.origin === "place";
  const closed = b.t_end !== null;

  return (
    <section ref={ref} className="card space-y-3 ring-2 ring-warn">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="muted text-sm">
            {clock(b.t_start)}–{b.t_end ? clock(b.t_end) : "now"} · {fromWhere(b.origin)}
          </p>
          <p className="text-xl font-semibold">
            {blockName(b)}
            {b.title && b.title !== blockName(b) ? <span className="muted text-base"> · {b.title}</span> : null}
          </p>
          <p className="text-sm">
            {b.committed
              ? `Confirmed${b.committed_via && b.committed_via !== "ui" ? ` by ${b.committed_via}` : ""}.`
              : b.proposal
                ? `Suggested${b.proposal.via === "agent" ? " by the agent" : ""}${b.proposal.reason ? `: ${b.proposal.reason}` : "."}`
                : b.rejected
                  ? "You said this label was wrong. Pick the right one, or leave it."
                  : isBare(b)
                    ? "Nothing to confirm. Label it if it was for something."
                    : "Not confirmed."}
          </p>
        </div>
        <button className="btn" onClick={onClose}>
          Close
        </button>
      </div>

      <div className="flex flex-wrap gap-2">
        {b.proposal && closed ? (
          <>
            <button className="btn btn-primary" disabled={busy} onClick={() => act(() => confirmFn({ data: { date, accept: [b.proposal!.id] } }))}>
              Yes, {b.intent}
            </button>
            <button className="btn" disabled={busy} onClick={() => act(() => confirmFn({ data: { date, reject: [b.proposal!.id] } }))}>
              Not this
            </button>
          </>
        ) : null}
        {b.committed ? (
          <button className="btn" disabled={busy} onClick={() => act(() => uncommitFn({ data: { blockId: b.id } }), "Unconfirmed.")}>
            Undo confirm
          </button>
        ) : null}
      </div>

      <div className="space-y-2">
        <p className="text-sm font-medium">{b.committed ? "Change it to" : "It was"}</p>
        <IntentPicker
          inputId="relabel-search"
          intents={data.intents}
          recent={data.recent}
          onPick={(i: PickIntent) => act(() => recategorizeFn({ data: { blockId: b.id, intentId: i.id, rememberTitle: calendar && remember, remember: !calendar && remember } })).then(onClose)}
        />
        {calendar && b.title ? (
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
            Label every “{b.title}” this way from now on
          </label>
        ) : null}
        {b.origin === "auto_activity" || b.origin === "place" ? (
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
            {b.origin === "place" ? `Label time ${b.title ? b.title.replace(/^At /, "at ") : "here"} this way from now on` : "Label this app this way from now on"}
          </label>
        ) : null}
      </div>

      {closed ? (
        <div className="flex flex-wrap items-end gap-2 border-t border-black/10 pt-3 dark:border-white/10">
          {!fill ? (
            <>
              <label className="text-sm">
                <span className="muted block">Start</span>
                <input className="input" type="time" value={start} onChange={(e) => setStart(e.target.value)} />
              </label>
              <label className="text-sm">
                <span className="muted block">End</span>
                <input className="input" type="time" value={end} onChange={(e) => setEnd(e.target.value)} />
              </label>
              <button className="btn" disabled={busy} onClick={() => act(() => editBlockFn({ data: { blockId: b.id, date, start, end } }), "Times changed.")}>
                Save times
              </button>
            </>
          ) : null}
          <label className="text-sm">
            <span className="muted block">Split at</span>
            <input className="input" type="time" value={splitAt} onChange={(e) => setSplitAt(e.target.value)} />
          </label>
          <button className="btn" disabled={busy || !splitAt} onClick={() => act(() => splitBlockFn({ data: { blockId: b.id, date, at: splitAt } }), fill ? "Split. Label one half now, or the next sync will join them again." : "Split.")}>
            Split
          </button>
          {!fill ? (
            <button className="btn btn-danger" disabled={busy} onClick={() => act(() => deleteBlockFn({ data: { blockId: b.id } }), "Deleted.").then(onClose)}>
              Delete
            </button>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
