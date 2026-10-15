// A small line diff for page history and `--dry-run`. Pages are short, so the
// quadratic LCS table is fine; very long inputs fall back to "replaced".

export interface DiffLine {
  op: " " | "+" | "-";
  text: string;
}

const MAX_CELLS = 4_000_000;

export function diffLines(before: string, after: string): DiffLine[] {
  return diffSeq(before.split("\n"), after.split("\n"));
}

/** The same diff over any sequence of strings (lines, words). */
export function diffSeq(a: string[], b: string[]): DiffLine[] {
  if (a.length * b.length > MAX_CELLS) {
    return [...a.map((text) => ({ op: "-" as const, text })), ...b.map((text) => ({ op: "+" as const, text }))];
  }
  const n = a.length;
  const m = b.length;
  // lcs[i][j] = length of the LCS of a[i..] and b[j..]
  const lcs: Uint32Array[] = Array.from({ length: n + 1 }, () => new Uint32Array(m + 1));
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      lcs[i]![j] = a[i] === b[j] ? lcs[i + 1]![j + 1]! + 1 : Math.max(lcs[i + 1]![j]!, lcs[i]![j + 1]!);
    }
  }
  const out: DiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) {
      out.push({ op: " ", text: a[i]! });
      i++;
      j++;
    } else if (lcs[i + 1]![j]! >= lcs[i]![j + 1]!) {
      out.push({ op: "-", text: a[i++]! });
    } else {
      out.push({ op: "+", text: b[j++]! });
    }
  }
  while (i < n) out.push({ op: "-", text: a[i++]! });
  while (j < m) out.push({ op: "+", text: b[j++]! });
  return out;
}

/** Changed lines with up to `context` unchanged lines around each change. */
export function renderDiff(lines: DiffLine[], context = 2): string {
  const keep = new Set<number>();
  lines.forEach((l, idx) => {
    if (l.op === " ") return;
    for (let k = Math.max(0, idx - context); k <= Math.min(lines.length - 1, idx + context); k++) keep.add(k);
  });
  const out: string[] = [];
  let prev = -1;
  for (const idx of [...keep].sort((x, y) => x - y)) {
    if (prev !== -1 && idx !== prev + 1) out.push("…");
    out.push(`${lines[idx]!.op} ${lines[idx]!.text}`);
    prev = idx;
  }
  return out.join("\n");
}
