import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { handleApi } from "../../src/server/api/v1.js";
import { confirmDay, startManualBlock, stopRunningBlock } from "../../src/server/attribution/blocks.js";
import { lockBlocks, syncCalendarFill } from "../../src/server/attribution/fill.js";
import { createIntent } from "../../src/server/intents.js";
import { getToday, renderToday, trailingConfirmRate } from "../../src/server/today.js";
import { dbDescribe } from "./helpers.js";

const ui = { via: "ui" as const };
// Local hours on Wed 2026-10-14 (PDT = UTC-7).
const at = (hour: number, minute = 0, day = 14) => new Date(Date.UTC(2026, 9, day, hour + 7, minute));

async function event(id: string, startHour: number, endHour: number, extra: Record<string, unknown> = {}, day = 14) {
  await sql`INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING`;
  await sql`INSERT INTO google_event ${sql({ calendar_id: "cal", event_id: id, title: id, t_start: at(startHour, 0, day), t_end: at(endHour, 0, day), ...extra })}`;
}
const fill = (day = 14) => sql.begin(async (tx) => { await lockBlocks(tx); return syncCalendarFill(tx, at(0, 0, day), at(24, 0, day)); });

const api = (method: string, path: string, body?: unknown) =>
  handleApi(new Request(`http://127.0.0.1:3847/api/v1${path}`, {
    method,
    headers: { authorization: "Bearer test-agent-token-000", "content-type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  }));

dbDescribe("the Today view", () => {
  it("shows planned and actual, what is left of the day, free stretches, and the confirm rate so far", async () => {
    await event("standup", 9, 10);
    await event("review", 14, 15);
    await event("focus", 10, 12, { event_type: "focusTime" });
    await event("declined", 16, 17, { self_response: "declined" });
    await fill();
    const noon = at(12);
    let t = await getToday(sql, "2026-10-14", noon);
    expect(t.is_today).toBe(true);
    expect(t.planned.map((e) => [e.title, e.fills])).toEqual([["standup", true], ["focus", false], ["review", true]]);
    expect(t.actual.map((b) => [b.title, b.committed, b.proposal !== null])).toEqual([["standup", false, true], ["review", false, true]]);
    expect(t.to_confirm).toBe(1); // only the meeting that has ended
    expect(t.remaining_s).toBe(10 * 3600);
    expect(t.gaps.map((g) => g.minutes)).toEqual([120, 420]); // 12–14, then 15–22
    expect(t.confirm_rate).toBe(0);

    await confirmDay(sql, { date: "2026-10-14", all: true }, ui, noon);
    t = await getToday(sql, "2026-10-14", noon);
    expect(t.confirm_rate).toBe(1);
    expect(t.to_confirm).toBe(0);
    expect(t.actual[0]).toMatchObject({ committed: true, committed_via: "ui", intent: "Meetings" });
    expect(renderToday(t)).toContain("[calendar] Meetings — standup (confirmed; block ");
  });

  it("a confirmed block whose event later moved carries a mismatch", async () => {
    await event("review", 9, 10);
    await fill();
    await confirmDay(sql, { date: "2026-10-14", all: true }, ui, at(12));
    await sql`UPDATE google_event SET t_start = ${at(15)}, t_end = ${at(16)} WHERE event_id = 'review'`;
    const t = await getToday(sql, "2026-10-14", at(13));
    expect(t.actual.find((b) => b.committed)!.mismatch).toBe("the event moved");
  });

  it("manual time outside meetings counts toward the confirm rate, and the trailing rate covers the last seven days", async () => {
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    await event("m", 9, 10, {}, 13);
    await fill(13);
    await startManualBlock(sql, { intentId: deep.id }, ui, at(10, 0, 13));
    await stopRunningBlock(sql, ui, at(11, 0, 13));
    // Yesterday: one hour of meeting unconfirmed, one hour of timer unconfirmed.
    expect(await trailingConfirmRate(sql, at(12))).toBe(0);
    await confirmDay(sql, { date: "2026-10-13", all: true }, ui, at(12));
    expect(await trailingConfirmRate(sql, at(12))).toBe(1);
    expect(await trailingConfirmRate(sql, at(12, 0, 25))).toBeNull(); // nothing in that week
  });
});

dbDescribe("time over the API", () => {
  it("timer, propose, confirm, and the day as markdown", async () => {
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    expect((await api("POST", "/timer/start", { intent_id: deep.id, title: "spec", thread: "t-1" })).status).toBe(200);
    // Backdate the start so the block is long enough to keep.
    await sql`UPDATE time_block SET t_start = now() - interval '30 minutes' WHERE t_end IS NULL`;
    const stopped = (await (await api("POST", "/timer/stop", { thread: "t-1" })).json()) as { id: string };
    const other = await createIntent(sql, { type: "project", title: "Admin" }, ui);
    const proposed = await api("POST", "/proposals", { block_id: stopped.id, intent_id: other.id, reason: "you were in the bank's site", thread: "t-1" });
    expect(proposed.status).toBe(200);

    const day = (await (await api("GET", "/day")).json()) as { markdown: string; to_confirm: number };
    expect(day.to_confirm).toBe(1);
    expect(day.markdown).toContain("suggested by agent, proposal ");

    const confirmed = (await (await api("POST", "/confirm", { date: "today", all: true, thread: "t-1" })).json()) as { accepted: number };
    expect(confirmed.accepted).toBe(1);
    const [c] = await sql<{ via: string; source_ref: string; intent: string }[]>`
      SELECT c.via, c.source_ref, i.title AS intent FROM commitment c JOIN intent i ON i.id = c.intent_id`;
    expect(c).toEqual({ via: "agent", source_ref: "t-1", intent: "Admin" });

    expect((await api("POST", "/timer/start", { thread: "" })).status).toBe(400);
    expect((await api("POST", `/blocks/${stopped.id}/recategorize`, { intent_id: deep.id, thread: "t-1" })).status).toBe(200);
  });
});
