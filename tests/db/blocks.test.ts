import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { systemIntentId } from "../../src/db/seed.js";
import {
  applyAutoRules, confirmDay, deleteBlock, editBlock, pauseRunningBlock, proposeTime, recategorizeBlock,
  resumeRunningBlock, splitBlock, startManualBlock, stopRunningBlock, uncommitBlock,
} from "../../src/server/attribution/blocks.js";
import { lockBlocks, syncCalendarFill } from "../../src/server/attribution/fill.js";
import { BadInput, Refused } from "../../src/server/errors.js";
import { createIntent } from "../../src/server/intents.js";
import { uuidv7 } from "../../src/shared/ids.js";
import { dbDescribe } from "./helpers.js";

const ui = { via: "ui" as const };
// Wed 2026-10-14, local (PDT = UTC-7). Hours below are local.
const at = (hour: number, minute = 0) => new Date(Date.UTC(2026, 9, 14, Math.floor(hour) + 7, minute + Math.round((hour % 1) * 60)));
const WINDOW = { from: at(0), to: at(24) };

async function event(id: string, startHour: number, endHour: number, extra: Record<string, unknown> = {}) {
  await sql`INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING`;
  const row = { calendar_id: "cal", event_id: id, title: id, t_start: at(startHour), t_end: at(endHour), ...extra };
  await sql`INSERT INTO google_event ${sql(row)}
    ON CONFLICT (calendar_id, event_id) DO UPDATE SET t_start = EXCLUDED.t_start, t_end = EXCLUDED.t_end, status = EXCLUDED.status`;
}

const fill = () => sql.begin(async (tx) => { await lockBlocks(tx); return syncCalendarFill(tx, WINDOW.from, WINDOW.to); });

const hm = (d: Date | null) => (d ? `${String((d.getUTCHours() + 17) % 24).padStart(2, "0")}:${String(d.getUTCMinutes()).padStart(2, "0")}` : "open");

async function blocks() {
  const rows = await sql<{ id: string; origin: string; t_start: Date; t_end: Date | null; committed: boolean; intent: string | null; event: string | null; pending: number }[]>`
    SELECT b.id, b.origin, b.t_start, b.t_end, b.committed, i.title AS intent, b.calendar_event_id AS event,
           (SELECT count(*)::int FROM proposal p WHERE p.time_block_id = b.id AND p.status = 'pending') AS pending
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id ORDER BY b.t_start`;
  return rows.map((r) => ({ ...r, span: `${hm(r.t_start)}-${hm(r.t_end)}` }));
}
const spans = async () => (await blocks()).map((b) => `${b.origin}:${b.event ?? b.intent} ${b.span}${b.committed ? " ✓" : ""}`);

