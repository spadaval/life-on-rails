#!/usr/bin/env node
// Production entry: the only thing that listens and the only thing that starts
// the scheduler. Run by lor-web.service as `node scripts/serve.mjs`.
//
// Two listeners, one process:
//   LOR_BIND:LOR_PORT        everything, no identity check. On-box use only.
//   127.0.0.1:LOR_SERVE_PORT UI and /ingest for the owner's other devices,
//                            published by `tailscale serve`. Every request must
//                            carry the owner's Tailscale login. /api/ is not routed.
//   LOR_TRMNL_BIND:LOR_TRMNL_PORT  the TRMNL X on the home network, when set: only
//                            the firmware's three calls and its image, keyed.
import { existsSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { serve } from "srvx";
import { serveStatic } from "srvx/static";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const envFile = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");
if (existsSync(envFile)) process.loadEnvFile(envFile);

const bundle = path.join(root, "dist/server/server.js");
if (!existsSync(bundle)) {
  console.error(`missing ${bundle}: run pnpm build`);
  process.exit(1);
}

process.env.LOR_RUN_JOBS = "1";
process.env.LOR_ROOT = root;
const { default: app } = await import(bundle);
// Database ping, seed, and scheduler start; exits non-zero if the database is down.
await globalThis.__lorReady;

const hostname = process.env.LOR_BIND || "127.0.0.1";
const port = Number(process.env.LOR_PORT || 3847);
const servePort = Number(process.env.LOR_SERVE_PORT || 3848);
const owner = (process.env.LOR_OWNER_LOGIN || "").trim().toLowerCase();

const assets = serveStatic({ dir: path.join(root, "dist/client") });
const fetch = (request) => app.fetch(request);

// /api/ is for processes on this box. The listener binds loopback by default;
// this holds even if LOR_BIND is ever widened by mistake.
const LOOPBACK = new Set(["127.0.0.1", "::1", "::ffff:127.0.0.1"]);
const apiFromLoopbackOnly = (request, next) => {
  if (new URL(request.url).pathname.startsWith("/api/") && !LOOPBACK.has(request.ip ?? "")) {
    return new Response("forbidden\n", { status: 403 });
  }
  return next();
};

serve({ hostname, port, fetch, middleware: [apiFromLoopbackOnly, assets], silent: true });
console.log(JSON.stringify({ ts: new Date().toISOString(), level: "info", msg: "listening", hostname, port }));

if (owner) {
  const ownerOnly = (request, next) => {
    const login = (request.headers.get("tailscale-user-login") || "").trim().toLowerCase();
    if (login !== owner) return new Response("forbidden\n", { status: 403 });
    if (new URL(request.url).pathname.startsWith("/api/")) return new Response("not found\n", { status: 404 });
    return next();
  };
  serve({ hostname: "127.0.0.1", port: servePort, fetch, middleware: [ownerOnly, assets], silent: true });
  console.log(JSON.stringify({ ts: new Date().toISOString(), level: "info", msg: "listening (owner only)", hostname: "127.0.0.1", port: servePort }));
}

// The wall display cannot join the tailnet. Its firmware calls /api/setup,
// /api/display and /api/log at the base URL it was given, then fetches the
// images and (when offered) the firmware build they name; nothing else is routed.
const trmnlBind = (process.env.LOR_TRMNL_BIND || "").trim();
if (trmnlBind) {
  const trmnlPort = Number(process.env.LOR_TRMNL_PORT || 3849);
  const FIRMWARE = { "/api/setup": "/trmnl/setup", "/api/display": "/trmnl/display", "/api/log": "/trmnl/log" };
  const trmnlFetch = async (request) => {
    const url = new URL(request.url);
    const to = FIRMWARE[url.pathname] ?? (/^\/trmnl\/(image|firmware)\//.test(url.pathname) ? url.pathname : null);
    if (!to) return new Response("not found\n", { status: 404 });
    url.pathname = to;
    const body = request.method === "POST" ? await request.text() : undefined;
    return app.fetch(new Request(url, { method: request.method, headers: request.headers, body }));
  };
  serve({ hostname: trmnlBind, port: trmnlPort, fetch: trmnlFetch, silent: true });
  console.log(JSON.stringify({ ts: new Date().toISOString(), level: "info", msg: "listening (trmnl)", hostname: trmnlBind, port: trmnlPort }));
}
