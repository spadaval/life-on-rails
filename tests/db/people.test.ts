import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { handleApi } from "../../src/server/api/v1.js";
import { type ContactsPort, GPerson, SyncTokenExpiredError } from "../../src/server/google/contactsClient.js";
import { syncContacts } from "../../src/server/google/contactsSync.js";
import { getContext } from "../../src/server/memory/context.js";
import { findEntity, mergeEntities, showEntity, updateEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { createPage, readPage } from "../../src/server/memory/pages.js";
import "../../src/server/memory/peopleSection.js";
import { lastContacts, listPeople } from "../../src/server/people.js";
import { recordObservation } from "../../src/server/observations.js";
import { addDays, localTimeOnDay, localYmd } from "../../src/shared/localTime.js";
import { dbDescribe } from "./helpers.js";

/** A Google Contacts account whose contents tests change between syncs. */
function contacts() {
  let people = new Map<string, Record<string, unknown>>();
  let token = 0;
  const changedSince = new Map<number, Set<string>>();
  const deleted = new Set<string>();
  let expire = false;
  const touch = (id: string) => {
    token++;
    for (const [t, set] of changedSince) if (t < token) set.add(id);
  };
  const port: ContactsPort = {
    async connections(syncToken) {
      if (syncToken && expire) {
        expire = false;
        throw new SyncTokenExpiredError();
      }
      let list: Record<string, unknown>[];
      if (!syncToken) {
        list = [...people.values()];
      } else {
        const ids = changedSince.get(Number(syncToken)) ?? new Set<string>();
        list = [...ids].map((id) => (deleted.has(id) ? { resourceName: id, metadata: { deleted: true } } : people.get(id)!));
      }
      changedSince.set(token, new Set());
      return { people: list.map((p) => GPerson.parse(p)), nextSyncToken: String(token), full: !syncToken };
    },
  };
  return {
    port,
    put(p: Record<string, unknown>) {
      people.set(p.resourceName as string, p);
      touch(p.resourceName as string);
    },
    remove(id: string) {
      people.delete(id);
      deleted.add(id);
      touch(id);
    },
    vanish(id: string) {
      people = new Map([...people].filter(([k]) => k !== id));
    },
    expireToken() {
      expire = true;
    },
  };
}

const priya = { resourceName: "people/c1", names: [{ displayName: "Priya Raman" }], emailAddresses: [{ value: "priya@example.com" }], phoneNumbers: [{ value: "(415) 555-0100" }], birthdays: [{ date: { month: 10, day: 20, year: 1990 } }] };
const jo = { resourceName: "people/c2", names: [{ displayName: "Jo Bloggs" }], emailAddresses: [{ value: "jo@example.com" }] };
const acme = { resourceName: "people/c3", organizations: [{ name: "Acme" }], emailAddresses: [{ value: "billing@acme.com" }] };

const entityRows = () => sql<{ name: string; kind: string; google: string | null; ids: string }[]>`
  SELECT e.name, e.kind, e.google_contact_id AS google,
         coalesce((SELECT string_agg(value, ',' ORDER BY value) FROM entity_identifier i WHERE i.entity_id = e.id), '') AS ids
  FROM entity e ORDER BY e.name`;

dbDescribe("contacts import", () => {
  it("imports contacts as entities with their addresses, numbers, and birthdays, and a second sync changes nothing", async () => {
    const g = contacts();
    g.put(priya);
    g.put(jo);
    g.put(acme);
    expect(await syncContacts(sql, g.port)).toMatchObject({ full: true, seen: 3, created: 3, linked: 0 });
    expect(await entityRows()).toEqual([
      { name: "Acme", kind: "org", google: "people/c3", ids: "billing@acme.com" },
      { name: "Jo Bloggs", kind: "person", google: "people/c2", ids: "jo@example.com" },
      { name: "Priya Raman", kind: "person", google: "people/c1", ids: "+14155550100,priya@example.com" },
    ]);
    const p = await showEntity(sql, "415-555-0100");
    expect(p).toMatchObject({ name: "Priya Raman", birthday: { month: 10, day: 20, year: 1990 } });
    expect(await syncContacts(sql, g.port)).toMatchObject({ full: false, seen: 0, created: 0, updated: 0 });
  });

  it("links a contact to someone the agent already knew by email instead of duplicating them", async () => {
    const known = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "email", value: "priya@example.com" }] });
    await updateEntity(sql, known.id, { keepInTouchDays: 30 });
    const g = contacts();
    g.put(priya);
    expect(await syncContacts(sql, g.port)).toMatchObject({ created: 0, linked: 1, updated: 1 });
    const after = await showEntity(sql, known.id);
    expect(after).toMatchObject({ name: "Priya Raman", aliases: ["Priya"], google_contact_id: "people/c1", keep_in_touch_days: 30 });
    expect(after.identifiers.map((i) => i.value).sort()).toEqual(["+14155550100", "priya@example.com"]);
  });

  it("a second card with the same address becomes its own entity; the address stays with the first", async () => {
    const g = contacts();
    g.put(priya);
    g.put({ resourceName: "people/dup", names: [{ displayName: "Priya (work)" }], emailAddresses: [{ value: "priya@example.com" }] });
    const stats = await syncContacts(sql, g.port);
    expect(stats).toMatchObject({ created: 2, shared_identifiers: 1 });
    expect((await findEntity(sql, "priya@example.com"))?.name).toBe("Priya Raman");
  });

  it("follows edits and deletions; a deleted contact is unlinked, not deleted", async () => {
    const g = contacts();
    g.put(priya);
    g.put(jo);
    await syncContacts(sql, g.port);
    g.put({ ...jo, names: [{ displayName: "Joanna Bloggs" }], emailAddresses: [{ value: "jo@example.com" }, { value: "jo@work.example" }] });
    g.remove("people/c1");
    expect(await syncContacts(sql, g.port)).toMatchObject({ full: false, seen: 2, updated: 1, unlinked: 1 });
    expect(await entityRows()).toEqual([
      { name: "Joanna Bloggs", kind: "person", google: "people/c2", ids: "jo@example.com,jo@work.example" },
      { name: "Priya Raman", kind: "person", google: null, ids: "+14155550100,priya@example.com" },
    ]);
  });

  it("an expired sync token falls back to a full listing, which unlinks anyone gone", async () => {
    const g = contacts();
    g.put(priya);
    g.put(jo);
    await syncContacts(sql, g.port);
    g.vanish("people/c2"); // deleted with no record of it
    g.expireToken();
    expect(await syncContacts(sql, g.port)).toMatchObject({ full: true, unlinked: 1 });
  });
});

