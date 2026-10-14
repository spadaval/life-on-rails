// /api/v1: the `lor` CLI's backend. One endpoint per verb; each handler calls
// the same function the UI calls, so every write has one implementation.
// Auth is a bearer token; the production entry also requires a loopback
// source address for /api/. Everything written here is stamped via=agent.
import { timingSafeEqual } from "node:crypto";
import { z, ZodError } from "zod";
import { sql } from "../../db/client.js";
import type { Json } from "../../shared/json.js";
import { env } from "../env.js";
import { BadInput, NotFound, Refused } from "../errors.js";
import { errorMessage, log } from "../log.js";
import { agentLog } from "../memory/agentLog.js";
import { getChanges } from "../memory/changes.js";
import { getContext } from "../memory/context.js";
import { getDay, resolveDate } from "../memory/day.js";
import { ENTITY_KINDS, showEntity, upsertEntity } from "../memory/entities.js";
import { createPage, pageHistory, readPage, searchPages, updatePage } from "../memory/pages.js";
import { SEARCH_KINDS, search } from "../memory/search.js";
import { getStatus } from "../status.js";

export interface ApiRequest {
  query: URLSearchParams;
  /** Captures from the route pattern. */
  params: string[];
  body: unknown;
}

type Handler = (req: ApiRequest) => Promise<unknown>;

interface Route {
  method: "GET" | "POST" | "PUT" | "DELETE";
  pattern: RegExp;
  handler: Handler;
}

const routes: Route[] = [];

export function route(method: Route["method"], pattern: RegExp, handler: Handler): void {
  routes.push({ method, pattern, handler });
}

// ---- shared input pieces ----

/** Every agent write names the T3 thread it came from. */
export const Thread = z.string().trim().min(1, "pass --thread <your T3 thread id>");
const Uuid = z.string().uuid();
const OptionalUuid = Uuid.nullish();

export function intParam(query: URLSearchParams, name: string): number | undefined {
  const raw = query.get(name);
  if (raw === null || raw === "") return undefined;
  const n = Number(raw);
  if (!Number.isInteger(n)) throw new BadInput(`${name} must be a whole number`);
  return n;
}

export function timeParam(query: URLSearchParams, name: string): Date | undefined {
  const raw = query.get(name);
  if (!raw) return undefined;
  const t = new Date(raw);
  if (Number.isNaN(t.getTime())) throw new BadInput(`${name} must be an RFC 3339 time`);
  return t;
}

const slugOf = (params: string[]) => decodeURIComponent(params[0] ?? "");

// ---- orientation ----

route("GET", /^\/context$/, async () => getContext(sql));
route("GET", /^\/status$/, async () => getStatus());
route("GET", /^\/changes$/, async ({ query }) => {
  const since = timeParam(query, "since");
  if (!since) throw new BadInput("--since <rfc3339> is required");
  return getChanges(sql, since);
});
route("GET", /^\/day$/, async ({ query }) => getDay(sql, resolveDate(query.get("date"))));

// ---- search ----

