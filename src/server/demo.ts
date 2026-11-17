// The demo set's named variants (seeds/demo/README.md): small changes applied
// over the base that `pnpm demo:test` loads, so a link can ask for a state with
// `data=<name>`. Only on the test database; anywhere else `data=` does nothing.
//
// One variant is applied at a time. The row `demo_variant` in sync_cursor says
// which, at what clock, and how to undo it, so moving to another variant goes
// back through the base first.
import { type Sql, sql as defaultSql } from "../db/client.js";
import { uuidv7 } from "../shared/ids.js";
import { deleteBlock, pauseRunningBlock, proposeTime, startManualBlock } from "./attribution/blocks.js";
import { clockNow, fixedNow, isTestDatabase } from "./clock.js";
import { env } from "./env.js";
import { JOBS } from "./jobs/registry.js";
import { unflagTask } from "./intents.js";
import { errorMessage, log } from "./log.js";

export const DEMO_VARIANTS = ["base", "running", "paused", "noflags", "failing"] as const;
export type DemoVariant = (typeof DEMO_VARIANTS)[number];

/** The demo task the timer variants run on (seeds/demo/demo.json). */
export const DEMO_TIMER_TASK = "Draft the offline maps spec";
/** How long the running timer has run, and how long ago the paused one stopped. */
const RUNNING_MIN = 47;
const PAUSED_MIN = 12;

const STATE = "demo_variant";
let queue: Promise<unknown> = Promise.resolve();

interface State {
  name: DemoVariant;
  /** The fixed clock it was applied at ("live" on the wall clock): the same name at another fixed time is applied again. */
  at: string;
  undo: { blocks?: string[]; proposals?: Suggestion[]; flags?: { id: string; flagged_at: string; flag_note: string | null }[]; jobRuns?: string[] };
}

/**
 * A suggestion someone made on a fill block (the agent's, or the owner's own): fill does not make
 * it again when it rebuilds the block, so a variant that covers the block puts it back on undo.
 */
interface Suggestion {
  origin: string;
  t_start: string;
  t_end: string;
  intent_id: string;
  via: "ui" | "agent";
  source_ref: string | null;
  reason: string | null;
}

async function suggestionsFrom(db: Sql, from: Date): Promise<Suggestion[]> {
  const rows = await db<{ origin: string; t_start: Date; t_end: Date; intent_id: string; via: "ui" | "agent"; source_ref: string | null; reason: string | null }[]>`
    SELECT b.origin::text AS origin, b.t_start, b.t_end, p.intent_id, p.via, p.source_ref, p.reason
    FROM proposal p JOIN time_block b ON b.id = p.time_block_id
    WHERE p.status = 'pending' AND p.via IN ('ui', 'agent') AND p.intent_id IS NOT NULL
      AND b.origin IN ('calendar', 'auto_activity', 'place') AND b.t_end > ${from}`;
  return rows.map((r) => ({ ...r, t_start: r.t_start.toISOString(), t_end: r.t_end.toISOString() }));
}

const isVariant = (s: string): s is DemoVariant => (DEMO_VARIANTS as readonly string[]).includes(s);

function refuseOffTest(): void {
  if (!isTestDatabase(env.DATABASE_URL)) throw new Error(`demo variants run on the test database only, not ${env.DATABASE_URL}`);
}

async function readState(db: Sql): Promise<State | null> {
  const [row] = await db<{ token: string | null }[]>`SELECT token FROM sync_cursor WHERE name = ${STATE}`;
  return row?.token ? (JSON.parse(row.token) as State) : null;
}

async function writeState(db: Sql, state: State | null): Promise<void> {
  if (!state) {
    await db`DELETE FROM sync_cursor WHERE name = ${STATE}`;
    return;
  }
  await db`INSERT INTO sync_cursor (name, token, updated_at) VALUES (${STATE}, ${JSON.stringify(state)}, ${clockNow()})
    ON CONFLICT (name) DO UPDATE SET token = EXCLUDED.token, updated_at = EXCLUDED.updated_at`;
}

