import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { diffLines, renderDiff } from "../../shared/diff.js";
import { addDays } from "../../shared/localTime.js";
import { env } from "../env.js";
import { NotFound } from "../errors.js";
import { resolveDate } from "../memory/day.js";
import { dayTimeline } from "../memory/timeline.js";
import { updateEntity } from "../memory/entities.js";
import { PEOPLE_FILTERS, type PeopleFilter, listPeople } from "../people.js";
import { createPage, listPages, pageHistory, readPage, revertPage, setInDigest, updatePage } from "../memory/pages.js";
import { search } from "../memory/search.js";

const ui = { via: "ui" as const };

const Query = z.object({
  tab: z.enum(["pages", "people", "timeline"]).optional(),
  slug: z.string().optional(),
  version: z.number().int().optional(),
  q: z.string().optional(),
  date: z.string().optional(),
  filter: z.enum(PEOPLE_FILTERS).optional(),
  pq: z.string().optional(),
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

    const people =
      tab === "people"
        ? await listPeople(sql, { filter: (data.filter ?? "important") as PeopleFilter, q: data.pq, limit: 200, birthdayWindow: 30 })
        : { people: [], total: 0, shown: 0 };

    const date = resolveDate(data.date);
    const timeline = tab === "timeline" ? await dayTimeline(sql, date) : [];

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
