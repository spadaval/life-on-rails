// The one field on every page that finds, adds and asks (`/` or ⌘K to find, `q` to add). Plain
// words find tasks, goals, people and notes. Text with a date, a #goal or an @person offers to
// add a task, and a question offers to ask the agent. Each choice says what it understood
// before anything happens; Enter takes the highlighted one.
import { useEffect, useMemo, useRef, useState } from "react";
import { parseQuickAdd, type Parsed } from "../../../../../src/shared/quickAdd.js";
import { search, type Hit } from "../../data/derive.js";
import { addIntent, askAgent, useStore } from "../../data/store.js";
import { act, go, shortDay } from "./lib.js";
import { cx, Icon } from "./ui.js";

type Option = { kind: "add"; parsed: Parsed } | { kind: "hit"; hit: Hit } | { kind: "ask"; text: string };

const QUESTION = /\?\s*$|^(what|when|who|why|how|where|which|is|are|can|could|should|did|do|does|will|would)\b/i;
const KIND: Record<Hit["kind"], string> = { task: "Task", goal: "Goal", project: "Project", habit: "Habit", person: "Person", page: "Note" };

function open(hit: Hit) {
  if (hit.kind === "task") go("/tasks", { id: hit.title });
  else if (hit.kind === "person") go("/people", { id: hit.title });
  else if (hit.kind === "page") go("/notes", { id: hit.id });
  else go("/goals", { id: hit.title });
}

export function Omnibox({ initial, initialMode, stale }: { initial?: string; initialMode?: "add"; stale?: boolean }) {
  const s = useStore();
  const [text, setText] = useState(initial ?? "");
  const [mode, setMode] = useState<"find" | "add">(initialMode ?? "find");
  const [shown, setShown] = useState(Boolean(initial || initialMode));
  const [hi, setHi] = useState(0);
  const input = useRef<HTMLInputElement>(null);
  const box = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (initial || initialMode) input.current?.focus();
  }, [initial, initialMode]);

  // `/` and ⌘K find, `q` adds; not while typing somewhere else.
  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement;
      const typing = t.closest("input, textarea, select, [contenteditable='true']");
      const mod = (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k";
      if (mod || (!typing && !e.metaKey && !e.ctrlKey && !e.altKey && (e.key === "/" || e.key === "q"))) {
        e.preventDefault();
        setMode(e.key === "q" ? "add" : "find");
        setShown(true);
        input.current?.focus();
      }
    };
    addEventListener("keydown", key);
    return () => removeEventListener("keydown", key);
  }, []);

  useEffect(() => {
    if (!shown) return;
    const down = (e: PointerEvent) => !box.current?.contains(e.target as Node) && setShown(false);
    addEventListener("pointerdown", down, true);
    return () => removeEventListener("pointerdown", down, true);
  }, [shown]);

  const options = useMemo<Option[]>(() => {
    const q = text.trim();
    if (!q) return [];
    const parents = s.nodes.filter((n) => (n.type === "goal" || n.type === "project") && n.status === "active").map((n) => ({ id: n.id, name: n.title }));
    const parsed = parseQuickAdd(q, { today: s.day.date, parents, people: s.entities.map((e) => ({ id: e.id, name: e.name })) });
    const hits: Option[] = search(s, q, 6).map((hit) => ({ kind: "hit", hit }));
    const add: Option = { kind: "add", parsed };
    const ask: Option = { kind: "ask", text: q };
    if (mode === "add" || parsed.tokens.length) return [add, ...hits, ask];
    if (QUESTION.test(q)) return [ask, ...hits, add];
    return hits.length ? [...hits, add, ask] : [add, ask];
  }, [text, mode, s]);

  const close = () => {
    setShown(false);
    setText("");
    setMode("find");
    setHi(0);
    input.current?.blur();
  };

  const choose = (o: Option) => {
    if (o.kind === "hit") {
      close();
      open(o.hit);
      return;
    }
    if (stale) return;
    if (o.kind === "ask") {
      act("Sent to the agent. It answers on your phone.", () => askAgent(o.text));
      close();
      return;
    }
    const p = o.parsed;
    if (!p.title) return;
    act(`Added “${p.title}”`, () => addIntent({ title: p.title, due: p.due, parentId: p.parentId, assignee: p.assignee, waitingOnEntityId: p.waitingOnId, status: p.waitingOnId ? "waiting" : "active" }), {
      undoable: true,
      action: { label: "Open", run: () => go("/tasks", { id: p.title }) },
    });
    close();
  };

  const placeholder = mode === "add" ? "Add a task: “book the ferry fri #Lisbon”" : "Search, add or ask";
  const hasText = text.trim().length > 0;
  return (
    <div className={cx("q-omni", shown && "is-open")} ref={box}>
      <div className="q-omni__field">
        {mode === "add" ? <span className="q-omni__mode">Add</span> : <Icon name="search" />}
        <input
          ref={input}
          className="q-omni__input"
          type="search"
          value={text}
          placeholder={placeholder}
          aria-label="Search, add a task, or ask the agent"
          aria-expanded={shown && hasText}
          aria-controls="q-omni-list"
          onFocus={() => setShown(true)}
          onChange={(e) => {
            setText(e.target.value);
            setHi(0);
            setShown(true);
          }}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") (e.preventDefault(), setHi((h) => Math.min(h + 1, options.length - 1)));
            else if (e.key === "ArrowUp") (e.preventDefault(), setHi((h) => Math.max(h - 1, 0)));
            else if (e.key === "Enter" && options[hi]) (e.preventDefault(), choose(options[hi]!));
            else if (e.key === "Escape") close();
            else if (e.key === "Backspace" && !text && mode === "add") setMode("find");
          }}
        />
        {!shown || !hasText ? <kbd className="q-kbd">/</kbd> : null}
      </div>
      {shown && (hasText || mode === "add") ? (
        <div className="q-omni__panel" id="q-omni-list" role="listbox" aria-label="Choices">
          {!hasText ? (
            <p className="q-omni__hint">Type a task. A date (fri, 3 dec), #goal, @person or @agent is read as you type.</p>
          ) : (
            options.map((o, i) => (
              <OptionRow key={o.kind === "hit" ? `${o.hit.kind}${o.hit.id}` : o.kind} o={o} on={i === hi} stale={stale} onHover={() => setHi(i)} onPick={() => choose(o)} />
            ))
          )}
          <p className="q-omni__keys">
            <span>
              <kbd className="q-kbd">↑</kbd>
              <kbd className="q-kbd">↓</kbd> choose
            </span>
            <span>
              <kbd className="q-kbd">↵</kbd> do it
            </span>
            <span>
              <kbd className="q-kbd">q</kbd> add
            </span>
            <span>
              <kbd className="q-kbd">esc</kbd> close
            </span>
          </p>
        </div>
      ) : null}
    </div>
  );
}

