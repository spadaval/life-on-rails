// Local-day arithmetic in a named time zone, on Intl only (no Temporal on
// this box). Everything that needs a day boundary goes through here. Days are
// 23 or 25 hours long twice a year, so never add 24 h to move a day.

/** A local calendar date, "2026-10-14". */
export type Ymd = string;

const dtfCache = new Map<string, Intl.DateTimeFormat>();
function partsFormat(tz: string): Intl.DateTimeFormat {
  let f = dtfCache.get(tz);
  if (!f) {
    f = new Intl.DateTimeFormat("en-US", {
      timeZone: tz,
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit", second: "2-digit",
      hourCycle: "h23",
    });
    dtfCache.set(tz, f);
  }
  return f;
}

function wallParts(tz: string, at: Date): Record<string, number> {
  const out: Record<string, number> = {};
  for (const p of partsFormat(tz).formatToParts(at)) {
    if (p.type !== "literal") out[p.type] = Number(p.value);
  }
  return out;
}

/** Minutes east of UTC at that instant (PDT = -420). */
export function tzOffsetMinutes(tz: string, at: Date): number {
  const p = wallParts(tz, at);
  const asUtc = Date.UTC(p.year!, p.month! - 1, p.day!, p.hour!, p.minute!, p.second!);
  return Math.round((asUtc - at.getTime()) / 60_000);
}

const pad = (n: number, w = 2) => String(n).padStart(w, "0");

export function localYmd(tz: string, at: Date): Ymd {
  const p = wallParts(tz, at);
  return `${pad(p.year!, 4)}-${pad(p.month!)}-${pad(p.day!)}`;
}

/** "HH:MM" on the wall clock at that instant. */
export function localHm(tz: string, at: Date): string {
  const p = wallParts(tz, at);
  return `${pad(p.hour!)}:${pad(p.minute!)}`;
}

export function parseYmd(ymd: Ymd): { y: number; m: number; d: number } {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd);
  if (!m) throw new Error(`bad date: ${ymd}`);
  return { y: Number(m[1]), m: Number(m[2]), d: Number(m[3]) };
}

export function isYmd(s: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(s) && !Number.isNaN(Date.parse(`${s}T00:00:00Z`));
}

/** Calendar arithmetic: the date n days after ymd. */
export function addDays(ymd: Ymd, n: number): Ymd {
  const { y, m, d } = parseYmd(ymd);
  return new Date(Date.UTC(y, m - 1, d + n)).toISOString().slice(0, 10);
}

/** The instant a wall-clock time ("HH:MM") happens on a local date. */
export function localTimeOnDay(tz: string, ymd: Ymd, hm = "00:00"): Date {
  const { y, m, d } = parseYmd(ymd);
  const [hh, mm] = hm.split(":").map(Number);
  const wall = Date.UTC(y, m - 1, d, hh ?? 0, mm ?? 0);
  let t = wall - tzOffsetMinutes(tz, new Date(wall)) * 60_000;
  t = wall - tzOffsetMinutes(tz, new Date(t)) * 60_000;
  return new Date(t);
}

export function localDayStart(tz: string, ymd: Ymd): Date {
  return localTimeOnDay(tz, ymd, "00:00");
}

export function localDayBounds(tz: string, ymd: Ymd): { start: Date; end: Date } {
  return { start: localDayStart(tz, ymd), end: localDayStart(tz, addDays(ymd, 1)) };
}

/** Monday of the ISO week containing ymd. */
export function weekStart(ymd: Ymd): Ymd {
  const { y, m, d } = parseYmd(ymd);
  const dow = (new Date(Date.UTC(y, m - 1, d)).getUTCDay() + 6) % 7; // Mon = 0
  return addDays(ymd, -dow);
}

/** ISO week label, "2026-W42". */
export function isoWeek(ymd: Ymd): string {
  const { y, m, d } = parseYmd(ymd);
  const date = new Date(Date.UTC(y, m - 1, d));
  const dow = (date.getUTCDay() + 6) % 7;
  date.setUTCDate(date.getUTCDate() - dow + 3); // Thursday of this week
  const firstThursday = new Date(Date.UTC(date.getUTCFullYear(), 0, 4));
  const week = 1 + Math.round(((date.getTime() - firstThursday.getTime()) / 86_400_000 - 3 + ((firstThursday.getUTCDay() + 6) % 7)) / 7);
  return `${date.getUTCFullYear()}-W${pad(week)}`;
}

/** Whole local days from `from` to `to` (0 when the same local date). */
export function localDaysBetween(tz: string, from: Date, to: Date): number {
  const a = parseYmd(localYmd(tz, from));
  const b = parseYmd(localYmd(tz, to));
  return Math.round((Date.UTC(b.y, b.m - 1, b.d) - Date.UTC(a.y, a.m - 1, a.d)) / 86_400_000);
}
