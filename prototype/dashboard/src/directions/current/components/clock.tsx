// The page's clock: the prototype store's (a scene's time, moving unless `still=1`), in place
// of the real dashboard's fixed-clock context (src/components/clock.tsx).
import { now, useNow as useStoreNow } from "../../../data/store.js";

/** Now in milliseconds as of this render. */
export function useRenderNow(): number {
  return now();
}

/** Now in milliseconds, re-read every `everyMs` (null: only when the data changes). */
export function useNow(everyMs: number | null, _initial?: string): number {
  return useStoreNow(everyMs ?? 3_600_000);
}
