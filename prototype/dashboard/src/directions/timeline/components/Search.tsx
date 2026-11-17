// The one field that finds, adds and asks, on every page at every width. `/` or ⌘K opens it,
// `q` opens it to add. It says what it understood before Enter does anything: the first row is
// what Enter will do, with the date and the goal it read out of the words shown as chips.
import { forwardRef, useImperativeHandle, useMemo, useRef, useState } from "react";
import { type Hit, search } from "../../../data/derive.js";
import { addIntent, askAgent, today as todayOf, useStore } from "../../../data/store.js";
import type { State } from "../../../data/store.js";
import { navigate } from "../lib/nav.js";
import { useMode } from "../lib/ctx.js";
import { dueWords, readDate, shortDate } from "../lib/time.js";
import { reversible, sendLater } from "../lib/ui.js";
import { Icon } from "./bits.js";

export interface SearchHandle {
  open: (mode: "find" | "add", text?: string) => void;
}

interface Parsed {
  title: string;
  due: { date: string; said: string } | null;
  parent: { id: string; title: string } | null;
}

/** Read `#goal` (the longest goal or project title the words start with) and one date. */
function parse(s: State, text: string, today: string): Parsed {
  let rest = text;
  let parent: Parsed["parent"] = null;
  const hash = rest.indexOf("#");
  if (hash >= 0) {
    const after = rest.slice(hash + 1).toLowerCase();
    const goals = s.nodes.filter((n) => (n.type === "goal" || n.type === "project") && n.status === "active");
    const word = after.split(/\s+/)[0] ?? "";
    const full = goals.filter((n) => after.startsWith(n.title.toLowerCase())).sort((a, b) => b.title.length - a.title.length)[0];
    const n = full ?? (word ? goals.find((g) => g.title.toLowerCase().split(/\s+/).some((w) => w.startsWith(word))) : undefined);
    if (n) {
      parent = { id: n.id, title: n.title };
      const len = full ? n.title.length : word.length;
      rest = rest.slice(0, hash) + rest.slice(hash + 1 + len);
    }
  }
  const d = readDate(rest, today);
  if (d) rest = d.rest;
  return { title: rest.replace(/\s+/g, " ").trim(), due: d ? { date: d.date, said: d.said } : null, parent };
}

type Row =
  | { kind: "add"; p: Parsed }
  | { kind: "hit"; hit: Hit }
  | { kind: "ask"; text: string };

const KIND: Record<Hit["kind"], string> = { task: "Task", goal: "Goal", project: "Project", habit: "Habit", person: "Person", page: "Note" };

export function openHit(h: Hit): void {
  if (h.kind === "task") navigate("/tasks", { id: h.title });
  else if (h.kind === "person") navigate("/people", { id: h.title });
  else if (h.kind === "page") navigate("/notes", { slug: h.id });
  else navigate("/goals", { id: h.title });
}

