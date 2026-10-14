import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { handleApi } from "../../src/server/api/v1.js";
import { Refused } from "../../src/server/errors.js";
import { getGraph, getIntentDetail, taskContextMarkdown } from "../../src/server/graph.js";
import {
  archiveIntent, assignTask, commitHabit, commitMetric, completeTask, createIntent, linkIntents, reopenTask,
  resetVisibility, undoCommitment, undoIntent, updateIntent,
} from "../../src/server/intents.js";
import { getContext } from "../../src/server/memory/context.js";
import { createPage } from "../../src/server/memory/pages.js";
import "../../src/server/memory/worldSections.js";
import { explainScore, recomputeScores } from "../../src/server/scoring/recompute.js";
import { dbDescribe } from "./helpers.js";

const ui = { via: "ui" as const };
const agent = { via: "agent" as const, sourceRef: "thread-9" };

const api = (method: string, path: string, body?: unknown) =>
  handleApi(
    new Request(`http://127.0.0.1:3847/api/v1${path}`, {
      method,
      headers: { authorization: "Bearer test-agent-token-000", "content-type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  );

dbDescribe("intents", () => {
  it("creates, links to a parent, edits, and archives; system intents are protected", async () => {
    const goal = await createIntent(sql, { type: "goal", title: "Ship LoR", weight: 3, weeklyHourTarget: 10 }, ui);
    const task = await createIntent(sql, { type: "task", title: "Write the fill sync", parentId: goal.id }, agent);
    expect(task).toMatchObject({ via: "agent", source_ref: "thread-9", status: "active", assignee: "owner" });
    const graph = await getGraph(sql);
    expect(graph.nodes.find((n) => n.id === task.id)!.parents.map((p) => p.title)).toEqual(["Ship LoR"]);
    expect(graph.nodes.find((n) => n.id === goal.id)!.children).toBe(1);
    expect(graph.nodes.some((n) => n.system)).toBe(false);

    const edited = await updateIntent(sql, goal.id, { title: "Ship Life on Rails", weeklyHourTarget: null });
    expect(edited).toMatchObject({ title: "Ship Life on Rails", weekly_hour_target: null, weight: 3 });

    const [meetings] = await sql<{ id: string }[]>`SELECT id FROM intent WHERE system AND title = 'Meetings'`;
    await expect(archiveIntent(sql, meetings!.id)).rejects.toBeInstanceOf(Refused);
    await archiveIntent(sql, goal.id);
    expect((await getGraph(sql)).nodes.map((n) => n.title)).toEqual(["Write the fill sync"]);
  });

  it("a waiting task records who it waits on and since when", async () => {
    const [who] = await sql<{ id: string }[]>`INSERT INTO entity (id, kind, name) VALUES (gen_random_uuid(), 'org', 'Acme') RETURNING id`;
    const task = await createIntent(sql, { type: "task", title: "Refund from Acme", waitingOnEntityId: who!.id }, agent);
    expect(task).toMatchObject({ status: "waiting", waiting_on: "Acme" });
    expect(task.waiting_since).not.toBeNull();
    const back = await updateIntent(sql, task.id, { status: "active" });
    expect(back.waiting_since).toBeNull();
  });

  it("delegated work is a task: assign, complete with a memo, read the outcome", async () => {
    const task = await createIntent(sql, { type: "task", title: "Book the dentist" }, ui);
    const assigned = await assignTask(sql, task.id, "agent", "worker-thread-1");
    expect(assigned).toMatchObject({ assignee: "agent", thread_ref: "worker-thread-1" });

    const ctx = await getContext(sql);
    expect(ctx.markdown).toContain("Tasks with the agent");
    expect(ctx.markdown).toContain("Book the dentist");

    await completeTask(sql, task.id, { memo: "Booked for 3 Nov at 2pm", via: "agent", sourceRef: "worker-thread-1" });
    const detail = await getIntentDetail(sql, task.id);
    expect(detail.intent.status).toBe("done");
    expect(detail.outcome).toMatchObject({ memo: "Booked for 3 Nov at 2pm", via: "agent", source_ref: "worker-thread-1" });
    expect((await getContext(sql)).markdown).not.toContain("Tasks with the agent");

    await reopenTask(sql, task.id);
    expect((await getIntentDetail(sql, task.id)).outcome).toBeNull();
  });

  it("a Google task cannot be completed here: the next poll would reopen it", async () => {
    await sql`INSERT INTO intent (id, type, title, google_task_id) VALUES (gen_random_uuid(), 'task', 'From Google', 'g-1')`;
    const [row] = await sql<{ id: string }[]>`SELECT id FROM intent WHERE google_task_id = 'g-1'`;
    const err = await completeTask(sql, row!.id, ui).catch((e) => e);
    expect(err).toBeInstanceOf(Refused);
    expect((err as Refused).code).toBe("google-task");
  });

  it("undo removes what the agent wrote: an intent with no history is deleted, a done mark is reversed", async () => {
    const stray = await createIntent(sql, { type: "task", title: "Agent's idea" }, agent);
    expect(await undoIntent(sql, stray.id)).toEqual({ removed: "deleted" });

    const used = await createIntent(sql, { type: "habit", title: "Stretch", frequency: 1 }, agent);
    const { id: cid } = await commitHabit(sql, used.id, agent);
    expect(await undoIntent(sql, used.id)).toEqual({ removed: "archived" });

    const task = await createIntent(sql, { type: "task", title: "T" }, ui);
    await completeTask(sql, task.id, agent);
    const [done] = await sql<{ id: string }[]>`SELECT id FROM commitment WHERE kind = 'task_done'`;
    await undoCommitment(sql, done!.id);
    expect((await getIntentDetail(sql, task.id)).intent.status).toBe("active");
    await undoCommitment(sql, cid);
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM commitment`;
    expect(n!.n).toBe(0);
  });

  it("Reset Visibility hides overdue tasks and projects only, and deletes nothing", async () => {
    const past = new Date(Date.now() - 86_400_000);
    const overdueTask = await createIntent(sql, { type: "task", title: "Overdue task", dueAt: past }, ui);
    const overdueProject = await createIntent(sql, { type: "project", title: "Overdue project", dueAt: past }, ui);
    const goal = await createIntent(sql, { type: "goal", title: "Overdue goal", dueAt: past }, ui);
    const fresh = await createIntent(sql, { type: "task", title: "Not due" }, ui);
    const picked = await createIntent(sql, { type: "task", title: "Picked by hand" }, ui);
    expect(await resetVisibility(sql, { alsoTaskIds: [picked.id] })).toEqual({ moved: 3 });
    const status = async (id: string) => (await getIntentDetail(sql, id)).intent.status;
    expect([await status(overdueTask.id), await status(overdueProject.id), await status(picked.id)]).toEqual(["later", "later", "later"]);
    expect([await status(goal.id), await status(fresh.id)]).toEqual(["active", "active"]);
  });
});

dbDescribe("scores in the database", () => {
  it("recompute writes a snapshot per intent and explain returns the parts", async () => {
    const goal = await createIntent(sql, { type: "goal", title: "Health", weight: 2, weeklyHourTarget: 5 }, ui);
    const habit = await createIntent(sql, { type: "habit", title: "Run", frequency: 3, frequencyHorizon: "week", parentId: goal.id }, ui);
    const metric = await createIntent(sql, { type: "metric", title: "Energy" }, ui);
    await linkIntents(sql, { fromId: metric.id, toId: habit.id, kind: "measures" }, ui);
    await commitHabit(sql, habit.id, ui);
    await commitHabit(sql, habit.id, ui);
    await commitHabit(sql, habit.id, ui);
    await commitMetric(sql, metric.id, 3, ui);

    const stats = await recomputeScores(sql);
    expect(stats.intents).toBe(3);
    const explained = await explainScore(sql, habit.id);
    // adherence 3/6, quality (3-1)/4, α 0.7
    expect(explained!.score).toBeCloseTo(0.5, 3);
    expect(explained!.parts.terms).toMatchObject({ completions: 3, expected: 6, adherence: 0.5, quality: 0.5 });

    const node = (await getGraph(sql)).nodes.find((n) => n.id === goal.id)!;
    expect(node).toMatchObject({ starving: true, week_hours: 0 });
    expect((await getContext(sql)).markdown).toContain("Starving goals");

    // Recomputing the same day replaces the snapshot instead of adding one.
    await recomputeScores(sql);
    const [n] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM score_snapshot`;
    expect(n!.n).toBe(3);
  });
});

dbDescribe("world model over the API", () => {
  it("creates a task under a parent, assigns it, closes it with a memo, and explains a score", async () => {
    const goal = await createIntent(sql, { type: "goal", title: "Home", weeklyHourTarget: 2 }, ui);
    const created = await api("POST", "/intents", { type: "task", title: "Fix the gate", parent_id: goal.id, assignee: "agent", thread: "main-1" });
    expect(created.status).toBe(200);
    const task = (await created.json()) as { id: string; via: string; assignee: string; thread_ref: string };
    expect(task).toMatchObject({ via: "agent", assignee: "agent", thread_ref: "main-1" });

    await createPage(sql, { slug: "goals/home", title: "Home", body: "The gate latch is a Yardlock.\n", intentId: goal.id, via: "ui" });
    const ctx = (await (await api("GET", `/context?task=${task.id}`)).json()) as { markdown: string };
    expect(ctx.markdown).toContain("Your task: Fix the gate");
    expect(ctx.markdown).toContain("The one rule");
    expect(ctx.markdown).toContain("Yardlock");
    expect(await taskContextMarkdown(sql, task.id)).toBe(ctx.markdown);

    expect((await api("POST", `/intents/${task.id}/done`, { memo: "Replaced the latch", thread: "worker-2" })).status).toBe(200);
    const detail = (await (await api("GET", `/intents/${task.id}`)).json()) as { outcome: { memo: string; source_ref: string } };
    expect(detail.outcome).toMatchObject({ memo: "Replaced the latch", source_ref: "worker-2" });

    const score = await api("GET", `/intents/${goal.id}/score`);
    expect(score.status).toBe(200);
    expect(((await score.json()) as { parts: { type: string } }).parts.type).toBe("goal");

    const graph = (await (await api("GET", "/graph")).json()) as { results: unknown[]; markdown: string };
    expect(graph.results).toHaveLength(2);
    expect(graph.markdown).toContain("## Goals");
    expect((await api("GET", "/graph?waiting=1").then((r) => r.json())) as { results: unknown[] }).toMatchObject({ results: [] });
  });
});
