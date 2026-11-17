// Panes' components: buttons, the checkbox, icons, a floating layer (a popover on a wide screen,
// a sheet at phone width), the label picker, the toast, and a page reader. Standard controls
// throughout: real buttons, inputs, selects and checkboxes, each with hover, focus, pressed and
// disabled states in styles.css.
import MarkdownIt from "markdown-it";
import { type ReactNode, useCallback, useLayoutEffect, useMemo, useRef, useState, forwardRef, useEffect } from "react";
import { createPortal } from "react-dom";
import { pickable } from "../../data/derive.js";
import { useStore } from "../../data/store.js";
import { dismissToast, phref, useFit, useLayer, useToast } from "./lib.js";

export function cx(...c: (string | false | null | undefined)[]) {
  return c.filter(Boolean).join(" ");
}

type Tone = "primary" | "quiet" | "ghost" | "agent";
export const Button = forwardRef<HTMLButtonElement, { tone?: Tone; size?: "sm" } & React.ComponentProps<"button">>(function Button(
  { tone = "quiet", size, children, className, ...rest },
  ref,
) {
  return (
    <button ref={ref} type="button" className={cx("p-btn", `p-btn--${tone}`, size === "sm" && "p-btn--sm", className)} {...rest}>
      {children}
    </button>
  );
});

export function Check({ checked, onChange, label, disabled, title }: { checked: boolean; onChange: (v: boolean) => void; label: string; disabled?: boolean; title?: string }) {
  return (
    <label className={cx("p-check", disabled && "is-disabled")} title={title} onClick={(e) => e.stopPropagation()}>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} aria-label={label} />
      <span aria-hidden="true" className="p-check__box">
        <svg viewBox="0 0 16 16">
          <path d="M3.5 8.5l3 3 6-7" />
        </svg>
      </span>
    </label>
  );
}

export type IconName =
  | "search" | "more" | "chev" | "left" | "right" | "close" | "plus" | "spark" | "play" | "stop" | "swap"
  | "today" | "tasks" | "goals" | "people" | "notes" | "flag" | "clock" | "cal" | "expand" | "collapse" | "back" | "brief";
export function Icon({ name }: { name: IconName }) {
  const d: Record<IconName, ReactNode> = {
    search: <path d="M11 11l3.5 3.5M7 12.5a5.5 5.5 0 110-11 5.5 5.5 0 010 11z" />,
    more: (
      <>
        <circle cx="3.5" cy="8" r="1.1" />
        <circle cx="8" cy="8" r="1.1" />
        <circle cx="12.5" cy="8" r="1.1" />
      </>
    ),
    chev: <path d="M4 6l4 4 4-4" />,
    left: <path d="M10 3.5L5.5 8l4.5 4.5" />,
    right: <path d="M6 3.5L10.5 8 6 12.5" />,
    back: <path d="M13 8H3.5M7.5 4L3.5 8l4 4" />,
    close: <path d="M4 4l8 8M12 4l-8 8" />,
    plus: <path d="M8 3v10M3 8h10" />,
    spark: <path d="M8 2v3M8 11v3M2 8h3M11 8h3M4 4l1.8 1.8M10.2 10.2L12 12M12 4l-1.8 1.8M5.8 10.2L4 12" />,
    play: <path d="M5 3.5v9l7-4.5z" />,
    stop: <rect x="4" y="4" width="8" height="8" rx="1.5" />,
    swap: <path d="M3 5.5h9l-2.5-2.5M13 10.5H4l2.5 2.5" />,
    today: (
      <>
        <circle cx="8" cy="8" r="5.5" />
        <path d="M8 5v3l2 1.5" />
      </>
    ),
    tasks: <path d="M2.5 4.5l1.5 1.5 2.5-3M8.5 4.5h5M2.5 10.5l1.5 1.5 2.5-3M8.5 10.5h5" />,
    goals: (
      <>
        <circle cx="8" cy="8" r="5.5" />
        <circle cx="8" cy="8" r="2" />
      </>
    ),
    people: (
      <>
        <circle cx="6" cy="5.5" r="2.3" />
        <path d="M1.8 13c.6-2.3 2.2-3.5 4.2-3.5s3.6 1.2 4.2 3.5M10.5 3.6a2.2 2.2 0 010 4M12 9.8c1.2.5 2 1.5 2.3 3.2" />
      </>
    ),
    notes: <path d="M4 2.5h5.5L12.5 5.5V13.5H4zM9.5 2.5V5.5h3M6 8.5h4.5M6 11h4.5" />,
    flag: <path d="M4 14V2.5M4 3h7.5l-1.8 3 1.8 3H4" />,
    clock: (
      <>
        <circle cx="8" cy="8" r="5.5" />
        <path d="M8 5v3h2.5" />
      </>
    ),
    cal: <path d="M3 4h10v9.5H3zM3 7h10M5.5 2.5v3M10.5 2.5v3" />,
    expand: <path d="M4 10l4-4 4 4" />,
    collapse: <path d="M4 6l4 4 4-4" />,
    brief: <path d="M3 3.5h10M3 6.5h10M3 9.5h7M3 12.5h5" />,
  };
  return (
    <svg className={`p-icon p-icon--${name}`} viewBox="0 0 16 16" aria-hidden="true">
      {d[name]}
    </svg>
  );
}

/**
 * A floating layer anchored to an element: a popover from 720px up, a sheet from the bottom
 * below that. Escape (when it is the layer in front) or a click outside closes it. Rendered on
 * <body>, so no pane's blur or clipping reaches it.
 */
