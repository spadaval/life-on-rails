import { ago } from "./format.js";

/**
 * "5m ago". The server and the browser render this a moment apart, so the text
 * may differ by a second at hydration; that is expected, not an error.
 */
export function Ago({ iso }: { iso: string | null }) {
  return <span suppressHydrationWarning>{ago(iso)}</span>;
}
