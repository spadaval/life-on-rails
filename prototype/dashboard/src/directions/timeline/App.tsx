// Timeline: the day is the spine. A left rail of pages (a tab bar at phone width), the one
// search field across the top of every page, and Today as a route down the middle with the
// untimed things beside it. Links: state=stale (LoR unreachable) and state=crowded (twelve
// waiting) are this direction's own; the scenes come from the shell.
import { useEffect, useRef } from "react";
import { useStore } from "../../data/store.js";
import { href } from "./lib/nav.js";
import type { Route } from "../../shell/nav.js";
import { Icon, NoticeBar } from "./components/bits.js";
import { Search, type SearchHandle } from "./components/Search.js";
import { TimerChip } from "./components/Timer.js";
import { ModeCtx } from "./lib/ctx.js";
import { ago, hm } from "./lib/time.js";
import { say, undoOffered } from "./lib/ui.js";
import { Goals } from "./screens/Goals.js";
import { Notes } from "./screens/Notes.js";
import { People } from "./screens/People.js";
import { Tasks } from "./screens/Tasks.js";
import { Today } from "./screens/Today.js";

const PAGES = [
  { path: "/", name: "Today", icon: "today" },
  { path: "/tasks", name: "Tasks", icon: "tasks" },
  { path: "/goals", name: "Goals", icon: "goals" },
  { path: "/people", name: "People", icon: "people" },
  { path: "/notes", name: "Notes", icon: "notes" },
];

export function App({ route }: { route: Route }) {
  const s = useStore();
  const search = useRef<SearchHandle>(null);
  const p = route.params;
  const staleSince = p.state === "stale" ? s.sceneAt - 38 * 60_000 : null;
  const crowded = p.state === "crowded";
  const page = PAGES.find((x) => x.path === route.path) ?? PAGES[0]!;

  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement;
      const typing = t.closest("input, textarea, select, [contenteditable]") !== null;
      const mod = e.metaKey || e.ctrlKey;
      if (mod && e.key.toLowerCase() === "k") {
        e.preventDefault();
        search.current?.open("find");
      } else if (mod && e.key.toLowerCase() === "z" && !typing) {
        if (undoOffered()) e.preventDefault();
      } else if (!typing && !mod && !e.altKey && e.key === "/") {
        e.preventDefault();
        search.current?.open("find");
      } else if (!typing && !mod && !e.altKey && e.key === "q") {
        e.preventDefault();
        search.current?.open("add");
      }
    };
    const add = () => search.current?.open("add");
    window.addEventListener("keydown", key);
    window.addEventListener("tl:add", add);
    return () => {
      window.removeEventListener("keydown", key);
      window.removeEventListener("tl:add", add);
    };
  }, []);

  return (
    <ModeCtx.Provider value={{ staleSince, crowded }}>
      <main className="tl" data-page={page.name.toLowerCase()} data-stale={staleSince !== null || undefined}>
        <nav className="tl-rail" aria-label="Pages">
          <a className="tl-brand" href={href("/")} aria-label="Life on Rails, Today">
            <span className="tl-brand-mark" aria-hidden="true" />
            <span className="tl-brand-name">Life on Rails</span>
          </a>
          <ul>
            {PAGES.map((x) => (
              <li key={x.path}>
                <a href={href(x.path)} aria-current={x === page ? "page" : undefined} title={x.name}>
                  <Icon name={x.icon} size={20} />
                  <span>{x.name}</span>
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <div className="tl-body">
          <div className="tl-top">
            <Search key={`${p.q ?? ""}|${p.add ?? ""}`} ref={search} initial={p.add ?? p.q ?? ""} initialMode={p.add !== undefined ? "add" : "find"} />
            {page.path !== "/" ? <TimerChip params={p} /> : null}
          </div>
          {staleSince !== null ? (
            <div className="tl-stale" role="status">
              <Icon name="cloud" size={18} />
              <p>
                <strong>LoR can’t be reached.</strong> Showing what it sent at <span className="tl-num">{hm(staleSince)}</span>, {ago(staleSince, s.sceneAt)}. Changes are off until it answers; it tries again every 30 seconds.
              </p>
              <button type="button" className="tl-btn tl-btn-sm" onClick={() => say("Still no answer from LoR.", { tone: "error" })}>
                Try now
              </button>
            </div>
          ) : null}
          {page.path === "/" ? <Today route={route} /> : page.path === "/tasks" ? <Tasks route={route} /> : page.path === "/goals" ? <Goals route={route} /> : page.path === "/people" ? <People route={route} /> : <Notes route={route} />}
        </div>
        <NoticeBar />
      </main>
    </ModeCtx.Provider>
  );
}
