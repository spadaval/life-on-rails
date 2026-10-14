import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";
import { decryptCrypt15, sixteenByteFields } from "../../src/server/whatsapp/crypt15.js";
import { messageObservation } from "../../src/server/whatsapp/import.js";
import { readMessages } from "../../src/server/whatsapp/msgstore.js";
import { KEY, crypt15, msgstore } from "./fixture.js";

describe("crypt15", () => {
  it("decrypts a backup made with the 64-digit key back to the SQLite file", () => {
    const db = msgstore();
    const plain = decryptCrypt15(crypt15(db), KEY.match(/.{4}/g)!.join(" "));
    expect(plain.subarray(0, 15).toString("latin1")).toBe("SQLite format 3");
  });

  it("refuses the wrong key", () => {
    expect(() => decryptCrypt15(crypt15(msgstore()), "f".repeat(64))).toThrow(/could not decrypt/);
  });

  it("refuses a truncated file", () => {
    const file = crypt15(msgstore());
    expect(() => decryptCrypt15(file.subarray(0, file.length - 100), KEY)).toThrow();
  });

  it("finds 16-byte fields at any depth of the header", () => {
    const iv = Buffer.alloc(16, 7);
    expect(sixteenByteFields(Buffer.concat([Buffer.from([0x08, 0x01, 0x1a, 0x12, 0x0a, 0x10]), iv]))).toEqual([iv]);
  });
});

describe("msgstore", () => {
  const roundTrip = () => {
    const dir = mkdtempSync(path.join(tmpdir(), "wa-out-"));
    const file = path.join(dir, "msgstore.db");
    writeFileSync(file, decryptCrypt15(crypt15(msgstore()), KEY));
    return readMessages(file);
  };

  it("reads one-to-one and group messages with their text, and leaves out system notices and status updates", () => {
    const m = roundTrip();
    expect(m.map((x) => x.keyId)).toEqual(["A1", "A2", "A3", "G1", "G2", "L1"]);
    expect(m[0]).toMatchObject({ phone: "14155550100", fromMe: false, group: false, text: "Did you send the TDS form?", type: "text" });
    expect(m[1]).toMatchObject({ fromMe: true, phone: "14155550100" });
    expect(m[2]).toMatchObject({ type: "image", text: null });
  });

  it("credits group messages to whoever wrote them, and none to the owner's own", () => {
    const m = roundTrip();
    expect(m[3]).toMatchObject({ group: true, chatName: "Wedding planning", phone: "919876543210", fromMe: false });
    expect(m[4]).toMatchObject({ group: true, phone: null, fromMe: true });
  });

  it("maps an @lid chat to its phone number", () => {
    expect(roundTrip()[5]).toMatchObject({ phone: "14155550111", text: "hi from a lid chat" });
  });

  it("reads only messages newer than the cursor", () => {
    const dir = mkdtempSync(path.join(tmpdir(), "wa-out-"));
    const file = path.join(dir, "msgstore.db");
    writeFileSync(file, decryptCrypt15(crypt15(msgstore()), KEY));
    expect(readMessages(file, 1760000180000).map((x) => x.keyId)).toEqual(["G2", "L1"]);
  });

  it("becomes an observation keyed by WhatsApp's own id, with the number in E.164", () => {
    const [a, , , g] = roundTrip();
    const o = messageObservation(a!, "pixel-9a");
    expect(o.payload).toMatchObject({ kind: "whatsapp", direction: "in", phone: "+14155550100", group: false, text: "Did you send the TDS form?" });
    expect(messageObservation(a!, "pixel-9a").id).toBe(o.id);
    expect(messageObservation(g!, "pixel-9a")).toMatchObject({ title: "Wedding planning", payload: { group: true, phone: "+919876543210" } });
  });
});

describe("crypt15 from a backup split across files", () => {
  it("decrypts when the file ends with the tag and no checksum", () => {
    const file = crypt15(msgstore());
    const plain = decryptCrypt15(file.subarray(0, file.length - 16), KEY);
    expect(plain.subarray(0, 15).toString("latin1")).toBe("SQLite format 3");
  });
});
