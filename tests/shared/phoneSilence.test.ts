import { describe, expect, it } from "vitest";
import { ago as agoText } from "../../src/components/format.js";
import { PHONE_SILENT_AFTER_MS, healthLine, phoneSilentMs } from "../../src/shared/phoneSilence.js";

const NOW = Date.parse("2026-11-25T18:00:00Z");
const ago = (h: number) => new Date(NOW - h * 3600_000).toISOString();
const line = (unwell: string[] | null, last: string | null) => healthLine(unwell, phoneSilentMs(last, NOW));

describe("the sidebar's health line", () => {
  it("is quiet while the phone reports", () => {
    expect(line([], ago(0.5))).toEqual({ ok: true, text: "All senses reporting" });
    expect(phoneSilentMs(new Date(NOW - PHONE_SILENT_AFTER_MS).toISOString(), NOW)).toBeNull();
  });

  it("names a phone silent past the threshold, with how long", () => {
    expect(line([], ago(5.2))).toEqual({ ok: false, text: "Phone silent for 5h" });
    expect(line([], ago(80))).toEqual({ ok: false, text: "Phone silent for 3d" });
  });

  it("says the same hours or days as the phone's \"ago\" on /status", () => {
    for (const h of [2.1, 4.4, 4.6, 23.7, 35.9, 36.1, 47.5, 80]) {
      const row = agoText(ago(h), NOW).replace(" ago", "");
      expect(line([], ago(h)).text).toBe(`Phone silent for ${row}`);
    }
  });

  it("says nothing about a phone that has never reported", () => {
    expect(line([], null)).toEqual({ ok: true, text: "All senses reporting" });
  });

  it("puts jobs first when both need a look", () => {
    expect(line(["sync", "nightly"], ago(5))).toEqual({ ok: false, text: "2 jobs need a look · phone silent for 5h" });
    expect(line(["sync"], ago(1))).toEqual({ ok: false, text: "1 job needs a look" });
  });

  it("says when the database cannot be read", () => {
    expect(line(null, ago(5))).toEqual({ ok: false, text: "Database unreachable" });
  });
});
