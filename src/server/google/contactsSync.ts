// Google Contacts → entities, one way. Google is the source for a contact's
// name and birthday; its addresses and numbers become identifiers, which is
// what lets mail, meetings, and calls be matched to the person. Notes,
// addresses, and photos stay in Google, where the agent reads them with gog.
import type { Sql, Tx } from "../../db/client.js";
import { uuidv7 } from "../../shared/ids.js";
import { normalizePhone } from "../../shared/phone.js";
import { env } from "../env.js";
import { type ContactsPort, type GPerson, SyncTokenExpiredError } from "./contactsClient.js";

export interface MappedContact {
  googleId: string;
  kind: "person" | "org";
  name: string;
  aliases: string[];
  emails: string[];
  phones: string[];
  birthday: { month: number; day: number; year: number | null } | null;
}

const CURSOR = "google_contacts";

export function mapContact(p: GPerson, countryCode: string): MappedContact | null {
  const names = p.names ?? [];
  const primary = names.find((n) => n.metadata?.primary) ?? names[0];
  const personName = primary?.displayName?.trim() || [primary?.givenName, primary?.familyName].filter(Boolean).join(" ").trim();
  const org = p.organizations?.find((o) => o.name?.trim())?.name?.trim();
  const emails = [...new Set((p.emailAddresses ?? []).map((e) => e.value.trim().toLowerCase()).filter((e) => e.includes("@")))];
  const phones = [
    ...new Set(
      (p.phoneNumbers ?? [])
        .map((n) => (n.canonicalForm ? normalizePhone(n.canonicalForm, countryCode) : n.value ? normalizePhone(n.value, countryCode) : null))
        .filter((n): n is string => Boolean(n)),
    ),
  ];
  const name = personName || org || emails[0] || phones[0];
  if (!name) return null; // an empty card
  const aliases = [
    ...(p.nicknames ?? []).map((n) => n.value.trim()),
    ...names.filter((n) => n !== primary).map((n) => n.displayName?.trim() ?? ""),
  ].filter((a) => a && a.toLowerCase() !== name.toLowerCase());
  const b = p.birthdays?.find((x) => x.date?.month && x.date?.day)?.date;
  return {
    googleId: p.resourceName,
    kind: personName ? "person" : org ? "org" : "person",
    name,
    aliases: [...new Set(aliases)],
    emails,
    phones,
    birthday: b?.month && b?.day ? { month: b.month, day: b.day, year: b.year ?? null } : null,
  };
}

export interface ContactsSyncStats {
  full: boolean;
  seen: number;
  created: number;
  linked: number;
  updated: number;
  unlinked: number;
  /** Addresses or numbers already belonging to someone else, left with them. */
  shared_identifiers: number;
  [key: string]: unknown;
}

interface Row {
  id: string;
  name: string;
  aliases: string[];
  google_contact_id: string | null;
  name_pinned: boolean;
  birthday_month: number | null;
  birthday_day: number | null;
  birthday_year: number | null;
}

