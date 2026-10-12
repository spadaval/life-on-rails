// POST /ingest core: classification + monotonic-t_end upsert SQL.
// DB access is injected as a minimal port so the rules are unit-testable.
import { IngestBody, type IngestResult } from "../../shared/ingest.js";
import { ObservationEnvelope, isWindowTitleSample, type ObservationEnvelope as E } from "../../shared/observation.js";

export interface IngestStore {
  /** Upsert one envelope. Returns "stored" | "dropped-disabled" | "rejected". */
  upsert(e: E, activityEnabled: boolean): Promise<"stored" | "dropped-disabled" | "rejected">;
}

export function classifyIngest(e: E, activityEnabled: boolean): "store" | "drop-disabled" {
  // Heartbeats (no_session) and idle always persist. Window-title samples drop
  // with 202 when LOR_ACTIVITY_ENABLED=false.
  if (!activityEnabled && isWindowTitleSample(e)) return "drop-disabled";
  return "store";
}

export async function ingestBatch(
  store: IngestStore,
  raw: unknown,
  activityEnabled: boolean,
): Promise<{ status: number } & IngestResult> {
  const parsed = IngestBody.safeParse(raw);
  if (!parsed.success) return { status: 400, stored: 0, droppedDisabled: 0, rejected: [] };
  let stored = 0;
  let droppedDisabled = 0;
  const rejected: string[] = [];
  for (const item of parsed.data) {
    const e = ObservationEnvelope.parse(item);
    if (classifyIngest(e, activityEnabled) === "drop-disabled") {
      droppedDisabled++;
      continue;
    }
    const r = await store.upsert(e, activityEnabled);
    if (r === "stored") stored++;
    else if (r === "dropped-disabled") droppedDisabled++;
    else rejected.push(e.id);
  }
  return { status: 200, stored, droppedDisabled, rejected };
}

/** Postgres upsert SQL: monotonic t_end only — never reopen a closed row. */
export const UPSERT_OBSERVATION_SQL = `
INSERT INTO observation (id, t_start, t_end, device, source, app, calendar_id, title, url, payload, intent_hint)
VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)
ON CONFLICT (id) DO UPDATE SET
  t_end = CASE WHEN EXCLUDED.t_end > observation.t_end THEN EXCLUDED.t_end ELSE observation.t_end END,
  title = EXCLUDED.title,
  payload = EXCLUDED.payload
WHERE observation.t_end IS NULL OR EXCLUDED.t_end > observation.t_end`;
