import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import {
  deleteBlock, editBlock, pauseRunningBlock, proposeTime, recategorizeBlock, resumeRunningBlock, startManualBlock,
  stopRunningBlock, undoBlockChange,
} from "../../src/server/attribution/blocks.js";
import { lockBlocks, syncCalendarFill } from "../../src/server/attribution/fill.js";
import { Refused } from "../../src/server/errors.js";
import { createIntent } from "../../src/server/intents.js";
import { dbDescribe } from "./helpers.js";

const ui = { via: "ui" as const };
const agent = { via: "agent" as const, sourceRef: "thread-1" };
// Wed 2026-10-14, local (PDT = UTC-7). Hours below are local.
const at = (hour: number, minute = 0, second = 0) => new Date(Date.UTC(2026, 9, 14, hour + 7, minute, second));

async function event(id: string, startHour: number, endHour: number) {
  await sql`INSERT INTO google_sync_state (calendar_id, summary) VALUES ('cal', 'cal') ON CONFLICT DO NOTHING`;
  await sql`INSERT INTO google_event ${sql({ calendar_id: "cal", event_id: id, title: id, t_start: at(startHour), t_end: at(endHour) })}`;
  await sql.begin(async (tx) => {
    await lockBlocks(tx);
    await syncCalendarFill(tx, at(0), at(24));
  });
}

const hm = (d: Date | null) => (d ? `${String((d.getUTCHours() + 17) % 24).padStart(2, "0")}:${String(d.getUTCMinutes()).padStart(2, "0")}` : "open");

/** Every block as origin, label and span: what the actual lane shows. Fill ids may change; this may not. */
async function spans() {
  const rows = await sql<{ origin: string; label: string | null; t_start: Date; t_end: Date | null; committed: boolean }[]>`
    SELECT b.origin, coalesce(b.calendar_event_id, i.title) AS label, b.t_start, b.t_end, b.committed
    FROM time_block b LEFT JOIN intent i ON i.id = b.intent_id ORDER BY b.t_start`;
  return rows.map((r) => `${r.origin}:${r.label} ${hm(r.t_start)}-${hm(r.t_end)}${r.committed ? " ✓" : ""}`);
}

/** One block and everything that hangs off it, every column. */
async function rowsOf(id: string) {
  const all = async (q: Promise<{ r: unknown }[]>) => (await q).map((x) => x.r);
  return {
    block: await all(sql`SELECT to_jsonb(b) AS r FROM time_block b WHERE id = ${id}`),
    proposals: await all(sql`SELECT to_jsonb(p) AS r FROM proposal p WHERE time_block_id = ${id} ORDER BY id`),
    commitments: await all(sql`SELECT to_jsonb(c) AS r FROM commitment c WHERE time_block_id = ${id} ORDER BY id`),
  };
}

/** Every block, proposal and commitment, every column. */
async function everything() {
  const all = async (q: Promise<{ r: unknown }[]>) => (await q).map((x) => x.r);
  return {
    blocks: await all(sql`SELECT to_jsonb(b) AS r FROM time_block b ORDER BY id`),
    proposals: await all(sql`SELECT to_jsonb(p) AS r FROM proposal p ORDER BY id`),
    commitments: await all(sql`SELECT to_jsonb(c) AS r FROM commitment c ORDER BY id`),
  };
}

async function stoppedBlock(startHour: number, endHour: number, intentId?: string) {
  const { id } = await startManualBlock(sql, { intentId }, ui, at(startHour));
  await stopRunningBlock(sql, ui, at(endHour));
  return id;
}

