// postgres.js client over the Unix socket (peer auth, no password).
// postgres.js treats an empty URL host as TCP localhost, where peer auth does
// not apply, so the socket directory is always passed explicitly.
import postgres from "postgres";
import { env } from "../server/env.js";

export interface DbOptions {
  host: string;
  database: string;
  username?: string;
}

export function parseDatabaseUrl(raw: string): DbOptions {
  const u = new URL(raw);
  const database = decodeURIComponent(u.pathname.replace(/^\//, "") || "life_on_rails");
  const host = u.hostname || u.searchParams.get("host") || "/var/run/postgresql";
  const username = decodeURIComponent(u.username || "");
  return { host, database, ...(username ? { username } : {}) };
}

export function connect(url: string, max = 5): postgres.Sql {
  const opts = parseDatabaseUrl(url);
  return postgres({
    host: opts.host,
    database: opts.database,
    ...(opts.username ? { username: opts.username } : {}),
    max,
    idle_timeout: 20,
    connect_timeout: 10,
    onnotice: () => {},
  });
}

export const sql = connect(env.DATABASE_URL);

export type Sql = postgres.Sql;
/** A transaction handle, or the pool itself: helpers accept either. */
export type Tx = postgres.TransactionSql | postgres.Sql;
