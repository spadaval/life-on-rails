// Small shared pieces: icons, a media hook, the checkbox, the notice at the bottom.
import { type ReactNode, useEffect, useState } from "react";
import { dismiss, useNotice } from "../lib/ui.js";

const paths: Record<string, ReactNode> = {
  today: <><circle cx="12" cy="12" r="8.5" /><path d="M12 7v5l3 2" /></>,
  tasks: <><rect x="4" y="4" width="16" height="16" rx="4" /><path d="m8.5 12 2.5 2.5 4.5-5" /></>,
  goals: <><circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="4.5" /><circle cx="12" cy="12" r="1" /></>,
  people: <><circle cx="9" cy="9" r="3.5" /><path d="M3.5 19c.8-3 3-4.5 5.5-4.5s4.7 1.5 5.5 4.5" /><path d="M15.5 5.8a3.3 3.3 0 0 1 0 6.4M17.5 14.8c1.4.6 2.5 2 3 4.2" /></>,
  notes: <><path d="M6 3.5h8l4 4V20a.5.5 0 0 1-.5.5h-11A.5.5 0 0 1 6 20z" /><path d="M14 3.5V8h4M9 12.5h6M9 16h6" /></>,
  search: <><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></>,
  play: <path d="M8 5.5v13l10.5-6.5z" />,
  stop: <rect x="6.5" y="6.5" width="11" height="11" rx="2" />,
  switch: <><path d="M5 8h12l-3.5-3.5M19 16H7l3.5 3.5" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  check: <path d="m5.5 12.5 4.5 4.5 8.5-9.5" />,
  close: <path d="M6.5 6.5l11 11M17.5 6.5l-11 11" />,
  chevron: <path d="m9 6 6 6-6 6" />,
  down: <path d="m6 9 6 6 6-6" />,
  back: <path d="m15 6-6 6 6 6" />,
  flag: <><path d="M6 21V4" /><path d="M6 4.5h11l-2.5 4 2.5 4H6" /></>,
  spark: <path d="M12 3.5 13.8 10l6.7 2-6.7 2L12 20.5 10.2 14 3.5 12l6.7-2z" />,
  cloud: <><path d="M7 18.5a4.5 4.5 0 0 1-.6-9A6 6 0 0 1 18 10a4.3 4.3 0 0 1-.5 8.5z" /><path d="m4 4 16 16" /></>,
  calendar: <><rect x="4" y="5.5" width="16" height="14.5" rx="3" /><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4" /></>,
  phone: <><rect x="7" y="3" width="10" height="18" rx="2.5" /><path d="M11 17.5h2" /></>,
  pin: <><path d="M12 21s6.5-6 6.5-11a6.5 6.5 0 0 0-13 0c0 5 6.5 11 6.5 11z" /><circle cx="12" cy="10" r="2.3" /></>,
  more: <><circle cx="6" cy="12" r="1.3" /><circle cx="12" cy="12" r="1.3" /><circle cx="18" cy="12" r="1.3" /></>,
  mail: <><rect x="3.5" y="5.5" width="17" height="13" rx="2.5" /><path d="m4 7 8 6 8-6" /></>,
  message: <path d="M4.5 6.5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H10l-4.5 3.5v-3.5h0a1 1 0 0 1-1-1z" />,
};

export function Icon({ name, size = 18 }: { name: keyof typeof paths | string; size?: number }) {
  return (
    <svg className="tl-icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] ?? null}
    </svg>
  );
}

/** True below `px` wide. */
export function useNarrow(px = 720): boolean {
  const q = `(max-width: ${px}px)`;
  const [m, setM] = useState(() => matchMedia(q).matches);
  useEffect(() => {
    const mq = matchMedia(q);
    const on = () => setM(mq.matches);
    mq.addEventListener("change", on);
    on();
    return () => mq.removeEventListener("change", on);
  }, [q]);
  return m;
}

export function Check({ checked, onChange, label, disabled, title }: { checked: boolean; onChange: (v: boolean) => void; label: string; disabled?: boolean; title?: string }) {
  return (
    <label className="tl-check" data-checked={checked || undefined} title={title}>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} aria-label={label} />
      <span aria-hidden="true">
        <Icon name="check" size={14} />
      </span>
    </label>
  );
}

/** The one notice, bottom centre: what changed, with Undo only where LoR can reverse it. */
export function NoticeBar() {
  const n = useNotice();
  if (!n) return null;
  return (
    <div className="tl-notice" role="status" data-tone={n.tone}>
      <span>{n.text}</span>
      {n.undo ? (
        <button type="button" className="tl-btn tl-btn-quiet" onClick={n.undo}>
          Undo <kbd>⌘Z</kbd>
        </button>
      ) : null}
      {n.cancel ? (
        <button type="button" className="tl-btn tl-btn-quiet" onClick={n.cancel}>
          Cancel
        </button>
      ) : null}
      <button type="button" className="tl-iconbtn" aria-label="Dismiss" onClick={dismiss}>
        <Icon name="close" size={16} />
      </button>
    </div>
  );
}

/** A little markdown: paragraphs, "- " bullets, **bold**, [[links]] shown as plain words. */
export function Prose({ text, className }: { text: string; className?: string }) {
  const blocks = text.trim().split(/\n\s*\n/);
  const inline = (s: string) =>
    s
      .replace(/\[\[([^\]]+)\]\]/g, (_m, p: string) => p.split("/").pop()!.replace(/-/g, " "))
      .split(/(\*\*[^*]+\*\*)/)
      .map((part, i) => (part.startsWith("**") ? <strong key={i}>{part.slice(2, -2)}</strong> : part));
  return (
    <div className={className ?? "tl-prose"}>
      {blocks.map((b, i) => {
        const lines = b.split("\n");
        if (lines.every((l) => /^\s*[-*] /.test(l)))
          return (
            <ul key={i}>
              {lines.map((l, j) => (
                <li key={j}>{inline(l.replace(/^\s*[-*] /, ""))}</li>
              ))}
            </ul>
          );
        if (/^#+ /.test(b)) return <h3 key={i}>{b.replace(/^#+ /, "")}</h3>;
        return <p key={i}>{inline(b.replace(/\n/g, " "))}</p>;
      })}
    </div>
  );
}
