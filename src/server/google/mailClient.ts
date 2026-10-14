// Gmail (headers only), behind a port. LoR's login has
// gmail.metadata: it can see who a message is from and its subject, and
// cannot fetch a body.
import { z } from "zod";
import { googleGet } from "./client.js";

const GMAIL = "https://gmail.googleapis.com/gmail/v1/users/me";

/** Gmail no longer has history that far back (it keeps about a week). */
export class HistoryExpiredError extends Error {
  constructor() {
    super("Gmail history id expired");
    this.name = "HistoryExpiredError";
  }
}

export const GMessage = z.object({
  id: z.string(),
  threadId: z.string(),
  labelIds: z.array(z.string()).default([]),
  internalDate: z.string().optional(),
  payload: z.object({ headers: z.array(z.object({ name: z.string(), value: z.string() })).default([]) }).default({ headers: [] }),
});
export type GMessage = z.infer<typeof GMessage>;

const HistoryPage = z.object({
  history: z
    .array(z.object({ messagesAdded: z.array(z.object({ message: z.object({ id: z.string(), labelIds: z.array(z.string()).default([]) }) })).default([]) }))
    .default([]),
  nextPageToken: z.string().optional(),
  historyId: z.string().optional(),
});

export interface MailPort {
  /** The mailbox's current history id. */
  currentHistoryId(): Promise<string>;
  /** Ids of messages added to INBOX or SENT after `startHistoryId` (drafts left out), and the id to continue from. */
  addedSince(startHistoryId: string): Promise<{ ids: string[]; historyId: string }>;
  /** Ids of the most recent INBOX and SENT messages. */
  recentMessages(max: number): Promise<string[]>;
  /** Headers and labels. Null if the message is gone. */
  metadata(id: string): Promise<GMessage | null>;
}

const HEADERS = ["From", "To", "Cc", "Subject", "Date", "List-Unsubscribe", "Precedence"];

export const googleMail: MailPort = {
  async currentHistoryId() {
    const profile = z.object({ historyId: z.string() }).parse(await googleGet(`${GMAIL}/profile`));
    return profile.historyId;
  },

  async addedSince(startHistoryId) {
    const ids = new Set<string>();
    let pageToken: string | undefined;
    let historyId = startHistoryId;
    do {
      // No label filter: one pass sees both what arrived and what the owner sent.
      const qs = new URLSearchParams({ startHistoryId, historyTypes: "messageAdded", maxResults: "500", ...(pageToken ? { pageToken } : {}) });
      let raw: unknown;
      try {
        raw = await googleGet(`${GMAIL}/history?${qs}`);
      } catch (err) {
        if (err instanceof Error && /Google 404/.test(err.message)) throw new HistoryExpiredError();
        throw err;
      }
      const page = HistoryPage.parse(raw);
      for (const h of page.history) {
        for (const m of h.messagesAdded) {
          const labels = m.message.labelIds;
          if (labels.includes("DRAFT")) continue; // every draft save adds a message
          if (labels.includes("INBOX") || labels.includes("SENT")) ids.add(m.message.id);
        }
      }
      if (page.historyId) historyId = page.historyId;
      pageToken = page.nextPageToken;
    } while (pageToken);
    return { ids: [...ids], historyId };
  },

  async recentMessages(max) {
    const ids: string[] = [];
    for (const label of ["INBOX", "SENT"]) {
      const qs = new URLSearchParams({ labelIds: label, maxResults: String(max) });
      const page = z.object({ messages: z.array(z.object({ id: z.string() })).default([]) }).parse(await googleGet(`${GMAIL}/messages?${qs}`));
      ids.push(...page.messages.map((m) => m.id));
    }
    return [...new Set(ids)];
  },

  async metadata(id) {
    const qs = new URLSearchParams({ format: "metadata" });
    for (const h of HEADERS) qs.append("metadataHeaders", h);
    try {
      return GMessage.parse(await googleGet(`${GMAIL}/messages/${encodeURIComponent(id)}?${qs}`));
    } catch (err) {
      if (err instanceof Error && /Google 404/.test(err.message)) return null;
      throw err;
    }
  },
};

