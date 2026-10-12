import { and, eq } from "drizzle-orm";
import { z } from "zod";
import {
  goal,
  goalTag,
  metric,
  metricMeasuresGoal,
  tag,
} from "../../../schema";
import {
  getGoals,
  getHabits,
  getMetrics,
  getPreferences,
  remapTypes,
} from "../../queries";
import { createTRPCRouter, protectedProcedure } from "../trpc";

export const goalsRouter = createTRPCRouter({
  deleteGoal: protectedProcedure
    .input(z.object({ id: z.string() }))
    .mutation(async ({ input, ctx }) => {
      console.log(`Deleting goal ${input.id} from db`);
      return await ctx.prisma.goal.delete({
        where: {
          id: input.id,
        },
      });
    }),

  getTags: protectedProcedure
    .input(z.object({ goalId: z.string() }))
    .query(async ({ input, ctx }) => {
      return (
        await ctx.db
          .select({ tag: tag })
          .from(tag)
          .innerJoin(goalTag, eq(tag.id, goalTag.tagId))
          .where(eq(goalTag.tagId, input.goalId))
      ).map((it) => it.tag);
    }),

  getMetrics: protectedProcedure
    .input(z.object({ goalId: z.string() }))
    .query(async ({ ctx, input }) => {
      const metrics = await ctx.db
        .select({ metric })
        .from(metric)
        .where(
          and(
            eq(metric.ownerId, ctx.session.user.id),
            eq(metricMeasuresGoal.goalId, input.goalId)
          )
        )
        .innerJoin(
          metricMeasuresGoal,
          eq(metric.id, metricMeasuresGoal.metricId)
        );

      const data = metrics.map((m) => remapTypes(m.metric));
      return data;
    }),

  getGoal: protectedProcedure
    .input(z.object({ id: z.string() }))
    .query(async ({ input, ctx }) => {
      const preferences = await getPreferences(ctx.db, ctx.session.user.id);
      const scoringWeeks = preferences.scoringWeeks;

      const g = (
        await ctx.db
          .select()
          .from(goal)
          .where(
            and(eq(goal.id, input.id), eq(goal.ownerId, ctx.session.user.id))
          )
      )[0]!;

      const [metrics, metricsMap] = await getMetrics({
        db: ctx.db,
        userId: ctx.session.user.id,
        scoringWeeks: scoringWeeks,
        goalIds: [g.id],
      });

      const [habits, _map] = await getHabits({
        prisma: ctx.prisma,
        db: ctx.db,
        metricsMap,
        userId: ctx.session.user.id,
        scoringWeeks: scoringWeeks,
        goalIds: [input.id],
      });

      return {
        ...g,
        habits,
        metrics,
      };
    }),

  getAllGoals: protectedProcedure.query(async ({ ctx }) => {
    const scoringWeeks = (await getPreferences(ctx.db, ctx.session.user.id))
      .scoringWeeks;
    const [metrics, metricsMap] = await getMetrics({
      db: ctx.db,
      userId: ctx.session.user.id,
      scoringWeeks,
    });
    console.log("1");

    const [habits, habitsMap] = await getHabits({
      prisma: ctx.prisma,
      db: ctx.db,
      metricsMap,
      userId: ctx.session.user.id,
      scoringWeeks,
    });
    console.log("2");

    const [goalsData, goalsMap] = await getGoals(
      ctx.prisma,
      ctx.db,
      ctx.session.user.id,
      metricsMap,
      habitsMap
    );
    console.log("3");

    return {
      goals: goalsData,
      goalsMap: goalsMap,
      habits: habits.filter((h) => h.goals.length == 0),
      metrics: metrics.filter(
        (m) => m.linkedHabits.length == 0 && m.goals.length == 0
      ),
    };
  }),

  getAllMetrics: protectedProcedure.query(async ({ ctx }) => {
    const preferences = await getPreferences(ctx.db, ctx.session.user.id);
    const scoringWeeks = preferences.scoringWeeks;

    const [metrics, metricsMap] = await getMetrics({
      db: ctx.db,
      userId: ctx.session.user.id,
      scoringWeeks,
    });

    return {
      metrics: metrics,
      metricsMap: metricsMap,
    };
  }),

  editGoal: protectedProcedure
    .input(
      z.object({
        goalId: z.string(),
        name: z.string(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      console.log(
        `Editing goal ${input.goalId} in db with description ${input.name}`
      );
      return await ctx.prisma.goal.update({
        where: {
          id: input.goalId,
        },
        data: {
          name: input.name,
        },
      });
    }),

  getGoalOnly: protectedProcedure.query(async ({ ctx }) => {
    const goals = await ctx.db
      .select({ id: goal.id, name: goal.name })
      .from(goal)
      .where(eq(goal.ownerId, ctx.session.user.id));

    return {
      goalData: goals,
    };
  }),

  createGoal: protectedProcedure
    .input(z.object({ name: z.string() }))
    .mutation(async ({ input, ctx }) => {
      const goal = await ctx.prisma.goal.create({
        data: {
          name: input.name,
          ownerId: ctx.session.user.id,
        },
      });
      return goal;
    }),

  getWeights: protectedProcedure
    .input(z.object({ goalId: z.string() }))
    .query(async ({ input, ctx }) => {
      return await ctx.db.query.goal.findFirst({
        where: eq(goal.id, input.goalId),
        with: {
          habits: true,
          metrics: true,
        },
      });
    }),

  updateHabitWeight: protectedProcedure
    .input(
      z.object({
        goalId: z.string(),
        habitId: z.string(),
        weight: z.number(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.habitMeasuresGoal.update({
        where: {
          goalId_habitId: {
            goalId: input.goalId,
            habitId: input.habitId,
          },
        },
        data: {
          weight: input.weight,
        },
      });
    }),

  updateMetricWeight: protectedProcedure
    .input(
      z.object({
        goalId: z.string(),
        metricId: z.string(),
        weight: z.number(),
      })
    )
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.metricMeasuresGoal.update({
        where: {
          goalId_metricId: {
            goalId: input.goalId,
            metricId: input.metricId,
          },
        },
        data: {
          weight: input.weight,
        },
      });
    }),

  archive: protectedProcedure
    .input(z.object({ goalId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.goal.update({
        data: {
          archived: true,
        },
        where: {
          id: input.goalId,
        },
      });
    }),

  unarchive: protectedProcedure
    .input(z.object({ goalId: z.string() }))
    .mutation(async ({ input, ctx }) => {
      await ctx.prisma.goal.update({
        data: {
          archived: false,
        },
        where: {
          id: input.goalId,
        },
      });
    }),
});
