import { z } from "zod";

import { createTRPCRouter, protectedProcedure } from "../trpc";
import type { PrismaClient } from "@prisma/client";

export const taggingRouter = createTRPCRouter({
  getSecretMessage: protectedProcedure.query(() => {
    return "you can now see this secret message!";
  }),

  linkGoal: protectedProcedure
    .input(z.object({ goalId: z.string(), tagName: z.string() }))
    .mutation(async ({ input, ctx }) => {
      const tagId = await getTag(
        ctx.prisma,
        ctx.session.user.id,
        input.tagName
      );
      await ctx.prisma.goalTag.upsert({
        where: {
          tagId_goalId: {
            goalId: input.goalId,
            tagId: tagId,
          },
        },
        create: {
          goalId: input.goalId,
          tagId: tagId,
        },
        update: {
          goalId: input.goalId,
          tagId: tagId,
        },
      });
    }),

  linkHabit: protectedProcedure
    .input(z.object({ habitId: z.string(), tagName: z.string() }))
    .mutation(async ({ input, ctx }) => {
      const tagId = await getTag(
        ctx.prisma,
        ctx.session.user.id,
        input.tagName
      );

      await ctx.prisma.habitTag.upsert({
        where: {
          tagId_habitId: {
            habitId: input.habitId,
            tagId: tagId,
          },
        },
        create: {
          habitId: input.habitId,
          tagId: tagId,
        },
        update: {
          habitId: input.habitId,
          tagId: input.habitId,
        },
      });
    }),

  unlinkHabit: protectedProcedure
    .input(z.object({ habitId: z.string(), tagName: z.string() }))
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.habitTag.deleteMany({
        where: {
          habitId: input.habitId,
          tag: { name: input.tagName },
        },
      });
    }),

  unlinkGoal: protectedProcedure
    .input(z.object({ goalId: z.string(), tagName: z.string() }))
    .mutation(async ({ input, ctx }) => {
      return await ctx.prisma.goalTag.deleteMany({
        where: {
          goalId: input.goalId,
          tag: { name: input.tagName },
        },
      });
    }),

  getTags: protectedProcedure.query(async ({ ctx }) => {
    return await ctx.prisma.tag.findMany({
      where: {
        ownerId: ctx.session.user.id,
      },
    });
  }),
});
async function getTag(
  prisma: PrismaClient,
  userId: string,
  tagName: string
): Promise<string> {
  const existingTag = await prisma.tag.findFirst({
    where: { name: tagName },
    select: { id: true, name: true },
  });

  const tagId =
    existingTag?.id ??
    (
      await prisma.tag.create({
        data: { name: tagName, ownerId: userId },
      })
    ).id;
  return tagId;
}
