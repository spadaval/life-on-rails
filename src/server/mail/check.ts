// Mail lives in Gmail. LoR's part is to notice that something arrived: one
// observation per message (sender, subject, labels; never a body), and a place
// in the wake queue for anything that is not bulk.
import type { Sql, Tx } from "../../db/client.js";
import { HistoryExpiredError, type MailPort } from "../google/mailClient.js";
import { entityForEmail } from "../memory/entities.js";
import { recordObservation, stableId } from "../observations.js";
import { env } from "../env.js";
import { addressesIn, headersOf, isBulk, parseFrom } from "./bulk.js";

export interface MailCheckStats {
  seen: number;
  recorded: number;
  /** Mail the owner sent: recorded for "last in touch", never a wake. */
  sent: number;
  bulk: number;
  queued: number;
  skipped: number;
  note?: string;
  [key: string]: unknown;
}

async function state(tx: Tx): Promise<{ history_id: string | null }> {
  await tx`INSERT INTO mail_state (id) VALUES (true) ON CONFLICT DO NOTHING`;
  return (await tx<{ history_id: string | null }[]>`SELECT history_id FROM mail_state`)[0]!;
}

/** How many recent messages to look through when Gmail has dropped our place in its history. */
const CATCH_UP = 50;

export async function mailCheck(db: Sql, port: MailPort, now = new Date()): Promise<MailCheckStats> {
  const stats: MailCheckStats = { seen: 0, recorded: 0, sent: 0, bulk: 0, queued: 0, skipped: 0 };
  const { history_id } = await state(db);

  let ids: string[];
  let next: string;
  if (!history_id) {
    // First run: start from here. Older mail is in Gmail; nothing is backfilled.
    next = await port.currentHistoryId();
    ids = [];
    stats.note = "started watching from now";
  } else {
    try {
      const added = await port.addedSince(history_id);
      ids = added.ids;
      next = added.historyId;
    } catch (err) {
      if (!(err instanceof HistoryExpiredError)) throw err;
      // Away for over a week: take recent messages we have no record of.
      next = await port.currentHistoryId();
      ids = await port.recentMessages(CATCH_UP);
      stats.note = "history expired; caught up from recent messages";
    }
  }

  for (const id of ids) {
    stats.seen++;
    const obsId = stableId("gmail", id);
    const known = await db`SELECT 1 FROM observation WHERE id = ${obsId}`;
    if (known.length > 0) continue;
    const message = await port.metadata(id);
    // What arrived in the inbox, and what the owner sent. Not drafts, not mail filtered past the inbox.
    if (!message || message.labelIds.includes("DRAFT") || !(message.labelIds.includes("INBOX") || message.labelIds.includes("SENT"))) {
      stats.skipped++;
      continue;
    }
    const headers = headersOf(message.payload.headers);
    const when = message.internalDate ? new Date(Number(message.internalDate)) : now;

    if (message.labelIds.includes("SENT")) {
      const self = env.GOOGLE_ACCOUNT.toLowerCase();
      const { inserted } = await recordObservation(db, {
        id: obsId,
        tStart: when,
        tEnd: when,
        device: "buildbot",
        source: "gmail",
        app: "SENT",
        title: headers.subject,
        payload: {
          gmail_id: message.id,
          thread_id: message.threadId,
          direction: "sent",
          to: headers.to,
          to_emails: addressesIn(headers.to, headers.cc).filter((a) => a !== self),
          labels: message.labelIds,
        },
      });
      if (inserted) {
        stats.recorded++;
        stats.sent++;
      }
      continue;
    }

    const sender = parseFrom(headers.from);
    const entity = sender.email ? await entityForEmail(db, sender.email) : null;
    const bulk = isBulk(headers, message.labelIds, entity !== null);

    await db.begin(async (tx) => {
      const { inserted } = await recordObservation(tx, {
        id: obsId,
        tStart: when,
        tEnd: when,
        device: "buildbot",
        source: "gmail",
        app: message.labelIds.find((l) => l.startsWith("CATEGORY_")) ?? "INBOX",
        title: headers.subject,
        payload: {
          gmail_id: message.id,
          thread_id: message.threadId,
          direction: "in",
          from: headers.from,
          from_email: sender.email,
          entity_id: entity?.id ?? null,
          labels: message.labelIds,
          bulk,
        },
      });
      if (!inserted) return;
      stats.recorded++;
      if (bulk) {
        stats.bulk++;
        return;
      }
      const priority = entity ? ((await tx<{ priority: boolean }[]>`SELECT priority FROM entity WHERE id = ${entity.id}`)[0]?.priority ?? false) : false;
      await tx`
        INSERT INTO wake_queue (gmail_thread_id, observation_ids, first_seen, last_seen, priority)
        VALUES (${message.threadId}, ${[obsId]}::uuid[], ${now}, ${now}, ${priority})
        ON CONFLICT (gmail_thread_id) DO UPDATE SET
          observation_ids = wake_queue.observation_ids || EXCLUDED.observation_ids,
          last_seen = EXCLUDED.last_seen, priority = wake_queue.priority OR EXCLUDED.priority`;
      stats.queued++;
    });
  }

  await db`UPDATE mail_state SET history_id = ${next}, last_check_at = ${now}`;
  return stats;
}

/** Re-register the watch (it lapses after seven days), then catch up. */
export async function renewWatch(db: Sql, port: MailPort, topic: string, now = new Date()): Promise<{ expires: string } & MailCheckStats> {
  await state(db);
  const watch = await port.watch(topic);
  await db`UPDATE mail_state SET watch_expires_at = ${watch.expiration}`;
  const stats = await mailCheck(db, port, now);
  return { ...stats, expires: watch.expiration.toISOString() };
}

export async function notePush(db: Tx, now = new Date()): Promise<void> {
  await db`INSERT INTO mail_state (id, last_push_at) VALUES (true, ${now}) ON CONFLICT (id) DO UPDATE SET last_push_at = EXCLUDED.last_push_at`;
}

export interface MailStatus {
  configured: boolean;
  watch_expires_at: string | null;
  last_push_at: string | null;
  last_check_at: string | null;
  queued_threads: number;
}

export async function mailStatus(db: Tx, configured: boolean): Promise<MailStatus> {
  const row = (await db<{ watch_expires_at: Date | null; last_push_at: Date | null; last_check_at: Date | null }[]>`
    SELECT watch_expires_at, last_push_at, last_check_at FROM mail_state`)[0];
  const queued = (await db<{ n: number }[]>`SELECT count(*)::int AS n FROM wake_queue`)[0]?.n ?? 0;
  return {
    configured,
    watch_expires_at: row?.watch_expires_at?.toISOString() ?? null,
    last_push_at: row?.last_push_at?.toISOString() ?? null,
    last_check_at: row?.last_check_at?.toISOString() ?? null,
    queued_threads: queued,
  };
}
