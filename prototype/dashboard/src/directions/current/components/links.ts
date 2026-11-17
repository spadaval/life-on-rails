// Linkable state (lor-nde9): a link can name a row by its title as well as its id, since
// ids change each time the demo set loads and a link in shots/dashboard.json must keep working.

/** The row a link names: by id, else by its title, ignoring case. */
export function byRef<T extends { id: string }>(rows: readonly T[], ref: string | undefined | null, title: (row: T) => string): T | null {
  if (!ref) return null;
  const want = ref.trim().toLowerCase();
  return rows.find((r) => r.id === ref) ?? rows.find((r) => title(r).trim().toLowerCase() === want) ?? null;
}

/** The rows `pick=` names, comma-separated: a 1-based position among the rows shown, an id, or a title. */
export function pickRefs<T extends { id: string }>(order: readonly T[], refs: string | undefined, title: (row: T) => string): Set<string> {
  const out = new Set<string>();
  for (const ref of (refs ?? "").split(",").map((r) => r.trim()).filter(Boolean)) {
    const row = /^\d+$/.test(ref) ? order[Number(ref) - 1] : byRef(order, ref, title);
    if (row) out.add(row.id);
  }
  return out;
}
