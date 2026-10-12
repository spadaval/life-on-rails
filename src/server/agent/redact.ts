// Redaction layer (spec: Security). Two tiers:
// - Calendar titles pass through (needed for attribution; truncated 200 chars).
// - Window titles / URLs are summarized to app + origin-only URL + IDs.
// Payload blobs, keystroke/mic buffers, and attendee emails never leave the box.
export interface RedactInput {
  app: string | null;
  title: string | null;
  url: string | null;
  source: string;
  id: string;
}

export interface Redacted {
  id: string;
  source: string;
  summary: string;
}

function originOnly(url: string): string {
  try {
    return new URL(url).origin;
  } catch {
    return "(invalid-url)";
  }
}

export function redactObservation(o: RedactInput): Redacted {
  if (o.source === "calendar") {
    const title = (o.title ?? "(no title)").slice(0, 200);
    return { id: o.id, source: o.source, summary: `calendar: ${title}` };
  }
  const parts = [`app: ${o.app ?? "unknown"}`];
  if (o.url) parts.push(`origin: ${originOnly(o.url)}`);
  else if (o.title) parts.push(`title_len: ${o.title.length}`);
  return { id: o.id, source: o.source, summary: parts.join(" ") };
}

/** Agent proposals derived from untrusted window titles: cap confidence 0.6. */
export function capUntrustedConfidence(confidence: number, source: string): number {
  if (source === "agent-untrusted") return Math.min(confidence, 0.6);
  return confidence;
}

const SECRET_PATTERNS = [
  /GOG_KEYRING_PASSWORD\s*=\s*\S+/gi,
  /(XAI_API_KEY|api[_-]?key)\s*=\s*\S+/gi,
  /xai-[a-z0-9_-]{10,}/gi,
  /bearer\s+\S+/gi,
];

/** Structured logs must never carry secrets. Throws-safe: returns "[redacted]". */
export function scrubLog(value: string): string {
  let out = value;
  for (const rx of SECRET_PATTERNS) out = out.replace(rx, "[redacted]");
  return out;
}
