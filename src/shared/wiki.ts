// Notes are a wiki: pages link with [[slug]] or [[slug|label]]. The server
// stores links as text; these read them back out, and place the pages for the
// graph view.

const LINK = /\[\[([a-z0-9][a-z0-9/-]*)(?:\|([^\]]+))?\]\]/g;

/** The pages a body links to, each once, in the order they first appear. */
export function wikiLinks(body: string): string[] {
  return [...new Set([...body.matchAll(LINK)].map((m) => m[1]!))];
}

/** Rewrite every [[slug]] in a body with `to(slug, label)`. */
export function replaceWikiLinks(body: string, to: (slug: string, label: string | undefined) => string): string {
  return body.replace(LINK, (_m, slug: string, label?: string) => to(slug, label));
}

/** The folder a page sits in: what comes before the first slash, or "" at the top. */
export const folderOf = (slug: string) => (slug.includes("/") ? slug.slice(0, slug.indexOf("/")) : "");

export interface Point {
  x: number;
  y: number;
}

/**
 * Where each page sits in the graph view: a force layout (linked pages pull
 * together, every pair pushes apart, pages in one folder drift together).
 * Deterministic: the same pages and links give the same picture, so it can be
 * drawn on the server and the browser alike.
 */
export function wikiLayout(nodes: { id: string; group: string }[], edges: [string, string][], iterations = 300): Map<string, Point> {
  const ids = [...nodes].sort((a, b) => a.id.localeCompare(b.id));
  const n = ids.length;
  const index = new Map(ids.map((p, i) => [p.id, i]));
  const links = edges.map(([a, b]) => [index.get(a), index.get(b)] as const).filter((e): e is readonly [number, number] => e[0] !== undefined && e[1] !== undefined && e[0] !== e[1]);
  // Start on a sunflower spiral, so nothing begins on top of anything else.
  const pos = ids.map((_, i) => {
    const r = 40 * Math.sqrt(i + 0.5);
    const a = i * 2.399963;
    return { x: r * Math.cos(a), y: r * Math.sin(a) };
  });
  const k = 90; // the distance two linked pages settle at
  let heat = 60;
  for (let it = 0; it < iterations; it++) {
    const d = pos.map(() => ({ x: 0, y: 0 }));
    for (let i = 0; i < n; i++) {
      for (let j = i + 1; j < n; j++) {
        const dx = pos[i]!.x - pos[j]!.x;
        const dy = pos[i]!.y - pos[j]!.y;
        const dist2 = Math.max(1, dx * dx + dy * dy);
        const f = (k * k) / dist2;
        d[i]!.x += dx * f;
        d[i]!.y += dy * f;
        d[j]!.x -= dx * f;
        d[j]!.y -= dy * f;
      }
    }
    for (const [a, b] of links) {
      const dx = pos[a]!.x - pos[b]!.x;
      const dy = pos[a]!.y - pos[b]!.y;
      const dist = Math.max(1, Math.hypot(dx, dy));
      const f = dist / k;
      d[a]!.x -= dx * f;
      d[a]!.y -= dy * f;
      d[b]!.x += dx * f;
      d[b]!.y += dy * f;
    }
    // Folders: a gentle pull toward the middle of the folder, and everything toward the centre.
    const centres = new Map<string, Point & { c: number }>();
    ids.forEach((p, i) => {
      const c = centres.get(p.group) ?? { x: 0, y: 0, c: 0 };
      centres.set(p.group, { x: c.x + pos[i]!.x, y: c.y + pos[i]!.y, c: c.c + 1 });
    });
    ids.forEach((p, i) => {
      const c = centres.get(p.group)!;
      d[i]!.x += (c.x / c.c - pos[i]!.x) * 0.4 - pos[i]!.x * 0.25;
      d[i]!.y += (c.y / c.c - pos[i]!.y) * 0.4 - pos[i]!.y * 0.25;
    });
    for (let i = 0; i < n; i++) {
      const len = Math.hypot(d[i]!.x, d[i]!.y);
      if (len === 0) continue;
      const step = Math.min(len, heat);
      pos[i]!.x += (d[i]!.x / len) * step;
      pos[i]!.y += (d[i]!.y / len) * step;
    }
    heat = Math.max(1, heat * 0.985);
  }
  // Rounded so server and browser draw the same thing.
  return new Map(ids.map((p, i) => [p.id, { x: Math.round(pos[i]!.x * 10) / 10, y: Math.round(pos[i]!.y * 10) / 10 }]));
}