dbDescribe("names, aliases, and merging", () => {
  const thread = "test-thread";

  it("a name set in LoR sticks: a contacts sync files Google's name as an alias instead", async () => {
    const g = contacts();
    g.put({ resourceName: "people/sis", names: [{ displayName: "Imouto" }], phoneNumbers: [{ value: "+91 81970 53878" }] });
    await syncContacts(sql, g.port);
    const e = (await findEntity(sql, "Imouto"))!;
    const renamed = await updateEntity(sql, e.id, { name: "Sanjana Padavala", addAliases: ["Sanju", "Sis"], removeAliases: ["Sis"] });
    expect(renamed).toMatchObject({ name: "Sanjana Padavala", name_pinned: true });
    expect(renamed.aliases.sort()).toEqual(["Imouto", "Sanju"]);
    g.put({ resourceName: "people/sis", names: [{ displayName: "Imouto-chan" }], phoneNumbers: [{ value: "+91 81970 53878" }] });
    await syncContacts(sql, g.port);
    const after = await showEntity(sql, e.id);
    expect(after.name).toBe("Sanjana Padavala");
    expect(after.aliases.sort()).toEqual(["Imouto", "Imouto-chan", "Sanju"]);
  });

  it("merges an agent-made person into a Google contact: identifiers, history, page and cadence move; the merged one is gone", async () => {
    const g = contacts();
    g.put({ resourceName: "people/sis", names: [{ displayName: "Imouto" }], phoneNumbers: [{ value: "+91 81970 53878" }] });
    await syncContacts(sql, g.port);
    const google = (await findEntity(sql, "Imouto"))!;
    const made = await upsertEntity(sql, { kind: "person", name: "Sanjana Padavala", aliases: ["Sanjana"], identifiers: [{ kind: "email", value: "sanjana@example.com" }] });
    await updateEntity(sql, made.id, { keepInTouchDays: 14, birthday: "03-14" });
    await createPage(sql, { slug: "people/sanjana", title: "Sanjana", body: "Sister.\n", entityId: made.id, via: "agent", sourceRef: thread });
    const at = new Date(Date.now() - 3600_000);
    await recordObservation(sql, { tStart: at, tEnd: at, device: "pixel-9a", source: "android", payload: { kind: "call", direction: "in", phone: "+918197053878", duration_s: 60 } });

    const { entity, detached_page } = await mergeEntities(sql, made.id, google.id, thread);
    expect(detached_page).toBeNull();
    expect(entity).toMatchObject({
      id: made.id, name: "Sanjana Padavala", google_contact_id: "people/sis", name_pinned: true,
      keep_in_touch_days: 14, birthday: { month: 3, day: 14 }, page: "people/sanjana", last_contact: { via: "phone" },
    });
    expect(entity.aliases.sort()).toEqual(["Imouto", "Sanjana"]);
    expect(entity.identifiers.map((i) => i.value).sort()).toEqual(["+918197053878", "sanjana@example.com"]);
    expect(await sql`SELECT 1 FROM entity WHERE id = ${google.id}`).toHaveLength(0);
    const [note] = await sql<{ title: string; payload: { merged: { name: string } } }[]>`SELECT title, payload FROM observation WHERE source = 'agent'`;
    expect(note).toMatchObject({ title: "Merged Imouto into Sanjana Padavala", payload: { merged: { name: "Imouto" } } });

    // Google keeps the contact linked, and the name stays.
    g.put({ resourceName: "people/sis", names: [{ displayName: "Imouto" }], phoneNumbers: [{ value: "+91 81970 53878" }, { value: "+1 415 555 0199" }] });
    expect(await syncContacts(sql, g.port)).toMatchObject({ created: 0 });
    expect(await showEntity(sql, "+14155550199")).toMatchObject({ id: made.id, name: "Sanjana Padavala" });
  });

  it("when both have a page, the merged one's page is detached for the agent to fold in", async () => {
    const a = await upsertEntity(sql, { kind: "person", name: "Jo Bloggs" });
    const b = await upsertEntity(sql, { kind: "person", name: "Joanna B" });
    await createPage(sql, { slug: "people/jo", title: "Jo", body: "A.\n", entityId: a.id, via: "agent", sourceRef: thread });
    await createPage(sql, { slug: "people/joanna", title: "Joanna", body: "B.\n", entityId: b.id, via: "agent", sourceRef: thread });
    const r = await mergeEntities(sql, a.id, b.id, thread);
    expect(r).toMatchObject({ detached_page: "people/joanna", entity: { page: "people/jo", aliases: ["Joanna B"] } });
    expect((await readPage(sql, "people/joanna")).body).toBe("B.\n");
  });

  it("refuses to merge two Google contacts, different kinds, or an entity into itself", async () => {
    const g = contacts();
    g.put(priya);
    g.put(jo);
    await syncContacts(sql, g.port);
    const p = (await findEntity(sql, "Priya Raman"))!;
    const j = (await findEntity(sql, "Jo Bloggs"))!;
    const acmeOrg = await upsertEntity(sql, { kind: "org", name: "Acme" });
    await expect(mergeEntities(sql, p.id, j.id, thread)).rejects.toMatchObject({ code: "both-from-google" });
    await expect(mergeEntities(sql, p.id, acmeOrg.id, thread)).rejects.toMatchObject({ code: "kinds-differ" });
    await expect(mergeEntities(sql, p.id, p.id, thread)).rejects.toThrow(/itself/);
  });
});

