// Todoist-style quick add: one line in, a task out. "Book venue #Wedding
// vendors fri @agent" is a task titled "Book venue", counting toward "Wedding
// vendors", due this Friday, given to the agent. `@Name` means it is waiting
// on that person. Pure: the caller passes today's local date and the names.
import { type Ymd, addDays, isYmd, parseYmd } from "./localTime.js";

export interface Named {
  id: string;
  name: string;
}

export type Token =
  | { kind: "parent"; start: number; end: number; text: string; id: string; name: string }
  | { kind: "agent"; start: number; end: number; text: string }
  | { kind: "waiting"; start: number; end: number; text: string; id: string; name: string }
  | { kind: "due"; start: number; end: number; text: string; ymd: Ymd };

export interface Parsed {
  title: string;
  /** The tokens that took effect, in the order they appear. */
  tokens: Token[];
  parentId: string | null;
  due: Ymd | null;
  assignee: "owner" | "agent";
  waitingOnId: string | null;
}

export interface QuickAddOptions {
  today: Ymd;
  parents: Named[];
  people: Named[];
  /** Tokens the owner dismissed, by `tokenKey`: they stay in the title as text. */
  ignore?: ReadonlySet<string>;
}

export const tokenKey = (t: Pick<Token, "kind" | "text">) => `${t.kind}:${t.text.trim().toLowerCase()}`;

const compact = (s: string) => s.toLowerCase().replace(/[^\p{L}\p{N}]/gu, "");
const boundaryAt = (s: string, i: number) => i >= s.length || /[\s,.;:!?)]/.test(s[i]!);
const sigilAt = (s: string, i: number) => (s[i] === "#" || s[i] === "@") && (i === 0 || /\s/.test(s[i - 1]!));

/**
 * The name written right after a sigil: the longest whole name, case-insensitive;
 * else one word that is a name with its spaces taken out ("#weddingvendors"), or
 * the start of exactly one name ("#wedd").
 */
function matchName(rest: string, names: Named[]): { item: Named; length: number } | null {
  const lower = rest.toLowerCase();
  let best: { item: Named; length: number } | null = null;
  for (const n of names) {
    const name = n.name.toLowerCase();
    if (name && lower.startsWith(name) && boundaryAt(rest, name.length) && (!best || name.length > best.length)) best = { item: n, length: name.length };
  }
  if (best) return best;
  const word = /^[^\s]+/.exec(rest)?.[0].replace(/[,.;:!?]+$/, "") ?? "";
  const w = compact(word);
  if (w.length < 2) return null;
  const exact = names.filter((n) => compact(n.name) === w);
  if (exact.length === 1) return { item: exact[0]!, length: word.length };
  const prefixed = names.filter((n) => compact(n.name).startsWith(w));
  return prefixed.length === 1 ? { item: prefixed[0]!, length: word.length } : null;
}

const WEEKDAYS: Record<string, number> = {
  sunday: 0, monday: 1, mon: 1, tuesday: 2, tue: 2, tues: 2, wednesday: 3, weds: 3, thursday: 4, thu: 4, thur: 4, thurs: 4,
  friday: 5, fri: 5, saturday: 6,
  // Only after "on", "by", "due" or "next": as bare words they are English ("sat", "sun cream", "get wed").
  sun: 0, sat: 6, wed: 3,
};
const AMBIGUOUS = new Set(["sun", "sat", "wed"]);
const MONTHS: Record<string, number> = {
  jan: 1, january: 1, feb: 2, february: 2, mar: 3, march: 3, apr: 4, april: 4, may: 5, jun: 6, june: 6,
  jul: 7, july: 7, aug: 8, august: 8, sep: 9, sept: 9, september: 9, oct: 10, october: 10, nov: 11, november: 11, dec: 12, december: 12,
};
const COUNTS: Record<string, number> = { a: 1, an: 1, one: 1, two: 2, three: 3, four: 4, five: 5, six: 6 };

