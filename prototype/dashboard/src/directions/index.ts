// The versions the dropdown offers. A direction is one folder under directions/ with its own
// tokens, components and screens; it shares only src/data (the snapshot and the store) and
// src/shell (the route and the Version control) with the others. See ../../README.md.
import type { ComponentType } from "react";
import type { Route } from "../shell/nav.js";

export interface Direction {
  /** Its stylesheets, as `import css from "./styles.css?url"`: linked only while it shows. */
  css: string[];
  App: ComponentType<{ route: Route }>;
}

export interface Version {
  id: string;
  name: string;
  load: () => Promise<{ default: Direction }>;
}

export const VERSIONS: Version[] = [
  { id: "current", name: "Current (shipped)", load: () => import("./current/index.js") },
  { id: "queue", name: "Queue: one list of what wants you", load: () => import("./queue/index.js") },
  // The starting point a direction item copies. Dev only: it is not a concept.
  ...(import.meta.env.DEV ? [{ id: "template", name: "Template (dev only)", load: () => import("./template/index.js") }] : []),
];

export const DEFAULT_VERSION = "current";
