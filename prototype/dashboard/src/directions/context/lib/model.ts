// What a context is and what belongs to it. Pure functions of the store's state and the clock:
// nothing here is held in React, so Undo, Switch and a scene change redraw it all.
//
// A context is the thing being worked on: a project or goal (everything under it), a person
// (what mentions them), a slot (the next fixed event and the gap before it), or "Loose ends"
// (tasks under nothing). The brief's risk is that some things belong to no context; Loose ends
// and the unnamed timer are where those live, and the agent's suggested links move them out.
import { isOpen } from "../../../data/derive.js";
import type { State } from "../../../data/store.js";
import type { BlockView, Node, PlannedEvent, Person } from "../../../data/types.js";

export type Kind = "project" | "goal" | "person" | "slot" | "loose" | "unnamed" | "label";

export interface Ctx {
  kind: Kind;
  /** The node, person or event id; "loose" and "unnamed" have none. */
  key: string;
  title: string;
  /** The link's id: a title or a name, since ids change with every snapshot. */
  linkId: string;
  node?: Node;
  person?: Person;
  event?: PlannedEvent;
}

/** Why this context is the one shown on Now. */
export type Why = "timer" | "stopped" | "one" | "recent" | "none";

const byId = (s: State) => new Map(s.nodes.map((n) => [n.id, n]));

// ---- the agent's one thing, and suggested links ----

/**
 * The agent's one thing for the day. The snapshot does not record it, so this direction names it
 * by title: the brief's first line is about it. In the quiet scene the agent named nothing.
 */
export const ONE_THING = {
  title: "Draft the offline maps spec",
  why: "The brief leads with it: finish it before the design review. Lena is waiting on the icon sizes.",
};

/**
 * Where the agent would put tasks that sit under nothing. Made up here (by title): the store has
 * no proposals for links. He accepts one with Link, which the store can undo.
 */
export const SUGGEST: Record<string, string> = {
  "Renew the parking permit": "Keep the house running",
  "Pick up dry cleaning": "Keep the house running",
  "Return the library books": "Keep the house running",
  "Buy a birthday card for Sam": "Be a good friend",
  "Read the essay Daniel sent": "Be a good friend",
  "Back up the laptop": "Ship the Field Guide app",
  "Look into a standing desk": "Keep the house running",
};

// ---- which context a thing belongs to ----

/** The context a node lives in: itself for a project or goal; else its nearest project or goal above. */
export function homeOf(s: State, n: Node, seen = new Set<string>()): Ctx {
  if (n.type === "project" || n.type === "goal") return nodeCtx(n);
  const map = byId(s);
  let cur: Node | undefined = n;
  while (cur && !seen.has(cur.id)) {
    seen.add(cur.id);
    const up: Node | undefined = cur.parents.map((p) => map.get(p.id)).find(Boolean);
    if (!up) break;
    if (up.type === "project" || up.type === "goal") return nodeCtx(up);
    cur = up;
  }
  return LOOSE;
}

export const LOOSE: Ctx = { kind: "loose", key: "loose", title: "Loose ends", linkId: "loose" };
export const nodeCtx = (n: Node): Ctx => ({ kind: n.type === "goal" ? "goal" : "project", key: n.id, title: n.title, linkId: n.title, node: n });
export const personCtx = (p: Person): Ctx => ({ kind: "person", key: p.id, title: p.name, linkId: p.name, person: p });
export const slotCtx = (e: PlannedEvent): Ctx => ({ kind: "slot", key: e.event_id, title: e.title ?? "An event", linkId: e.title ?? e.event_id, event: e });

/** Every node under `root` (not itself), following parent links downwards. */
export function under(s: State, root: Node): Node[] {
  const kids = new Map<string, Node[]>();
  for (const n of s.nodes) for (const p of n.parents) kids.set(p.id, [...(kids.get(p.id) ?? []), n]);
  const out: Node[] = [];
  const seen = new Set([root.id]);
  const stack = [...(kids.get(root.id) ?? [])];
  while (stack.length) {
    const n = stack.shift()!;
    if (seen.has(n.id)) continue;
    seen.add(n.id);
    out.push(n);
    stack.push(...(kids.get(n.id) ?? []));
  }
  return out;
}

