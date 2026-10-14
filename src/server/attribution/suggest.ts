// Suggestion matching: attribution_rule (app glob / title regex / URL glob /
// place) then calendar overlap.
export interface Rule {
  id: string;
  matchDevice: string | null;
  matchApp: string | null;
  matchTitle: string | null;
  matchUrl: string | null;
  /** A place entity id. */
  matchPlace: string | null;
  intentId: string;
  autoApply: boolean;
  priority: number;
}

export interface Candidate {
  app: string | null;
  title: string | null;
  url: string | null;
  device: string | null;
  /** The place entity the candidate is at, for location stays. */
  place: string | null;
}

/** validating regex on write: must compile, ≤200 chars. */
export function validateTitleRegex(pattern: string): boolean {
  if (pattern.length === 0 || pattern.length > 200) return false;
  try {
    // eslint-disable-next-line no-new
    new RegExp(pattern, "i");
    return true;
  } catch {
    return false;
  }
}

/** `*`/`?` glob (case-insensitive), matched in JS. */
export function globMatch(pattern: string, value: string): boolean {
  const rx = new RegExp(
    "^" + pattern.replace(/[.+^${}()|[\]\\]/g, "\\$&").replace(/\*/g, ".*").replace(/\?/g, ".") + "$",
    "i",
  );
  return rx.test(value);
}

function ruleMatches(rule: Rule, c: Candidate): boolean {
  if (rule.matchDevice !== null && rule.matchDevice !== c.device) return false;
  if (rule.matchPlace !== null && rule.matchPlace !== c.place) return false;
  if (rule.matchApp !== null) {
    if (c.app === null) return false;
    if (!globMatch(rule.matchApp, c.app)) return false;
  }
  if (rule.matchTitle !== null) {
    if (c.title === null) return false;
    let rx: RegExp;
    try {
      rx = new RegExp(rule.matchTitle, "i"); // same semantics as SQL ~*
    } catch {
      return false;
    }
    if (!rx.test(c.title)) return false;
  }
  if (rule.matchUrl !== null) {
    if (c.url === null) return false;
    const origin = (() => {
      try {
        const u = new URL(c.url);
        return u.origin + u.pathname;
      } catch {
        return c.url;
      }
    })();
    if (!globMatch(rule.matchUrl, origin) && !globMatch(rule.matchUrl, c.url)) return false;
  }
  return true;
}

/** Highest priority, then insertion order. Returns null when nothing matches. */
export function matchRule(rules: Rule[], c: Candidate): Rule | null {
  const sorted = [...rules].sort((a, b) => b.priority - a.priority);
  return sorted.find((r) => ruleMatches(r, c)) ?? null;
}

/** Calendar overlap suggests intent when one event covers ≥50% of the block. */
export function calendarOverlapSuggest(args: {
  blockStart: number;
  blockEnd: number;
  eventStart: number;
  eventEnd: number;
  eventIntentId: string;
}): string | null {
  const { blockStart, blockEnd, eventStart, eventEnd, eventIntentId } = args;
  if (!(blockEnd > blockStart)) return null;
  const overlap = Math.max(0, Math.min(blockEnd, eventEnd) - Math.max(blockStart, eventStart));
  return overlap / (blockEnd - blockStart) >= 0.5 ? eventIntentId : null;
}
