// pnpm demo:test (node .claude/skills/verify-lor/verify.mjs demo)
//
// Empties the TEST database and loads the demo set (seeds/demo/), a made-up
// life with enough of everything to fill every screen. Every row is written
// through LoR's own write paths, each at the clock time it happened, so "done
// today", flag ages and page histories read as they would in a real week.
// Times in the set are relative to the clock: LOR_FAKE_NOW when set (as on the
// test server), else now. Refuses any database but life_on_rails_test.
import { readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type Sql, sql as defaultSql } from "../src/db/client.js";
import { ensureSeed } from "../src/db/seed.js";
import { proposeTime, recategorizeBlock, startManualBlock, stopRunningBlock } from "../src/server/attribution/blocks.js";
import { TEST_DATABASE, clockNow, isTestDatabase, withClock } from "../src/server/clock.js";
import { ingest } from "../src/server/device.js";
import { env } from "../src/server/env.js";
import { GCalendar, GEvent, GTask, GTaskList } from "../src/server/google/client.js";
import { syncCalendars } from "../src/server/google/calendarSync.js";
import type { GMessage, MailPort } from "../src/server/google/mailClient.js";
import { syncTasks } from "../src/server/google/tasksSync.js";
import { commitHabit, commitMetric, completeTask, createIntent, flagTask, queueTask } from "../src/server/intents.js";
import { JOBS } from "../src/server/jobs/registry.js";
import { mailCheck } from "../src/server/mail/check.js";
import { updateEntity, upsertEntity } from "../src/server/memory/entities.js";
import { createPage, setInDigest, updatePage } from "../src/server/memory/pages.js";
import { addPlace, addRule } from "../src/server/places.js";
import { recomputeScores } from "../src/server/scoring/recompute.js";
import { uuidv7 } from "../src/shared/ids.js";
import { type Ymd, addDays, localTimeOnDay, localYmd, weekStart } from "../src/shared/localTime.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const SEED = path.join(ROOT, "seeds/demo");
const ui = { via: "ui" as const };
const agent = (thread: string) => ({ via: "agent" as const, sourceRef: thread });

/** Refuse before the first query: the loader empties every table. */
export function refuseUnlessTest(databaseUrl: string): void {
  if (!isTestDatabase(databaseUrl)) {
    throw new Error(`the demo set loads into ${TEST_DATABASE} only, and empties it first; refusing ${databaseUrl}. Instead: node .claude/skills/verify-lor/verify.mjs demo`);
  }
}

interface Demo {
  device: string;
  places: { key: string; name: string; lat: number; lon: number; radius: number; goal?: string }[];
  goals: {
    key: string; title: string; short: string; weight: number; hours?: number;
    projects: { key: string; title: string; hours?: number }[];
    habits: { key: string; title: string; frequency: number; horizon: "day" | "week"; done: string[] }[];
  }[];
  metrics: { title: string; goal: string; values: Record<string, number> }[];
  tasks: {
    title: string; parent?: string; due?: string; created: string; status?: "waiting" | "later"; waiting?: string; waiting_since?: string;
    agent?: boolean; queued?: string; thread?: string; flag?: string; flagged?: string; done?: string;
  }[];
  google_tasks: { id: string; title: string; due?: string; notes?: string }[];
  people: { key: string; name: string; emails?: string[]; phones?: string[]; cadence?: number; priority?: boolean; birthday_in?: number; born?: number; page?: boolean }[];
  orgs: { key: string; name: string; emails?: string[]; domains?: string[] }[];
  short_codes: { name: string; phone: string }[];
  groups: { key: string; name: string; chat: string; members: string[] }[];
  events: { id: string; title: string; start?: string; end?: string; all_day?: string; attendees?: string[]; transparent?: boolean }[];
  timers: { intent: string; start: string; end: string }[];
  apps: { name: string; pkg: string; start: string; end: string; goal?: string }[];
  fixes: { place: string; from: string; to: string }[];
  proposal: { app: string; intent: string; reason: string };
  messages: {
    person?: string; group?: string; from?: string; short_code?: string; at: string; kind: "sms" | "whatsapp" | "call"; direction: "in" | "out"; text?: string; duration_s?: number;
  }[];
  mail: { id: string; thread: string; from?: string; to?: string; subject: string; at: string; direction: "in" | "sent"; bulk?: boolean }[];
  pinned: string[];
  versioned: { slug: string; title: string; versions: { via: "ui" | "agent"; at: string; note?: string; file: string }[] };
}