/** The nodes a context holds (tasks, habits, projects under it). */
export function members(s: State, c: Ctx): Node[] {
  if (c.node) return under(s, c.node);
  if (c.kind === "loose") return s.nodes.filter((n) => n.type === "task" && n.parents.length === 0);
  if (c.kind === "person" && c.person) return s.nodes.filter((n) => n.type === "task" && (n.waiting_on_entity_id === c.person!.id || mentions(n.title, c.person!.name)));
  if (c.kind === "slot" && c.event) {
    const names = s.people.filter((p) => mentions(c.event!.title ?? "", p.name));
    return s.nodes.filter((n) => n.type === "task" && names.some((p) => mentions(n.title, p.name)));
  }
  return [];
}

/** Whether `text` names a person: their full name, or their first name as a word. */
export function mentions(text: string, name: string): boolean {
  if (!text) return false;
  const esc = (x: string) => x.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const first = name.split(/\s+/)[0]!;
  const words = name.startsWith("Grandpa") || name.split(/\s+/).length === 1 ? [name] : [name, first];
  return words.some((w) => new RegExp(`(^|[^\\p{L}])${esc(w)}($|[^\\p{L}])`, "iu").test(text));
}

// ---- what Now shows ----

/** The running block, if any. */
export const runningBlock = (s: State) => s.blocks.find((b) => b.t_end === null) ?? null;

/** The context a block of time belongs to. */
export function blockCtx(s: State, b: BlockView): Ctx | null {
  if (!b.intent_id) return null;
  const n = s.nodes.find((x) => x.id === b.intent_id);
  if (n) return homeOf(s, n);
  const sys = s.system.find((x) => x.id === b.intent_id);
  return sys ? { kind: "label", key: sys.id, title: sys.title, linkId: sys.title } : null;
}

/** The context Now opens on, and why. */
export function current(s: State, now: number): { ctx: Ctx; why: Why; focus: Node | null } {
  const r = runningBlock(s);
  if (r) {
    if (!r.intent_id) return { ctx: { kind: "unnamed", key: r.id, title: "An unnamed timer", linkId: "unnamed" }, why: "timer", focus: null };
    const c = blockCtx(s, r);
    const focus = s.nodes.find((n) => n.id === r.intent_id && n.type === "task") ?? null;
    if (c) return { ctx: c, why: "timer", focus };
  }
  // Just stopped (within 20 minutes): stay where he was, rather than jumping away under him.
  const last = [...s.blocks].filter((b) => b.origin === "manual" && b.t_end && b.intent_id).sort((a, b) => b.t_end!.localeCompare(a.t_end!))[0];
  if (last && now - Date.parse(last.t_end!) < 20 * 60_000) {
    const c = blockCtx(s, last);
    if (c && c.kind !== "label") return { ctx: c, why: "stopped", focus: s.nodes.find((n) => n.id === last.intent_id && n.type === "task" && isOpen(n)) ?? null };
  }
  if (s.scene !== "quiet") {
    const one = s.nodes.find((n) => n.title === ONE_THING.title && isOpen(n));
    if (one) return { ctx: homeOf(s, one), why: "one", focus: one };
  }
  for (const r of s.recent) {
    const n = s.nodes.find((x) => x.id === r.id && x.status !== "archived");
    if (n) return { ctx: homeOf(s, n), why: "recent", focus: n.type === "task" && isOpen(n) ? n : null };
  }
  return { ctx: LOOSE, why: "none", focus: null };
}

// ---- what a context holds ----

export interface PersonLink {
  person: Person;
  /** Why they are here: "waiting on the icon sizes", "named in a task", "on the page". */
  why: string;
  waiting: boolean;
}

/** The context's page in Notes: the same title, or a page whose title starts its title. */
export function pageOf(s: State, c: Ctx) {
  if (c.person) return s.pages.find((p) => p.slug === c.person!.page) ?? null;
  if (!c.node) return null;
  const t = c.node.title.toLowerCase();
  return s.pages.find((p) => p.title.toLowerCase() === t) ?? s.pages.find((p) => p.slug.startsWith("topics/") && t.startsWith(p.title.toLowerCase())) ?? null;
}

