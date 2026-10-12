import { createFileRoute } from "@tanstack/react-router";
import { getGraph } from "../server/functions/graph.js";

export const Route = createFileRoute("/graph")({
  loader: () => getGraph({ data: {} }),
  component: Graph,
});

function Graph() {
  const data = Route.useLoaderData();
  return (
    <main className="mx-auto max-w-2xl p-6">
      <h1 className="text-3xl font-bold">Graph</h1>
      <p className="mt-2 text-sm text-neutral-500">{data.nodes.length} intents, {data.links.length} links. Full CRUD lands in PR 3.</p>
    </main>
  );
}