async function upsertContact(tx: Tx, c: MappedContact, stats: ContactsSyncStats): Promise<void> {
  let entity = (await tx<Row[]>`SELECT * FROM entity WHERE google_contact_id = ${c.googleId} FOR UPDATE`)[0];
  if (!entity) {
    // Someone the agent (or an earlier contact) already knows by an address or number.
    const owners = await tx<Row[]>`
      SELECT DISTINCT e.* FROM entity e JOIN entity_identifier i ON i.entity_id = e.id
      WHERE e.google_contact_id IS NULL
        AND ((i.kind = 'email' AND i.value = ANY(${c.emails}::text[])) OR (i.kind = 'phone' AND i.value = ANY(${c.phones}::text[])))
      ORDER BY e.id LIMIT 1`;
    entity = owners[0];
    if (entity) {
      await tx`UPDATE entity SET google_contact_id = ${c.googleId} WHERE id = ${entity.id}`;
      stats.linked++;
    }
  }
  if (!entity) {
    const id = uuidv7();
    await tx`INSERT INTO entity (id, kind, name, aliases, google_contact_id, birthday_month, birthday_day, birthday_year)
      VALUES (${id}, ${c.kind}, ${c.name}, ${c.aliases}, ${c.googleId}, ${c.birthday?.month ?? null}, ${c.birthday?.day ?? null}, ${c.birthday?.year ?? null})`;
    stats.created++;
  } else {
    // Google wins on the name and the birthday; an older name is kept as an alias, and a birthday set in LoR stays if Google has none.
    // A name pinned in LoR stays, and Google's name becomes an alias instead.
    const name = entity.name_pinned ? entity.name : c.name;
    const aliases = new Map<string, string>();
    for (const a of [...entity.aliases, ...c.aliases, entity.name, c.name]) aliases.set(a.toLowerCase(), a);
    aliases.delete(name.toLowerCase());
    const b = c.birthday ?? (entity.birthday_month ? { month: entity.birthday_month, day: entity.birthday_day!, year: entity.birthday_year } : null);
    const changed =
      entity.name !== name ||
      [...aliases.values()].sort().join("\n") !== [...entity.aliases].sort().join("\n") ||
      (b?.month ?? null) !== entity.birthday_month || (b?.day ?? null) !== entity.birthday_day || (b?.year ?? null) !== entity.birthday_year;
    if (changed) {
      await tx`UPDATE entity SET name = ${name}, aliases = ${[...aliases.values()]},
          birthday_month = ${b?.month ?? null}, birthday_day = ${b?.day ?? null}, birthday_year = ${b?.year ?? null}, updated_at = now()
        WHERE id = ${entity.id}`;
      stats.updated++;
    }
  }
  const id = entity?.id ?? (await tx<{ id: string }[]>`SELECT id FROM entity WHERE google_contact_id = ${c.googleId}`)[0]!.id;
  for (const [kind, values] of [["email", c.emails], ["phone", c.phones]] as const) {
    for (const value of values) {
      const r = await tx<{ entity_id: string }[]>`
        INSERT INTO entity_identifier (entity_id, kind, value) VALUES (${id}, ${kind}, ${value})
        ON CONFLICT (kind, value) DO UPDATE SET entity_id = entity_identifier.entity_id
        RETURNING entity_id`;
      if (r[0]?.entity_id !== id) stats.shared_identifiers++;
    }
  }
}

export async function syncContacts(db: Sql, port: ContactsPort): Promise<ContactsSyncStats> {
  const cursor = (await db<{ token: string | null }[]>`SELECT token FROM sync_cursor WHERE name = ${CURSOR}`)[0]?.token ?? null;
  let page;
  try {
    page = await port.connections(cursor);
  } catch (err) {
    if (!(err instanceof SyncTokenExpiredError)) throw err;
    page = await port.connections(null);
  }
  const stats: ContactsSyncStats = { full: page.full, seen: page.people.length, created: 0, linked: 0, updated: 0, unlinked: 0, shared_identifiers: 0 };
  const present: string[] = [];

  await db.begin(async (tx) => {
    for (const person of page.people) {
      if (person.metadata?.deleted) {
        // Unlinked, not deleted: the person's page and history stay.
        const r = await tx`UPDATE entity SET google_contact_id = NULL, updated_at = now() WHERE google_contact_id = ${person.resourceName} RETURNING id`;
        stats.unlinked += r.length;
        continue;
      }
      const mapped = mapContact(person, env.LOR_PHONE_COUNTRY_CODE);
      if (!mapped) continue;
      present.push(mapped.googleId);
      await upsertContact(tx, mapped, stats);
    }
    if (page.full) {
      // A full listing is the whole truth: anyone linked who is not in it was deleted while we were not looking.
      const r = await tx`UPDATE entity SET google_contact_id = NULL, updated_at = now()
        WHERE google_contact_id IS NOT NULL AND NOT (google_contact_id = ANY(${present}::text[])) RETURNING id`;
      stats.unlinked += r.length;
    }
    await tx`INSERT INTO sync_cursor (name, token, updated_at) VALUES (${CURSOR}, ${page.nextSyncToken}, now())
      ON CONFLICT (name) DO UPDATE SET token = EXCLUDED.token, updated_at = EXCLUDED.updated_at`;
  });
  return stats;
}
