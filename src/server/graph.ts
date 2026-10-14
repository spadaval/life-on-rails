// Read side of the world model: every intent with its latest score, its
// parents, and the flags Today and the agent care about.
import type { Tx } from "../db/client.js";
import type { JsonObject } from "../shared/json.js";
import { localYmd } from "../shared/localTime.js";
import { env } from "./env.js";
import { NotFound } from "./errors.js";
import { type IntentView, type LinkView, getIntent, listIntents, listLinks, taskOutcome } from "./intents.js";
import { latestScores } from "./scoring/recompute.js";

export interface GraphNode extends IntentView {
  score: number | null;
  confidence: string | null;
  /** Goals: hours in the last 7 days against the weekly target. */
  week_hours: number | null;
  starving: boolean;
  overdue: boolean;
  /** The due date as a local calendar date, for display and editing. */
  due_local: string | null;
  /** Projects: how many of their tasks are done. */
  tasks_done: number;
  tasks_total: number;
  parents: { link_id: string; id: string; title: string; kind: string; via: string }[];
  children: number;
}

export interface Graph {
  nodes: GraphNode[];
  links: LinkView[];
}

export async function getGraph(db: Tx, now = new Date()): Promise<Graph> {
  const [intents, links, scores] = await Promise.all([listIntents(db), listLinks(db), latestScores(db)]);
  const byId = new Map(intents.map((i) => [i.id, i]));
  const scoreBy = new Map(scores.map((s) => [s.intent_id, s]));
  const nodes = intents
    .filter((i) => !i.system)
    .map((i): GraphNode => {
      const s = scoreBy.get(i.id);
      const terms = (s?.parts.terms ?? {}) as JsonObject;
      const week = (terms.last_7_days ?? null) as { hours?: number; starving?: boolean } | null;
      return {
        ...i,
        score: s?.score ?? null,
        confidence: typeof s?.parts.confidence === "string" ? s.parts.confidence : null,
        week_hours: typeof week?.hours === "number" ? week.hours : null,
        starving: i.type === "goal" && i.status === "active" && week?.starving === true,
        overdue: i.due_at !== null && new Date(i.due_at).getTime() < now.getTime() && i.status !== "done",
        due_local: i.due_at ? localYmd(env.LOR_TZ, new Date(i.due_at)) : null,
        tasks_done: typeof terms.tasks_done === "number" ? terms.tasks_done : 0,
        tasks_total: typeof terms.tasks_total === "number" ? terms.tasks_total : 0,
        parents: links
          .filter((l) => l.from_id === i.id && byId.has(l.to_id))
          .map((l) => ({ link_id: l.id, id: l.to_id, title: byId.get(l.to_id)!.title, kind: l.kind, via: l.via })),
        children: links.filter((l) => l.to_id === i.id && l.kind === "measures" && byId.has(l.from_id)).length,
      };
    });
  return { nodes, links };
}

const pct = (x: number | null) => (x === null ? "—" : `${Math.round(x * 100)}%`);

export function starvingLine(n: GraphNode): string {
  return `${n.title}: ${n.week_hours ?? 0}h of ${n.weekly_hour_target}h in the last 7 days (id ${n.id})`;
}

export function taskLine(n: GraphNode): string {
  const bits = [n.title];
  if (n.due_local) bits.push(`due ${n.due_local}`);
  if (n.status === "waiting") bits.push(`waiting on ${n.waiting_on ?? "someone"} since ${n.waiting_since?.slice(0, 10) ?? "?"}`);
  if (n.parents.length) bits.push(`for ${n.parents.map((p) => p.title).join(", ")}`);
  if (n.assignee === "agent") bits.push("with the agent");
  return `${bits.join(" · ")} (id ${n.id})`;
}

export function renderGraph(nodes: GraphNode[]): string {
  const out: string[] = [];
  const section = (title: string, type: string, line: (n: GraphNode) => string) => {
    const rows = nodes.filter((n) => n.type === type && n.status !== "done");
    if (rows.length === 0) return;
    out.push(`## ${title}`, "");
    for (const n of rows) out.push(`- ${line(n)}`);
    out.push("");
  };
  section("Goals", "goal", (n) =>
    `${n.title} — ${pct(n.score)}${n.weekly_hour_target ? `, ${n.week_hours ?? 0}/${n.weekly_hour_target}h this week` : ""}${n.starving ? " **starving**" : ""} (id ${n.id})`);
  section("Projects", "project", (n) =>
    `${n.title} — ${pct(n.score)}${n.tasks_total ? `, ${n.tasks_done}/${n.tasks_total} tasks done` : ""}${n.parents.length ? ` · for ${n.parents.map((p) => p.title).join(", ")}` : ""} (id ${n.id})`);
  section("Habits", "habit", (n) => `${n.title} — ${pct(n.score)} (id ${n.id})`);
  section("Metrics", "metric", (n) => `${n.title} — ${pct(n.score)} (id ${n.id})`);
  section("Tasks", "task", taskLine);
  return out.join("\n") || "Nothing in the graph yet.\n";
}