dbDescribe("last in touch", () => {
  const H = 3600_000;
  // Local days, not 24-hour steps: across a DST change those land on the wrong date for an hour a day.
  const tz = "America/Los_Angeles";
  const daysAgo = (d: number) => localTimeOnDay(tz, addDays(localYmd(tz, new Date()), -d), "12:00");

  async function mail(id: string, at: Date, payload: Record<string, unknown>) {
    await recordObservation(sql, { id: undefined, tStart: at, tEnd: at, device: "buildbot", source: "gmail", title: id, payload: { gmail_id: id, ...payload } });
  }

  it("takes the latest of mail either way, meetings, and calls, matching by address, domain, and number", async () => {
    const g = contacts();
    g.put(priya);
    g.put(acme);
    await syncContacts(sql, g.port);
    const p = (await findEntity(sql, "priya@example.com"))!;
    const a = (await findEntity(sql, "billing@acme.com"))!;
    await sql`INSERT INTO entity_identifier (entity_id, kind, value) VALUES (${a.id}, 'domain', 'acme.com')`;

    await mail("in-1", daysAgo(20), { direction: "in", from_email: "priya@example.com" });
    await mail("out-1", daysAgo(9), { direction: "sent", to_emails: ["priya@example.com", "x@y.com"] });
    await mail("in-acme", daysAgo(3), { direction: "in", from_email: "support@acme.com" });
    await sql`INSERT INTO google_sync_state (calendar_id) VALUES ('cal') ON CONFLICT DO NOTHING`;
    await sql`INSERT INTO google_event (calendar_id, event_id, title, t_start, t_end, attendees)
      VALUES ('cal', 'lunch', 'Lunch', ${daysAgo(6)}, ${new Date(daysAgo(6).getTime() + H)}, ${["priya@example.com"]}::text[]),
             ('cal', 'declined', 'Skip', ${daysAgo(1)}, ${new Date(daysAgo(1).getTime() + H)}, ${["priya@example.com"]}::text[])`;
    await sql`UPDATE google_event SET self_response = 'declined' WHERE event_id = 'declined'`;

    let last = await lastContacts(sql);
    expect(last.get(p.id)).toMatchObject({ via: "meeting" });
    expect(last.get(p.id)!.mail_out).not.toBeNull();
    expect(last.get(a.id)).toMatchObject({ via: "mail_in" }); // by domain

    // A call from the phone sensor, under the contract it will use.
    await recordObservation(sql, { tStart: daysAgo(2), tEnd: daysAgo(2), device: "pixel-9a", source: "android", payload: { kind: "call", direction: "in", phone: "+14155550100", duration_s: 300 } });
    last = await lastContacts(sql, [p.id]);
    expect(last.get(p.id)).toMatchObject({ via: "phone" });
  });

  it("lists who is overdue and whose birthday is near, and says so in the digest", async () => {
    const g = contacts();
    const [, month, day] = addDays(localYmd(tz, new Date()), 3).split("-").map(Number);
    g.put({ ...priya, birthdays: [{ date: { month, day, year: 1990 } }] });
    g.put(jo);
    g.put(acme);
    await syncContacts(sql, g.port);
    const p = (await findEntity(sql, "priya@example.com"))!;
    const j = (await findEntity(sql, "jo@example.com"))!;
    await updateEntity(sql, p.id, { keepInTouchDays: 30 });
    await updateEntity(sql, j.id, { keepInTouchDays: 14 });
    await mail("in-1", daysAgo(40), { direction: "in", from_email: "priya@example.com" });
    await mail("in-2", daysAgo(5), { direction: "in", from_email: "jo@example.com" });

    const overdue = await listPeople(sql, { filter: "overdue" });
    expect(overdue.people.map((x) => [x.name, x.overdue_by])).toEqual([["Priya Raman", 10]]);
    const birthdays = await listPeople(sql, { filter: "birthdays", birthdayWindow: 7 });
    expect(birthdays.people.map((x) => [x.name, x.birthday_in])).toEqual([["Priya Raman", 3]]);
    // Who matters: a cadence, or in touch recently. A contact with neither is not shown by default.
    expect((await listPeople(sql)).people.map((x) => x.name)).toEqual(["Jo Bloggs", "Priya Raman"]);
    expect((await listPeople(sql, { filter: "all" })).total).toBe(3);
    expect((await listPeople(sql, { q: "acme" })).people.map((x) => x.name)).toEqual(["Acme"]);

    const ctx = await getContext(sql);
    expect(ctx.markdown).toContain("## People");
    expect(ctx.markdown).toContain("Priya Raman: birthday in 3 days");
    expect(ctx.markdown).toContain("Priya Raman: last in touch 40 days ago (they emailed); aiming for every 30 days");
    expect(ctx.markdown).not.toContain("Jo Bloggs");
  });

  it("is set and read over the API", async () => {
    const g = contacts();
    g.put(jo);
    await syncContacts(sql, g.port);
    const call = (method: string, path: string, body?: unknown) =>
      handleApi(new Request(`http://127.0.0.1:3847/api/v1${path}`, {
        method,
        headers: { authorization: "Bearer test-agent-token-000", "content-type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
      }));
    const set = await call("POST", `/entities/${encodeURIComponent("jo@example.com")}`, { keep_in_touch_days: 21, birthday: "07-04", priority: true, thread: "t" });
    expect(set.status).toBe(200);
    expect(await set.json()).toMatchObject({ keep_in_touch_days: 21, birthday: { month: 7, day: 4, year: null }, priority: true });
    const list = (await (await call("GET", "/people?filter=overdue")).json()) as { results: { name: string }[]; markdown: string };
    expect(list.results.map((x) => x.name)).toEqual(["Jo Bloggs"]);
    expect(list.markdown).toContain("never in touch; aiming for every 21 days");
    const cleared = await call("POST", "/entities/Jo%20Bloggs", { clear_keep_in_touch: true, clear_birthday: true, thread: "t" });
    expect(await cleared.json()).toMatchObject({ keep_in_touch_days: null, birthday: null });
    expect((await call("POST", "/entities/jo@example.com", { birthday: "13-40", thread: "t" })).status).toBe(400);
  });
});
