// Reading WhatsApp's message database (msgstore.db, decrypted from a backup).
// The schema is WhatsApp's and changes between versions; this reads the
// tables it has had since 2022: message, chat, jid, and jid_map for the
// newer @lid addresses.
import { DatabaseSync } from "node:sqlite";

export interface WaMessage {
  /** WhatsApp's own id for the message, unique within its chat. */
  keyId: string;
  chatJid: string;
  /** The group's name; null for a one-to-one chat. */
  chatName: string | null;
  group: boolean;
  fromMe: boolean;
  /** The other person's number in a one-to-one chat, or who wrote a group message; digits only. */
  phone: string | null;
  at: Date;
  text: string | null;
  type: string;
}

/** WhatsApp's message_type numbers, as words. Anything else is "other". */
const TYPES: Record<number, string> = {
  0: "text",
  1: "image",
  2: "audio",
  3: "video",
  4: "contact",
  5: "location",
  9: "document",
  13: "gif",
  15: "deleted",
  16: "live location",
  20: "sticker",
  42: "view once image",
  43: "view once video",
  46: "poll",
  64: "event",
};
const SYSTEM = 7;

interface Row {
  key_id: string;
  from_me: number;
  timestamp: number;
  message_type: number;
  text_data: string | null;
  chat_user: string | null;
  chat_server: string | null;
  chat_raw: string | null;
  chat_phone: string | null;
  subject: string | null;
  sender_user: string | null;
  sender_server: string | null;
  sender_phone: string | null;
}

function tableNames(db: DatabaseSync): Set<string> {
  const rows = db.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").all() as { name: string }[];
  return new Set(rows.map((r) => r.name));
}

/** A jid's phone number: the user part of a phone jid, or what an @lid maps to. */
function phoneOf(user: string | null, server: string | null, mapped: string | null): string | null {
  if (server === "s.whatsapp.net" && user && /^\d{6,15}$/.test(user)) return user;
  if (mapped && /^\d{6,15}$/.test(mapped)) return mapped;
  return null;
}

/** Messages newer than `sinceMs` (epoch ms), oldest first. System notices and status updates are left out. */
export function readMessages(dbPath: string, sinceMs = 0): WaMessage[] {
  const db = new DatabaseSync(dbPath, { readOnly: true });
  try {
    const tables = tableNames(db);
    for (const t of ["message", "chat", "jid"]) {
      if (!tables.has(t)) throw new Error(`msgstore.db has no ${t} table: WhatsApp changed its schema`);
    }
    const lid = tables.has("jid_map");
    // Map an @lid jid row to the phone jid's user part, when WhatsApp knows it.
    const mapped = (alias: string) =>
      lid
        ? `(SELECT pj.user FROM jid_map m JOIN jid pj ON pj._id = m.jid_row_id WHERE m.lid_row_id = ${alias}._id LIMIT 1)`
        : "NULL";
    const rows = db
      .prepare(
        `SELECT m.key_id, m.from_me, m.timestamp, m.message_type, m.text_data,
                cj.user AS chat_user, cj.server AS chat_server, (cj.user || '@' || cj.server) AS chat_raw, ${mapped("cj")} AS chat_phone,
                c.subject,
                sj.user AS sender_user, sj.server AS sender_server, ${mapped("sj")} AS sender_phone
         FROM message m
         JOIN chat c ON c._id = m.chat_row_id
         JOIN jid cj ON cj._id = c.jid_row_id
         LEFT JOIN jid sj ON sj._id = m.sender_jid_row_id
         WHERE m.timestamp > ? AND m.key_id IS NOT NULL AND m.message_type <> ${SYSTEM}
           AND cj.server NOT IN ('broadcast', 'status')
         ORDER BY m.timestamp, m._id`,
      )
      .all(sinceMs) as unknown as Row[];
    return rows.map((r) => {
      const group = r.chat_server === "g.us";
      const fromMe = r.from_me === 1;
      const phone = group
        ? fromMe
          ? null
          : phoneOf(r.sender_user, r.sender_server, r.sender_phone)
        : phoneOf(r.chat_user, r.chat_server, r.chat_phone);
      return {
        keyId: r.key_id,
        chatJid: r.chat_raw ?? `${r.chat_user}@${r.chat_server}`,
        chatName: group ? r.subject : null,
        group,
        fromMe,
        phone,
        at: new Date(Number(r.timestamp)),
        text: r.text_data && r.text_data.length > 0 ? r.text_data : null,
        type: TYPES[r.message_type] ?? "other",
      };
    });
  } finally {
    db.close();
  }
}
