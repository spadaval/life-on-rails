import { z } from "zod";

// Uniform capture envelope (spec: Time attribution and sync / Capture event envelope).
// All timestamps are UTC ISO-8601 with offset; agents send offsets, server stores timestamptz.
export const ObservationSource = z.enum([
  "calendar",
  "linux_activity",
  "windows_activity",
  "android",
  "manual",
  "agent",
  "system",
]);
export type ObservationSource = z.infer<typeof ObservationSource>;

export const ObservationEnvelope = z.object({
  id: z.string().uuid(),
  t_start: z.string().datetime({ offset: true }),
  t_end: z.string().datetime({ offset: true }).nullable(),
  device: z.string().min(1),
  source: ObservationSource,
  app: z.string().nullable(),
  calendar_id: z.string().nullable(),
  title: z.string().nullable(),
  url: z.string().nullable(),
  // Source-specific, stays on box. Never shipped to the LLM (see agent/redact.ts).
  payload: z.record(z.string(), z.unknown()),
  intent_hint: z.string().uuid().nullable(),
});
export type ObservationEnvelope = z.infer<typeof ObservationEnvelope>;

// Heartbeat the linux-activity agent emits when no graphical session exists.
// /ingest always accepts these, even when LOR_ACTIVITY_ENABLED=false.
export function isNoSessionHeartbeat(e: ObservationEnvelope): boolean {
  return e.source === "linux_activity" && e.title === "no_session";
}

// Window-title sample (dropped with 202 when LOR_ACTIVITY_ENABLED=false).
export function isWindowTitleSample(e: ObservationEnvelope): boolean {
  return (
    e.source === "linux_activity" && e.title !== null && e.title !== "no_session" && e.title !== "idle"
  );
}
