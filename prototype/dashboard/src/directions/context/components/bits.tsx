// Small pieces every screen uses: icons, the checkbox, the notice, page prose.
import { type ReactNode } from "react";
import { dismiss, useNotice } from "../lib/ui.js";

const paths: Record<string, ReactNode> = {
  now: <><circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="3" /></>,
  tasks: <><rect x="4" y="4" width="16" height="16" rx="4.5" /><path d="m8.5 12 2.5 2.5 4.5-5" /></>,
  goal: <><circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="4.5" /><circle cx="12" cy="12" r="1" /></>,
  project: <><path d="M4 7.5a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" /></>,
  person: <><circle cx="12" cy="8.5" r="3.8" /><path d="M5 20c1-3.6 3.8-5.5 7-5.5s6 1.9 7 5.5" /></>,
  people: <><circle cx="9" cy="9" r="3.5" /><path d="M3.5 19c.8-3 3-4.5 5.5-4.5s4.7 1.5 5.5 4.5" /><path d="M15.5 5.8a3.3 3.3 0 0 1 0 6.4M17.5 14.8c1.4.6 2.5 2 3 4.2" /></>,
  notes: <><path d="M6 3.5h8l4 4V20a.5.5 0 0 1-.5.5h-11A.5.5 0 0 1 6 20z" /><path d="M14 3.5V8h4M9 12.5h6M9 16h6" /></>,
  loose: <><circle cx="7" cy="7" r="2.5" /><circle cx="17" cy="9" r="2.5" /><circle cx="10" cy="17" r="2.5" /></>,
  slot: <><rect x="4" y="5.5" width="16" height="14.5" rx="3" /><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4" /></>,
  search: <><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></>,
  play: <path d="M8.5 6v12l10-6z" />,
  stop: <rect x="7" y="7" width="10" height="10" rx="2" />,
  switch: <><path d="M5 8h12l-3.5-3.5M19 16H7l3.5 3.5" /></>,
  plus: <path d="M12 5.5v13M5.5 12h13" />,
  check: <path d="m5.5 12.5 4.5 4.5 8.5-9.5" />,
  close: <path d="M6.5 6.5l11 11M17.5 6.5l-11 11" />,
  chevron: <path d="m9 6 6 6-6 6" />,
  down: <path d="m6 9 6 6 6-6" />,
  flag: <><path d="M6 21V4" /><path d="M6 4.5h11l-2.5 4 2.5 4H6" /></>,
  spark: <path d="M12 3.5 13.8 10l6.7 2-6.7 2L12 20.5 10.2 14 3.5 12l6.7-2z" />,
  agent: <><rect x="5" y="7" width="14" height="11" rx="3.5" /><path d="M12 4v3M9.5 12h.01M14.5 12h.01" /></>,
  cloud: <><path d="M7 18.5a4.5 4.5 0 0 1-.6-9A6 6 0 0 1 18 10a4.3 4.3 0 0 1-.5 8.5z" /><path d="m4 4 16 16" /></>,
  calendar: <><rect x="4" y="5.5" width="16" height="14.5" rx="3" /><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4" /></>,
  clock: <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>,
  phone: <><rect x="7" y="3" width="10" height="18" rx="2.5" /><path d="M11 17.5h2" /></>,
  mail: <><rect x="3.5" y="5.5" width="17" height="13" rx="2.5" /><path d="m4 7 8 6 8-6" /></>,
  message: <path d="M4.5 6.5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H10l-4.5 3.5v-3.5h0a1 1 0 0 1-1-1z" />,
  out: <><path d="M14 4.5h5.5V10M19.5 4.5 11 13" /><path d="M17 14v4a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 5 18V8.5A1.5 1.5 0 0 1 6.5 7H10" /></>,
  link: <><path d="M10 14a3.5 3.5 0 0 0 5 0l3-3a3.5 3.5 0 0 0-5-5l-1 1" /><path d="M14 10a3.5 3.5 0 0 0-5 0l-3 3a3.5 3.5 0 0 0 5 5l1-1" /></>,
  star: <path d="m12 4 2.4 4.9 5.4.8-3.9 3.8.9 5.4L12 16.4l-4.8 2.5.9-5.4-3.9-3.8 5.4-.8z" />,
  undo: <><path d="M9 7 5 11l4 4" /><path d="M5 11h9a5 5 0 0 1 0 10h-2" /></>,
  more: <><circle cx="6" cy="12" r="1.3" /><circle cx="12" cy="12" r="1.3" /><circle cx="18" cy="12" r="1.3" /></>,
  ask: <><path d="M4.5 6.5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H10l-4.5 3.5v-3.5h0a1 1 0 0 1-1-1z" /><path d="M10 9.2a2 2 0 1 1 2.6 1.9c-.4.2-.6.5-.6.9v.2M12 14h.01" /></>,
};

