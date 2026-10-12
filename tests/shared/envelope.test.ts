import { describe, expect, it } from "vitest";
import { IngestBody } from "../../src/shared/ingest.js";
import { ObservationEnvelope, isNoSessionHeartbeat, isWindowTitleSample } from "../../src/shared/observation.js";

const base = {
  id: "123e4567-e89b-12d3-a456-426614174000",
  t_start: "2026-10-12T09:00:00.000-07:00",
  t_end: null,
  device: "buildbot",
  source: "linux_activity",
  app: null,
  calendar_id: null,
  title: "no_session",
  url: null,
  payload: { reason: "no_graphical_session" },
  intent_hint: null,
} as const;

describe("ObservationEnvelope", () => {
  it("accepts a heartbeat", () => {
    const e = ObservationEnvelope.parse({ ...base });
    expect(isNoSessionHeartbeat(e)).toBe(true);
    expect(isWindowTitleSample(e)).toBe(false);
  });
  it("classifies window-title samples", () => {
    const e = ObservationEnvelope.parse({ ...base, title: "my editor", app: "Code" });
    expect(isWindowTitleSample(e)).toBe(true);
  });
  it("rejects naive datetimes and bad ids", () => {
    expect(() => ObservationEnvelope.parse({ ...base, t_start: "2026-10-12 09:00:00" })).toThrow();
    expect(() => ObservationEnvelope.parse({ ...base, id: "nope" })).toThrow();
  });
  it("ingest body bounds batch size", () => {
    expect(IngestBody.safeParse([]).success).toBe(false);
    expect(IngestBody.safeParse([{ ...base }]).success).toBe(true);
  });
});
