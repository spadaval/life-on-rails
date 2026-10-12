// postgres.js client. Forces the Unix socket dir when DATABASE_URL has an
// empty host: postgres.js would otherwise treat it as TCP localhost:5432,
// but peer auth only works over the socket (local … peer in pg_hba.conf).
// Spec: DATABASE_URL=postgres:///life_on_rails?host=/var/run/postgresql
import postgres from "postgres";

export interface DbOptions {
  host: string;
  database: string;
  username?: string;
}

export function parseDatabaseUrl(raw?: string): DbOptions {
  const input = raw ?? process.env.DATABASE_URL ?? "postgres:///life_on_rails?host=/var/run/postgresql";
  const u = new URL(input);
  const params = u.searchParams;
  const database = decodeURIComponent(u.pathname.replace(/^\//, "") || "life_on_rails");
  // Empty hostname (postgres:///db?host=/sock) means Unix socket; postgres.js
  // must get the socket dir as `host`, never "" (which falls back to TCP).
  const host = u.hostname || params.get("host") || "/var/run/postgresql";
  const username = decodeURIComponent(u.username || "");
  return { host, database, ...(username ? { username } : {}) };
}

const opts = parseDatabaseUrl();

export const sql = postgres({
  host: opts.host,
  database: opts.database,
  ...(opts.username ? { username: opts.username } : {}),
  // One box, one user: small pool.
  max: 5,
  idle_timeout: 20,
  connect_timeout: 10,
});
