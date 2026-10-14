import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { deleteBlock, proposeTime, recategorizeBlock } from "../../src/server/attribution/blocks.js";
import { lockBlocks, syncFill } from "../../src/server/attribution/fill.js";
import { ingest } from "../../src/server/device.js";
import { Refused } from "../../src/server/errors.js";
import { createIntent } from "../../src/server/intents.js";
import { addPlace, addRule, listApps, listRules } from "../../src/server/places.js";
import { getToday } from "../../src/server/today.js";
import { uuidv7 } from "../../src/shared/ids.js";
import { dbDescribe } from "./helpers.js";

const ui = { via: "ui" as const };
const at = (h: number, m = 0) => new Date(Date.UTC(2026, 9, 14, h, m));
const hm = (d: Date) => `${String(d.getUTCHours()).padStart(2, "0")}:${String(d.getUTCMinutes()).padStart(2, "0")}`;

const app = (name: string, pkg: string, from: Date, to: Date) => ({
  id: uuidv7(), t_start: from.toISOString(), t_end: to.toISOString(), device: "pixel", source: "android", app: pkg,
  calendar_id: null, title: name, url: null, payload: { kind: "app" }, intent_hint: null,
});
const fix = (when: Date, lat = 35.8553, lon = -78.8752) => ({
  id: uuidv7(), t_start: when.toISOString(), t_end: null, device: "pixel", source: "android", app: null,
  calendar_id: null, title: null, url: null, payload: { kind: "location", lat, lon, accuracy_m: 12 }, intent_hint: null,
});

async function lane() {
  const rows = await sql<{ id: string; origin: string; t_start: Date; t_end: Date; intent: string | null; title: string | null; pending: number }[]>`
    SELECT b.id, b.origin, b.t_start, b.t_end, i.title AS intent, b.title,
      (SELECT count(*)::int FROM proposal p WHERE p.time_block_id = b.id AND p.status = 'pending') AS pending
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id ORDER BY b.t_start`;
  return rows.map((r) => ({ ...r, line: `${r.origin} ${hm(r.t_start)}-${hm(r.t_end)} ${r.intent ?? "-"}${r.pending ? " ?" : ""}` }));
}

dbDescribe("phone and place fill", () => {
  it("fills by precedence: own blocks, calendar, phone apps, then places; rules label; the agent's label survives a re-sync", async () => {
    const work = await createIntent(sql, { type: "goal", title: "Work" }, ui);
    const lor = await createIntent(sql, { type: "project", title: "Ship LoR" }, ui);
    const rest = await createIntent(sql, { type: "goal", title: "Rest" }, ui);
    await addPlace(sql, { name: "Office", lat: 35.8553, lon: -78.8752, intentId: work.id });
    await addRule(sql, { app: "pkg.t3", intentId: lor.id });
    await sql`INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING`;
    await sql`INSERT INTO google_event ${sql({ calendar_id: "cal", event_id: "standup", title: "Standup", t_start: at(15), t_end: at(15, 30) })}`;
    await sql`INSERT INTO time_block (id, t_start, t_end, origin, title) VALUES (${uuidv7()}, ${at(13, 40)}, ${at(13, 50)}, 'manual', 'call')`;

    const fixes = [0, 20, 40, 60, 80, 100, 120, 140, 160, 180].map((m) => fix(new Date(at(13).getTime() + m * 60_000)));
    await ingest({
      device: "pixel",
      observations: [
        app("T3 Code", "pkg.t3", at(13, 30), at(13, 45)),
        app("YouTube", "pkg.youtube", at(14), at(14, 20)),
        app("Settings", "pkg.settings", at(14, 40), at(14, 41)),
        ...fixes,
      ],
    });
    expect((await lane()).map((b) => b.line)).toEqual([
      "place 12:50-13:30 Work ?",
      "auto_activity 13:30-13:40 Ship LoR ?",
      "manual 13:40-13:50 -",
      "place 13:50-14:00 Work ?",
      "auto_activity 14:00-14:20 -",
      "place 14:20-15:00 Work ?",
      "calendar 15:00-15:30 Meetings ?",
      "place 15:30-16:10 Work ?",
    ]);

    // Unlabelled phone time is shown but not asked about.
    const today = await getToday(sql, "2026-10-14", at(23));
    expect(today.phone).toMatchObject({ screen_s: 36 * 60, pickups: 3, glances: 1 });

    // The agent labels the YouTube block; a re-sync keeps the block and the suggestion.
    const youtube = (await lane()).find((b) => b.title === "YouTube 20m")!;
    await proposeTime(sql, youtube.id, rest.id, { via: "agent", sourceRef: "claude:x" });
    await sql.begin(async (tx) => {
      await lockBlocks(tx);
      await syncFill(tx, at(0), at(24));
    });
    const again = (await lane()).find((b) => b.title === "YouTube 20m")!;
    expect([again.id, again.intent, again.pending]).toEqual([youtube.id, "Rest", 1]);

    await expect(deleteBlock(sql, youtube.id)).rejects.toBeInstanceOf(Refused);

    // Confirming with "remember" makes the app a rule.
    await recategorizeBlock(sql, youtube.id, rest.id, ui, { remember: true });
    expect((await listRules(sql)).map((r) => [r.app ?? r.place, r.intent])).toEqual(
      expect.arrayContaining([["pkg.youtube", "Rest"], ["pkg.t3", "Ship LoR"], ["Office", "Work"]]),
    );
    expect((await listApps(sql, 3650, at(23))).find((a) => a.app === "pkg.youtube")).toMatchObject({ intent: "Rest", sessions: 1 });
  });
});
