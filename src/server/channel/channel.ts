// The owner's channel to the agent. LoR carries it; the conversation lives in
// one T3 thread. Every message, either way, is an observation (kind `channel`),
// which is also the history the agent searches when compaction has taken it.
import { execFile } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import type { Sql } from "../../db/client.js";
import { localHm, localYmd } from "../../shared/localTime.js";
import { type SentEarlier, splitMessage, wrapInbound } from "../../shared/channel.js";
import { env } from "../env.js";
import { Refused } from "../errors.js";
import { errorMessage, log } from "../log.js";
import { recordObservation, stableId } from "../observations.js";
import { type ChannelPort, type InboundMessage, type Outbound, whatsappChannel } from "./client.js";

const OFFSET = "channel_offset";
const RECIPIENT = "channel_recipient";
const POLL_TIMEOUT_S = 25;
/** The API allows 15 polls a minute. */
const MIN_POLL_MS = 4_000;

export function channelConfigured(): boolean {
  return env.LOR_CHANNEL_KEY !== "" && env.LOR_CHANNEL_THREAD !== "";
}

/** Passes text to a T3 thread. A busy thread gets it as its next turn. */
export type Handoff = (text: string) => Promise<void>;

export const t3Handoff: Handoff = (text) =>
  new Promise((resolve, reject) => {
    const child = execFile(env.LOR_T3_BIN, ["thread", "send", env.LOR_CHANNEL_THREAD, "-"], { timeout: 60_000 }, (err, _out, stderr) =>
      err ? reject(new Error(`t3 thread send: ${stderr.trim() || err.message}`)) : resolve(),
    );
    child.stdin?.end(text);
  });

/** Starts a thread in the assistant project and returns its id. */
export function t3StartThread(title: string, prompt: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const child = execFile(
      env.LOR_T3_BIN,
      ["thread", "start", env.LOR_ASSISTANT_DIR, "-", "--title", title, "--json"],
      { timeout: 60_000 },
      (err, out, stderr) => {
        if (err) return reject(new Error(`t3 thread start: ${stderr.trim() || err.message}`));
        resolve((JSON.parse(out) as { threadId: string }).threadId);
      },
    );
    child.stdin?.end(prompt);
  });
}

async function cursor(db: Sql, name: string): Promise<string | null> {
  const rows = await db<{ token: string | null }[]>`SELECT token FROM sync_cursor WHERE name = ${name}`;
  return rows[0]?.token ?? null;
}

async function setCursor(db: Sql, name: string, token: string): Promise<void> {
  await db`INSERT INTO sync_cursor (name, token, updated_at) VALUES (${name}, ${token}, now())
    ON CONFLICT (name) DO UPDATE SET token = EXCLUDED.token, updated_at = now()`;
}

function stamp(at: Date): string {
  return `${localYmd(env.LOR_TZ, at)} ${localHm(env.LOR_TZ, at)}`;
}

/** What other runs sent him since he last spoke. The thread never saw these, and "yes" means nothing without them. */
async function sentSinceHisLast(db: Sql): Promise<SentEarlier[]> {
  const rows = await db<{ t_start: Date; text: string; thread: string | null; intent_id: string | null }[]>`
    SELECT t_start, payload->>'text' AS text, payload->>'thread' AS thread, payload->>'intent_id' AS intent_id
    FROM observation
    WHERE kind = 'channel' AND payload->>'direction' = 'out'
      AND coalesce(payload->>'thread', '') <> ${env.LOR_CHANNEL_THREAD}
      AND t_start > coalesce((SELECT max(t_start) FROM observation WHERE kind = 'channel' AND payload->>'direction' = 'in'), '-infinity')
    ORDER BY t_start`;
  return rows.map((r) => ({ at: localHm(env.LOR_TZ, r.t_start), from: r.thread ?? "LoR", text: r.text, intentId: r.intent_id }));
}

const EXTENSIONS: Record<string, string> = {
  "audio/ogg": "ogg", "audio/mpeg": "mp3", "audio/mp4": "m4a", "audio/aac": "aac", "image/jpeg": "jpg", "image/png": "png",
  "image/webp": "webp", "video/mp4": "mp4", "application/pdf": "pdf",
};

async function saveMedia(port: ChannelPort, m: InboundMessage): Promise<{ kind: string; path: string; voice: boolean } | null> {
  const media = m.audio ?? m.image ?? m.video ?? m.document ?? m.sticker;
  if (!media) return null;
  const bytes = await port.download(media.id);
  mkdirSync(env.LOR_CHANNEL_DIR, { recursive: true, mode: 0o700 });
  const name = m.document?.filename ? path.basename(m.document.filename) : `${media.id}.${EXTENSIONS[media.mime_type] ?? "bin"}`;
  const file = path.join(env.LOR_CHANNEL_DIR, `${stableId("channel", m.id).slice(0, 8)}-${name}`);
  writeFileSync(file, bytes, { mode: 0o600 });
  return { kind: m.type, path: file, voice: m.audio?.voice === true };
}

