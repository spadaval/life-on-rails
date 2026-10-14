// Newsletters and notifications are recorded and never wake the agent. Fixed
// rules, no model: deciding what matters among the rest is the agent's work.

export interface MailHeaders {
  from: string | null;
  subject: string | null;
  listUnsubscribe: string | null;
  precedence: string | null;
}

const BULK_LABELS = new Set(["CATEGORY_PROMOTIONS", "CATEGORY_SOCIAL", "CATEGORY_UPDATES", "CATEGORY_FORUMS", "SPAM"]);

export function isBulk(headers: MailHeaders, labels: string[], senderIsKnown: boolean): boolean {
  if (senderIsKnown) return false; // someone the owner knows is never bulk
  if (headers.listUnsubscribe) return true;
  if (headers.precedence && /^(bulk|list|junk)$/i.test(headers.precedence.trim())) return true;
  return labels.some((l) => BULK_LABELS.has(l));
}

/** "Priya Raman <priya@example.com>" → name and lowercased address. */
export function parseFrom(from: string | null): { name: string | null; email: string | null } {
  if (!from) return { name: null, email: null };
  const angle = /^\s*(.*?)\s*<([^<>\s]+@[^<>\s]+)>\s*$/.exec(from);
  if (angle) return { name: angle[1]!.replace(/^"|"$/g, "").trim() || null, email: angle[2]!.toLowerCase() };
  const bare = /([^\s<>"]+@[^\s<>"]+)/.exec(from);
  return { name: null, email: bare ? bare[1]!.toLowerCase() : null };
}

export function headersOf(list: { name: string; value: string }[]): MailHeaders {
  const get = (name: string) => list.find((h) => h.name.toLowerCase() === name)?.value ?? null;
  return { from: get("from"), subject: get("subject"), listUnsubscribe: get("list-unsubscribe"), precedence: get("precedence") };
}
