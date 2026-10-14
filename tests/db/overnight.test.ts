import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { confirmDay, startManualBlock, stopRunningBlock } from "../../src/server/attribution/blocks.js";
import { createIntent } from "../../src/server/intents.js";
import { overnightClose } from "../../src/server/jobs/overnight.js";
import { getToday } from "../../src/server/today.js";
import { dbDescribe } from "./helpers.js";

const ui = { via: "ui" as const };
// Local hours on Tue 2026-10-13; the close runs at 01:30 on the 14th.
const at = (hour: number, minute = 0, day = 13) => new Date(Date.UTC(2026, 9, day, hour + 7, minute));
const CLOSE_AT = at(1, 30, 14);

async function timer(intentId: string, start: Date, stop: Date, title?: string) {
  await startManualBlock(sql, { intentId, title }, ui, start);
  await stopRunningBlock(sql, ui, stop);
}

dbDescribe("overnight close", () => {
  it("merges stretches of the same work, leaves confirmed blocks alone, and never confirms", async () => {
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    const admin = await createIntent(sql, { type: "project", title: "Admin" }, ui);
    await timer(deep.id, at(9), at(9, 50));
    await timer(deep.id, at(9, 51), at(10, 30)); // one minute later: the same stretch
    await timer(deep.id, at(10, 31), at(11)); // and again
    await timer(admin.id, at(11, 1), at(11, 30)); // a different label
    await timer(deep.id, at(14), at(15)); // hours later
    const stats = await overnightClose(sql, CLOSE_AT);
    expect(stats).toMatchObject({ date: "2026-10-13", merged: 2 });

    const day = await getToday(sql, "2026-10-13", CLOSE_AT);
    expect(day.actual.map((b) => [b.intent, b.origin, b.committed, b.proposal !== null])).toEqual([
      ["Deep work", "merged", false, true],
      ["Admin", "manual", false, true],
      ["Deep work", "manual", false, true],
    ]);
    expect(day.actual[0]!.t_start).toBe(at(9).toISOString());
    expect(day.actual[0]!.t_end).toBe(at(11).toISOString());
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM commitment`;
    expect(n!.n).toBe(0);
    // The whole day confirms in one pass.
    expect(await confirmDay(sql, { date: "2026-10-13", all: true }, ui, CLOSE_AT)).toEqual({ accepted: 3, rejected: 0 });
  });

  it("gives a label to accept to any block that has none, but not one that was turned down", async () => {
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    await timer(deep.id, at(9), at(10));
    await timer(deep.id, at(13), at(14));
    const props = await sql<{ id: string; t: Date }[]>`
      SELECT p.id, b.t_start AS t FROM proposal p JOIN time_block b ON b.id = p.time_block_id ORDER BY b.t_start`;
    await confirmDay(sql, { date: "2026-10-13", reject: [props[0]!.id] }, ui, at(20));
    await sql`DELETE FROM proposal WHERE id = ${props[1]!.id}`; // a block left with nothing to accept
    const stats = await overnightClose(sql, CLOSE_AT);
    expect(stats.proposals).toBe(1);
    const rows = await sql<{ status: string; via: string }[]>`
      SELECT p.status, p.via FROM proposal p JOIN time_block b ON b.id = p.time_block_id ORDER BY b.t_start`;
    expect(rows).toEqual([{ status: "rejected", via: "ui" }, { status: "pending", via: "job" }]);
    // Running it again adds nothing.
    expect((await overnightClose(sql, CLOSE_AT)).proposals).toBe(0);
  });

  it("writes score snapshots", async () => {
    await createIntent(sql, { type: "goal", title: "Health", weeklyHourTarget: 3 }, ui);
    expect((await overnightClose(sql, CLOSE_AT)).scored).toBe(1);
    const [s] = await sql<{ as_of: string }[]>`SELECT as_of::text AS as_of FROM score_snapshot`;
    expect(s!.as_of).toBe("2026-10-14");
  });
});
