import { createFileRoute } from "@tanstack/react-router";

async function dbPing(): Promise<boolean> {
  try {
    const { sql } = await import("../db/client.js");
    await sql`SELECT 1`;
    return true;
  } catch {
    return false;
  }
}

export const Route = createFileRoute("/health")({
  server: {
    handlers: {
      GET: async () => {
        const db = await dbPing();
        // Liveness: process is up (we answered). Readiness details are
        // best-effort; sensors stale → ok:false, still HTTP 200.
        let lastCalendarPoll: string | null = null;
        let activity: "tracking" | "no_session" | "stale" | "error" | "disabled" = "disabled";
        if (db) {
          try {
            const { sql } = await import("../db/client.js");
            const poll = (await sql`SELECT finished_at FROM job_run
              WHERE name = 'calendar_poll' ORDER BY started_at DESC LIMIT 1`) as { finished_at: Date | null }[];
            lastCalendarPoll = poll[0]?.finished_at?.toISOString() ?? null;
            if (lastCalendarPoll && Date.now() - new Date(lastCalendarPoll).getTime() > 15 * 60_000) activity = "stale";
          } catch {
            activity = "error";
          }
        }
        return Response.json({
          ok: db && activity !== "stale" && activity !== "error",
          db,
          gog: "unknown",
          activity,
          last_calendar_poll: lastCalendarPoll,
          last_overnight: null,
        });
      },
    },
  },
});
