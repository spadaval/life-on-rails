// The weather where the phone last was, for the clock block (decision lor-g4ta):
// Google's current conditions, fetched on read and kept in memory. A success is
// kept 30 minutes from when it was fetched, a failure 5, and the location is
// only looked up when a fetch is due. Nothing here logs the key or coordinates.
import { z } from "zod";

export interface Weather {
  temp_c: number;
  condition: string;
  is_day: boolean;
  observed_at: string;
}

export interface Place {
  lat: number;
  lon: number;
}

const URL_BASE = "https://weather.googleapis.com/v1/currentConditions:lookup";
export const FRESH_MS = 30 * 60_000;
export const FAILED_MS = 5 * 60_000;
const TIMEOUT_MS = 3000;

const Reply = z.object({
  currentTime: z.string(),
  isDaytime: z.boolean(),
  weatherCondition: z.object({ description: z.object({ text: z.string() }) }),
  temperature: z.object({ degrees: z.number() }),
});

/** Google's `currentConditions:lookup` reply as the phone gets it; throws on a reply without what it needs. */
export function parseConditions(body: unknown): Weather {
  const r = Reply.parse(body);
  return {
    temp_c: r.temperature.degrees,
    condition: r.weatherCondition.description.text,
    is_day: r.isDaytime,
    observed_at: new Date(r.currentTime).toISOString(),
  };
}

export interface WeatherDeps {
  /** Read on every fetch, so a key added to the env file needs only a restart. Empty: no weather. */
  key: () => string;
  fetch: typeof globalThis.fetch;
  now: () => number;
  log: (line: string) => void;
  timeoutMs?: number;
}

export interface WeatherCache {
  /** The cached weather, or a fresh fetch at `place()` when the entry has run out. */
  read(place: () => Promise<Place | null>): Promise<Weather | null>;
}

export function weatherCache(deps: WeatherDeps): WeatherCache {
  let entry: { value: Weather | null; until: number } | null = null;

  async function fetchAt(key: string, at: Place): Promise<Weather | null> {
    const url = new URL(URL_BASE);
    url.searchParams.set("key", key);
    url.searchParams.set("location.latitude", String(at.lat));
    url.searchParams.set("location.longitude", String(at.lon));
    url.searchParams.set("unitsSystem", "METRIC");
    const started = deps.now();
    let outcome = "";
    try {
      const res = await deps.fetch(url, { signal: AbortSignal.timeout(deps.timeoutMs ?? TIMEOUT_MS) });
      outcome = String(res.status);
      if (res.status !== 200) return null;
      return parseConditions(await res.json());
    } catch (err) {
      const name = err instanceof Error ? err.name : "Error";
      // A bad body after a 200 keeps the status and adds why.
      outcome = name === "TimeoutError" ? "timeout" : outcome ? `${outcome} ${name}` : name;
      return null;
    } finally {
      deps.log(`weather: ${outcome} in ${deps.now() - started} ms`);
    }
  }

  return {
    async read(place) {
      const now = deps.now();
      if (entry && now < entry.until) return entry.value;
      const key = deps.key();
      if (!key) return null;
      const at = await place();
      if (!at) return null;
      const value = await fetchAt(key, at);
      entry = { value, until: deps.now() + (value ? FRESH_MS : FAILED_MS) };
      return value;
    },
  };
}