const linksIn = (body: string) => [...body.matchAll(/\[\[([^\]|]+)(?:\|[^\]]*)?\]\]/g)].map((m) => m[1]!.trim());

/** The people a context waits on, then those its tasks or its page (and the pages it links) name. */
export function peopleOf(s: State, c: Ctx, nodes: Node[]): PersonLink[] {
  const out = new Map<string, PersonLink>();
  const add = (p: Person | undefined, why: string, waiting = false) => {
    if (!p || (c.person && p.id === c.person.id) || out.has(p.id)) return;
    out.set(p.id, { person: p, why, waiting });
  };
  const open = nodes.filter((n) => n.type === "task" && isOpen(n));
  for (const n of open) if (n.waiting_on_entity_id) add(s.people.find((p) => p.id === n.waiting_on_entity_id), `waiting: ${n.title}`, true);
  // A project also shows who its goal waits on: the goal's work is the reason it exists.
  if (c.kind === "project" && c.node) {
    for (const g of c.node.parents) {
      const goal = s.nodes.find((x) => x.id === g.id);
      if (!goal) continue;
      for (const n of under(s, goal)) if (n.type === "task" && isOpen(n) && n.waiting_on_entity_id) add(s.people.find((p) => p.id === n.waiting_on_entity_id), `waiting: ${n.title}`, true);
    }
  }
  for (const n of open) for (const p of s.people) if (p.kind === "person" && mentions(n.title, p.name)) add(p, n.title);
  const page = pageOf(s, c);
  if (page) {
    const hop = [page.slug, ...linksIn(page.body)];
    for (const slug of hop) {
      const pg = s.pages.find((p) => p.slug === slug);
      if (!pg) continue;
      for (const l of linksIn(pg.body)) if (l.startsWith("people/")) add(s.people.find((p) => p.page === l), slug === page.slug ? "on its page" : `on ${pg.title}`);
    }
  }
  if (c.event) for (const p of s.people) if (mentions(c.event.title ?? "", p.name)) add(p, "at this event");
  return [...out.values()];
}

/** Pages that link to a person's page: the contexts they are part of. */
export function pagesNaming(s: State, slug: string) {
  return s.pages.filter((p) => p.slug !== slug && linksIn(p.body).includes(slug));
}

export interface TimeView {
  /** Today's blocks that count toward this context, oldest first; the running one last. */
  blocks: BlockView[];
  /** The agent's guesses that point here, waiting for a yes. */
  guesses: BlockView[];
  /** ms today: committed and running. */
  todayMs: number;
  /** The goal whose week is shown, its hours and target. Projects show their goal's week. */
  week: { goal: Node; hours: number; target: number | null } | null;
}

export function timeOf(s: State, c: Ctx, nodes: Node[], now: number): TimeView {
  const ids = new Set(nodes.map((n) => n.id));
  if (c.node) ids.add(c.node.id);
  if (c.kind === "label") ids.add(c.key);
  const mine = s.blocks.filter((b) => b.intent_id && ids.has(b.intent_id));
  const blocks = mine.filter((b) => b.committed || b.t_end === null || (b.origin === "manual" && !b.proposal));
  const guesses = mine.filter((b) => b.proposal && !b.committed && b.t_end);
  const todayMs = blocks.reduce((a, b) => a + ((b.t_end ? Date.parse(b.t_end) : b.paused_at ? Date.parse(b.paused_at) : now) - Date.parse(b.t_start)), 0);
  let goal: Node | undefined = c.kind === "goal" ? c.node : undefined;
  if (c.kind === "project" && c.node) goal = s.nodes.find((x) => x.type === "goal" && c.node!.parents.some((p) => p.id === x.id));
  // Week figures come from goals only: a project's week_hours is not kept until its first Stop.
  const week = goal ? { goal, hours: goal.week_hours ?? 0, target: goal.weekly_hour_target } : null;
  return { blocks: blocks.sort((a, b) => a.t_start.localeCompare(b.t_start)), guesses, todayMs, week };
}

