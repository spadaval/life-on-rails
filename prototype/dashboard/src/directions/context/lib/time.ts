// Clock and date words, always in the snapshot's time zone (not the browser's).
import { addDays, localHm, localTimeOnDay, localYmd } from "../../../../../../src/shared/localTime.js";
import { TZ } from "../../../data/store.js";

export const hm = (ms: number | string) => localHm(TZ, new Date(ms));
export const ymdOf = (ms: number | string) => localYmd(TZ, new Date(ms));
/** ms of HH:MM on the local day `ymd`. */
export const at = (ymd: string, time: string) => localTimeOnDay(TZ, ymd, time).getTime();

const WD = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MO = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const LONG_WD = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

function parts(ymd: string) {
  const [y, m, d] = ymd.split("-").map(Number) as [number, number, number];
  return { y, m, d, wd: new Date(Date.UTC(y, m - 1, d)).getUTCDay() };
}
export const weekday = (ymd: string) => WD[parts(ymd).wd]!;
export const shortDate = (ymd: string) => {
  const p = parts(ymd);
  return `${WD[p.wd]} ${p.d} ${MO[p.m - 1]}`;
};
export const longDate = (ymd: string) => {
  const p = parts(ymd);
  return `${LONG_WD[p.wd]} ${p.d} ${MO[p.m - 1]}`;
};

/** A due date as words, against today: "today", "tomorrow", "Thu", "Fri 27 Nov", "2 days late". */
export function dueWords(due: string, today: string): string {
  if (due === today) return "today";
  if (due === addDays(today, 1)) return "tomorrow";
  if (due < today) {
    const late = Math.round((Date.parse(today) - Date.parse(due)) / 86_400_000);
    return late === 1 ? "yesterday" : `${late} days late`;
  }
  if (due <= addDays(today, 6)) return weekday(due);
  return shortDate(due);
}

/** 47 min, 2 h 05, 1 h. */
export function dur(ms: number): string {
  const min = Math.max(0, Math.round(ms / 60_000));
  if (min < 60) return `${min} min`;
  const h = Math.floor(min / 60);
  const m = min % 60;
  return m ? `${h} h ${String(m).padStart(2, "0")}` : `${h} h`;
}
/** Hours to one decimal: 18.7 h. */
export const hours = (h: number) => `${Math.round(h * 10) / 10} h`;

/** A running clock: 0:47:12. */
export function clock(ms: number): string {
  const s = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  return `${h}:${String(m).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
}

/** "12 min ago", "3 days ago". */
export function ago(then: number, now: number): string {
  const min = Math.round((now - then) / 60_000);
  if (min < 1) return "just now";
  if (min < 60) return `${min} min ago`;
  const h = Math.round(min / 60);
  if (h < 24) return `${h} h ago`;
  const d = Math.round(h / 24);
  return d === 1 ? "yesterday" : `${d} days ago`;
}

/** The date a word names, from today: today, tomorrow, mon..sun (the next one), "next week". */
export function dateWord(word: string, today: string): string | null {
  const w = word.toLowerCase();
  if (w === "today" || w === "tonight") return today;
  if (w === "tomorrow" || w === "tmr") return addDays(today, 1);
  const i = ["sun", "mon", "tue", "wed", "thu", "fri", "sat"].findIndex((d) => w.startsWith(d) && /^[a-z]+$/.test(w) && ("sunday monday tuesday wednesday thursday friday saturday".includes(w) || w.length === 3));
  if (i >= 0) {
    const cur = parts(today).wd;
    const ahead = (i - cur + 7) % 7 || 7;
    return addDays(today, ahead);
  }
  return null;
}