route("GET", /^\/search$/, async ({ query }) => {
  const kinds = (query.get("in") ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  for (const k of kinds) {
    if (!(SEARCH_KINDS as readonly string[]).includes(k)) throw new BadInput(`--in takes ${SEARCH_KINDS.join(",")}`);
  }
  const results = await search(sql, {
    q: query.get("q") ?? "",
    in: kinds as (typeof SEARCH_KINDS)[number][],
    from: timeParam(query, "from"),
    to: timeParam(query, "to"),
    limit: intParam(query, "limit"),
  });
  return { results };
});

// ---- pages ----

route("GET", /^\/pages$/, async ({ query }) => ({
  results: await searchPages(sql, query.get("q") ?? "", intParam(query, "limit") ?? 10),
}));

route("GET", /^\/pages\/(.+)$/, async ({ params, query }) => {
  const slug = slugOf(params);
  if (query.get("history")) return { slug, results: await pageHistory(sql, slug) };
  return readPage(sql, slug, intParam(query, "version"));
});

const CreatePage = z.object({
  slug: z.string(),
  title: z.string(),
  body: z.string(),
  entity_id: OptionalUuid,
  intent_id: OptionalUuid,
  note: z.string().nullish(),
  thread: Thread,
});

route("POST", /^\/pages$/, async ({ body }) => {
  const d = CreatePage.parse(body);
  return createPage(sql, {
    slug: d.slug,
    title: d.title,
    body: d.body,
    entityId: d.entity_id,
    intentId: d.intent_id,
    via: "agent",
    sourceRef: d.thread,
    note: d.note,
  });
});

const UpdatePage = z.object({
  base: z.number().int().optional(),
  body: z.string().optional(),
  find: z.string().optional(),
  replace: z.string().optional(),
  append: z.string().optional(),
  title: z.string().optional(),
  note: z.string().nullish(),
  dry_run: z.boolean().optional(),
  thread: Thread,
});

route("PUT", /^\/pages\/(.+)$/, async ({ params, body }) => {
  const d = UpdatePage.parse(body);
  return updatePage(sql, {
    slug: slugOf(params),
    base: d.base,
    body: d.body,
    find: d.find,
    replace: d.replace,
    append: d.append,
    title: d.title,
    dryRun: d.dry_run,
    via: "agent",
    sourceRef: d.thread,
    note: d.note,
  });
});

// ---- entities ----

const UpsertEntity = z.object({
  kind: z.enum(ENTITY_KINDS),
  name: z.string(),
  emails: z.array(z.string()).optional(),
  aliases: z.array(z.string()).optional(),
  identifiers: z.array(z.object({ kind: z.string(), value: z.string() })).optional(),
  thread: Thread,
});

route("POST", /^\/entities$/, async ({ body }) => {
  const d = UpsertEntity.parse(body);
  return upsertEntity(sql, {
    kind: d.kind,
    name: d.name,
    aliases: d.aliases,
    identifiers: [...(d.identifiers ?? []), ...(d.emails ?? []).map((value) => ({ kind: "email", value }))],
  });
});

route("GET", /^\/entities\/(.+)$/, async ({ params }) => showEntity(sql, slugOf(params)));

// ---- log ----

const LogEntry = z.object({ text: z.string().trim().min(1), intent_id: OptionalUuid, thread: Thread });

route("POST", /^\/log$/, async ({ body }) => {
  const d = LogEntry.parse(body);
  return agentLog(sql, { text: d.text, intentId: d.intent_id, thread: d.thread });
});

// ---- dispatch ----

function json(status: number, body: Json): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

function authorized(request: Request): boolean {
  const token = env.LOR_AGENT_TOKEN;
  if (!token) return false;
  const header = request.headers.get("authorization") ?? "";
  const given = header.startsWith("Bearer ") ? header.slice(7) : "";
  const a = Buffer.from(given);
  const b = Buffer.from(token);
  return a.length === b.length && timingSafeEqual(a, b);
}

export async function handleApi(request: Request): Promise<Response> {
  if (!authorized(request)) {
    return json(401, { error: { code: "unauthorized", message: "missing or wrong bearer token" } });
  }
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/api\/v1/, "") || "/";
  let body: unknown = undefined;
  if (request.method !== "GET" && request.method !== "HEAD") {
    const text = await request.text();
    if (text) {
      try {
        body = JSON.parse(text);
      } catch {
        return json(400, { error: { code: "bad-json", message: "request body is not JSON" } });
      }
    }
  }
  for (const r of routes) {
    if (r.method !== request.method) continue;
    const m = r.pattern.exec(path);
    if (!m) continue;
    try {
      const data = await r.handler({ query: url.searchParams, params: m.slice(1), body: body ?? {} });
      return json(200, (data ?? {}) as Json);
    } catch (err) {
      if (err instanceof Refused) return json(409, { error: { code: err.code, message: err.message, detail: err.detail } });
      if (err instanceof NotFound) return json(404, { error: { code: "not-found", message: err.message } });
      if (err instanceof BadInput) return json(400, { error: { code: "bad-input", message: err.message } });
      if (err instanceof ZodError) {
        const message = err.issues.map((i) => `${i.path.join(".") || "input"}: ${i.message}`).join("; ");
        return json(400, { error: { code: "bad-input", message } });
      }
      log("error", "api error", { path, method: request.method, err: errorMessage(err) });
      return json(500, { error: { code: "server-error", message: errorMessage(err) } });
    }
  }
  return json(404, { error: { code: "no-such-endpoint", message: `${request.method} ${path}` } });
}