/** The next fixed event after now, and the free time before it. */
export function nextEvent(s: State, now: number) {
  const e = s.day.planned.filter((x) => !x.all_day && Date.parse(x.t_start) > now).sort((a, b) => a.t_start.localeCompare(b.t_start))[0];
  return e ? { event: e, gapMs: Date.parse(e.t_start) - now } : null;
}
export const eventNow = (s: State, now: number) => s.day.planned.find((x) => !x.all_day && Date.parse(x.t_start) <= now && Date.parse(x.t_end) > now) ?? null;

// ---- what fits before the next fixed event ----

/**
 * Minutes each task should take, as the agent would estimate them. Made up here (by title): the
 * snapshot has no estimates, and "what fits in the gap" needs some.
 */
export const ESTIMATE: Record<string, number> = {
  "Renew the parking permit": 15,
  "Return the library books": 20,
  "Pick up dry cleaning": 20,
  "Buy a birthday card for Sam": 15,
  "Send Noor the photos from the hike": 10,
  "Replace the hallway bulb": 10,
  "Book a sports massage": 10,
  "Fix the dripping bathroom tap": 45,
  "Finish unit 8 of the course": 40,
  "Write release notes for beta 0.4": 60,
  "Reply to the App Store review notes": 30,
  "Move the emergency fund to the high-yield account": 15,
  "Choose the kitchen paint colour": 30,
  "Draft the offline maps spec": 120,
  "Fix the crash on rotate in the map view": 90,
  "Invite twenty beta testers": 30,
  "Book flights to Lisbon": 30,
  "Find a physio for the left knee": 20,
};

/** Up to three open tasks whose estimate fits the gap: this context's first, then the soonest due. */
export function fitsBefore(s: State, here: Ctx, gapMs: number) {
  const mine = new Set(members(s, here).map((n) => n.id));
  return s.nodes
    .filter((n) => n.type === "task" && isOpen(n) && n.assignee !== "agent" && n.status !== "waiting" && ESTIMATE[n.title] !== undefined && ESTIMATE[n.title]! * 60_000 <= gapMs)
    .sort((a, b) => Number(!mine.has(a.id)) - Number(!mine.has(b.id)) || (a.due_at ?? "9999").localeCompare(b.due_at ?? "9999") || a.title.localeCompare(b.title))
    .slice(0, 3)
    .map((n) => ({ node: n, minutes: ESTIMATE[n.title]!, home: homeOf(s, n), here: mine.has(n.id) }));
}

// ---- what waits on him elsewhere ----

export interface Waiting {
  id: string;
  node: Node;
  kind: "flag" | "due";
  note: string;
  /** Laid over a real task for the crowded state; Seen on it is kept in the direction. */
  madeUp: boolean;
  home: Ctx;
}

/** Notes for the crowded state, laid over open tasks (by title) so twelve wait. Nothing is written. */
const CROWD: Record<string, string> = {
  "Book flights to Lisbon": "Fares went up 8% this week; the Thursday flight still has seats.",
  "Move the emergency fund to the high-yield account": "Two days late, and the transfer takes three business days.",
  "Fix the crash on rotate in the map view": "Three beta testers hit it yesterday; the trace is in the thread.",
  "Choose the kitchen paint colour": "The painter has to order by Monday.",
  "Write release notes for beta 0.4": "Beta 0.4 builds tonight and the notes are half done.",
  "Return the library books": "Due tomorrow; the branch closes at 18:00.",
  "Book a sports massage": "Sunday is the long run; Thursday has openings.",
  "Renew the developer account": "It lapses 30 Nov, and the beta stops installing after that.",
  "Invite twenty beta testers": "Lena has the list ready and asked whether to send it.",
  "Send Noor the photos from the hike": "Noor asked for them again yesterday.",
};

