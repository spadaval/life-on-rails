// `lor log`: something the agent did or noticed, kept as an observation.
import type { Tx } from "../../db/client.js";
import { recordObservation } from "../observations.js";

export async function agentLog(
  db: Tx,
  input: { text: string; intentId?: string | null; thread: string },
): Promise<{ id: string }> {
  const now = new Date();
  const { id } = await recordObservation(db, {
    tStart: now,
    tEnd: now,
    device: "buildbot",
    source: "agent",
    title: input.text,
    payload: { thread: input.thread },
    intentHint: input.intentId ?? null,
  });
  return { id };
}
