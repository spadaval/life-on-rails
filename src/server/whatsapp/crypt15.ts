// WhatsApp's encrypted backup format (msgstore.db.crypt15), for a backup made
// with end-to-end encryption and the 64-digit key. Pure: bytes in, bytes out.
//
// Layout (as wa-crypt-tools reads and writes it): a varint giving the size of
// a protobuf header (BackupPrefix; the IV is field 3, field 1), the header,
// then AES-256-GCM ciphertext of a zlib stream of the SQLite file, then the
// 16-byte GCM tag and, unless the backup was split into several files, a
// 16-byte MD5 of everything before it. The AES key is HKDF-SHA256 of the
// 64-digit key with a zero salt and info "backup encryption".
import { createDecipheriv, hkdfSync } from "node:crypto";
import { inflateSync } from "node:zlib";

const SQLITE_MAGIC = Buffer.from("SQLite format 3\0", "latin1");

/** The 64-digit key as WhatsApp shows it (hex, spaces allowed) to the AES key. */
export function backupKey(key64: string): Buffer {
  const hex = key64.replace(/[\s-]/g, "").toLowerCase();
  if (!/^[0-9a-f]{64}$/.test(hex)) throw new Error("the WhatsApp backup key must be 64 hexadecimal digits");
  return Buffer.from(hkdfSync("sha256", Buffer.from(hex, "hex"), Buffer.alloc(32), Buffer.from("backup encryption"), 32));
}

/** Every 16-byte length-delimited field in a protobuf message, at any depth. */
export function sixteenByteFields(buf: Buffer, depth = 0): Buffer[] {
  const out: Buffer[] = [];
  let i = 0;
  const varint = (): number => {
    let shift = 0;
    let v = 0;
    for (;;) {
      if (i >= buf.length || shift > 35) throw new Error("bad varint");
      const b = buf[i++]!;
      v += (b & 0x7f) * 2 ** shift;
      if (!(b & 0x80)) return v;
      shift += 7;
    }
  };
  try {
    while (i < buf.length) {
      const tag = varint();
      const wire = tag & 7;
      if (wire === 0) varint();
      else if (wire === 1) i += 8;
      else if (wire === 5) i += 4;
      else if (wire === 2) {
        const len = varint();
        if (i + len > buf.length) break;
        const field = buf.subarray(i, i + len);
        if (len === 16) out.push(Buffer.from(field));
        else if (depth < 4) out.push(...sixteenByteFields(field, depth + 1));
        i += len;
      } else break;
    }
  } catch {
    // A field that is not a nested message parses as garbage; what was found so far stands.
  }
  return out;
}

/** The first bytes of a zlib stream: CMF 0x78 and a valid FCHECK. */
function looksLikeZlib(b: Buffer): boolean {
  return b.length >= 2 && b[0] === 0x78 && ((b[0]! << 8) | b[1]!) % 31 === 0;
}

function gcm(key: Buffer, iv: Buffer, data: Buffer): Buffer {
  return createDecipheriv("aes-256-gcm", key, iv).update(data);
}

/** Decrypt a .crypt15 backup to the bytes of its SQLite database. */
export function decryptCrypt15(file: Buffer, key64: string): Buffer {
  const key = backupKey(key64);
  if (file.length < 64) throw new Error("too short to be a crypt15 backup");
  let headerSize = 0;
  let start = 0;
  for (let shift = 0; ; shift += 7) {
    if (shift > 28) throw new Error("not a crypt15 backup: bad header size");
    const b = file[start++]!;
    headerSize += (b & 0x7f) * 2 ** shift;
    if (!(b & 0x80)) break;
  }
  const header = file.subarray(start, start + headerSize);
  const body = file.subarray(start + headerSize);
  {
    // The IV is the one 16-byte field whose first block decrypts to a zlib header.
    for (const iv of sixteenByteFields(header)) {
      if (!looksLikeZlib(gcm(key, iv, body.subarray(0, 16)))) continue;
      // Tag and checksum at the end (32 bytes), or the tag alone (16).
      for (const trailer of [32, 16]) {
        const ciphertext = body.subarray(0, body.length - trailer);
        const tag = body.subarray(body.length - trailer, body.length - trailer + 16);
        const d = createDecipheriv("aes-256-gcm", key, iv);
        d.setAuthTag(tag);
        let plain: Buffer;
        try {
          plain = Buffer.concat([d.update(ciphertext), d.final()]);
        } catch {
          continue; // wrong trailer length, or tampered
        }
        const db = inflateSync(plain);
        if (!db.subarray(0, 16).equals(SQLITE_MAGIC)) throw new Error("decrypted, but the result is not a SQLite database");
        return db;
      }
      throw new Error("the backup decrypts but fails its integrity check: the file may be incomplete");
    }
  }
  throw new Error("could not decrypt: wrong key, or not a crypt15 backup made with the 64-digit key");
}
