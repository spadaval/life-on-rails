// People, organisations, places, things: the subjects that raw data mentions.
// Identifiers (an email address, a domain) are how a sender or attendee is
// recognised as an entity.
import type { Sql, Tx } from "../../db/client.js";
import { uuidv7 } from "../../shared/ids.js";
import { BadInput, NotFound, Refused } from "../errors.js";

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
  /** The latest messages from this entity (headers only; the mail itself is in Gmail). */
  recent_mail: { gmail_id: string; subject: string | null; at: string }[];
  updated_at: string;
}

interface EntityRow {
  id: string;
  kind: string;
  name: string;
  aliases: string[];
  priority: boolean;
  updated_at: Date;
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function normalizeIdentifier(id: Identifier): Identifier {
  return { kind: id.kind.trim().toLowerCase(), value: id.value.trim().toLowerCase() };
}

async function viewOf(tx: Tx, e: EntityRow): Promise<EntityView> {
  const identifiers = await tx<Identifier[]>`
    SELECT kind, value FROM entity_identifier WHERE entity_id = ${e.id} ORDER BY kind, value`;
  const page = await tx<{ slug: string }[]>`SELECT slug FROM page WHERE entity_id = ${e.id}`;
  const mail = await tx<{ gmail_id: string; subject: string | null; at: Date }[]>`
    SELECT payload->>'gmail_id' AS gmail_id, title AS subject, t_start AS at FROM observation
    WHERE source = 'gmail' AND payload->>'entity_id' = ${e.id} ORDER BY t_start DESC LIMIT 10`;
  return {
    id: e.id,
    kind: e.kind,
    name: e.name,
    aliases: e.aliases,
    identifiers: identifiers.map((i) => ({ kind: i.kind, value: i.value })),
    page: page[0]?.slug ?? null,
    priority: e.priority,
    recent_mail: mail.map((m) => ({ gmail_id: m.gmail_id, subject: m.subject, at: m.at.toISOString() })),
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
  const byIdentifier = await db<EntityRow[]>`
    SELECT e.* FROM entity e JOIN entity_identifier i ON i.entity_id = e.id
    WHERE i.value = ${k.toLowerCase()} LIMIT 1`;
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

export async function listEntities(db: Tx): Promise<EntityView[]> {
  const rows = await db<EntityRow[]>`SELECT * FROM entity ORDER BY lower(name)`;
  const out: EntityView[] = [];
  for (const r of rows) out.push(await viewOf(db, r));
  return out;
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

export async function setEntityPriority(db: Tx, id: string, priority: boolean): Promise<void> {
  const rows = await db`UPDATE entity SET priority = ${priority}, updated_at = now() WHERE id = ${id} RETURNING id`;
  if (rows.length === 0) throw new NotFound(`no entity ${id}`);
}
