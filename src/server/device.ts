// What the owner's phone talks to: its sensors upload observations here, the
// timer tile reads and changes the running block, and the launcher's agent
// page reads the day. Served on both listeners; on the one behind `tailscale
// serve`, every request has already been checked for the owner's Tailscale
// identity.
import { createWriteStream, mkdirSync, renameSync, rmSync } from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import type { ReadableStream as WebReadableStream } from "node:stream/web";
import { z } from "zod";
import { sql } from "../db/client.js";
import { systemIntentId } from "../db/seed.js";
import { ObservationEnvelope } from "../shared/observation.js";
import { localYmd } from "../shared/localTime.js";
import { normalizePhone } from "../shared/phone.js";
import { fillWindow, pauseRunningBlock, resumeRunningBlock, startManualBlock, stopRunningBlock } from "./attribution/blocks.js";
import { LOOKBACK_MS, lockBlocks, syncFill } from "./attribution/fill.js";
import { env } from "./env.js";
import { type TaskRow, getGraph, openTasks } from "./graph.js";
import { needsOwner } from "./intents.js";
import { type Channel, listPeople } from "./people.js";
import { getToday } from "./today.js";
import { briefLine, oneFrom, runningName } from "./trmnl/device.js";
import { getPin } from "./trmnl/pin.js";
import { BadInput } from "./errors.js";
import { recordObservation } from "./observations.js";
import { BACKUP_FILE, noteUploader } from "./whatsapp/import.js";
import { type Place, type Weather, type WeatherCache, weatherCache } from "./weather.js";

/** Sources a device may send. Calendar, mail, and the agent's log have their own writers. */
const DEVICE_SOURCES = new Set(["android", "linux_activity", "windows_activity"]);
const MAX_BATCH = 1000;

const IngestBody = z.object({
  device: z.string().trim().min(1).max(64),
  observations: z.array(z.unknown()).max(MAX_BATCH),
});

export interface IngestResult {
  received: number;
  stored: number;
  rejected: { id: string | null; error: string }[];
}

export async function ingest(body: unknown): Promise<IngestResult> {
  const parsed = IngestBody.safeParse(body);
  if (!parsed.success) throw new BadInput(`expected {device, observations[]} with at most ${MAX_BATCH} observations`);
  const { device, observations } = parsed.data;
  const result: IngestResult = { received: observations.length, stored: 0, rejected: [] };
  // App sessions and location fixes are evidence for the actual lane: re-sync fill over the time they cover.
  let evidenceFrom = Infinity;
  let evidenceTo = -Infinity;
  await sql.begin(async (tx) => {
    for (const raw of observations) {
      const o = ObservationEnvelope.safeParse(raw);
      const id = typeof (raw as { id?: unknown })?.id === "string" ? (raw as { id: string }).id : null;
      if (!o.success) {
        result.rejected.push({ id, error: o.error.issues.map((i) => `${i.path.join(".")}: ${i.message}`).join("; ") });
        continue;
      }
      const e = o.data;
      if (!DEVICE_SOURCES.has(e.source)) {
        result.rejected.push({ id, error: `a device cannot send source ${e.source}` });
        continue;
      }
      if (e.device !== device) {
        result.rejected.push({ id, error: `device ${e.device} in a batch from ${device}` });
        continue;
      }
      const payload = { ...e.payload };
      // Numbers are stored in one form, the same as contacts', so calls and texts match people.
      if (typeof payload.phone === "string") payload.phone = normalizePhone(payload.phone, env.LOR_PHONE_COUNTRY_CODE) ?? payload.phone;
      const { inserted } = await recordObservation(tx, {
        id: e.id,
        tStart: new Date(e.t_start),
        tEnd: e.t_end ? new Date(e.t_end) : null,
        device: e.device,
        source: e.source,
        app: e.app,
        calendarId: e.calendar_id,
        title: e.title,
        url: e.url,
        payload,
        intentHint: e.intent_hint,
      });
      if (inserted) result.stored++;
      if (payload.kind === "app" || payload.kind === "location") {
        evidenceFrom = Math.min(evidenceFrom, Date.parse(e.t_start));
        evidenceTo = Math.max(evidenceTo, Date.parse(e.t_end ?? e.t_start));
      }
    }
  });
  if (evidenceFrom <= evidenceTo) {
    // New evidence can change a block that starts a little outside it (a stay is padded before its first fix; a
    // late fix can split a stay). Re-sync as far either side as the fill reads evidence.
    const { from, to } = fillWindow(new Date(), new Date(evidenceFrom - LOOKBACK_MS), new Date(evidenceTo + LOOKBACK_MS));
    await sql.begin(async (tx) => {
      await lockBlocks(tx);
      await syncFill(tx, from, to);
    });
  }
  return result;
}

export interface DeviceState {
  /** Who the request came from: the Tailscale login, or "this box" on loopback. */
  login: string;
  now: string;
  timezone: string;
  running: { id: string; intent: string | null; title: string | null; t_start: string; paused: boolean; paused_at: string | null } | null;
  recent: { id: string; title: string }[];
  intents: { id: string; title: string; type: string }[];
  last_upload: string | null;
}

