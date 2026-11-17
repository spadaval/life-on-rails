// The one field on every page that finds, adds and asks: at the top of the rail from 720px, at
// the top of the page below. `/` or ⌘K finds, `q` adds, and the Add buttons on the lists open it
// in add mode for a tap. Plain words find tasks, goals, people and notes; text with a date, a
// #goal or an @person offers to add a task; a question offers to ask the agent. Each choice says
// what it understood before anything happens. Choosing a result opens it in its page's detail pane.
import { useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { parseQuickAdd, type Parsed } from "../../../../../src/shared/quickAdd.js";
import { search, type Hit } from "../../data/derive.js";
import { addIntent, askAgent, useStore } from "../../data/store.js";
import { act, go, shortDay, STALE, useFit, useLayer } from "./lib.js";
import { cx, Icon } from "./ui.js";

type Option = { kind: "add"; parsed: Parsed } | { kind: "hit"; hit: Hit } | { kind: "ask"; text: string };

const QUESTION = /\?\s*$|^(what|when|who|why|how|where|which|is|are|can|could|should|did|do|does|will|would)\b/i;
const KIND: Record<Hit["kind"], string> = { task: "Task", goal: "Goal", project: "Project", habit: "Habit", person: "Person", page: "Note" };

/** Open the field from a button: `add` starts it in add mode. */
export function openField(mode: "find" | "add") {
  dispatchEvent(new CustomEvent("panes:field", { detail: mode }));
}

function open(hit: Hit) {
  if (hit.kind === "task") go("/tasks", { id: hit.id });
  else if (hit.kind === "person") go("/people", { id: hit.id });
  else if (hit.kind === "page") go("/notes", { id: hit.id });
  else go("/goals", { id: hit.id });
}

export function Omnibox({ initial, initialMode, stale }: { initial?: string; initialMode?: "add"; stale?: boolean }) {
  const s = useStore();
  const fit = useFit();
  const [text, setText] = useState(initial ?? "");
  const [mode, setMode] = useState<"find" | "add">(initialMode ?? "find");
  const [shown, setShown] = useState(Boolean(initial || initialMode));
  const [hi, setHi] = useState(0);
  const input = useRef<HTMLInputElement>(null);
  const box = useRef<HTMLDivElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const [rect, setRect] = useState<DOMRect | null>(null);

  useEffect(() => {
    if (initial || initialMode) input.current?.focus();
  }, [initial, initialMode]);

  // `/` and ⌘K find, `q` adds; not while typing somewhere else. The Add buttons send an event.
  useEffect(() => {
    const begin = (m: "find" | "add") => {
      setMode(m);
      setShown(true);
      input.current?.focus();
    };
    const key = (e: KeyboardEvent) => {
      const t = e.target as HTMLElement;
      const typing = t.closest("input, textarea, select, [contenteditable='true']");
      const mod = (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k";
      if (mod || (!typing && !e.metaKey && !e.ctrlKey && !e.altKey && (e.key === "/" || e.key === "q"))) {
        e.preventDefault();
        begin(e.key === "q" ? "add" : "find");
      }
    };
    const fromButton = (e: Event) => begin((e as CustomEvent<"find" | "add">).detail);
    addEventListener("keydown", key);
    addEventListener("panes:field", fromButton);
    return () => {
      removeEventListener("keydown", key);
      removeEventListener("panes:field", fromButton);
    };
  }, []);

  useEffect(() => {
    if (!shown) return;
    const down = (e: PointerEvent) => {
      const t = e.target as Node;
      if (!box.current?.contains(t) && !panel.current?.contains(t)) setShown(false);
    };
    addEventListener("pointerdown", down, true);
    return () => removeEventListener("pointerdown", down, true);
  }, [shown]);

  const hasText = text.trim().length > 0;
  const panelOpen = shown && (hasText || mode === "add");

  // The choices float on <body>, below the field, so no pane's blur or clipping reaches them.
  useEffect(() => {
    if (!panelOpen) return;
    const place = () => box.current && setRect(box.current.getBoundingClientRect());
    place();
    addEventListener("resize", place);
    addEventListener("scroll", place, true);
    return () => {
      removeEventListener("resize", place);
      removeEventListener("scroll", place, true);
    };
  }, [panelOpen]);

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
  useLayer(shown, close);

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
    let id = "";
    act(`Added “${p.title}”`, () => (id = addIntent({ title: p.title, due: p.due, parentId: p.parentId, assignee: p.assignee, waitingOnEntityId: p.waitingOnId, status: p.waitingOnId ? "waiting" : "active" })), {
      undoable: true,
      action: { label: "Open", run: () => go("/tasks", { id }) },
    });
    close();
  };

  const placeholder = mode === "add" ? "Add a task: “ferry fri #Lisbon”" : "Search, add or ask";
  const narrow = fit === "narrow";
  const width = narrow ? innerWidth - 24 : Math.max(rect?.width ?? 0, 460);
  return (
    <div className={cx("p-omni", shown && "is-open", mode === "add" && "is-add")} ref={box}>
      <span className="p-omni__lead">{mode === "add" ? <span className="p-omni__mode">Add</span> : <Icon name="search" />}</span>
      <input
        ref={input}
        className="p-omni__input"
        type="search"
        value={text}
        placeholder={placeholder}
        aria-label="Search, add a task, or ask the agent"
        aria-expanded={panelOpen}
        aria-controls="p-omni-list"
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
          else if (e.key === "Backspace" && !text && mode === "add") setMode("find");
        }}
      />
      {shown ? (
        <button type="button" className="p-omni__x" aria-label="Close the field" onClick={close}>
          <Icon name="close" />
        </button>
      ) : (
        <kbd className="p-kbd">/</kbd>
      )}
      {panelOpen && rect
        ? createPortal(
            <div
              ref={panel}
              className={cx("p-omni__panel", narrow && "is-narrow")}
              id="p-omni-list"
              role="listbox"
              aria-label="Choices"
              style={{ top: rect.bottom + 8, left: narrow ? 12 : rect.left, width }}
            >
              {!hasText ? (
                <p className="p-omni__hint">Type a task. A date (fri, 3 dec), #goal, @person or @agent is read as you type, and shown here before you add it.</p>
              ) : (
                options.map((o, i) => <OptionRow key={o.kind === "hit" ? `${o.hit.kind}${o.hit.id}` : o.kind} o={o} on={i === hi} stale={stale} onHover={() => setHi(i)} onPick={() => choose(o)} />)
              )}
              {!narrow ? (
                <p className="p-omni__keys">
                  <span>
                    <kbd className="p-kbd">↑</kbd>
                    <kbd className="p-kbd">↓</kbd> choose
                  </span>
                  <span>
                    <kbd className="p-kbd">↵</kbd> do it
                  </span>
                  <span>
                    <kbd className="p-kbd">q</kbd> add
                  </span>
                  <span>
                    <kbd className="p-kbd">esc</kbd> close
                  </span>
                </p>
              ) : null}
            </div>,
            document.body,
          )
        : null}
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
        <span className="p-omni__kind">{KIND[o.hit.kind]}</span>
        <span className="p-omni__main">
          <span className="p-omni__title">{o.hit.title}</span>
          {o.hit.detail ? <span className="p-omni__detail">{o.hit.detail}</span> : null}
        </span>
        <span className="p-omni__verb">Open</span>
      </>
    );
  } else if (o.kind === "ask") {
    body = (
      <>
        <span className="p-omni__kind is-agent">Ask</span>
        <span className="p-omni__main">
          <span className="p-omni__title">“{o.text}”</span>
          <span className="p-omni__detail">The agent answers on your phone</span>
        </span>
        <span className="p-omni__verb">Ask</span>
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
        <span className="p-omni__kind is-add">Add</span>
        <span className="p-omni__main">
          <span className="p-omni__title">{p.title || "…"}</span>
          <span className="p-omni__chips">{chips.length ? chips.map((c) => <span key={c} className="p-tag">{c}</span>) : <span className="p-omni__detail">A task with no date</span>}</span>
        </span>
        <span className="p-omni__verb">Add task</span>
      </>
    );
  }
  return (
    <button type="button" role="option" aria-selected={on} aria-disabled={off || undefined} className={cx("p-omni__opt", on && "is-hi", off && "is-off")} onMouseMove={onHover} onClick={onPick} title={off ? STALE : undefined}>
      {body}
    </button>
  );
}
