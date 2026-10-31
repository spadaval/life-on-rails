// /api/v1: the `lor` CLI's backend. One endpoint per verb; each handler calls
// the same function the UI calls, so every write has one implementation.
// Auth is a bearer token; the production entry also requires a loopback
// source address for /api/. Everything written here is stamped via=agent.
import { timingSafeEqual } from "node:crypto";
import { z, ZodError } from "zod";
import { sql } from "../../db/client.js";
import type { Json } from "../../shared/json.js";
import {
  confirmDay, pauseRunningBlock, proposeTime, recategorizeBlock, resumeRunningBlock, startManualBlock, stopRunningBlock,
} from "../attribution/blocks.js";
import { env } from "../env.js";
import { BadInput, NotFound, Refused } from "../errors.js";
import { errorMessage, log } from "../log.js";
import { getGraph, getIntentDetail, renderGraph, taskContextMarkdown } from "../graph.js";
import {
  INTENT_TYPES, LINK_KINDS, getIntent, unflagTask, assignTask, claimTask, nextQueuedTask, queueTask, commitHabit, commitMetric, completeTask, createIntent, flagTask, linkIntents,
  unlinkEnds, updateIntent,
} from "../intents.js";
import { agentLog } from "../memory/agentLog.js";
import { getChanges } from "../memory/changes.js";
import { getContext } from "../memory/context.js";
import { resolveDate } from "../memory/day.js";
import "../memory/todaySection.js";
import "../memory/peopleSection.js";
import { PEOPLE_FILTERS, type PeopleFilter, birthdayLine, dueLine, listPeople } from "../people.js";
import { ENTITY_KINDS, findEntity, mergeEntities, showEntity, updateEntity, upsertEntity } from "../memory/entities.js";
import { createPage, pageHistory, readPage, searchPages, updatePage } from "../memory/pages.js";
import { SEARCH_KINDS, search } from "../memory/search.js";
import "../memory/worldSections.js";
import { explainScore, recomputeScores } from "../scoring/recompute.js";
import { markMailHandled, pendingMail } from "../mail/wake.js";
import { messageHistory, sendMessage } from "../channel/channel.js";
import { whatsappChannel } from "../channel/client.js";
import { addPlace, addRule, deleteRule, listApps, listPlaces, listRules } from "../places.js";
import { getStatus } from "../status.js";
import { getToday, renderToday } from "../today.js";

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

route("GET", /^\/context$/, async ({ query }) => {
  const task = query.get("task");
  if (task) return { task, markdown: await taskContextMarkdown(sql, Uuid.parse(task)) };
  return getContext(sql);
});
route("GET", /^\/status$/, async () => getStatus());
route("GET", /^\/changes$/, async ({ query }) => {
  const since = timeParam(query, "since");
  if (!since) throw new BadInput("--since <rfc3339> is required");
  return getChanges(sql, since);
});
route("GET", /^\/day$/, async ({ query }) => {
  const date = resolveDate(query.get("date"));
  const day = await getToday(sql, date);
  const page = await sql<{ body: string }[]>`SELECT body FROM page WHERE slug = ${"days/" + date}`;
  const notes = page[0]?.body ?? null;
  return { ...day, page: notes, markdown: renderToday(day) + (notes ? `\n## Day page\n\n${notes.trim()}\n` : "") };
});

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

const MergeEntity = z.object({ from: z.string().min(1), thread: Thread });

route("POST", /^\/entities\/(.+)\/merge$/, async ({ params, body }) => {
  const d = MergeEntity.parse(body);
  const keep = await findEntity(sql, slugOf(params));
  if (!keep) throw new NotFound(`no entity matches ${JSON.stringify(slugOf(params))}`);
  const from = await findEntity(sql, d.from);
  if (!from) throw new NotFound(`no entity matches ${JSON.stringify(d.from)}`);
  return mergeEntities(sql, keep.id, from.id, d.thread);
});

const SetEntity = z.object({
  name: z.string().nullish(),
  add_aliases: z.array(z.string()).optional(),
  remove_aliases: z.array(z.string()).optional(),
  keep_in_touch_days: z.number().int().positive().nullish(),
  clear_keep_in_touch: z.boolean().optional(),
  birthday: z.string().nullish(),
  clear_birthday: z.boolean().optional(),
  priority: z.boolean().optional(),
  thread: Thread,
});