const wd = Object.keys(WEEKDAYS).sort((a, b) => b.length - a.length).join("|");
const mo = Object.keys(MONTHS).sort((a, b) => b.length - a.length).join("|");
const DATE = new RegExp(
  "(?<![\\p{L}\\p{N}'’#@])" +
    "(?:(due|by|on)\\s+)?" +
    "(?:" +
    "(today|tod|tonight)" + // 2
    "|(tomorrow|tmrw|tmr)" + // 3
    "|(next\\s+week|next\\s+month|this\\s+weekend|weekend)" + // 4
    `|(?:(next)\\s+)?(${wd})` + // 5 next, 6 weekday
    "|in\\s+(\\d+|a|an|one|two|three|four|five|six)\\s+(days?|weeks?|months?)" + // 7 n, 8 unit
    `|(${mo})\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?` + // 9 month, 10 day, 11 year
    `|(\\d{1,2})(?:st|nd|rd|th)?\\s+(${mo})\\.?(?:\\s+(\\d{4}))?` + // 12 day, 13 month, 14 year
    "|(\\d{1,2})\\/(\\d{1,2})(?:\\/(\\d{2}|\\d{4}))?" + // 15 month, 16 day, 17 year
    "|(\\d{4}-\\d{2}-\\d{2})" + // 18
    ")" +
    "(?![\\p{L}\\p{N}'’/])",
  "giu",
);

const weekday = (ymd: Ymd) => {
  const { y, m, d } = parseYmd(ymd);
  return new Date(Date.UTC(y, m - 1, d)).getUTCDay();
};
const pad = (n: number) => String(n).padStart(2, "0");

function ymdOf(y: number, m: number, d: number): Ymd | null {
  const s = `${y}-${pad(m)}-${pad(d)}`;
  if (!isYmd(s)) return null;
  const { m: mm, d: dd } = parseYmd(new Date(Date.UTC(y, m - 1, d)).toISOString().slice(0, 10));
  return mm === m && dd === d ? s : null; // 30 Feb is not a date
}

function addMonths(ymd: Ymd, n: number): Ymd {
  const { y, m, d } = parseYmd(ymd);
  const last = new Date(Date.UTC(y, m - 1 + n + 1, 0)).getUTCDate();
  return new Date(Date.UTC(y, m - 1 + n, Math.min(d, last))).toISOString().slice(0, 10);
}

/** A month and day with no year: this year, or next if it has passed. */
function upcoming(today: Ymd, m: number, d: number, year?: string): Ymd | null {
  if (year) return ymdOf(year.length === 2 ? 2000 + Number(year) : Number(year), m, d);
  const { y } = parseYmd(today);
  const s = ymdOf(y, m, d);
  return s && s < today ? ymdOf(y + 1, m, d) : s;
}

function resolveDate(g: (string | undefined)[], today: Ymd): Ymd | null {
  const lc = (i: number) => g[i]?.toLowerCase().replace(/\s+/g, " ");
  const now = weekday(today);
  if (g[2]) return today;
  if (g[3]) return addDays(today, 1);
  const nextMonday = addDays(today, ((8 - now) % 7) || 7);
  if (g[4]) {
    const p = lc(4);
    if (p === "next week") return nextMonday;
    if (p === "next month") {
      const { y, m } = parseYmd(addMonths(today, 1));
      return `${y}-${pad(m)}-01`;
    }
    return now === 0 ? today : addDays(today, (6 - now + 7) % 7);
  }
  if (g[6]) {
    const target = WEEKDAYS[lc(6)!]!;
    if (AMBIGUOUS.has(lc(6)!) && !g[1] && !g[5]) return null;
    if (g[5]) return addDays(nextMonday, (target + 6) % 7);
    return addDays(today, (target - now + 7) % 7);
  }
  if (g[7]) {
    const n = COUNTS[lc(7)!] ?? Number(g[7]);
    const unit = lc(8)!;
    if (unit.startsWith("day")) return addDays(today, n);
    if (unit.startsWith("week")) return addDays(today, 7 * n);
    return addMonths(today, n);
  }
  if (g[9]) return upcoming(today, MONTHS[lc(9)!]!, Number(g[10]), g[11]);
  if (g[12]) return upcoming(today, MONTHS[lc(13)!]!, Number(g[12]), g[14]);
  if (g[15]) return upcoming(today, Number(g[15]), Number(g[16]), g[17]);
  if (g[18]) return isYmd(g[18]) ? g[18] : null;
  return null;
}

