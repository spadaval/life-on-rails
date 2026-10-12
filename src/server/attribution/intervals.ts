// Wall-clock interval math on ms spans. All coverage/scoring counts use
// MERGED seconds — never sums of overlapping row durations.
export interface Span {
  t: number;
  tEnd: number;
}

export function normalize(spans: Span[]): Span[] {
  const valid = spans.filter((s) => Number.isFinite(s.t) && Number.isFinite(s.tEnd) && s.tEnd > s.t);
  valid.sort((a, b) => a.t - b.t);
  const out: Span[] = [];
  for (const s of valid) {
    const last = out[out.length - 1];
    if (last && s.t <= last.tEnd) {
      if (s.tEnd > last.tEnd) last.tEnd = s.tEnd;
    } else out.push({ t: s.t, tEnd: s.tEnd });
  }
  return out;
}

export function totalSeconds(spans: Span[]): number {
  return normalize(spans).reduce((a, s) => a + (s.tEnd - s.t) / 1000, 0);
}

/** base minus cut (both merged internally). Used for calendar remainder fill. */
export function subtractSpans(base: Span[], cut: Span[]): Span[] {
  const b = normalize(base);
  const c = normalize(cut);
  const out: Span[] = [];
  for (const seg of b) {
    let cur = seg.t;
    for (const k of c) {
      if (k.tEnd <= cur) continue;
      if (k.t >= seg.tEnd) break;
      if (k.t > cur) out.push({ t: cur, tEnd: Math.min(k.t, seg.tEnd) });
      cur = Math.max(cur, k.tEnd);
      if (cur >= seg.tEnd) break;
    }
    if (cur < seg.tEnd) out.push({ t: cur, tEnd: seg.tEnd });
  }
  return out;
}

/** |a ∩ b| in seconds. */
export function intersectSeconds(a: Span[], b: Span[]): number {
  const x = normalize(a);
  const y = normalize(b);
  let i = 0;
  let j = 0;
  let total = 0;
  while (i < x.length && j < y.length) {
    const s = Math.max(x[i]!.t, y[j]!.t);
    const e = Math.min(x[i]!.tEnd, y[j]!.tEnd);
    if (e > s) total += (e - s) / 1000;
    if (x[i]!.tEnd < y[j]!.tEnd) i++;
    else j++;
  }
  return total;
}

export interface CalendarConfirmRate {
  /** Undefined on empty days (|E| + |M\E| = 0) — display —. */
  rate: number | undefined;
}

/**
 * no_session calendar-confirm rate:
 * (|C ∩ E| + |M \ E|) / (|E| + |M \ E|), all wall-clock second sets.
 * C = committed seconds, E = non-cancelled timed events, M = committed manual.
 */
export function calendarConfirmRate(args: { committed: Span[]; events: Span[]; manual: Span[] }): CalendarConfirmRate {
  const { committed, events, manual } = args;
  const e = totalSeconds(events);
  const mMinusE = totalSeconds(subtractSpans(manual, events));
  const denom = e + mMinusE;
  if (denom <= 0) return { rate: undefined };
  const cAndE = intersectSeconds(committed, events);
  return { rate: (cAndE + mMinusE) / denom };
}

/** Tracking-mode attribution coverage: unique committed waking seconds / waking seconds. */
export function attributionCoverage(committed: Span[], wakeStartMs: number, wakeEndMs: number): number | undefined {
  if (!(wakeEndMs > wakeStartMs)) return undefined;
  return totalSeconds(committed) / ((wakeEndMs - wakeStartMs) / 1000);
}