route("POST", /^\/entities\/(.+)$/, async ({ params, body }) => {
  const d = SetEntity.parse(body);
  const row = await findEntity(sql, slugOf(params));
  if (!row) throw new NotFound(`no entity matches ${JSON.stringify(slugOf(params))}`);
  return updateEntity(sql, row.id, {
    name: d.name ?? undefined,
    addAliases: d.add_aliases,
    removeAliases: d.remove_aliases,
    priority: d.priority,
    keepInTouchDays: d.clear_keep_in_touch ? null : (d.keep_in_touch_days ?? undefined),
    birthday: d.clear_birthday ? null : (d.birthday ?? undefined),
  });
});


route("GET", /^\/people$/, async ({ query }) => {
  const filter = (query.get("filter") ?? "important") as PeopleFilter;
  if (!(PEOPLE_FILTERS as readonly string[]).includes(filter)) throw new BadInput(`filter is one of ${PEOPLE_FILTERS.join(", ")}`);
  const { people, total } = await listPeople(sql, { filter, q: query.get("q") ?? undefined, limit: intParam(query, "limit") ?? 50, birthdayWindow: intParam(query, "days") ?? 7 });
  const lines = people.map((p) =>
    filter === "birthdays" ? `- ${birthdayLine(p)}` : p.keep_in_touch_days ? `- ${dueLine(p)}` : `- ${p.name}: ${p.days_since === null ? "never in touch" : `last in touch ${p.days_since} days ago`} (entity ${p.id})`,
  );
  return { results: people, total, markdown: `${total} ${filter === "important" ? "people who matter" : filter}${total > people.length ? `, first ${people.length}` : ""}\n\n${lines.join("\n")}\n` };
});

// ---- log ----

const LogEntry = z.object({ text: z.string().trim().min(1), intent_id: OptionalUuid, thread: Thread });

route("POST", /^\/log$/, async ({ body }) => {
  const d = LogEntry.parse(body);
  return agentLog(sql, { text: d.text, intentId: d.intent_id, thread: d.thread });
});

// ---- places, apps, and rules: what phone and place time stands for ----

route("GET", /^\/places$/, async () => listPlaces(sql));

const AddPlace = z.object({
  name: z.string(), lat: z.number(), lon: z.number(), radius_m: z.number().nullish(), intent_id: Uuid.nullish(), thread: Thread,
});

route("POST", /^\/places$/, async ({ body }) => {
  const d = AddPlace.parse(body);
  return addPlace(sql, { name: d.name, lat: d.lat, lon: d.lon, radiusM: d.radius_m, intentId: d.intent_id });
});

route("GET", /^\/apps$/, async ({ query }) => listApps(sql, Number(query.get("days") ?? 14) || 14));

route("GET", /^\/rules$/, async () => listRules(sql));

const AddRule = z.object({ app: z.string().nullish(), title: z.string().nullish(), place: z.string().nullish(), intent_id: Uuid, thread: Thread });

route("POST", /^\/rules$/, async ({ body }) => {
  const d = AddRule.parse(body);
  return addRule(sql, { app: d.app, title: d.title, place: d.place, intentId: d.intent_id });
});

route("DELETE", /^\/rules\/([0-9a-f-]{36})$/, async ({ params }) => deleteRule(sql, params[0]!));

// ---- mail waiting for the agent ----

route("GET", /^\/mail\/pending$/, async () => pendingMail(sql, { quietS: env.LOR_WAKE_QUIET_S, maxS: env.LOR_WAKE_MAX_S }));

const MailDone = z.object({ observation_ids: z.array(Uuid).min(1), thread: Thread });

route("POST", /^\/mail\/done$/, async ({ body }) => markMailHandled(sql, MailDone.parse(body).observation_ids));

route("POST", /^\/intents\/([0-9a-f-]{36})\/unflag$/, async ({ params, body }) => {
  z.object({ thread: Thread }).parse(body);
  await unflagTask(sql, params[0]!);
  return getIntent(sql, params[0]!);
});

// ---- the agent's own work ----

route("GET", /^\/work\/next$/, async () => ({ task: await nextQueuedTask(sql) }));
route("POST", /^\/intents\/([0-9a-f-]{36})\/queue$/, async ({ params, body }) => {
  z.object({ thread: Thread }).parse(body);
  return queueTask(sql, params[0]!);
});
route("POST", /^\/intents\/([0-9a-f-]{36})\/claim$/, async ({ params, body }) =>
  claimTask(sql, params[0]!, z.object({ thread: Thread }).parse(body).thread),
);

