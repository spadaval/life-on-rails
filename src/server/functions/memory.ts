import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { diffLines, renderDiff } from "../../shared/diff.js";
import { localYmd } from "../../shared/localTime.js";
import { folderOf, wikiLayout, wikiLinks } from "../../shared/wiki.js";
import { env } from "../env.js";
import { NotFound } from "../errors.js";
import { showEntity, updateEntity } from "../memory/entities.js";
import { PEOPLE_FILTERS, type PeopleFilter, listPeople } from "../people.js";
import { createPage, listPages, pageHistory, readPage, revertPage, setInDigest, updatePage } from "../memory/pages.js";
import { search } from "../memory/search.js";

const ui = { via: "ui" as const };

// Notes (the wiki), People, and search. Server functions for those three screens.

export const getNotesFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => z.object({ slug: z.string().optional(), version: z.number().int().optional(), graph: z.boolean().optional() }).parse(d ?? {}))
  .handler(async ({ data }) => {
    const [list, bodies] = await Promise.all([listPages(sql), sql<{ slug: string; body: string; entity_id: string | null }[]>`SELECT slug, body, entity_id FROM page`]);
    const linksOf = new Map(bodies.map((b) => [b.slug, wikiLinks(b.body)]));
    const entityOf = new Map(bodies.map((b) => [b.slug, b.entity_id]));
    const pages = list.map((p) => ({ ...p, links: (linksOf.get(p.slug) ?? []).filter((l) => linksOf.has(l) && l !== p.slug), entity_id: entityOf.get(p.slug) ?? null }));

    let page = null;
    if (data.slug) {
      try {
        const view = await readPage(sql, data.slug, data.version);
        const history = await pageHistory(sql, data.slug);
        // What this version changed, against the one before it.
        let diff: string | null = null;
        // Versions can skip numbers (a run of typing is one version), so "before" is the next one down.
        const prev = history.find((h) => h.version < view.version);
        if (prev) {
          const before = await readPage(sql, data.slug, prev.version);
          diff = renderDiff(diffLines(before.body, view.body), 1);
        }
        const backlinks = pages.filter((p) => p.links.includes(view.slug)).map((p) => ({ slug: p.slug, title: p.title }));
        const missing = wikiLinks(view.body).filter((l) => !linksOf.has(l));
        page = { view, history, diff, backlinks, missing };
      } catch (err) {
        if (!(err instanceof NotFound)) throw err;
      }
    }

    // Placed here rather than in the browser, so the picture is drawn once and the same on both.
    const layout = data.graph
      ? Object.fromEntries(wikiLayout(pages.map((p) => ({ id: p.slug, group: folderOf(p.slug) })), pages.flatMap((p) => p.links.map((l) => [p.slug, l] as [string, string]))))
      : null;
    return { pages, page, layout };
  });

export const searchFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => z.object({ q: z.string() }).parse(d))
  .handler(async ({ data }) => ({ results: data.q.trim() ? await search(sql, { q: data.q, limit: 40 }) : [] }));

export const getPeopleFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => z.object({ filter: z.enum(PEOPLE_FILTERS).optional(), q: z.string().optional() }).parse(d ?? {}))
  .handler(async ({ data }) => listPeople(sql, { filter: (data.filter ?? "important") as PeopleFilter, q: data.q, limit: 200, birthdayWindow: 30 }));

/** One person's hub: who they are, their page, when you were last in touch, what was said, and what waits on them. */
export const getPersonFn = createServerFn({ method: "GET" })
  .inputValidator((d: unknown) => z.object({ id: z.string().uuid() }).parse(d))
  .handler(async ({ data }) => {
    const entity = await showEntity(sql, data.id);
    const page = entity.page ? await readPage(sql, entity.page) : null;
    const waiting = await sql<{ id: string; title: string; due_at: Date | null; status: string }[]>`
      SELECT id, title, due_at, status FROM intent
      WHERE waiting_on_entity_id = ${data.id} AND status NOT IN ('done', 'archived')
      ORDER BY due_at NULLS LAST, created_at`;
    const [person] = (await listPeople(sql, { filter: "all", q: entity.name, limit: 500 })).people.filter((p) => p.id === data.id);
    const timezone = env.LOR_TZ;
    return {
      entity,
      person: person ?? null,
      page: page ? { slug: page.slug, title: page.title, body: page.body, via: page.via, written_at: page.written_at } : null,
      waiting: waiting.map((w) => ({ id: w.id, title: w.title, status: w.status, due: w.due_at ? localYmd(timezone, w.due_at) : null })),
      today: localYmd(timezone, new Date()),
      timezone,
    };
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

export const setPersonFn = createServerFn({ method: "POST" })
  .inputValidator((d: unknown) =>
    z.object({
      id: z.string().uuid(),
      priority: z.boolean().optional(),
      keepInTouchDays: z.number().int().positive().nullable().optional(),
      birthday: z.string().nullable().optional(),
    }).parse(d),
  )
  .handler(async ({ data }) => {
    const { id, ...patch } = data;
    await updateEntity(sql, id, patch);
    return { ok: true };
  });
