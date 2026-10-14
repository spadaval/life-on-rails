import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  component: Today,
});

function Today() {
  return (
    <div>
      <h1 className="text-3xl font-bold">Today</h1>
      <p className="muted mt-2">The timer and the day's timeline are not built yet.</p>
    </div>
  );
}