dbDescribe("calendar fill in the database", () => {
  it("creates one uncommitted block and one pending proposal per event, defaulting to Meetings", async () => {
    await event("standup", 9, 9.5);
    expect(await fill()).toMatchObject({ inserted: 1, kept: 0, deleted: 0 });
    const [b] = await blocks();
    expect(b).toMatchObject({ origin: "calendar", intent: "Meetings", committed: false, pending: 1, span: "09:00-09:30" });
  });

  it("a second sync with nothing changed touches nothing: same ids, a rejected label stays rejected", async () => {
    await event("standup", 9, 9.5);
    await event("dentist", 14, 15);
    await fill();
    const before = await blocks();
    const [p] = await sql<{ id: string }[]>`SELECT p.id FROM proposal p JOIN time_block b ON b.id = p.time_block_id WHERE b.calendar_event_id = 'dentist'`;
    await confirmDay(sql, { date: "2026-10-14", reject: [p!.id] }, ui, at(20));
    expect(await fill()).toMatchObject({ inserted: 0, kept: 2, deleted: 0 });
    const after = await blocks();
    expect(after.map((b) => b.id)).toEqual(before.map((b) => b.id));
    expect(after.find((b) => b.event === "dentist")!.pending).toBe(0);
    const [status] = await sql<{ status: string }[]>`SELECT status FROM proposal WHERE id = ${p!.id}`;
    expect(status!.status).toBe("rejected");
  });

  it("skips declined, free, focus-time, all-day, and cancelled events", async () => {
    await event("declined", 9, 10, { self_response: "declined" });
    await event("free", 10, 11, { transparency: "transparent" });
    await event("focus", 11, 12, { event_type: "focusTime" });
    await event("allday", 0, 24, { all_day: true });
    await event("cancelled", 13, 14, { status: "cancelled" });
    await event("real", 15, 16);
    await fill();
    expect(await spans()).toEqual(["calendar:real 15:00-16:00"]);
  });

  it("two events at once: the shorter one gets the overlap", async () => {
    await event("hold", 13, 17);
    await event("dentist", 14, 15);
    await fill();
    expect(await spans()).toEqual(["calendar:hold 13:00-14:00", "calendar:dentist 14:00-15:00", "calendar:hold 15:00-17:00"]);
  });

  it("a moved event moves its uncommitted block; a cancelled one loses it", async () => {
    await event("dentist", 14, 15);
    await event("standup", 9, 9.5);
    await fill();
    await event("dentist", 16, 17);
    await sql`UPDATE google_event SET status = 'cancelled' WHERE event_id = 'standup'`;
    expect(await fill()).toMatchObject({ inserted: 1, deleted: 2 });
    expect(await spans()).toEqual(["calendar:dentist 16:00-17:00"]);
  });

  it("a committed calendar block is never rewritten and is not filled over again", async () => {
    await event("dentist", 14, 15);
    await fill();
    await confirmDay(sql, { date: "2026-10-14", all: true }, ui, at(20));
    expect(await spans()).toEqual(["calendar:dentist 14:00-15:00 ✓"]);
    expect(await fill()).toMatchObject({ inserted: 0, deleted: 0 });
    // The event moves afterwards: the confirmed block stays where it was, and the new time is filled.
    await event("dentist", 16, 17);
    await fill();
    expect(await spans()).toEqual(["calendar:dentist 14:00-15:00 ✓", "calendar:dentist 16:00-17:00"]);
  });

  it("labels events from a matching rule, and auto-apply rules commit once the event is over", async () => {
    const gym = await createIntent(sql, { type: "habit", title: "Gym", frequency: 3, frequencyHorizon: "week" }, ui);
    await sql`INSERT INTO attribution_rule (id, match_title, intent_id, auto_apply) VALUES (${uuidv7()}, '^gym', ${gym.id}, true)`;
    await event("Gym session", 7, 8);
    await event("Gym tomorrow-ish", 22, 23);
    await fill();
    expect((await blocks()).map((b) => b.intent)).toEqual(["Gym", "Gym"]);
    const n = await sql.begin((tx) => applyAutoRules(tx, WINDOW.from, WINDOW.to, at(12)));
    expect(n).toBe(1); // the 22:00 one has not happened yet
    expect(await spans()).toEqual(["calendar:Gym session 07:00-08:00 ✓", "calendar:Gym tomorrow-ish 22:00-23:00"]);
    const [c] = await sql<{ via: string }[]>`SELECT via FROM commitment`;
    expect(c!.via).toBe("rule");
  });
});

