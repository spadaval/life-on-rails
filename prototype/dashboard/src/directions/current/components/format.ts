// Display helpers shared by the routes. Times render in the browser's zone,
// which is the owner's.
export function ago(iso: string | null, now = Date.now()): string {
  if (!iso) return "never";
  const s = Math.max(0, Math.round((now - new Date(iso).getTime()) / 1000));
  if (s < 90) return `${s}s ago`;
  if (s < 90 * 60) return `${Math.round(s / 60)}m ago`;
  if (s < 36 * 3600) return `${Math.round(s / 3600)}h ago`;
  return `${Math.round(s / 86400)}d ago`;
}

export function clock(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

export function duration(seconds: number): string {
  const m = Math.round(seconds / 60);
  if (m < 60) return `${m}m`;
  const h = Math.floor(m / 60);
  const rest = m % 60;
  return rest ? `${h}h ${rest}m` : `${h}h`;
}

export function pct(x: number | null | undefined): string {
  return x === null || x === undefined ? "—" : `${Math.round(x * 100)}%`;
}

const ymdUtc = (ymd: string) => new Date(`${ymd}T12:00:00Z`);
const dayDiff = (a: string, b: string) => Math.round((ymdUtc(a).getTime() - ymdUtc(b).getTime()) / 86_400_000);

/** "15 Oct", for a local calendar date. */
export const shortDate = (ymd: string) => new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", day: "numeric", month: "short" }).format(ymdUtc(ymd));

/** A due date the way a list reads it: Today, Tomorrow, a weekday within the week, else the date. */
export function dueLabel(ymd: string, today: string): string {
  const d = dayDiff(ymd, today);
  if (d === 0) return "Today";
  if (d === 1) return "Tomorrow";
  if (d === -1) return "Yesterday";
  if (d > 1 && d < 7) return new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "short" }).format(ymdUtc(ymd));
  return shortDate(ymd);
}

/** "Fri 16 Oct". */
export const longDate = (ymd: string) =>
  new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", weekday: "short", day: "numeric", month: "short" }).format(ymdUtc(ymd));
