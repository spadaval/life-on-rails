// The wiki. One page per subject, plain markdown. Every write stores the whole
// body as a new version; links and citations are text in the body that the
// server does not parse. The UI and the agent both write through here.
import type { Sql, Tx } from "../../db/client.js";
import { diffLines, renderDiff } from "../../shared/diff.js";
import { uuidv7 } from "../../shared/ids.js";
import { BadInput, NotFound, Refused } from "../errors.js";

const SLUG = /^[a-z0-9][a-z0-9/-]*$/;

export type Via = "ui" | "agent" | "seed";

export interface WriteMeta {
  via: Via;
  sourceRef?: string | null;
  note?: string | null;
}

export interface PageView {
  slug: string;
  title: string;
  entity_id: string | null;
  intent_id: string | null;
  /** The version this view shows. */
  version: number;
  head_version: number;
  in_digest: boolean;
  body: string;
  /** Who wrote the shown version, and from which thread. */
  via: string;
  source_ref: string | null;
  note: string | null;
  written_at: string;
}

export interface VersionMeta {
  version: number;
  title: string;
  via: string;
  source_ref: string | null;
  note: string | null;
  created_at: string;
  chars: number;
}

interface PageRow {
  id: string;
  slug: string;
  title: string;
  entity_id: string | null;
  intent_id: string | null;
  head_version: number;
  body: string;
  in_digest: boolean;
}

interface VersionRow {
  version: number;
  title: string;
  body: string;
  via: string;
  source_ref: string | null;
  note: string | null;
  created_at: Date;
}

function view(page: PageRow, v: VersionRow): PageView {
  return {
    slug: page.slug,
    title: v.title,
    entity_id: page.entity_id,
    intent_id: page.intent_id,
    version: v.version,
    head_version: page.head_version,
    in_digest: page.in_digest,
    body: v.body,
    via: v.via,
    source_ref: v.source_ref,
    note: v.note,
    written_at: v.created_at.toISOString(),
  };
}

async function loadPage(tx: Tx, slug: string, forUpdate = false): Promise<PageRow> {
  const rows = forUpdate
    ? await tx<PageRow[]>`SELECT * FROM page WHERE slug = ${slug} FOR UPDATE`
    : await tx<PageRow[]>`SELECT * FROM page WHERE slug = ${slug}`;
  const page = rows[0];
  if (!page) throw new NotFound(`no page with slug ${slug}`);
  return page;
}

async function loadVersion(tx: Tx, pageId: string, version: number): Promise<VersionRow> {
  const rows = await tx<VersionRow[]>`
    SELECT version, title, body, via, source_ref, note, created_at
    FROM page_version WHERE page_id = ${pageId} AND version = ${version}`;
  const v = rows[0];
  if (!v) throw new NotFound(`no version ${version}`);
  return v;
}

export interface CreatePageInput extends WriteMeta {
  slug: string;
  title: string;
  body: string;
  entityId?: string | null;
  intentId?: string | null;
}

export async function createPage(db: Sql, input: CreatePageInput): Promise<PageView & { warnings: string[] }> {
  const page = await createPageRow(db, input);
  return { ...page, warnings: footnoteWarnings(page.body) };
}

async function createPageRow(db: Sql, input: CreatePageInput): Promise<PageView> {
  if (!SLUG.test(input.slug)) {
    throw new BadInput(`bad slug ${JSON.stringify(input.slug)}: lowercase letters, digits, "-" and "/" only`);
  }
  if (!input.title.trim()) throw new BadInput("a page needs a title");
  if (input.entityId && input.intentId) throw new BadInput("a page is about an entity or an intent, not both");

  return db
    .begin(async (tx) => createInTx(tx, input))
    .catch((err: unknown) => {
      // Two creates racing past the check: the unique index decides.
      if ((err as { code?: string }).code === "23505") {
        throw new Refused("page-exists", `a page for that slug or subject was just created: search, then update it`);
      }
      throw err;
    }) as Promise<PageView>;
}

