// The calls LoR makes to Google, behind ports so the sync logic is tested
// against recorded responses with no network.
import { z } from "zod";
import { AuthRequiredError, RetryableError, accessToken, forgetAccessToken } from "./auth.js";

async function googleGet(url: string, init: RequestInit = {}): Promise<unknown> {
  for (let attempt = 0; attempt < 2; attempt++) {
    const token = await accessToken();
    const res = await fetch(url, { ...init, headers: { ...(init.headers ?? {}), authorization: `Bearer ${token}` } });
    if (res.status === 401 && attempt === 0) {
      forgetAccessToken();
      continue;
    }
    const text = await res.text();
    if (res.ok) return text ? JSON.parse(text) : {};
    const where = new URL(url).pathname;
    if (res.status === 401) throw new AuthRequiredError(`401 from ${where}`);
    if (res.status === 403 && /insufficient|scope|PERMISSION_DENIED/i.test(text) && !/rateLimit|quota/i.test(text)) {
      throw new AuthRequiredError(`403 from ${where}: the token lacks a scope`);
    }
    if (res.status === 429 || res.status >= 500 || /rateLimitExceeded|quotaExceeded/.test(text)) {
      throw new RetryableError(`${res.status} from ${where}`);
    }
    throw new Error(`Google ${res.status} from ${where}: ${text.slice(0, 300)}`);
  }
  throw new AuthRequiredError("token rejected twice");
}

async function paged<T>(base: string, params: Record<string, string>, item: z.ZodType<T>): Promise<T[]> {
  const out: T[] = [];
  let pageToken: string | undefined;
  do {
    const qs = new URLSearchParams({ ...params, ...(pageToken ? { pageToken } : {}) });
    const page = z.object({ items: z.array(z.unknown()).default([]), nextPageToken: z.string().optional() }).parse(await googleGet(`${base}?${qs}`));
    for (const raw of page.items) out.push(item.parse(raw));
    pageToken = page.nextPageToken;
  } while (pageToken);
  return out;
}

// ---- Calendar ----

export const GCalendar = z.object({
  id: z.string(),
  summary: z.string().default(""),
  primary: z.boolean().default(false),
  selected: z.boolean().default(false),
  accessRole: z.string().default("reader"),
});
export type GCalendar = z.infer<typeof GCalendar>;

const EventTime = z.object({ dateTime: z.string().optional(), date: z.string().optional(), timeZone: z.string().optional() });

export const GEvent = z.object({
  id: z.string(),
  status: z.enum(["confirmed", "tentative", "cancelled"]).default("confirmed"),
  summary: z.string().optional(),
  htmlLink: z.string().optional(),
  start: EventTime.optional(),
  end: EventTime.optional(),
  transparency: z.enum(["opaque", "transparent"]).default("opaque"),
  eventType: z.string().default("default"),
  attendees: z.array(z.object({ email: z.string().optional(), self: z.boolean().optional(), responseStatus: z.string().optional() })).optional(),
  recurringEventId: z.string().optional(),
});
export type GEvent = z.infer<typeof GEvent>;

export interface CalendarPort {
  listCalendars(): Promise<GCalendar[]>;
  /** Single instances (recurring events expanded) overlapping [timeMin, timeMax). */
  listEvents(calendarId: string, timeMin: Date, timeMax: Date): Promise<GEvent[]>;
}

const CAL = "https://www.googleapis.com/calendar/v3";

export const googleCalendar: CalendarPort = {
  listCalendars: () => paged(`${CAL}/users/me/calendarList`, { maxResults: "250" }, GCalendar),
  listEvents: (calendarId, timeMin, timeMax) =>
    paged(
      `${CAL}/calendars/${encodeURIComponent(calendarId)}/events`,
      { singleEvents: "true", orderBy: "startTime", maxResults: "250", timeMin: timeMin.toISOString(), timeMax: timeMax.toISOString() },
      GEvent,
    ),
};

// ---- Tasks ----

export const GTaskList = z.object({ id: z.string(), title: z.string().default("") });
export type GTaskList = z.infer<typeof GTaskList>;

export const GTask = z.object({
  id: z.string(),
  title: z.string().default(""),
  notes: z.string().optional(),
  status: z.enum(["needsAction", "completed"]).default("needsAction"),
  /** Date only, as midnight UTC of the due day. */
  due: z.string().optional(),
  completed: z.string().optional(),
  deleted: z.boolean().default(false),
  hidden: z.boolean().default(false),
  parent: z.string().optional(),
  updated: z.string().optional(),
});
export type GTask = z.infer<typeof GTask>;

export interface TasksPort {
  listTaskLists(): Promise<GTaskList[]>;
  /** Every task in the list, including completed, hidden, and deleted ones. */
  listTasks(taskListId: string): Promise<GTask[]>;
}

const TASKS = "https://tasks.googleapis.com/tasks/v1";

export const googleTasks: TasksPort = {
  listTaskLists: () => paged(`${TASKS}/users/@me/lists`, { maxResults: "100" }, GTaskList),
  listTasks: (taskListId) =>
    paged(
      `${TASKS}/lists/${encodeURIComponent(taskListId)}/tasks`,
      { maxResults: "100", showCompleted: "true", showHidden: "true", showDeleted: "true" },
      GTask,
    ),
};

export { googleGet };