/** Flags first (oldest first), then tasks due today or late; none of them in `here`. */
export function waitingElsewhere(s: State, here: Ctx, today: string, crowded: boolean, seen: string[]): Waiting[] {
  const inHere = new Set(members(s, here).map((n) => n.id));
  if (here.node) inHere.add(here.node.id);
  const flags = s.nodes.filter((n) => n.flagged_at && isOpen(n)).sort((a, b) => a.flagged_at!.localeCompare(b.flagged_at!));
  const out: Waiting[] = flags.map((n) => ({ id: n.id, node: n, kind: "flag", note: n.flag_note ?? "", madeUp: false, home: homeOf(s, n) }));
  if (crowded) {
    for (const n of s.nodes) {
      if (n.title in CROWD && isOpen(n) && !n.flagged_at && !seen.includes(n.id)) out.push({ id: n.id, node: n, kind: "flag", note: CROWD[n.title]!, madeUp: true, home: homeOf(s, n) });
    }
  }
  const flagged = new Set(out.map((w) => w.id));
  const due = s.nodes
    .filter((n) => n.type === "task" && isOpen(n) && n.due_local && n.due_local <= today && !flagged.has(n.id))
    .sort((a, b) => a.due_local!.localeCompare(b.due_local!) || a.title.localeCompare(b.title));
  out.push(...due.map((n) => ({ id: n.id, node: n, kind: "due" as const, note: "", madeUp: false, home: homeOf(s, n) })));
  return out.filter((w) => !inHere.has(w.id));
}

/** Time today nobody has named, and the agent's guesses: the time that belongs to no context yet. */
export function unnamedTime(s: State) {
  const closed = s.blocks.filter((b) => b.t_end);
  const bare = closed.filter((b) => !b.intent_id && !b.committed);
  const guesses = closed.filter((b) => b.proposal && !b.committed);
  const ms = (xs: BlockView[]) => xs.reduce((a, b) => a + Date.parse(b.t_end!) - Date.parse(b.t_start), 0);
  return { bare, guesses, bareMs: ms(bare), guessMs: ms(guesses) };
}

/** A block's name as he would say it. */
export const blockName = (b: BlockView) => b.intent ?? b.title ?? (b.origin === "auto_activity" ? "Phone" : b.origin === "place" ? "Somewhere" : "No label");
export const blockSource = (b: BlockView) =>
  b.origin === "manual" ? "Timer" : b.origin === "calendar" ? (b.title ?? "Calendar") : b.origin === "auto_activity" ? (b.title ?? "Phone") : b.origin === "place" ? (b.title ?? "A place") : "Logged";

/** The contexts he can pick from (to switch the timer, or relabel time), recent first. */
export function labelChoices(s: State) {
  const recent = s.recent.map((r) => s.nodes.find((n) => n.id === r.id) ?? null).filter((n): n is Node => !!n && isOpen(n));
  const open = s.nodes.filter((n) => isOpen(n) && n.type !== "metric");
  return { recent, open, system: s.system };
}

/** Resolve a context from a link. */
export function fromLink(s: State, kind: string, id: string | undefined): Ctx | null {
  if (kind === "loose") return LOOSE;
  if (!id) return null;
  if (kind === "person") {
    const p = s.people.find((x) => x.name === id);
    return p ? personCtx(p) : null;
  }
  if (kind === "slot") {
    const e = s.day.planned.find((x) => x.title === id || x.event_id === id);
    return e ? slotCtx(e) : null;
  }
  const n = s.nodes.find((x) => x.title === id && (kind === "goal" ? x.type === "goal" : x.type === "project"));
  return n ? nodeCtx(n) : null;
}

/** Where a context opens. */
export function ctxPath(c: Ctx): { path: string; params: Record<string, string> } {
  switch (c.kind) {
    case "goal": return { path: "/goals", params: { id: c.linkId } };
    case "person": return { path: "/people", params: { id: c.linkId } };
    case "project": return { path: "/project", params: { id: c.linkId } };
    case "slot": return { path: "/slot", params: { id: c.linkId } };
    case "loose": return { path: "/loose", params: {} };
    default: return { path: "/", params: {} };
  }
}

export const sameCtx = (a: Ctx | null | undefined, b: Ctx | null | undefined) => !!a && !!b && a.kind === b.kind && a.key === b.key;
