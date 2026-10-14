// People, organisations, places, things: the subjects that raw data mentions.
// Identifiers (an email address, a domain) are how a sender or attendee is
// recognised as an entity.
import type { Sql, Tx } from "../../db/client.js";
import { uuidv7 } from "../../shared/ids.js";
import { normalizePhone } from "../../shared/phone.js";
import { env } from "../env.js";
import { BadInput, NotFound, Refused } from "../errors.js";
import { type LastContact, lastContacts } from "../people.js";

export const ENTITY_KINDS = ["person", "org", "place", "thing"] as const;
export type EntityKind = (typeof ENTITY_KINDS)[number];

export interface Identifier {
  kind: string; // email | phone | handle | domain
  value: string;
}

export interface EntityView {
  id: string;
  kind: string;
  name: string;
  aliases: string[];
  identifiers: Identifier[];
  /** Slug of the page about this entity, if one exists. */
  page: string | null;
  /** Mail from this entity reaches the agent without the usual wait. */
  priority: boolean;
  /** Set when the entity came from Google Contacts. */
  google_contact_id: string | null;
  birthday: { month: number; day: number; year: number | null } | null;
  /** Keep in touch at least this often (days); null = no expectation. */
  keep_in_touch_days: number | null;
  /** When the owner was last in touch, by mail either way, a meeting, or the phone. */
  last_contact: LastContact | null;
  /** The latest mail with this entity, both ways (headers only; the mail itself is in Gmail). */
  recent_mail: { gmail_id: string; subject: string | null; at: string; direction: "in" | "sent" }[];
  updated_at: string;
}

