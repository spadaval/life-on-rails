// The wake: non-bulk arrivals wait in a queue, coalesced by Gmail thread, and
// are handed to the agent in one POST once the burst has gone quiet. Rows
// leave the queue only when the POST succeeds.
import type { Sql, Tx } from "../../db/client.js";

export interface WakeConfig {
  url: string;
  quietS: number;
  maxS: number;
}

export interface WakeMessage {
  gmail_id: string;
  thread_id: string;
  from: string | null;
  subject: string | null;
  date: string;
  known_sender: boolean;
}

interface QueueRow {
  gmail_thread_id: string;
  observation_ids: string[];
  first_seen: Date;
  last_seen: Date;
  priority: boolean;
}

/** Quiet for long enough, or waiting too long, or someone who skips the wait. */
export function isDue(rows: Pick<QueueRow, "first_seen" | "last_seen" | "priority">[], now: Date, cfg: Pick<WakeConfig, "quietS" | "maxS">): boolean {
  if (rows.length === 0) return false;
  if (rows.some((r) => r.priority)) return true;
  const lastSeen = Math.max(...rows.map((r) => r.last_seen.getTime()));
  const firstSeen = Math.min(...rows.map((r) => r.first_seen.getTime()));
  return now.getTime() - lastSeen >= cfg.quietS * 1000 || now.getTime() - firstSeen >= cfg.maxS * 1000;
}

export async function wakeDue(db: Tx, cfg: WakeConfig, now = new Date()): Promise<boolean> {
  if (!cfg.url) return false;
  const rows = await db<QueueRow[]>`SELECT * FROM wake_queue`;
  return isDue(rows, now, cfg);
}

export type Poster = (url: string, body: unknown) => Promise<{ ok: boolean; status: number }>;

const httpPost: Poster = async (url, body) => {
  const res = await fetch(url, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });
  return { ok: res.ok, status: res.status };
};

export function wakeText(messages: WakeMessage[]): string {
  const lines = messages.map((m) => `- ${m.from ?? "unknown sender"} — ${m.subject ?? "(no subject)"} (gmail id ${m.gmail_id}, thread ${m.thread_id})`);
  return `${messages.length} new message${messages.length === 1 ? "" : "s"} in the inbox:\n${lines.join("\n")}`;
}

export interface FlushStats {
  threads: number;
  messages: number;
  posted: boolean;
  status?: number;
  [key: string]: unknown;
}

export async function flushWake(db: Sql, cfg: WakeConfig, post: Poster = httpPost): Promise<FlushStats> {
  const rows = await db<QueueRow[]>`SELECT * FROM wake_queue ORDER BY first_seen`;
  if (rows.length === 0 || !cfg.url) return { threads: 0, messages: 0, posted: false };
  const ids = rows.flatMap((r) => r.observation_ids);
  const obs = await db<{ id: string; title: string | null; t_start: Date; payload: { gmail_id: string; thread_id: string; from: string | null; entity_id: string | null } }[]>`
    SELECT id, title, t_start, payload FROM observation WHERE id = ANY(${ids}::uuid[]) ORDER BY t_start`;
  const messages: WakeMessage[] = obs.map((o) => ({
    gmail_id: o.payload.gmail_id,
    thread_id: o.payload.thread_id,
    from: o.payload.from,
    subject: o.title,
    date: o.t_start.toISOString(),
    known_sender: o.payload.entity_id !== null,
  }));
  const result = await post(cfg.url, { text: wakeText(messages), count: messages.length, messages });
  if (!result.ok) {
    // T3 is down, or already holding its limit for a busy thread. Try again next time.
    throw new Error(`wake POST failed with ${result.status}; ${rows.length} thread(s) stay queued`);
  }
  // Remove what was sent. A thread that got another message meanwhile stays, with only the new one.
  for (const r of rows) {
    await db`
      UPDATE wake_queue SET observation_ids = ARRAY(SELECT unnest(observation_ids) EXCEPT SELECT unnest(${r.observation_ids}::uuid[]))
      WHERE gmail_thread_id = ${r.gmail_thread_id}`;
  }
  await db`DELETE FROM wake_queue WHERE cardinality(observation_ids) = 0`;
  return { threads: rows.length, messages: messages.length, posted: true, status: result.status };
}
