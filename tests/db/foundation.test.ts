import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { systemIntentId } from "../../src/db/seed.js";
import { recordObservation } from "../../src/server/observations.js";
import { registerJobs, runJob, startScheduler, stopScheduler } from "../../src/server/jobs/scheduler.js";
import { uuidv7 } from "../../src/shared/ids.js";
import { dbDescribe } from "./helpers.js";

async function newIntent(title = "Ship LoR"): Promise<string> {
  const id = uuidv7();
  await sql`INSERT INTO intent (id, type, title) VALUES (${id}, 'project', ${title})`;
  return id;
}

async function newBlock(opts: { open?: boolean; intentId?: string } = {}): Promise<string> {
  const id = uuidv7();
  const start = new Date("2026-10-14T16:00:00Z");
  const end = opts.open ? null : new Date("2026-10-14T17:00:00Z");
  await sql`INSERT INTO time_block (id, t_start, t_end, origin, intent_id)
    VALUES (${id}, ${start}, ${end}, 'manual', ${opts.intentId ?? null})`;
  return id;
}

dbDescribe("migration and seed", () => {
  it("creates the tables and the two system intents", async () => {
    const tables = await sql<{ tablename: string }[]>`SELECT tablename FROM pg_tables WHERE schemaname = 'public'`;
    const names = tables.map((t) => t.tablename);
    for (const t of ["intent", "observation", "proposal", "commitment", "time_block", "job_run", "score_snapshot"]) {
      expect(names).toContain(t);
    }
    expect(names).not.toContain("user_profile");
    expect(names).not.toContain("title_embedding");
    expect(await systemIntentId(sql, "Uncategorized")).toMatch(/^[0-9a-f-]{36}$/);
    expect(await systemIntentId(sql, "Meetings")).toMatch(/^[0-9a-f-]{36}$/);
  });
});

dbDescribe("time_block invariants", () => {
  it("allows at most one open block", async () => {
    await newBlock({ open: true });
    await expect(newBlock({ open: true })).rejects.toThrow(/time_block_one_open/);
    // Closed blocks are unlimited.
    await newBlock();
    await newBlock();
  });
});

