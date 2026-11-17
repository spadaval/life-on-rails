// The page's clock. On the test database the server can hand down a fixed time
// (LOR_FAKE_NOW, or `?at=` for one tab; src/server/clock.ts), and then every
// screen reads that instead of the browser's own time and stops ticking.
import { createContext, useContext, useEffect, useState } from "react";

const FixedClock = createContext<string | null>(null);

/** Set at the root from the shell: the fixed ISO time, or null for the browser's clock. */
export const FixedClockProvider = FixedClock.Provider;

/** Now in milliseconds, re-read every `everyMs` (null: once). `initial` is the server's now, so the first render agrees with it. */
export function useNow(everyMs: number | null, initial?: string): number {
  const fixed = useContext(FixedClock);
  const [now, setNow] = useState(() => Date.parse(fixed ?? initial ?? "") || Date.now());
  useEffect(() => {
    if (fixed !== null || everyMs === null) return;
    setNow(Date.now());
    const id = setInterval(() => setNow(Date.now()), everyMs);
    return () => clearInterval(id);
  }, [everyMs, fixed]);
  return fixed !== null ? Date.parse(fixed) : now;
}
