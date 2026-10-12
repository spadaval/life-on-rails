// Calendar sync mapping: gog JSON → google_event rows + calendar observations.
// Pure (no DB, no spawn) so it runs on golden fixtures. DB upserts live in
// jobs/calendarPoll.ts.
import { z } from "zod";

// Defensive shape: mirrors the Google Calendar event resource as emitted by
// `gog calendar events --json --results-only`. Unknown fields ride along in
// payload; only the documented subset is read.
export const GogEvent = z.object({
  id: z.string(),
  calendarId: z.string().optional(),
  summary: z.string().optional(),
  description: z.string().optional(),
  status: z.enum(["confirmed", "tentative", "cancelled"]).optional().default("confirmed"),
  htmlLink: z.string().optional(),
  iCalUID: z.string().optional(),
  recurringEventId: z.string().optional(),
  start: z
    .object({ dateTime: z.string().optional(), date: z.string().optional(), timeZone: z.string().optional() })
    .optional(),
  end: z
    .object({ dateTime: z.string().optional(), date: z.string().optional(), timeZone: z.string().optional() })
    .optional(),
  location: z.string().optional(),
  attendees: z.array(z.object({ email: z.string().optional() }).passthrough()).optional(),
}).passthrough();
export type GogEvent = z.infer<typeof GogEvent>;

export const GogEventList = z.array(GogEvent);
export type GogEventList = z.infer<typeof GogEvent>;

export interface MappedEvent {
  eventId: string;
  icalUid: string | null;
  title: string | null;
  tStart: Date;
  tEnd: Date;
  allDay: boolean;
  status: string;
  htmlLink: string | null;
  /** Attendee count only — never emails (fixtures must not carry third-party PII). */
  attendeesCount: number;
}

function parseBound(dateTime: string | undefined, date: string | undefined, tz: string, isEnd: boolean): Date | null {
  if (dateTime) {
    const d = new Date(dateTime);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  if (date) {
    // All-day: date is YYYY-MM-DD; interpret day bounds in the profile tz.
    // Represented as UTC midnight ± day; caller replaces with tz day bounds.
    const [y, m, dd] = date.split("-").map(Number);
    if (!y || !m || !dd) return null;
    void tz;
    const base = Date.UTC(y, m - 1, dd);
    return new Date(isEnd ? base : base);
  }
  return null;
}

export function mapGogEvent(row: unknown, fallbackCalendarId: string, profileTz: string): MappedEvent | null {
  const parsed = GogEvent.safeParse(row);
  if (!parsed.success) return null;
  const e = parsed.data;
  const allDay = !!(e.start?.date ?? e.end?.date);
  let tStart = parseBound(e.start?.dateTime, e.start?.date, profileTz, false);
  let tEnd = parseBound(e.end?.dateTime, e.end?.date ?? e.start?.date, profileTz, true);
  if (!tStart || !tEnd) return null;
  if (allDay) {
    // End date in Google is exclusive: [start_day, end_day). Day bounds are
    // resolved against the profile tz at the poll layer (dayBoundsInTz).
    tEnd = new Date(tEnd.getTime());
  }
  if (!(tEnd.getTime() > tStart.getTime()) && !allDay) return null;
  return {
    eventId: e.id,
    icalUid: e.iCalUID ?? null,
    title: e.summary ?? "(no title)",
    tStart,
    tEnd,
    allDay,
    status: e.status ?? "confirmed",
    htmlLink: e.htmlLink ?? null,
    attendeesCount: e.attendees?.length ?? 0,
  };
}

// --- Poll window: start of local yesterday → end of local +2 days ---

export function tzOffsetMinutes(tz: string, at: Date): number {
  const dtf = new Intl.DateTimeFormat("en-US", {
    timeZone: tz,
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
    hour12: false,
  });
  const parts = Object.fromEntries(dtf.formatToParts(at).map((p) => [p.type, p.value]));
  const asUtc = Date.UTC(
    Number(parts.year), Number(parts.month) - 1, Number(parts.day),
    Number(parts.hour) % 24, Number(parts.minute), Number(parts.second),
  );
  return Math.round((asUtc - at.getTime()) / 60_000);
}

export function localYmd(tz: string, at: Date): { y: number; m: number; d: number } {
  const dtf = new Intl.DateTimeFormat("en-CA", { timeZone: tz, year: "numeric", month: "2-digit", day: "2-digit" });
  const [y, m, d] = dtf.format(at).split("-").map(Number);
  return { y: y!, m: m!, d: d! };
}

/** Local midnight for a tz-local YMD (iterated twice for DST boundaries). */
export function localMidnight(tz: string, y: number, m: number, d: number): Date {
  let guess = Date.UTC(y, m - 1, d);
  for (let i = 0; i < 2; i++) guess = Date.UTC(y, m - 1, d) - tzOffsetMinutes(tz, new Date(guess)) * 60_000;
  const off = tzOffsetMinutes(tz, new Date(guess + 12 * 3600_000));
  return new Date(Date.UTC(y, m - 1, d) - off * 60_000);
}

export function pollWindow(now: Date, tz: string): { from: Date; to: Date } {
  const day = 24 * 3600_000;
  const { y, m, d } = localYmd(tz, now);
  const todayStart = localMidnight(tz, y, m, d);
  return {
    from: new Date(todayStart.getTime() - day),
    to: new Date(todayStart.getTime() + 3 * day),
  };
}

/** Resolve an all-day [start_day, end_day-exclusive] pair to tz day bounds. */
export function allDayBounds(startDate: string, endDateExclusive: string, tz: string): { tStart: Date; tEnd: Date } {
  const [sy, sm, sd] = startDate.split("-").map(Number);
  const [ey, em, ed] = endDateExclusive.split("-").map(Number);
  return {
    tStart: localMidnight(tz, sy!, sm!, sd!),
    tEnd: localMidnight(tz, ey!, em!, ed!),
  };
}
