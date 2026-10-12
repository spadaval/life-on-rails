import type { Goal, Habit, Metric, Tag } from "@prisma/client";
import { FrequencyHorizon } from "@prisma/client";
import { endOfDay, isSameDay, startOfDay, subDays } from "date-fns";

import type { SQL } from "drizzle-orm";
import { and, eq, gt, inArray, sql } from "drizzle-orm";
import {
  goal,
  habit,
  habitCompletion,
  habitMeasuresGoal,
  linkedMetric,
  metric,
  metricAnswer,
  metricMeasuresGoal,
  preferences,
} from "../schema";
import { getOrCompute } from "./api/cache";
import { type DB, type prisma as prismaClient } from "./db";

function avg(arr: number[]) {
  return arr.length == 0 ? 0 : arr.reduce((a, b) => a + b) / arr.length;
}

function sum(arr: number[]) {
  return arr.reduce((a, b) => a + b, 0);
}

export interface ExpandedHabit extends Habit {
  metrics: ExpandedMetric[];
  tags: string[];
  goals: string[];
  completions: number;
  score: number;
}

// get habit completions from a user for x amount of days ago
export async function getHabitCompletionSubDays({
  prisma,
  userId,
  days,
}: {
  prisma: typeof prismaClient;
  userId: string;
  days: number;
}): Promise<number[]> {
  const currentDate = new Date();
  const completionCounts: number[] = [];

  for (let i = days; i >= 0; i--) {
    const targetDate = subDays(currentDate, i);

    const completions = await prisma.habitCompletion.count({
      where: {
        Habit: { ownerId: userId },
        date: { gt: startOfDay(targetDate), lt: endOfDay(targetDate) },
      },
    });

    completionCounts.push(completions);
  }

  return completionCounts;
}

export async function getHabits({
  db,
  metricsMap,
  userId,
  scoringWeeks,
  goalIds,
  date = new Date(),
}: {
  prisma: typeof prismaClient;
  db: DB;
  metricsMap: Map<string, ExpandedMetric>;
  userId: string;
  scoringWeeks: number;
  goalIds?: string[];
  date?: Date;
}): Promise<[ExpandedHabit[], Map<string, ExpandedHabit>]> {
  const conditions = [eq(habit.ownerId, userId), eq(habit.archived, 0)];
  if (goalIds) {
    const goalSubquery = db
      .select({ data: habit.id })
      .from(habit)
      .innerJoin(habitMeasuresGoal, eq(habit.id, habitMeasuresGoal.habitId))
      .where(inArray(habitMeasuresGoal.goalId, goalIds));
    conditions.push(inArray(habit.id, goalSubquery));
  }

  const habits = await db.query.habit.findMany({
    where: and(...conditions),
    with: {
      metrics: true,
      tags: { with: { tag: true } },
      goals: { columns: { goalId: true } },
      completions: {
        where: gt(habitCompletion.date, subDays(date, 7).toISOString()),
      },
    },
  });

  const habitsMap = new Map<string, (typeof habits)[0]>(
    habits.map((h) => [h.id, h])
  );

  const habitCompletions = await db
    .select({ habitId: habitCompletion.habitId, count: sql<number>`count(*)` })
    .from(habitCompletion)
    .leftJoin(habit, eq(habitCompletion.habitId, habit.id))
    .where(
      and(
        eq(habit.ownerId, userId),
        gt(
          habitCompletion.date,
          subDays(new Date(), scoringWeeks * 7).toISOString()
        )
      )
    )
    .groupBy(habit.id);

  const habitScores = new Map<string, number>(
    habitCompletions
      .filter((it) => habitsMap.has(it.habitId))
      .map((it) => {
        const habit = habitsMap.get(it.habitId)!;

        const normalizedFrequency =
          habit.frequencyHorizon == FrequencyHorizon.WEEK
            ? habit.frequency
            : habit.frequency * 7;
        const maxCompletionCount = normalizedFrequency * scoringWeeks;
        const completionScore = it.count / maxCompletionCount;

        const metricsScore = avg(
          habit.metrics.map((m) => metricsMap.get(m.metricId)?.score ?? 0)
        );

        const score =
          habit.completionWeight * completionScore +
          (1 - habit.completionWeight) * metricsScore;

        return [it.habitId, score];
      })
  );

  const expandedHabits: ExpandedHabit[] = habits.map(remapTypes).map((h) => ({
    ...h,
    score: habitScores.get(h.id) ?? 0,
    goals: h.goals.map((it) => it.goalId),
    tags: h.tags.map((it) => it.tag.name),
    metrics: h.metrics.map((it) => metricsMap.get(it.metricId)!),
    completions:
      h.frequencyHorizon == FrequencyHorizon.WEEK
        ? h.completions.length
        : h.completions.filter((it) => isSameDay(new Date(it.date), date))
            .length,
  }));
  const expandedHabitsMap = new Map<string, ExpandedHabit>(
    expandedHabits.map((h) => [h.id, h])
  );

  return [expandedHabits, expandedHabitsMap];
}

export interface ExpandedMetric extends Metric {
  linkedHabits: string[];
  tags: Tag[];
  score: number;
  goals: Goal[];
  value: number;
}

