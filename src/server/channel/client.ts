// The WhatsApp Agent Platform: a private contact only the owner can message,
// driven over a small HTTP API. There is no webhook; the box polls, so nothing
// needs to reach it from outside.
import { env } from "../env.js";

const BASE = "https://api.whatsapp.com/agent/v1";

export interface InboundMessage {
  from: string;
  id: string;
  timestamp: string;
  type: string;
  text?: { body: string };
  reaction?: { message_id: string; emoji: string };
  image?: InboundMedia;
  audio?: InboundMedia & { voice?: boolean };
  video?: InboundMedia;
  document?: InboundMedia & { filename?: string };
  sticker?: InboundMedia;
}

export interface InboundMedia {
  id: string;
  mime_type: string;
  caption?: string;
}

export interface Updates {
  messages: InboundMessage[];
  nextOffset: number;
}

export type Outbound =
  | { type: "text"; text: string }
  | { type: "image" | "audio" | "video" | "document"; mediaId: string; caption?: string; filename?: string };

export interface ChannelPort {
  /** Null when the timeout passed with nothing to deliver. */
  updates(offset: number, timeoutS: number): Promise<Updates | null>;
  /** Returns WhatsApp's id for the sent message. */
  send(to: string, message: Outbound): Promise<string>;
  /** Tells him the message landed; the typing indicator lasts until the reply, or 25 s. */
  markRead(messageId: string): Promise<void>;
  upload(bytes: Uint8Array, mime: string, filename: string): Promise<string>;
  download(mediaId: string): Promise<Uint8Array>;
}

export class ChannelError extends Error {
  constructor(message: string, readonly status: number, readonly code: number | null) {
    super(message);
  }
}

function auth(): Record<string, string> {
  return { Authorization: `Bearer ${env.LOR_CHANNEL_KEY}` };
}

async function fail(res: Response, what: string): Promise<never> {
  const body = (await res.json().catch(() => null)) as { error?: { code?: number; message?: string } } | null;
  throw new ChannelError(`${what}: ${res.status} ${body?.error?.message ?? res.statusText}`, res.status, body?.error?.code ?? null);
}

/** The two answers that mean "not sent, try again later". Anything else unknown is never retried: it could send twice. */
export function retriable(err: unknown): boolean {
  return err instanceof ChannelError && (err.status === 429 || (err.status === 503 && err.code === 131016));
}

async function withBackoff<T>(fn: () => Promise<T>): Promise<T> {
  for (let attempt = 0; ; attempt++) {
    try {
      return await fn();
    } catch (err) {
      if (attempt >= 3 || !retriable(err)) throw err;
      await new Promise((r) => setTimeout(r, 6_000 * (attempt + 1)));
    }
  }
}

export const whatsappChannel: ChannelPort = {
  async updates(offset, timeoutS) {
    const res = await fetch(`${BASE}/updates?offset=${offset}&limit=50&timeout=${timeoutS}`, {
      headers: auth(),
      signal: AbortSignal.timeout((timeoutS + 20) * 1000),
    });
    if (res.status === 204) return null;
    if (!res.ok) await fail(res, "updates");
    const body = (await res.json()) as {
      next_offset: number;
      entry?: { changes?: { value?: { messages?: InboundMessage[] } }[] }[];
    };
    const messages = (body.entry ?? []).flatMap((e) => (e.changes ?? []).flatMap((c) => c.value?.messages ?? []));
    return { messages, nextOffset: body.next_offset };
  },

  send: (to, message) =>
    withBackoff(async () => {
      const payload =
        message.type === "text"
          ? { text: { body: message.text } }
          : { [message.type]: { id: message.mediaId, caption: message.caption, filename: message.filename } };
      const res = await fetch(`${BASE}/messages`, {
        method: "POST",
        headers: { ...auth(), "Content-Type": "application/json" },
        body: JSON.stringify({ messaging_product: "whatsapp", to, type: message.type, ...payload }),
        signal: AbortSignal.timeout(60_000),
      });
      if (!res.ok) await fail(res, "send");
      const body = (await res.json()) as { messages: { id: string }[] };
      return body.messages[0]!.id;
    }),

  async markRead(messageId) {
    const res = await fetch(`${BASE}/statuses`, {
      method: "POST",
      headers: { ...auth(), "Content-Type": "application/json" },
      body: JSON.stringify({ messaging_product: "whatsapp", status: "read", message_id: messageId, typing_indicator: { type: "text" } }),
      signal: AbortSignal.timeout(30_000),
    });
    if (!res.ok) await fail(res, "mark read");
  },

  upload: (bytes, mime, filename) =>
    withBackoff(async () => {
      const form = new FormData();
      form.set("messaging_product", "whatsapp");
      form.set("type", mime);
      form.set("file", new Blob([bytes as BlobPart], { type: mime }), filename);
      const res = await fetch(`${BASE}/media`, { method: "POST", headers: auth(), body: form, signal: AbortSignal.timeout(120_000) });
      if (!res.ok) await fail(res, "upload");
      return ((await res.json()) as { id: string }).id;
    }),

  async download(mediaId) {
    const meta = await fetch(`${BASE}/media/${mediaId}`, { headers: auth(), signal: AbortSignal.timeout(30_000) });
    if (!meta.ok) await fail(meta, "media");
    const { url } = (await meta.json()) as { url: string };
    const res = await fetch(url, { headers: auth(), signal: AbortSignal.timeout(120_000) });
    if (!res.ok) await fail(res, "download");
    return new Uint8Array(await res.arrayBuffer());
  },
};
