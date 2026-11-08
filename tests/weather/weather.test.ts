import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { FAILED_MS, FRESH_MS, type Place, parseConditions, weatherCache } from "../../src/server/weather.js";

const reply = JSON.parse(readFileSync(new URL("./fixtures/currentConditions.json", import.meta.url), "utf8"));
const KEY = "AIzaTestKey-not-real-0123456789";
const HERE: Place = { lat: 37.4323, lon: -121.8996 };
const THERE: Place = { lat: 40.7128, lon: -74.006 };

describe("parseConditions", () => {
  it("turns Google's reply into what the phone gets", () => {
    expect(parseConditions(reply)).toEqual({ temp_c: 17.3, condition: "Partly cloudy", is_day: true, observed_at: "2026-11-07T22:14:31.442Z" });
  });

  it("refuses a reply without a temperature", () => {
    const { temperature: _, ...rest } = reply;
    expect(() => parseConditions(rest)).toThrow();
    expect(() => parseConditions({ ...reply, temperature: { unit: "CELSIUS" } })).toThrow();
  });
});

/** A cache over a fake clock and a fetch that answers what `answer` says, recording every call and log line. */
function harness(answer: (url: URL, init: RequestInit | undefined) => Promise<Response>, key = KEY) {
  let t = Date.parse("2026-11-07T12:00:00Z");
  const calls: URL[] = [];
  const lines: string[] = [];
  let place: Place | null = HERE;
  let placeReads = 0;
  const cache = weatherCache({
    key: () => key,
    fetch: (async (input: URL, init?: RequestInit) => {
      calls.push(input);
      t += 120; // the call takes 120 ms
      return answer(input, init);
    }) as typeof fetch,
    now: () => t,
    log: (line) => lines.push(line),
    timeoutMs: 20,
  });
  return {
    calls,
    lines,
    read: () => cache.read(async () => { placeReads++; return place; }),
    advance: (ms: number) => { t += ms; },
    moveTo: (p: Place | null) => { place = p; },
    placeReads: () => placeReads,
  };
}

const ok = async () => new Response(JSON.stringify(reply), { status: 200 });
const weather = parseConditions(reply);

/** No line may carry the key or either coordinate. */
function expectClean(lines: string[]) {
  for (const line of lines) {
    expect(line).not.toContain(KEY);
    for (const p of [HERE, THERE]) {
      expect(line).not.toContain(String(p.lat));
      expect(line).not.toContain(String(p.lon));
    }
  }
}

describe("weatherCache", () => {
  it("calls Google once, then holds the reply 30 minutes whatever the location does", async () => {
    const h = harness(ok);
    expect(await h.read()).toEqual(weather);
    expect(h.calls).toHaveLength(1);
    const url = h.calls[0]!;
    expect(url.origin + url.pathname).toBe("https://weather.googleapis.com/v1/currentConditions:lookup");
    expect(Object.fromEntries(url.searchParams)).toEqual({
      key: KEY, "location.latitude": "37.4323", "location.longitude": "-121.8996", unitsSystem: "METRIC",
    });

    h.moveTo(THERE);
    h.advance(FRESH_MS - 1000);
    expect(await h.read()).toEqual(weather);
    expect(h.calls).toHaveLength(1);
    expect(h.placeReads()).toBe(1); // a fresh entry doesn't even look up the location

    h.advance(1000);
    expect(await h.read()).toEqual(weather);
    expect(h.calls).toHaveLength(2);
    expect(h.calls[1]!.searchParams.get("location.latitude")).toBe("40.7128");
    expect(h.calls[1]!.searchParams.get("location.longitude")).toBe("-74.006");

    expect(h.lines).toEqual(["weather: 200 in 120 ms", "weather: 200 in 120 ms"]);
    expectClean(h.lines);
  });

  const failures: [string, (url: URL, init: RequestInit | undefined) => Promise<Response>, string][] = [
    ["a failed call", async () => { throw new TypeError("fetch failed"); }, "weather: TypeError in 120 ms"],
    ["a non-200 reply", async () => new Response(`{"error":{"message":"API key not valid. key=${KEY}"}}`, { status: 403 }), "weather: 403 in 120 ms"],
    ["a 200 without a temperature", async () => new Response(JSON.stringify({ ...reply, temperature: {} }), { status: 200 }), "weather: 200 ZodError in 120 ms"],
    [
      "a timeout",
      // Never answers; only the cache's own timeout ends it.
      (_url, init) => new Promise((_, reject) => init!.signal!.addEventListener("abort", () => reject(init!.signal!.reason))),
      "weather: timeout in 120 ms",
    ],
  ];

  for (const [what, answer, line] of failures) {
    it(`gives null after ${what}, calls nothing for 5 minutes, then calls again`, async () => {
      const h = harness(answer);
      expect(await h.read()).toBeNull();
      expect(h.calls).toHaveLength(1);
      h.advance(FAILED_MS - 1000);
      expect(await h.read()).toBeNull();
      expect(h.calls).toHaveLength(1);
      h.advance(1000);
      expect(await h.read()).toBeNull();
      expect(h.calls).toHaveLength(2);
      expect(h.lines).toEqual([line, line]);
      expectClean(h.lines);
    });
  }

  it("recovers on the first call after a failure's 5 minutes", async () => {
    let fail = true;
    const h = harness(async () => (fail ? new Response("", { status: 503 }) : ok()));
    expect(await h.read()).toBeNull();
    fail = false;
    h.advance(FAILED_MS);
    expect(await h.read()).toEqual(weather);
  });

  it("calls nothing and gives null without a key", async () => {
    const h = harness(ok, "");
    expect(await h.read()).toBeNull();
    h.advance(FRESH_MS * 2);
    expect(await h.read()).toBeNull();
    expect(h.calls).toEqual([]);
    expect(h.lines).toEqual([]);
    expect(h.placeReads()).toBe(0);
  });

  it("calls nothing and gives null without a location, and calls once one arrives", async () => {
    const h = harness(ok);
    h.moveTo(null);
    expect(await h.read()).toBeNull();
    expect(await h.read()).toBeNull();
    expect(h.calls).toEqual([]);
    h.moveTo(HERE);
    expect(await h.read()).toEqual(weather);
    expect(h.calls).toHaveLength(1);
  });
});
