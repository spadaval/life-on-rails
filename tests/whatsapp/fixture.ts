// A small msgstore.db in WhatsApp's shape, and a crypt15 backup of it made the
// way WhatsApp makes one, for tests.
import { createCipheriv, createHash, randomBytes } from "node:crypto";
import { mkdtempSync, readFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { DatabaseSync } from "node:sqlite";
import { deflateSync } from "node:zlib";
import { backupKey } from "../../src/server/whatsapp/crypt15.js";

export const KEY = "0123456789abcdef".repeat(4);

/** Builds msgstore.db: a one-to-one chat with Priya, a group with its members, a chat known only by @lid, and noise to skip. */
export function msgstore(groupName = "Wedding planning"): string {
  const file = path.join(mkdtempSync(path.join(tmpdir(), "wa-")), "msgstore.db");
  const db = new DatabaseSync(file);
  db.exec(`
    CREATE TABLE jid (_id INTEGER PRIMARY KEY, user TEXT, server TEXT, raw_string TEXT);
    CREATE TABLE jid_map (lid_row_id INTEGER, jid_row_id INTEGER);
    CREATE TABLE chat (_id INTEGER PRIMARY KEY, jid_row_id INTEGER, subject TEXT);
    CREATE TABLE message (_id INTEGER PRIMARY KEY, chat_row_id INTEGER, from_me INTEGER, key_id TEXT,
      sender_jid_row_id INTEGER, timestamp INTEGER, message_type INTEGER, text_data TEXT);
    INSERT INTO jid VALUES (1, '14155550100', 's.whatsapp.net', '14155550100@s.whatsapp.net'),
                           (2, '120363000000000000', 'g.us', '120363000000000000@g.us'),
                           (3, '919876543210', 's.whatsapp.net', '919876543210@s.whatsapp.net'),
                           (4, '987654321012345', 'lid', '987654321012345@lid'),
                           (5, '14155550111', 's.whatsapp.net', '14155550111@s.whatsapp.net'),
                           (6, 'status', 'broadcast', 'status@broadcast');
    CREATE TABLE group_participant_user (_id INTEGER PRIMARY KEY, group_jid_row_id INTEGER, user_jid_row_id INTEGER, pending INTEGER);
    INSERT INTO jid_map VALUES (4, 5);
    -- The group: Raj, someone known only by @lid, and an invite still pending.
    INSERT INTO group_participant_user VALUES (1, 2, 3, 0), (2, 2, 4, 0), (3, 2, 1, 1);
    INSERT INTO chat VALUES (1, 1, NULL), (2, 2, '${groupName}'), (3, 4, NULL), (4, 6, NULL);
    INSERT INTO message VALUES
      (1, 1, 0, 'A1', 0, 1760000000000, 0, 'Did you send the TDS form?'),
      (2, 1, 1, 'A2', 0, 1760000060000, 0, 'Yes, just now'),
      (3, 1, 0, 'A3', 0, 1760000120000, 1, NULL),
      (4, 2, 0, 'G1', 3, 1760000180000, 0, 'Venue shortlist is up'),
      (5, 2, 1, 'G2', 0, 1760000240000, 0, 'Looking now'),
      (6, 2, 0, 'G3', 0, 1760000250000, 7, NULL),
      (7, 3, 0, 'L1', 0, 1760000300000, 0, 'hi from a lid chat'),
      (8, 4, 0, 'S1', 5, 1760000360000, 0, 'a status update');
  `);
  db.close();
  return file;
}

/** WhatsApp's crypt15, as wa-crypt-tools writes it: varint header size, protobuf header with the IV, GCM ciphertext, tag, MD5. */
export function crypt15(dbFile: string, key64 = KEY): Buffer {
  const iv = randomBytes(16);
  // key_type 1, then field 3 { field 1: IV }, then field 4 padded out with an app version so the
  // header is longer than 127 bytes and its size takes two varint bytes, as real backups' do.
  const version = Buffer.from("2.26.18.75 ".repeat(12));
  const header = Buffer.concat([Buffer.from([0x08, 0x01, 0x1a, 0x12, 0x0a, 0x10]), iv, Buffer.from([0x22, version.length + 2, 0x0a, version.length]), version]);
  const cipher = createCipheriv("aes-256-gcm", backupKey(key64), iv);
  const ciphertext = Buffer.concat([cipher.update(deflateSync(readFileSync(dbFile))), cipher.final()]);
  const size = header.length < 128 ? Buffer.from([header.length]) : Buffer.from([(header.length & 0x7f) | 0x80, header.length >> 7]);
  const body = Buffer.concat([size, header, ciphertext, cipher.getAuthTag()]);
  return Buffer.concat([body, createHash("md5").update(body).digest()]);
}
