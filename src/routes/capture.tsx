import { createFileRoute } from "@tanstack/react-router";
import { getCaptureStatus } from "../server/functions/cockpit.js";

export const Route = createFileRoute("/capture")({
  loader: () => getCaptureStatus({ data: {} }),
  component: Capture,
});

function Capture() {
  const data = Route.useLoaderData();
  return (
    <main className="mx-auto max-w-2xl p-6">
      <h1 className="text-3xl font-bold">Capture</h1>
      <p className="mt-2 text-sm text-neutral-500">Activity: {data.activity}</p>
    </main>
  );
}
