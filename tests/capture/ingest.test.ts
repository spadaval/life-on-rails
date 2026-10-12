import { describe, expect, it } from "vitest";
import { classifyIngest } from "../../src/server/capture/ingest.js";
import { parseSeatInfo, shouldReportIdle } from "../../src/server/capture/idle.js";
import { capUntrustedConfidence, redactObservation, scrubLog } from "../../src/server/agent/redact.js";
import type { ObservationEnvelope } from "../../src/shared/observation.js";

const hb = (over: Partial<ObservationEnvelope> = {}) =>
  ({
    id: "123e4567-e89b-12d3-a456-426614174000",
    t_start: "2026-10-12T09:00:00.000-07:00",
    t_end: null,
    device: "buildbot",
    source: "linux_activity",
    app: null,
    calendar_id: null,
    title: "no_session",
    url: null,
    payload: {},
    intent_hint: null,
    ...over,
  }) as ObservationEnvelope;

describe("ingest classification", () => {
  it("heartbeats persist even when activity is disabled", () => {
    expect(classifyIngest(hb(), false)).toBe("store");
    expect(classifyIngest(hb({ title: "idle" }), false)).toBe("store");
  });
  it("window titles drop with 202 when disabled", () => {
    expect(classifyIngest(hb({ title: "my editor", app: "Code" }), false)).toBe("drop-disabled");
    expect(classifyIngest(hb({ title: "my editor", app: "Code" }), true)).toBe("store");
  });
});

describe("idle seat guard", () => {
  it("only CLASS=user with a seat reports idle", () => {
    expect(shouldReportIdle(parseSeatInfo("Class=user\nSeat=seat0\n"))).toBe(true);
    expect(shouldReportIdle(parseSeatInfo("Class=manager\nSeat=\n"))).toBe(false);
    expect(shouldReportIdle(parseSeatInfo("Class=user\nSeat=\n"))).toBe(false);
  });
});

describe("redaction", () => {
  it("calendar titles pass (truncated); window data summarized", () => {
    const cal = redactObservation({ id: "a", source: "calendar", app: null, title: "Team standup with the whole crew", url: null });
    expect(cal.summary).toContain("Team standup");
    const win = redactObservation({ id: "b", source: "linux_activity", app: "Code", title: "secret-plan.md — editing", url: "https://github.com/org/repo/pull/1/files" });
    expect(win.summary).not.toContain("secret-plan");
    expect(win.summary).not.toContain("/org/repo/pull/1/files");
    expect(win.summary).toContain("https://github.com");
  });
  it("long calendar titles truncate at 200", () => {
    const r = redactObservation({ id: "a", source: "calendar", app: null, title: "x".repeat(500), url: null });
    expect(r.summary.length).toBeLessThanOrEqual("calendar: ".length + 200);
  });
  it("untrusted agent confidence capped at 0.6", () => {
    expect(capUntrustedConfidence(0.95, "agent-untrusted")).toBe(0.6);
    expect(capUntrustedConfidence(0.4, "agent-untrusted")).toBe(0.4);
    expect(capUntrustedConfidence(0.95, "rule")).toBe(0.95);
  });
  it("log scrubbing", () => {
    expect(scrubLog("key=xai-abcdef1234567890")).not.toContain("xai-abcdef");
    expect(scrubLog("GOG_KEYRING_PASSWORD=hunter2")).not.toContain("hunter2");
  });
});
