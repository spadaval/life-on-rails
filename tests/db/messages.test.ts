import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { env } from "../../src/server/env.js";
import { ingest } from "../../src/server/device.js";
import { showEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { search } from "../../src/server/memory/search.js";
import { recordObservation } from "../../src/server/observations.js";
import { lastContacts } from "../../src/server/people.js";
import { BACKUP_FILE, importWhatsapp, noteUploader, whatsappPending } from "../../src/server/whatsapp/import.js";
import { KEY, crypt15, msgstore } from "../whatsapp/fixture.js";
import { dbDescribe } from "./helpers.js";

const H = 3600_000;
const daysAgo = (d: number) => new Date(Date.now() - d * 24 * H);

dbDescribe("messages", () => {
  it("keeps a text's words: searchable, and shown with the person", async () => {
    const priya = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "phone", value: "+1 415 555 0100" }] });
    const sms = (id: string, at: Date, direction: string, text: string) => ({
      id, t_start: at.toISOString(), t_end: at.toISOString(), device: "pixel-9a", source: "android", app: "sms", calendar_id: null,
      title: "(415) 555-0100", url: null, payload: { kind: "sms", direction, phone: "(415) 555-0100", text }, intent_hint: null,
    });
    const r = await ingest({
      device: "pixel-9a",
      observations: [
        sms("0199e000-0000-7000-8000-000000000001", daysAgo(2), "in", "Your passport appointment is on Friday"),
        sms("0199e000-0000-7000-8000-000000000002", daysAgo(1), "out", "Thanks, see you then"),
      ],
    });
    expect(r.stored).toBe(2);

    const hits = await search(sql, { q: "passport appointment", in: ["obs"] });
    expect(hits).toHaveLength(1);
    expect(hits[0]!.snippet).toContain("Your passport appointment is on Friday");

    const view = await showEntity(sql, priya.id);
    expect(view.recent_messages.map((m) => [m.direction, m.text])).toEqual([
      ["out", "Thanks, see you then"],
      ["in", "Your passport appointment is on Friday"],
    ]);
    expect(view.last_contact).toMatchObject({ via: "phone" });
  });

  it("a group chat is not being in touch with everyone in it, but what someone wrote there is shown with them", async () => {
    const raj = await upsertEntity(sql, { kind: "person", name: "Raj", identifiers: [{ kind: "phone", value: "+91 98765 43210" }] });
    await recordObservation(sql, {
      tStart: daysAgo(1), tEnd: daysAgo(1), device: "pixel-9a", source: "android", app: "com.whatsapp", title: "Wedding planning",
      payload: { kind: "whatsapp", direction: "in", phone: "+919876543210", group: true, chat_name: "Wedding planning", text: "Venue shortlist is up" },
    });
    expect((await lastContacts(sql, [raj.id])).get(raj.id)).toBeUndefined();
    expect((await showEntity(sql, raj.id)).recent_messages).toEqual([
      expect.objectContaining({ kind: "whatsapp", group: "Wedding planning", text: "Venue shortlist is up" }),
    ]);
  });

  it("imports a WhatsApp backup once, and only what is new the next time", async () => {
    const dir = mkdtempSync(path.join(tmpdir(), "lor-wa-"));
    const saved = { dir: env.LOR_WHATSAPP_DIR, key: env.LOR_WHATSAPP_KEY_FILE };
    env.LOR_WHATSAPP_DIR = dir;
    env.LOR_WHATSAPP_KEY_FILE = path.join(dir, "key");
    try {
      writeFileSync(env.LOR_WHATSAPP_KEY_FILE, `${KEY}\n`);
      expect(await whatsappPending()).toBe(false);
      writeFileSync(path.join(dir, BACKUP_FILE), crypt15(msgstore()));
      noteUploader("pixel-9a");
      expect(await whatsappPending()).toBe(true);

      const priya = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "phone", value: "+14155550100" }] });
      const first = await importWhatsapp(sql);
      expect(first).toMatchObject({ read: 6, stored: 6 });
      expect(await whatsappPending()).toBe(false);
      const view = await showEntity(sql, priya.id);
      expect(view.recent_messages[0]).toMatchObject({ kind: "whatsapp", direction: "in", text: null }); // the image
      expect(view.last_contact).toMatchObject({ via: "phone" });
      expect(await search(sql, { q: "TDS form", in: ["obs"] })).toHaveLength(1);

      // The same backup again: nothing to do. A new backup: only the overlap is re-read, and nothing duplicates.
      expect(await importWhatsapp(sql)).toMatchObject({ skipped: expect.any(String) });
      writeFileSync(path.join(dir, BACKUP_FILE), crypt15(msgstore()));
      expect(await importWhatsapp(sql)).toMatchObject({ stored: 0 });
      expect((await sql`SELECT count(*)::int AS n FROM observation WHERE payload->>'kind' = 'whatsapp'`)[0]!.n).toBe(6);
    } finally {
      env.LOR_WHATSAPP_DIR = saved.dir;
      env.LOR_WHATSAPP_KEY_FILE = saved.key;
    }
  });
});
