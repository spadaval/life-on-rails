import { type GetServerSidePropsContext } from "next";

import { getAuth } from "@clerk/nextjs/server";

/**
 * Wrapper for unstable_getServerSession, used in trpc createContext and the
 * restricted API route
 *
 * Don't worry too much about the "unstable", it's safe to use but the syntax
 * may change in future versions
 *
 * @see https://next-auth.js.org/configuration/nextjs
 */

export const getServerAuthSession = (ctx: {
  req: GetServerSidePropsContext["req"];
  res: GetServerSidePropsContext["res"];
}) => {
  const { userId } = getAuth(ctx.req);
  return { user: (userId ? {id: userId} : undefined) };
};
