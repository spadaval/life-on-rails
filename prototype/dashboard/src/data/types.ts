// The shapes of the snapshot, taken from LoR's own read paths. Types only: nothing from src/
// reaches the browser bundle.
import type { GraphNode } from "../../../../src/server/graph.js";
import type { BlockView, PlannedEvent, TodayView } from "../../../../src/server/today.js";
import type { Person } from "../../../../src/server/people.js";
import type { EntityView } from "../../../../src/server/memory/entities.js";
import type { PageListItem } from "../../../../src/server/memory/pages.js";

export type { BlockView, PlannedEvent, Person, EntityView };

/** A goal, project, habit, task or metric, as Goals and Tasks read it, with when it was last done. */
export type Node = GraphNode & { done_at: string | null };
export type IntentType = Node["type"];
export type IntentStatus = Node["status"];

/** The day as Today reads it, without its blocks: the store keeps those as `blocks`. */
export type Day = Omit<TodayView, "actual" | "running">;

export interface Hub {
  entity: EntityView;
  page: { slug: string; title: string; body: string; via: string; written_at: string } | null;
}

export type Page = PageListItem & { body: string };

export interface SceneData {
  at: string;
  today: TodayView;
  recent: { id: string; title: string }[];
  brief: string | null;
  unwell: string[] | null;
}

export interface Snapshot {
  about: string;
  day: string;
  timezone: string;
  scenes: { afternoon: SceneData; morning: SceneData };
  nodes: Node[];
  habitDone: { intent_id: string; t: string }[];
  system: { id: string; title: string }[];
  entities: { id: string; name: string }[];
  people: Person[];
  peopleLists: Record<string, string[]>;
  hubs: Record<string, Hub>;
  pages: Page[];
}