dbDescribe("undo a stop", () => {
  it("a running timer runs again with its id, start, label and suggestions, and the fill it let in goes again", async () => {
    await event("review", 8, 11);
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    const { id } = await startManualBlock(sql, { intentId: deep.id, title: "spec" }, ui, at(9));
    await proposeTime(sql, id, deep.id, agent, "looks like the spec");
    const before = await rowsOf(id);
    const lane = await spans();
    expect(lane).toEqual(["calendar:review 08:00-09:00", "manual:Deep work 09:00-open"]);

    const { undo } = await stopRunningBlock(sql, ui, at(10));
    expect(await spans()).toEqual(["calendar:review 08:00-09:00", "manual:Deep work 09:00-10:00", "calendar:review 10:00-11:00"]);
    const [made] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM proposal WHERE time_block_id = ${id} AND status = 'pending'`;
    expect(made!.n).toBe(1); // the stop's own suggestion; the agent's was expired

    await undoBlockChange(sql, undo!, at(10, 5));
    expect(await rowsOf(id)).toEqual(before); // the agent's suggestion pending again, the stop's gone
    expect(await spans()).toEqual(lane);
  });

  it("a paused timer is paused again at the same moment, so Resume still leaves the pause out", async () => {
    const { id } = await startManualBlock(sql, { title: "reading" }, ui, at(9));
    await pauseRunningBlock(sql, at(9, 30));
    const before = await rowsOf(id);
    const { undo } = await stopRunningBlock(sql, ui, at(10));
    expect(await spans()).toEqual(["manual:Uncategorized 09:00-09:30"]);
    await undoBlockChange(sql, undo!, at(10, 5));
    expect(await rowsOf(id)).toEqual(before);
    await resumeRunningBlock(sql, ui, at(10, 10));
    await stopRunningBlock(sql, ui, at(10, 30));
    expect(await spans()).toEqual(["manual:Uncategorized 09:00-09:30", "manual:Uncategorized 10:10-10:30"]);
  });

  it("a timer under a minute, which the stop dropped, comes back with its id, label and pause", async () => {
    const deep = await createIntent(sql, { type: "project", title: "Deep work" }, ui);
    const { id } = await startManualBlock(sql, { intentId: deep.id, title: "spec" }, ui, at(9));
    await pauseRunningBlock(sql, at(9, 0, 20));
    const before = await rowsOf(id);
    const { id: kept, undo } = await stopRunningBlock(sql, ui, at(9, 5));
    expect(kept).toBeNull();
    expect(await spans()).toEqual([]);
    await undoBlockChange(sql, undo!, at(9, 6));
    expect(await rowsOf(id)).toEqual(before);
  });

  it("is refused once another timer has started, or the block has been changed since", async () => {
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    await startManualBlock(sql, { intentId: a.id }, ui, at(9));
    const { undo: first } = await stopRunningBlock(sql, ui, at(10));
    await startManualBlock(sql, {}, ui, at(10, 30));
    const state = await everything();
    await expect(undoBlockChange(sql, first!, at(10, 40))).rejects.toThrow(/another timer has started/);
    expect(await everything()).toEqual(state);

    await stopRunningBlock(sql, ui, at(10, 50));
    const { id } = await startManualBlock(sql, {}, ui, at(11));
    const { undo: second } = await stopRunningBlock(sql, ui, at(12));
    await recategorizeBlock(sql, id, a.id, ui, {}, at(12, 5));
    const confirmed = await everything();
    await expect(undoBlockChange(sql, second!, at(12, 10))).rejects.toThrow(/changed since/);
    expect(await everything()).toEqual(confirmed);
  });

  it("is refused when a manual block now covers part of a 30 second timer's span, and changes nothing", async () => {
    const other = await stoppedBlock(8, 9);
    await startManualBlock(sql, { title: "mis-tap" }, ui, at(9));
    const { undo } = await stopRunningBlock(sql, ui, at(9, 0, 30));
    await editBlock(sql, other, { tStart: at(8, 30), tEnd: at(9, 0, 20) }, at(9, 1));
    const state = await everything();
    await expect(undoBlockChange(sql, undo!, at(9, 1))).rejects.toThrow(/now covers part of that time/);
    expect(await everything()).toEqual(state);
  });
});

dbDescribe("undo a delete", () => {
  it("a committed block comes back with its commitment, and the fill it let in goes again", async () => {
    await event("review", 9, 12);
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const id = await stoppedBlock(9, 11, a.id);
    await recategorizeBlock(sql, id, a.id, ui, {}, at(12));
    const before = await rowsOf(id);
    const lane = await spans();
    expect(lane).toEqual(["manual:A 09:00-11:00 ✓", "calendar:review 11:00-12:00"]);

    const undo = await deleteBlock(sql, id, at(12));
    expect(await spans()).toEqual(["calendar:review 09:00-12:00"]);
    await undoBlockChange(sql, undo, at(12, 1));
    expect(await rowsOf(id)).toEqual(before);
    expect(await spans()).toEqual(lane);
  });

  it("an uncommitted block comes back with its suggestion still pending", async () => {
    const id = await stoppedBlock(9, 10);
    const before = await rowsOf(id);
    expect(before.proposals).toEqual([expect.objectContaining({ status: "pending" })]);
    const undo = await deleteBlock(sql, id, at(11));
    expect(await rowsOf(id)).toEqual({ block: [], proposals: [], commitments: [] });
    await undoBlockChange(sql, undo, at(11, 1));
    expect(await rowsOf(id)).toEqual(before);
    await expect(undoBlockChange(sql, undo, at(11, 2))).rejects.toThrow(/already back/);
  });

  it("is refused when a manual block has moved into that hour since, and changes nothing", async () => {
    const a = await createIntent(sql, { type: "project", title: "A" }, ui);
    const gone = await stoppedBlock(9, 10, a.id);
    await recategorizeBlock(sql, gone, a.id, ui, {}, at(12));
    const other = await stoppedBlock(11, 12);
    const undo = await deleteBlock(sql, gone, at(12, 30));
    await editBlock(sql, other, { tStart: at(9, 30), tEnd: at(10, 30) }, at(12, 30));
    const state = await everything();
    await expect(undoBlockChange(sql, undo, at(12, 31))).rejects.toThrow(/now covers part of that time/);
    expect(await everything()).toEqual(state);
  });
});