async function undo(db: Sql, state: State): Promise<void> {
  for (const id of state.undo.blocks ?? []) {
    const [b] = await db<{ id: string }[]>`SELECT id FROM time_block WHERE id = ${id}`;
    if (!b) continue;
    // Deleting the block re-syncs the fill under it.
    await deleteBlock(db, id);
    await db`DELETE FROM observation WHERE source = 'manual' AND payload->>'block_id' = ${id}`;
  }
  for (const p of state.undo.proposals ?? []) {
    const [b] = await db<{ id: string }[]>`
      SELECT id FROM time_block WHERE origin = ${p.origin}::block_origin AND t_start = ${new Date(p.t_start)} AND t_end = ${new Date(p.t_end)} AND NOT committed`;
    if (b) await proposeTime(db, b.id, p.intent_id, { via: p.via, sourceRef: p.source_ref }, p.reason);
  }
  for (const f of state.undo.flags ?? []) {
    await db`UPDATE intent SET flagged_at = ${new Date(f.flagged_at)}, flag_note = ${f.flag_note}, notified_at = ${new Date(f.flagged_at)} WHERE id = ${f.id}`;
  }
  if (state.undo.jobRuns?.length) await db`DELETE FROM job_run WHERE id = ANY(${state.undo.jobRuns}::uuid[])`;
}

async function apply(db: Sql, name: DemoVariant, now: Date): Promise<State["undo"]> {
  if (name === "base") return {};
  if (name === "running" || name === "paused") {
    const [task] = await db<{ id: string }[]>`SELECT id FROM intent WHERE title = ${DEMO_TIMER_TASK} AND status <> 'archived' LIMIT 1`;
    if (!task) throw new Error(`no "${DEMO_TIMER_TASK}": load the demo set first (pnpm demo:test)`);
    const start = new Date(now.getTime() - RUNNING_MIN * 60_000);
    // The running block takes the lane from its start on; the suggestions there go with the fill it replaces.
    const proposals = await suggestionsFrom(db, start);
    const { id } = await startManualBlock(db, { intentId: task.id }, { via: "ui", sourceRef: "demo" }, start);
    if (name === "paused") await pauseRunningBlock(db, new Date(now.getTime() - PAUSED_MIN * 60_000));
    return { blocks: [id], proposals };
  }
  if (name === "noflags") {
    const flagged = await db<{ id: string; flagged_at: Date; flag_note: string | null }[]>`
      SELECT id, flagged_at, flag_note FROM intent WHERE flagged_at IS NOT NULL`;
    for (const f of flagged) await unflagTask(db, f.id);
    return { flags: flagged.map((f) => ({ id: f.id, flagged_at: f.flagged_at.toISOString(), flag_note: f.flag_note })) };
  }
  // failing: every job's last run is an error, a few minutes ago, or just after the base's own
  // runs, which the loader dates at the end of the demo day (scripts/demo.ts).
  const [last] = await db<{ t: Date | null }[]>`SELECT max(started_at) AS t FROM job_run`;
  const at = new Date(Math.max(now.getTime() - 4 * 60_000, (last?.t?.getTime() ?? 0) + 60_000));
  const ids: string[] = [];
  for (const job of JOBS) {
    const id = uuidv7();
    await db`INSERT INTO job_run (id, name, started_at, finished_at, status, error)
      VALUES (${id}, ${job.name}, ${at}, ${at}, 'error', ${`demo: ${job.name} could not reach its source`})`;
    ids.push(id);
  }
  return { jobRuns: ids };
}

/** Move the test database to variant `name` over the demo base, at the current clock. */
export async function setDemoVariant(name: string, db: Sql = defaultSql): Promise<DemoVariant> {
  refuseOffTest();
  if (!isVariant(name)) throw new Error(`unknown demo variant ${name}; have: ${DEMO_VARIANTS.join(", ")}`);
  const now = clockNow();
  const at = fixedNow() ?? "live";
  // One request at a time moves the variant: a page and its calls can ask together. The test
  // server is one process, so a queue here is enough, and holds no connection while it waits.
  const turn = queue.then(async (): Promise<DemoVariant> => {
    const state = await readState(db);
    if (state?.name === name && state.at === at) return name;
    if (!state && name === "base") return name;
    if (state) await undo(db, state);
    const undone = await apply(db, name, now);
    await writeState(db, name === "base" ? null : { name, at, undo: undone });
    return name;
  });
  queue = turn.catch(() => undefined);
  return turn;
}

/** The variant a page load asks for with `data=`, on the test database only. */
export async function demoVariantForRequest(request: Request): Promise<void> {
  if (request.method !== "GET") return;
  const name = new URL(request.url).searchParams.get("data");
  if (!name || !isTestDatabase(env.DATABASE_URL)) return;
  try {
    await setDemoVariant(name);
  } catch (err) {
    log("error", "demo variant failed", { variant: name, err: errorMessage(err) });
  }
}
