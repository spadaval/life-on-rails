// What the owner's phone talks to: its sensors upload observations here, and
// the timer tile reads and changes the running block. Served on both
// listeners; on the one behind `tailscale serve`, every request has already
// been checked for the owner's Tailscale identity.
import { createWriteStream, mkdirSync, renameSync, rmSync } from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";
import type { ReadableStream as WebReadableStream } from "node:stream/web";
import { z } from "zod";
import { sql } from "../db/client.js";
import { systemIntentId } from "../db/seed.js";
import { ObservationEnvelope } from "../shared/observation.js";
import { normalizePhone } from "../shared/phone.js";
import { fillWindow, pauseRunningBlock, resumeRunningBlock, startManualBlock, stopRunningBlock } from "./attribution/blocks.js";
import { lockBlocks, syncFill } from "./attribution/fill.js";
import { env } from "./env.js";
import { BadInput } from "./errors.js";
import { recordObservation } from "./observations.js";
import { BACKUP_FILE, noteUploader } from "./whatsapp/import.js";

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
    const { from, to } = fillWindow(new Date(), new Date(evidenceFrom), new Date(evidenceTo));
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