// ---- the channel ----

const MessageSend = z.object({
  text: z.string().trim().min(1),
  attach: z.string().min(1).nullish(),
  intent_id: Uuid.nullish(),
  thread: Thread,
});

route("POST", /^\/messages$/, async ({ body }) => {
  const d = MessageSend.parse(body);
  return sendMessage(sql, whatsappChannel, { text: d.text, thread: d.thread, intentId: d.intent_id, attach: d.attach });
});

route("GET", /^\/messages$/, async ({ query }) => {
  const around = query.get("around");
  const since = query.get("since");
  return {
    results: await messageHistory(sql, {
      since: since ? new Date(since) : null,
      search: query.get("q"),
      around: around ? Uuid.parse(around) : null,
      limit: query.get("limit") ? z.coerce.number().int().min(1).parse(query.get("limit")) : undefined,
    }),
  };
});

// ---- the world model ----

route("GET", /^\/graph$/, async ({ query }) => {
  // Scores are recomputed shortly after a write; a read straight after one must not see the old number.
  await recomputeScores(sql);
  let { nodes } = await getGraph(sql);
  if (query.get("starving")) nodes = nodes.filter((n) => n.starving);
  if (query.get("waiting")) nodes = nodes.filter((n) => n.status === "waiting");
  return { results: nodes, markdown: renderGraph(nodes) };
});

route("GET", /^\/intents\/([0-9a-f-]{36})$/, async ({ params }) => {
  await recomputeScores(sql);
  return getIntentDetail(sql, params[0]!);
});

const CreateIntent = z.object({
  type: z.enum(INTENT_TYPES),
  title: z.string(),
  description: z.string().nullish(),
  parent_id: OptionalUuid,
  due_at: z.string().nullish(),
  waiting_on_entity_id: OptionalUuid,
  assignee: z.enum(["owner", "agent"]).nullish(),
  weight: z.number().nullish(),
  weekly_hour_target: z.number().nullish(),
  frequency: z.number().int().nullish(),
  frequency_horizon: z.enum(["day", "week"]).nullish(),
  estimate_hours: z.number().nullish(),
  /** Why it needs Sumeet: also flags it (see flagTask). */
  flag: z.string().trim().min(1).nullish(),
  thread: Thread,
});

function parseTime(value: string | null | undefined, name: string): Date | null {
  if (!value) return null;
  const t = new Date(value);
  if (Number.isNaN(t.getTime())) throw new BadInput(`${name} must be an RFC 3339 time`);
  return t;
}

route("POST", /^\/intents$/, async ({ body }) => {
  const d = CreateIntent.parse(body);
  if (d.flag && d.type !== "task") throw new BadInput("only a task can be flagged");
  const created = await createIntent(
    sql,
    {
      type: d.type,
      title: d.title,
      description: d.description,
      parentId: d.parent_id,
      dueAt: parseTime(d.due_at, "--due"),
      waitingOnEntityId: d.waiting_on_entity_id,
      assignee: d.assignee ?? undefined,
      threadRef: d.assignee === "agent" ? d.thread : null,
      weight: d.weight ?? undefined,
      weeklyHourTarget: d.weekly_hour_target,
      frequency: d.frequency,
      frequencyHorizon: d.frequency_horizon,
      estimateHours: d.estimate_hours,
    },
    { via: "agent", sourceRef: d.thread },
  );
  return d.flag ? flagTask(sql, created.id, d.flag) : created;
});

const CreateLink = z.object({ from_id: Uuid, to_id: Uuid, kind: z.enum(LINK_KINDS), weight: z.number().min(0).nullish(), thread: Thread });

route("POST", /^\/links$/, async ({ body }) => {
  const d = CreateLink.parse(body);
  return linkIntents(sql, { fromId: d.from_id, toId: d.to_id, kind: d.kind, weight: d.weight ?? undefined }, { via: "agent", sourceRef: d.thread });
});

route("POST", /^\/links\/remove$/, async ({ body }) => {
  const d = CreateLink.omit({ weight: true }).parse(body);
  return unlinkEnds(sql, { fromId: d.from_id, toId: d.to_id, kind: d.kind });
});

