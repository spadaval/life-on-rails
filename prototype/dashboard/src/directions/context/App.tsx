// Context: everything about what you are doing, in one place. A bar with the one search field
// and the timer, a row of contexts like tabs (Now, then recent projects, goals and people, then
// All tasks, Goals, People and Notes), and the current context as one raised surface with the
// next context and what waits elsewhere beside it. Links: state=stale (LoR unreachable) and
// state=crowded (twelve waiting) are this direction's own; the scenes come from the shell.
import { useEffect, useRef } from "react";
import { useNow, useStore } from "../../data/store.js";
import type { Route } from "../../shell/nav.js";
import { Icon, kindIcon, NoticeBar } from "./components/bits.js";
import { Search, type SearchHandle } from "./components/Search.js";
import { BlockLayer, TimerChip, TimerLayer } from "./components/Timer.js";
import { blockCtx, type Ctx, ctxPath, current, fromLink, homeOf, LOOSE, runningBlock, sameCtx, type Why } from "./lib/model.js";
import { href, navigate } from "./lib/nav.js";
import { ago, hm } from "./lib/time.js";
import { ModeCtx, say, undoOffered, useVisits, visit, type Visit } from "./lib/ui.js";
import { ContextView } from "./screens/ContextView.js";
import { GoalsIndex, Notes, PageReader, PeopleIndex, Tasks } from "./screens/Index.js";
import { Side } from "./screens/Side.js";

const INDEX = [
  { path: "/tasks", name: "All tasks", icon: "tasks" },
  { path: "/goals", name: "Goals", icon: "goal" },
  { path: "/people", name: "People", icon: "people" },
  { path: "/notes", name: "Notes", icon: "notes" },
];

