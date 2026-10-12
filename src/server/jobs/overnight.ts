// Overnight close (01:30 local): reaper stale opens (grace window), merge
// adjacent uncommitted same-intent non-calendar blocks (gap ≤120s), fill
// missing proposals, score snapshots, retention enforcement. Never confirms.
// Optional 1× LLM day summary only when XAI_API_KEY is set (else skip).
// Catch-up: if boot misses 01:30, run once for yesterday (job lock guards).

export const RETENTION = {
  observationPayloadDays: 90,
  observationRowsMonths: 18,
  proposalDays: 90,
  jobRunDays: 30,
  messageMonths: 18,
} as const;

export const RETENTION_SQL: string[] = [
  // Strip payloads after 90 days (keep app/title for history).
  `UPDATE observation SET payload = jsonb_build_object('app', app, 'title', title)
   WHERE ingested_at < now() - interval '90 days' AND payload <> jsonb_build_object('app', app, 'title', title)`,
  `DELETE FROM observation WHERE ingested_at < now() - interval '18 months'`,
  `DELETE FROM title_embedding te WHERE NOT EXISTS (
     SELECT 1 FROM observation o
     WHERE (o.app IS NOT DISTINCT FROM te.app) AND o.title = te.title
       AND o.ingested_at > now() - interval '18 months')`,
  `DELETE FROM proposal WHERE status IN ('accepted','rejected') AND resolved_at < now() - interval '90 days'`,
  `DELETE FROM job_run WHERE started_at < now() - interval '30 days'`,
  `DELETE FROM message WHERE created_at < now() - interval '18 months'`,
];

export const REAPER_SQL = `
UPDATE observation SET t_end = ingested_at
WHERE t_end IS NULL
  AND ingested_at < now() - make_interval(secs => 2 * $1 + 60)`;
// $1 = LOR_ACTIVITY_FLUSH_S. Grace window (60s) lets late agent flushes land.

export const MERGE_BLOCKS_SQL = `
-- Merge adjacent uncommitted same-intent non-calendar blocks, gap ≤120s.
-- Implemented as: extend the earlier block, delete the later, re-point observations.
WITH ordered AS (
  SELECT id, t_start, t_end, intent_id,
         lag(t_end) OVER (PARTITION BY intent_id ORDER BY t_start) AS prev_end,
         lag(id) OVER (PARTITION BY intent_id ORDER BY t_start) AS prev_id
  FROM time_block
  WHERE committed = false AND origin <> 'calendar' AND t_end IS NOT NULL
    AND t_start >= $1 AND t_start < $2
)
SELECT id, prev_id FROM ordered
WHERE prev_end IS NOT NULL AND t_start - prev_end <= interval '120 seconds'`;
