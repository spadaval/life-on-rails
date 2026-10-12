/* eslint-disable @typescript-eslint/no-unsafe-assignment */
import { z } from "zod";

import type { Metric } from "@prisma/client";
import { endOfDay, startOfDay } from "date-fns";
import { getMetrics, getPreferences } from "../../queries";
import { createTRPCRouter, protectedProcedure } from "../trpc";

export const metricsRouter = createTRPCRouter({
  getMetrics: protectedProcedure
    .input(z.object({ date: z.date() }))
    .query(async ({ input, ctx }) => {
      const preferences = await getPreferences(
        ctx.db,
        ctx.session.user.id
      );
      const [metrics, _map] = await getMetrics({
        db: ctx.db,
        userId: ctx.session.user.id,
        scoringWeeks: preferences.scoringWeeks,
        date: input.date,
      });
      return metrics;
    }),

  createMetric: protectedProcedure
    .input(
      z.object({
        prompt: z.string(),
        habitId: z.string().optional(),
        goalId: z.string().optional(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      const format = await ctx.prisma.answerFormat.create({
        data: { format: "FIVE_POINT" },
      });

      const metric: Metric = await ctx.prisma.metric.create({
        data: {
          prompt: input.prompt,
          ownerId: ctx.session.user.id,
          formatId: format.id,
        },
      });
      if (input.habitId) {
        await ctx.prisma.linkedMetric.create({
          data: {
            metricId: metric.id,
            habitId: input.habitId,
          },
        });
      }
      if (input.goalId) {
        await ctx.prisma.metricMeasuresGoal.create({
          data: {
            metricId: metric.id,
            goalId: input.goalId,
          },
        });
      }
    }),

  archive: protectedProcedure
    .input(z.object({ metricId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.metric.update({
        data: {
          archived: true,
        },
        where: {
          id: input.metricId,
        },
      });
    }),

  unarchive: protectedProcedure
    .input(z.object({ metricId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.metric.update({
        data: {
          archived: false,
        },
        where: {
          id: input.metricId,
        },
      });
    }),

  setScore: protectedProcedure
    .input(
      z.object({
        metricId: z.string(),
        memo: z.string().optional(),
        value: z.number(),
        score: z.number().refine((val) => val >= 0 && val <= 1),
        date: z.date(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      const existing = await ctx.prisma.metricAnswer.findFirst({
        where: {
          metricId: input.metricId,
          createdAt: onDay(input.date),
        },
      });
      if (existing === null) {
        await ctx.prisma.metricAnswer.create({
          data: {
            metricId: input.metricId,
            value: input.value,
            score: input.score,
            memo: input.memo,
          },
        });
      } else {
        await ctx.prisma.metricAnswer.update({
          where: {
            id: existing.id,
          },
          data: {
            value: input.value,
            score: input.score,
            memo: input.memo,
          },
        });
      }
    }),

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
});

const onDay = (date: Date) => {
  const startDate = startOfDay(date);
  const endDate = endOfDay(startDate);
  return {
    gte: startDate,
    lte: endDate,
  };
};
