//import { PrismaClient } from "@prisma/client";
import { PrismaClient } from "@prisma/client/edge";
import { env } from "../env/server.mjs";

declare global {
  // eslint-disable-next-line no-var
  var prisma: PrismaClient | undefined;
}

export const prisma =
  global.prisma ||
  new PrismaClient({
    log:
      env.NODE_ENV === "development" ? ["query", "error", "warn"] : ["error"],
  });

if (env.NODE_ENV !== "production") {
  global.prisma = prisma;
}

import { connect } from "@planetscale/database";
// import chalkTemplate from "chalk-template";
import { type Logger } from "drizzle-orm";
import type { PlanetScaleDatabase } from "drizzle-orm/planetscale-serverless";
import { drizzle } from "drizzle-orm/planetscale-serverless";
import type { Schema } from "../schema";
import schema from "../schema";

class MyLogger implements Logger {
  logQuery(query: string, params: unknown[]): void {
    const placeholders = params.map(() => "?").join(",");

    // console.log(
    //   chalkTemplate`{bold.red drizzle.query} ${query} (${placeholders})`
    // );
    console.log(`drizzle.query  ${query} (${placeholders})`);
  }
}

export const db = drizzle(
  connect({
    url: env.DATABASE_URL,
  }),
  { logger: new MyLogger(), schema: schema }
);

export type DB = PlanetScaleDatabase<Schema>;
