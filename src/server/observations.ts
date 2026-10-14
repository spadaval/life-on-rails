// Observation writes. Sensors write these; nothing here is ever scored.
import { createHash } from "node:crypto";
import type { Tx } from "../db/client.js";
import { uuidv7 } from "../shared/ids.js";
import type { ObservationSource } from "../shared/observation.js";

export interface ObservationInput {
  id?: string;
  tStart: Date;
  tEnd?: Date | null;
  device: string;
  source: ObservationSource;
  app?: string | null;
  calendarId?: string | null;
  title?: string | null;
  url?: string | null;
  payload?: Record<string, unknown>;
  intentHint?: string | null;
}

/**
 * A stable UUID for something that has its own identity elsewhere (a Gmail
 * message, a calendar event), so recording it twice is one row.
 */
export function stableId(kind: string, key: string): string {
  const h = createHash("sha256").update(`${kind}\n${key}`).digest("hex");
  // Shaped as a version-8 (custom) UUID.
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-8${h.slice(13, 16)}-a${h.slice(17, 20)}-${h.slice(20, 32)}`;
}

/**
 * Insert, or extend. Sending an id again with a later t_end extends the row;
 * it never shortens it and never reopens a closed one.
 */
export async function recordObservation(tx: Tx, o: ObservationInput): Promise<{ id: string; inserted: boolean }> {
  const id = o.id ?? uuidv7();
  const rows = await tx<{ inserted: boolean }[]>`
    INSERT INTO observation (id, t_start, t_end, device, source, app, calendar_id, title, url, payload, intent_hint)
    VALUES (${id}, ${o.tStart}, ${o.tEnd ?? null}, ${o.device}, ${o.source}, ${o.app ?? null},
      ${o.calendarId ?? null}, ${o.title ?? null}, ${o.url ?? null}, ${tx.json((o.payload ?? {}) as never)},
      ${o.intentHint ?? null})
    ON CONFLICT (id) DO UPDATE SET t_end = EXCLUDED.t_end, payload = EXCLUDED.payload
      WHERE EXCLUDED.t_end IS NOT NULL AND (observation.t_end IS NULL OR EXCLUDED.t_end > observation.t_end)
    RETURNING (xmax = 0) AS inserted`;
  return { id, inserted: rows[0]?.inserted ?? false };
}
