// Commitment writers (spec: recategorizeBlock / splitBlock / confirmDay).
// Source of truth for scoring is `commitment`; time_block.(committed,
// commitment_id) are updated in the same transaction.
import type postgres from "postgres";
import { uuidv7 } from "../../shared/ids.js";

type Tx = postgres.TransactionSql | postgres.Sql;

export function blockHours(tStart: Date, tEnd: Date): number {
  return Math.max(0, (tEnd.getTime() - tStart.getTime()) / 3600_000);
}

async function commitBlock(
  tx: Tx,
  blockId: string,
  intentId: string,
  proposalId: string | null,
): Promise<void> {
  const rows = (await tx`SELECT id, t_start, t_end FROM time_block WHERE id = ${blockId}`) as {
    id: string; t_start: Date; t_end: Date | null;
  }[];
  const block = rows[0];
  if (!block) throw new Error(`time_block ${blockId} not found`);
  // Close-then-commit (same as recategorize): confirmDay accept shares this path.
  if (!block.t_end) {
    const now = new Date();
    await tx`UPDATE time_block SET t_end = ${now}, updated_at = ${now} WHERE id = ${blockId}`;
    block.t_end = now;
  }
  const tEnd = block.t_end;
  // Reject other pending time_attribution proposals for this block.
  if (proposalId) {
    await tx`UPDATE proposal SET status = 'rejected', resolved_at = now()
      WHERE kind = 'time_attribution' AND status = 'pending'
        AND time_block_id = ${blockId} AND id <> ${proposalId}`;
  } else {
    await tx`UPDATE proposal SET status = 'rejected', resolved_at = now()
      WHERE kind = 'time_attribution' AND status = 'pending' AND time_block_id = ${blockId}`;
  }
  // Replace any previous time_spent commitment for this block.
  await tx`DELETE FROM commitment WHERE kind = 'time_spent' AND time_block_id = ${blockId}`;
  const cid = uuidv7();
  await tx`INSERT INTO commitment (id, kind, intent_id, t, t_end, value, proposal_id, time_block_id)
    VALUES (${cid}, 'time_spent', ${intentId}, ${block.t_start}, ${tEnd},
      ${blockHours(block.t_start, tEnd)}, ${proposalId}, ${blockId})`;
  await tx`UPDATE time_block SET intent_id = ${intentId}, committed = true,
    commitment_id = ${cid}, updated_at = now() WHERE id = ${blockId}`;
  if (proposalId) {
    await tx`UPDATE proposal SET status = 'accepted', resolved_at = now() WHERE id = ${proposalId}`;
  }
}

/** recategorizeBlock: close if open, then (replace-)commit to intentId. */
export async function recategorizeBlock(
  db: postgres.Sql,
  blockId: string,
  intentId: string,
): Promise<void> {
  await db.begin(async (tx) => {
    await commitBlock(tx, blockId, intentId, null);
  });
}

