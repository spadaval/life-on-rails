// Today's signature: the waking day as an arc from rising to setting, drawn
// like a plotted route. Tracked time is solid orange, the rest is dashed,
// calendar events are cyan diamonds, and now sits in a target reticle.
import type { ReactNode } from "react";

export interface ArcBlock {
  t_start: string;
  t_end: string | null;
  committed: boolean;
  /** Phone or place time nobody has labelled. */
  bare: boolean;
}

export interface ArcEvent {
  key: string;
  title: string;
  t_start: string;
}

const W = 1200;
const H = 300;
const CX = W / 2;
const BASE = 268;
const RX = 520;
const RY = 214;

// Rounded so the server's and the browser's trigonometry draw the same numbers.
const r2 = (v: number) => Math.round(v * 100) / 100;
const point = (f: number) => {
  const th = Math.PI * (1 - f);
  return { x: r2(CX + RX * Math.cos(th)), y: r2(BASE - RY * Math.sin(th)) };
};
/** Outward unit normal at fraction f, in screen coordinates. */
const normal = (f: number) => {
  const th = Math.PI * (1 - f);
  const nx = Math.cos(th) / RX;
  const ny = -Math.sin(th) / RY;
  const len = Math.hypot(nx, ny);
  return { x: r2(nx / len), y: r2(ny / len) };
};
const arcPath = (f0: number, f1: number) => {
  const steps = Math.max(2, Math.ceil((f1 - f0) * 80));
  let d = "";
  for (let i = 0; i <= steps; i++) {
    const p = point(f0 + ((f1 - f0) * i) / steps);
    d += `${i === 0 ? "M" : "L"}${p.x.toFixed(1)},${p.y.toFixed(1)}`;
  }
  return d;
};

export function DayArc({ wakeStart, wakeEnd, now, isToday, blocks, events, hm, children }: {
  wakeStart: string;
  wakeEnd: string;
  now: number;
  isToday: boolean;
  blocks: ArcBlock[];
  events: ArcEvent[];
  hm: (ms: number) => string;
  children: ReactNode;
}) {
  const t0 = new Date(wakeStart).getTime();
  const t1 = new Date(wakeEnd).getTime();
  const frac = (ms: number) => Math.min(1, Math.max(0, (ms - t0) / (t1 - t0)));

  const hours: number[] = [];
  for (let h = Math.ceil(t0 / 3_600_000) * 3_600_000; h <= t1; h += 3_600_000) hours.push(h);

  const nowF = frac(now);
  const showNow = isToday && now > t0 && now < t1;

  // Event labels skip any that would crowd the one before.
  let lastLabelX = -Infinity;
  const labelled = events
    .map((e) => ({ ...e, f: frac(new Date(e.t_start).getTime()) }))
    .filter((e) => new Date(e.t_start).getTime() >= t0 && new Date(e.t_start).getTime() <= t1)
    .map((e) => {
      const p = point(e.f);
      const show = Math.abs(p.x - lastLabelX) > 130;
      if (show) lastLabelX = p.x;
      return { ...e, show };
    });

  return (
    <section className="relative mx-auto max-w-[1240px]" aria-label="The waking day">
      <PhoneBar t0={t0} t1={t1} now={now} isToday={isToday} blocks={blocks} hm={hm} />
      <svg viewBox={`0 0 ${W} ${H}`} className="hidden h-auto w-full sm:block" role="img" aria-label={`Waking day from ${hm(t0)} to ${hm(t1)}`}>
        <defs>
          <pattern id="arc-grid" width="60" height="60" patternUnits="userSpaceOnUse">
            <path d="M60 0H0V60" fill="none" stroke="rgb(255 255 255 / 0.03)" />
          </pattern>
        </defs>
        <rect width={W} height={H} fill="url(#arc-grid)" />
        <line x1={0} x2={W} y1={BASE} y2={BASE} stroke="#1e2029" />

        <path d={arcPath(0, 1)} fill="none" stroke="#4a2e14" strokeWidth={1.5} strokeDasharray="5 7" />

        {blocks.map((b, i) => {
          const s = frac(new Date(b.t_start).getTime());
          const e = frac(b.t_end ? new Date(b.t_end).getTime() : now);
          if (e - s < 0.001) return null;
          return (
            <path
              key={i}
              d={arcPath(s, e)}
              fill="none"
              stroke="#f08a2c"
              strokeOpacity={b.committed || !b.t_end ? 1 : b.bare ? 0.7 : 0.85}
              strokeWidth={!b.t_end ? 4.5 : b.committed ? 3.5 : 2.5}
            />
          );
        })}

        {hours.map((h) => {
          const f = frac(h);
          const p = point(f);
          const n = normal(f);
          const hour = Number(hm(h).slice(0, 2));
          return (
            <g key={h}>
              <line x1={p.x - n.x * 6} y1={p.y - n.y * 6} x2={p.x + n.x * 6} y2={p.y + n.y * 6} stroke="#6b4420" />
              {hour % 2 === 0 && f > 0.03 && f < 0.97 ? (
                <text x={p.x - n.x * 24} y={p.y - n.y * 24 + 4} textAnchor="middle" fill="#928777" fontSize={13} className="font-cond">
                  {hm(h).slice(0, 2)}
                </text>
              ) : null}
            </g>
          );
        })}

        {labelled.map((e) => {
          const p = point(e.f);
          const n = normal(e.f);
          const dx = p.x + n.x * 18;
          const dy = p.y + n.y * 18;
          const anchor = p.x < CX - 80 ? "end" : p.x > CX + 80 ? "start" : "middle";
          const lx = p.x + n.x * 34 + (anchor === "end" ? -4 : anchor === "start" ? 4 : 0);
          const ly = p.y + n.y * 34 + 4;
          return (
            <g key={e.key}>
              <rect x={dx - 5} y={dy - 5} width={10} height={10} transform={`rotate(45 ${dx} ${dy})`} fill="#0c0d12" stroke="#6cc6ec" strokeWidth={1.5} />
              {e.show ? (
                <text x={lx} y={ly} textAnchor={anchor} fill="#6cc6ec" fontSize={14}>
                  {hm(new Date(e.t_start).getTime())} {e.title.length > 34 ? `${e.title.slice(0, 33)}…` : e.title}
                </text>
              ) : null}
            </g>
          );
        })}

        {showNow ? <Reticle f={nowF} label={`${hm(now)} now`} /> : null}

        <circle cx={point(0).x} cy={point(0).y} r={3} fill="#928777" />
        <circle cx={point(1).x} cy={point(1).y} r={3} fill="#928777" />
        <text x={point(0).x} y={BASE + 22} fill="#b1a596" fontSize={13.5}>
          Rising {hm(t0)}
        </text>
        <text x={point(1).x} y={BASE + 22} fill="#b1a596" fontSize={13.5} textAnchor="end">
          Setting {hm(t1)}
        </text>
      </svg>
      <div className="mx-auto mt-4 flex max-w-[640px] flex-col items-center text-center sm:absolute sm:inset-x-0 sm:top-[34%] sm:mt-0">{children}</div>
    </section>
  );
}

