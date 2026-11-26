// The few shared pieces of the Mostly Harmless look: panels, page heads,
// notices, star glyphs. Colours, type sizes and button levels live in styles.css.
import type { ReactNode } from "react";

/**
 * A panel: the panel tone on the ground. `framed` adds the bronze chamfered frame, which is kept
 * for what is open or live (an open editor, the selected block), at most two on a screen.
 */
export function Panel({ title, aside, children, className = "", bodyClass = "", framed = false }: {
  title?: ReactNode;
  aside?: ReactNode;
  children: ReactNode;
  className?: string;
  bodyClass?: string;
  framed?: boolean;
}) {
  return (
    <section className={`panel ${framed ? "panel-framed" : ""} ${className}`}>
      <div className={`face flex flex-col px-5 py-4 ${bodyClass}`}>
        {title ? (
          <header className="mb-2 flex items-baseline justify-between gap-3">
            <h2 className="h-sec">{title}</h2>
            {aside ? <span className="text-small text-mute">{aside}</span> : null}
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
      <h1 className="cond text-title font-semibold tracking-[0.08em]">{title}</h1>
      {sub ? <span className="text-body text-mute">{sub}</span> : null}
      <div className="ml-auto flex flex-wrap items-center gap-2">{children}</div>
    </header>
  );
}

export function Notice({ text, onClose }: { text: string | null; onClose: () => void }) {
  if (!text) return null;
  return (
    <p role="status" className="row-sel flex items-center justify-between gap-3 rounded-sm px-3 py-2">
      <span>{text}</span>
      <button className="btn btn-quiet btn-sm" onClick={onClose}>
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
  return <p className="py-3 text-dim">{children}</p>;
}
