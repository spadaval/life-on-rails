// The few shared pieces of the Mostly Harmless look: framed panels, page
// heads, notices, star glyphs. Colours and type live in styles.css.
import type { ReactNode } from "react";

export function Panel({ title, aside, children, className = "", bodyClass = "" }: {
  title?: ReactNode;
  aside?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClass?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      <div className={`face flex flex-col px-5 py-4 ${bodyClass}`}>
        {title ? (
          <header className="mb-2 flex items-baseline justify-between gap-3">
            <h2 className="h-sec">{title}</h2>
            {aside ? <span className="text-[13px] text-mute">{aside}</span> : null}
          </header>
        ) : null}
        {children}
      </div>
    </section>
  );
}

export function PageHead({ title, sub, children }: { title: ReactNode; sub?: ReactNode; children?: ReactNode }) {
  return (
    <header className="flex min-h-[62px] flex-wrap items-center gap-x-4 gap-y-2 border-b border-line py-3">
      <h1 className="cond text-[22px] font-semibold tracking-[0.08em]">{title}</h1>
      {sub ? <span className="text-[14px] text-mute">{sub}</span> : null}
      <div className="ml-auto flex flex-wrap items-center gap-2">{children}</div>
    </header>
  );
}

export function Notice({ text, onClose }: { text: string | null; onClose: () => void }) {
  if (!text) return null;
  return (
    <p role="status" className="row-sel flex items-center justify-between gap-3 px-3 py-2 text-[14px]">
      <span>{text}</span>
      <button className="text-[13px] text-mute underline hover:text-txt" onClick={onClose}>
        dismiss
      </button>
    </p>
  );
}

/** The four-point star: goals, flags, now. Size in px. */
export function Star({ size = 10, className = "", fill = "currentColor" }: { size?: number; className?: string; fill?: string }) {
  return (
    <svg width={size} height={size} viewBox="-10 -10 20 20" className={`inline-block shrink-0 ${className}`} aria-hidden="true">
      <path d={STAR_PATH} fill={fill} />
    </svg>
  );
}

export const STAR_PATH = "M0,-10 L2.2,-2.2 L10,0 L2.2,2.2 L0,10 L-2.2,2.2 L-10,0 L-2.2,-2.2 Z";

export function AgentTag({ onUndo }: { onUndo?: () => void }) {
  return (
    <span className="tag-agent">
      agent
      {onUndo ? (
        <button className="text-txt underline decoration-dim hover:decoration-cy" onClick={onUndo}>
          undo
        </button>
      ) : null}
    </span>
  );
}

/** A thin hours-against-target bar. Under target is dim, never red. */
export function Meter({ value, target }: { value: number; target: number }) {
  const w = target > 0 ? Math.min(1, value / target) : 0;
  return (
    <span className="relative block h-[3px] w-full bg-line">
      <span className="absolute inset-y-0 left-0 bg-or/85" style={{ width: `${w * 100}%` }} />
    </span>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <p className="py-3 text-[14px] text-dim">{children}</p>;
}
