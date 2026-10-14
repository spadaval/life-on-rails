// Structured logs: one JSON line per event to stdout (journald under systemd).
// Titles and subjects may be logged; secrets never.
import { env } from "./env.js";

const TOKEN_PATTERNS: RegExp[] = [
  /Bearer\s+[A-Za-z0-9._~+/=-]{8,}/g,
  /ya29\.[A-Za-z0-9._-]{10,}/g, // Google access token
  /1\/\/[A-Za-z0-9._-]{20,}/g, // Google refresh token
  /\/api\/hooks\/[^/\s"]+\/[^/\s"]+/g, // T3 webhook URL (the path carries the token)
];

function secretValues(): string[] {
  return [env.LOR_AGENT_TOKEN, env.LOR_WAKE_URL].filter((v) => v.length >= 8);
}

/** Remove bearer tokens, Google tokens, and configured secrets from a string. */
export function scrubLog(text: string, extraSecrets: string[] = []): string {
  let out = text;
  for (const s of [...secretValues(), ...extraSecrets]) {
    if (s.length >= 8) out = out.split(s).join("[redacted]");
  }
  for (const rx of TOKEN_PATTERNS) out = out.replace(rx, "[redacted]");
  return out;
}

export type Level = "debug" | "info" | "warn" | "error";

export function log(level: Level, msg: string, fields: Record<string, unknown> = {}): void {
  const line = JSON.stringify({ ts: new Date().toISOString(), level, msg, ...fields });
  const out = scrubLog(line);
  if (level === "error" || level === "warn") console.error(out);
  else console.log(out);
}

export function errorMessage(err: unknown): string {
  return err instanceof Error ? err.message : String(err);
}
