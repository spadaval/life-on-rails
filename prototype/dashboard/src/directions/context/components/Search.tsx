// The one field that finds, adds and asks, on every page at every width (`/` or ⌘K, `q` to add).
// Plain words find; a date word or #goal offers to add a task, read back as chips before Enter;
// anything else can go to the agent. In add mode a new task goes into the context on show.
import { forwardRef, useImperativeHandle, useMemo, useRef, useState } from "react";
import { search } from "../../../data/derive.js";
import { addIntent, today as todayOf, useStore } from "../../../data/store.js";
import type { Node } from "../../../data/types.js";
import { type Ctx, ctxPath, homeOf } from "../lib/model.js";
import { navigate } from "../lib/nav.js";
import { dateWord, dueWords, shortDate } from "../lib/time.js";
import { ask, reversible, say, useOff } from "../lib/ui.js";
import { Icon, kindIcon } from "./bits.js";

export interface SearchHandle {
  open: (mode: "find" | "add") => void;
}

interface Parsed {
  title: string;
  due: string | null;
  parent: Node | null;
  /** The parent came from #tag (not from the context on show). */
  tagged: boolean;
}

/** Read a task out of plain words: a date word anywhere, and #goal or #project (as many words as match). */
function parse(text: string, nodes: Node[], today: string, fallback: Node | null): Parsed {
  let words = text.trim().split(/\s+/).filter(Boolean);
  let due: string | null = null;
  let parent: Node | null = null;
  const hash = words.findIndex((w) => w.startsWith("#"));
  if (hash >= 0) {
    const rest = [words[hash]!.slice(1), ...words.slice(hash + 1)];
    let best: { n: Node; len: number } | null = null;
    for (let len = rest.length; len >= 1 && !best; len--) {
      const tag = rest.slice(0, len).join(" ").toLowerCase();
      if (!tag) continue;
      const n = nodes.find((x) => (x.type === "goal" || x.type === "project") && x.status === "active" && x.title.toLowerCase().startsWith(tag));
      if (n) best = { n, len };
    }
    if (best) {
      parent = best.n;
      words = [...words.slice(0, hash), ...words.slice(hash + best.len)];
    }
  }
  const di = words.findIndex((w) => dateWord(w, today) !== null);
  if (di >= 0) {
    due = dateWord(words[di]!, today);
    words = words.filter((_, i) => i !== di);
  }
  const title = words.join(" ").replace(/^(add|todo|task)\s+/i, "");
  return { title: title ? title[0]!.toUpperCase() + title.slice(1) : "", due, parent: parent ?? fallback, tagged: parent !== null };
}

type Row =
  | { kind: "add"; p: Parsed }
  | { kind: "hit"; hit: ReturnType<typeof search>[number] }
  | { kind: "ask"; text: string };