export async function deviceState(login: string, device: string | null): Promise<DeviceState> {
  const running = await sql<{ id: string; intent: string | null; title: string | null; t_start: Date; paused_at: Date | null }[]>`
    SELECT b.id, i.title AS intent, b.title, b.t_start, b.paused_at
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id WHERE b.t_end IS NULL LIMIT 1`;
  const recent = await sql<{ id: string; title: string }[]>`
    SELECT i.id, i.title FROM intent i
    JOIN (SELECT intent_id, max(t_start) AS last FROM time_block WHERE origin <> 'calendar' OR committed GROUP BY intent_id) b ON b.intent_id = i.id
    WHERE NOT i.system AND i.status IN ('active', 'waiting') ORDER BY b.last DESC LIMIT 6`;
  const intents = await sql<{ id: string; title: string; type: string }[]>`
    SELECT id, title, type::text AS type FROM intent
    WHERE NOT system AND status IN ('active', 'waiting') AND type <> 'metric' ORDER BY type, lower(title)`;
  const last = device
    ? await sql<{ t: Date | null }[]>`SELECT max(ingested_at) AS t FROM observation WHERE device = ${device}`
    : [{ t: null }];
  const r = running[0];
  return {
    login,
    now: new Date().toISOString(),
    timezone: env.LOR_TZ,
    running: r
      ? { id: r.id, intent: r.intent, title: r.title, t_start: r.t_start.toISOString(), paused: r.paused_at !== null, paused_at: r.paused_at?.toISOString() ?? null }
      : null,
    recent: recent.map((x) => ({ id: x.id, title: x.title })),
    intents: intents.map((x) => ({ id: x.id, title: x.title, type: x.type })),
    last_upload: last[0]?.t?.toISOString() ?? null,
  };
}

/** How many of today's timed events the agent page gets. */
const HOME_EVENTS = 5;

export interface DeviceHome {
  now: string;
  timezone: string;
  /** The one thing, as the wall picks it (pin, timer, event on or soon, brief, oldest flag); null for none. */
  one: { kicker: string; text: string; why: string | null } | null;
  /** The first line of today's brief. */
  brief: string | null;
  /** Flagged tasks, oldest flag first. */
  needs_you: { id: string; title: string; note: string | null; flagged_at: string }[];
  day: {
    wake_start: string;
    wake_end: string;
    remaining_s: number;
    remaining_fraction: number;
    running: { title: string; start: string; paused: boolean; paused_at: string | null } | null;
    /** Timed events today not yet over (the one under way included), in order, at most HOME_EVENTS. */
    events: { title: string; start: string; end: string }[];
  };
  /** The agent's work queue: open tasks given to the agent, queued ones first (oldest first), then the rest (newest change first). */
  agent_queue: { id: string; title: string; queued_at: string | null; thread: string | null }[];
  /** Current conditions where the phone last was (Celsius); null without a key, a location, or after a failed fetch. */
  weather: Weather | null;
}

/** How many of the agent's tasks /device/home carries. */
const AGENT_QUEUE = 10;

async function agentQueue(): Promise<DeviceHome["agent_queue"]> {
  const rows = await sql<{ id: string; title: string; queued_at: Date | null; thread_ref: string | null }[]>`
    SELECT id, title, queued_at, thread_ref FROM intent
    WHERE type = 'task' AND assignee = 'agent' AND status IN ('active', 'waiting')
    ORDER BY queued_at IS NULL, queued_at, updated_at DESC
    LIMIT ${AGENT_QUEUE}`;
  return rows.map((r) => ({ id: r.id, title: r.title, queued_at: r.queued_at?.toISOString() ?? null, thread: r.thread_ref }));
}

/** Where the phone last was: its latest `location` observation, at any age. */
async function lastPlace(): Promise<Place | null> {
  const [row] = await sql<Place[]>`
    SELECT (payload->>'lat')::float8 AS lat, (payload->>'lon')::float8 AS lon
    FROM observation WHERE kind = 'location' ORDER BY t_start DESC LIMIT 1`;
  return row ?? null;
}

const weather = weatherCache({ key: () => env.GOOGLE_MAPS_KEY, fetch: (...a) => globalThis.fetch(...a), now: Date.now, log: console.log });

/** What the launcher's agent page shows from LoR, in one read. */
export async function deviceHome(now = new Date(), weatherNow: WeatherCache = weather): Promise<DeviceHome> {
  const date = localYmd(env.LOR_TZ, now);
  const [today, needs, pin, brief, queue, conditions] = await Promise.all([
    getToday(sql, date, now), needsOwner(sql), getPin(sql), briefLine(sql, date), agentQueue(), weatherNow.read(lastPlace),
  ]);
  const r = today.running;
  return {
    now: now.toISOString(),
    timezone: env.LOR_TZ,
    one: oneFrom(now, { today, needs, pin, brief }),
    brief,
    needs_you: needs.map((n) => ({ id: n.id, title: n.title, note: n.flag_note, flagged_at: n.flagged_at! })),
    day: {
      wake_start: today.wake_start,
      wake_end: today.wake_end,
      remaining_s: today.remaining_s,
      remaining_fraction: today.remaining_fraction,
      running: r ? { title: runningName(r), start: r.t_start, paused: r.paused_at !== null, paused_at: r.paused_at } : null,
      events: today.planned
        .filter((e) => !e.all_day && Date.parse(e.t_end) > now.getTime())
        .slice(0, HOME_EVENTS)
        .map((e) => ({ title: e.title ?? "(no title)", start: e.t_start, end: e.t_end })),
    },
    agent_queue: queue,
    weather: conditions,
  };
}

