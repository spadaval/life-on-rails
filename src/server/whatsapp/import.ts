// WhatsApp messages into LoR. The phone uploads WhatsApp's nightly local
// backup, still encrypted (device.ts); this job decrypts it with the owner's
// 64-digit backup key and records each new message as an observation, text
// included, so it counts toward "last in touch" and can be searched. Each
// group becomes a group entity, with its current members.
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import path from "node:path";
import type { Sql } from "../../db/client.js";
import { uuidv7 } from "../../shared/ids.js";
import { normalizePhone } from "../../shared/phone.js";
import { env } from "../env.js";
import { recordObservation, stableId } from "../observations.js";
import { decryptCrypt15 } from "./crypt15.js";
import { readGroups, readMessages, type WaGroup, type WaMessage } from "./msgstore.js";

export const BACKUP_FILE = "msgstore.db.crypt15";
const STATE_FILE = "state.json";
/** Re-read this far behind the newest message already recorded: edits and late arrivals. */
const OVERLAP_MS = 24 * 3600_000;
const BATCH = 1000;

interface State {
  /** sha256 of the last backup imported. */
  sha256: string | null;
  /** Newest message timestamp recorded, epoch ms. */
  cursor_ms: number;
  /** The phone that uploaded the backup, recorded as each observation's device. */
  device: string | null;
  imported_at: string | null;
}

const dir = () => env.LOR_WHATSAPP_DIR;
const statePath = () => path.join(dir(), STATE_FILE);

export function readState(): State {
  try {
    return JSON.parse(readFileSync(statePath(), "utf8")) as State;
  } catch {
    return { sha256: null, cursor_ms: 0, device: null, imported_at: null };
  }
}

function writeState(s: State): void {
  mkdirSync(dir(), { recursive: true });
  writeFileSync(statePath(), JSON.stringify(s, null, 2));
}

/** Called by the upload endpoint once the new backup is in place. */
export function noteUploader(device: string): void {
  writeState({ ...readState(), device });
}

function backupHash(file: string): string {
  return createHash("sha256").update(readFileSync(file)).digest("hex");
}

/** A backup is waiting if one exists that differs from the last one imported. */
export async function whatsappPending(): Promise<boolean> {
  const file = path.join(dir(), BACKUP_FILE);
  if (!existsSync(file) || !existsSync(env.LOR_WHATSAPP_KEY_FILE)) return false;
  const s = readState();
  return s.imported_at === null || statSync(file).mtime.toISOString() > s.imported_at;
}

/** The observation one message becomes. */
export function messageObservation(m: WaMessage, device: string) {
  const phone = m.phone ? (normalizePhone(`+${m.phone}`, env.LOR_PHONE_COUNTRY_CODE) ?? `+${m.phone}`) : null;
  return {
    id: stableId("whatsapp", `${m.chatJid}\n${m.keyId}\n${m.fromMe ? 1 : 0}`),
    tStart: m.at,
    tEnd: m.at,
    device,
    source: "android" as const,
    app: "com.whatsapp",
    title: m.group ? (m.chatName ?? "WhatsApp group") : phone,
    payload: {
      kind: "whatsapp",
      direction: m.fromMe ? "out" : "in",
      phone,
      chat: m.chatJid,
      chat_name: m.chatName,
      group: m.group,
      type: m.type,
      text: m.text,
    },
  };
}

export async function importWhatsapp(db: Sql): Promise<Record<string, unknown>> {
  const file = path.join(dir(), BACKUP_FILE);
  const state = readState();
  const sha256 = backupHash(file);
  if (sha256 === state.sha256) {
    writeState({ ...state, imported_at: new Date().toISOString() });
    return { skipped: "same backup as last time" };
  }
  const key = readFileSync(env.LOR_WHATSAPP_KEY_FILE, "utf8");
  const plain = decryptCrypt15(readFileSync(file), key);
  // node:sqlite reads from a path; the decrypted copy lives only for this run.
  const dbPath = path.join(dir(), `msgstore-${process.pid}.db`);
  writeFileSync(dbPath, plain, { mode: 0o600 });
  let messages: WaMessage[];
  let groups: WaGroup[];
  try {
    messages = readMessages(dbPath, Math.max(0, state.cursor_ms - OVERLAP_MS));
    groups = readGroups(dbPath);
  } finally {
    rmSync(dbPath, { force: true });
  }
  const device = state.device ?? "phone";
  let stored = 0;
  let cursor = state.cursor_ms;
  for (let i = 0; i < messages.length; i += BATCH) {
    const batch = messages.slice(i, i + BATCH);
    await db.begin(async (tx) => {
      for (const m of batch) {
        const { inserted } = await recordObservation(tx, messageObservation(m, device));
        if (inserted) stored++;
        cursor = Math.max(cursor, m.at.getTime());
      }
    });
  }
  const g = await syncGroups(db, groups);
  writeState({ sha256, cursor_ms: cursor, device, imported_at: new Date().toISOString() });
  return { read: messages.length, stored, newest: cursor ? new Date(cursor).toISOString() : null, groups: groups.length, new_groups: g.created };
}

/**
 * One group entity per WhatsApp group, found by its chat id. WhatsApp is the
 * source of the name (a rename keeps the old one as an alias) and of who is in it.
 */
export async function syncGroups(db: Sql, groups: WaGroup[]): Promise<{ created: number }> {
  let created = 0;
  await db.begin(async (tx) => {
    for (const g of groups) {
      const jid = g.jid.toLowerCase();
      const found = (
        await tx<{ id: string; name: string; aliases: string[] }[]>`
          SELECT e.id, e.name, e.aliases FROM entity e JOIN entity_identifier i ON i.entity_id = e.id
          WHERE i.kind = 'whatsapp_group' AND i.value = ${jid}`
      )[0];
      let id: string;
      if (!found) {
        id = uuidv7();
        created++;
        await tx`INSERT INTO entity (id, kind, name) VALUES (${id}, 'group', ${g.name ?? `WhatsApp group ${jid.split("@")[0]!.slice(-6)}`})`;
        await tx`INSERT INTO entity_identifier (entity_id, kind, value) VALUES (${id}, 'whatsapp_group', ${jid})`;
      } else {
        id = found.id;
        if (g.name && g.name !== found.name) {
          const aliases = [...new Set([...found.aliases, found.name])].filter((a) => a !== g.name);
          await tx`UPDATE entity SET name = ${g.name}, aliases = ${aliases}, updated_at = now() WHERE id = ${id}`;
        }
      }
      const phones = [...new Set(g.members.map((m) => normalizePhone(`+${m}`, env.LOR_PHONE_COUNTRY_CODE) ?? `+${m}`))];
      await tx`DELETE FROM entity_member WHERE entity_id = ${id}`;
      if (phones.length) {
        await tx`INSERT INTO entity_member ${tx(phones.map((value) => ({ entity_id: id, kind: "phone", value })))} ON CONFLICT DO NOTHING`;
      }
    }
  });
  return { created };
}
