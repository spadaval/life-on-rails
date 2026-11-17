// Times as the data reads them: every clock and date on screen is in the snapshot's timezone
// (TZ), never the browser's, so NOW sits at the same hour on any machine.
import { addDays, localHm, localTimeOnDay, localYmd } from "../../../../../../src/shared/localTime.js";
import { TZ } from "../../../data/store.js";

export { addDays };

/** HH:MM in TZ. */
export const hm = (t: number | string) => localHm(TZ, new Date(t));
/** YYYY-MM-DD in TZ. */
export const ymd = (t: number | string) => localYmd(TZ, new Date(t));
/** ms at HH:MM on a local day. */
export const at = (day: string, time: string) => localTimeOnDay(TZ, day, time).getTime();

const WD = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const WD_LONG = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
const MON = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const MON_LONG = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];

const parts = (d: string) => {
  const [y, m, dd] = d.split("-").map(Number);
  return { y: y!, m: m!, d: dd!, wd: new Date(Date.UTC(y!, m! - 1, dd!, 12)).getUTCDay() };
};
export const weekday = (d: string) => parts(d).wd;

/** "Tuesday 17 November". */
export function longDate(d: string): string {
  const p = parts(d);
  return `${WD_LONG[p.wd]} ${p.d} ${MON_LONG[p.m - 1]}`;
}

/** A due date said the short way, relative to today: "today", "tomorrow", "Fri", "2 days late", "8 Dec". */
export function dueWords(d: string | null, today: string): string {
  if (!d) return "no date";
  if (d === today) return "today";
  if (d === addDays(today, 1)) return "tomorrow";
  if (d === addDays(today, -1)) return "yesterday";
  const diff = daysBetween(today, d);
  if (diff < 0) return `${-diff} days late`;
  if (diff < 7) return WD[parts(d).wd]!;
  const p = parts(d);
  return `${p.d} ${MON[p.m - 1]}`;
}

/** "Fri 20 Nov". */
export function shortDate(d: string): string {
  const p = parts(d);
  return `${WD[p.wd]} ${p.d} ${MON[p.m - 1]}`;
}

export function daysBetween(a: string, b: string): number {
  return Math.round((Date.UTC(parts(b).y, parts(b).m - 1, parts(b).d) - Date.UTC(parts(a).y, parts(a).m - 1, parts(a).d)) / 86_400_000);
}

/** "1h 40m", "35m", "2h". */
export function dur(ms: number): string {
  const m = Math.max(0, Math.round(ms / 60_000));
  const h = Math.floor(m / 60);
  const r = m % 60;
  if (!h) return `${r}m`;
  return r ? `${h}h ${r}m` : `${h}h`;
}

/** A running clock: "47:12", or "1:47:12". */
export function clock(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const mm = String(Math.floor((s % 3600) / 60)).padStart(h ? 2 : 1, "0");
  const ss = String(s % 60).padStart(2, "0");
  return h ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

/** "3 hours ago", "2 days ago", from ms to now. */
export function ago(t: number, now: number): string {
  const m = Math.round((now - t) / 60_000);
  if (m < 1) return "just now";
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  if (h < 24) return `${h} hour${h === 1 ? "" : "s"} ago`;
  const d = Math.round(h / 24);
  return `${d} day${d === 1 ? "" : "s"} ago`;
}

// ---- reading a date out of typed text, for the search field's "add a task" ----

const DAY_WORDS: Record<string, number> = {
  sun: 0, sunday: 0, mon: 1, monday: 1, tue: 2, tues: 2, tuesday: 2, wed: 3, wednesday: 3,
  thu: 4, thur: 4, thurs: 4, thursday: 4, fri: 5, friday: 5, sat: 6, saturday: 6,
};
const MONTH_WORDS: Record<string, number> = Object.fromEntries(
  MON.flatMap((m, i) => [[m.toLowerCase(), i + 1], [MON_LONG[i]!.toLowerCase(), i + 1]]),
);

/**
 * Find one date in the words: today, tomorrow, a weekday (the next one), next week, 20 nov,
 * nov 20, 2026-11-20. Returns the date and the words with it taken out, or null.
 */
export function readDate(text: string, today: string): { date: string; rest: string; said: string } | null {
  const words = text.split(/\s+/).filter(Boolean);
  const lower = words.map((w) => w.toLowerCase().replace(/[.,]$/, ""));
  const take = (i: number, n: number, date: string) => ({
    date,
    said: words.slice(i, i + n).join(" "),
    rest: [...words.slice(0, i), ...words.slice(i + n)].join(" "),
  });
  const year = Number(today.slice(0, 4));
  const ymdOf = (m: number, d: number) => {
    let y = year;
    const s = (yy: number) => `${yy}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
    if (s(y) < today) y += 1;
    return s(y);
  };
  for (let i = 0; i < lower.length; i++) {
    const w = lower[i]!;
    if (w === "today") return take(i, 1, today);
    if (w === "tomorrow" || w === "tmr") return take(i, 1, addDays(today, 1));
    if (w === "next" && lower[i + 1] === "week") return take(i, 2, addDays(today, 7));
    if (/^\d{4}-\d{2}-\d{2}$/.test(w)) return take(i, 1, w);
    if (w in DAY_WORDS) {
      const diff = (DAY_WORDS[w]! - weekday(today) + 7) % 7 || 7;
      return take(i, 1, addDays(today, diff));
    }
    const n = Number(w);
    if (Number.isInteger(n) && n >= 1 && n <= 31 && lower[i + 1] && lower[i + 1]! in MONTH_WORDS) return take(i, 2, ymdOf(MONTH_WORDS[lower[i + 1]!]!, n));
    if (w in MONTH_WORDS && lower[i + 1] && /^\d{1,2}$/.test(lower[i + 1]!)) return take(i, 2, ymdOf(MONTH_WORDS[w]!, Number(lower[i + 1])));
  }
  return null;
}