/** How many open tasks /device/tasks lists. */
const TASKS = 100;

export interface DeviceTasks {
  now: string;
  timezone: string;
  today: string;
  /** Every open task, including those past the cap. */
  open: number;
  /** The open tasks as the web's Today lists them in Due soon, at most TASKS. */
  tasks: TaskRow[];
}

/** The phone's Tasks: the open tasks, from the same function as the web's Today. */
export async function deviceTasks(now = new Date()): Promise<DeviceTasks> {
  const all = openTasks((await getGraph(sql, now)).nodes);
  return { now: now.toISOString(), timezone: env.LOR_TZ, today: localYmd(env.LOR_TZ, now), open: all.length, tasks: all.slice(0, TASKS) };
}

/** How many people each list of /device/people carries, and how far ahead a birthday counts. */
const PEOPLE = 30;
const BIRTHDAY_WINDOW = 30;

export interface DevicePerson {
  id: string;
  name: string;
  kind: string;
  last_at: string | null;
  last_via: Channel | null;
  overdue_by: number | null;
  keep_in_touch_days: number | null;
  birthday_in: number | null;
  turning: number | null;
}

export interface DevicePeople {
  today: string;
  /** Past their keep-in-touch cadence, most overdue first. */
  overdue: DevicePerson[];
  /** A birthday within BIRTHDAY_WINDOW days, soonest first. */
  birthdays: DevicePerson[];
}

/** The phone's People: who to be in touch with, by the web's People filters. No addresses or numbers. */
export async function devicePeople(now = new Date()): Promise<DevicePeople> {
  const [overdue, birthdays] = await Promise.all([
    listPeople(sql, { filter: "overdue", limit: PEOPLE, now }),
    listPeople(sql, { filter: "birthdays", birthdayWindow: BIRTHDAY_WINDOW, limit: PEOPLE, now }),
  ]);
  const person = (p: (typeof overdue.people)[number]): DevicePerson => ({
    id: p.id, name: p.name, kind: p.kind, last_at: p.last.at, last_via: p.last.via, overdue_by: p.overdue_by,
    keep_in_touch_days: p.keep_in_touch_days, birthday_in: p.birthday_in, turning: p.turning,
  });
  return { today: localYmd(env.LOR_TZ, now), overdue: overdue.people.map(person), birthdays: birthdays.people.map(person) };
}

const TimerBody = z.object({
  action: z.enum(["start", "stop", "pause", "resume", "toggle"]),
  intent_id: z.string().uuid().nullish(),
  title: z.string().max(200).nullish(),
  device: z.string().nullish(),
});

export async function deviceTimer(body: unknown, login: string): Promise<DeviceState> {
  const d = TimerBody.parse(body);
  const who = { via: "ui" as const, sourceRef: d.device ?? null };
  let action = d.action;
  if (action === "toggle") {
    const open = await sql`SELECT 1 FROM time_block WHERE t_end IS NULL`;
    action = open.length > 0 ? "stop" : "start";
  }
  if (action === "start") await startManualBlock(sql, { intentId: d.intent_id ?? (await systemIntentId(sql, "Uncategorized")), title: d.title }, who);
  else if (action === "stop") await stopRunningBlock(sql, who);
  else if (action === "pause") await pauseRunningBlock(sql);
  else await resumeRunningBlock(sql, who);
  return deviceState(login, d.device ?? null);
}

/**
 * The phone's copy of WhatsApp's nightly backup, still encrypted. Replaces the
 * last one (only the newest is kept); the whatsapp_import job reads it.
 */
export async function receiveWhatsappBackup(body: ReadableStream<Uint8Array> | null, device: string | null): Promise<{ bytes: number }> {
  if (!body) throw new BadInput("expected the backup file as the request body");
  if (!device || !/^[a-z0-9-]{1,64}$/.test(device)) throw new BadInput("expected ?device=<the phone's name>");
  mkdirSync(env.LOR_WHATSAPP_DIR, { recursive: true });
  const final = path.join(env.LOR_WHATSAPP_DIR, BACKUP_FILE);
  const partial = `${final}.partial`;
  let bytes = 0;
  const counted = Readable.fromWeb(body as WebReadableStream<Uint8Array>).on("data", (c: Buffer) => (bytes += c.length));
  try {
    await pipeline(counted, createWriteStream(partial, { mode: 0o600 }));
  } catch (err) {
    rmSync(partial, { force: true });
    throw err;
  }
  if (bytes < 64) {
    rmSync(partial, { force: true });
    throw new BadInput("the upload is too small to be a WhatsApp backup");
  }
  renameSync(partial, final);
  noteUploader(device);
  return { bytes };
}
