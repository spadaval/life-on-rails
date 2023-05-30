import type { Metric } from "@prisma/client";
import { z } from "zod";
import type { prisma as prismaClient } from "../../db";
import { createTRPCRouter, protectedProcedure } from "../trpc";

export const profileRouter = createTRPCRouter({
  getProfile: protectedProcedure.query(async ({ ctx }) => {
    const data = await ctx.prisma.user.findFirstOrThrow({
      where: {
        id: ctx.session.user.id,
      },
      select: {
        name: true,
        email: true,
        image: true,
        scoringWeeks: true,
        scoringUnit: true,
        accounts: {
          select: {
            provider: true,
          },
        },
        createdAt: true,
      },
    });

    return {
      name: data.name,
      email: data.email,
      image: data.image,
      scoringWeeks: data.scoringWeeks,
      scoringUnit: data.scoringUnit,
      providers: data.accounts.map((it) => it.provider),
      createdAt: data.createdAt,
    };
  }),

  updateScoringWeeks: protectedProcedure
    .input(z.object({ scoringWeeks: z.number() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.user.update({
        where: {
          id: ctx.session.user.id,
        },
        data: { scoringWeeks: input.scoringWeeks },
      });
    }),

  updateScoringUnit: protectedProcedure
    .input(z.object({ scoringUnit: z.enum(["Percentage", "Normalized"]) }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.user.update({
        where: {
          id: ctx.session.user.id,
        },
        data: { scoringUnit: input.scoringUnit },
      });
    }),

  resetAccount: protectedProcedure.mutation(async ({ ctx }) => {
    await Promise.all([
      deleteGoals(ctx.prisma, ctx.session.user.id),
      deleteHabits(ctx.prisma, ctx.session.user.id),
      deleteMetrics(ctx.prisma, ctx.session.user.id),
    ]);
  }),
});

async function deleteGoals(prisma: typeof prismaClient, userId: string) {
  const goals = await prisma.goal.findMany({
    where: {
      ownerId: userId,
    },
  });

  return Promise.all(
    goals.map((it) => prisma.goal.delete({ where: { id: it.id } }))
  );
}
async function deleteHabits(prisma: typeof prismaClient, userId: string) {
  const habits = await prisma.habit.findMany({
    where: {
      ownerId: userId,
    },
  });

  return Promise.all(
    habits.map((it) => prisma.habit.delete({ where: { id: it.id } }))
  );
}
async function deleteMetrics(prisma: typeof prismaClient, userId: string) {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const metrics: Metric[] = await prisma.metric.findMany({
    where: {
      ownerId: userId,
    },
  });

  return Promise.all(
    metrics.map((it) => prisma.metric.delete({ where: { id: it.id } }))
  );
}