interface PageFile {
  slug: string;
  title: string;
  entity?: string;
  intent?: string;
  body: string;
}

function readPageFile(file: string): PageFile {
  const text = readFileSync(path.join(SEED, "pages", file), "utf8");
  const [head, ...rest] = text.split("\n---\n");
  const meta = Object.fromEntries(head!.split("\n").map((l) => [l.slice(0, l.indexOf(":")).trim(), l.slice(l.indexOf(":") + 1).trim()]));
  return { slug: meta.slug!, title: meta.title!, entity: meta.entity, intent: meta.intent, body: rest.join("\n---\n").trim() + "\n" };
}

/** The demo set's times, on the clock's local day: "d0 09:30", "d-2 18:00", or a date "d+3". */
export function demoClock(now: Date, tz: string) {
  const today = localYmd(tz, now);
  const day = (spec: string): Ymd => {
    const m = /^d([+-]?\d+)(?: \d{2}:\d{2})?$/.exec(spec);
    if (!m) throw new Error(`bad demo time ${spec}`);
    return addDays(today, Number(m[1]));
  };
  const at = (spec: string): Date => {
    const m = /^d[+-]?\d+ (\d{2}:\d{2})$/.exec(spec);
    if (!m) throw new Error(`bad demo time ${spec}: wants "d<offset> HH:MM"`);
    return localTimeOnDay(tz, day(spec), m[1]!);
  };
  return { today, day, at };
}

export interface LoadResult {
  now: string;
  ms: number;
  counts: Record<string, number>;
}

