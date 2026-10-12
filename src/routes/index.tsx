import { createFileRoute, Link } from "@tanstack/react-router";
import { getToday } from "../server/functions/time.js";

export const Route = createFileRoute("/")({
  loader: () => getToday({ data: {} }),
  component: Today,
});

function Today() {
  const data = Route.useLoaderData();
  return (
    <main className="mx-auto max-w-2xl p-6">
      <h1 className="text-3xl font-bold">Today</h1>
      <p className="mt-2 text-sm text-neutral-500">
        Running block, planned vs actual, and confirm-day controls land in PR 5.
        Pending proposals: {data.pendingProposals}
      </p>
      <nav className="mt-4 flex gap-4 text-sm">
        <Link to="/graph">Graph</Link>
        <Link to="/memory">Memory</Link>
        <Link to="/chat">Chat</Link>
        <Link to="/capture">Capture</Link>
      </nav>
    </main>
  );
}