export function Floating({
  anchor,
  onClose,
  label,
  children,
  width = 360,
}: {
  anchor: HTMLElement | null | (() => HTMLElement | null);
  onClose: () => void;
  label: string;
  children: ReactNode;
  width?: number;
}) {
  const narrow = useFit() === "narrow";
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const get = useCallback(() => (typeof anchor === "function" ? anchor() : anchor), [anchor]);
  useLayer(true, onClose);
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
      const h = ref.current?.offsetHeight ?? 320;
      const left = Math.min(Math.max(12, r.left + r.width / 2 - width / 2), innerWidth - width - 12);
      const below = r.bottom + 8;
      const top = below + h > innerHeight - 12 && r.top - h - 8 > 12 ? r.top - h - 8 : Math.min(below, innerHeight - h - 12);
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
    const down = (e: PointerEvent) => {
      const t = e.target as Node;
      if (ref.current?.contains(t) || get()?.contains(t)) return;
      onClose();
    };
    addEventListener("pointerdown", down, true);
    return () => removeEventListener("pointerdown", down, true);
  }, [onClose, get]);
  return createPortal(
    <>
      {narrow ? <div className="p-scrim" aria-hidden="true" /> : null}
      <div
        ref={ref}
        role="dialog"
        aria-label={label}
        className={cx("p-float", narrow ? "p-float--sheet" : "p-float--pop")}
        style={narrow ? undefined : { top: pos?.top ?? -9999, left: pos?.left ?? -9999, width }}
      >
        {narrow ? <div className="p-float__grip" aria-hidden="true" /> : null}
        {children}
      </div>
    </>,
    document.body,
  );
}

/**
 * Pick a label: recent ones as chips, and a field that finds any goal, project, habit, task or
 * system label. `extra` puts a choice first (the agent's guess, or "Start without a label").
 */
export function LabelPicker({
  onPick,
  current,
  extra,
  autoFocus = true,
  disabled,
}: {
  onPick: (id: string | null, title: string) => void;
  current?: string | null;
  extra?: ReactNode;
  autoFocus?: boolean;
  disabled?: boolean;
}) {
  const s = useStore();
  const [q, setQ] = useState("");
  const [hi, setHi] = useState(0);
  const all = useMemo(() => pickable(s), [s]);
  const recent = s.recent.filter((r) => r.id !== current).slice(0, 5);
  const found = q.trim() ? all.filter((x) => x.title.toLowerCase().includes(q.trim().toLowerCase())).slice(0, 7) : [];
  return (
    <fieldset className="p-picker" disabled={disabled}>
      {extra}
      {recent.length ? (
        <div className="p-picker__recent" role="group" aria-label="Recent labels">
          {recent.map((r) => (
            <button key={r.id} type="button" className="p-chip" onClick={() => onPick(r.id, r.title)}>
              {r.title}
            </button>
          ))}
        </div>
      ) : null}
      <input
        className="p-input"
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
        <ul className="p-picker__list" role="listbox" aria-label="Labels">
          {found.map((x, i) => (
            <li key={x.id}>
              <button type="button" role="option" aria-selected={i === hi} className={cx("p-option", i === hi && "is-hi")} onMouseEnter={() => setHi(i)} onClick={() => onPick(x.id, x.title)}>
                <span>{x.title}</span>
                <span className="p-option__kind">{x.type === "system" ? "label" : x.type}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : q.trim() ? (
        <p className="p-picker__none">Nothing called “{q.trim()}”.</p>
      ) : null}
    </fieldset>
  );
}

export function ToastView() {
  const t = useToast();
  if (!t) return null;
  return createPortal(
    <div className={cx("p-toast", t.error && "is-error")} role="status" aria-live="polite" key={t.id}>
      <span className="p-toast__text">{t.text}</span>
      {t.action ? (
        <button type="button" className="p-toast__btn" onClick={() => (t.action!.run(), dismissToast())}>
          {t.action.label}
        </button>
      ) : null}
      {t.undo ? (
        <button type="button" className="p-toast__btn p-toast__btn--undo" onClick={t.undo}>
          Undo
        </button>
      ) : null}
      <button type="button" className="p-toast__x" aria-label="Dismiss" onClick={dismissToast}>
        <Icon name="close" />
      </button>
    </div>,
    document.body,
  );
}

// Raw HTML off; only links inside the prototype stay links.
const md = new MarkdownIt({ html: false, linkify: false, breaks: false });
md.validateLink = (url: string) => url.startsWith("#");

/** A page's markdown, with [[slug]] links opening the page in Notes. */
export function Prose({ source }: { source: string }) {
  const html = useMemo(() => {
    const body = source.replace(/\[\[([^\]|]+)(?:\|([^\]]+))?\]\]/g, (_m, slug: string, label?: string) => `[${label ?? slug}](${phref("/notes", { id: slug.trim() })})`);
    return md.render(body.replace(/\[\^[^\]]+\]/g, ""));
  }, [source]);
  return <div className="p-prose" dangerouslySetInnerHTML={{ __html: html }} />;
}

/** A group in a list: a small label with a count, and its rows. */
export function Group({ label, count, children, aside, id }: { label: string; count?: number; children: ReactNode; aside?: ReactNode; id?: string }) {
  const hid = id ?? `g-${label.toLowerCase().replace(/[^a-z]+/g, "-")}`;
  return (
    <section className="p-group" aria-labelledby={hid}>
      <h3 className="p-group__label" id={hid}>
        <span>{label}</span>
        {count !== undefined ? <span className="p-count">{count}</span> : null}
        {aside ? <span className="p-group__aside">{aside}</span> : null}
      </h3>
      {children}
    </section>
  );
}
