import { inflateSync } from "node:zlib";
import { describe, expect, it } from "vitest";
import { frameHtml } from "../../src/server/trmnl/frame.js";
import { grayPng2 } from "../../src/server/trmnl/png.js";
import { sample } from "./sample.js";

const at = (hm: string) => new Date(`2026-10-15T${hm}:00-04:00`);
const text = (html: string) => html.replace(/<style>[\s\S]*?<\/style>/, "").replace(/<[^>]+>/g, " ").replace(/\s+/g, " ");

describe("grayPng2", () => {
  it("packs four pixels to a byte, leftmost highest, a filter byte per row", () => {
    const png = grayPng2(5, 2, Uint8Array.from([0, 1, 2, 3, 3, 3, 2, 1, 0, 0]));
    expect(png.subarray(1, 4).toString()).toBe("PNG");
    expect(png.readUInt32BE(16)).toBe(5);
    expect(png.readUInt32BE(20)).toBe(2);
    expect([png[24], png[25]]).toEqual([2, 0]);
    const idatLen = png.readUInt32BE(33);
    expect(png.subarray(37, 41).toString()).toBe("IDAT");
    const raw = inflateSync(png.subarray(41, 41 + idatLen));
    expect([...raw]).toEqual([0, 0b00011011, 0b11000000, 0, 0b11100100, 0]);
  });

  it("refuses a pixel count that does not match", () => {
    expect(() => grayPng2(2, 2, new Uint8Array(3))).toThrow();
  });
});

describe("frameHtml", () => {
  it("shows what is left of the day, what runs, and only events still ahead", () => {
    const t = text(frameHtml(sample()));
    expect(t).toContain("Thursday 15 October");
    expect(t).toContain("Updated 14:55");
    expect(t).toContain("7h 5m left");
    expect(t).toContain("until 22:00");
    expect(t).toContain("Thesis since 13:20");
    expect(t).toContain("16:00 Supervisor meeting");
    expect(t).toContain("19:30 Dinner with Sam");
    expect(t).not.toContain("Lab standup");
    expect(t).toContain("4 to confirm");
    expect(t).toContain("Approve the dentist reschedule");
  });

  it("before the day starts shows when it does; after it ends, what was recorded", () => {
    expect(text(frameHtml(sample({ now: at("06:30"), running: null })))).toContain("08:00 rising");
    expect(text(frameHtml(sample({ now: at("23:10"), running: null })))).toContain("5h recorded");
  });

  it("caps Needs you at three and says how many more", () => {
    const t = text(frameHtml(sample({ needsYou: ["a", "b", "c", "d", "e"] })));
    expect(t).toContain("and 2 more");
    expect(t).not.toContain(" d ");
  });

  it("puts alerts in the head, and escapes titles", () => {
    const html = frameHtml(sample({ alerts: ["Battery 12%"], needsYou: ["<b>bold</b> & co"] }));
    expect(html).toContain('<span class="alert cond">Battery 12%</span>');
    expect(html).toContain("&lt;b&gt;bold&lt;/b&gt; &amp; co");
  });

  it("says so when nothing is coming up or flagged", () => {
    const t = text(frameHtml(sample({ events: [], needsYou: [] })));
    expect(t).toContain("Nothing else on the calendar today");
    expect(t).toContain("Nothing flagged");
  });
});