function bodyOf(m: InboundMessage): string {
  if (m.text) return m.text.body;
  if (m.reaction) return m.reaction.emoji ? `(reacted ${m.reaction.emoji} to ${m.reaction.message_id})` : "";
  return (m.image ?? m.video ?? m.document)?.caption ?? "";
}

/**
 * One message from him, at least once: hand over, then record. A crash in
 * between hands it over twice, which is harmless; his message is never lost.
 */
export async function receive(db: Sql, port: ChannelPort, handoff: Handoff, m: InboundMessage): Promise<boolean> {
  const id = stableId("channel", m.id);
  await setCursor(db, RECIPIENT, m.from);
  const seen = await db`SELECT 1 FROM observation WHERE id = ${id}`;
  if (seen.length > 0) return false;
  const text = bodyOf(m);
  // A removed reaction says nothing.
  if (!text && m.type === "reaction") return false;
  await port.markRead(m.id).catch((err: unknown) => log("warn", "channel: mark read failed", { err: errorMessage(err) }));
  const file = await saveMedia(port, m);
  const at = new Date(Number(m.timestamp) * 1000);
  await handoff(wrapInbound({ at: stamp(at), via: "WhatsApp", text, file, sent: await sentSinceHisLast(db) }));
  await recordObservation(db, {
    id,
    tStart: at,
    tEnd: at,
    device: "whatsapp",
    source: "channel",
    title: "Message from Sumeet",
    payload: { kind: "channel", direction: "in", text, wamid: m.id, type: m.type, file: file?.path ?? null },
  });
  return true;
}

/** He asks from the dashboard. The reply comes to WhatsApp, like any other. */
export async function askFromDashboard(db: Sql, handoff: Handoff, text: string, about?: string | null): Promise<{ id: string }> {
  const at = new Date();
  await handoff(wrapInbound({ at: stamp(at), via: "the dashboard", text, about, sent: await sentSinceHisLast(db) }));
  return recordObservation(db, {
    tStart: at,
    tEnd: at,
    device: "dashboard",
    source: "channel",
    title: "Message from Sumeet",
    payload: { kind: "channel", direction: "in", text, about: about ?? null, type: "text" },
  });
}

const MIME: Record<string, string> = {
  ".pdf": "application/pdf", ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".png": "image/png", ".mp4": "video/mp4",
  ".m4a": "audio/mp4", ".mp3": "audio/mpeg", ".ogg": "audio/ogg", ".txt": "text/plain", ".md": "text/plain",
  ".csv": "text/csv", ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
};

function attachmentType(mime: string): "image" | "audio" | "video" | "document" {
  if (mime === "image/jpeg" || mime === "image/png") return "image";
  if (mime.startsWith("audio/")) return "audio";
  if (mime.startsWith("video/")) return "video";
  return "document";
}

// The API does not keep the order of concurrent sends to one person.
let sending: Promise<unknown> = Promise.resolve();
function oneAtATime<T>(fn: () => Promise<T>): Promise<T> {
  const next = sending.then(fn, fn);
  sending = next.catch(() => {});
  return next;
}

export interface SendInput {
  text: string;
  thread: string;
  intentId?: string | null;
  /** A file on this box. */
  attach?: string | null;
}

/** The only way anything reaches him. An unclear failure is not retried: it may already have gone out. */
export function sendMessage(db: Sql, port: ChannelPort, input: SendInput): Promise<{ id: string; parts: number }> {
  return oneAtATime(async () => {
    const to = await cursor(db, RECIPIENT);
    if (!channelConfigured()) throw new Refused("channel-off", "the channel is not set up: LOR_CHANNEL_KEY and LOR_CHANNEL_THREAD");
    if (!to) throw new Refused("channel-no-recipient", "WhatsApp only lets the agent reply: Sumeet has to message it once first");
    const messages: Outbound[] = [];
    if (input.attach) {
      const mime = MIME[path.extname(input.attach).toLowerCase()] ?? "application/octet-stream";
      const filename = path.basename(input.attach);
      const mediaId = await port.upload(readFileSync(input.attach), mime, filename);
      const type = attachmentType(mime);
      messages.push({ type, mediaId, ...(type === "document" ? { filename } : {}) });
    }
    for (const text of splitMessage(input.text)) messages.push({ type: "text", text });
    const wamids: string[] = [];
    for (const m of messages) wamids.push(await port.send(to, m));
    const now = new Date();
    const { id } = await recordObservation(db, {
      tStart: now,
      tEnd: now,
      device: "whatsapp",
      source: "channel",
      title: "Message to Sumeet",
      payload: {
        kind: "channel", direction: "out", text: input.text, wamid: wamids[0] ?? null, thread: input.thread,
        intent_id: input.intentId ?? null, file: input.attach ?? null,
      },
    });
    return { id, parts: messages.length };
  });
}

