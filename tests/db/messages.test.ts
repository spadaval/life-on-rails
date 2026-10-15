import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { env } from "../../src/server/env.js";
import { handleApi } from "../../src/server/api/v1.js";
import { ingest } from "../../src/server/device.js";
import { findEntity, showEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { dayTimeline } from "../../src/server/memory/timeline.js";
import { localYmd } from "../../src/shared/localTime.js";
import { search } from "../../src/server/memory/search.js";
import { recordObservation } from "../../src/server/observations.js";
import { lastContacts, listPeople } from "../../src/server/people.js";
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
      expect((await sql`SELECT count(*)::int AS n FROM observation WHERE kind = 'whatsapp'`)[0]!.n).toBe(6);
    } finally {
      env.LOR_WHATSAPP_DIR = saved.dir;
      env.LOR_WHATSAPP_KEY_FILE = saved.key;
    }
  });

  it("a WhatsApp group becomes a group entity with its members; it is in touch with the group, not with each member", async () => {
    const dir = mkdtempSync(path.join(tmpdir(), "lor-wa-"));
    const saved = { dir: env.LOR_WHATSAPP_DIR, key: env.LOR_WHATSAPP_KEY_FILE };
    env.LOR_WHATSAPP_DIR = dir;
    env.LOR_WHATSAPP_KEY_FILE = path.join(dir, "key");
    try {
      writeFileSync(env.LOR_WHATSAPP_KEY_FILE, KEY);
      const raj = await upsertEntity(sql, { kind: "person", name: "Raj", identifiers: [{ kind: "phone", value: "+91 98765 43210" }] });
      writeFileSync(path.join(dir, BACKUP_FILE), crypt15(msgstore()));
      expect(await importWhatsapp(sql)).toMatchObject({ groups: 1, new_groups: 1 });

      const group = await showEntity(sql, (await findEntity(sql, "120363000000000000@g.us"))!.id);
      expect(group).toMatchObject({ kind: "group", name: "Wedding planning", groups: [] });
      expect(group.members).toEqual([
        { entity_id: raj.id, name: "Raj", identifier: "+919876543210" },
        { entity_id: null, name: null, identifier: "+14155550111" },
      ]);
      // What was said in the group, and who said it.
      expect(group.recent_messages.map((m) => [m.from, m.text])).toEqual([
        [null, "Looking now"],
        ["Raj", "Venue shortlist is up"],
      ]);
      expect(group.last_contact).toMatchObject({ via: "phone" });

      const rajView = await showEntity(sql, raj.id);
      expect(rajView.groups).toEqual([{ id: group.id, name: "Wedding planning" }]);
      expect(rajView.recent_messages).toEqual([expect.objectContaining({ group: "Wedding planning", from: "Raj" })]);
      expect(rajView.last_contact).toBeNull();

      // Groups are their own list, not among the people.
      expect((await listPeople(sql, { filter: "groups" })).people.map((p) => p.name)).toEqual(["Wedding planning"]);
      expect((await listPeople(sql, { filter: "all" })).people.map((p) => p.name)).not.toContain("Wedding planning");
      const api = await handleApi(new Request("http://127.0.0.1:3847/api/v1/people?filter=groups", { headers: { authorization: "Bearer test-agent-token-000" } }));
      expect(await api.json()).toMatchObject({ total: 1, results: [expect.objectContaining({ name: "Wedding planning" })] });

      // A rename in WhatsApp renames the entity and keeps the old name as an alias.
      writeFileSync(path.join(dir, BACKUP_FILE), crypt15(msgstore("Wedding crew")));
      expect(await importWhatsapp(sql)).toMatchObject({ new_groups: 0 });
      expect(await showEntity(sql, group.id)).toMatchObject({ name: "Wedding crew", aliases: ["Wedding planning"] });
    } finally {
      env.LOR_WHATSAPP_DIR = saved.dir;
      env.LOR_WHATSAPP_KEY_FILE = saved.key;
    }
  });

  it("the timeline shows a day's messages as one line per conversation", async () => {
    await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "phone", value: "+14155550100" }] });
    const at = new Date(Date.now() - 2 * H);
    const msg = (min: number, direction: string, phone: string) =>
      recordObservation(sql, {
        tStart: new Date(at.getTime() + min * 60_000), device: "pixel-9a", source: "android", app: "com.whatsapp", title: phone,
        payload: { kind: "whatsapp", direction, phone, group: false, text: `m${min}` },
      });
    const a = await msg(0, "in", "+14155550100");
    await msg(1, "out", "+14155550100");
    await msg(2, "in", "+14155550199");
    await recordObservation(sql, { tStart: at, tEnd: new Date(at.getTime() + 60_000), device: "pixel-9a", source: "android", app: "com.example", title: "Example", payload: { kind: "app" } });

    const rows = await dayTimeline(sql, localYmd(env.LOR_TZ, at));
    const lines = rows.map((r) => [r.source, r.title, r.detail]);
    expect(lines).toContainEqual(["whatsapp", "Priya", "2 messages"]);
    expect(lines).toContainEqual(["whatsapp", "+14155550199", "1 message"]);
    expect(lines).toContainEqual(["android", "Example", "com.example"]);
    expect(rows.find((r) => r.title === "Priya")!.ids).toContain(a.id);
  });

  it("mail is with its sender, or with everyone it was sent to, by address and by domain", async () => {
    const acme = await upsertEntity(sql, { kind: "org", name: "Acme", identifiers: [{ kind: "domain", value: "acme.com" }] });
    await recordObservation(sql, {
      tStart: new Date(Date.now() - H), device: "buildbot", source: "gmail", title: "Re: invoice",
      payload: { gmail_id: "g1", direction: "sent", to_emails: ["Bill@Acme.com", "x@other.org"] },
    });
    const [row] = await sql<{ kind: string; parties: string[]; author: string | null }[]>`SELECT kind, parties, author FROM observation WHERE source = 'gmail'`;
    expect(row).toEqual({ kind: "mail", parties: ["domain:acme.com", "domain:other.org", "email:bill@acme.com", "email:x@other.org"], author: null });
    expect((await lastContacts(sql, [acme.id])).get(acme.id)).toMatchObject({ via: "mail_out" });
    expect((await showEntity(sql, acme.id)).recent_mail).toHaveLength(1);
  });
});
