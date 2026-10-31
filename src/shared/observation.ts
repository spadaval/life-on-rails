import { z } from "zod";

// The one envelope every sensor uses. Timestamps are ISO-8601 with an offset;
// the server stores timestamptz.
export const ObservationSource = z.enum([
  "calendar",
  "google_tasks",
  "gmail",
  "android",
  "linux_activity",
  "windows_activity",
  "manual",
  "agent",
  "channel",
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
  payload: z.record(z.string(), z.unknown()),
  intent_hint: z.string().uuid().nullable(),
});
export type ObservationEnvelope = z.infer<typeof ObservationEnvelope>;
