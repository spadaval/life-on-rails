import type { Metric } from "@prisma/client";
import { z } from "zod";
import { createTRPCRouter, protectedProcedure } from "../trpc";

const templateMetadata = {
  fitness: {
    goal: "Improve cardiovascular health and endurance",
    habits: [
      {
        description: "1 hour of cardio exercise (running, cycling, swimming)",
        linkedmetrics: [
          {
            name: "Rate your perceived exertion level during each cardio session",
          },
        ],
      },
    ],
    metrics: [
      {
        prompt: "Rate your overall energy level after each cardio workout",
      },
    ],
  },
  productivity: {
    goal: "Enhance focus and time management skills",
    habits: [
      {
        description: "Time blocking for focused work periods.",
        linkedmetrics: [{ name: "Rate your level of focus and productivity" }],
      },
    ],
    metrics: [
      {
        prompt: "Rate your ability to stick to the scheduled time blocks",
      },
    ],
  },
  peace: {
    goal: "Cultivate daily mindfulness and reduce stress.",
    habits: [
      {
        description: "Mindful breathing exercises",
        linkedmetrics: [
          {
            name: "Rate your ability to maintain focus on your breath without distractions",
          },
        ],
      },
    ],
    metrics: [
      {
        prompt:
          "Rate your level of relaxation and peace of mind after each mindfulness practice",
      },
    ],
  },
};

const data = new Map(Object.entries(templateMetadata));

export const templateRouter = createTRPCRouter({
  createFromTemplate: protectedProcedure
    .input(z.object({ key: z.string() }))
    .mutation(async ({ input, ctx }) => {
      const createMeta = data.get(input.key)!;

      const goal = await ctx.prisma.goal.create({
        data: {
          name: createMeta.goal,
          ownerId: ctx.session.user.id,
        },
      });

      createMeta.habits.map(async (habit) => {
        const createdHabit = await ctx.prisma.habit.create({
          data: {
            description: habit.description,
            ownerId: ctx.session.user.id,
          },
        });
        await ctx.prisma.habitMeasuresGoal.create({
          data: {
            goalId: goal.id,
            habitId: createdHabit.id,
          },
        });

        await Promise.all(
          habit.linkedmetrics.map(async (linkedmetric) => {
            const format = await ctx.prisma.answerFormat.create({
              data: { format: "FIVE_POINT" },
            });

            // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
            const createdMetric: Metric = await ctx.prisma.metric.create({
              data: {
                prompt: linkedmetric.name,
                ownerId: ctx.session.user.id,
                formatId: format.id,
              },
            });
            return ctx.prisma.linkedMetric.create({
              data: {
                metricId: createdMetric.id,
                habitId: createdHabit.id,
              },
            });
          })
        );
      }); // end habits

      await Promise.all(
        createMeta.metrics.map(async (metric) => {
          const format = await ctx.prisma.answerFormat.create({
            data: { format: "FIVE_POINT" },
          });
          // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
          const m: Metric = await ctx.prisma.metric.create({
            data: {
              prompt: metric.prompt,
              ownerId: ctx.session.user.id,
              formatId: format.id,
            },
          });

          return ctx.prisma.metricMeasuresGoal.create({
            data: {
              metricId: m.id,
              goalId: goal.id,
            },
          });
        })
      );
    }),
});