// ---- flags ----

/** Flagged tasks not yet sent to him. */
export async function pendingFlags(db: Sql): Promise<{ id: string; title: string; note: string }[]> {
  const rows = await db<{ id: string; title: string; note: string }[]>`
    SELECT id, title, flag_note AS note FROM intent
    WHERE flagged_at IS NOT NULL AND notified_at IS NULL AND status IN ('active', 'waiting') ORDER BY flagged_at`;
  return [...rows];
}

export async function sendFlags(db: Sql, port: ChannelPort): Promise<{ sent: number }> {
  const flags = await pendingFlags(db);
  for (const f of flags) {
    await sendMessage(db, port, { text: `Needs you: ${f.title}\n${f.note}`, thread: "lor:flag", intentId: f.id });
    await db`UPDATE intent SET notified_at = now() WHERE id = ${f.id}`;
  }
  return { sent: flags.length };
}

// ---- history ----

export interface ChannelMessage {
  id: string;
  t: string;
  direction: "in" | "out";
  text: string;
  thread: string | null;
  file: string | null;
}

interface HistoryRow {
  id: string;
  t_start: Date;
  direction: "in" | "out";
  text: string;
  thread: string | null;
  file: string | null;
}

const toMessage = (r: HistoryRow): ChannelMessage => ({
  id: r.id, t: r.t_start.toISOString(), direction: r.direction, text: r.text, thread: r.thread, file: r.file,
});

/** Newest last, so it reads as a conversation. */
export async function messageHistory(
  db: Sql,
  q: { since?: Date | null; search?: string | null; around?: string | null; limit?: number },
): Promise<ChannelMessage[]> {
  const limit = Math.min(q.limit ?? 20, 200);
  const columns = db`id, t_start, payload->>'direction' AS direction, payload->>'text' AS text,
    payload->>'thread' AS thread, payload->>'file' AS file`;
  if (q.around) {
    const half = Math.ceil(limit / 2);
    const rows = await db<HistoryRow[]>`
      (SELECT ${columns} FROM observation WHERE kind = 'channel'
        AND t_start <= (SELECT t_start FROM observation WHERE id = ${q.around}) ORDER BY t_start DESC LIMIT ${half + 1})
      UNION ALL
      (SELECT ${columns} FROM observation WHERE kind = 'channel'
        AND t_start > (SELECT t_start FROM observation WHERE id = ${q.around}) ORDER BY t_start LIMIT ${half})
      ORDER BY t_start`;
    return rows.map(toMessage);
  }
  const rows = await db<HistoryRow[]>`
    SELECT ${columns} FROM observation
    WHERE kind = 'channel'
      ${q.since ? db`AND t_start >= ${q.since}` : db``}
      ${q.search ? db`AND search @@ websearch_to_tsquery('english', ${q.search})` : db``}
    ORDER BY t_start DESC LIMIT ${limit}`;
  return rows.reverse().map(toMessage);
}

// ---- the loop ----

let lastPoll = 0;

/** When the channel last heard back from WhatsApp; 0 until it has. */
export function lastChannelPoll(): number {
  return lastPoll;
}

/** One poll: every message handed over before the position moves. */
export async function pollOnce(db: Sql, port: ChannelPort, handoff: Handoff, timeoutS = POLL_TIMEOUT_S): Promise<number> {
  // From the start, never from "now": an offset-less poll can miss what arrives between polls, and receive() skips what it has seen.
  const offset = Number((await cursor(db, OFFSET)) ?? 0);
  const updates = await port.updates(offset, timeoutS);
  if (!updates) return 0;
  let handled = 0;
  for (const m of updates.messages) if (await receive(db, port, handoff, m)) handled++;
  await setCursor(db, OFFSET, String(updates.nextOffset));
  return handled;
}

/** Runs for the life of the process. Long-polling, so it is not a scheduler job. */
export async function runChannelLoop(db: Sql): Promise<void> {
  let lastError = "";
  for (;;) {
    const started = Date.now();
    try {
      await pollOnce(db, whatsappChannel, t3Handoff);
      lastPoll = Date.now();
      lastError = "";
    } catch (err) {
      const message = errorMessage(err);
      if (message !== lastError) log("error", "channel poll failed", { err: message });
      lastError = message;
      await new Promise((r) => setTimeout(r, 30_000));
    }
    await new Promise((r) => setTimeout(r, Math.max(0, MIN_POLL_MS - (Date.now() - started))));
  }
}
