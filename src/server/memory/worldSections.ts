// What the world model adds to the agent's digest and to `lor changes`.
import { type GraphNode, getGraph, starvingLine, taskLine } from "../graph.js";
import { registerChangeProvider } from "./changes.js";
import { registerContextSection } from "./context.js";

const open = (n: GraphNode) => n.status === "active" || n.status === "waiting";

registerContextSection(async (db, now) => {
  const { nodes } = await getGraph(db, now);
  const starving = nodes.filter((n) => n.starving);
  if (starving.length === 0) return null;
  return { title: "Starving goals", markdown: starving.map((n) => `- ${starvingLine(n)}`).join("\n") };
});

registerContextSection(async (db, now) => {
  const { nodes } = await getGraph(db, now);
  const mine = nodes.filter((n) => n.type === "task" && n.assignee === "agent" && open(n));
  if (mine.length === 0) return null;
  return { title: "Tasks with the agent", markdown: mine.map((n) => `- ${taskLine(n)}`).join("\n") };
});

registerContextSection(async (db, now) => {
  const { nodes } = await getGraph(db, now);
  const waiting = nodes.filter((n) => n.type === "task" && n.status === "waiting");
  const due = nodes
    .filter((n) => n.type === "task" && n.status === "active" && n.assignee === "owner" && n.due_at)
    .filter((n) => new Date(n.due_at!).getTime() - now.getTime() < 3 * 86_400_000)
    .sort((a, b) => a.due_at!.localeCompare(b.due_at!));
  if (waiting.length === 0 && due.length === 0) return null;
  const out: string[] = [];
  if (due.length) out.push("Due within three days or overdue:", ...due.slice(0, 15).map((n) => `- ${taskLine(n)}`));
  if (waiting.length) out.push(...(out.length ? [""] : []), "Waiting on others:", ...waiting.slice(0, 15).map((n) => `- ${taskLine(n)}`));
  return { title: "Sumeet's tasks", markdown: out.join("\n") };
});

registerChangeProvider(async (db, since, until) => {
  const added = await db<{ id: string; summary: string; at: Date }[]>`
    SELECT id, concat(type::text, ': ', title, ' (', via, ')') AS summary, created_at AS at
    FROM intent WHERE NOT system AND created_at > ${since} AND created_at <= ${until} ORDER BY created_at DESC LIMIT 100`;
  const done = await db<{ id: string; summary: string; at: Date }[]>`
    SELECT i.id, concat(i.title, coalesce(' — ' || c.memo, ''), ' (', c.via, ')') AS summary, c.created_at AS at
    FROM commitment c JOIN intent i ON i.id = c.intent_id
    WHERE c.kind = 'task_done' AND c.created_at > ${since} AND c.created_at <= ${until} ORDER BY c.created_at DESC LIMIT 100`;
  const toItems = (rows: { id: string; summary: string; at: Date }[]) =>
    rows.map((r) => ({ id: r.id, summary: r.summary, at: r.at.toISOString() }));
  return [
    { kind: "intent:added", count: added.length, items: toItems(added) },
    { kind: "task:done", count: done.length, items: toItems(done) },
  ];
});