export interface EntityRow {
  id: string;
  kind: string;
  name: string;
  aliases: string[];
  priority: boolean;
  google_contact_id: string | null;
  birthday_month: number | null;
  birthday_day: number | null;
  birthday_year: number | null;
  keep_in_touch_days: number | null;
  updated_at: Date;
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function normalizeIdentifier(id: Identifier): Identifier {
  const kind = id.kind.trim().toLowerCase();
  if (kind === "phone") return { kind, value: normalizePhone(id.value, env.LOR_PHONE_COUNTRY_CODE) ?? "" };
  if (kind === "domain") return { kind, value: id.value.trim().toLowerCase().replace(/^@/, "") };
  return { kind, value: id.value.trim().toLowerCase() };
}

/** Mail with this entity, both ways, matched by its addresses and domains at read time. */
export async function recentMail(tx: Tx, entityId: string, limit = 10): Promise<EntityView["recent_mail"]> {
  const rows = await tx<{ gmail_id: string; subject: string | null; at: Date; direction: "in" | "sent" }[]>`
    WITH ids AS (SELECT kind, value FROM entity_identifier WHERE entity_id = ${entityId} AND kind IN ('email', 'domain'))
    SELECT o.payload->>'gmail_id' AS gmail_id, o.title AS subject, o.t_start AS at,
           coalesce(o.payload->>'direction', 'in') AS direction
    FROM observation o
    WHERE o.source = 'gmail' AND (
      EXISTS (SELECT 1 FROM ids WHERE (ids.kind = 'email' AND ids.value = o.payload->>'from_email')
                                   OR (ids.kind = 'domain' AND ids.value = split_part(o.payload->>'from_email', '@', 2)))
      OR EXISTS (SELECT 1 FROM ids, jsonb_array_elements_text(coalesce(o.payload->'to_emails', '[]'::jsonb)) t
                 WHERE (ids.kind = 'email' AND ids.value = t) OR (ids.kind = 'domain' AND ids.value = split_part(t, '@', 2))))
    ORDER BY o.t_start DESC LIMIT ${limit}`;
  return rows.map((m) => ({ gmail_id: m.gmail_id, subject: m.subject, at: m.at.toISOString(), direction: m.direction }));
}

export async function viewOf(tx: Tx, e: EntityRow): Promise<EntityView> {
  const identifiers = await tx<Identifier[]>`
    SELECT kind, value FROM entity_identifier WHERE entity_id = ${e.id} ORDER BY kind, value`;
  const page = await tx<{ slug: string }[]>`SELECT slug FROM page WHERE entity_id = ${e.id}`;
  return {
    id: e.id,
    kind: e.kind,
    name: e.name,
    aliases: e.aliases,
    identifiers: identifiers.map((i) => ({ kind: i.kind, value: i.value })),
    page: page[0]?.slug ?? null,
    priority: e.priority,
    google_contact_id: e.google_contact_id,
    birthday: e.birthday_month && e.birthday_day ? { month: e.birthday_month, day: e.birthday_day, year: e.birthday_year } : null,
    keep_in_touch_days: e.keep_in_touch_days,
    last_contact: (await lastContacts(tx, [e.id])).get(e.id) ?? null,
    recent_mail: await recentMail(tx, e.id),
    updated_at: e.updated_at.toISOString(),
  };
}

export interface UpsertEntityInput {
  kind: EntityKind;
  name: string;
  aliases?: string[];
  identifiers?: Identifier[];
}

/**
 * Find the entity by any of its identifiers, else by name within its kind;
 * create it if neither matches. New aliases and identifiers are added, never
 * removed. A different name for a known identifier becomes the name, and the
 * old name becomes an alias.
 */
export async function upsertEntity(db: Sql, input: UpsertEntityInput): Promise<EntityView & { created: boolean }> {
  const name = input.name.trim();
  if (!name) throw new BadInput("an entity needs a name");
  if (!ENTITY_KINDS.includes(input.kind)) throw new BadInput(`kind must be one of ${ENTITY_KINDS.join(", ")}`);
  const identifiers = (input.identifiers ?? []).map(normalizeIdentifier).filter((i) => i.value);
  const aliases = (input.aliases ?? []).map((a) => a.trim()).filter(Boolean);

  return db.begin(async (tx) => {
    let found: EntityRow | undefined;
    const owners = new Set<string>();
    for (const i of identifiers) {
      const rows = await tx<{ entity_id: string }[]>`
        SELECT entity_id FROM entity_identifier WHERE kind = ${i.kind} AND value = ${i.value}`;
      if (rows[0]) owners.add(rows[0].entity_id);
    }
    if (owners.size > 1) {
      throw new Refused("identifiers-span-entities", "those identifiers belong to different entities", {
        entity_ids: [...owners],
      });
    }
    if (owners.size === 1) {
      found = (await tx<EntityRow[]>`SELECT * FROM entity WHERE id = ${[...owners][0]!} FOR UPDATE`)[0];
    } else {
      found = (
        await tx<EntityRow[]>`
          SELECT * FROM entity
          WHERE kind = ${input.kind} AND (lower(name) = lower(${name}) OR lower(${name}) = ANY (SELECT lower(a) FROM unnest(aliases) a))
          ORDER BY created_at LIMIT 1 FOR UPDATE`
      )[0];
    }

    let created = false;
    let id: string;
    if (!found) {
      id = uuidv7();
      created = true;
      await tx`INSERT INTO entity (id, kind, name, aliases) VALUES (${id}, ${input.kind}, ${name}, ${aliases})`;
    } else {
      id = found.id;
      const merged = new Map<string, string>();
      for (const a of [...found.aliases, ...aliases]) merged.set(a.toLowerCase(), a);
      if (found.name.toLowerCase() !== name.toLowerCase()) merged.set(found.name.toLowerCase(), found.name);
      merged.delete(name.toLowerCase());
      await tx`UPDATE entity SET name = ${name}, aliases = ${[...merged.values()]}, updated_at = now() WHERE id = ${id}`;
    }
    for (const i of identifiers) {
      await tx`INSERT INTO entity_identifier (entity_id, kind, value) VALUES (${id}, ${i.kind}, ${i.value})
        ON CONFLICT (kind, value) DO NOTHING`;
    }
    const row = (await tx<EntityRow[]>`SELECT * FROM entity WHERE id = ${id}`)[0]!;
    return { ...(await viewOf(tx, row)), created };
  }) as Promise<EntityView & { created: boolean }>;
}

/** Look an entity up by id, by an identifier value (an email address), or by name. */
export async function findEntity(db: Tx, key: string): Promise<EntityRow | null> {
  const k = key.trim();
  if (!k) return null;
  if (UUID.test(k)) {
    return (await db<EntityRow[]>`SELECT * FROM entity WHERE id = ${k}`)[0] ?? null;
  }
  const phone = /^[+\d][\d\s().-]{5,}$/.test(k) ? normalizePhone(k, env.LOR_PHONE_COUNTRY_CODE) : null;
  const byIdentifier = await db<EntityRow[]>`
    SELECT e.* FROM entity e JOIN entity_identifier i ON i.entity_id = e.id
    WHERE i.value = ${k.toLowerCase()} OR (i.kind = 'phone' AND i.value = ${phone ?? ""}) LIMIT 1`;
  if (byIdentifier[0]) return byIdentifier[0];
  const byName = await db<EntityRow[]>`
    SELECT * FROM entity
    WHERE lower(name) = lower(${k}) OR lower(${k}) = ANY (SELECT lower(a) FROM unnest(aliases) a)
       OR similarity(name, ${k}) > 0.45
    ORDER BY (lower(name) = lower(${k})) DESC, similarity(name, ${k}) DESC LIMIT 1`;
  return byName[0] ?? null;
}

export async function showEntity(db: Tx, key: string): Promise<EntityView> {
  const row = await findEntity(db, key);
  if (!row) throw new NotFound(`no entity matches ${JSON.stringify(key)}`);
  return viewOf(db, row);
}

/** Id and name of every entity, for pickers. */
export async function entityNames(db: Tx): Promise<{ id: string; name: string }[]> {
  return db<{ id: string; name: string }[]>`SELECT id, name FROM entity ORDER BY lower(name)`;
}

/** Which entity, if any, an email address belongs to: exact address, then its domain. */
export async function entityForEmail(db: Tx, email: string): Promise<{ id: string; name: string } | null> {
  const address = email.trim().toLowerCase();
  const domain = address.split("@")[1] ?? "";
  const rows = await db<{ id: string; name: string }[]>`
    SELECT e.id, e.name FROM entity e JOIN entity_identifier i ON i.entity_id = e.id
    WHERE (i.kind = 'email' AND i.value = ${address}) OR (i.kind = 'domain' AND i.value = ${domain})
    ORDER BY (i.kind = 'email') DESC LIMIT 1`;
  return rows[0] ?? null;
}

export interface EntityPatch {
  priority?: boolean;
  /** Days; null clears it. */
  keepInTouchDays?: number | null;
  /** "MM-DD" or "YYYY-MM-DD"; null clears it. */
  birthday?: string | null;
}

export function parseBirthday(value: string): { month: number; day: number; year: number | null } {
  const m = /^(?:(\d{4})-)?(\d{1,2})-(\d{1,2})$/.exec(value.trim());
  const month = m ? Number(m[2]) : NaN;
  const day = m ? Number(m[3]) : NaN;
  if (!m || month < 1 || month > 12 || day < 1 || day > 31) throw new BadInput("birthday must be MM-DD or YYYY-MM-DD");
  return { month, day, year: m[1] ? Number(m[1]) : null };
}

export async function updateEntity(db: Tx, id: string, patch: EntityPatch): Promise<EntityView> {
  const rows = await db<EntityRow[]>`SELECT * FROM entity WHERE id = ${id}`;
  const e = rows[0];
  if (!e) throw new NotFound(`no entity ${id}`);
  if (patch.keepInTouchDays !== undefined && patch.keepInTouchDays !== null && !(Number.isInteger(patch.keepInTouchDays) && patch.keepInTouchDays > 0)) {
    throw new BadInput("keep in touch takes a whole number of days");
  }
  const bday = patch.birthday === undefined ? undefined : patch.birthday === null ? null : parseBirthday(patch.birthday);
  const month = bday === undefined ? e.birthday_month : (bday?.month ?? null);
  const day = bday === undefined ? e.birthday_day : (bday?.day ?? null);
  const year = bday === undefined ? e.birthday_year : (bday?.year ?? null);
  await db`UPDATE entity SET
      priority = ${patch.priority ?? e.priority},
      keep_in_touch_days = ${patch.keepInTouchDays === undefined ? e.keep_in_touch_days : patch.keepInTouchDays},
      birthday_month = ${month}, birthday_day = ${day}, birthday_year = ${year},
      updated_at = now()
    WHERE id = ${id}`;
  return viewOf(db, (await db<EntityRow[]>`SELECT * FROM entity WHERE id = ${id}`)[0]!);
}

export async function setEntityPriority(db: Tx, id: string, priority: boolean): Promise<void> {
  await updateEntity(db, id, { priority });
}
