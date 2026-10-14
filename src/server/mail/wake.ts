// The wake: non-bulk arrivals wait in a queue, coalesced by Gmail thread, until
// the burst has gone quiet. LoR calls nobody: the unattended mail run asks for
// what is due (`lor mail pending`) and, once it has handled it, says so
// (`lor mail done`). Rows leave the queue only then.
import type { Sql, Tx } from "../../db/client.js";

export interface WakeConfig {
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
  const rows = await db<QueueRow[]>`SELECT * FROM wake_queue`;
  return isDue(rows, now, cfg);
}

export function wakeText(messages: WakeMessage[]): string {
  const lines = messages.map((m) => `- ${m.from ?? "unknown sender"} — ${m.subject ?? "(no subject)"} (gmail id ${m.gmail_id}, thread ${m.thread_id})`);
  return `${messages.length} new message${messages.length === 1 ? "" : "s"} in the inbox:\n${lines.join("\n")}`;
}

export interface PendingMail {
  /** Quiet for long enough (or waiting too long, or a priority sender). Nothing is listed until then. */
  due: boolean;
  messages: WakeMessage[];
  /** What to pass back to `lor mail done` once these are handled. */
  observation_ids: string[];
  text: string;
}

export async function pendingMail(db: Sql, cfg: WakeConfig, now = new Date()): Promise<PendingMail> {
  const rows = await db<QueueRow[]>`SELECT * FROM wake_queue ORDER BY first_seen`;
  if (!isDue(rows, now, cfg)) return { due: false, messages: [], observation_ids: [], text: "" };
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
  return { due: true, messages, observation_ids: obs.map((o) => o.id), text: wakeText(messages) };
}

/** Take handled arrivals off the queue. A thread that got another message meanwhile stays, with only the new one. */
export async function markMailHandled(db: Sql, observationIds: string[]): Promise<{ removed: number }> {
  return db.begin(async (tx) => {
    const before = (await tx<{ n: number }[]>`SELECT coalesce(sum(cardinality(observation_ids)), 0)::int AS n FROM wake_queue`)[0]!.n;
    await tx`
      UPDATE wake_queue SET observation_ids = ARRAY(SELECT unnest(observation_ids) EXCEPT SELECT unnest(${observationIds}::uuid[]))
      WHERE observation_ids && ${observationIds}::uuid[]`;
    await tx`DELETE FROM wake_queue WHERE cardinality(observation_ids) = 0`;
    const after = (await tx<{ n: number }[]>`SELECT coalesce(sum(cardinality(observation_ids)), 0)::int AS n FROM wake_queue`)[0]!.n;
    return { removed: before - after };
  });
}
