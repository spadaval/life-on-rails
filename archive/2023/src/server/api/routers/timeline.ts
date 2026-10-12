import type {
  Goal,
  Habit,
  HabitMeasuresGoal,
  LinkedMetric,
  Metric,
  MetricMeasuresGoal,
} from "@prisma/client";
import { createTRPCRouter, protectedProcedure } from "../trpc";
import type { TimelineEvent } from "../types";
import { TimelineEventType } from "../types";
import { startOfDay } from "date-fns";

export const timelineRouter = createTRPCRouter({
  getTimeline: protectedProcedure.query(async ({ ctx }) => {
    type QueryType = (Goal & {
      habits: (HabitMeasuresGoal & {
        habit: Habit;
      })[];
      metrics: (MetricMeasuresGoal & {
        metric: Metric;
      })[];
    })[];

    const goals: QueryType = await ctx.prisma.goal.findMany({
      where: {
        ownerId: ctx.session.user.id,
      },
      include: {
        metrics: {
          include: {
            metric: true,
          },
        },
        habits: {
          include: {
            habit: true,
          },
        },
      },
    });

    type QueryType2 = (Habit & {
      goals: (HabitMeasuresGoal & {
        goal: Goal;
      })[];
      metrics: (LinkedMetric & {
        metric: Metric;
      })[];
    })[];
    const habits: QueryType2 = await ctx.prisma.habit.findMany({
      where: {
        ownerId: ctx.session.user.id,
      },
      include: {
        metrics: {
          include: {
            metric: true,
          },
        },
        goals: {
          include: {
            goal: true,
          },
        },
      },
    });

    const events: TimelineEvent[] = [];

    goals.forEach((goal) => {
      events.push({
        eventType: TimelineEventType.GOAL_ADDED,
        goal: goal,
        habits: goal.habits.map((habit) => habit.habit),
        metrics: goal.metrics.map((metric) => metric.metric),
        date: goal.createdAt,
      });
      if (goal.archived) {
        events.push({
          eventType: TimelineEventType.GOAL_ARCHIVED,
          goal: goal,
          created: goal.createdAt,
          habits: goal.habits.map((habit) => habit.habit),
          metrics: goal.metrics.map((metric) => metric.metric),
          date: goal.archivedAt,
        });
      }
    });
    habits.forEach((habit) => {
      events.push({
        eventType: TimelineEventType.HABIT_ADDED,
        goals: habit.goals.map((goal) => goal.goal),
        habit: habit,
        metrics: habit.metrics.map((metric) => metric.metric),
        date: habit.createdAt,
      });
      if (habit.archived) {
        events.push({
          eventType: TimelineEventType.HABIT_ARCHIVED,
          created: habit.createdAt,
          goals: habit.goals.map((goal) => goal.goal),
          habit: habit,
          metrics: habit.metrics.map((metric) => metric.metric),
          date: habit.archivedAt,
        });
      }
    });
    //return events;
    return groupEvents(events);
  }),
});

function groupEvents(events: TimelineEvent[]) {
  const grouped: Map<number, TimelineEvent[]> = new Map();
  events.forEach((event) => {
    const day = startOfDay(event.date).getTime();
    if (!grouped.has(day)) {
      grouped.set(day, []);
    }
    grouped.get(day)!.push(event);
  });
  const groups = Array.from(grouped.entries());
  const e = groups.sort((a, b) => b[0] - a[0]);
  return e;
}
