import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { dto, one } from "../dto.js";

export const getCaptureStatus = createServerFn({ method: "GET" })
  .validator((d: unknown) => z.object({}).parse(d))
  .handler(async () => {
    const poll = one(await sql`SELECT name, status, started_at, finished_at, stats FROM job_run
      WHERE name = 'calendar_poll' ORDER BY started_at DESC LIMIT 1`);
    const ingest = one(await sql`SELECT max(ingested_at) AS last FROM observation WHERE source = 'linux_activity'`);
    const overnight = one(await sql`SELECT status, finished_at FROM job_run
      WHERE name = 'overnight_close' ORDER BY started_at DESC LIMIT 1`);
    const hb = one(await sql`SELECT title, ingested_at FROM observation
      WHERE source = 'linux_activity' ORDER BY ingested_at DESC LIMIT 1`);
    const now = Date.now();
    const pollAgeMin = poll?.finished_at ? (now - new Date(String(poll.finished_at)).getTime()) / 60_000 : null;
    const lastIngestAgeMin = ingest?.last ? (now - new Date(String(ingest.last)).getTime()) / 60_000 : null;
    let activity: "tracking" | "no_session" | "stale" | "error" | "disabled" = "disabled";
    if (pollAgeMin !== null && pollAgeMin > 15) activity = "stale";
    else if (hb && lastIngestAgeMin !== null && lastIngestAgeMin < 15) {
      activity = hb.title === "no_session" ? "no_session" : "tracking";
    } else if (poll?.status === "error") activity = "error";
    return {
      activity,
      lastCalendarPoll: poll?.finished_at ? String(poll.finished_at) : null,
      lastOvernight: overnight?.finished_at ? String(overnight.finished_at) : null,
      lastActivityIngest: ingest?.last ? String(ingest.last) : null,
    };
  });

export const listRules = createServerFn({ method: "GET" })
  .validator((d: unknown) => z.object({}).parse(d))
  .handler(async () => {
    return dto(await sql`SELECT * FROM attribution_rule ORDER BY priority DESC`);
  });

export const upsertRule = createServerFn({ method: "POST" })
  .validator((d: unknown) =>
    z.object({
      id: z.string().uuid().optional(),
      matchApp: z.string().nullable().optional(),
      matchTitle: z.string().max(200).nullable().optional(),
      matchUrl: z.string().nullable().optional(),
      matchDevice: z.string().nullable().optional(),
      intentId: z.string().uuid(),
      autoApply: z.boolean().optional(),
      priority: z.number().int().optional(),
    }).parse(d),
  )
  .handler(async ({ data }) => {
    const { validateTitleRegex } = await import("../attribution/suggest.js");
    if (data.matchTitle && !validateTitleRegex(data.matchTitle)) throw new Error("invalid match_title regex");
    if (data.id) {
      await sql`UPDATE attribution_rule SET match_app = ${data.matchApp ?? null},
        match_title = ${data.matchTitle ?? null}, match_url = ${data.matchUrl ?? null},
        intent_id = ${data.intentId} WHERE id = ${data.id}`;
      return { id: data.id };
    }
    const { uuidv7 } = await import("../../shared/ids.js");
    const id = uuidv7();
    await sql`INSERT INTO attribution_rule (id, match_device, match_app, match_title, match_url, intent_id, auto_apply, priority)
      VALUES (${id}, ${data.matchDevice ?? null}, ${data.matchApp ?? null}, ${data.matchTitle ?? null},
        ${data.matchUrl ?? null}, ${data.intentId}, ${data.autoApply ?? false}, ${data.priority ?? 0})`;
    return { id };
  });