route("POST", /^\/intents\/([0-9a-f-]{36})\/assign$/, async ({ params, body }) => {
  const d = z.object({ assignee: z.enum(["owner", "agent"]), thread: Thread }).parse(body);
  return assignTask(sql, params[0]!, d.assignee, d.thread);
});

route("POST", /^\/intents\/([0-9a-f-]{36})\/flag$/, async ({ params, body }) => {
  const d = z.object({ note: z.string(), thread: Thread }).parse(body);
  return flagTask(sql, params[0]!, d.note);
});

// Only when Sumeet asks in the thread: pause, shelve, archive, or bring back a goal, project, or task.
route("POST", /^\/intents\/([0-9a-f-]{36})\/status$/, async ({ params, body }) => {
  const d = z.object({ status: z.enum(["active", "paused", "later", "archived"]), thread: Thread }).parse(body);
  return updateIntent(sql, params[0]!, { status: d.status });
});
// Only when Sumeet asks in the thread: move a task's due time, or clear it with "off".
route("POST", /^\/intents\/([0-9a-f-]{36})\/due$/, async ({ params, body }) => {
  const d = z.object({ due_at: z.string().nullable(), thread: Thread }).parse(body);
  return updateIntent(sql, params[0]!, { dueAt: parseTime(d.due_at, "<t>") });
});

route("POST", /^\/intents\/([0-9a-f-]{36})\/done$/, async ({ params, body }) => {
  const d = z.object({ memo: z.string().nullish(), thread: Thread }).parse(body);
  return completeTask(sql, params[0]!, { memo: d.memo, via: "agent", sourceRef: d.thread });
});

route("GET", /^\/intents\/([0-9a-f-]{36})\/score$/, async ({ params }) => {
  await recomputeScores(sql);
  const s = await explainScore(sql, params[0]!);
  if (!s) throw new NotFound(`no score for ${params[0]}`);
  return s;
});

const Dated = z.object({ date: z.string().nullish(), memo: z.string().nullish(), thread: Thread });

route("POST", /^\/intents\/([0-9a-f-]{36})\/habit$/, async ({ params, body }) => {
  const d = Dated.parse(body);
  return commitHabit(sql, params[0]!, { date: d.date ? resolveDate(d.date) : undefined, memo: d.memo, via: "agent", sourceRef: d.thread });
});

route("POST", /^\/intents\/([0-9a-f-]{36})\/metric$/, async ({ params, body }) => {
  const d = Dated.extend({ value: z.number() }).parse(body);
  return commitMetric(sql, params[0]!, d.value, { date: d.date ? resolveDate(d.date) : undefined, memo: d.memo, via: "agent", sourceRef: d.thread });
});

// ---- time ----

const agentWho = (thread: string) => ({ via: "agent" as const, sourceRef: thread });

route("POST", /^\/timer\/(start|stop|pause|resume)$/, async ({ params, body }) => {
  const d = z.object({ intent_id: OptionalUuid, title: z.string().nullish(), thread: Thread }).parse(body);
  switch (params[0]) {
    case "start":
      return startManualBlock(sql, { intentId: d.intent_id, title: d.title }, agentWho(d.thread));
    case "stop":
      return stopRunningBlock(sql, agentWho(d.thread));
    case "pause":
      return pauseRunningBlock(sql);
    default:
      return resumeRunningBlock(sql, agentWho(d.thread));
  }
});

route("POST", /^\/blocks\/([0-9a-f-]{36})\/recategorize$/, async ({ params, body }) => {
  const d = z.object({ intent_id: Uuid, thread: Thread }).parse(body);
  await recategorizeBlock(sql, params[0]!, d.intent_id, agentWho(d.thread));
  return { ok: true };
});

route("POST", /^\/confirm$/, async ({ body }) => {
  const d = z.object({ date: z.string(), all: z.boolean().optional(), accept: z.array(Uuid).optional(), reject: z.array(Uuid).optional(), thread: Thread }).parse(body);
  return confirmDay(sql, { date: resolveDate(d.date), all: d.all, accept: d.accept, reject: d.reject }, agentWho(d.thread));
});

route("POST", /^\/proposals$/, async ({ body }) => {
  const d = z.object({ block_id: Uuid, intent_id: Uuid, reason: z.string().nullish(), thread: Thread }).parse(body);
  return proposeTime(sql, d.block_id, d.intent_id, agentWho(d.thread), d.reason);
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
