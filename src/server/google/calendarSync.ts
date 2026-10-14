// Calendar poll: mirror the visible window of the owner's calendars into
// google_event, and record one observation per event. The full-window poll is
// also the retract path: an event that is no longer returned was cancelled or
// moved out of the window.
import type { Sql } from "../../db/client.js";
import { addDays, localDayStart, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { errorMessage } from "../log.js";
import { stableId } from "../observations.js";
import { AuthRequiredError } from "./auth.js";
import type { CalendarPort, GEvent } from "./client.js";

export interface MappedEvent {
  calendarId: string;
  eventId: string;
  title: string | null;
  tStart: Date;
  tEnd: Date;
  allDay: boolean;
  status: "confirmed" | "tentative" | "cancelled";
  transparency: "opaque" | "transparent";
  eventType: string;
  /** The owner's RSVP when the event has attendees, else null. */
  selfResponse: string | null;
  htmlLink: string | null;
  attendees: number;
}

export function mapEvent(calendarId: string, e: GEvent, tz: string): MappedEvent | null {
  let tStart: Date;
  let tEnd: Date;
  let allDay = false;
  if (e.start?.dateTime && e.end?.dateTime) {
    tStart = new Date(e.start.dateTime);
    tEnd = new Date(e.end.dateTime);
  } else if (e.start?.date && e.end?.date) {
    // All-day: [start date, end date) in the owner's zone.
    allDay = true;
    tStart = localDayStart(tz, e.start.date);
    tEnd = localDayStart(tz, e.end.date);
  } else {
    return null;
  }
  if (Number.isNaN(tStart.getTime()) || Number.isNaN(tEnd.getTime()) || !(tEnd.getTime() > tStart.getTime())) return null;
  return {
    calendarId,
    eventId: e.id,
    title: e.summary ?? null,
    tStart,
    tEnd,
    allDay,
    status: e.status,
    transparency: e.transparency,
    eventType: e.eventType,
    selfResponse: e.attendees?.find((a) => a.self)?.responseStatus ?? null,
    htmlLink: e.htmlLink ?? null,
    attendees: e.attendees?.length ?? 0,
  };
}

/**
 * Does this event fill the actual lane? Timed, not cancelled, not declined by
 * the owner, not marked free, and an ordinary event (or one Gmail created from
 * a booking). All-day events, focus time, out-of-office, and working-location
 * entries show in the planned lane only.
 */
export function fillsActual(e: {
  allDay: boolean;
  status: string;
  transparency: string;
  eventType: string;
  selfResponse: string | null;
}): boolean {
  if (e.allDay || e.status === "cancelled") return false;
  if (e.selfResponse === "declined") return false;
  if (e.transparency === "transparent") return false;
  return e.eventType === "default" || e.eventType === "fromGmail";
}

/** Start of local yesterday → end of local day +2. */
export function pollWindow(now: Date, tz: string): { from: Date; to: Date } {
  const today = localYmd(tz, now);
  return { from: localDayStart(tz, addDays(today, -1)), to: localDayStart(tz, addDays(today, 3)) };
}

export interface CalendarSyncStats {
  calendars: number;
  events: number;
  changed: number;
  cancelled: number;
  errors: string[];
  [key: string]: unknown;
}

export async function syncCalendars(db: Sql, port: CalendarPort, now = new Date()): Promise<CalendarSyncStats> {
  const tz = env.LOR_TZ;
  const { from, to } = pollWindow(now, tz);
  const calendars = (await port.listCalendars()).filter((c) => c.selected || c.primary);
  const stats: CalendarSyncStats = { calendars: calendars.length, events: 0, changed: 0, cancelled: 0, errors: [] };

  for (const cal of calendars) {
    let events: MappedEvent[];
    try {
      events = (await port.listEvents(cal.id, from, to))
        .map((e) => mapEvent(cal.id, e, tz))
        .filter((e): e is MappedEvent => e !== null);
    } catch (err) {
      if (err instanceof AuthRequiredError) throw err;
      const message = errorMessage(err);
      stats.errors.push(`${cal.summary || cal.id}: ${message}`);
      await db`INSERT INTO google_sync_state (calendar_id, summary, last_error) VALUES (${cal.id}, ${cal.summary}, ${message})
        ON CONFLICT (calendar_id) DO UPDATE SET summary = EXCLUDED.summary, last_error = EXCLUDED.last_error`;
      continue;
    }
    stats.events += events.length;

    await db.begin(async (tx) => {
      await tx`INSERT INTO google_sync_state (calendar_id, summary, last_polled_at, last_error)
        VALUES (${cal.id}, ${cal.summary}, ${now}, NULL)
        ON CONFLICT (calendar_id) DO UPDATE SET summary = EXCLUDED.summary, last_polled_at = EXCLUDED.last_polled_at, last_error = NULL`;
      for (const e of events) {
        // updated_at moves only when something the owner would notice changed.
        const changed = await tx`
          INSERT INTO google_event (calendar_id, event_id, title, t_start, t_end, all_day, status, transparency, event_type, self_response, html_link, payload, updated_at)
          VALUES (${e.calendarId}, ${e.eventId}, ${e.title}, ${e.tStart}, ${e.tEnd}, ${e.allDay}, ${e.status}, ${e.transparency},
            ${e.eventType}, ${e.selfResponse}, ${e.htmlLink}, ${tx.json({ attendees: e.attendees })}, ${now})
          ON CONFLICT (calendar_id, event_id) DO UPDATE SET
            title = EXCLUDED.title, t_start = EXCLUDED.t_start, t_end = EXCLUDED.t_end, all_day = EXCLUDED.all_day,
            status = EXCLUDED.status, transparency = EXCLUDED.transparency, event_type = EXCLUDED.event_type,
            self_response = EXCLUDED.self_response, html_link = EXCLUDED.html_link, payload = EXCLUDED.payload,
            updated_at = EXCLUDED.updated_at
          WHERE (google_event.title, google_event.t_start, google_event.t_end, google_event.all_day, google_event.status,
                 google_event.transparency, google_event.event_type, google_event.self_response)
            IS DISTINCT FROM (EXCLUDED.title, EXCLUDED.t_start, EXCLUDED.t_end, EXCLUDED.all_day, EXCLUDED.status,
                 EXCLUDED.transparency, EXCLUDED.event_type, EXCLUDED.self_response)
          RETURNING event_id`;
        if (changed.length > 0) {
          stats.changed++;
          const id = stableId("calendar", `${e.calendarId}\n${e.eventId}`);
          await tx`
            INSERT INTO observation (id, t_start, t_end, device, source, calendar_id, title, payload)
            VALUES (${id}, ${e.tStart}, ${e.tEnd}, 'buildbot', 'calendar', ${e.calendarId}, ${e.title},
              ${tx.json({ event_id: e.eventId, status: e.status, all_day: e.allDay, attendees: e.attendees })})
            ON CONFLICT (id) DO UPDATE SET t_start = EXCLUDED.t_start, t_end = EXCLUDED.t_end,
              title = EXCLUDED.title, payload = EXCLUDED.payload`;
        }
      }
      // In the window before, not returned now: cancelled, or moved out of it.
      const seen = events.map((e) => e.eventId);
      const gone = await tx`
        UPDATE google_event SET status = 'cancelled', updated_at = ${now}
        WHERE calendar_id = ${cal.id} AND status <> 'cancelled' AND t_start < ${to} AND t_end > ${from}
          AND NOT (event_id = ANY(${seen}::text[]))
        RETURNING event_id`;
      stats.cancelled += gone.length;
    });
  }

  // A calendar the owner unticked: its events stop counting.
  const active = calendars.map((c) => c.id);
  const dropped = await db`
    UPDATE google_event SET status = 'cancelled', updated_at = ${now}
    WHERE status <> 'cancelled' AND t_end > ${from} AND NOT (calendar_id = ANY(${active}::text[]))
    RETURNING event_id`;
  stats.cancelled += dropped.length;

  if (stats.errors.length > 0 && stats.errors.length === calendars.length) {
    throw new Error(`every calendar failed: ${stats.errors[0]}`);
  }
  return stats;
}