export const Search = forwardRef<SearchHandle, { initial?: string; initialMode?: "find" | "add" }>(function Search({ initial = "", initialMode = "find" }, handle) {
  const s = useStore();
  const { staleSince } = useMode();
  const input = useRef<HTMLInputElement>(null);
  const [text, setText] = useState(initial);
  const [mode, setMode] = useState<"find" | "add">(initialMode);
  const [open, setOpen] = useState(Boolean(initial));
  const [sel, setSel] = useState(0);
  const today = todayOf();

  useImperativeHandle(handle, () => ({
    open: (m, t) => {
      setMode(m);
      if (t !== undefined) setText(t);
      setOpen(true);
      setSel(0);
      requestAnimationFrame(() => input.current?.focus());
    },
  }));

  const rows: Row[] = useMemo(() => {
    const t = text.trim();
    if (!t) return [];
    const p = parse(s, t, today);
    const hits = search(s, t, 7);
    const wantsAdd = mode === "add" || p.due !== null || p.parent !== null;
    const out: Row[] = [];
    if (wantsAdd && p.title) out.push({ kind: "add", p });
    for (const h of hits) out.push({ kind: "hit", hit: h });
    if (!wantsAdd) out.push({ kind: "ask", text: t });
    if (!wantsAdd && p.title) out.push({ kind: "add", p });
    if (wantsAdd) out.push({ kind: "ask", text: t });
    return out;
  }, [s, text, mode, today]);

  const close = () => {
    setOpen(false);
    setText("");
    setMode("find");
    input.current?.blur();
  };

  const run = (r: Row | undefined) => {
    if (!r) return;
    if (r.kind === "hit") {
      openHit(r.hit);
      close();
    } else if (staleSince !== null) {
      return;
    } else if (r.kind === "add") {
      const { title, due, parent } = r.p;
      reversible(`Added “${title}”${due ? `, due ${dueWords(due.date, today)}` : ""}`, () => addIntent({ title, due: due?.date ?? null, parentId: parent?.id ?? null }));
      close();
    } else {
      sendLater(r.text, () => askAgent(r.text));
      close();
    }
  };

  return (
    <div className="tl-search" data-open={(open && text.trim() !== "") || undefined}>
      <label className="tl-search-field">
        <Icon name={mode === "add" ? "plus" : "search"} />
        <input
          ref={input}
          value={text}
          placeholder={mode === "add" ? "Add a task: “book the ferry fri #Lisbon”" : "Find, add or ask"}
          aria-label="Find, add or ask"
          aria-expanded={open && rows.length > 0}
          aria-controls="tl-search-rows"
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 120)}
          onChange={(e) => {
            setText(e.target.value);
            setSel(0);
            setOpen(true);
          }}
          onKeyDown={(e) => {
            if (e.key === "Escape") close();
            else if (e.key === "ArrowDown") {
              e.preventDefault();
              setSel((x) => Math.min(rows.length - 1, x + 1));
            } else if (e.key === "ArrowUp") {
              e.preventDefault();
              setSel((x) => Math.max(0, x - 1));
            } else if (e.key === "Enter") {
              e.preventDefault();
              run(rows[sel]);
            }
          }}
        />
        {text ? null : (
          <span className="tl-keys" aria-hidden="true">
            <kbd>/</kbd>
            <kbd>q</kbd>
          </span>
        )}
      </label>
      {open && rows.length ? (
        <ul className="tl-search-rows" id="tl-search-rows" role="listbox" aria-label="Results">
          {rows.map((r, i) => (
            <li key={i} role="option" aria-selected={i === sel} data-kind={r.kind} onMouseEnter={() => setSel(i)} onMouseDown={(e) => e.preventDefault()} onClick={() => run(r)}>
              {r.kind === "add" ? (
                <>
                  <span className="tl-sr-icon tl-sr-add">
                    <Icon name="plus" size={16} />
                  </span>
                  <span className="tl-sr-main">
                    <span className="tl-sr-k">Add task</span>
                    <span className="tl-sr-title">{r.p.title}</span>
                    <span className="tl-sr-chips">
                      {r.p.due ? (
                        <span className="tl-tag tl-tag-date">
                          {shortDate(r.p.due.date)} <small>from “{r.p.due.said}”</small>
                        </span>
                      ) : (
                        <span className="tl-tag">no date</span>
                      )}
                      {r.p.parent ? <span className="tl-tag tl-tag-goal">{r.p.parent.title}</span> : null}
                    </span>
                  </span>
                </>
              ) : r.kind === "ask" ? (
                <>
                  <span className="tl-sr-icon tl-sr-ask">
                    <Icon name="spark" size={16} />
                  </span>
                  <span className="tl-sr-main">
                    <span className="tl-sr-k">Ask the agent</span>
                    <span className="tl-sr-title">{r.text}</span>
                  </span>
                </>
              ) : (
                <>
                  <span className="tl-sr-icon">
                    <Icon name={r.hit.kind === "person" ? "people" : r.hit.kind === "page" ? "notes" : r.hit.kind === "task" ? "tasks" : "goals"} size={16} />
                  </span>
                  <span className="tl-sr-main">
                    <span className="tl-sr-title">{r.hit.title}</span>
                    {r.hit.detail ? <span className="tl-sr-detail">{r.hit.detail}</span> : null}
                  </span>
                  <span className="tl-sr-kind">{KIND[r.hit.kind]}</span>
                </>
              )}
              {i === sel ? <kbd className="tl-sr-enter">↵</kbd> : null}
            </li>
          ))}
          {staleSince !== null ? <li className="tl-sr-off">LoR can’t be reached, so adding and asking wait until it answers. Search reads the last data.</li> : null}
        </ul>
      ) : null}
    </div>
  );
});
