// Quick add, after Todoist's: type the task in one line and the parts light up
// as they are understood. `#` what it counts toward, `@agent` or `@person`,
// and a date in words. Press `q` on any screen; inline at the top of Tasks.
import { type ReactNode, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "@tanstack/react-router";
import { getQuickAddFn, saveIntentFn } from "../server/functions/graph.js";
import { type Named, type Token, activeSigil, parseQuickAdd, suggest, tokenKey } from "../shared/quickAdd.js";
import { longDate } from "./format.js";

export interface QuickAddOptions {
  today: string;
  parents: (Named & { type: string })[];
  people: Named[];
}

const AGENT: Named = { id: "agent", name: "agent" };

const MARK: Record<Token["kind"], string> = {
  parent: "bg-or/25",
  due: "bg-or/25",
  agent: "bg-cy/25",
  waiting: "bg-cy/25",
};

function chipText(t: Token): ReactNode {
  if (t.kind === "parent") return <>For <b className="font-medium text-txt">{t.name}</b></>;
  if (t.kind === "due") return <>Due <b className="font-medium text-txt">{longDate(t.ymd)}</b></>;
  if (t.kind === "agent") return <b className="font-medium text-cy">With the agent</b>;
  return <>Waiting on <b className="font-medium text-txt">{t.name}</b></>;
}

export function QuickAdd({ options, autoFocus = false, onEscape, onAdded }: {
  options: QuickAddOptions;
  autoFocus?: boolean;
  onEscape?: () => void;
  onAdded?: (title: string) => void;
}) {
  const router = useRouter();
  const input = useRef<HTMLInputElement>(null);
  const overlay = useRef<HTMLDivElement>(null);
  const [text, setText] = useState("");
  const [caret, setCaret] = useState(0);
  const [ignore, setIgnore] = useState<ReadonlySet<string>>(new Set());
  const [pick, setPick] = useState(0);
  const [closed, setClosed] = useState(false);
  const [focused, setFocused] = useState(autoFocus);
  const [busy, setBusy] = useState(false);
  const [said, setSaid] = useState<string | null>(null);

  const parsed = useMemo(() => parseQuickAdd(text, { ...options, ignore }), [text, options, ignore]);
  const active = activeSigil(text, caret);
  const choices = active ? suggest(active.query, active.sigil === "#" ? options.parents : [AGENT, ...options.people]) : [];
  const showList = focused && !closed && choices.length > 0;

  // After a suggestion is chosen the caret goes after it, before the next key arrives.
  const caretTo = useRef<number | null>(null);
  useLayoutEffect(() => {
    if (caretTo.current !== null && input.current) {
      input.current.setSelectionRange(caretTo.current, caretTo.current);
      caretTo.current = null;
    }
    if (overlay.current && input.current) overlay.current.scrollLeft = input.current.scrollLeft;
  });

  const edit = (value: string, at: number) => {
    setText(value);
    setCaret(at);
    setClosed(false);
    setPick(0);
    setSaid(null);
  };

  const choose = (n: Named) => {
    if (!active) return;
    const insert = `${active.sigil}${n.name} `;
    const value = text.slice(0, active.start) + insert + text.slice(caret).replace(/^\S*\s?/, "");
    const at = active.start + insert.length;
    caretTo.current = at;
    edit(value, at);
  };

  // The field clears at once so the next task can be typed while this one saves.
  const submit = async () => {
    if (!parsed.title) return;
    const sent = { text, ignore, parsed };
    edit("", 0);
    setIgnore(new Set());
    setBusy(true);
    try {
      await saveIntentFn({
        data: {
          type: "task",
          title: sent.parsed.title,
          parentId: sent.parsed.parentId,
          dueAt: sent.parsed.due,
          waitingOnEntityId: sent.parsed.waitingOnId,
          assignee: sent.parsed.assignee,
          weight: 1,
        },
      });
      setSaid(`Added “${sent.parsed.title}”.`);
      onAdded?.(sent.parsed.title);
      await router.invalidate();
    } catch (err) {
      setText((now) => (now ? now : sent.text));
      setIgnore(sent.ignore);
      setSaid(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  };

  // The input sits on a copy of its own text in which the understood parts are marked.
  let at = 0;
  const marked: ReactNode[] = [];
  for (const t of parsed.tokens) {
    marked.push(text.slice(at, t.start), <mark key={t.start} className={`rounded-[2px] text-transparent ${MARK[t.kind]}`}>{text.slice(t.start, t.end)}</mark>);
    at = t.end;
  }
  marked.push(text.slice(at));

  return (
    <div className="relative">
      <div className={`relative border bg-well ${focused ? "border-or/70" : "border-edge"}`}>
        <div ref={overlay} aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden px-3 py-2 text-[15px] whitespace-pre text-transparent">
          {marked}
        </div>
        <input
          ref={input}
          className="relative w-full bg-transparent px-3 py-2 text-[15px] text-txt outline-none placeholder:text-dim"
          placeholder="Add a task: Call the florist #Wedding vendors fri @agent"
          aria-label="Add a task"
          autoFocus={autoFocus}
          autoComplete="off"
          spellCheck={false}
          value={text}
          role="combobox"
          aria-expanded={showList}
          aria-controls="quick-add-choices"
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          onChange={(e) => edit(e.target.value, e.target.selectionStart ?? e.target.value.length)}
          onSelect={(e) => setCaret(e.currentTarget.selectionStart ?? 0)}
          onKeyDown={(e) => {
            if (showList && (e.key === "ArrowDown" || e.key === "ArrowUp")) {
              e.preventDefault();
              setPick((p) => (p + (e.key === "ArrowDown" ? 1 : choices.length - 1)) % choices.length);
            } else if (showList && (e.key === "Enter" || e.key === "Tab")) {
              e.preventDefault();
              choose(choices[Math.min(pick, choices.length - 1)]!);
            } else if (e.key === "Escape") {
              e.preventDefault();
              if (showList) setClosed(true);
              else if (text) edit("", 0);
              else onEscape?.();
            } else if (e.key === "Enter") {
              e.preventDefault();
              void submit();
            }
          }}
        />
      </div>

      {showList ? (
        <ul id="quick-add-choices" role="listbox" className="absolute inset-x-0 top-full z-30 mt-1 border border-edge bg-raised py-1 shadow-xl shadow-black/40">
          {choices.map((n, i) => {
            const type = options.parents.find((p) => p.id === n.id)?.type;
            return (
              <li
                key={n.id}
                role="option"
                aria-selected={i === pick}
                onMouseDown={(e) => {
                  e.preventDefault();
                  choose(n);
                }}
                onMouseEnter={() => setPick(i)}
                className={`flex cursor-pointer items-baseline justify-between gap-3 px-3 py-1.5 text-[14px] ${i === pick ? "bg-or/10 text-txt" : "text-mute"}`}
              >
                <span className="truncate">{n.id === "agent" ? "the agent" : n.name}</span>
                <span className="shrink-0 text-[12px] text-dim">{type ?? (n.id === "agent" ? "do it for me" : "waiting on")}</span>
              </li>
            );
          })}
        </ul>
      ) : null}

      <div className="mt-1.5 flex min-h-6 flex-wrap items-center gap-1.5 text-[13px] text-mute">
        {parsed.tokens.map((t) => (
          <span key={tokenKey(t)} className="chip gap-1.5 text-[12.5px]">
            {chipText(t)}
            <button className="text-dim hover:text-txt" title="Not that: keep it as text" aria-label="Keep it as text" onClick={() => setIgnore(new Set([...ignore, tokenKey(t)]))}>
              ×
            </button>
          </span>
        ))}
        {said || busy ? <span className={busy ? "text-dim" : "text-txt"}>{busy ? "Adding…" : said}</span> : null}
        {parsed.tokens.length === 0 && !said && !busy ? (
          <span className="text-dim">
            <Key>#</Key> counts toward · <Key>@agent</Key> or <Key>@person</Key> · dates like <i>fri</i>, <i>tomorrow</i>, <i>oct 20</i>, <i>in 2 weeks</i> · <Key>Enter</Key> adds
          </span>
        ) : null}
      </div>
    </div>
  );
}

export function Key({ children }: { children: ReactNode }) {
  return <kbd className="border border-edge px-1 font-sans text-[11.5px] leading-[18px] text-mute not-italic">{children}</kbd>;
}

/** Quick add over any screen: `q` opens it, Escape closes it. It stays open for the next task. */
export function QuickAddDialog() {
  const [open, setOpen] = useState(false);
  const [options, setOptions] = useState<QuickAddOptions | null>(null);

  useEffect(() => {
    const show = () => setOpen(true);
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== "q" || e.metaKey || e.ctrlKey || e.altKey || typing(e.target)) return;
      e.preventDefault();
      show();
    };
    window.addEventListener("keydown", onKey);
    window.addEventListener("lor:quick-add", show);
    return () => {
      window.removeEventListener("keydown", onKey);
      window.removeEventListener("lor:quick-add", show);
    };
  }, []);

  useEffect(() => {
    if (open) void getQuickAddFn().then(setOptions);
  }, [open]);

  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/55 px-4 pt-[18vh]" onMouseDown={(e) => e.target === e.currentTarget && setOpen(false)}>
      <div className="panel w-full max-w-[720px]" role="dialog" aria-label="Add a task">
        <div className="face px-5 py-4">
          <h2 className="h-sec mb-2.5">Add a task</h2>
          {options ? <QuickAdd options={options} autoFocus onEscape={() => setOpen(false)} /> : <p className="py-2 text-dim">…</p>}
        </div>
      </div>
    </div>
  );
}

export const openQuickAdd = () => window.dispatchEvent(new Event("lor:quick-add"));

/** Whether a key press is going into a field, where single-key shortcuts must not fire. */
export function typing(target: EventTarget | null): boolean {
  const el = target as HTMLElement | null;
  return !!el && (el.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName));
}
