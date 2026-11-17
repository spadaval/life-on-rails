// Queue's components: buttons, the checkbox, the floating layer (a popover on a wide screen, a
// sheet at phone width), the label picker the timer and the day strip share, the toast, and a
// page reader. Standard controls throughout: real buttons, inputs and selects.
import MarkdownIt from "markdown-it";
import { type ReactNode, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { pickable } from "../../data/derive.js";
import { useStore } from "../../data/store.js";
import { dismissToast, qhref, useToast } from "./lib.js";

export function cx(...c: (string | false | null | undefined)[]) {
  return c.filter(Boolean).join(" ");
}

/** True below 720px, where floating layers become sheets and actions stay visible. */
export function useNarrow(): boolean {
  const q = "(max-width: 719px)";
  const [narrow, set] = useState(() => matchMedia(q).matches);
  useEffect(() => {
    const m = matchMedia(q);
    const on = () => set(m.matches);
    m.addEventListener("change", on);
    return () => m.removeEventListener("change", on);
  }, []);
  return narrow;
}

type Tone = "primary" | "quiet" | "ghost" | "danger";
export function Button({
  tone = "quiet",
  size,
  children,
  className,
  ...rest
}: { tone?: Tone; size?: "sm" } & React.ComponentProps<"button">) {
  return (
    <button type="button" className={cx("q-btn", `q-btn--${tone}`, size === "sm" && "q-btn--sm", className)} {...rest}>
      {children}
    </button>
  );
}

export function Check({ checked, onChange, label, disabled, title }: { checked: boolean; onChange: (v: boolean) => void; label: string; disabled?: boolean; title?: string }) {
  return (
    <label className={cx("q-check", disabled && "is-disabled")} title={title}>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} aria-label={label} />
      <span aria-hidden="true" className="q-check__box">
        <svg viewBox="0 0 16 16">
          <path d="M3.5 8.5l3 3 6-7" />
        </svg>
      </span>
    </label>
  );
}

/** The "now" reticle the lead card wears. */
export function Reticle({ live }: { live?: boolean }) {
  return (
    <svg className={cx("q-reticle", live && "is-live")} viewBox="0 0 40 40" aria-hidden="true">
      <circle cx="20" cy="20" r="15" />
      <path d="M20 1v8M20 31v8M1 20h8M31 20h8" />
      <circle className="q-reticle__dot" cx="20" cy="20" r="2.5" />
    </svg>
  );
}

export function Icon({ name }: { name: "search" | "more" | "chev" | "left" | "right" | "close" | "plus" | "spark" | "play" | "stop" | "swap" }) {
  const d: Record<string, ReactNode> = {
    search: <path d="M11 11l3.5 3.5M7 12.5a5.5 5.5 0 110-11 5.5 5.5 0 010 11z" />,
    more: (
      <>
        <circle cx="3.5" cy="8" r="1.2" />
        <circle cx="8" cy="8" r="1.2" />
        <circle cx="12.5" cy="8" r="1.2" />
      </>
    ),
    chev: <path d="M4 6l4 4 4-4" />,
    left: <path d="M10 3.5L5.5 8l4.5 4.5" />,
    right: <path d="M6 3.5L10.5 8 6 12.5" />,
    close: <path d="M4 4l8 8M12 4l-8 8" />,
    plus: <path d="M8 3v10M3 8h10" />,
    spark: <path d="M8 2v3M8 11v3M2 8h3M11 8h3M4 4l1.8 1.8M10.2 10.2L12 12M12 4l-1.8 1.8M5.8 10.2L4 12" />,
    play: <path d="M5 3.5v9l7-4.5z" />,
    stop: <rect x="4" y="4" width="8" height="8" rx="1.5" />,
    swap: <path d="M3 5.5h9l-2.5-2.5M13 10.5H4l2.5 2.5" />,
  };
  return (
    <svg className={`q-icon q-icon--${name}`} viewBox="0 0 16 16" aria-hidden="true">
      {d[name]}
    </svg>
  );
}

/**
 * A floating layer anchored to an element: a popover on a wide screen, a sheet from the bottom
 * at phone width. Escape or a click outside closes it. Rendered on <body>, so no ancestor clips it.
 */
