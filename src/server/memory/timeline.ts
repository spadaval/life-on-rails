// Memory's Timeline tab: what was recorded on one local day.
import type { Tx } from "../../db/client.js";
import { type Ymd, localDayBounds } from "../../shared/localTime.js";
import { env } from "../env.js";

export interface TimelineRow {
  id: string;
  /** Every observation the row stands for; more than one for a conversation. */
  ids: string[];
  t: string;
  source: string;
  title: string | null;
  detail: string | null;
  bulk: boolean;
}

/** Newest first. Texts and WhatsApp messages are one row per conversation (named, with a count), not one per message. */
export async function dayTimeline(db: Tx, date: Ymd): Promise<TimelineRow[]> {
  const { start, end } = localDayBounds(env.LOR_TZ, date);
  const rows = await db<{ id: string; ids: string[] | null; t_start: Date; source: string; title: string | null; detail: string | null; bulk: boolean }[]>`
    WITH day AS (SELECT * FROM observation WHERE t_start >= ${start} AND t_start < ${end})
    SELECT id, NULL::uuid[] AS ids, t_start, source, title,
           coalesce(payload->>'from', app) AS detail,
           coalesce((payload->>'bulk')::boolean, false) AS bulk
    FROM day WHERE kind IS NULL OR kind NOT IN ('sms', 'whatsapp')
    UNION ALL
    SELECT (array_agg(id ORDER BY t_start DESC))[1], array_agg(id), max(t_start), kind,
           coalesce((SELECT e.name FROM entity_identifier i JOIN entity e ON e.id = i.entity_id WHERE i.kind || ':' || i.value = conv LIMIT 1),
                    max(payload->>'chat_name'), max(payload->>'phone'), 'unknown'),
           count(*) || CASE WHEN count(*) = 1 THEN ' message' ELSE ' messages' END,
           false
    FROM (SELECT *, coalesce(parties[1], author, '') AS conv FROM day WHERE kind IN ('sms', 'whatsapp')) m
    GROUP BY kind, conv
    ORDER BY t_start DESC LIMIT 500`;
  return rows.map((r) => ({ id: r.id, ids: r.ids ?? [r.id], t: r.t_start.toISOString(), source: r.source, title: r.title, detail: r.detail, bulk: r.bulk }));
}
