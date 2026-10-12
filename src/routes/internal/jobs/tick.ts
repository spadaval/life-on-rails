import { createFileRoute } from "@tanstack/react-router";

// POST /internal/jobs/tick — systemd watchdog pokes due jobs when
// calendar_poll is stale. Bearer job token, loopback only. The advisory lock
// in jobs/scheduler.ts makes concurrent triggers exit `skipped`.
export const Route = createFileRoute("/internal/jobs/tick")({
  server: {
    handlers: {
      POST: async ({ request }) => {
        const auth = request.headers.get("authorization") ?? "";
        if (auth !== `Bearer ${process.env.LOR_JOB_TOKEN ?? ""}` || !process.env.LOR_JOB_TOKEN) {
          return new Response("unauthorized", { status: 401 });
        }
        // Job execution wiring lands with the lor-web scheduler (PR 4/5);
        // this endpoint is the stable contract (auth + shape) for the watchdog.
        return Response.json({ ok: true, ran: [], note: "scheduler wiring lands PR 4/5" });
      },
    },
  },
});