export function Floating({
  anchor,
  onClose,
  label,
  children,
  width = 360,
}: {
  /** The element it hangs from, or a getter for one that may not be drawn yet (a layer opened by a link). */
  anchor: HTMLElement | null | (() => HTMLElement | null);
  onClose: () => void;
  label: string;
  children: ReactNode;
  width?: number;
}) {
  const narrow = useNarrow();
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const get = useCallback(() => (typeof anchor === "function" ? anchor() : anchor), [anchor]);
  useLayoutEffect(() => {
    if (narrow) return;
    let frame = 0;
    const place = () => {
      const el = get();
      if (!el) {
        frame = requestAnimationFrame(place);
        return;
      }
      const r = el.getBoundingClientRect();
      const h = ref.current?.offsetHeight ?? 300;
      const left = Math.min(Math.max(12, r.left + r.width / 2 - width / 2), innerWidth - width - 12);
      const below = r.bottom + 8;
      const top = below + h > innerHeight - 12 && r.top - h - 8 > 12 ? r.top - h - 8 : below;
      setPos({ top, left });
    };
    place();
    addEventListener("resize", place);
    addEventListener("scroll", place, true);
    return () => {
      cancelAnimationFrame(frame);
      removeEventListener("resize", place);
      removeEventListener("scroll", place, true);
    };
  }, [get, narrow, width]);
  useEffect(() => {
    const key = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    const down = (e: PointerEvent) => {
      const t = e.target as Node;
      if (ref.current?.contains(t) || get()?.contains(t)) return;
      onClose();
    };
    addEventListener("keydown", key);
    addEventListener("pointerdown", down, true);
    return () => {
      removeEventListener("keydown", key);
      removeEventListener("pointerdown", down, true);
    };
  }, [onClose, get]);
  return createPortal(
    <>
      {narrow ? <div className="q-scrim" aria-hidden="true" /> : null}
      <div
        ref={ref}
        role="dialog"
        aria-label={label}
        className={cx("q-float", narrow ? "q-float--sheet" : "q-float--pop")}
        style={narrow ? undefined : { top: pos?.top ?? -9999, left: pos?.left ?? -9999, width }}
      >
        {narrow ? <div className="q-float__grip" aria-hidden="true" /> : null}
        {children}
      </div>
    </>,
    document.body,
  );
}

/**
 * Pick a label: what was used lately as chips, and a field that finds any goal, project, habit,
 * task or system label. `extra` puts a choice before them (the agent's suggestion, or "No label").
 */
export function LabelPicker({
  onPick,
  current,
  extra,
  autoFocus = true,
}: {
  onPick: (id: string | null, title: string) => void;
  current?: string | null;
  extra?: ReactNode;
  autoFocus?: boolean;
}) {
  const s = useStore();
  const [q, setQ] = useState("");
  const [hi, setHi] = useState(0);
  const all = useMemo(() => pickable(s), [s]);
  const recent = s.recent.filter((r) => r.id !== current).slice(0, 4);
  const found = q.trim() ? all.filter((x) => x.title.toLowerCase().includes(q.trim().toLowerCase())).slice(0, 7) : [];
  return (
    <div className="q-picker">
      {extra}
      {recent.length ? (
        <div className="q-picker__recent" role="group" aria-label="Recent labels">
          {recent.map((r) => (
            <button key={r.id} type="button" className="q-chip" onClick={() => onPick(r.id, r.title)}>
              {r.title}
            </button>
          ))}
        </div>
      ) : null}
      <input
        className="q-input"
        autoFocus={autoFocus}
        placeholder="Find a goal, project or task"
        value={q}
        onChange={(e) => {
          setQ(e.target.value);
          setHi(0);
        }}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") (e.preventDefault(), setHi((h) => Math.min(h + 1, found.length - 1)));
          if (e.key === "ArrowUp") (e.preventDefault(), setHi((h) => Math.max(h - 1, 0)));
          if (e.key === "Enter" && found[hi]) onPick(found[hi]!.id, found[hi]!.title);
        }}
        aria-label="Find a label"
      />
      {found.length ? (
        <ul className="q-picker__list" role="listbox">
          {found.map((x, i) => (
            <li key={x.id}>
              <button type="button" role="option" aria-selected={i === hi} className={cx("q-option", i === hi && "is-hi")} onMouseEnter={() => setHi(i)} onClick={() => onPick(x.id, x.title)}>
                <span>{x.title}</span>
                <span className="q-option__kind">{x.type === "system" ? "label" : x.type}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : q.trim() ? (
        <p className="q-picker__none">Nothing called “{q.trim()}”.</p>
      ) : null}
    </div>
  );
}

export function ToastView() {
  const t = useToast();
  if (!t) return null;
  return (
    <div className={cx("q-toast", t.error && "is-error")} role="status" aria-live="polite" key={t.id}>
      <span className="q-toast__text">{t.text}</span>
      {t.action ? (
        <button type="button" className="q-toast__btn" onClick={() => (t.action!.run(), dismissToast())}>
          {t.action.label}
        </button>
      ) : null}
      {t.undo ? (
        <button type="button" className="q-toast__btn q-toast__btn--undo" onClick={t.undo}>
          Undo
        </button>
      ) : null}
      <button type="button" className="q-toast__x" aria-label="Dismiss" onClick={dismissToast}>
        <Icon name="close" />
      </button>
    </div>
  );
}

// Raw HTML off; only links inside the prototype stay links.
const md = new MarkdownIt({ html: false, linkify: false, breaks: false });
md.validateLink = (url: string) => url.startsWith("#");

/** A page's markdown, with [[slug]] links opening the page in Notes. */
export function Prose({ source }: { source: string }) {
  const html = useMemo(() => {
    const body = source.replace(/\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/g, (_m, slug: string, label?: string) => `[${label ?? slug}](${qhref("/notes", { id: slug.trim() })})`);
    return md.render(body.replace(/\[\^[^\]]+\]/g, ""));
  }, [source]);
  return <div className="q-prose" dangerouslySetInnerHTML={{ __html: html }} />;
}
