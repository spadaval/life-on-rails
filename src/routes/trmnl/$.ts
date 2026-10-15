import { createFileRoute } from "@tanstack/react-router";
import { errorMessage, log } from "../../server/log.js";
import { trmnlDisplay, trmnlImage, trmnlLog, trmnlSetup } from "../../server/trmnl/device.js";

async function handle(request: Request): Promise<Response> {
  const path = new URL(request.url).pathname.replace(/^\/trmnl/, "");
  try {
    if (request.method === "GET" && path === "/setup") return await trmnlSetup(request);
    if (request.method === "GET" && path === "/display") return await trmnlDisplay(request);
    if (request.method === "POST" && path === "/log") return await trmnlLog(request);
    const image = /^\/image\/([0-9a-f-]{36}|setup)\.png$/.exec(path);
    if (request.method === "GET" && image) return trmnlImage(request, image[1]!);
    return new Response("not found\n", { status: 404 });
  } catch (err) {
    log("error", "trmnl endpoint failed", { path, err: errorMessage(err) });
    return new Response(JSON.stringify({ status: 500, error: errorMessage(err) }), { status: 500, headers: { "content-type": "application/json" } });
  }
}

export const Route = createFileRoute("/trmnl/$")({
  server: { handlers: { GET: ({ request }) => handle(request), POST: ({ request }) => handle(request) } },
});
