// Queue: one list of what wants you (docs/design/lab/brief.md, "1. Queue"). A slim top bar with
// the pages and the one field that finds, adds and asks; one centred column below it. Today is
// the ranked list; Tasks, Goals, People and Notes are for managing, from the bar.
//
// Its own parameters, kept on every link (lib.ts qhref): `state=stale` (LoR not answering, its
// last data shown) and `state=crowded` (twelve things waiting). Per page: `id=` opens a row in
// place (by title; a page by slug), `block=HH:MM` opens the relabel popover on Today, `timer=switch`
// or `timer=start` opens the timer's picker, `brief=1|0` the brief, `find=` and `add=` the field.
import { useNow } from "../../data/store.js";
import type { Route } from "../../shell/nav.js";
import { go, hm, modeOf, qhref } from "./lib.js";
import { Omnibox } from "./omnibox.js";
import { Goals } from "./screens/Goals.js";
import { Notes } from "./screens/Notes.js";
import { People } from "./screens/People.js";
import { Tasks } from "./screens/Tasks.js";
import { Today } from "./screens/Today.js";
import { TimerChip } from "./timer.js";
import { Button, cx, ToastView } from "./ui.js";

const PAGES = [
  ["/", "Today"],
  ["/tasks", "Tasks"],
  ["/goals", "Goals"],
  ["/people", "People"],
  ["/notes", "Notes"],
] as const;

export function App({ route }: { route: Route }) {
  const mode = modeOf(route.params);
  const page = PAGES.some(([p]) => p === route.path) ? route.path : "/";
  const Screen = { "/": Today, "/tasks": Tasks, "/goals": Goals, "/people": People, "/notes": Notes }[page]!;
  return (
    <div className="q-app" data-mode={mode ?? undefined}>
      <header className="q-bar">
        <div className="q-bar__in">
          <a className="q-mark" href={qhref("/")} aria-label="LoR, Today">
            LoR
          </a>
          <nav className="q-nav" aria-label="Pages">
            {PAGES.map(([p, name]) => (
              <a key={p} className={cx("q-nav__a", page === p && "is-on")} aria-current={page === p ? "page" : undefined} href={qhref(p)}>
                {name}
              </a>
            ))}
          </nav>
          <div className="q-bar__right">
            {page !== "/" ? <TimerChip stale={mode === "stale"} /> : null}
            <Omnibox key={`${route.params.find ?? ""}|${route.params.add ?? ""}`} initial={route.params.find ?? route.params.add} initialMode={route.params.add !== undefined ? "add" : undefined} stale={mode === "stale"} />
          </div>
        </div>
      </header>
      {mode === "stale" ? <Stale path={route.path} params={route.params} /> : null}
      <main className="q-main">
        <Screen route={route} />
      </main>
      <ToastView />
    </div>
  );
}

/** LoR is not answering: say since when, keep showing its last data, and hold every change. */
function Stale({ path, params }: { path: string; params: Record<string, string> }) {
  const now = useNow(60_000);
  const since = now - 9 * 60_000;
  return (
    <div className="q-stale" role="status">
      <span className="q-stale__dot" aria-hidden="true" />
      <p className="q-stale__text">
        <strong>LoR has not answered since {hm(since)}.</strong> This is what it last said. Ticks, timers and labels wait until it is back.
      </p>
      <Button tone="quiet" size="sm" onClick={() => go(path, { ...params, state: undefined })}>
        Try again
      </Button>
    </div>
  );
}
