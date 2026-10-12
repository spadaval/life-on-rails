// Hourly score tick: recompute scores from commitments, write score_snapshot
// rows per intent per local date. Pure part: which intents are stale.
export function staleIntents(args: {
  intentIds: string[];
  lastSnapshotAt: Map<string, number>;
  nowMs: number;
  maxAgeMs?: number;
}): string[] {
  const maxAge = args.maxAgeMs ?? 3600_000;
  return args.intentIds.filter((id) => args.nowMs - (args.lastSnapshotAt.get(id) ?? 0) >= maxAge);
}

export const SCORE_SNAPSHOT_SQL = `
INSERT INTO score_snapshot (intent_id, as_of, score, parts)
VALUES ($1, $2, $3, $4)
ON CONFLICT (intent_id, as_of) DO UPDATE SET score = EXCLUDED.score, parts = EXCLUDED.parts`;
