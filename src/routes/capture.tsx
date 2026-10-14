import { createFileRoute, useRouter } from "@tanstack/react-router";
import { getCaptureStatus } from "../server/functions/cockpit.js";
import { ago } from "../components/format.js";

export const Route = createFileRoute("/capture")({
  loader: () => getCaptureStatus(),
  component: Capture,
});

const STATE_CLASS: Record<string, string> = {
  ok: "text-rail",
  stale: "text-warn",
  error: "text-bad",
  never: "muted",
};

function Capture() {
  const status = Route.useLoaderData();
  const router = useRouter();
  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Capture</h1>
        <button className="btn" onClick={() => router.invalidate()}>
          Refresh
        </button>
      </header>

      <section className="card">
        <h2 className="text-lg font-semibold">Database</h2>
        <p className={status.db ? "text-rail" : "text-bad"}>{status.db ? "up" : "down"}</p>
      </section>

      <section className="card">
        <h2 className="text-lg font-semibold">Jobs</h2>
        <ul className="mt-2 divide-y divide-black/10 dark:divide-white/10">
          {status.jobs.map((j) => (
            <li key={j.name} className="flex flex-wrap items-baseline justify-between gap-x-4 py-2">
              <span className="font-medium">{j.name}</span>
              <span className="muted text-sm">{j.schedule}</span>
              <span className={`${STATE_CLASS[j.state]} text-sm font-semibold`}>{j.state}</span>
              <span className="text-sm">last ok {ago(j.lastOk)}</span>
              {j.lastError ? <span className="basis-full text-sm text-bad">{j.lastError}</span> : null}
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h2 className="text-lg font-semibold">Sources</h2>
        {status.sources.length === 0 ? (
          <p className="muted">Nothing has arrived yet.</p>
        ) : (
          <ul className="mt-2 divide-y divide-black/10 dark:divide-white/10">
            {status.sources.map((s) => (
              <li key={s.source} className="flex justify-between py-2">
                <span className="font-medium">{s.source}</span>
                <span className="text-sm">
                  {s.count} · last {ago(s.lastArrival)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
