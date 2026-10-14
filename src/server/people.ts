// Keeping track of people: when the owner was last in touch with each one
// (mail either way, a meeting, a call or a text), who is overdue against the
// cadence the owner set, and whose birthday is coming. Everything is matched by
// identifiers at read time, so mail that arrived before a contact was imported
// still counts.
import type { Tx } from "../db/client.js";
import { type Ymd, addDays, localDaysBetween, localYmd, parseYmd } from "../shared/localTime.js";
import { env } from "./env.js";
import type { EntityRow } from "./memory/entities.js";

export type Channel = "mail_in" | "mail_out" | "meeting" | "phone";

export interface LastContact {
  /** The latest of the channels below. */
  at: string | null;
  via: Channel | null;
  mail_in: string | null;
  mail_out: string | null;
  meeting: string | null;
  phone: string | null;
}

const EMPTY: LastContact = { at: null, via: null, mail_in: null, mail_out: null, meeting: null, phone: null };

/**
 * Last contact per entity. Mail and meetings match an entity's email
 * addresses (and its domains, for an organisation); calls and texts from the
 * phone match its phone numbers.
 */
export async function lastContacts(db: Tx, entityIds: string[] | null = null): Promise<Map<string, LastContact>> {
  const rows = await db<{ entity_id: string; mail_in: Date | null; mail_out: Date | null; meeting: Date | null; phone: Date | null }[]>`
    WITH ids AS (
      SELECT entity_id, kind, value FROM entity_identifier
      WHERE ${entityIds}::uuid[] IS NULL OR entity_id = ANY(${entityIds}::uuid[])
    ),
    touches AS (
      SELECT 'mail_in' AS ch, lower(payload->>'from_email') AS v, max(t_start) AS t FROM observation
      WHERE source = 'gmail' AND coalesce(payload->>'direction', 'in') = 'in' AND payload->>'from_email' IS NOT NULL
      GROUP BY 2
      UNION ALL
      SELECT 'mail_out', lower(x), max(o.t_start) FROM observation o, jsonb_array_elements_text(o.payload->'to_emails') x
      WHERE o.source = 'gmail' AND o.payload->>'direction' = 'sent'
      GROUP BY 2
      UNION ALL
      SELECT 'meeting', a, max(g.t_end) FROM google_event g, unnest(g.attendees) a
      WHERE g.t_end <= now() AND g.status <> 'cancelled' AND coalesce(g.self_response, '') <> 'declined'
      GROUP BY 2
    ),
    calls AS (
      SELECT payload->>'phone' AS v, max(t_start) AS t FROM observation
      WHERE source = 'android' AND payload ? 'phone' GROUP BY 1
    ),
    matched AS (
      SELECT i.entity_id, t.ch, t.t FROM touches t JOIN ids i ON i.kind = 'email' AND i.value = t.v
      UNION ALL
      SELECT i.entity_id, t.ch, t.t FROM touches t JOIN ids i ON i.kind = 'domain' AND i.value = split_part(t.v, '@', 2)
      UNION ALL
      SELECT i.entity_id, 'phone', c.t FROM calls c JOIN ids i ON i.kind = 'phone' AND i.value = c.v
    )
    SELECT entity_id,
           max(t) FILTER (WHERE ch = 'mail_in') AS mail_in,
           max(t) FILTER (WHERE ch = 'mail_out') AS mail_out,
           max(t) FILTER (WHERE ch = 'meeting') AS meeting,
           max(t) FILTER (WHERE ch = 'phone') AS phone
    FROM matched GROUP BY entity_id`;
  const out = new Map<string, LastContact>();
  for (const r of rows) {
    const channels: [Channel, Date | null][] = [["mail_in", r.mail_in], ["mail_out", r.mail_out], ["meeting", r.meeting], ["phone", r.phone]];
    let best: [Channel, Date] | null = null;
    for (const [ch, t] of channels) if (t && (!best || t.getTime() > best[1].getTime())) best = [ch, t];
    out.set(r.entity_id, {
      at: best?.[1].toISOString() ?? null,
      via: best?.[0] ?? null,
      mail_in: r.mail_in?.toISOString() ?? null,
      mail_out: r.mail_out?.toISOString() ?? null,
      meeting: r.meeting?.toISOString() ?? null,
      phone: r.phone?.toISOString() ?? null,
    });
  }
  return out;
}

