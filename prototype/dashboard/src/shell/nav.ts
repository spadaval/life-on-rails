// Where the prototype is, all in the URL hash so a link names a whole state and nothing ever
// asks a server: #/<version>/<path>?<params>, as in #/current/tasks?id=Book%20flights.
//
// The shell owns three parameters, kept across every link: `scene` (the data and clock, see
// src/data/store.ts SCENES), `still=1` (the clock does not move: for screenshots) and `shell=0`
// (hides the Version control: for screenshots). Everything else belongs to the direction.
import { useSyncExternalStore } from "react";

export const STICKY = ["scene", "still", "shell"] as const;

export interface Route {
  version: string;
  /** The direction's own path, always starting with "/": "/", "/tasks". */
  path: string;
  /** The direction's parameters, without the shell's. */
  params: Record<string, string>;
  /** The shell's parameters. */
  sticky: Partial<Record<(typeof STICKY)[number], string>>;
}

export function parse(hash: string): Route {
  const body = hash.replace(/^#/, "");
  const [pathPart = "", query = ""] = body.split("?");
  const parts = pathPart.split("/").filter(Boolean).map(decodeURIComponent);
  const version = parts[0] ?? "";
  const path = `/${parts.slice(1).join("/")}`;
  const params: Record<string, string> = {};
  const sticky: Route["sticky"] = {};
  for (const [k, v] of new URLSearchParams(query)) {
    if ((STICKY as readonly string[]).includes(k)) sticky[k as (typeof STICKY)[number]] = v;
    else params[k] = v;
  }
  return { version, path, params, sticky };
}

export function format(r: { version: string; path: string; params?: Record<string, string | number | boolean | null | undefined>; sticky?: Route["sticky"] }): string {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(r.params ?? {})) if (v !== undefined && v !== null && v !== false && v !== "") q.set(k, String(v));
  for (const [k, v] of Object.entries(r.sticky ?? {})) if (v) q.set(k, v);
  const path = r.path.startsWith("/") ? r.path : `/${r.path}`;
  const qs = q.toString().replace(/\+/g, "%20");
  return `#/${encodeURIComponent(r.version)}${path}${qs ? `?${qs}` : ""}`;
}

const read = () => location.hash;
const subscribe = (l: () => void) => {
  window.addEventListener("hashchange", l);
  return () => window.removeEventListener("hashchange", l);
};

/** The route, re-rendering when the hash changes. */
export function useRoute(): Route {
  return parse(useSyncExternalStore(subscribe, read, read));
}

export type Params = Record<string, string | number | boolean | null | undefined>;

/** A link within the version showing now, keeping the shell's parameters: `href("/tasks", { id })`. */
export function href(path: string, params: Params = {}): string {
  const cur = parse(location.hash);
  return format({ version: cur.version, path, params, sticky: cur.sticky });
}

/** Go somewhere within the version showing now. `replace` changes the entry instead of adding one. */
export function navigate(path: string, params: Params = {}, opts: { replace?: boolean } = {}): void {
  const next = href(path, params);
  if (next === location.hash) return;
  if (opts.replace) history.replaceState(null, "", next);
  else history.pushState(null, "", next);
  window.dispatchEvent(new HashChangeEvent("hashchange"));
}

/** Change some of this page's parameters (undefined removes one), keeping the path. */
export function setParams(patch: Params, opts: { replace?: boolean } = { replace: true }): void {
  const cur = parse(location.hash);
  navigate(cur.path, { ...cur.params, ...patch }, opts);
}

/** Switch version, keeping the path, the parameters and the scene. */
export function switchVersion(version: string): void {
  const cur = parse(location.hash);
  location.hash = format({ version, path: cur.path, params: cur.params, sticky: cur.sticky });
}

/** Change one of the shell's own parameters. */
export function setSticky(patch: Route["sticky"]): void {
  const cur = parse(location.hash);
  location.hash = format({ version: cur.version, path: cur.path, params: cur.params, sticky: { ...cur.sticky, ...patch } });
}