export async function loadDemo(opts: { db?: Sql; databaseUrl?: string; now?: Date } = {}): Promise<LoadResult> {
  refuseUnlessTest(opts.databaseUrl ?? env.DATABASE_URL);
  const db = opts.db ?? defaultSql;
  const started = Date.now();
  const now = opts.now ?? clockNow();
  const tz = env.LOR_TZ;
  const { today, day, at } = demoClock(now, tz);
  const demo = JSON.parse(readFileSync(path.join(SEED, "demo.json"), "utf8")) as Demo;
  /** Run a write at the time it happened. */
  const when = <T>(t: Date, fn: () => Promise<T>) => withClock(t, fn);
  const due = (spec: string) => localTimeOnDay(tz, day(spec), "23:59");

  // ---- empty, then the system rows ----
  const tables = await db<{ tablename: string }[]>`SELECT tablename FROM pg_tables WHERE schemaname = 'public'`;
  if (tables.length) await db.unsafe(`TRUNCATE ${tables.map((t) => `"${t.tablename}"`).join(", ")} RESTART IDENTITY CASCADE`);
  await ensureSeed(db);
  const weekAgo = at("d-7 08:00");

  // ---- goals, projects, habits, metrics ----
  const ids = new Map<string, string>();
  const title = new Map<string, string>();
  await when(weekAgo, async () => {
    for (const g of demo.goals) {
      const goal = await createIntent(db, { type: "goal", title: g.title, shortTitle: g.short, weight: g.weight, weeklyHourTarget: g.hours ?? null }, ui);
      ids.set(g.key, goal.id);
      for (const p of g.projects) {
        ids.set(p.key, (await createIntent(db, { type: "project", title: p.title, weeklyHourTarget: p.hours ?? null, parentId: goal.id }, ui)).id);
      }
      for (const h of g.habits) {
        ids.set(h.key, (await createIntent(db, { type: "habit", title: h.title, frequency: h.frequency, frequencyHorizon: h.horizon, parentId: goal.id }, ui)).id);
      }
    }
    for (const m of demo.metrics) {
      ids.set(m.title, (await createIntent(db, { type: "metric", title: m.title, metricKind: "five_point", metricMin: 1, metricMax: 5, parentId: ids.get(m.goal)! }, ui)).id);
    }
  });
  for (const g of demo.goals) {
    for (const h of g.habits) {
      for (const d of h.done) await when(at(`${d} 21:00`), () => commitHabit(db, ids.get(h.key)!, { date: day(d), ...ui }));
    }
  }
  for (const m of demo.metrics) {
    for (const [d, v] of Object.entries(m.values)) await when(at(`${d} 21:30`), () => commitMetric(db, ids.get(m.title)!, v, { date: day(d), ...ui }));
  }

  // ---- people, organisations, short codes, a group ----
  const people = new Map<string, { id: string; email: string | null; phone: string | null }>();
  await when(weekAgo, async () => {
    for (const p of demo.people) {
      const identifiers = [...(p.emails ?? []).map((value) => ({ kind: "email" as const, value })), ...(p.phones ?? []).map((value) => ({ kind: "phone" as const, value }))];
      const e = await upsertEntity(db, { kind: "person", name: p.name, identifiers });
      let birthday: string | null = null;
      if (p.birthday_in !== undefined) {
        const md = addDays(today, p.birthday_in).slice(5);
        birthday = p.born ? `${p.born}-${md}` : md;
      }
      await updateEntity(db, e.id, { keepInTouchDays: p.cadence ?? null, priority: p.priority ?? false, ...(birthday ? { birthday } : {}) });
      people.set(p.key, { id: e.id, email: p.emails?.[0] ?? null, phone: p.phones?.[0] ?? null });
    }
    for (const o of demo.orgs) {
      const identifiers = [...(o.emails ?? []).map((value) => ({ kind: "email" as const, value })), ...(o.domains ?? []).map((value) => ({ kind: "domain" as const, value }))];
      const e = await upsertEntity(db, { kind: "org", name: o.name, identifiers });
      people.set(o.key, { id: e.id, email: o.emails?.[0] ?? null, phone: null });
    }
    for (const s of demo.short_codes) await upsertEntity(db, { kind: "org", name: s.name, identifiers: [{ kind: "phone", value: s.phone }] });
    for (const g of demo.groups) {
      const e = await upsertEntity(db, { kind: "group", name: g.name, identifiers: [{ kind: "whatsapp_group", value: g.chat }] });
      // Members come from a WhatsApp import, which has no write path but the backup itself: written here as it would.
      const phones = g.members.map((k) => people.get(k)?.phone).filter((p): p is string => Boolean(p)).map((p) => p.replace(/[^\d+]/g, ""));
      if (phones.length) await db`INSERT INTO entity_member ${db(phones.map((value) => ({ entity_id: e.id, kind: "phone", value })))} ON CONFLICT DO NOTHING`;
      people.set(g.key, { id: e.id, email: null, phone: null });
    }
  });

  // ---- places, and what time there and in some apps is for ----
  await when(weekAgo, async () => {
    for (const p of demo.places) await addPlace(db, { name: p.name, lat: p.lat, lon: p.lon, radiusM: p.radius, intentId: p.goal ? ids.get(p.goal)! : null });
    for (const a of demo.apps.filter((a) => a.goal)) await addRule(db, { app: a.pkg, intentId: ids.get(a.goal!)! });
  });

  // ---- tasks ----
  for (const t of demo.tasks) {
    const task = await when(at(t.created), () =>
      createIntent(
        db,
        {
          type: "task",
          title: t.title,
          parentId: t.parent ? ids.get(t.parent)! : null,
          dueAt: t.due ? due(t.due) : null,
          status: t.status === "later" ? "later" : undefined,
          waitingOnEntityId: t.waiting ? people.get(t.waiting)!.id : null,
          assignee: t.agent ? "agent" : "owner",
          threadRef: t.thread ?? null,
        },
        t.agent ? agent("demo-thread-inbox") : ui,
      ),
    );
    ids.set(t.title, task.id);
    title.set(task.id, t.title);
    if (t.queued) await when(at(t.queued), () => queueTask(db, task.id));
    if (t.flag) await when(at(t.flagged ?? t.created), () => flagTask(db, task.id, t.flag!));
    if (t.done) await when(at(t.done), () => completeTask(db, task.id, ui));
  }

  // ---- Google Tasks, through the sync ----
  const lists = [GTaskList.parse({ id: "demo-list", title: "My Tasks" })];
  const gtasks = demo.google_tasks.map((t) => GTask.parse({ id: t.id, title: t.title, notes: t.notes, due: t.due ? `${day(t.due)}T00:00:00.000Z` : undefined, updated: now.toISOString() }));
  await when(at("d0 06:00"), () => syncTasks(db, { listTaskLists: async () => lists, listTasks: async () => gtasks }));

  // ---- the calendar, through the sync: polled a few days ago and this morning ----
  const calendars = [GCalendar.parse({ id: "demo@example.com", summary: "demo@example.com", primary: true, selected: true, accessRole: "owner" })];
  const events = demo.events.map((e) => {
    const attendees = (e.attendees ?? []).map((k) => people.get(k)?.email).filter((x): x is string => Boolean(x)).map((email) => ({ email, responseStatus: "accepted" }));
    return GEvent.parse({
      id: e.id,
      summary: e.title,
      start: e.all_day ? { date: day(e.all_day) } : { dateTime: at(e.start!).toISOString() },
      end: e.all_day ? { date: addDays(day(e.all_day), 1) } : { dateTime: at(e.end!).toISOString() },
      transparency: e.transparent ? "transparent" : "opaque",
      attendees: attendees.length ? [{ email: "demo@example.com", self: true, responseStatus: "accepted" }, ...attendees] : undefined,
    });
  });
  const span = (e: GEvent) => [Date.parse(e.start?.dateTime ?? `${e.start?.date}T00:00:00Z`), Date.parse(e.end?.dateTime ?? `${e.end?.date}T00:00:00Z`)] as const;
  const calendarPort = {
    listCalendars: async () => calendars,
    listEvents: async (_id: string, from: Date, to: Date) => events.filter((e) => span(e)[0] < to.getTime() && span(e)[1] > from.getTime()),
  };
  for (const poll of ["d-3 06:00", "d0 06:00"]) await when(at(poll), () => syncCalendars(db, calendarPort, at(poll)));

  // ---- the timer: blocks started and stopped, then confirmed ----
  for (const b of demo.timers) {
    const intentId = ids.get(b.intent);
    if (!intentId) throw new Error(`a timer names ${b.intent}, which the set does not have`);
    const { id } = await when(at(b.start), () => startManualBlock(db, { intentId }, ui, at(b.start)));
    await when(at(b.end), () => stopRunningBlock(db, ui, at(b.end)));
    await when(at(b.end), () => recategorizeBlock(db, id, intentId, ui, {}, at(b.end)));
  }

  // ---- the phone: app use, where it was, texts and calls ----
  const obs = (o: { t: Date; tEnd?: Date | null; app: string | null; title: string | null; payload: Record<string, unknown> }) => ({
    id: uuidv7(o.t.getTime()), t_start: o.t.toISOString(), t_end: o.tEnd === undefined ? o.t.toISOString() : o.tEnd?.toISOString() ?? null,
    device: demo.device, source: "android", app: o.app, calendar_id: null, title: o.title, url: null, payload: o.payload, intent_hint: null,
  });
  const sensed: ReturnType<typeof obs>[] = [];
  for (const a of demo.apps) sensed.push(obs({ t: at(a.start), tEnd: at(a.end), app: a.pkg, title: a.name, payload: { kind: "app", package: a.pkg } }));
  for (const f of demo.fixes) {
    const place = demo.places.find((p) => p.key === f.place)!;
    for (let t = at(f.from).getTime(); t <= at(f.to).getTime(); t += 10 * 60_000) {
      sensed.push(obs({ t: new Date(t), tEnd: null, app: null, title: null, payload: { kind: "location", lat: place.lat, lon: place.lon, accuracy_m: 12 } }));
    }
  }
  for (const m of demo.messages) {
    const t = at(m.at);
    const author = m.group ? people.get(m.from!)?.phone : m.short_code ?? (m.person ? people.get(m.person)?.phone : null);
    if (!author) throw new Error(`a message to ${m.person ?? m.group ?? m.short_code} has no phone number`);
    const group = m.group ? demo.groups.find((g) => g.key === m.group)! : null;
    const payload: Record<string, unknown> = { kind: m.kind, direction: m.direction, phone: author };
    if (m.text) payload.text = m.text;
    if (m.duration_s) payload.duration_s = m.duration_s;
    if (group) Object.assign(payload, { group: true, chat: group.chat, chat_name: group.name });
    const app = m.kind === "whatsapp" ? "com.whatsapp" : m.kind === "call" ? "com.google.android.dialer" : "sms";
    sensed.push(obs({ t, tEnd: m.duration_s ? new Date(t.getTime() + m.duration_s * 1000) : undefined, app, title: group?.name ?? author, payload }));
  }
  const ingested = await when(at("d0 16:20"), () => ingest({ device: demo.device, observations: sensed }));
  if (ingested.rejected.length) throw new Error(`the phone's observations were refused: ${JSON.stringify(ingested.rejected.slice(0, 3))}`);

  // ---- one suggestion from the agent, on a phone block ----
  const [phoneBlock] = await db<{ id: string }[]>`
    SELECT id FROM time_block WHERE origin = 'auto_activity' AND title LIKE ${`${demo.proposal.app}%`} AND NOT committed ORDER BY t_start DESC LIMIT 1`;
  if (phoneBlock) await when(at("d0 16:25"), () => proposeTime(db, phoneBlock.id, ids.get(demo.proposal.intent)!, agent("demo-thread-day"), demo.proposal.reason));

  // ---- mail headers, through the mail check: the first check starts watching, the second sees what came ----
  const messages: GMessage[] = demo.mail.map((m) => ({
    id: m.id,
    threadId: m.thread,
    labelIds: m.direction === "sent" ? ["SENT"] : m.bulk ? ["INBOX", "CATEGORY_UPDATES"] : ["INBOX", "CATEGORY_PERSONAL"],
    internalDate: String(at(m.at).getTime()),
    payload: {
      headers: [
        { name: "From", value: m.direction === "sent" ? "Me <demo@example.com>" : m.from! },
        ...(m.to ? [{ name: "To", value: m.to }] : [{ name: "To", value: "demo@example.com" }]),
        { name: "Subject", value: m.subject },
        ...(m.bulk ? [{ name: "List-Unsubscribe", value: "<mailto:leave@example.net>" }] : []),
      ],
    },
  }));
  let history = 1;
  const mailPort: MailPort = {
    currentHistoryId: async () => String(history),
    addedSince: async () => ({ ids: messages.map((m) => m.id), historyId: String(++history) }),
    recentMessages: async () => [],
    metadata: async (id) => messages.find((m) => m.id === id) ?? null,
  };
  await when(at("d-7 08:00"), () => mailCheck(db, mailPort, at("d-7 08:00")));
  await when(at("d0 16:30"), () => mailCheck(db, mailPort, at("d0 16:30")));

  // ---- the wiki ----
  const files = readdirSync(path.join(SEED, "pages")).filter((f) => f.endsWith(".md"));
  const plain = files.filter((f) => !/^(lisbon-\d|brief-|day-)/.test(f)).map(readPageFile);
  let i = 0;
  for (const p of plain) {
    // Spread over the week, some written by the owner and most by the agent.
    const t = at(`d-${7 - (i % 7)} ${String(8 + (i % 12)).padStart(2, "0")}:15`);
    const who = i % 3 === 0 ? ui : agent("demo-thread-notes");
    // The system's own pages (src/db/seed.ts) are there already: they get the demo's text over them.
    const [seeded] = await db<{ head_version: number }[]>`SELECT head_version FROM page WHERE slug = ${p.slug}`;
    await when<unknown>(t, () =>
      seeded
        ? updatePage(db, { slug: p.slug, base: seeded.head_version, title: p.title, body: p.body, ...who })
        : createPage(db, {
            slug: p.slug, title: p.title, body: p.body, ...who,
            entityId: p.entity ? people.get(p.entity)!.id : null,
            intentId: p.intent ? ids.get(p.intent)! : null,
          }),
    );
    i++;
  }
  for (const slug of demo.pinned) await setInDigest(db, slug, true);
  const v = demo.versioned;
  const body = (file: string) => readFileSync(path.join(SEED, "pages", file), "utf8");
  for (const [n, version] of v.versions.entries()) {
    const who = version.via === "agent" ? agent("demo-thread-lisbon") : ui;
    await when<unknown>(at(version.at), () =>
      n === 0
        ? createPage(db, { slug: v.slug, title: v.title, body: body(version.file), ...who })
        : updatePage(db, { slug: v.slug, base: n, body: body(version.file), ...who, note: version.note ?? null }),
    );
  }
  // The briefs: this morning's, and the long one on Monday (yesterday's short one when today is Monday).
  const monday = weekStart(today);
  const shortDay = monday === today ? addDays(today, -1) : today;
  await when(localTimeOnDay(tz, monday, "06:30"), () => createPage(db, { slug: `briefs/${monday}`, title: `Brief ${monday}`, body: body("brief-monday.md"), ...agent("demo-thread-brief") }));
  await when(localTimeOnDay(tz, shortDay, "06:30"), () => createPage(db, { slug: `briefs/${shortDay}`, title: `Brief ${shortDay}`, body: body("brief-short.md"), ...agent("demo-thread-brief") }));
  for (const n of [1, 2]) {
    const d = addDays(today, -n);
    await when(localTimeOnDay(tz, addDays(d, 1), "05:30"), () => createPage(db, { slug: `days/${d}`, title: `Day ${d}`, body: body(`day-${n}.md`), ...agent("demo-thread-nightly") }));
  }

  // ---- the jobs, healthy: each reported in at the end of the demo day, so every clock time that day reads healthy ----
  const reported = localTimeOnDay(tz, today, "23:55");
  for (const job of JOBS) {
    await db`INSERT INTO job_run (id, name, started_at, finished_at, status, stats)
      VALUES (${uuidv7()}, ${job.name}, ${reported}, ${reported}, 'ok', ${db.json({ demo: true })})`;
  }

  await when(now, () => recomputeScores(db, now));

  const [counts] = await db<Record<string, number>[]>`
    SELECT (SELECT count(*)::int FROM intent WHERE type = 'goal') AS goals,
           (SELECT count(*)::int FROM intent WHERE type = 'task') AS tasks,
           (SELECT count(*)::int FROM entity) AS entities,
           (SELECT count(*)::int FROM page) AS pages,
           (SELECT count(*)::int FROM time_block) AS blocks,
           (SELECT count(*)::int FROM observation) AS observations`;
  return { now: now.toISOString(), ms: Date.now() - started, counts: counts! };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  // Scores are computed once, at the end, at the demo clock.
  process.env.LOR_SYNC_RECOMPUTE = "off";
  try {
    const result = await loadDemo();
    console.log(JSON.stringify({ ok: true, ...result }));
  } catch (err) {
    console.error(`demo: ${err instanceof Error ? err.message : String(err)}`);
    process.exitCode = 1;
  } finally {
    await defaultSql.end({ timeout: 5 });
  }
}
