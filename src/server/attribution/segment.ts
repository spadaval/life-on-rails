// Observation → focus session → time block rollup (v1: identical app+title only).
// Embeddings are NOT used for merge in v1 (PR 8 adds them to title_embedding only).
export interface Sample {
  app: string | null;
  title: string | null;
  tStart: number;
  tEnd: number | null;
  intentId: string | null;
}

export interface Session {
  app: string | null;
  title: string | null;
  tStart: number;
  tEnd: number;
  intentId: string | null;
}

function sameLabel(a: Sample | Session, b: Sample | Session): boolean {
  return (a.app ?? null) === (b.app ?? null) && (a.title ?? null) === (b.title ?? null);
}

/** Consecutive same-label samples with gaps ≤ idleThresholdS merge into sessions. */
export function coalesceSamples(samples: Sample[], idleThresholdS: number): Session[] {
  const sorted = [...samples].sort((a, b) => a.tStart - b.tStart);
  const out: Session[] = [];
  for (const s of sorted) {
    const end = s.tEnd ?? s.tStart;
    const last = out[out.length - 1];
    if (last && sameLabel(last, s) && s.tStart - last.tEnd <= idleThresholdS * 1000) {
      if (end > last.tEnd) last.tEnd = end;
    } else {
      out.push({ app: s.app, title: s.title, tStart: s.tStart, tEnd: end, intentId: s.intentId });
    }
  }
  return out;
}

/** Sessions mapping to the same intent separated by ≤120s merge into one block. */
export function mergeSessions(sessions: Session[], gapS = 120): Session[] {
  const sorted = [...sessions].sort((a, b) => a.tStart - b.tStart);
  const out: Session[] = [];
  for (const s of sorted) {
    const last = out[out.length - 1];
    if (
      last &&
      (last.intentId ?? null) === (s.intentId ?? null) &&
      s.tStart - last.tEnd <= gapS * 1000
    ) {
      if (s.tEnd > last.tEnd) last.tEnd = s.tEnd;
    } else {
      out.push({ ...s });
    }
  }
  return out;
}
