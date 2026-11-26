// When the phone has stopped reporting, Today is showing an untrue day, so
// every page's health line says so (lor-0v56).

/**
 * How long the phone may go without an upload before it counts as silent.
 * Chosen from the real database's arrivals, 2026-11-12 to 2026-11-26: the phone
 * uploads every 15 minutes (median gap), dozes to 30 minutes overnight (longest
 * gap 00:00 to 08:00), and its longest ordinary gap was 46 minutes. Two hours
 * is well past all of those; the gaps longer than it (14h and 10 days) were outages.
 */
export const PHONE_SILENT_AFTER_MS = 2 * 3600_000;

/** How long the phone has been silent, or null when it reported recently or never has (a fresh database is not a fault). */
export function phoneSilentMs(lastReport: string | null, nowMs: number): number | null {
  if (!lastReport) return null;
  const gap = nowMs - new Date(lastReport).getTime();
  return gap > PHONE_SILENT_AFTER_MS ? gap : null;
}

/** "5h", or "3d" past two days. */
export function silentFor(ms: number): string {
  const h = Math.floor(ms / 3600_000);
  return h < 48 ? `${h}h` : `${Math.floor(h / 24)}d`;
}

/** The sidebar's health line: jobs that need a look first, then a silent phone. `unwell` null means the database could not be read. */
export function healthLine(unwell: string[] | null, silentMs: number | null): { ok: boolean; text: string } {
  if (unwell === null) return { ok: false, text: "Database unreachable" };
  const parts: string[] = [];
  if (unwell.length > 0) parts.push(`${unwell.length} ${unwell.length === 1 ? "job needs" : "jobs need"} a look`);
  if (silentMs !== null) parts.push(`${parts.length ? "phone" : "Phone"} silent for ${silentFor(silentMs)}`);
  return parts.length ? { ok: false, text: parts.join(" · ") } : { ok: true, text: "All senses reporting" };
}