function OptionRow({ o, on, stale, onHover, onPick }: { o: Option; on: boolean; stale?: boolean; onHover: () => void; onPick: () => void }) {
  const s = useStore();
  const off = stale && o.kind !== "hit";
  let body;
  if (o.kind === "hit") {
    body = (
      <>
        <span className="q-omni__kind">{KIND[o.hit.kind]}</span>
        <span className="q-omni__main">
          <span className="q-omni__title">{o.hit.title}</span>
          {o.hit.detail ? <span className="q-omni__detail">{o.hit.detail}</span> : null}
        </span>
        <span className="q-omni__verb">Open</span>
      </>
    );
  } else if (o.kind === "ask") {
    body = (
      <>
        <span className="q-omni__kind q-omni__kind--agent">Ask</span>
        <span className="q-omni__main">
          <span className="q-omni__title">“{o.text}”</span>
          <span className="q-omni__detail">The agent answers on your phone</span>
        </span>
        <span className="q-omni__verb">Ask</span>
      </>
    );
  } else {
    const p = o.parsed;
    const chips: string[] = [];
    if (p.due) chips.push(p.due === s.day.date ? "today" : shortDay(p.due));
    for (const t of p.tokens) {
      if (t.kind === "parent") chips.push(t.name);
      if (t.kind === "waiting") chips.push(`waiting on ${t.name}`);
      if (t.kind === "agent") chips.push("for the agent");
    }
    body = (
      <>
        <span className="q-omni__kind q-omni__kind--add">Add</span>
        <span className="q-omni__main">
          <span className="q-omni__title">{p.title || "…"}</span>
          <span className="q-omni__chips">
            {chips.length ? chips.map((c) => <span key={c} className="q-tag">{c}</span>) : <span className="q-omni__detail">A task with no date</span>}
          </span>
        </span>
        <span className="q-omni__verb">Add task</span>
      </>
    );
  }
  return (
    <button type="button" role="option" aria-selected={on} aria-disabled={off || undefined} className={cx("q-omni__opt", on && "is-hi", off && "is-off")} onMouseMove={onHover} onClick={onPick} title={off ? "LoR is not answering; try again when it is back" : undefined}>
      {body}
    </button>
  );
}
