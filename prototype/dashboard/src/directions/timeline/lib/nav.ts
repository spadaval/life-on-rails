// The shell's links, keeping this direction's own `state` (stale, crowded) across its pages:
// the shell keeps only scene, still and shell, and leaving a page must not fake LoR answering.
import { href as shellHref, navigate as shellNavigate, parse, type Params } from "../../../shell/nav.js";

export { setParams } from "../../../shell/nav.js";

const keep = (): Params => {
  const state = parse(location.hash).params.state;
  return state ? { state } : {};
};

export const href = (path: string, params: Params = {}) => shellHref(path, { ...keep(), ...params });
export const navigate = (path: string, params: Params = {}, opts: { replace?: boolean } = {}) => shellNavigate(path, { ...keep(), ...params }, opts);