async function createInTx(tx: Tx, input: CreatePageInput): Promise<PageView> {
  {
    const taken = await tx<{ slug: string; why: string }[]>`
      SELECT slug, CASE WHEN slug = ${input.slug} THEN 'slug'
                        WHEN entity_id = ${input.entityId ?? null} THEN 'entity' ELSE 'intent' END AS why
      FROM page
      WHERE slug = ${input.slug}
         OR (${input.entityId ?? null}::uuid IS NOT NULL AND entity_id = ${input.entityId ?? null})
         OR (${input.intentId ?? null}::uuid IS NOT NULL AND intent_id = ${input.intentId ?? null})
      LIMIT 1`;
    if (taken[0]) {
      const reason =
        taken[0].why === "slug" ? "that slug is taken" : `that ${taken[0].why} already has a page`;
      throw new Refused("page-exists", `${reason}: update ${taken[0].slug} instead`, { existing: taken[0].slug });
    }
    const id = uuidv7();
    await tx`INSERT INTO page (id, slug, title, entity_id, intent_id, head_version, body)
      VALUES (${id}, ${input.slug}, ${input.title}, ${input.entityId ?? null}, ${input.intentId ?? null}, 1, ${input.body})`;
    await tx`INSERT INTO page_version (page_id, version, title, body, via, source_ref, note)
      VALUES (${id}, 1, ${input.title}, ${input.body}, ${input.via}, ${input.sourceRef ?? null}, ${input.note ?? null})`;
    const page = await loadPage(tx, input.slug);
    return view(page, await loadVersion(tx, id, 1));
  }
}

export interface UpdatePageInput extends WriteMeta {
  slug: string;
  /** The version the caller read. Required unless appending. */
  base?: number;
  /** Replace the whole body ... */
  body?: string;
  /** ... or replace one exact occurrence ... */
  find?: string;
  replace?: string;
  /** ... or add to the end (no base needed). */
  append?: string;
  title?: string;
  dryRun?: boolean;
}

export interface UpdateResult {
  page: PageView;
  changed: boolean;
  dry_run: boolean;
  diff: string;
  /** Things worth fixing that do not stop the write (a footnote with no definition). */
  warnings: string[];
}

/**
 * Footnote markers with no definition, and definitions nothing points at.
 * A check on the text as written; nothing is stored about citations.
 */
export function footnoteWarnings(body: string): string[] {
  const defined = new Set<string>();
  const used = new Set<string>();
  for (const line of body.split("\n")) {
    const def = /^\[\^([^\]\s]+)\]:/.exec(line);
    if (def) defined.add(def[1]!);
    const rest = def ? line.slice(def[0].length) : line;
    for (const m of rest.matchAll(/\[\^([^\]\s]+)\]/g)) used.add(m[1]!);
  }
  const out: string[] = [];
  for (const k of used) if (!defined.has(k)) out.push(`[^${k}] is used but has no definition line`);
  for (const k of defined) if (!used.has(k)) out.push(`[^${k}] is defined but never used`);
  return out;
}

const LIST_LINE = /^\s*(?:[-*+]\s|\d+\.\s)/;
const FOOTNOTE_LINE = /^\[\^[^\]\s]+\]:/;

/** Append continues a list or a footnote block; anything else starts a new paragraph. */
function appendTo(head: string, fragment: string): string {
  const base = head.replace(/\n*$/, "");
  const add = fragment.replace(/^\n+|\n*$/g, "");
  if (!base) return `${add}\n`;
  const last = base.slice(base.lastIndexOf("\n") + 1);
  const first = add.split("\n", 1)[0]!;
  const continues =
    (LIST_LINE.test(last) && LIST_LINE.test(first)) || (FOOTNOTE_LINE.test(last) && FOOTNOTE_LINE.test(first));
  return `${base}${continues ? "\n" : "\n\n"}${add}\n`;
}

export async function updatePage(db: Sql, input: UpdatePageInput): Promise<UpdateResult> {
  const modes = [input.body !== undefined, input.find !== undefined, input.append !== undefined].filter(Boolean).length;
  if (modes > 1) throw new BadInput("give one of: a whole body, --find/--replace, or --append");
  if (modes === 0 && input.title === undefined) throw new BadInput("nothing to change");
  if (input.find !== undefined && input.replace === undefined) throw new BadInput("--find needs --replace");

  return db.begin(async (tx) => {
    const page = await loadPage(tx, input.slug, true);
    const appending = input.append !== undefined;
    if (!appending) {
      if (input.base === undefined) throw new BadInput("--base <version> is required (the version you read)");
      if (input.base !== page.head_version) {
        throw new Refused(
          "stale-base",
          `page ${page.slug} is at version ${page.head_version}, not ${input.base}: read it again and reapply`,
          { head_version: page.head_version, head_body: page.body },
        );
      }
    }

    let body = page.body;
    if (input.body !== undefined) {
      body = input.body;
    } else if (input.find !== undefined) {
      const count = input.find === "" ? 0 : page.body.split(input.find).length - 1;
      if (count !== 1) {
        throw new Refused("find-not-unique", `--find text occurs ${count} times; it must occur exactly once`, {
          occurrences: count,
        });
      }
      body = page.body.replace(input.find, () => input.replace ?? "");
    } else if (input.append !== undefined) {
      body = appendTo(page.body, input.append);
    }
    const title = input.title ?? page.title;
    const changed = body !== page.body || title !== page.title;
    const diff = renderDiff(diffLines(page.body, body));
    const head = await loadVersion(tx, page.id, page.head_version);

    const warnings = footnoteWarnings(body);
    if (!changed || input.dryRun) {
      const shown = { ...head, title, body };
      return { page: view(page, input.dryRun ? shown : head), changed, dry_run: Boolean(input.dryRun), diff, warnings };
    }

    const next = page.head_version + 1;
    await tx`INSERT INTO page_version (page_id, version, title, body, via, source_ref, note)
      VALUES (${page.id}, ${next}, ${title}, ${body}, ${input.via}, ${input.sourceRef ?? null}, ${input.note ?? null})`;
    await tx`UPDATE page SET title = ${title}, body = ${body}, head_version = ${next}, updated_at = now()
      WHERE id = ${page.id}`;
    const fresh = await loadPage(tx, page.slug);
    return { page: view(fresh, await loadVersion(tx, page.id, next)), changed: true, dry_run: false, diff, warnings };
  });
}

