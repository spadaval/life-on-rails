import { createFileRoute, useRouter } from "@tanstack/react-router";
import { Ago } from "../components/Ago.js";
import { PageHead, Panel } from "../components/ui.js";
import { getCaptureStatus } from "../server/functions/cockpit.js";
import { GlobalActions } from "./__root.js";

export const Route = createFileRoute("/status")({
  loader: () => getCaptureStatus(),
  component: Status,
});

const STATE: Record<string, { text: string; className: string }> = {
  ok: { text: "ok", className: "text-mute" },
  stale: { text: "late", className: "text-txt font-medium" },
  error: { text: "failing", className: "text-bad" },
  never: { text: "not run yet", className: "text-dim" },
};

const SOURCE: Record<string, string> = { android: "Phone", gmail: "Mail", calendar: "Calendar", agent: "Agent", manual: "You", google_tasks: "Google Tasks", system: "System" };

function Status() {
  const status = Route.useLoaderData();
  const router = useRouter();
  const unwell = status.jobs.filter((j) => j.state === "stale" || j.state === "error");

  return (
    <div className="space-y-[22px] pb-4">
      <PageHead title="Status" sub={unwell.length === 0 && status.db ? "every sense reporting" : `${unwell.length} ${unwell.length === 1 ? "job needs" : "jobs need"} a look`}>
        <button className="btn btn-sm" onClick={() => router.invalidate()}>
          Refresh
        </button>
        <GlobalActions />
      </PageHead>

      <Panel>
        <dl className="grid gap-x-10 gap-y-3 text-[14px] md:grid-cols-[auto_1fr_auto_1fr_auto_1fr]">
          <dt className="cond text-[13px] font-semibold text-mute">Database</dt>
          <dd className={status.db ? "" : "text-bad"}>{status.db ? "Up, on this box's socket" : "Down"}</dd>
          <dt className="cond text-[13px] font-semibold text-mute">Google</dt>
          <dd>
            {status.google.state === "ok" ? (
              <>
                Signed in
                <span className="block text-[13px] text-dim">{status.google.scopes.map((x) => x.replace("https://www.googleapis.com/auth/", "")).join(" · ")}</span>
              </>
            ) : (
              <span className="text-bad">
                {status.google.state === "not-configured" ? "Not set up" : "Sign-in needed"}
                <span className="block text-[13px] text-mute">
                  {status.google.detail}. Run <code className="bg-raised px-1">pnpm google:auth</code> in the repo.
                </span>
              </span>
            )}
          </dd>
          <dt className="cond text-[13px] font-semibold text-mute">Mail</dt>
          <dd>
            {status.mail.configured ? (
              <>
                {status.mail.queued_threads} waiting for the mail run
                <span className="block text-[13px] text-dim">
                  Checked <Ago iso={status.mail.last_check_at} />
                </span>
              </>
            ) : (
              <span className="text-mute">
                Not set up. Run <code className="bg-raised px-1">pnpm google:auth --mail</code> in the repo.
              </span>
            )}
          </dd>
        </dl>
      </Panel>

      <div className="grid items-start gap-[22px] xl:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
        <Panel title="Jobs" aside={`${status.jobs.length}`}>
          <table className="w-full text-left text-[14px]">
            <thead>
              <tr className="cond border-b border-line text-[12.5px] text-dim">
                <th className="py-2 pr-4 font-medium">Job</th>
                <th className="py-2 pr-4 font-medium">Runs</th>
                <th className="py-2 pr-4 font-medium">State</th>
                <th className="py-2 text-right font-medium">Last ok</th>
              </tr>
            </thead>
            <tbody>
              {status.jobs.map((j) => (
                <tr key={j.name} className="border-b border-line align-top last:border-b-0">
                  <td className="py-2 pr-4 font-medium">
                    {j.name}
                    {j.lastError ? <span className="block text-[13px] font-normal text-bad">{j.lastError}</span> : null}
                  </td>
                  <td className="py-2 pr-4 text-mute">{j.schedule}</td>
                  <td className={`py-2 pr-4 ${STATE[j.state]!.className}`}>{STATE[j.state]!.text}</td>
                  <td className="py-2 text-right text-mute">
                    <Ago iso={j.lastOk} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        <Panel title="Sources" aside="what has arrived">
          {status.sources.length === 0 ? <p className="text-dim">Nothing has arrived yet.</p> : null}
          <ul>
            {status.sources.map((s) => (
              <li key={s.source} className="grid grid-cols-[1fr_auto_90px] items-baseline gap-4 border-b border-line py-2 last:border-b-0">
                <span className="font-medium">{SOURCE[s.source] ?? s.source}</span>
                <span className="text-right text-mute">{s.count.toLocaleString("en-US")}</span>
                <span className="text-right text-[13.5px] text-mute">
                  <Ago iso={s.lastArrival} />
                </span>
              </li>
            ))}
          </ul>
        </Panel>
      </div>
    </div>
  );
}