dbDescribe("the timer", () => {
  it("two rapid taps leave one open block", async () => {
    const results = await Promise.allSettled([
      startManualBlock(sql, { title: "a" }, ui, at(10)),
      startManualBlock(sql, { title: "b" }, ui, at(10, 0)),
      startManualBlock(sql, { title: "c" }, ui, at(10, 0)),
    ]);
    expect(results.every((r) => r.status === "fulfilled")).toBe(true);
    const [n] = await sql<{ open: number; total: number }[]>`
      SELECT count(*) FILTER (WHERE t_end IS NULL)::int AS open, count(*)::int AS total FROM time_block`;
    expect(n).toEqual({ open: 1, total: 1 }); // the superseded ones were under a minute and dropped
  });

  it("start inside a meeting removes the fill ahead of it; stop brings the rest of the meeting back", async () => {
    await event("review", 9, 11);
    await event("later", 15, 16);
    await fill();
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    await startManualBlock(sql, { intentId: deep.id }, ui, at(10));
    expect(await spans()).toEqual(["calendar:review 09:00-10:00", "manual:Deep work 10:00-open"]);
    await stopRunningBlock(sql, ui, at(10, 30));
    expect(await spans()).toEqual([
      "calendar:review 09:00-10:00", "manual:Deep work 10:00-10:30", "calendar:review 10:30-11:00", "calendar:later 15:00-16:00",
    ]);
    const stopped = (await blocks()).find((b) => b.origin === "manual")!;
    expect(stopped).toMatchObject({ committed: false, pending: 1 });
  });

  it("starting a new timer closes the running one; a mis-tap under a minute is dropped", async () => {
    await startManualBlock(sql, { title: "first" }, ui, at(9));
    await startManualBlock(sql, { title: "second" }, ui, at(9, 40));
    await stopRunningBlock(sql, ui, at(9, 40)); // stopped within the same minute
    expect(await spans()).toEqual(["manual:Uncategorized 09:00-09:40"]);
    expect((await stopRunningBlock(sql, ui, at(10))).id).toBeNull();
  });

  it("a pause is not time spent: resume starts a new block with the same label", async () => {
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    await startManualBlock(sql, { intentId: deep.id, title: "spec" }, ui, at(9));
    await pauseRunningBlock(sql, at(9, 30));
    const [paused] = await sql<{ open: boolean; paused: boolean }[]>`SELECT t_end IS NULL AS open, paused_at IS NOT NULL AS paused FROM time_block`;
    expect(paused).toEqual({ open: true, paused: true });
    await resumeRunningBlock(sql, ui, at(10));
    await pauseRunningBlock(sql, at(10, 20));
    await stopRunningBlock(sql, ui, at(11));
    expect(await spans()).toEqual(["manual:Deep work 09:00-09:30", "manual:Deep work 10:00-10:20"]);
  });
});

