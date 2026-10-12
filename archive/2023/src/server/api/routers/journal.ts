import type {
  HabitCompletion,
  HabitTag,
  LinkedMetric,
  Metric,
  MetricAnswer,
  MetricTag,
  Tag,
} from "@prisma/client";
import { endOfDay, startOfDay } from "date-fns";
import { z } from "zod";
import { createTRPCRouter, protectedProcedure } from "../trpc";

const onDay = (date: Date) => {
  const startDate = startOfDay(date);
  const endDate = endOfDay(startDate);
  return {
    gte: startDate,
    lte: endDate,
  };
};

export const journalRouter = createTRPCRouter({
  addHabit: protectedProcedure
    .input(z.object({ description: z.string(), goal: z.string().optional() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.habit.create({
        data: {
          description: input.description,
          ownerId: ctx.session.user.id,
        },
      });
    }),

  complete: protectedProcedure
    .input(
      z.object({
        date: z.date(),
        habitId: z.string(),
        memo: z.string().optional(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.habitCompletion.create({
        data: {
          date: input.date,
          habitId: input.habitId,
          memo: input.memo,
        },
      });
    }),

  deleteCompletion: protectedProcedure
    .input(z.object({ habitCompletionId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.habitCompletion.delete({
        where: {
          id: input.habitCompletionId,
        },
      });
    }),

  deleteHabit: protectedProcedure
    .input(z.object({ habitId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.$transaction([
        ctx.prisma.habitMeasuresGoal.deleteMany({
          where: {
            habitId: input.habitId,
          },
        }),
        ctx.prisma.habitCompletion.deleteMany({
          where: {
            habitId: input.habitId,
          },
        }),
        ctx.prisma.habit.delete({
          where: {
            id: input.habitId,
          },
        }),
      ]);
    }),
  editHabit: protectedProcedure
    .input(z.object({ habitId: z.string(), description: z.string() }))
    .mutation(
      async ({ input, ctx }) =>
        await ctx.prisma.habit.update({
          where: {
            id: input.habitId,
          },
          data: {
            description: input.description,
          },
        })
    ),

  editMetric: protectedProcedure
    .input(z.object({ metricId: z.string(), prompt: z.string() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.metric.update({
        where: { id: input.metricId },
        data: { prompt: input.prompt },
      });
    }),

  deleteMetric: protectedProcedure
    .input(z.object({ metricId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.metric.delete({ where: { id: input.metricId } });
    }),

  getHabits: protectedProcedure
    .input(
      z.object({
        date: z.date(),
      })
    )
    .query(async ({ input, ctx }) => {
      type QueryType = {
        id: string;
        metrics: {
          metric: Metric;
        }[];
        completions: HabitCompletion[];
        description: string;
        tags: (HabitTag & { tag: Tag })[];
      }[];

      const data: QueryType = await ctx.prisma.habit.findMany({
        where: {
          ownerId: ctx.session.user.id,
        },
        select: {
          id: true,
          description: true,
          tags: {
            include: {
              tag: true,
            },
          },
          metrics: {
            select: {
              metric: {
                include: {
                  metricAnswers: {
                    where: {
                      createdAt: onDay(input.date),
                    },
                  },
                },
              },
            },
          },
          completions: {
            where: {
              date: onDay(input.date),
            },
          },
        },
      });
      const habitsData = data.map(
        ({ id, description, metrics, completions, tags }) => ({
          id,
          description,
          metrics: metrics.map((m) => m.metric),
          completions,
          tags: tags.map((it) => it.tag),
        })
      );

      return {
        habits: habitsData,
        date: input.date,
      };
    }),

  getMetrics: protectedProcedure
    .input(
      z.object({
        date: z.date(),
      })
    )
    .query(async ({ input, ctx }) => {
      type QueryType = {
        id: string;
        prompt: string;
        metricAnswers: MetricAnswer[];
        completionMetric: LinkedMetric[];
        tags: (MetricTag & { tag: Tag })[];
      };

      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      const data: QueryType[] = await ctx.prisma.metric.findMany({
        where: {
          ownerId: ctx.session.user.id,
        },
        select: {
          id: true,
          prompt: true,
          completionMetric: true,
          tags: { include: { tag: true } },
          metricAnswers: {
            where: {
              createdAt: onDay(input.date),
            },
            orderBy: {
              createdAt: "desc",
            },
          },
        },
      });

      const metrics = data.map(
        ({ id, prompt, metricAnswers, completionMetric, tags }) => ({
          id,
          prompt,
          tags: tags.map((it) => it.tag),
          score: metricAnswers[0]?.value,
          habits: completionMetric.map((m) => m.habitId),
        })
      );

      return {
        metrics: metrics,
        date: input.date,
      };
    }),
});