export function App({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(1000);
  const search = useRef<SearchHandle>(null);
  const p = route.params;
  const staleSince = p.state === "stale" ? s.sceneAt - 38 * 60_000 : null;
  const crowded = p.state === "crowded";
  const visits = useVisits();

  const cur = current(s, now);
  // Which context this page shows, if it shows one.
  const path = route.path;
  let shown: { ctx: Ctx; why?: Why; focus?: typeof cur.focus; isNow: boolean } | null = null;
  let page: "now" | "ctx" | "tasks" | "goals" | "people" | "notes" = "now";
  if (path === "/project" || path === "/slot" || path === "/loose" || ((path === "/goals" || path === "/people") && p.id)) {
    const kind = path === "/project" ? "project" : path === "/slot" ? "slot" : path === "/loose" ? "loose" : path === "/goals" ? "goal" : "person";
    const c = fromLink(s, kind, p.id);
    if (c) {
      shown = { ctx: c, isNow: sameCtx(c, cur.ctx), why: sameCtx(c, cur.ctx) ? cur.why : undefined, focus: sameCtx(c, cur.ctx) ? cur.focus : null };
      page = "ctx";
    }
  }
  if (!shown) {
    if (path === "/tasks") page = "tasks";
    else if (path === "/goals") page = "goals";
    else if (path === "/people") page = "people";
    else if (path === "/notes") page = "notes";
    else {
      page = "now";
      shown = { ...cur, isNow: true };
    }
  }

  // A context opened is remembered as a tab.
  const shownKey = shown && page === "ctx" ? `${shown.ctx.kind}:${shown.ctx.linkId}` : null;
  useEffect(() => {
    if (shown && page === "ctx" && shown.ctx.kind !== "unnamed" && shown.ctx.kind !== "label") visit({ kind: shown.ctx.kind as Visit["kind"], id: shown.ctx.linkId });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shownKey]);

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
    window.addEventListener("cx:add", add);
    return () => {
      window.removeEventListener("keydown", key);
      window.removeEventListener("cx:add", add);
    };
  }, []);

  // Tabs: the contexts he opened, then those of what he timed lately; never the one Now shows.
  const seeded: Ctx[] = [];
  for (const r of s.recent) {
    const n = s.nodes.find((x) => x.id === r.id);
    if (n) seeded.push(homeOf(s, n));
  }
  const fromVisits = visits.map((v) => fromLink(s, v.kind, v.id)).filter((c): c is Ctx => !!c);
  const tabs: Ctx[] = [];
  for (const c of [...fromVisits, ...seeded]) {
    if (c.kind === "loose" && !fromVisits.includes(c)) continue;
    if (sameCtx(c, cur.ctx) || tabs.some((t) => sameCtx(t, c))) continue;
    tabs.push(c);
  }
  const r = runningBlock(s);
  const runCtx = r ? blockCtx(s, r) : null;
  const ctxShown = shown?.ctx ?? null;
  const chip = r && !(ctxShown && (sameCtx(ctxShown, runCtx) || (ctxShown.kind === "unnamed" && !r.intent_id)));
  const layerBlock = p.blk ?? (p.block ? s.blocks.filter((b) => hm(b.t_start) === p.block).at(-1)?.id : undefined);
  const searchHere = ctxShown ?? cur.ctx;

  return (
    <ModeCtx.Provider value={{ staleSince, crowded }}>
      <main className="cx" data-page={page} data-still={route.sticky.still === "1" || undefined} data-stale={staleSince !== null || undefined}>
        <header className="cx-bar">
          <div className="cx-bar-row">
            <a className="cx-brand" href={href("/")} aria-label="Life on Rails, Now">
              <span className="cx-brand-mark" aria-hidden="true" />
              <span className="cx-brand-name">Life on Rails</span>
            </a>
            <Search key={`${p.q ?? ""}|${p.add ?? ""}`} ref={search} here={searchHere} initial={p.add ?? p.q ?? ""} initialMode={p.add !== undefined ? "add" : "find"} />
            <div className="cx-bar-end">{chip ? <TimerChip onOpen={() => navigate("/")} /> : null}</div>
          </div>
          <nav className="cx-tabs" aria-label="Contexts">
            <a className="cx-tab cx-tab-now" href={href("/")} aria-current={page === "now" || (shown?.isNow && page === "ctx") ? "page" : undefined}>
              <span className="cx-tab-kicker">
                {r ? <span className="cx-live-dot" aria-hidden="true" /> : <Icon name="now" size={13} />} Now
              </span>
              <span className="cx-tab-title">{cur.ctx.title}</span>
            </a>
            <span className="cx-tabs-sep" aria-hidden="true" />
            {tabs.slice(0, 5).map((c) => (
              <a key={`${c.kind}:${c.key}`} className="cx-tab" href={href(ctxPath(c).path, ctxPath(c).params)} aria-current={ctxShown && page === "ctx" && sameCtx(ctxShown, c) ? "page" : undefined}>
                <Icon name={kindIcon(c.kind)} size={14} />
                <span className="cx-tab-title">{c.title}</span>
              </a>
            ))}
            <span className="cx-tabs-gap" aria-hidden="true" />
            {INDEX.map((x) => (
              <a key={x.path} className="cx-tab cx-tab-index" href={href(x.path)} aria-current={page === x.path.slice(1) ? "page" : undefined}>
                <Icon name={x.icon} size={15} />
                <span className="cx-tab-title">{x.name}</span>
              </a>
            ))}
          </nav>
        </header>

        {staleSince !== null ? (
          <div className="cx-stale" role="status">
            <Icon name="cloud" size={18} />
            <p>
              <strong>LoR can’t be reached.</strong> This is what it sent at <span className="cx-num">{hm(staleSince)}</span>, {ago(staleSince, s.sceneAt)}. Changes are off until it answers; it tries again every 30 seconds.
            </p>
            <button type="button" className="cx-btn cx-btn-sm" onClick={() => say("Still no answer from LoR.", { tone: "error" })}>
              Try now
            </button>
          </div>
        ) : null}

        <div className="cx-body">
          {shown ? (
            <div className="cx-work" data-reading={p.page ? true : undefined}>
              <ContextView ctx={shown.ctx} why={shown.why} focus={shown.focus} route={route} isNow={shown.isNow} />
              {p.page ? (
                <>
                  <PageReader slug={p.page} here={shown.ctx} />
                  <div className="cx-side-under"><Side here={shown.ctx} route={route} /></div>
                </>
              ) : (
                <Side here={shown.ctx} route={route} />
              )}
            </div>
          ) : page === "tasks" ? (
            <Tasks route={route} />
          ) : page === "goals" ? (
            <GoalsIndex />
          ) : page === "people" ? (
            <PeopleIndex route={route} />
          ) : (
            <Notes route={route} />
          )}
        </div>

        {p.timer ? <TimerLayer mode={p.timer} onDone={() => (page === "now" ? undefined : undefined)} /> : null}
        {layerBlock ? <BlockLayer blockId={layerBlock} /> : null}
        <NoticeBar />
      </main>
    </ModeCtx.Provider>
  );
}

export { LOOSE };
