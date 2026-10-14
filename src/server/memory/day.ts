// One local day as the agent sees it: planned, actual, what was committed,
// and the day page the nightly run writes.
import type { Tx } from "../../db/client.js";
import { type Ymd, isYmd, localDayBounds, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { BadInput } from "../errors.js";

export interface DayView {
  date: Ymd;
  start: string;
  end: string;
  planned: { calendar_id: string; event_id: string; title: string | null; t_start: string; t_end: string; all_day: boolean; status: string }[];
  blocks: {
    id: string; t_start: string; t_end: string | null; paused: boolean; origin: string;
    intent_id: string | null; intent: string | null; title: string | null; committed: boolean;
  }[];
  commitments: { id: string; kind: string; intent_id: string; intent: string; t: string; t_end: string | null; value: number | null; memo: string | null; via: string }[];
  observations: { source: string; count: number }[];
  /** Body of the `days/<date>` page, if the nightly run has written it. */
  page: string | null;
}

export function resolveDate(input: string | undefined | null, now = new Date()): Ymd {
  if (!input || input === "today") return localYmd(env.LOR_TZ, now);
  if (!isYmd(input)) throw new BadInput(`bad date ${JSON.stringify(input)}: use YYYY-MM-DD`);
  return input;
}

export async function getDay(db: Tx, date: Ymd): Promise<DayView> {
  const { start, end } = localDayBounds(env.LOR_TZ, date);
  const planned = await db<{ calendar_id: string; event_id: string; title: string | null; t_start: Date; t_end: Date; all_day: boolean; status: string }[]>`
    SELECT calendar_id, event_id, title, t_start, t_end, all_day, status FROM google_event
    WHERE t_start < ${end} AND t_end > ${start} AND status <> 'cancelled' ORDER BY t_start`;
  const blocks = await db<{ id: string; t_start: Date; t_end: Date | null; paused_at: Date | null; origin: string; intent_id: string | null; intent: string | null; title: string | null; committed: boolean }[]>`
    SELECT b.id, b.t_start, b.t_end, b.paused_at, b.origin, b.intent_id, i.title AS intent, b.title, b.committed
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id
    WHERE b.t_start < ${end} AND coalesce(b.t_end, now()) > ${start} ORDER BY b.t_start`;
  const commitments = await db<{ id: string; kind: string; intent_id: string; intent: string; t: Date; t_end: Date | null; value: number | null; memo: string | null; via: string }[]>`
    SELECT c.id, c.kind, c.intent_id, i.title AS intent, c.t, c.t_end, c.value, c.memo, c.via
    FROM commitment c JOIN intent i ON i.id = c.intent_id
    WHERE c.t >= ${start} AND c.t < ${end} ORDER BY c.t`;
  const observations = await db<{ source: string; count: number }[]>`
    SELECT source, count(*)::int AS count FROM observation
    WHERE t_start >= ${start} AND t_start < ${end} GROUP BY source ORDER BY source`;
  const page = await db<{ body: string }[]>`SELECT body FROM page WHERE slug = ${"days/" + date}`;
  return {
    date,
    start: start.toISOString(),
    end: end.toISOString(),
    planned: planned.map((e) => ({ ...e, t_start: e.t_start.toISOString(), t_end: e.t_end.toISOString() })),
    blocks: blocks.map((b) => ({
      id: b.id, t_start: b.t_start.toISOString(), t_end: b.t_end?.toISOString() ?? null, paused: b.paused_at !== null,
      origin: b.origin, intent_id: b.intent_id, intent: b.intent, title: b.title, committed: b.committed,
    })),
    commitments: commitments.map((c) => ({ ...c, t: c.t.toISOString(), t_end: c.t_end?.toISOString() ?? null })),
    observations: observations.map((o) => ({ source: o.source, count: o.count })),
    page: page[0]?.body ?? null,
  };
}
