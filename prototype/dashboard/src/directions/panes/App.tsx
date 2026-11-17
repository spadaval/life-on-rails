// Panes: nothing leaves the screen (docs/design/lab/brief.md, "3. Panes"). A quiet nav rail with
// the one field that finds, adds and asks, and the running timer's chip; then on every page a
// list pane and a detail pane (panes.tsx). Below 720px the rail becomes a top bar with the field,
// the chip and the pages as tabs.
//
// Its own parameters, kept on every link (lib.ts phref): `state=stale` (LoR not answering, its
// last data shown) and `state=crowded` (twelve flags waiting). Per page, what is open: `id=` (a
// task, goal or person by title; a note by slug), and on Today `block=HH:MM`, `timer=1`,
// `event=<title>` or `brief=1`. `sheet=full` raises the phone-width sheet to full height,
// `switch=1` opens the timer's Switch picker, `find=` and `add=` fill the field.
import { openTasks } from "../../data/derive.js";
import { useNow, useStore } from "../../data/store.js";
import type { Route } from "../../shell/nav.js";
import { dueNow, flags, hm, modeOf, phref, setHere, useFit, useLocal } from "./lib.js";
import { Omnibox } from "./omnibox.js";
import { Goals } from "./screens/Goals.js";
import { Notes } from "./screens/Notes.js";
import { People } from "./screens/People.js";
import { Tasks } from "./screens/Tasks.js";
import { Today } from "./screens/Today.js";
import { TimerChip } from "./timer.js";
import { Button, cx, Icon, type IconName, ToastView } from "./ui.js";

const PAGES: [string, string, IconName][] = [
  ["/", "Today", "today"],
  ["/tasks", "Tasks", "tasks"],
  ["/goals", "Goals", "goals"],
  ["/people", "People", "people"],
  ["/notes", "Notes", "notes"],
];

export function App({ route }: { route: Route }) {
  const s = useStore();
  const fit = useFit();
  const mode = modeOf(route.params);
  const stale = mode === "stale";
  const page = PAGES.some(([p]) => p === route.path) ? route.path : "/";
  const Screen = { "/": Today, "/tasks": Tasks, "/goals": Goals, "/people": People, "/notes": Notes }[page]!;
  const fl = flags(s, mode, useLocal());
  const counts: Record<string, number | null> = {
    "/": fl.length + dueNow(s, new Set(fl.map((f) => f.id))).length || null,
    "/tasks": openTasks(s).length,
  };
  const field = <Omnibox key={`${route.params.find ?? ""}|${route.params.add ?? ""}`} initial={route.params.find ?? route.params.add} initialMode={route.params.add !== undefined ? "add" : undefined} stale={stale} />;
  const chip = <TimerChip stale={stale} initial={route.params.switch === "1" && page !== "/" ? "switch" : undefined} />;

  if (fit === "narrow") {
    return (
      <div className={cx("p-app", "is-narrow", stale && "is-stale")}>
        <header className="p-top">
          <div className="p-top__row">
            {field}
            {page !== "/" ? chip : null}
          </div>
          <nav className="p-tabs" aria-label="Pages">
            {PAGES.map(([p, name]) => (
              <a key={p} className={cx("p-tab", page === p && "is-on")} aria-current={page === p ? "page" : undefined} href={phref(p)}>
                {name}
                {counts[p] ? <span className="p-count">{counts[p]}</span> : null}
              </a>
            ))}
          </nav>
        </header>
        {stale ? <Stale /> : null}
        <main className="p-main">
          <Screen route={route} />
        </main>
        <ToastView />
      </div>
    );
  }

  return (
    <div className={cx("p-app", stale && "is-stale")}>
      <aside className="p-rail" aria-label="LoR">
        <a className="p-mark" href={phref("/")}>
          <span className="p-mark__dot" aria-hidden="true" />
          Life on Rails
        </a>
        {field}
        <nav className="p-nav" aria-label="Pages">
          {PAGES.map(([p, name, icon]) => (
            <a key={p} className={cx("p-nav__a", page === p && "is-on")} aria-current={page === p ? "page" : undefined} href={phref(p)}>
              <Icon name={icon} />
              <span>{name}</span>
              {counts[p] ? <span className="p-nav__count">{counts[p]}</span> : null}
            </a>
          ))}
        </nav>
        <div className="p-rail__foot">{chip}</div>
      </aside>
      <main className="p-main">
        {stale ? <Stale /> : null}
        <Screen route={route} />
      </main>
      <ToastView />
    </div>
  );
}

/** LoR is not answering: say since when, keep showing its last data, and hold every change. */
function Stale() {
  const now = useNow(60_000);
  return (
    <div className="p-stale" role="status">
      <span className="p-stale__dot" aria-hidden="true" />
      <p className="p-stale__text">
        <strong>LoR has not answered since {hm(now - 9 * 60_000)}.</strong> What you see is what it last said. Ticks, timers and labels wait until it is back; search still works.
      </p>
      <Button tone="quiet" size="sm" onClick={() => setHere({ state: undefined })}>
        Try again
      </Button>
    </div>
  );
}
