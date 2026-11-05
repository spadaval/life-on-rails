// What the agent pinned as the one thing on the wall's Today: one row at
// most, over the automatic choice until it expires or is unpinned.
import type { Tx } from "../../db/client.js";
import { BadInput } from "../errors.js";

export interface WallPin {
  text: string;
  why: string | null;
  until: string | null;
  source_ref: string | null;
  created_at: string;
}

export async function getPin(db: Tx): Promise<WallPin | null> {
  const rows = await db<{ text: string; why: string | null; until: Date | null; source_ref: string | null; created_at: Date }[]>`
    SELECT text, why, until, source_ref, created_at FROM wall_pin`;
  const r = rows[0];
  return r ? { text: r.text, why: r.why, until: r.until?.toISOString() ?? null, source_ref: r.source_ref, created_at: r.created_at.toISOString() } : null;
}

export async function setPin(db: Tx, pin: { text: string; why?: string | null; until?: Date | null; sourceRef?: string | null }, now = new Date()): Promise<WallPin> {
  const text = pin.text.trim();
  if (!text) throw new BadInput("a pin needs text");
  if (pin.until && pin.until.getTime() <= now.getTime()) throw new BadInput("--until is already past");
  await db`
    INSERT INTO wall_pin (id, text, why, until, source_ref, created_at)
    VALUES (true, ${text}, ${pin.why?.trim() || null}, ${pin.until ?? null}, ${pin.sourceRef ?? null}, now())
    ON CONFLICT (id) DO UPDATE SET text = EXCLUDED.text, why = EXCLUDED.why, until = EXCLUDED.until,
      source_ref = EXCLUDED.source_ref, created_at = EXCLUDED.created_at`;
  return (await getPin(db))!;
}

export async function clearPin(db: Tx): Promise<{ cleared: boolean }> {
  const rows = await db`DELETE FROM wall_pin RETURNING id`;
  return { cleared: rows.length > 0 };
}