/** On a phone the arc would be too small to read: the same day as a straight bar. */
function PhoneBar({ t0, t1, now, isToday, blocks, hm }: { t0: number; t1: number; now: number; isToday: boolean; blocks: ArcBlock[]; hm: (ms: number) => string }) {
  const pct = (ms: number) => `${(Math.min(1, Math.max(0, (ms - t0) / (t1 - t0))) * 100).toFixed(2)}%`;
  return (
    <div className="pt-3 sm:hidden">
      <div className="relative h-3 border border-frame bg-well">
        {blocks.map((b, i) => {
          const a = new Date(b.t_start).getTime();
          const z = b.t_end ? new Date(b.t_end).getTime() : now;
          if (z <= t0 || a >= t1) return null;
          return <span key={i} className="absolute inset-y-0 bg-or" style={{ left: pct(a), right: `calc(100% - ${pct(z)})`, opacity: b.committed || !b.t_end ? 1 : b.bare ? 0.7 : 0.85 }} />;
        })}
        {isToday && now > t0 && now < t1 ? <span className="absolute -inset-y-1 w-0.5 bg-txt" style={{ left: pct(now) }} /> : null}
      </div>
      <div className="mt-1 flex justify-between text-[12.5px] text-mute">
        <span>Rising {hm(t0)}</span>
        <span>Setting {hm(t1)}</span>
      </div>
    </div>
  );
}

function Reticle({ f, label }: { f: number; label: string }) {
  const p = point(f);
  const n = normal(f);
  const s = 9;
  const c = 4;
  const corners = [
    [-1, -1],
    [1, -1],
    [1, 1],
    [-1, 1],
  ] as const;
  const anchor = p.x > CX + 80 ? "start" : p.x < CX - 80 ? "end" : "start";
  return (
    <g>
      {corners.map(([sx, sy]) => (
        <path
          key={`${sx}${sy}`}
          d={`M${p.x + sx * s},${p.y + sy * (s - c)} L${p.x + sx * s},${p.y + sy * s} L${p.x + sx * (s - c)},${p.y + sy * s}`}
          fill="none"
          stroke="#f08a2c"
          strokeWidth={1.5}
        />
      ))}
      <path d="M0,-6 L1.3,-1.3 L6,0 L1.3,1.3 L0,6 L-1.3,1.3 L-6,0 L-1.3,-1.3 Z" transform={`translate(${p.x} ${p.y})`} fill="#f08a2c" />
      <text x={p.x + n.x * 30 + 6} y={p.y + n.y * 30} textAnchor={anchor} fill="#f08a2c" fontSize={14} fontWeight={500}>
        {label}
      </text>
    </g>
  );
}