export function parseQuickAdd(text: string, opts: QuickAddOptions): Parsed {
  const ignore = opts.ignore ?? new Set<string>();
  const tokens: Token[] = [];
  const taken = (start: number, end: number) => tokens.some((t) => start < t.end && t.start < end);
  const keep = (t: Token) => !ignore.has(tokenKey(t));

  for (let i = 0; i < text.length; i++) {
    if (!sigilAt(text, i)) continue;
    const rest = text.slice(i + 1);
    if (text[i] === "#") {
      if (tokens.some((t) => t.kind === "parent")) continue;
      const m = matchName(rest, opts.parents);
      if (!m) continue;
      const t: Token = { kind: "parent", start: i, end: i + 1 + m.length, text: text.slice(i, i + 1 + m.length), id: m.item.id, name: m.item.name };
      if (keep(t)) tokens.push(t);
    } else {
      if (tokens.some((t) => t.kind === "agent" || t.kind === "waiting")) continue;
      const agent = /^agent(?![\p{L}\p{N}])/iu.exec(rest);
      if (agent) {
        const t: Token = { kind: "agent", start: i, end: i + 6, text: text.slice(i, i + 6) };
        if (keep(t)) tokens.push(t);
        continue;
      }
      const m = matchName(rest, opts.people);
      if (!m) continue;
      const t: Token = { kind: "waiting", start: i, end: i + 1 + m.length, text: text.slice(i, i + 1 + m.length), id: m.item.id, name: m.item.name };
      if (keep(t)) tokens.push(t);
    }
  }

  for (const m of text.matchAll(DATE)) {
    const start = m.index;
    const end = start + m[0].length;
    if (taken(start, end)) continue;
    const ymd = resolveDate([...m], opts.today);
    if (!ymd) continue;
    const t: Token = { kind: "due", start, end, text: m[0], ymd };
    if (!keep(t)) continue;
    tokens.push(t);
    break; // one due date; later date words stay in the title
  }

  tokens.sort((a, b) => a.start - b.start);
  let title = "";
  let at = 0;
  for (const t of tokens) {
    title += text.slice(at, t.start);
    at = t.end;
  }
  title = (title + text.slice(at)).replace(/\s+/g, " ").trim();

  const parent = tokens.find((t) => t.kind === "parent");
  const due = tokens.find((t) => t.kind === "due");
  const waiting = tokens.find((t) => t.kind === "waiting");
  return {
    title,
    tokens,
    parentId: parent?.kind === "parent" ? parent.id : null,
    due: due?.kind === "due" ? due.ymd : null,
    assignee: tokens.some((t) => t.kind === "agent") ? "agent" : "owner",
    waitingOnId: waiting?.kind === "waiting" ? waiting.id : null,
  };
}

/** The `#…` or `@…` being typed at the caret, for the suggestion list. */
export function activeSigil(text: string, caret: number): { sigil: "#" | "@"; start: number; query: string } | null {
  for (let i = caret - 1; i >= 0 && caret - i <= 48; i--) {
    if (sigilAt(text, i)) {
      const query = text.slice(i + 1, caret);
      if (/[#@]/.test(query) || query.split(/\s+/).length > 4) return null;
      return { sigil: text[i] as "#" | "@", start: i, query };
    }
  }
  return null;
}

/** Names that fit what has been typed so far: starts-with first, then contains. */
export function suggest(query: string, names: Named[], limit = 6): Named[] {
  const q = query.trim().toLowerCase();
  const c = compact(query);
  if (q && names.some((n) => n.name.toLowerCase() === q)) return []; // a whole name: Enter adds the task
  const score = (n: Named) => {
    const name = n.name.toLowerCase();
    if (!q) return 1;
    if (name.startsWith(q) || compact(n.name).startsWith(c)) return 3;
    if (name.split(/[^\p{L}\p{N}]+/u).some((w) => w.startsWith(q))) return 2;
    return c && compact(n.name).includes(c) ? 1 : 0;
  };
  return names
    .map((n) => ({ n, s: score(n) }))
    .filter((x) => x.s > 0)
    .sort((a, b) => b.s - a.s || a.n.name.localeCompare(b.n.name))
    .slice(0, limit)
    .map((x) => x.n);
}
