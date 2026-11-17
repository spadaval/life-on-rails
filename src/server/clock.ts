// The owner's "now", for every read and write that means the present moment.
//
// On the real service this is the wall clock and nothing else. On the test
// database alone (life_on_rails_test) it can be fixed, so a screen can be drawn
// at a chosen time from the same data (the demo set, seeds/demo/README.md):
//   LOR_FAKE_NOW=<time>   the test server's clock, for every request;
//   ?at=<time>            one tab's clock: the page URL's, or the Referer's for
//                         the server-function calls that page makes.
// A time is an ISO instant with an offset ("2026-11-16T09:00:00-08:00"), a
// local date and time in LOR_TZ ("2026-11-16T09:00"), or a local time alone
// ("09:00"), which is that time on the clock's own local day.
//
// Durations, ids, logs and token expiry stay on the real clock (Date.now()).
import { AsyncLocalStorage } from "node:async_hooks";
import { parseDatabaseUrl } from "../db/client.js";
import { localTimeOnDay, localYmd } from "../shared/localTime.js";
import { env } from "./env.js";

export const TEST_DATABASE = "life_on_rails_test";

/** Whether a database URL names the test database, the only one a fixed clock or a demo variant touches. */
export function isTestDatabase(databaseUrl: string): boolean {
  try {
    return parseDatabaseUrl(databaseUrl).database === TEST_DATABASE;
  } catch {
    return false;
  }
}

/** A clock value as written in LOR_FAKE_NOW or `at=`; null when it is not one. */
export function parseClock(value: string | null | undefined, tz: string, base: Date): Date | null {
  const v = (value ?? "").trim();
  if (!v) return null;
  if (/^\d{1,2}:\d{2}$/.test(v)) return localTimeOnDay(tz, localYmd(tz, base), v.padStart(5, "0"));
  const local = /^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?$/.exec(v);
  if (local) return new Date(localTimeOnDay(tz, local[1]!, local[2]!).getTime() + Number(local[3] ?? 0) * 1000);
  if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})$/.test(v)) return null;
  const t = new Date(v);
  return Number.isNaN(t.getTime()) ? null : t;
}

/**
 * The fixed time, or null for the wall clock. Only the test database is ever
 * given one: on any other, LOR_FAKE_NOW and `at=` are ignored.
 */
export function fixedClock(o: { databaseUrl: string; fakeNow?: string | null; at?: string | null; tz: string; real?: Date }): Date | null {
  if (!isTestDatabase(o.databaseUrl)) return null;
  const real = o.real ?? new Date();
  const server = parseClock(o.fakeNow, o.tz, real);
  return parseClock(o.at, o.tz, server ?? real) ?? server;
}

const store = new AsyncLocalStorage<Date | null>();

/** LOR_FAKE_NOW, resolved once: the test server's clock outside a request. */
let serverFixed: Date | null | undefined;
function serverClock(): Date | null {
  if (serverFixed === undefined) serverFixed = fixedClock({ databaseUrl: env.DATABASE_URL, fakeNow: process.env.LOR_FAKE_NOW, tz: env.LOR_TZ });
  return serverFixed;
}

/** Now, as the owner's data reads it. */
export function clockNow(): Date {
  const fixed = store.getStore() ?? serverClock();
  return fixed ? new Date(fixed.getTime()) : new Date();
}

/** The fixed time this request runs at, or null on the wall clock. The page takes it to stop its own clock. */
export function fixedNow(): string | null {
  const fixed = store.getStore() ?? serverClock();
  return fixed ? fixed.toISOString() : null;
}

/** Run fn with the clock fixed at `at` (null: the server's clock). Used per request, and by the demo loader. */
export function withClock<T>(at: Date | null, fn: () => T): T {
  return store.run(at, fn);
}

/** The `at=` a request asks for: its own URL's, or for a call made by a page, that page's. */
export function requestedAt(request: Request): string | null {
  const own = new URL(request.url).searchParams.get("at");
  if (own) return own;
  const referer = request.headers.get("referer");
  if (!referer) return null;
  try {
    return new URL(referer).searchParams.get("at");
  } catch {
    return null;
  }
}

/** Run a request's handler at the time it asks for, on the test database only. */
export function clockForRequest<T>(request: Request, fn: () => T): T {
  const at = fixedClock({ databaseUrl: env.DATABASE_URL, fakeNow: process.env.LOR_FAKE_NOW, at: requestedAt(request), tz: env.LOR_TZ });
  return store.run(at, fn);
}
