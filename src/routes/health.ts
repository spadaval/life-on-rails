import { createFileRoute } from "@tanstack/react-router";
import { getStatus } from "../server/status.js";

// 200 whenever the process is listening. Stale sensors are reported in the
// body, not by failing: the cockpit is the alerting, not a restart loop.
export const Route = createFileRoute("/health")({
  server: {
    handlers: {
      GET: async () => Response.json(await getStatus()),
    },
  },
});