export interface IntentDetail {
  intent: IntentView;
  parents: { id: string; title: string; type: string; kind: string }[];
  children: { id: string; title: string; type: string; kind: string; status: string }[];
  score: number | null;
  parts: JsonObject | null;
  /** Slug of the page about this intent, if one exists. */
  page: string | null;
  /** For a finished task: what was recorded when it was closed. */
  outcome: { memo: string | null; at: string; via: string; source_ref: string | null } | null;
}

export async function getIntentDetail(db: Tx, id: string): Promise<IntentDetail> {
  const intent = await getIntent(db, id);
  const parents = await db<{ id: string; title: string; type: string; kind: string }[]>`
    SELECT i.id, i.title, i.type, l.kind FROM intent_link l JOIN intent i ON i.id = l.to_id WHERE l.from_id = ${id}`;
  const children = await db<{ id: string; title: string; type: string; kind: string; status: string }[]>`
    SELECT i.id, i.title, i.type, l.kind, i.status FROM intent_link l JOIN intent i ON i.id = l.from_id
    WHERE l.to_id = ${id} AND i.status <> 'archived' ORDER BY i.type, lower(i.title)`;
  const score = await db<{ score: number | null; parts: JsonObject }[]>`
    SELECT score, parts FROM score_snapshot WHERE intent_id = ${id} ORDER BY as_of DESC LIMIT 1`;
  const page = await db<{ slug: string }[]>`SELECT slug FROM page WHERE intent_id = ${id}`;
  return {
    intent,
    parents: parents.map((p) => ({ ...p })),
    children: children.map((c) => ({ ...c })),
    score: score[0]?.score ?? null,
    parts: score[0]?.parts ?? null,
    page: page[0]?.slug ?? null,
    outcome: intent.type === "task" ? await taskOutcome(db, id) : null,
  };
}

/**
 * The digest for a worker doing one task: the task, what it serves, and the
 * pages for the projects and people it touches. Nothing about the rest of the day.
 */
export async function taskContextMarkdown(db: Tx, id: string): Promise<string> {
  const d = await getIntentDetail(db, id);
  if (d.intent.type !== "task") throw new NotFound(`${id} is a ${d.intent.type}, not a task`);
  const now = new Date();
  const out: string[] = [`# Context for one task: ${localYmd(env.LOR_TZ, now)} (${env.LOR_TZ})`, ""];
  const agent = await db<{ body: string }[]>`SELECT body FROM page WHERE slug = 'agent'`;
  if (agent[0]) out.push("## Page: agent", "", agent[0].body.trim(), "");
  out.push(`## Your task: ${d.intent.title}`, "", `- id: ${d.intent.id}`, `- status: ${d.intent.status}, assigned to: ${d.intent.assignee}`);
  if (d.intent.due_at) out.push(`- due: ${d.intent.due_at}`);
  if (d.intent.waiting_on) out.push(`- waiting on: ${d.intent.waiting_on}`);
  if (d.intent.description) out.push("", d.intent.description.trim());
  if (d.parents.length) out.push("", "It serves: " + d.parents.map((p) => `${p.title} (${p.type}, id ${p.id})`).join("; "));
  if (d.children.length) out.push("", "Parts of it: " + d.children.map((c) => `${c.title} (${c.status}, id ${c.id})`).join("; "));
  out.push("", `Finish with: lor done ${d.intent.id} --memo "<what happened>"`, "");

  const related = [d.intent.id, ...d.parents.map((p) => p.id)];
  const pages = await db<{ slug: string; title: string; body: string }[]>`
    SELECT slug, title, body FROM page
    WHERE intent_id = ANY(${related}::uuid[])
       OR (${d.intent.waiting_on_entity_id}::uuid IS NOT NULL AND entity_id = ${d.intent.waiting_on_entity_id})`;
  for (const p of pages) out.push(`## Page: ${p.slug}`, "", p.body.trim(), "");
  return out.join("\n");
}