export function Icon({ name, size = 18 }: { name: string; size?: number }) {
  return (
    <svg className="cx-icon" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name] ?? null}
    </svg>
  );
}

export const kindIcon = (k: string) => ({ project: "project", goal: "goal", person: "person", slot: "slot", loose: "loose", unnamed: "clock", label: "clock" })[k] ?? "now";

/** The one checkbox: a real input, drawn round. */
export function Check({ checked, onChange, label, disabled, title }: { checked: boolean; onChange: (v: boolean) => void; label: string; disabled?: boolean; title?: string }) {
  return (
    <label className="cx-check" title={title} data-disabled={disabled || undefined}>
      <input type="checkbox" checked={checked} disabled={disabled} onChange={(e) => onChange(e.target.checked)} aria-label={label} />
      <span aria-hidden="true">
        <Icon name="check" size={14} />
      </span>
    </label>
  );
}

/** The notice: what just happened, with Undo only where it can be taken back. */
export function NoticeBar() {
  const n = useNotice();
  if (!n) return null;
  return (
    <div className="cx-notice" role="status" data-tone={n.tone} key={n.id}>
      <p>{n.text}</p>
      {n.undo ? (
        <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" onClick={n.undo}>
          <Icon name="undo" size={15} /> Undo
        </button>
      ) : null}
      {n.cancel ? (
        <button type="button" className="cx-btn cx-btn-sm cx-btn-ghost" onClick={n.cancel}>
          Cancel
        </button>
      ) : null}
      <button type="button" className="cx-iconbtn cx-iconbtn-sm" onClick={dismiss} aria-label="Dismiss">
        <Icon name="close" size={14} />
      </button>
    </div>
  );
}

/** A page's markdown, as much of it as the notes use: headings, lists, bold, [[links]]. */
export function Prose({ body, onLink, limit }: { body: string; onLink?: (slug: string) => void; limit?: number }) {
  const lines = body.split("\n");
  const shown = limit ? lines.filter((l) => l.trim()).slice(0, limit) : lines;
  const out: ReactNode[] = [];
  let list: ReactNode[] = [];
  const flush = (k: number) => {
    if (list.length) out.push(<ul key={`u${k}`}>{list}</ul>);
    list = [];
  };
  shown.forEach((raw, i) => {
    const l = raw.trimEnd();
    if (/^\s*[-*] /.test(l)) {
      list.push(<li key={i}>{inline(l.replace(/^\s*[-*] /, ""), onLink)}</li>);
      return;
    }
    flush(i);
    if (!l.trim()) return;
    const h = /^(#{1,3}) (.*)$/.exec(l);
    if (h) out.push(<h4 key={i}>{h[2]}</h4>);
    else out.push(<p key={i}>{inline(l, onLink)}</p>);
  });
  flush(shown.length);
  return <div className="cx-prose">{out}</div>;
}

function inline(text: string, onLink?: (slug: string) => void): ReactNode[] {
  const parts: ReactNode[] = [];
  const re = /\[\[([^\]|]+)(?:\|([^\]]*))?\]\]|\*\*([^*]+)\*\*/g;
  let last = 0;
  let m: RegExpExecArray | null;
  let k = 0;
  while ((m = re.exec(text))) {
    if (m.index > last) parts.push(text.slice(last, m.index));
    if (m[3]) parts.push(<strong key={k++}>{m[3]}</strong>);
    else {
      const slug = m[1]!.trim();
      const label = m[2] ?? slug.split("/").pop()!.replace(/-/g, " ");
      parts.push(
        onLink ? (
          <button key={k++} type="button" className="cx-wikilink" onClick={() => onLink(slug)}>
            {label}
          </button>
        ) : (
          <span key={k++} className="cx-wikilink">
            {label}
          </span>
        ),
      );
    }
    last = m.index + m[0].length;
  }
  if (last < text.length) parts.push(text.slice(last));
  return parts;
}
