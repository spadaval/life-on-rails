import { type ReactNode, useEffect, useState } from "react";
import { Link, Outlet, createRootRoute, HeadContent, Scripts, useLocation, useNavigate, useRouter } from "@tanstack/react-router";
import { duration } from "../components/format.js";
import { getShellFn } from "../server/functions/shell.js";
import { timerFn } from "../server/functions/today.js";
import appCss from "../styles.css?url";

export const Route = createRootRoute({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { name: "theme-color", content: "#0c0d12" },
      { title: "Life on Rails" },
    ],
    links: [
      { rel: "stylesheet", href: appCss },
      { rel: "icon", type: "image/svg+xml", href: "/favicon.svg" },
    ],
  }),
  loader: () => getShellFn(),
  component: RootComponent,
  notFoundComponent: () => <p className="p-6 text-mute">Nothing here.</p>,
});

export type Shell = Awaited<ReturnType<typeof getShellFn>>;

const NAV = [
  { to: "/", label: "Today" },
  { to: "/tasks", label: "Tasks" },
  { to: "/graph", label: "Graph" },
  { to: "/memory", label: "Memory" },
  { to: "/capture", label: "Capture" },
] as const;

const MEMORY_TABS = [
  { tab: "pages", label: "Pages" },
  { tab: "people", label: "People" },
  { tab: "timeline", label: "Timeline" },
] as const;

function RootComponent() {
  const shell = Route.useLoaderData();
  const location = useLocation();
  const memoryTab = location.pathname === "/memory" ? ((location.search as { tab?: string }).tab ?? "pages") : null;
  const stamp = useStamp();

  return (
    <RootDocument>
      <div className="min-h-dvh md:grid md:grid-cols-[208px_1fr]">
        <aside className="hidden border-r border-frame md:sticky md:top-0 md:flex md:h-dvh md:flex-col md:py-5">
          <Link to="/" className="cond mx-[22px] text-[19px] font-semibold tracking-[0.1em] text-or">
            Life on Rails
          </Link>
          <p className="mx-[22px] mb-6 text-[13px] text-mute" suppressHydrationWarning>
            {stamp}
          </p>
          <nav className="flex flex-col gap-[3px]" aria-label="Main">
            {NAV.map((n) => (
              <div key={n.to}>
                <Link
                  to={n.to}
                  className="cond flex h-[34px] items-center justify-between px-[22px] text-[15px] font-semibold text-mute hover:text-txt"
                  activeProps={{ className: "!bg-or !text-[#1a0d02]" }}
                  activeOptions={{ exact: n.to === "/", includeSearch: false }}
                >
                  {n.label}
                  {n.to === "/tasks" && shell.openTasks > 0 ? <span className="font-sans text-[13px] font-medium normal-case opacity-80">{shell.openTasks}</span> : null}
                </Link>
                {n.to === "/memory"
                  ? MEMORY_TABS.map((t) => (
                      <Link
                        key={t.tab}
                        to="/memory"
                        search={{ tab: t.tab }}
                        className={`cond flex h-7 items-center pl-9 text-[13.5px] font-medium hover:text-txt ${memoryTab === t.tab ? "text-or" : "text-dim"}`}
                      >
                        {t.label}
                      </Link>
                    ))
                  : null}
              </div>
            ))}
          </nav>
          <Health unwell={shell.unwell} className="mt-auto border-t border-line px-[22px] pt-3.5" />
        </aside>

        <header className="flex items-center gap-1 overflow-x-auto border-b border-frame px-3 md:hidden">
          <Link to="/" className="cond mr-2 py-3 text-[15px] font-semibold tracking-[0.1em] text-or">
            LoR
          </Link>
          {NAV.map((n) => (
            <Link
              key={n.to}
              to={n.to}
              className="cond px-2.5 py-3 text-[14px] font-semibold whitespace-nowrap text-mute"
              activeProps={{ className: "!text-or" }}
              activeOptions={{ exact: n.to === "/", includeSearch: false }}
            >
              {n.label}
            </Link>
          ))}
        </header>

        <main className="min-w-0 px-4 pb-10 md:px-[30px]">
          <Outlet />
        </main>
      </div>
    </RootDocument>
  );
}

function useStamp(): string {
  const fmt = () => new Intl.DateTimeFormat("en-GB", { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date()).replace(",", " ·");
  const [s, setS] = useState(fmt);
  useEffect(() => {
    const id = setInterval(() => setS(fmt()), 20_000);
    return () => clearInterval(id);
  }, []);
  return s;
}

function Health({ unwell, className }: { unwell: Shell["unwell"]; className: string }) {
  const ok = unwell !== null && unwell.length === 0;
  return (
    <Link to="/capture" className={`block text-[13px] text-mute hover:text-txt ${className}`}>
      <span className={`mr-2 inline-block size-1.5 rounded-full align-[2px] ${ok ? "bg-or" : "bg-bad"}`} />
      {unwell === null ? "Database unreachable" : ok ? "All senses reporting" : `${unwell.length} ${unwell.length === 1 ? "job needs" : "jobs need"} a look`}
    </Link>
  );
}

/** Search and the timer, on the right of every page head. */
export function GlobalActions() {
  const shell = Route.useLoaderData();
  const router = useRouter();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [busy, setBusy] = useState(false);
  const now = useTick(shell.running ? 20_000 : null);
  const run = shell.running;

  return (
    <>
      <form
        className="hidden lg:block"
        onSubmit={(e) => {
          e.preventDefault();
          if (q.trim()) void navigate({ to: "/memory", search: { q: q.trim() } });
        }}
      >
        <input className="input w-[300px]" placeholder="Search pages, people, tasks" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search everything" />
      </form>
      {run ? (
        <Link to="/" className="flex items-center gap-2 border border-or/60 bg-or/10 px-3 py-1 text-[13.5px] hover:bg-or/15">
          <span className={`size-1.5 rounded-full bg-or ${run.paused_at ? "opacity-40" : ""}`} />
          <span className="max-w-[220px] truncate">{run.intent ?? run.title ?? "Timer"}</span>
          <span className="cond font-semibold text-or" suppressHydrationWarning>
            {duration(Math.max(0, ((run.paused_at ? new Date(run.paused_at).getTime() : now) - new Date(run.t_start).getTime()) / 1000))}
          </span>
        </Link>
      ) : (
        <button
          className="btn btn-primary"
          disabled={busy}
          onClick={async () => {
            setBusy(true);
            await timerFn({ data: { action: "start", intentId: null, title: null } }).catch(() => undefined);
            await router.invalidate();
            setBusy(false);
          }}
        >
          Start timer
        </button>
      )}
    </>
  );
}

function useTick(everyMs: number | null): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (everyMs === null) return;
    setNow(Date.now());
    const id = setInterval(() => setNow(Date.now()), everyMs);
    return () => clearInterval(id);
  }, [everyMs]);
  return now;
}

function RootDocument({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en">
      <head>
        <HeadContent />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}
