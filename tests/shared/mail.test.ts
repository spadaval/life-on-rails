import { describe, expect, it } from "vitest";
import { headersOf, isBulk, parseFrom } from "../../src/server/mail/bulk.js";
import { isDue, wakeText } from "../../src/server/mail/wake.js";

const h = (extra: Record<string, string> = {}) =>
  headersOf(Object.entries({ From: "Jo <jo@example.com>", Subject: "Hi", ...extra }).map(([name, value]) => ({ name, value })));

describe("bulk rules", () => {
  it("marks list mail, bulk precedence, and category labels as bulk", () => {
    expect(isBulk(h(), ["INBOX"], false)).toBe(false);
    expect(isBulk(h({ "List-Unsubscribe": "<mailto:u@x.com>" }), ["INBOX"], false)).toBe(true);
    expect(isBulk(h({ Precedence: "bulk" }), ["INBOX"], false)).toBe(true);
    expect(isBulk(h({ precedence: "List" }), ["INBOX"], false)).toBe(true);
    expect(isBulk(h(), ["INBOX", "CATEGORY_PROMOTIONS"], false)).toBe(true);
    expect(isBulk(h(), ["INBOX", "CATEGORY_UPDATES"], false)).toBe(true);
    expect(isBulk(h(), ["INBOX", "CATEGORY_PERSONAL", "IMPORTANT"], false)).toBe(false);
  });

  it("a known sender is never bulk", () => {
    expect(isBulk(h({ "List-Unsubscribe": "<mailto:u@x.com>" }), ["INBOX", "CATEGORY_UPDATES"], true)).toBe(false);
  });

  it("parses the From header", () => {
    expect(parseFrom('"Priya Raman" <Priya@Example.com>')).toEqual({ name: "Priya Raman", email: "priya@example.com" });
    expect(parseFrom("billing@acme.com")).toEqual({ name: null, email: "billing@acme.com" });
    expect(parseFrom(null)).toEqual({ name: null, email: null });
  });
});

describe("wake timing", () => {
  const cfg = { quietS: 180, maxS: 900 };
  const now = new Date("2026-10-14T18:00:00Z");
  const ago = (s: number) => new Date(now.getTime() - s * 1000);
  const row = (firstS: number, lastS: number, priority = false) => ({ first_seen: ago(firstS), last_seen: ago(lastS), priority });

  it("waits for quiet, but not forever, and not at all for a priority sender", () => {
    expect(isDue([], now, cfg)).toBe(false);
    expect(isDue([row(60, 60)], now, cfg)).toBe(false);
    expect(isDue([row(200, 181)], now, cfg)).toBe(true); // quiet for three minutes
    expect(isDue([row(600, 10)], now, cfg)).toBe(false); // still arriving
    expect(isDue([row(901, 10)], now, cfg)).toBe(true); // oldest has waited fifteen minutes
    expect(isDue([row(5, 5, true)], now, cfg)).toBe(true);
  });

  it("the wake text names each message with its Gmail id", () => {
    const text = wakeText([{ gmail_id: "18c2", thread_id: "t1", from: "Jo <jo@x.com>", subject: "Lunch?", date: "", known_sender: true }]);
    expect(text).toBe("1 new message in the inbox:\n- Jo <jo@x.com> — Lunch? (gmail id 18c2, thread t1)");
  });
});
