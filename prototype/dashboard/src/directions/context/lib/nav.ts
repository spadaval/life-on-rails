// Links inside Context. The shared href() keeps only the shell's parameters; this one also keeps
// `state` (stale, crowded), so leaving a page never fakes LoR coming back or the queue clearing.
import { href as shellHref, navigate as shellNavigate, parse, setParams as shellSetParams, type Params } from "../../../shell/nav.js";

const keep = (): Params => {
  const st = parse(location.hash).params.state;
  return st ? { state: st } : {};
};

export const href = (path: string, params: Params = {}) => shellHref(path, { ...keep(), ...params });
export const navigate = (path: string, params: Params = {}, opts: { replace?: boolean } = {}) => shellNavigate(path, { ...keep(), ...params }, opts);
export const setParams = (patch: Params, opts?: { replace?: boolean }) => shellSetParams(patch, opts);
