// /lab/: the design lab's static files (galleries, the launcher lab, the dashboard
// prototype), served read-only from LOR_LAB_DIR on both of lor-web's UI listeners
// (lor-zwfd). scripts/lab/publish.sh is what fills the directory.
//
// A lab page shares the dashboard's origin and its identity check, so every response
// carries a policy that keeps it off LoR's data: it may connect to and frame only
// /lab/ on this origin, and submit no form. Nothing under /lab/ ever reaches the app:
// a miss here is this handler's own 404, never next().
import { createReadStream, realpathSync, statSync } from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";

const TYPES = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".map": "application/json; charset=utf-8",
  ".txt": "text/plain; charset=utf-8",
  ".md": "text/plain; charset=utf-8",
  ".wasm": "application/wasm",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".webp": "image/webp",
  ".gif": "image/gif",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".mp4": "video/mp4",
  ".webm": "video/webm",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".otf": "font/otf",
};

/**
 * A bundler's content-hashed name: `index-B3x9aZ_k.js` (Vite: mixed case and digits), or
 * `homecore.4f2a9c1e.wasm` and Kotlin's `0d7a432110fc5479b82d.wasm` (hex), the whole stem or
 * set off by `-` or `.` before the extension. Such a file never changes under its name, so
 * it is cached for a year. Anything else, including hand-named files like `today@1100.png`
 * or `home-version2.png`, is revalidated each time: a hash missed here only costs a 304, a
 * name taken for a hash would go stale for a year.
 */
export const isHashed = (name) => {
  const m = /(?:^|[.-])([A-Za-z0-9_]{8,})\.[A-Za-z0-9]+$/.exec(name);
  if (!m) return false;
  const h = m[1];
  return (/^[0-9a-f]+$/.test(h) && /\d/.test(h) && /[a-f]/.test(h)) || (/[A-Z]/.test(h) && /[a-z]/.test(h) && /\d/.test(h));
};

/** The policy on every /lab/ response, for the origin the browser used. */
export const labPolicy = (origin) =>
  [`connect-src ${origin}/lab/`, `frame-src ${origin}/lab/`, `worker-src ${origin}/lab/ blob:`, "object-src 'none'", "form-action 'none'", "base-uri 'none'"].join("; ");

const isLab = (pathname) => pathname === "/lab" || pathname.startsWith("/lab/");

/**
 * srvx middleware serving `dir` at /lab/. With no `dir`, /lab/ is a 404. Any other path
 * goes on to `next`.
 */
export function labStatic(dir) {
  return (request, next) => {
    const url = new URL(request.url);
    if (!isLab(url.pathname)) return next();
    const headers = { "content-security-policy": labPolicy(url.origin), "x-content-type-options": "nosniff" };
    const answer = (status, body, extra = {}) =>
      new Response(request.method === "HEAD" ? null : body, { status, headers: { ...headers, "content-type": "text/plain; charset=utf-8", ...extra } });
    if (request.method !== "GET" && request.method !== "HEAD") return answer(405, "method not allowed\n", { allow: "GET, HEAD" });
    // Resolved per request: a publish swaps the directory, and it may not exist yet.
    const real = dir ? (() => { try { return realpathSync(dir); } catch { return null; } })() : null;
    if (!real) return answer(404, "not found\n");
    if (url.pathname === "/lab") return answer(308, "", { location: `/lab/${url.search}` });

    let rel;
    try {
      rel = decodeURIComponent(url.pathname.slice("/lab/".length));
    } catch {
      return answer(400, "bad path\n");
    }
    if (rel.includes("\0")) return answer(400, "bad path\n");
    const file = resolveInside(real, rel);
    if (!file) return answer(404, "not found\n");

    let st = statSync(file, { throwIfNoEntry: false });
    let target = file;
    if (st?.isDirectory()) {
      // A directory without its slash would break every relative link in its index.
      if (!url.pathname.endsWith("/")) return answer(308, "", { location: `${url.pathname}/${url.search}` });
      target = path.join(file, "index.html");
      st = statSync(target, { throwIfNoEntry: false });
    }
    if (!st?.isFile()) return answer(404, "not found\n");

    const etag = `W/"${st.size.toString(16)}-${Math.floor(st.mtimeMs).toString(16)}"`;
    const out = {
      ...headers,
      "content-type": TYPES[path.extname(target).toLowerCase()] ?? "application/octet-stream",
      "cache-control": isHashed(path.basename(target)) ? "public, max-age=31536000, immutable" : "no-cache",
      etag,
      "last-modified": st.mtime.toUTCString(),
    };
    if (request.headers.get("if-none-match") === etag) return new Response(null, { status: 304, headers: out });
    out["content-length"] = String(st.size);
    if (request.method === "HEAD") return new Response(null, { status: 200, headers: out });
    return new Response(Readable.toWeb(createReadStream(target)), { status: 200, headers: out });
  };
}

/**
 * `rel` under `root`, or null when it would land outside: `..`, an absolute path, or a
 * symlink that points out of the directory.
 */
export function resolveInside(root, rel) {
  const full = path.resolve(root, `.${path.sep}${rel}`);
  if (full !== root && !full.startsWith(root + path.sep)) return null;
  let actual;
  try {
    actual = realpathSync(full);
  } catch {
    return full; // missing: the caller's stat says so
  }
  return actual === root || actual.startsWith(root + path.sep) ? actual : null;
}
