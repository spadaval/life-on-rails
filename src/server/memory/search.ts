// One lexical search over everything the agent can look up. Returns ids and
// snippets, never full bodies; the caller follows up with a read.
import type { Tx } from "../../db/client.js";

export const SEARCH_KINDS = ["page", "entity", "intent", "obs"] as const;
export type SearchKind = (typeof SEARCH_KINDS)[number];

export interface SearchHit {
  kind: SearchKind;
  /** A page's slug; otherwise the row's id. */
  id: string;
  title: string;
  snippet: string;
  /** When the thing happened or last changed. */
  t: string;
  score: number;
}

export interface SearchInput {
  q: string;
  in?: SearchKind[];
  from?: Date;
  to?: Date;
  limit?: number;
}

interface Row {
  id: string;
  title: string | null;
  snippet: string | null;
  t: Date;
  score: number;
}

// At equal relevance, maintained synthesis comes before raw events.
const KIND_BONUS: Record<SearchKind, number> = { page: 0.2, entity: 0.15, intent: 0.1, obs: 0 };

function recency(t: Date, now: number): number {
  const ageDays = Math.max(0, (now - t.getTime()) / 86_400_000);
  return 1 + 0.5 * Math.exp(-ageDays / 30);
}

export async function search(db: Tx, input: SearchInput): Promise<SearchHit[]> {
  const q = input.q.trim();
  if (!q) return [];
  const kinds = input.in?.length ? input.in : [...SEARCH_KINDS];
  const limit = Math.min(Math.max(input.limit ?? 20, 1), 100);
  const from = input.from ?? null;
  const to = input.to ?? null;
  const like = `%${q.replace(/[%_\\]/g, "\\$&")}%`;
  const now = Date.now();
  const hits: SearchHit[] = [];
  const add = (kind: SearchKind, rows: Row[]) => {
    for (const r of rows) {
      hits.push({
        kind,
        id: r.id,
        title: r.title ?? "",
        snippet: (r.snippet ?? "").slice(0, 400),
        t: r.t.toISOString(),
        score: Number((r.score * recency(r.t, now) + KIND_BONUS[kind]).toFixed(4)),
      });
    }
  };

  if (kinds.includes("page")) {
    add(
      "page",
      await db<Row[]>`
        WITH q AS (SELECT websearch_to_tsquery('english', ${q}) AS tsq)
        SELECT p.slug AS id, p.title,
               ts_headline('english', p.body, q.tsq, 'MaxWords=30, MinWords=10, MaxFragments=2, StartSel="", StopSel=""') AS snippet,
               p.updated_at AS t,
               (ts_rank_cd(p.search, q.tsq) + similarity(p.title, ${q}))::float8 AS score
        FROM page p, q
        WHERE (p.search @@ q.tsq OR similarity(p.title, ${q}) > 0.25)
          AND (${from}::timestamptz IS NULL OR p.updated_at >= ${from})
          AND (${to}::timestamptz IS NULL OR p.updated_at < ${to})
        ORDER BY score DESC LIMIT ${limit}`,
    );
  }
  if (kinds.includes("entity")) {
    add(
      "entity",
      await db<Row[]>`
        SELECT e.id, e.name AS title,
               concat_ws(' · ', e.kind, nullif(array_to_string(e.aliases, ', '), ''),
                 (SELECT string_agg(i.value, ', ') FROM entity_identifier i WHERE i.entity_id = e.id)) AS snippet,
               e.updated_at AS t,
               greatest(similarity(e.name, ${q}),
                 CASE WHEN EXISTS (SELECT 1 FROM unnest(e.aliases) a WHERE a ILIKE ${like}) THEN 0.6 ELSE 0 END,
                 CASE WHEN EXISTS (SELECT 1 FROM entity_identifier i WHERE i.entity_id = e.id AND i.value ILIKE ${like}) THEN 0.6 ELSE 0 END
               )::float8 AS score
        FROM entity e
        WHERE similarity(e.name, ${q}) > 0.3
           OR EXISTS (SELECT 1 FROM unnest(e.aliases) a WHERE a ILIKE ${like})
           OR EXISTS (SELECT 1 FROM entity_identifier i WHERE i.entity_id = e.id AND i.value ILIKE ${like})
        ORDER BY score DESC LIMIT ${limit}`,
    );
  }
  if (kinds.includes("intent")) {
    add(
      "intent",
      await db<Row[]>`
        SELECT i.id, i.title, concat_ws(' · ', i.type::text, i.status::text, i.description) AS snippet,
               i.updated_at AS t,
               greatest(similarity(i.title, ${q}), CASE WHEN i.title ILIKE ${like} THEN 0.5 ELSE 0 END,
                 CASE WHEN i.description ILIKE ${like} THEN 0.3 ELSE 0 END)::float8 AS score
        FROM intent i
        WHERE NOT i.system AND i.status <> 'archived'
          AND (similarity(i.title, ${q}) > 0.3 OR i.title ILIKE ${like} OR i.description ILIKE ${like})
        ORDER BY score DESC LIMIT ${limit}`,
    );
  }
  if (kinds.includes("obs")) {
    add(
      "obs",
      await db<Row[]>`
        WITH q AS (SELECT websearch_to_tsquery('english', ${q}) AS tsq)
        SELECT o.id, o.title, concat_ws(' · ', o.source, o.app, o.payload->>'from') AS snippet,
               o.t_start AS t, ts_rank_cd(o.search, q.tsq)::float8 AS score
        FROM observation o, q
        WHERE o.search @@ q.tsq
          AND (${from}::timestamptz IS NULL OR o.t_start >= ${from})
          AND (${to}::timestamptz IS NULL OR o.t_start < ${to})
        ORDER BY score DESC, o.t_start DESC LIMIT ${limit}`,
    );
  }
  return hits.sort((a, b) => b.score - a.score || b.t.localeCompare(a.t)).slice(0, limit);
}
