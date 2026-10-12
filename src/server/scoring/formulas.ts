// Pure scoring formulas (spec: Scoring). No DB imports — testable in isolation.
// Scoring reads commitments only. Decay applies ONCE, last, on the node
// being displayed. Child scores passed between levels are always RAW.

export interface WeightedChild {
  /** Raw (pre-decay) child score. Undefined = silent, skipped. */
  score: number | undefined;
  weight: number;
}

export function clip01(x: number): number {
  return Math.min(1, Math.max(0, x));
}

// --- Metric ---

export function normalizeMetricValue(v: number, vMin: number, vMax: number): number {
  if (!(vMax > vMin)) return 0;
  return clip01((v - vMin) / (vMax - vMin));
}

/** Mean of normalized committed values. Undefined when silent (never 0). */
export function metricRaw(values: number[], vMin = 1, vMax = 5): number | undefined {
  if (values.length === 0) return undefined;
  const norm = values.map((v) => normalizeMetricValue(v, vMin, vMax));
  return norm.reduce((a, b) => a + b, 0) / norm.length;
}

// --- Habit ---

export function expectedCompletions(frequency: number, horizon: "day" | "week", weeks: number): number {
  const perWeek = horizon === "week" ? frequency : frequency * 7;
  return perWeek * weeks;
}

export function habitScore(args: {
  completions: number;
  frequency: number;
  frequencyHorizon: "day" | "week";
  weeks: number;
  /** Habit mix α. */
  alpha: number;
  /** Raw scores of linked measures-metrics. Empty = no linked metrics. */
  linkedMetricRaws: (number | undefined)[];
}): number | undefined {
  const { completions, frequency, frequencyHorizon, weeks, alpha, linkedMetricRaws } = args;
  const e = expectedCompletions(frequency, frequencyHorizon, weeks);
  if (!(e > 0)) return undefined;
  const adh = Math.min(completions / e, 1);
  const defined = linkedMetricRaws.filter((s): s is number => s !== undefined);
  if (defined.length === 0) return adh; // α treated as 1 when no linked metrics
  const qual = defined.reduce((a, b) => a + b, 0) / defined.length;
  return alpha * adh + (1 - alpha) * qual;
}

// --- Project ---

export function projectScore(args: {
  tasksDone: number;
  tasksTotal: number;
  committedHours: number;
  estimateHours: number | null;
}): number | undefined {
  const { tasksDone, tasksTotal, committedHours, estimateHours } = args;
  const parts: { w: number; v: number }[] = [];
  if (tasksTotal > 0) parts.push({ w: 0.6, v: clip01(tasksDone / tasksTotal) });
  if (estimateHours !== null && estimateHours > 0)
    parts.push({ w: 0.4, v: Math.min(committedHours / estimateHours, 1) });
  if (parts.length === 0) return undefined;
  const wSum = parts.reduce((a, p) => a + p.w, 0);
  return parts.reduce((a, p) => a + (p.w / wSum) * p.v, 0);
}

// --- Intervals (hour union) ---

export interface Span {
  t: number;
  tEnd: number;
}

/** Sweep-line merge of overlapping [t, tEnd] spans. Invalid spans dropped. */
export function mergeIntervals(spans: Span[]): Span[] {
  const valid = spans.filter((s) => Number.isFinite(s.t) && Number.isFinite(s.tEnd) && s.tEnd > s.t);
  valid.sort((a, b) => a.t - b.t);
  const out: Span[] = [];
  for (const s of valid) {
    const last = out[out.length - 1];
    if (last && s.t <= last.tEnd) {
      if (s.tEnd > last.tEnd) last.tEnd = s.tEnd;
    } else {
      out.push({ t: s.t, tEnd: s.tEnd });
    }
  }
  return out;
}

export function mergedHours(spans: Span[]): number {
  return mergeIntervals(spans).reduce((a, s) => a + (s.tEnd - s.t) / 3600_000, 0);
}

// --- Goal ---

export interface GoalEdge {
  fromId: string;
  toId: string;
  kind: "measures" | "blocks" | "derived_from";
}

/**
 * Descendant set: walk `measures` edges upside-down (parent to children),
 * starting at the goal, including the goal itself. Cycle edges dropped.
 */
export function descendantSet(goalId: string, edges: GoalEdge[]): Set<string> {
  const children = new Map<string, string[]>();
  for (const e of edges) {
    if (e.kind !== "measures") continue;
    const list = children.get(e.toId) ?? [];
    list.push(e.fromId);
    children.set(e.toId, list);
  }
  const seen = new Set<string>([goalId]);
  const visiting = new Set<string>([goalId]);
  const stack = [goalId];
  while (stack.length > 0) {
    const cur = stack.pop() as string;
    visiting.delete(cur);
    for (const child of children.get(cur) ?? []) {
      if (visiting.has(child)) continue; // drop cycle edge
      if (!seen.has(child)) {
        seen.add(child);
        visiting.add(child);
        stack.push(child);
      }
    }
  }
  return seen;
}

export function goalScore(args: {
  children: WeightedChild[];
  /** Merged unique hours on the descendant set in the window. */
  hours: number;
  weeklyHourTarget: number | null;
  weeks: number;
  /** Goal mix β. NOT completion_weight. */
  beta: number;
}): number | undefined {
  const { children, hours, weeklyHourTarget, weeks, beta } = args;
  const defined = children.filter((c): c is { score: number; weight: number } => c.score !== undefined && c.weight > 0);
  let childPart: number | undefined;
  if (defined.length > 0) {
    const wSum = defined.reduce((a, c) => a + c.weight, 0);
    childPart = defined.reduce((a, c) => a + (c.score * c.weight) / wSum, 0);
  }
  let hoursPart: number | undefined;
  if (weeklyHourTarget !== null && weeklyHourTarget > 0 && weeks > 0) {
    hoursPart = Math.min(hours / (weeklyHourTarget * weeks), 1);
  }
  if (childPart === undefined && hoursPart === undefined) return undefined;
  if (childPart === undefined) return hoursPart;
  if (hoursPart === undefined) return childPart;
  return beta * childPart + (1 - beta) * hoursPart;
}

// --- Decay ---

export function decay(silenceDays: number): number {
  if (silenceDays <= 2) return 1;
  return Math.exp(-(silenceDays - 2) / 7);
}

/** Displayed score: raw × δ(d), δ applied once here only. */
export function displayed(raw: number | undefined, silenceDays: number): number | undefined {
  if (raw === undefined) return undefined;
  return raw * decay(silenceDays);
}

export type Confidence = "high" | "medium" | "low";

/** Trailing-7d evidence only — never this morning's partial day. */
export function confidence(args: {
  lastCommitmentDaysAgo: number | null;
  anyCommitmentInWindow: boolean;
  trailingCoverage: number | null;
  decayDominated: boolean;
}): Confidence {
  const { lastCommitmentDaysAgo, anyCommitmentInWindow, trailingCoverage, decayDominated } = args;
  if (
    lastCommitmentDaysAgo !== null &&
    lastCommitmentDaysAgo < 2 &&
    trailingCoverage !== null &&
    trailingCoverage >= 0.5 &&
    !decayDominated
  )
    return "high";
  if (anyCommitmentInWindow && !decayDominated) return "medium";
  return "low";
}
