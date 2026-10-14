// The digest every agent run starts with: who it is, what it knows by heart,
// and what today looks like. Kept short on purpose; there is no size budget
// until a digest is actually too long.
import type { Tx } from "../../db/client.js";
import { localHm, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";

export interface DigestPage {
  slug: string;
  title: string;
  body: string;
  version: number;
  updated_at: string;
  last_via: string;
  last_source_ref: string | null;
}

export interface RecentPage {
  slug: string;
  title: string;
  version: number;
  updated_at: string;
  last_via: string;
}

export interface Context {
  now: string;
  today: string;
  timezone: string;
  pages: DigestPage[];
  recent_pages: RecentPage[];
  pending_proposals: number;
  /** Sections other build steps add (today's plan, goals, tasks), already rendered. */
  sections: { title: string; markdown: string }[];
  markdown: string;
}

export type SectionProvider = (db: Tx, now: Date) => Promise<{ title: string; markdown: string } | null>;

const providers: SectionProvider[] = [];

/** Other modules contribute sections (Today, goals, tasks) as they are built. */
export function registerContextSection(provider: SectionProvider): void {
  providers.push(provider);
}

async function digestPages(db: Tx): Promise<DigestPage[]> {
  const rows = await db<(Omit<DigestPage, "updated_at"> & { updated_at: Date })[]>`
    SELECT p.slug, p.title, p.body, p.head_version AS version, p.updated_at,
           v.via AS last_via, v.source_ref AS last_source_ref
    FROM page p JOIN page_version v ON v.page_id = p.id AND v.version = p.head_version
    WHERE p.in_digest
    ORDER BY (p.slug = 'agent') DESC, (p.slug = 'me') DESC, p.slug`;
  return rows.map((r) => ({ ...r, updated_at: r.updated_at.toISOString() }));
}

function render(ctx: Omit<Context, "markdown">): string {
  const out: string[] = [];
  out.push(`# Context: ${ctx.today} ${localHm(ctx.timezone, new Date(ctx.now))} (${ctx.timezone})`);
  for (const p of ctx.pages) {
    out.push("", `## Page: ${p.slug} (v${p.version}, last written via ${p.last_via})`, "", p.body.trim());
  }
  for (const s of ctx.sections) {
    out.push("", `## ${s.title}`, "", s.markdown.trim());
  }
  out.push("", `## Pending proposals: ${ctx.pending_proposals}`);
  if (ctx.recent_pages.length > 0) {
    out.push("", "## Recently changed pages", "");
    for (const p of ctx.recent_pages) {
      out.push(`- ${p.slug} — ${p.title} (v${p.version}, ${p.last_via}, ${p.updated_at.slice(0, 16)}Z)`);
    }
  }
  return out.join("\n") + "\n";
}

export async function getContext(db: Tx): Promise<Context> {
  const now = new Date();
  const pages = await digestPages(db);
  const recent = await db<(Omit<RecentPage, "updated_at"> & { updated_at: Date })[]>`
    SELECT p.slug, p.title, p.head_version AS version, p.updated_at, v.via AS last_via
    FROM page p JOIN page_version v ON v.page_id = p.id AND v.version = p.head_version
    WHERE NOT p.in_digest AND p.updated_at > now() - interval '7 days'
    ORDER BY p.updated_at DESC LIMIT 15`;
  const pending = await db<{ n: number }[]>`SELECT count(*)::int AS n FROM proposal WHERE status = 'pending'`;
  const sections: { title: string; markdown: string }[] = [];
  for (const provider of providers) {
    const s = await provider(db, now);
    if (s) sections.push(s);
  }
  const base = {
    now: now.toISOString(),
    today: localYmd(env.LOR_TZ, now),
    timezone: env.LOR_TZ,
    pages,
    recent_pages: recent.map((r) => ({ ...r, updated_at: r.updated_at.toISOString() })),
    pending_proposals: pending[0]?.n ?? 0,
    sections,
  };
  return { ...base, markdown: render(base) };
}
