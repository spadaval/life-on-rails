// Merging and attributing page text on top of the stored whole versions: a
// three-way merge (two edits of one base), taking back one old change while
// keeping later ones, and who last wrote each line. Merging is by line, and
// where both sides touched the same lines (a paragraph is one line), by word.
import { diffSeq } from "./diff.js";

/** For each item of `a`, the index in `b` it survives as, or -1 if it was removed. */
function matchSeq(a: string[], b: string[]): number[] {
  const out: number[] = [];
  let j = 0;
  for (const l of diffSeq(a, b)) {
    if (l.op === " ") out.push(j++);
    else if (l.op === "-") out.push(-1);
    else j++;
  }
  return out;
}

export type Chunk =
  | { kind: "same"; lines: string[] }
  /** Only one side changed this stretch, or both changed it compatibly. */
  | { kind: "merged"; lines: string[]; from: "mine" | "theirs" | "both" }
  /** Both sides changed this stretch, differently. */
  | { kind: "conflict"; base: string[]; mine: string[]; theirs: string[] };

export interface Merge {
  chunks: Chunk[];
  conflicts: number;
  /** The merged text, when nothing conflicts. */
  text: string | null;
}

const same = (a: string[], b: string[]) => a.length === b.length && a.every((x, i) => x === b[i]);
const startsWith = (x: string[], p: string[]) => p.length <= x.length && p.every((v, i) => x[i] === v);
const endsWith = (x: string[], s: string[]) => s.length <= x.length && s.every((v, i) => x[x.length - s.length + i] === v);

/**
 * One stretch both sides changed. Compatible when one side only added next to it: "rewrote
 * the line" and "added a line after it" are both kept.
 */
function settle(bs: string[], ms: string[], ts: string[]): string[] | null {
  if (bs.length === 0) return null;
  if (startsWith(ms, bs)) return [...ts, ...ms.slice(bs.length)];
  if (endsWith(ms, bs)) return [...ms.slice(0, ms.length - bs.length), ...ts];
  if (startsWith(ts, bs)) return [...ms, ...ts.slice(bs.length)];
  if (endsWith(ts, bs)) return [...ts.slice(0, ts.length - bs.length), ...ms];
  return null;
}

/** diff3 over sequences. Edits with nothing unchanged between them count as one stretch. */
function mergeSeq(b: string[], m: string[], t: string[], deeper?: (bs: string[], ms: string[], ts: string[]) => string[] | null): Chunk[] {
  const toMine = matchSeq(b, m);
  const toTheirs = matchSeq(b, t);
  const chunks: Chunk[] = [];
  const push = (c: Chunk) => {
    const last = chunks[chunks.length - 1];
    if (c.kind === "same" && last?.kind === "same") last.lines.push(...c.lines);
    else chunks.push(c);
  };
  let i = 0;
  let j = 0;
  let k = 0;
  while (i < b.length || j < m.length || k < t.length) {
    // An item both sides kept, right where both sides are: unchanged.
    if (i < b.length && toMine[i] === j && toTheirs[i] === k) {
      push({ kind: "same", lines: [b[i]!] });
      i++;
      j++;
      k++;
      continue;
    }
    // The next item both sides kept marks the end of this changed stretch.
    let e = i;
    while (e < b.length && !(toMine[e]! >= j && toTheirs[e]! >= k)) e++;
    const je = e < b.length ? toMine[e]! : m.length;
    const ke = e < b.length ? toTheirs[e]! : t.length;
    const bs = b.slice(i, e);
    const ms = m.slice(j, je);
    const ts = t.slice(k, ke);
    if (same(ms, bs)) push({ kind: "merged", lines: ts, from: "theirs" });
    else if (same(ts, bs)) push({ kind: "merged", lines: ms, from: "mine" });
    else if (same(ms, ts)) push({ kind: "merged", lines: ms, from: "both" });
    else {
      const lines = settle(bs, ms, ts) ?? deeper?.(bs, ms, ts) ?? null;
      push(lines ? { kind: "merged", lines, from: "both" } : { kind: "conflict", base: bs, mine: ms, theirs: ts });
    }
    i = e;
    j = je;
    k = ke;
  }
  return chunks;
}

/** Words and the spaces between them, so joining gives the text back exactly. */
const words = (lines: string[]) => lines.join("\n").split(/(\s+)/).filter(Boolean);

/** The same lines merged word by word; null if words clash too. */
function byWord(bs: string[], ms: string[], ts: string[]): string[] | null {
  const chunks = mergeSeq(words(bs), words(ms), words(ts));
  if (chunks.some((c) => c.kind === "conflict")) return null;
  return chunks.flatMap((c) => (c.kind === "conflict" ? [] : c.lines)).join("").split("\n");
}

/** Two edits of the same base, combined; only stretches both changed in the same words conflict. */
export function merge3(base: string, mine: string, theirs: string): Merge {
  const chunks = mergeSeq(base.split("\n"), mine.split("\n"), theirs.split("\n"), byWord);
  const conflicts = chunks.filter((c) => c.kind === "conflict").length;
  return { chunks, conflicts, text: conflicts ? null : join(chunks, () => []) };
}

/** The merged text, with each conflict settled by `pick`. */
export function join(chunks: Chunk[], pick: (c: Extract<Chunk, { kind: "conflict" }>, index: number) => string[]): string {
  const out: string[] = [];
  let n = 0;
  for (const c of chunks) out.push(...(c.kind === "conflict" ? pick(c, n++) : c.lines));
  return out.join("\n");
}

/**
 * Undo one old change and keep everything since: `now` with the step from `before` to
 * `after` reversed. Null when later edits changed the same words.
 */
export function takeBack(before: string, after: string, now: string): string | null {
  return merge3(after, now, before).text;
}

export interface Author {
  via: string;
  version: number;
}

/** Who last wrote each line of the newest version, walking the versions oldest first. */
export function blame(versions: { version: number; via: string; body: string }[]): Author[] {
  const ordered = [...versions].sort((a, b) => a.version - b.version);
  let prev = "";
  let who: Author[] = [];
  ordered.forEach((v, n) => {
    const fresh = v.body.split("\n").map(() => ({ via: v.via, version: v.version }));
    if (n > 0) {
      matchSeq(prev.split("\n"), v.body.split("\n")).forEach((to, from) => {
        if (to >= 0) fresh[to] = who[from]!;
      });
    }
    who = fresh;
    prev = v.body;
  });
  return who;
}

/** Line marks carried from one text to another: lines that survive keep theirs, new lines get `fresh`. */
export function carry<T>(marks: T[], from: string, to: string, fresh: T): T[] {
  const out: T[] = to.split("\n").map(() => fresh);
  matchSeq(from.split("\n"), to.split("\n")).forEach((j, i) => {
    if (j >= 0 && i < marks.length) out[j] = marks[i]!;
  });
  return out;
}