/** The next time a month/day comes round, on or after `today`. 29 February falls on the 28th in other years. */
export function nextBirthday(month: number, day: number, today: Ymd): Ymd {
  const { y } = parseYmd(today);
  const on = (year: number) => {
    const leap = (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
    const d = month === 2 && day === 29 && !leap ? 28 : day;
    return `${year}-${String(month).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
  };
  const thisYear = on(y);
  return thisYear >= today ? thisYear : on(y + 1);
}

export interface Person {
  id: string;
  kind: string;
  name: string;
  priority: boolean;
  from_google: boolean;
  page: string | null;
  keep_in_touch_days: number | null;
  birthday: string | null;
  /** Days until the next birthday (0 = today). */
  birthday_in: number | null;
  /** Turning this old at the next birthday, when the year is known. */
  turning: number | null;
  last: LastContact;
  /** Whole local days since the last contact, or null if never. */
  days_since: number | null;
  /** Days past the cadence (positive = overdue), or null with no cadence. */
  overdue_by: number | null;
  identifiers: { kind: string; value: string }[];
}

export type PeopleFilter = "important" | "all" | "keep-in-touch" | "overdue" | "birthdays";

export interface PeopleQuery {
  filter?: PeopleFilter;
  q?: string;
  limit?: number;
  /** Birthdays within this many days count as soon. */
  birthdayWindow?: number;
  now?: Date;
}

/** People who matter: a page, a cadence, priority, made by the agent, or in touch within 90 days. */
function important(p: Person): boolean {
  return Boolean(p.page) || p.keep_in_touch_days !== null || p.priority || !p.from_google || (p.days_since !== null && p.days_since <= 90);
}

export async function listPeople(db: Tx, query: PeopleQuery = {}): Promise<{ people: Person[]; total: number; shown: number }> {
  const now = query.now ?? new Date();
  const tz = env.LOR_TZ;
  const today = localYmd(tz, now);
  const window = query.birthdayWindow ?? 7;
  const q = query.q?.trim().toLowerCase() ?? "";

  const rows = await db<(EntityRow & { page: string | null })[]>`
    SELECT e.*, p.slug AS page FROM entity e LEFT JOIN page p ON p.entity_id = e.id ORDER BY lower(e.name)`;
  const identifiers = await db<{ entity_id: string; kind: string; value: string }[]>`
    SELECT entity_id, kind, value FROM entity_identifier ORDER BY kind, value`;
  const idsBy = new Map<string, { kind: string; value: string }[]>();
  for (const i of identifiers) idsBy.set(i.entity_id, [...(idsBy.get(i.entity_id) ?? []), { kind: i.kind, value: i.value }]);
  const last = await lastContacts(db);

  const people: Person[] = rows.map((e) => {
    const l = last.get(e.id) ?? EMPTY;
    const daysSince = l.at ? localDaysBetween(tz, new Date(l.at), now) : null;
    let birthday: string | null = null;
    let birthdayIn: number | null = null;
    let turning: number | null = null;
    if (e.birthday_month && e.birthday_day) {
      birthday = `${e.birthday_year ? `${e.birthday_year}-` : ""}${String(e.birthday_month).padStart(2, "0")}-${String(e.birthday_day).padStart(2, "0")}`;
      const next = nextBirthday(e.birthday_month, e.birthday_day, today);
      const a = parseYmd(today);
      const b = parseYmd(next);
      birthdayIn = Math.round((Date.UTC(b.y, b.m - 1, b.d) - Date.UTC(a.y, a.m - 1, a.d)) / 86_400_000);
      turning = e.birthday_year ? b.y - e.birthday_year : null;
    }
    // Never in touch counts as overdue from the day the cadence could first have been met.
    const overdueBy = e.keep_in_touch_days === null ? null : daysSince === null ? e.keep_in_touch_days : daysSince - e.keep_in_touch_days;
    return {
      id: e.id,
      kind: e.kind,
      name: e.name,
      priority: e.priority,
      from_google: e.google_contact_id !== null,
      page: e.page,
      keep_in_touch_days: e.keep_in_touch_days,
      birthday,
      birthday_in: birthdayIn,
      turning,
      last: l,
      days_since: daysSince,
      overdue_by: overdueBy,
      identifiers: idsBy.get(e.id) ?? [],
    };
  });

  const filter = query.filter ?? "important";
  let shown = people.filter((p) => {
    if (q && !(p.name.toLowerCase().includes(q) || p.identifiers.some((i) => i.value.includes(q)))) return false;
    switch (filter) {
      case "all":
        return true;
      case "keep-in-touch":
        return p.keep_in_touch_days !== null;
      case "overdue":
        return p.overdue_by !== null && p.overdue_by > 0;
      case "birthdays":
        return p.birthday_in !== null && p.birthday_in <= window;
      default:
        return q ? true : important(p);
    }
  });
  if (filter === "overdue") shown.sort((a, b) => (b.overdue_by ?? 0) - (a.overdue_by ?? 0));
  if (filter === "birthdays") shown.sort((a, b) => (a.birthday_in ?? 0) - (b.birthday_in ?? 0));
  const total = shown.length;
  shown = shown.slice(0, query.limit ?? 200);
  return { people: shown, total, shown: shown.length };
}

export function dueLine(p: Person): string {
  const since = p.days_since === null ? "never in touch" : `last in touch ${p.days_since} days ago${p.last.via ? ` (${CHANNEL_LABEL[p.last.via]})` : ""}`;
  return `${p.name}: ${since}; aiming for every ${p.keep_in_touch_days} days (entity ${p.id})`;
}

export function birthdayLine(p: Person): string {
  const when = p.birthday_in === 0 ? "today" : p.birthday_in === 1 ? "tomorrow" : `in ${p.birthday_in} days`;
  return `${p.name}: birthday ${when}${p.turning ? `, turning ${p.turning}` : ""} (entity ${p.id})`;
}

export const CHANNEL_LABEL: Record<Channel, string> = {
  mail_in: "they emailed",
  mail_out: "you emailed",
  meeting: "you met",
  phone: "call or text",
};

/** Days until a date, from today, for the digest. */
export function daysFrom(today: Ymd, date: Ymd): number {
  return Math.round((Date.parse(`${date}T00:00:00Z`) - Date.parse(`${today}T00:00:00Z`)) / 86_400_000);
}

export { addDays };
