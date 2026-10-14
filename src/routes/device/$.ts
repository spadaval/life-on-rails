import { createFileRoute } from "@tanstack/react-router";
import { ZodError } from "zod";
import { deviceNotifications, deviceState, deviceTimer, ingest } from "../../server/device.js";
import { BadInput, NotFound, Refused } from "../../server/errors.js";
import { errorMessage, log } from "../../server/log.js";

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

/** The Tailscale login Serve attached, or "this box" for a request on the loopback listener. */
const loginOf = (request: Request) => request.headers.get("tailscale-user-login") ?? "this box";

async function handle(request: Request): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/device/, "");
  try {
    if (request.method === "GET" && path === "/state") return json(200, await deviceState(loginOf(request), url.searchParams.get("device")));
    if (request.method === "POST" && path === "/ingest") return json(200, await ingest(await request.json()));
    if (request.method === "POST" && path === "/notifications") return json(200, await deviceNotifications());
    if (request.method === "POST" && path === "/timer") return json(200, await deviceTimer(await request.json(), loginOf(request)));
    return json(404, { error: { code: "no-such-endpoint", message: `${request.method} ${url.pathname}` } });
  } catch (err) {
    if (err instanceof BadInput || err instanceof ZodError || err instanceof SyntaxError) {
      return json(400, { error: { code: "bad-input", message: err instanceof ZodError ? err.issues.map((i) => i.message).join("; ") : err.message } });
    }
    if (err instanceof NotFound) return json(404, { error: { code: "not-found", message: err.message } });
    if (err instanceof Refused) return json(409, { error: { code: err.code, message: err.message } });
    log("error", "device endpoint failed", { path, err: errorMessage(err) });
    return json(500, { error: { code: "server-error", message: errorMessage(err) } });
  }
}

export const Route = createFileRoute("/device/$")({
  server: { handlers: { GET: ({ request }) => handle(request), POST: ({ request }) => handle(request) } },
});
