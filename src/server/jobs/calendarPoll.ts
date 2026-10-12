// Calendar poll job (every LOR_CALENDAR_POLL_S, default 300s).
// Read-only gog path; upserts google_event + calendar observations, then
// runs syncCalendarFill per event (fill part lives in PR 5 wiring; the set
// logic is in attribution/fill.ts). Exit 4 → error row, app keeps running.
import { AuthRequiredError, RateLimitedError, gogJson } from "../google/gog.js";
import { GogEventList, mapGogEvent, pollWindow } from "../google/calendarSync.js";

export interface PollResult {
  calendars: number;
  eventsUpserted: number;
  observationsWritten: number;
  window: { from: string; to: string };
}

export function pollArgs(from: Date, to: Date): string[] {
  return [
    "calendar", "events",
    "--all",
    "--from", from.toISOString(),
    "--to", to.toISOString(),
    "--all-pages",
  ];
}

export async function fetchCalendars(): Promise<{ id: string; summary: string }[]> {
  const raw = await gogJson(["calendar", "calendars"]);
  const list = Array.isArray(raw) ? raw : [];
  return list
    .filter((c): c is Record<string, unknown> => typeof c === "object" && c !== null)
    .map((c) => ({ id: String(c.id ?? c.calendarId ?? "primary"), summary: String(c.summary ?? c.id ?? "") }));
}

export async function fetchEvents(from: Date, to: Date): Promise<unknown[]> {
  const raw = await gogJson(pollArgs(from, to));
  return Array.isArray(raw) ? raw : [];
}

export function mapPollEvents(rows: unknown[], profileTz: string) {
  const list = GogEventList.safeParse(rows);
  if (!list.success) return [];
  return list.data
    .map((e) => mapGogEvent(e, "primary", profileTz))
    .filter((m): m is NonNullable<typeof m> => m !== null);
}

export function describePollError(err: unknown): string {
  if (err instanceof AuthRequiredError) return `auth-required: ${err.message}`;
  if (err instanceof RateLimitedError) return `retryable: ${err.message}`;
  return `error: ${(err as Error).message}`;
}

export { pollWindow };
