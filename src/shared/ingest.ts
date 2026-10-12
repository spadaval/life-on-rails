import { z } from "zod";
import { ObservationEnvelope } from "./observation.js";

// HTTP contract for POST /ingest (server route src/routes/ingest.ts).
// Loopback only in v1, Bearer LOR_INGEST_TOKEN.
export const IngestBody = z.array(ObservationEnvelope).min(1).max(500);
export type IngestBody = z.infer<typeof IngestBody>;

export interface IngestResult {
  stored: number;
  /** Window-title samples dropped because LOR_ACTIVITY_ENABLED=false. */
  droppedDisabled: number;
  /** ids rejected by validation or monotonic-t_end guard. */
  rejected: string[];
}

export function ingestAuthHeader(token: string): Record<string, string> {
  return { authorization: `Bearer ${token}` };
}
