import { clerkClient } from "@clerk/nextjs";
import { type Metric } from "@prisma/client";
import { subDays } from "date-fns";
import type { InferModel } from "drizzle-orm";
import { eq } from "drizzle-orm";
import { z } from "zod";
import { habitCompletion, metric, metricAnswer } from "../../../schema";
import { habit, preferences } from "../../../schema";
import type { prisma as prismaClient } from "../../db";
import { createTRPCRouter, protectedProcedure } from "../trpc";
import { toMysqlDate } from "../../../utils/dates";

export const profileRouter = createTRPCRouter({
  getProfile: protectedProcedure.query(async ({ ctx }) => {
    const userId = ctx.session.user.id;

    const user = await clerkClient.users.getUser(userId);

    // const d = await ctx.db.query.user.findFirst({
    //   where: eq(user.id, ctx.session.user.id),
    //   with: {
    //     accounts: true,
    //   },
    // });
    const data = user;

    const p = (
      await ctx.db
        .select()
        .from(preferences)
        .where(eq(preferences.userId, ctx.session.user.id))
    )[0]!;

    return {
      name: `${data.firstName || ""} ${data.lastName || ""}`,
      email: data.emailAddresses[0],
      image: data.imageUrl,
      scoringWeeks: p.scoringWeeks,
      scoringUnit: p.scoringUnit,
      providers: [],
      createdAt: new Date(data.createdAt),
    };
  }),

  updateScoringWeeks: protectedProcedure
    .input(z.object({ scoringWeeks: z.number() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.preferences.update({
        where: {
          userId: ctx.session.user.id,
        },
        data: { scoringWeeks: input.scoringWeeks },
      });
    }),

  updateScoringUnit: protectedProcedure
    .input(z.object({ scoringUnit: z.enum(["Percentage", "Normalized"]) }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.preferences.update({
        where: {
          userId: ctx.session.user.id,
        },
        data: { scoringUnit: input.scoringUnit },
      });
    }),

  resetAccount: protectedProcedure.mutation(async ({ ctx }) => {
    await Promise.all([
      deleteGoals(ctx.prisma, ctx.session.user.id),
      deleteHabits(ctx.prisma, ctx.session.user.id),
      deleteMetrics(ctx.prisma, ctx.session.user.id),
      deleteTags(ctx.prisma, ctx.session.user.id),
    ]);
  }),

  generateTestData: protectedProcedure.mutation(async ({ ctx }) => {
    const habits = await ctx.db.select().from(habit);
    type HabitCompletionInsert = InferModel<typeof habitCompletion, "insert">;

    const habitCompletions: HabitCompletionInsert[] = [];

    habits.forEach((it) => {
      let daysBack = 100;
      while (daysBack > 0) {
        habitCompletions.push({
          habitId: it.id,
          date: toMysqlDate(subDays(new Date(), daysBack)),
        });

        daysBack -= Math.floor(1 + Math.random() * 3);
      }
    });

    const metrics = await ctx.db.select().from(metric);

    type MetricAnswer = InferModel<typeof metricAnswer, "insert">;
    const metricAnswers: MetricAnswer[] = [];

    metrics.forEach((it) => {
      let daysBack = 100;
      while (daysBack > 0) {
        const date = toMysqlDate(subDays(new Date(), daysBack));
        const score = Math.floor(1 + Math.random() * 3);
        metricAnswers.push({
          metricId: it.id,
          createdAt: date,
          updatedAt: date,
          value: score,
          score,
        });

        daysBack -= 1;
      }
    });

    await Promise.all([
      ctx.db.insert(habitCompletion).values(habitCompletions),
      ctx.db.insert(metricAnswer).values(metricAnswers),
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
async function deleteTags(prisma: typeof prismaClient, userId: string) {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const tag = await prisma.tag.findMany({
    where: {
      ownerId: userId,
    },
  });

  return Promise.all(
    tag.map((it) => prisma.tag.delete({ where: { id: it.id } }))
  );
}
