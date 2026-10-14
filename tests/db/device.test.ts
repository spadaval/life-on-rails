import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { deviceState, deviceTimer, ingest } from "../../src/server/device.js";
import { findEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { lastContacts } from "../../src/server/people.js";
import { dbDescribe } from "./helpers.js";

const obs = (id: string, extra: Record<string, unknown> = {}) => ({
  id,
  t_start: "2026-10-14T16:00:00.000Z",
  t_end: "2026-10-14T16:05:00.000Z",
  device: "pixel-9a",
  source: "android",
  app: "com.google.android.apps.maps",
  calendar_id: null,
  title: "Maps",
  url: null,
  payload: { kind: "app", package: "com.google.android.apps.maps" },
  intent_hint: null,
  ...extra,
});

dbDescribe("device ingest", () => {
  it("stores a batch, is idempotent, extends an observation, and rejects what a device may not send", async () => {
    const a = "6f1c2c1e-0000-4000-8000-000000000001";
    const b = "6f1c2c1e-0000-4000-8000-000000000002";
    const first = await ingest({ device: "pixel-9a", observations: [obs(a), obs(b, { t_end: null })] });
    expect(first).toEqual({ received: 2, stored: 2, rejected: [] });
    const again = await ingest({ device: "pixel-9a", observations: [obs(a), obs(b, { t_end: "2026-10-14T16:20:00.000Z" })] });
    expect(again).toEqual({ received: 2, stored: 0, rejected: [] });
    const [row] = await sql<{ t_end: Date }[]>`SELECT t_end FROM observation WHERE id = ${b}`;
    expect(row!.t_end.toISOString()).toBe("2026-10-14T16:20:00.000Z");

    const bad = await ingest({
      device: "pixel-9a",
      observations: [
        obs("6f1c2c1e-0000-4000-8000-000000000003", { source: "gmail" }),
        obs("6f1c2c1e-0000-4000-8000-000000000004", { device: "someone-else" }),
        { id: "not-a-uuid" },
      ],
    });
    expect(bad.stored).toBe(0);
    expect(bad.rejected.map((r) => r.error)).toEqual([
      "a device cannot send source gmail",
      "device someone-else in a batch from pixel-9a",
      expect.stringContaining("id"),
    ]);
    await expect(ingest({ observations: [] })).rejects.toThrow(/device/);
  });

  it("normalises numbers on calls and texts, so they count toward last in touch", async () => {
    const p = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "phone", value: "+1 415 555 0100" }] });
    await ingest({
      device: "pixel-9a",
      observations: [obs("6f1c2c1e-0000-4000-8000-000000000010", { app: "phone", title: "Priya", payload: { kind: "call", direction: "out", phone: "(415) 555-0100", duration_s: 120 } })],
    });
    const [o] = await sql<{ phone: string }[]>`SELECT payload->>'phone' AS phone FROM observation`;
    expect(o!.phone).toBe("+14155550100");
    expect((await lastContacts(sql, [p.id])).get(p.id)).toMatchObject({ via: "phone" });
    expect((await findEntity(sql, "415 555 0100"))?.id).toBe(p.id);
  });
});

dbDescribe("device timer and state", () => {
  it("toggles the timer and reports what is running", async () => {
    const empty = await deviceState("sumeet@example.com", "pixel-9a");
    expect(empty).toMatchObject({ login: "sumeet@example.com", running: null, last_upload: null });
    const started = await deviceTimer({ action: "toggle", title: "from the tile", device: "pixel-9a" }, "me");
    expect(started.running).toMatchObject({ intent: "Uncategorized", title: "from the tile", paused: false });
    const paused = await deviceTimer({ action: "pause" }, "me");
    expect(paused.running?.paused).toBe(true);
    await sql`UPDATE time_block SET t_start = now() - interval '10 minutes', paused_at = now() - interval '2 minutes'`;
    const stopped = await deviceTimer({ action: "toggle" }, "me");
    expect(stopped.running).toBeNull();
    const [b] = await sql<{ minutes: number }[]>`SELECT round(extract(epoch FROM t_end - t_start) / 60)::int AS minutes FROM time_block`;
    expect(b!.minutes).toBe(8); // ended at the pause
  });
});
