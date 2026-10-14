// Scores from commitments, and nothing else. One pass loads the graph and the
// commitments, computes every intent's raw score, applies decay once on the
// node being displayed, and stores a snapshot per intent for today's local
// date with the parts that explain the number.
import { sql as defaultSql, type Tx } from "../../db/client.js";
import type { JsonObject } from "../../shared/json.js";
import { addDays, localDayStart, localDaysBetween, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { errorMessage, log } from "../log.js";
import {
  type Confidence,
  type Span,
  confidence,
  decay,
  descendantSet,
  expectedCompletions,
  goalScore,
  habitScore,
  mergedHours,
  metricRaw,
  projectScore,
} from "./formulas.js";

interface IntentRow {
  id: string;
  type: "goal" | "project" | "habit" | "task" | "metric";
  title: string;
  status: string;
  weight: number;
  weekly_hour_target: number | null;
  frequency: number | null;
  frequency_horizon: "day" | "week" | null;
  completion_weight: number | null;
  goal_mix: number | null;
  metric_min: number | null;
  metric_max: number | null;
  estimate_hours: number | null;
  created_at: Date;
}

interface LinkRow {
  from_id: string;
  to_id: string;
  kind: "measures" | "blocks" | "derived_from";
  weight: number;
}

interface CommitmentRow {
  kind: string;
  intent_id: string;
  t: Date;
  t_end: Date | null;
  value: number | null;
}

export interface ScoreResult {
  intentId: string;
  /** Displayed score: raw × decay. Null when there is nothing to score. */
  score: number | null;
  parts: JsonObject;
}

export interface ScoreInputs {
  intents: IntentRow[];
  links: LinkRow[];
  commitments: CommitmentRow[];
  now: Date;
  tz: string;
  weeks: number;
  /** Trailing 7-day calendar-confirm rate, for the confidence chip. */
  trailingConfirmRate: number | null;
}

const round = (x: number | undefined | null, places = 3): number | null =>
  x === undefined || x === null || !Number.isFinite(x) ? null : Number(x.toFixed(places));

/** Pure: everything `recomputeScores` decides, given rows. */
export function computeScores(input: ScoreInputs): ScoreResult[] {
  const { intents, links, commitments, now, tz, weeks } = input;
  const byId = new Map(intents.map((i) => [i.id, i]));
  const today = localYmd(tz, now);
  const windowStart = localDayStart(tz, addDays(today, -(7 * weeks - 1))).getTime();
  const weekStart = localDayStart(tz, addDays(today, -6)).getTime();
  const nowMs = now.getTime();

  const byIntent = new Map<string, CommitmentRow[]>();
  for (const c of commitments) {
    const list = byIntent.get(c.intent_id) ?? [];
    list.push(c);
    byIntent.set(c.intent_id, list);
  }
  const of = (id: string, kind: string) => (byIntent.get(id) ?? []).filter((c) => c.kind === kind);
  const inWindow = (c: CommitmentRow) => c.t.getTime() >= windowStart && c.t.getTime() <= nowMs;
  const spans = (ids: Iterable<string>, from: number): Span[] => {
    const out: Span[] = [];
    for (const id of ids) {
      for (const c of of(id, "time_spent")) {
        if (!c.t_end) continue;
        const t = Math.max(c.t.getTime(), from);
        const tEnd = Math.min(c.t_end.getTime(), nowMs);
        if (tEnd > t) out.push({ t, tEnd });
      }
    }
    return out;
  };

  // measures links: child (from) → parent (to)
  const childrenOf = new Map<string, LinkRow[]>();
  for (const l of links) {
    if (l.kind !== "measures" || !byId.has(l.from_id) || !byId.has(l.to_id)) continue;
    const list = childrenOf.get(l.to_id) ?? [];
    list.push(l);
    childrenOf.set(l.to_id, list);
  }
  const edges = links.map((l) => ({ fromId: l.from_id, toId: l.to_id, kind: l.kind }));

  const rawMemo = new Map<string, number | undefined>();
  const partsMemo = new Map<string, JsonObject>();
  const visiting = new Set<string>();

  function raw(id: string): number | undefined {
    if (rawMemo.has(id)) return rawMemo.get(id);
    if (visiting.has(id)) {
      log("warn", "scoring.cycle", { intent: id });
      return undefined;
    }
    visiting.add(id);
    const intent = byId.get(id)!;
    let value: number | undefined;
    let terms: JsonObject = {};
    const kids = (childrenOf.get(id) ?? []).filter((l) => byId.get(l.from_id)!.status !== "archived");

    switch (intent.type) {
      case "metric": {
        const values = of(id, "metric_value").filter(inWindow).map((c) => c.value).filter((v): v is number => v !== null);
        const min = intent.metric_min ?? 1;
        const max = intent.metric_max ?? 5;
        value = metricRaw(values, min, max);
        terms = { values: values.length, min, max, mean_normalized: round(value) };
        break;
      }
      case "task": {
        const done = of(id, "task_done").length > 0;
        value = done ? 1 : 0;
        terms = { done };
        break;
      }
      case "habit": {
        const completions = of(id, "habit_completion").filter(inWindow).length;
        const metrics = kids.filter((l) => byId.get(l.from_id)!.type === "metric");
        const metricRaws = metrics.map((l) => raw(l.from_id));
        const alpha = intent.completion_weight ?? 0.7;
        const frequency = intent.frequency ?? 0;
        const horizon = intent.frequency_horizon ?? "day";
        value = habitScore({ completions, frequency, frequencyHorizon: horizon, weeks, alpha, linkedMetricRaws: metricRaws });
        const expected = expectedCompletions(frequency, horizon, weeks);
        const defined = metricRaws.filter((m): m is number => m !== undefined);
        terms = {
          completions,
          expected,
          adherence: expected > 0 ? round(Math.min(completions / expected, 1)) : null,
          quality: defined.length ? round(defined.reduce((a, b) => a + b, 0) / defined.length) : null,
          alpha,
          metrics: metrics.map((l, i) => ({ id: l.from_id, title: byId.get(l.from_id)!.title, raw: round(metricRaws[i]) })),
        };
        break;
      }
      case "project": {
        const tasks = kids.filter((l) => byId.get(l.from_id)!.type === "task");
        const tasksDone = tasks.filter((l) => raw(l.from_id) === 1).length;
        // An estimate is a total, so the project's own hours count from the start, not the window.
        const direct = mergedHours(spans([id], 0));
        const descendants = mergedHours(spans(descendantSet(id, edges), 0));
        value = projectScore({ tasksDone, tasksTotal: tasks.length, committedHours: direct, estimateHours: intent.estimate_hours });
        terms = {
          tasks_done: tasksDone,
          tasks_total: tasks.length,
          hours_direct: round(direct, 2),
          hours_including_children: round(descendants, 2),
          estimate_hours: intent.estimate_hours,
          note: "project hours count blocks filed on the project itself; a parent goal also counts its children's",
        };
        break;
      }
      case "goal": {
        const children = kids.map((l) => ({ link: l, score: raw(l.from_id) }));
        const set = descendantSet(id, edges);
        const hours = mergedHours(spans(set, windowStart));
        const beta = intent.goal_mix ?? 0.7;
        value = goalScore({
          children: children.map((c) => ({ score: c.score, weight: c.link.weight })),
          hours,
          weeklyHourTarget: intent.weekly_hour_target,
          weeks,
          beta,
        });
        const weekHours = mergedHours(spans(set, weekStart));
        const target = intent.weekly_hour_target;
        const adequacy = target && target > 0 ? weekHours / target : null;
        terms = {
          children: children.map((c) => ({
            id: c.link.from_id,
            title: byId.get(c.link.from_id)!.title,
            type: byId.get(c.link.from_id)!.type,
            weight: c.link.weight,
            raw: round(c.score),
          })),
          hours_in_window: round(hours, 2),
          target_hours_in_window: target ? target * weeks : null,
          beta,
          last_7_days: {
            hours: round(weekHours, 2),
            target,
            adequacy: round(adequacy),
            starving: adequacy !== null && intent.weight >= 2 && adequacy < 0.5,
          },
        };
        break;
      }
    }
    visiting.delete(id);
    rawMemo.set(id, value);
    partsMemo.set(id, terms);
    return value;
  }

  function lastRelevant(intent: IntentRow): Date | null {
    let ids: string[];
    let kinds: string[];
    switch (intent.type) {
      case "goal":
        ids = [...descendantSet(intent.id, edges)];
        kinds = ["time_spent", "habit_completion", "metric_value", "task_done"];
        break;
      case "habit": {
        const metrics = (childrenOf.get(intent.id) ?? []).filter((l) => byId.get(l.from_id)!.type === "metric").map((l) => l.from_id);
        ids = [intent.id, ...metrics];
        kinds = ["habit_completion", "metric_value"];
        break;
      }
      case "project": {
        const tasks = (childrenOf.get(intent.id) ?? []).filter((l) => byId.get(l.from_id)!.type === "task").map((l) => l.from_id);
        ids = [intent.id, ...tasks];
        kinds = ["time_spent", "task_done"];
        break;
      }
      default:
        ids = [intent.id];
        kinds = ["time_spent", "habit_completion", "metric_value", "task_done"];
    }
    let last: Date | null = null;
    for (const id of ids) {
      for (const c of byIntent.get(id) ?? []) {
        if (!kinds.includes(c.kind) || c.t.getTime() > nowMs) continue;
        if (!last || c.t.getTime() > last.getTime()) last = c.t;
      }
    }
    return last;
  }

  const results: ScoreResult[] = [];
  for (const intent of intents) {
    if (intent.status === "archived") continue;
    const rawScore = raw(intent.id);
    const last = lastRelevant(intent);
    // Silence is counted from the last relevant commitment, or from creation.
    const silenceDays = Math.max(0, localDaysBetween(tz, last ?? intent.created_at, now));
    const d = decay(silenceDays);
    const score = rawScore === undefined ? null : rawScore * d;
    const anyInWindow = (byIntent.get(intent.id) ?? []).some(inWindow) || (last !== null && last.getTime() >= windowStart);
    const chip: Confidence = confidence({
      lastCommitmentDaysAgo: last ? localDaysBetween(tz, last, now) : null,
      anyCommitmentInWindow: anyInWindow,
      trailingCoverage: input.trailingConfirmRate,
      decayDominated: rawScore === undefined || d < 0.5,
    });
    results.push({
      intentId: intent.id,
      score: round(score),
      parts: {
        type: intent.type,
        title: intent.title,
        raw: round(rawScore),
        decay: round(d),
        silence_days: silenceDays,
        score: round(score),
        confidence: chip,
        window_days: 7 * weeks,
        terms: partsMemo.get(intent.id) ?? {},
      },
    });
  }
  return results;
}

/** Other modules provide the trailing confirm rate once Today exists. */
let confirmRateProvider: ((db: Tx, now: Date) => Promise<number | null>) | null = null;
export function setConfirmRateProvider(fn: (db: Tx, now: Date) => Promise<number | null>): void {
  confirmRateProvider = fn;
}

export async function recomputeScores(db: Tx = defaultSql, now = new Date()): Promise<{ intents: number; as_of: string }> {
  const intents = await db<IntentRow[]>`
    SELECT id, type, title, status, weight, weekly_hour_target, frequency, frequency_horizon, completion_weight,
           goal_mix, metric_min, metric_max, estimate_hours, created_at
    FROM intent WHERE NOT system`;
  const links = await db<LinkRow[]>`SELECT from_id, to_id, kind, weight FROM intent_link`;
  const commitments = await db<CommitmentRow[]>`
    SELECT c.kind, c.intent_id, c.t, c.t_end, c.value FROM commitment c JOIN intent i ON i.id = c.intent_id WHERE NOT i.system`;
  const trailingConfirmRate = confirmRateProvider ? await confirmRateProvider(db, now) : null;
  const results = computeScores({ intents, links, commitments, now, tz: env.LOR_TZ, weeks: env.LOR_SCORING_WEEKS, trailingConfirmRate });
  const asOf = localYmd(env.LOR_TZ, now);
  for (const r of results) {
    await db`INSERT INTO score_snapshot (intent_id, as_of, score, parts) VALUES (${r.intentId}, ${asOf}, ${r.score}, ${db.json(r.parts as never)})
      ON CONFLICT (intent_id, as_of) DO UPDATE SET score = EXCLUDED.score, parts = EXCLUDED.parts`;
  }
  return { intents: results.length, as_of: asOf };
}

let pending: NodeJS.Timeout | null = null;

/** Recompute shortly after a commitment or graph write, coalescing a burst into one pass. */
export function scheduleRecompute(): void {
  if (process.env.LOR_SYNC_RECOMPUTE === "off") return;
  if (pending) return;
  pending = setTimeout(() => {
    pending = null;
    recomputeScores().catch((err) => log("error", "score recompute failed", { err: errorMessage(err) }));
  }, 500);
  pending.unref?.();
}

export interface ScoreView {
  intent_id: string;
  as_of: string;
  score: number | null;
  parts: JsonObject;
}

/** The latest snapshot for each intent. */
export async function latestScores(db: Tx): Promise<ScoreView[]> {
  const rows = await db<{ intent_id: string; as_of: string; score: number | null; parts: JsonObject }[]>`
    SELECT DISTINCT ON (intent_id) intent_id, as_of::text AS as_of, score, parts
    FROM score_snapshot ORDER BY intent_id, as_of DESC`;
  return rows.map((r) => ({ intent_id: r.intent_id, as_of: r.as_of, score: r.score, parts: r.parts }));
}

export async function explainScore(db: Tx, intentId: string): Promise<ScoreView | null> {
  const rows = await db<{ intent_id: string; as_of: string; score: number | null; parts: JsonObject }[]>`
    SELECT intent_id, as_of::text AS as_of, score, parts FROM score_snapshot
    WHERE intent_id = ${intentId} ORDER BY as_of DESC LIMIT 1`;
  return rows[0] ? { intent_id: rows[0].intent_id, as_of: rows[0].as_of, score: rows[0].score, parts: rows[0].parts } : null;
}