export async function readPage(db: Tx, slug: string, version?: number): Promise<PageView> {
  const page = await loadPage(db, slug);
  return view(page, await loadVersion(db, page.id, version ?? page.head_version));
}

export async function pageHistory(db: Tx, slug: string): Promise<VersionMeta[]> {
  const page = await loadPage(db, slug);
  const rows = await db<(Omit<VersionRow, "body"> & { chars: number })[]>`
    SELECT version, title, via, source_ref, note, created_at, char_length(body)::int AS chars
    FROM page_version WHERE page_id = ${page.id} ORDER BY version DESC`;
  return rows.map((r) => ({
    version: r.version,
    title: r.title,
    via: r.via,
    source_ref: r.source_ref,
    note: r.note,
    created_at: r.created_at.toISOString(),
    chars: r.chars,
  }));
}

/** A new version carrying an old version's title and body. */
export async function revertPage(db: Sql, slug: string, toVersion: number, meta: WriteMeta): Promise<UpdateResult> {
  const page = await loadPage(db, slug);
  const old = await loadVersion(db, page.id, toVersion);
  return updatePage(db, {
    slug,
    base: page.head_version,
    body: old.body,
    title: old.title,
    via: meta.via,
    sourceRef: meta.sourceRef,
    note: meta.note ?? `revert to version ${toVersion}`,
  });
}

export async function setInDigest(db: Tx, slug: string, value: boolean): Promise<void> {
  const rows = await db`UPDATE page SET in_digest = ${value} WHERE slug = ${slug} RETURNING id`;
  if (rows.length === 0) throw new NotFound(`no page with slug ${slug}`);
}

export interface PageListItem {
  slug: string;
  title: string;
  in_digest: boolean;
  head_version: number;
  updated_at: string;
  last_via: string;
  last_source_ref: string | null;
}

export async function listPages(db: Tx, prefix = ""): Promise<PageListItem[]> {
  const rows = await db<(Omit<PageListItem, "updated_at"> & { updated_at: Date })[]>`
    SELECT p.slug, p.title, p.in_digest, p.head_version, p.updated_at,
           v.via AS last_via, v.source_ref AS last_source_ref
    FROM page p JOIN page_version v ON v.page_id = p.id AND v.version = p.head_version
    WHERE p.slug LIKE ${prefix.replace(/[%_\\]/g, "\\$&") + "%"}
    ORDER BY p.slug`;
  return rows.map((r) => ({ ...r, updated_at: r.updated_at.toISOString() }));
}

export interface PageHit {
  slug: string;
  title: string;
  snippet: string;
  score: number;
  updated_at: string;
}

/** Find the page for a subject: title first, then body. */
export async function searchPages(db: Tx, query: string, limit = 10): Promise<PageHit[]> {
  const q = query.trim();
  if (!q) return [];
  const rows = await db<{ slug: string; title: string; snippet: string; score: number; updated_at: Date }[]>`
    WITH q AS (SELECT websearch_to_tsquery('english', ${q}) AS tsq)
    SELECT p.slug, p.title,
           ts_headline('english', p.body, q.tsq, 'MaxWords=30, MinWords=10, MaxFragments=2, StartSel="", StopSel=""') AS snippet,
           (ts_rank_cd(p.search, q.tsq) + similarity(p.title, ${q}) + CASE WHEN p.slug ILIKE ${"%" + q.replace(/\s+/g, "-") + "%"} THEN 0.5 ELSE 0 END)::float8 AS score,
           p.updated_at
    FROM page p, q
    WHERE p.search @@ q.tsq OR similarity(p.title, ${q}) > 0.25 OR p.slug ILIKE ${"%" + q.replace(/\s+/g, "-") + "%"}
    ORDER BY score DESC, p.updated_at DESC
    LIMIT ${limit}`;
  return rows.map((r) => ({ ...r, updated_at: r.updated_at.toISOString() }));
}
