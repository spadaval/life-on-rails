// Gmail (headers only) and Pub/Sub, behind ports. LoR's login has
// gmail.metadata: it can see who a message is from and its subject, and
// cannot fetch a body.
import { z } from "zod";
import { accessToken } from "./auth.js";
import { googleGet } from "./client.js";

const GMAIL = "https://gmail.googleapis.com/gmail/v1/users/me";
const PUBSUB = "https://pubsub.googleapis.com/v1";

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
  /** (Re-)register the push watch on INBOX and SENT. */
  watch(topic: string): Promise<{ historyId: string; expiration: Date }>;
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

  async watch(topic) {
    const res = z.object({ historyId: z.string(), expiration: z.string() }).parse(
      await googleGet(`${GMAIL}/watch`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ topicName: topic, labelIds: ["INBOX", "SENT"], labelFilterBehavior: "INCLUDE" }),
      }),
    );
    return { historyId: res.historyId, expiration: new Date(Number(res.expiration)) };
  },
};

export interface PubSubPort {
  /** Waits for notifications (the request is held open until some arrive or it times out). */
  pull(subscription: string, signal?: AbortSignal): Promise<{ ackId: string }[]>;
  ack(subscription: string, ackIds: string[]): Promise<void>;
}

async function pubsubPost(path: string, body: unknown, signal?: AbortSignal): Promise<unknown> {
  const res = await fetch(`${PUBSUB}/${path}`, {
    method: "POST",
    headers: { authorization: `Bearer ${await accessToken()}`, "content-type": "application/json" },
    body: JSON.stringify(body),
    signal,
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`Pub/Sub ${res.status}: ${text.slice(0, 200)}`);
  return text ? JSON.parse(text) : {};
}

export const googlePubSub: PubSubPort = {
  async pull(subscription, signal) {
    const res = z
      .object({ receivedMessages: z.array(z.object({ ackId: z.string() })).default([]) })
      .parse(await pubsubPost(`${subscription}:pull`, { maxMessages: 20 }, signal));
    return res.receivedMessages;
  },
  async ack(subscription, ackIds) {
    if (ackIds.length > 0) await pubsubPost(`${subscription}:acknowledge`, { ackIds });
  },
};