dbDescribe("labelling and fixing blocks", () => {
  async function stoppedBlock(startHour: number, endHour: number, intentId?: string) {
    const { id } = await startManualBlock(sql, { intentId }, ui, at(startHour));
    await stopRunningBlock(sql, ui, at(endHour));
    return id;
  }

  it("recategorize commits the block and replaces an earlier commitment", async () => {
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const b = await createIntent(sql, { type: "project", title: "B" }, ui);
    const id = await stoppedBlock(9, 10);
    await recategorizeBlock(sql, id, a.id, ui, {}, at(12));
    await recategorizeBlock(sql, id, b.id, ui, {}, at(12));
    const rows = await sql<{ intent: string; value: number; committed: boolean; linked: boolean }[]>`
      SELECT i.title AS intent, c.value, b.committed, b.commitment_id = c.id AS linked
      FROM commitment c JOIN intent i ON i.id = c.intent_id JOIN time_block b ON b.id = c.time_block_id`;
    expect(rows).toEqual([{ intent: "B", value: 1, committed: true, linked: true }]);
    const [pending] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM proposal WHERE status = 'pending'`;
    expect(pending!.n).toBe(0);
  });

  it("recategorizing the running block closes it first", async () => {
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const { id } = await startManualBlock(sql, {}, ui, at(9));
    await recategorizeBlock(sql, id, a.id, ui, {}, at(10));
    expect(await spans()).toEqual(["manual:A 09:00-10:00 ✓"]);
  });

  it("remembering a calendar title makes the next event with that title arrive labelled", async () => {
    const one = await createIntent(sql, { type: "project", title: "1:1s" }, ui);
    await event("Priya 1:1 (weekly)", 9, 10);
    await fill();
    const [b] = await blocks();
    await recategorizeBlock(sql, b!.id, one.id, ui, { rememberTitle: true }, at(12));
    await sql`UPDATE google_event SET event_id = 'next-week', title = 'Priya 1:1 (weekly)', t_start = ${at(13)}, t_end = ${at(14)} WHERE event_id = 'Priya 1:1 (weekly)'`;
    await fill();
    const next = (await blocks()).find((x) => x.event === "next-week")!;
    expect(next).toMatchObject({ intent: "1:1s", committed: false, pending: 1 });
  });

  it("the agent can suggest a label but not change a confirmed block; undoing a confirmation restores the suggestion", async () => {
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const b = await createIntent(sql, { type: "project", title: "B" }, ui);
    const id = await stoppedBlock(9, 10);
    await proposeTime(sql, id, a.id, { via: "agent", sourceRef: "t-1" }, "calendar says so");
    const [p] = await sql<{ via: string; source_ref: string; reason: string; n: number }[]>`
      SELECT via, source_ref, reason, (SELECT count(*)::int FROM proposal WHERE status = 'pending') AS n FROM proposal WHERE status = 'pending'`;
    expect(p).toEqual({ via: "agent", source_ref: "t-1", reason: "calendar says so", n: 1 });
    await confirmDay(sql, { date: "2026-10-14", all: true }, ui, at(20));
    expect(await spans()).toEqual(["manual:A 09:00-10:00 ✓"]);
    await expect(proposeTime(sql, id, b.id, { via: "agent" })).rejects.toBeInstanceOf(Refused);
    await uncommitBlock(sql, id, at(21));
    expect(await blocks()).toMatchObject([{ committed: false, pending: 1, intent: "A" }]);
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM commitment`;
    expect(n!.n).toBe(0);
  });

  it("edit moves a block and its commitment, refuses overlap and the future, and lets fill back in", async () => {
    await event("review", 9, 11);
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const id = await stoppedBlock(9, 11, a.id); // a timer left running over the whole meeting
    await recategorizeBlock(sql, id, a.id, ui, {}, at(12));
    expect(await spans()).toEqual(["manual:A 09:00-11:00 ✓"]);
    await editBlock(sql, id, { tEnd: at(10) }, at(12));
    expect(await spans()).toEqual(["manual:A 09:00-10:00 ✓", "calendar:review 10:00-11:00"]);
    const [c] = await sql<{ value: number; t_end: Date }[]>`SELECT value, t_end FROM commitment`;
    expect(c).toMatchObject({ value: 1 });
    expect(c!.t_end.toISOString()).toBe(at(10).toISOString());

    const other = await stoppedBlock(11, 12);
    await expect(editBlock(sql, other, { tStart: at(9, 30) }, at(13))).rejects.toBeInstanceOf(Refused);
    await expect(editBlock(sql, other, { tEnd: at(23) }, at(13))).rejects.toBeInstanceOf(BadInput);
  });

  it("delete removes a block and its commitment, and the calendar fills back in", async () => {
    await event("review", 9, 11);
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const id = await stoppedBlock(9, 11, a.id);
    await recategorizeBlock(sql, id, a.id, ui, {}, at(12));
    await deleteBlock(sql, id, at(12));
    expect(await spans()).toEqual(["calendar:review 09:00-11:00"]);
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM commitment`;
    expect(n!.n).toBe(0);
  });

  it("split divides a block and its commitment", async () => {
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const id = await stoppedBlock(9, 11, a.id);
    await recategorizeBlock(sql, id, a.id, ui, {}, at(12));
    const { rightId } = await splitBlock(sql, id, at(10, 30), at(12));
    expect(await spans()).toEqual(["manual:A 09:00-10:30 ✓", "manual:A 10:30-11:00 ✓"]);
    const values = await sql<{ value: number }[]>`SELECT value FROM commitment ORDER BY t`;
    expect(values.map((v) => v.value)).toEqual([1.5, 0.5]);
    expect(rightId).not.toBe(id);
    await expect(splitBlock(sql, id, at(12), at(13))).rejects.toBeInstanceOf(BadInput);
  });

  it("confirm --all accepts what has ended and leaves the rest of the day pending", async () => {
    await event("morning", 9, 10);
    await event("evening", 20, 21);
    await fill();
    const result = await confirmDay(sql, { date: "2026-10-14", all: true }, ui, at(12));
    expect(result).toEqual({ accepted: 1, rejected: 0 });
    expect(await spans()).toEqual(["calendar:morning 09:00-10:00 ✓", "calendar:evening 20:00-21:00"]);
    const meetings = await systemIntentId(sql, "Meetings");
    const [c] = await sql<{ intent_id: string; via: string }[]>`SELECT intent_id, via FROM commitment`;
    expect(c).toEqual({ intent_id: meetings, via: "ui" });
  });
});
