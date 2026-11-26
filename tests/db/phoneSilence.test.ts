import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { getStatus, phoneLastReport } from "../../src/server/status.js";
import { dbDescribe } from "./helpers.js";

const arrived = (source: string, at: string) => sql`
  INSERT INTO observation (id, t_start, t_end, device, source, ingested_at)
  VALUES (gen_random_uuid(), ${at}, ${at}, 'phone', ${source}, ${at})`;

dbDescribe("the phone's last report", () => {
  it("is the newest android arrival, the same one /status shows, and null before any", async () => {
    expect(await phoneLastReport()).toBeNull();
    await arrived("android", "2026-11-20T10:00:00Z");
    await arrived("android", "2026-11-20T12:00:00Z");
    await arrived("gmail", "2026-11-21T09:00:00Z");
    expect(await phoneLastReport()).toBe("2026-11-20T12:00:00.000Z");
    const status = await getStatus();
    const phone = status.sources.find((s) => s.source === "android")!;
    expect(phone.lastArrival).toBe("2026-11-20T12:00:00.000Z");
    expect(phone.silent).toBe(true);
    expect(status.sources.find((s) => s.source === "gmail")!.silent).toBe(false);
  });
});
