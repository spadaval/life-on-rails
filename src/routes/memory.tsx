import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/memory")({
  component: Memory,
});

function Memory() {
  return (
    <main className="mx-auto max-w-2xl p-6">
      <h1 className="text-3xl font-bold">Memory</h1>
      <p className="mt-2 text-sm text-neutral-500">Observation search + weekly review land in PR 9.</p>
    </main>
  );
}
