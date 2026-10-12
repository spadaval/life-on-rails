import { createFileRoute } from "@tanstack/react-router";
import { IngestBody } from "../shared/ingest.js";

function isLoopback(req: Request): boolean {
  // Start exposes the remote address via headers in most presets; loopback
  // check is defense-in-depth — v1 binds 127.0.0.1 so non-loopback cannot arrive.
  const forwarded = req.headers.get("x-forwarded-for");
  if (forwarded) return forwarded.split(",")[0]!.trim() === "127.0.0.1";
  return true;
}

export const Route = createFileRoute("/ingest")({
  server: {
    handlers: {
      POST: async ({ request }) => {
        if (!isLoopback(request)) return new Response("forbidden", { status: 403 });
        const auth = request.headers.get("authorization") ?? "";
        if (auth !== `Bearer ${process.env.LOR_INGEST_TOKEN ?? ""}` || !process.env.LOR_INGEST_TOKEN) {
          return new Response("unauthorized", { status: 401 });
        }
        const body: unknown = await request.json().catch(() => null);
        const parsed = IngestBody.safeParse(body);
        if (!parsed.success) return Response.json({ error: "invalid envelope batch" }, { status: 400 });
        const activityEnabled = (process.env.LOR_ACTIVITY_ENABLED ?? "true") !== "false";
        const { UPSERT_OBSERVATION_SQL, classifyIngest } = await import("../server/capture/ingest.js");
        const { sql } = await import("../db/client.js");
        let stored = 0;
        let droppedDisabled = 0;
        const rejected: string[] = [];
        for (const e of parsed.data) {
          if (classifyIngest(e, activityEnabled) === "drop-disabled") {
            droppedDisabled++;
            continue;
          }
          try {
            await sql.unsafe(UPSERT_OBSERVATION_SQL, [
              e.id, e.t_start, e.t_end, e.device, e.source, e.app,
              e.calendar_id, e.title, e.url, JSON.stringify(e.payload), e.intent_hint,
            ]);
            stored++;
          } catch {
            rejected.push(e.id);
          }
        }
        if (droppedDisabled > 0 && stored === 0) {
          return Response.json({ stored, droppedDisabled, rejected }, { status: 202 });
        }
        return Response.json({ stored, droppedDisabled, rejected });
      },
    },
  },
});