export async function getMetrics({
  db,
  userId,
  scoringWeeks,
  goalIds,
  habitIds,
  date = new Date(),
}: {
  db: DB;
  userId: string;
  scoringWeeks: number;
  goalIds?: string[];
  habitIds?: string[];
  date?: Date;
}): Promise<[ExpandedMetric[], Map<string, ExpandedMetric>]> {
  const conditions: SQL[] = [eq(metric.ownerId, userId)];

  if (goalIds) {
    const metricsLinkedToGivenGoals = db
      .select({ data: metricMeasuresGoal.metricId })
      .from(metricMeasuresGoal)
      .innerJoin(goal, eq(goal.id, metricMeasuresGoal.goalId))
      .where(inArray(goal.id, goalIds));
    conditions.push(inArray(metric.id, metricsLinkedToGivenGoals));
  }
  if (habitIds) {
    const metricsLinkedToGivenHabits = db
      .select({ data: linkedMetric.metricId })
      .from(linkedMetric)
      .innerJoin(habit, eq(habit.id, linkedMetric.habitId))
      .where(inArray(habit.id, habitIds));
    conditions.push(inArray(metric.id, metricsLinkedToGivenHabits));
  }

  const metrics = await db.query.metric.findMany({
    where: and(...conditions),
    with: {
      habits: { columns: { habitId: true } },
      goals: { with: { goal: true } },
      tags: {
        with: { tag: true },
      },
      answers: {
        where: gt(metricAnswer.createdAt, startOfDay(date).toISOString()),
      },
    },
  });

  const metricIds = metrics.map((m) => m.id);

  const startDate = subDays(date, 7 * scoringWeeks).toISOString();
  const metricAnswers = await db
    .select({ metricId: metricAnswer.metricId, count: sql<number>`count(*)` })
    .from(metricAnswer)
    .where(
      //and(
      gt(metricAnswer.createdAt, startDate)
      //inArray(metricAnswer.metricId, metricIds)
      //)
    )
    .groupBy(metricAnswer.metricId);

  const metricScores = new Map<string, number>(
    metricAnswers.map((a) => [
      a.metricId,
      (a.count ?? 0) / 5 / (7 * scoringWeeks),
    ])
  );

  const expandedMetrics: ExpandedMetric[] = metrics
    .map(remapTypes)
    .map((m) => ({
      ...m,
      linkedHabits: m.habits.map((it) => it.habitId),
      tags: m.tags.map((mt) => mt.tag),
      goals: m.goals.map((g) => ({ ...g.goal })).map(remapTypes),
      score: metricScores.get(m.id) ?? 0,
      value: m.answers[0]?.value ?? 0,
    }));

  const metricsMap = new Map<string, ExpandedMetric>();
  expandedMetrics.forEach((m) => {
    metricsMap.set(m.id, m);
  });

  return [expandedMetrics, metricsMap];
}

export interface ExpandedGoal extends Goal {
  tags: Tag[];
  score: number;
}

export interface GoalsReturnType {
  goal: ExpandedGoal;
  metrics: ExpandedMetric[];
  habits: ExpandedHabit[];
}

interface RemappableTypes {
  createdAt: string;
  updatedAt: string;
  archivedAt: string;
  archived: number;
}

interface MappedTypes {
  createdAt: Date;
  updatedAt: Date;
  archivedAt: Date;
  archived: boolean;
}

export function remapTypes<T extends RemappableTypes>(
  it: T
): Omit<T, keyof RemappableTypes> & MappedTypes {
  return {
    ...it,
    createdAt: new Date(it.createdAt),
    updatedAt: new Date(it.updatedAt),
    archivedAt: new Date(it.archivedAt),
    archived: it.archived == 1,
  };
}

export async function getGoals(
  prisma: typeof prismaClient,
  db: DB,
  userId: string,
  metricsMap: Map<string, ExpandedMetric>,
  habitsMap: Map<string, ExpandedHabit>
): Promise<[GoalsReturnType[], Map<string, GoalsReturnType>]> {
  const g = await db.query.goal.findMany({
    where: and(eq(goal.ownerId, userId), eq(goal.archived, 0)),
    with: {
      habits: true,
      metrics: true,
      tags: { with: { tag: true } },
    },
  });

  const goals = g.map(remapTypes);

  const goalsData = goals.map((g) => {
    const m: number[] = g.metrics.map(
      (it) => metricsMap.get(it.metricId)?.score ?? 0
    );
    const mWeight: number[] = g.metrics.map((it) => it.weight);
    const h: number[] = g.habits.map(
      (it) => habitsMap.get(it.habitId)?.score ?? 0
    );
    const hWeight: number[] = g.habits.map((it) => it.weight);
    const weightedMetricScores = m.map((score, i) => score * (mWeight[i] ?? 1));
    const weightedHabitScores = h.map((score, i) => score * (hWeight[i] ?? 1));

    const score =
      (sum(weightedMetricScores) + sum(weightedHabitScores)) /
      (sum(mWeight) + sum(hWeight));

    const linkedHabits = g.habits.map((h) => habitsMap.get(h.habitId)!);

    return {
      goal: { ...g, score, tags: g.tags.map((it) => it.tag) },
      habits: linkedHabits.filter((it) => it && it.archived === false),
      metrics: g.metrics.map((m) => ({
        ...metricsMap.get(m.metricId)!,
      })),
    };
  });
  const goalsMap = new Map<string, GoalsReturnType>();
  goalsData.forEach((g) => {
    goalsMap.set(g.goal.id, g);
  });
  return [goalsData, goalsMap];
}

export async function getPreferences(
  db: DB,
  userId: string
): Promise<typeof preferences._.model.select> {
  return getOrCompute(userId, async () => {
    await db.insert(preferences).ignore().values({ userId });

    const p = await db
      .select()
      .from(preferences)
      .where(eq(preferences.userId, userId));
    return p[0]!;
  });
}