export const Search = forwardRef<SearchHandle, { here: Ctx; initial?: string; initialMode?: "find" | "add" }>(function Search({ here, initial = "", initialMode = "find" }, ref) {
  const s = useStore();
  const off = useOff();
  const today = todayOf();
  const input = useRef<HTMLInputElement>(null);
  const [q, setQ] = useState(initial);
  const [mode, setMode] = useState<"find" | "add">(initialMode);
  const [open, setOpen] = useState(initial !== "" || initialMode === "add");
  const [sel, setSel] = useState(0);
  const [keepHere, setKeepHere] = useState(true);

  useImperativeHandle(ref, () => ({
    open: (m) => {
      setMode(m);
      setOpen(true);
      setSel(0);
      setKeepHere(true);
      // At once, not on the next frame: keys typed straight after `/` or `q` must land in the field.
      input.current?.focus();
    },
  }));

  const hereNode = here.node && keepHere ? here.node : null;
  const rows = useMemo<Row[]>(() => {
    const text = q.trim();
    if (!text) return [];
    const p = parse(text, s.nodes, today, mode === "add" ? hereNode : null);
    const out: Row[] = [];
    const looksLikeTask = mode === "add" || p.due !== null || p.tagged;
    if (looksLikeTask && p.title) out.push({ kind: "add", p });
    if (mode === "find") for (const hit of search(s, text, 7)) out.push({ kind: "hit", hit });
    if (mode === "find" && !looksLikeTask && p.title) out.push({ kind: "add", p: { ...p, parent: hereNode } });
    if (mode === "find") out.push({ kind: "ask", text });
    return out;
  }, [q, s, today, mode, hereNode]);

  const close = () => {
    setOpen(false);
    setQ("");
    setMode("find");
    input.current?.blur();
  };

  const run = (row: Row) => {
    if (row.kind === "add") {
      if (off) return say("LoR can’t be reached, so nothing can be added until it answers.", { tone: "error" });
      const { title, due, parent } = row.p;
      reversible(`Added “${title}”${parent ? ` to ${parent.title}` : ""}${due ? `, due ${dueWords(due, today)}` : ""}`, () => {
        addIntent({ title, due, parentId: parent?.id ?? null });
      });
      close();
      return;
    }
    if (row.kind === "ask") {
      if (off) return say("LoR can’t be reached, so the agent can’t be asked from here. Ask from T3 on the phone.", { tone: "error" });
      ask(row.text, here.node?.title ?? here.person?.name ?? null);
      close();
      return;
    }
    const h = row.hit;
    close();
    if (h.kind === "person") return navigate("/people", { id: h.title });
    if (h.kind === "page") return navigate("/notes", { id: h.id });
    const n = s.nodes.find((x) => x.id === h.id);
    if (!n) return;
    if (n.type === "goal" || n.type === "project") {
      const c = homeOf(s, n);
      return navigate(ctxPath(c).path, ctxPath(c).params);
    }
    const c = homeOf(s, n);
    navigate(ctxPath(c).path, { ...ctxPath(c).params, ...(n.type === "task" ? { task: n.title } : {}) });
  };

  const onKey = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSel((x) => Math.min(rows.length - 1, x + 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setSel((x) => Math.max(0, x - 1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      const row = rows[sel];
      if (row) run(row);
    } else if (e.key === "Escape") {
      e.preventDefault();
      close();
    } else if (e.key === "Backspace" && q === "" && mode === "add") {
      setMode("find");
    }
  };

  const showing = open && (q.trim() !== "" || mode === "add");

  return (
    <div className="cx-search" data-open={showing || undefined} data-mode={mode}>
      <label className="cx-search-field">
        {mode === "add" ? (
          <span className="cx-search-mode">
            <Icon name="plus" size={14} /> Add
          </span>
        ) : (
          <Icon name="search" size={17} />
        )}
        <input
          ref={input}
          value={q}
          onChange={(e) => {
            setQ(e.target.value);
            setSel(0);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen((o) => (document.activeElement?.closest(".cx-search") ? o : false)), 120)}
          onKeyDown={onKey}
          placeholder={mode === "add" ? "A task, with a day or #goal if you like" : "Search, add or ask"}
          aria-label={mode === "add" ? "Add a task" : "Search, add a task or ask the agent"}
          role="combobox"
          aria-expanded={showing}
          aria-controls="cx-search-results"
          aria-activedescendant={showing && rows[sel] ? `cx-sr-${sel}` : undefined}
        />
        <kbd className="cx-kbd" aria-hidden="true">{mode === "add" ? "↵" : "⌘K"}</kbd>
      </label>
      {showing ? (
        <div className="cx-results" id="cx-search-results" role="listbox" aria-label="What Enter does">
          {mode === "add" && !q.trim() ? (
            <p className="cx-results-hint">
              Type the task. “fri” or “tomorrow” sets the day, “#Lisbon” puts it under a goal or project.
              {hereNode ? <> Without a #, it goes into <strong>{hereNode.title}</strong>.</> : null}
            </p>
          ) : null}
          {rows.map((row, i) => (
            <button
              key={i}
              id={`cx-sr-${i}`}
              type="button"
              role="option"
              aria-selected={i === sel}
              className="cx-result"
              data-kind={row.kind}
              onMouseEnter={() => setSel(i)}
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => run(row)}
              disabled={off && row.kind !== "hit"}
            >
              {row.kind === "add" ? (
                <>
                  <span className="cx-result-icon cx-result-icon-add"><Icon name="plus" size={16} /></span>
                  <span className="cx-result-text">
                    <span className="cx-result-kicker">Add task</span>
                    <span className="cx-result-title">{row.p.title}</span>
                    <span className="cx-result-chips">
                      {row.p.due ? <span className="cx-chip cx-chip-amber"><Icon name="calendar" size={12} /> {shortDate(row.p.due)}</span> : <span className="cx-chip">No date</span>}
                      {row.p.parent ? (
                        <span className="cx-chip">
                          <Icon name={kindIcon(row.p.parent.type)} size={12} /> {row.p.parent.title}
                          {!row.p.tagged && mode === "add" ? (
                            <span
                              role="button"
                              tabIndex={-1}
                              className="cx-chip-x"
                              aria-label="Not in this context"
                              onMouseDown={(e) => e.preventDefault()}
                              onClick={(e) => {
                                e.stopPropagation();
                                setKeepHere(false);
                              }}
                            >
                              <Icon name="close" size={11} />
                            </span>
                          ) : null}
                        </span>
                      ) : (
                        <span className="cx-chip">Loose ends</span>
                      )}
                    </span>
                  </span>
                  <kbd className="cx-kbd">↵</kbd>
                </>
              ) : row.kind === "ask" ? (
                <>
                  <span className="cx-result-icon cx-result-icon-ask"><Icon name="ask" size={16} /></span>
                  <span className="cx-result-text">
                    <span className="cx-result-kicker">Ask the agent{here.node || here.person ? `, about ${here.title}` : ""}</span>
                    <span className="cx-result-title">“{row.text}”</span>
                  </span>
                </>
              ) : (
                <>
                  <span className="cx-result-icon" data-kind={row.hit.kind}>
                    <Icon name={row.hit.kind === "page" ? "notes" : row.hit.kind === "task" ? "tasks" : row.hit.kind === "habit" ? "spark" : kindIcon(row.hit.kind)} size={16} />
                  </span>
                  <span className="cx-result-text">
                    <span className="cx-result-kicker">{row.hit.kind === "page" ? "Note" : row.hit.kind[0]!.toUpperCase() + row.hit.kind.slice(1)}</span>
                    <span className="cx-result-title">{row.hit.title}</span>
                    {row.hit.detail ? <span className="cx-result-detail">{row.hit.detail}</span> : null}
                  </span>
                </>
              )}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
});