dbDescribe("delete rules", () => {
  it("history cannot vanish: intent and block deletes are restricted by a commitment", async () => {
    const intentId = await newIntent();
    const blockId = await newBlock({ intentId });
    const proposalId = uuidv7();
    await sql`INSERT INTO proposal (id, confidence, time_block_id, intent_id) VALUES (${proposalId}, 1, ${blockId}, ${intentId})`;
    const commitmentId = uuidv7();
    await sql`INSERT INTO commitment (id, kind, intent_id, t, t_end, proposal_id, time_block_id)
      VALUES (${commitmentId}, 'time_spent', ${intentId}, now(), now(), ${proposalId}, ${blockId})`;
    await sql`UPDATE time_block SET committed = true, commitment_id = ${commitmentId} WHERE id = ${blockId}`;

    await expect(sql`DELETE FROM intent WHERE id = ${intentId}`).rejects.toThrow(/foreign key/);
    await expect(sql`DELETE FROM time_block WHERE id = ${blockId}`).rejects.toThrow(/foreign key/);

    // Deleting the proposal leaves the commitment, minus its pointer.
    await sql`DELETE FROM proposal WHERE id = ${proposalId}`;
    const [c] = await sql<{ proposal_id: string | null }[]>`SELECT proposal_id FROM commitment WHERE id = ${commitmentId}`;
    expect(c?.proposal_id).toBeNull();

    // Deleting the commitment clears the block's pointer; then the block can go.
    await sql`DELETE FROM commitment WHERE id = ${commitmentId}`;
    const [b] = await sql<{ commitment_id: string | null }[]>`SELECT commitment_id FROM time_block WHERE id = ${blockId}`;
    expect(b?.commitment_id).toBeNull();
    await sql`DELETE FROM time_block WHERE id = ${blockId}`;
  });

  it("deleting a block takes its proposals with it", async () => {
    const intentId = await newIntent();
    const blockId = await newBlock({ intentId });
    await sql`INSERT INTO proposal (id, confidence, time_block_id, intent_id) VALUES (${uuidv7()}, 0.7, ${blockId}, ${intentId})`;
    await sql`DELETE FROM time_block WHERE id = ${blockId}`;
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM proposal`;
    expect(n?.n).toBe(0);
  });

  it("a second time_spent commitment for one block is refused", async () => {
    const intentId = await newIntent();
    const blockId = await newBlock({ intentId });
    const insert = () => sql`INSERT INTO commitment (id, kind, intent_id, t, time_block_id)
      VALUES (${uuidv7()}, 'time_spent', ${intentId}, now(), ${blockId})`;
    await insert();
    await expect(insert()).rejects.toThrow(/commitment_one_time_spent_per_block/);
  });
});

dbDescribe("recordObservation", () => {
  const base = { device: "pixel-9a", source: "android" as const, title: "Maps", tStart: new Date("2026-10-14T16:00:00Z") };

  it("stores payload as a JSON object, not a string", async () => {
    const { id } = await recordObservation(sql, { ...base, tEnd: null, payload: { pkg: "com.google.maps" } });
    const [row] = await sql<{ t: string; pkg: string }[]>`SELECT jsonb_typeof(payload) AS t, payload->>'pkg' AS pkg FROM observation WHERE id = ${id}`;
    expect(row).toEqual({ t: "object", pkg: "com.google.maps" });
  });

  it("extends t_end, never shortens it, never reopens", async () => {
    const { id, inserted } = await recordObservation(sql, { ...base, tEnd: null });
    expect(inserted).toBe(true);
    const end = async () => (await sql<{ t_end: Date | null }[]>`SELECT t_end FROM observation WHERE id = ${id}`)[0]?.t_end?.toISOString() ?? null;

    await recordObservation(sql, { ...base, id, tEnd: new Date("2026-10-14T16:05:00Z") });
    expect(await end()).toBe("2026-10-14T16:05:00.000Z");
    await recordObservation(sql, { ...base, id, tEnd: new Date("2026-10-14T16:09:00Z") });
    expect(await end()).toBe("2026-10-14T16:09:00.000Z");
    await recordObservation(sql, { ...base, id, tEnd: new Date("2026-10-14T16:02:00Z") });
    expect(await end()).toBe("2026-10-14T16:09:00.000Z");
    const again = await recordObservation(sql, { ...base, id, tEnd: null });
    expect(again.inserted).toBe(false);
    expect(await end()).toBe("2026-10-14T16:09:00.000Z");
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM observation`;
    expect(n?.n).toBe(1);
  });
});

dbDescribe("scheduler", () => {
  it("records a run, skips a second trigger while one is in flight, records a failure", async () => {
    let release: () => void = () => {};
    const gate = new Promise<void>((r) => (release = r));
    registerJobs([
      { name: "slow", everyS: 60, run: async () => { await gate; return { n: 1 }; } },
      { name: "broken", everyS: 60, run: async () => { throw new Error("boom"); } },
    ]);
    const first = runJob("slow");
    await new Promise((r) => setTimeout(r, 50));
    expect(await runJob("slow")).toBe("skipped");
    release();
    expect(await first).toBe("ok");
    expect(await runJob("broken")).toBe("error");

    const rows = await sql<{ name: string; status: string; error: string | null; n: string | null }[]>`
      SELECT name, status, error, stats->>'n' AS n FROM job_run ORDER BY name`;
    expect(rows).toEqual([
      { name: "broken", status: "error", error: "boom", n: null },
      { name: "slow", status: "ok", error: null, n: "1" },
    ]);
  });

  it("marks runs left `running` by a dead process as abandoned at start", async () => {
    await sql`INSERT INTO job_run (id, name, status) VALUES (${uuidv7()}, 'calendar_poll', 'running')`;
    registerJobs([]);
    await startScheduler();
    stopScheduler();
    const [row] = await sql<{ status: string; error: string }[]>`SELECT status, error FROM job_run WHERE name = 'calendar_poll'`;
    expect(row).toEqual({ status: "error", error: "abandoned" });
  });
});
