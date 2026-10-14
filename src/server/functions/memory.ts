import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { diffLines, renderDiff } from "../../shared/diff.js";
import { addDays, localDayBounds } from "../../shared/localTime.js";
import { env } from "../env.js";
import { NotFound } from "../errors.js";
import { resolveDate } from "../memory/day.js";
import { listEntities, setEntityPriority } from "../memory/entities.js";
import { createPage, listPages, pageHistory, readPage, revertPage, setInDigest, updatePage } from "../memory/pages.js";
import { search } from "../memory/search.js";

const ui = { via: "ui" as const };

const Query = z.object({
  tab: z.enum(["pages", "people", "timeline"]).optional(),
  slug: z.string().optional(),
  version: z.number().int().optional(),
  q: z.string().optional(),
  date: z.string().optional(),
});

export const getMemoryFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => Query.parse(d ?? {}))
  .handler(async ({ data }) => {
    const tab = data.tab ?? "pages";
    const results = data.q?.trim() ? await search(sql, { q: data.q, limit: 30 }) : null;

    const pages = tab === "pages" ? await listPages(sql) : [];
    let page = null;
    if (tab === "pages" && data.slug) {
      try {
        const view = await readPage(sql, data.slug, data.version);
        const history = await pageHistory(sql, data.slug);
        // What this version changed, against the one before it.
        let diff: string | null = null;
        if (view.version > 1) {
          const before = await readPage(sql, data.slug, view.version - 1);
          diff = renderDiff(diffLines(before.body, view.body), 1);
        }
        page = { view, history, diff };
      } catch (err) {
        if (!(err instanceof NotFound)) throw err;
      }
    }

    const people = tab === "people" ? await listEntities(sql) : [];

    const date = resolveDate(data.date);
    let timeline: { id: string; t: string; source: string; title: string | null; detail: string | null; bulk: boolean }[] = [];
    if (tab === "timeline") {
      const { start, end } = localDayBounds(env.LOR_TZ, date);
      const rows = await sql<{ id: string; t_start: Date; source: string; title: string | null; detail: string | null; bulk: boolean }[]>`
        SELECT id, t_start, source, title,
               coalesce(payload->>'from', app) AS detail,
               coalesce((payload->>'bulk')::boolean, false) AS bulk
        FROM observation WHERE t_start >= ${start} AND t_start < ${end} ORDER BY t_start DESC LIMIT 500`;
      timeline = rows.map((r) => ({ id: r.id, t: r.t_start.toISOString(), source: r.source, title: r.title, detail: r.detail, bulk: r.bulk }));
    }

    return { tab, q: data.q ?? "", results, pages, page, people, timeline, date, prev: addDays(date, -1), next: addDays(date, 1), timezone: env.LOR_TZ };
  });

export const savePageFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) =>
    z.object({ slug: z.string(), base: z.number().int(), body: z.string(), title: z.string().optional(), note: z.string().optional(), force: z.boolean().optional() }).parse(d),
  )
  .handler(async ({ data }) => {
    const { force, ...input } = data;
    // "Save mine on top": write over whatever is newest. The version it replaces stays in the history.
    if (force) input.base = (await readPage(sql, input.slug)).head_version;
    const r = await updatePage(sql, { ...input, ...ui });
    return { version: r.page.version, changed: r.changed, warnings: r.warnings };
  });

export const createPageFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ slug: z.string(), title: z.string(), body: z.string() }).parse(d))
  .handler(async ({ data }) => {
    const p = await createPage(sql, { ...data, ...ui });
    return { slug: p.slug };
  });

export const revertPageFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ slug: z.string(), version: z.number().int() }).parse(d))
  .handler(async ({ data }) => {
    const r = await revertPage(sql, data.slug, data.version, ui);
    return { version: r.page.version };
  });

export const setInDigestFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ slug: z.string(), value: z.boolean() }).parse(d))
  .handler(async ({ data }) => {
    await setInDigest(sql, data.slug, data.value);
    return { ok: true };
  });

export const setPriorityFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) => z.object({ id: z.string().uuid(), value: z.boolean() }).parse(d))
  .handler(async ({ data }) => {
    await setEntityPriority(sql, data.id, data.value);
    return { ok: true };
  });