/** splitBlock({blockId, at}): left keeps id, right is a new row. */
export async function splitBlock(db: postgres.Sql, blockId: string, at: Date): Promise<string> {
  let rightId = "";
  await db.begin(async (tx) => {
    const rows = (await tx`SELECT * FROM time_block WHERE id = ${blockId} FOR UPDATE`) as {
      id: string; t_start: Date; t_end: Date | null; paused_at: Date | null;
      origin: string; intent_id: string | null; committed: boolean; commitment_id: string | null;
      calendar_id: string | null; calendar_event_id: string | null; calendar_t_start: Date | null;
      title: string | null; note: string | null; idle_s: number; confidence: number | null;
    }[];
    const b = rows[0];
    if (!b) throw new Error(`time_block ${blockId} not found`);
    const endMs = b.t_end ? b.t_end.getTime() : Date.now();
    if (!(at.getTime() > b.t_start.getTime() && at.getTime() < endMs)) {
      throw new Error("split point must satisfy t_start < at < t_end (or at < now if open)");
    }
    await tx`UPDATE time_block SET t_end = ${at}, updated_at = now() WHERE id = ${blockId}`;
    rightId = uuidv7();
    await tx`INSERT INTO time_block (id, t_start, t_end, paused_at, origin, intent_id,
        calendar_id, calendar_event_id, calendar_t_start, title, note, idle_s, committed, confidence)
      VALUES (${rightId}, ${at}, ${b.t_end}, ${b.paused_at}, ${b.origin as "manual"}, ${b.intent_id},
        ${b.calendar_id}, ${b.calendar_event_id}, ${b.calendar_t_start}, ${b.title}, ${b.note},
        0, ${b.committed}, ${b.confidence})`;
    // Re-point observations by midpoint.
    await tx`UPDATE time_block_observation SET block_id = ${rightId}
      WHERE block_id = ${blockId} AND observation_id IN (
        SELECT id FROM observation WHERE (EXTRACT(EPOCH FROM t_start) + EXTRACT(EPOCH FROM COALESCE(t_end, t_start))) / 2
          >= EXTRACT(EPOCH FROM ${at}::timestamptz))`;
    if (b.committed && b.commitment_id) {
      const c = (await tx`SELECT intent_id, proposal_id FROM commitment WHERE id = ${b.commitment_id}`) as {
        intent_id: string; proposal_id: string | null;
      }[];
      if (c[0]) {
        await tx`UPDATE commitment SET t_end = ${at}, value = ${blockHours(b.t_start, at)} WHERE id = ${b.commitment_id}`;
        const nc = uuidv7();
        await tx`INSERT INTO commitment (id, kind, intent_id, t, t_end, value, proposal_id, time_block_id)
          VALUES (${nc}, 'time_spent', ${c[0].intent_id}, ${at}, ${b.t_end},
            ${b.t_end ? blockHours(at, b.t_end) : 0}, ${c[0].proposal_id}, ${rightId})`;
        await tx`UPDATE time_block SET commitment_id = ${nc} WHERE id = ${rightId}`;
      }
    }
  });
  return rightId;
}

export interface ConfirmDayInput {
  acceptProposalIds: string[];
  rejectProposalIds: string[];
  acceptAllPending?: boolean;
  dayStart: Date;
  dayEnd: Date;
}

/**
 * confirmDay: accept listed (or all pending for the day) time_attribution
 * proposals; reject listed. acceptAll skips proposals on COMMITTED calendar
 * blocks fully covered by an already-committed non-calendar block (expire the
 * proposal; hour union counts the seconds once). Gaps with no block stay gaps.
 */
export async function confirmDay(db: postgres.Sql, input: ConfirmDayInput): Promise<{ accepted: number; rejected: number; skipped: number }> {
  let accepted = 0;
  let rejected = 0;
  let skipped = 0;
  await db.begin(async (tx) => {
    await tx`UPDATE proposal SET status = 'rejected', resolved_at = now()
      WHERE id = ANY(${input.rejectProposalIds}) AND status = 'pending'`;
    rejected += input.rejectProposalIds.length;

    let ids = [...input.acceptProposalIds];
    if (input.acceptAllPending) {
      const pending = (await tx`SELECT p.id FROM proposal p
        JOIN time_block b ON b.id = p.time_block_id
        WHERE p.kind = 'time_attribution' AND p.status = 'pending'
          AND b.t_start >= ${input.dayStart} AND b.t_start < ${input.dayEnd}`) as { id: string }[];
      for (const p of pending) if (!ids.includes(p.id)) ids.push(p.id);
    }
    for (const pid of ids) {
      const rows = (await tx`SELECT p.intent_id, p.time_block_id, b.t_start, b.t_end,
          b.origin, b.committed
        FROM proposal p JOIN time_block b ON b.id = p.time_block_id
        WHERE p.id = ${pid} AND p.status = 'pending'`) as {
        intent_id: string | null; time_block_id: string | null; t_start: Date; t_end: Date | null;
        origin: string; committed: boolean;
      }[];
      const r = rows[0];
      if (!r || !r.intent_id || !r.time_block_id) continue;
      if (r.origin === "calendar" && r.committed && r.t_end) {
        // Covered by an already-committed non-calendar block? No-op: expire.
        const cover = (await tx`SELECT 1 FROM time_block
          WHERE origin <> 'calendar' AND committed = true
            AND t_start <= ${r.t_start} AND t_end >= ${r.t_end} LIMIT 1`) as { "?column?": number }[];
        if (cover.length > 0) {
          await tx`UPDATE proposal SET status = 'expired', resolved_at = now() WHERE id = ${pid}`;
          skipped++;
          continue;
        }
      }
      await commitBlock(tx, r.time_block_id, r.intent_id, pid);
      accepted++;
    }
  });
  return { accepted, rejected, skipped };
}
