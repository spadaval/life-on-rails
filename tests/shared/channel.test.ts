import { describe, expect, it } from "vitest";
import { MAX_MESSAGE_CHARS, splitMessage, wrapInbound } from "../../src/shared/channel.js";

describe("splitMessage", () => {
  it("leaves a short message alone", () => {
    expect(splitMessage(" hello ")).toEqual(["hello"]);
  });
  it("cuts at a paragraph, and no part is over the limit", () => {
    const a = "a".repeat(3000);
    const b = "b".repeat(3000);
    expect(splitMessage(`${a}\n\n${b}`)).toEqual([a, b]);
    const parts = splitMessage("x".repeat(MAX_MESSAGE_CHARS * 2 + 5));
    expect(parts).toHaveLength(3);
    expect(parts.every((p) => p.length <= MAX_MESSAGE_CHARS)).toBe(true);
  });
});

describe("wrapInbound", () => {
  const base = { at: "2026-10-31 14:02", via: "WhatsApp", text: "yes", sent: [] };
  it("says on every message that only lor message reaches him", () => {
    const out = wrapInbound(base);
    expect(out).toContain("He is not reading this thread");
    expect(out).toContain("`lor message`");
    expect(out).toContain("`lor message history`");
    expect(out.endsWith("yes")).toBe(true);
  });
  it("carries what other runs sent, so a one-word reply means something", () => {
    const out = wrapInbound({ ...base, sent: [{ at: "09:12", from: "claude:abc", text: "Draft the notice?", intentId: "t-1" }] });
    expect(out).toContain("09:12, claude:abc (task t-1): Draft the notice?");
  });
  it("points at a voice note", () => {
    const out = wrapInbound({ ...base, text: "", file: { kind: "audio", path: "/x/a.ogg", voice: true } });
    expect(out).toContain("transcribe");
    expect(out).toContain("/x/a.ogg");
  });
});
