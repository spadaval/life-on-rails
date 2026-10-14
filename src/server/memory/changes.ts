// "What is new since I last looked." Keyed on arrival time (when LoR recorded
// it), not event time, so something recorded late is not missed. The caller
// remembers its own last run time.
import type { Tx } from "../../db/client.js";

export interface ChangeItem {
  id: string;
  summary: string;
  at: string;
}

export interface ChangeGroup {
  kind: string;
  count: number;
  /** Capped; `count` is the true total. */
  items: ChangeItem[];
}

export interface Changes {
  since: string;
  until: string;
  results: ChangeGroup[];
}

const CAP = 100;

export type ChangeProvider = (db: Tx, since: Date, until: Date) => Promise<ChangeGroup[]>;
const providers: ChangeProvider[] = [];

export function registerChangeProvider(provider: ChangeProvider): void {
  providers.push(provider);
}

function group(kind: string, rows: { id: string; summary: string | null; at: Date }[], count: number): ChangeGroup {
  return {
    kind,
    count,
    items: rows.map((r) => ({ id: r.id, summary: r.summary ?? "", at: r.at.toISOString() })),
  };
}

export async function getChanges(db: Tx, since: Date): Promise<Changes> {
  const until = new Date();
  const results: ChangeGroup[] = [];

  // Observations, one group per source (mail arrivals, agent log entries, ...).
  const sources = await db<{ source: string; n: number }[]>`
    SELECT source, count(*)::int AS n FROM observation
    WHERE ingested_at > ${since} AND ingested_at <= ${until} GROUP BY source ORDER BY source`;
  for (const s of sources) {
    const rows = await db<{ id: string; summary: string | null; at: Date }[]>`
      SELECT id, concat_ws(' — ', title, nullif(payload->>'from', '')) AS summary, ingested_at AS at
      FROM observation WHERE source = ${s.source} AND ingested_at > ${since} AND ingested_at <= ${until}
      ORDER BY ingested_at DESC LIMIT ${CAP}`;
    results.push(group(`observation:${s.source}`, rows, s.n));
  }

  const pages = await db<{ id: string; summary: string; at: Date }[]>`
    SELECT p.slug AS id,
           concat(p.title, ' (v', max(v.version), ', ', string_agg(DISTINCT v.via, '+'), ')') AS summary,
           max(v.created_at) AS at
    FROM page_version v JOIN page p ON p.id = v.page_id
    WHERE v.created_at > ${since} AND v.created_at <= ${until}
    GROUP BY p.slug, p.title ORDER BY at DESC LIMIT ${CAP}`;
  if (pages.length > 0) results.push(group("page", pages, pages.length));

  for (const provider of providers) results.push(...(await provider(db, since, until)));

  return { since: since.toISOString(), until: until.toISOString(), results: results.filter((g) => g.count > 0) };
}
