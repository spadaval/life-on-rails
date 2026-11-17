// Where details open in this direction: a popover beside the thing on the desktop, a bottom
// sheet at phone width. There is no detail pane.
import { type ReactNode, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Icon, useNarrow } from "./bits.js";

export function Popover({ anchorId, onClose, label, children, width = 380 }: { anchorId: string; onClose: () => void; label: string; children: ReactNode; width?: number }) {
  const narrow = useNarrow();
  const ref = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ left: number; top: number; maxHeight: number; side: "right" | "left" | "below" | "above" } | null>(null);

  // Fixed to the viewport and drawn above the whole page, so no scrolling column can clip it.
  // It follows its anchor on every scroll and resize, and always fits the visible area.
  useLayoutEffect(() => {
    if (narrow) return;
    const place = () => {
      const anchor = document.getElementById(anchorId);
      if (!anchor) return;
      const a = anchor.getBoundingClientRect();
      const vw = document.documentElement.clientWidth;
      const vh = innerHeight;
      const m = 8;
      const w = Math.min(width, vw - 2 * m);
      const maxHeight = vh - 2 * m;
      const h = Math.min(ref.current?.scrollHeight ?? 320, maxHeight);
      let left: number;
      let top: number;
      let side: "right" | "left" | "below" | "above";
      if (a.width < vw * 0.4 && vw - a.right > w + 2 * m) {
        side = "right";
        left = a.right + 12;
        top = a.top - 12;
      } else if (a.width < vw * 0.4 && a.left > w + 2 * m) {
        side = "left";
        left = a.left - w - 12;
        top = a.top - 12;
      } else if (vh - a.bottom > h + m || vh - a.bottom >= a.top) {
        // A wide block: under it, at its right end, so the block itself stays readable.
        side = "below";
        left = a.right - w - m;
        top = a.bottom + m;
      } else {
        side = "above";
        left = a.right - w - m;
        top = a.top - h - m;
      }
      left = Math.max(m, Math.min(left, vw - w - m));
      top = Math.max(m, Math.min(top, vh - h - m));
      setPos({ left, top, maxHeight, side });
    };
    place();
    const ro = new ResizeObserver(place);
    if (ref.current) ro.observe(ref.current);
    window.addEventListener("resize", place);
    window.addEventListener("scroll", place, true);
    return () => {
      ro.disconnect();
      window.removeEventListener("resize", place);
      window.removeEventListener("scroll", place, true);
    };
  }, [anchorId, narrow, width]);

  // Escape closes; so does a press outside it (but not on its own anchor, which toggles).
  useEffect(() => {
    const key = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    const down = (e: PointerEvent) => {
      const t = e.target as Node;
      if (ref.current?.contains(t)) return;
      if (document.getElementById(anchorId)?.contains(t)) return;
      if ((t as Element).closest?.(".tl-notice, .tl-search")) return;
      onClose();
    };
    window.addEventListener("keydown", key);
    if (!narrow) window.addEventListener("pointerdown", down);
    return () => {
      window.removeEventListener("keydown", key);
      window.removeEventListener("pointerdown", down);
    };
  }, [anchorId, onClose, narrow]);

  // What it points at is brought into view (a link can open a task far down a list).
  useEffect(() => {
    const a = document.getElementById(anchorId)?.getBoundingClientRect();
    if (a && (a.top < 80 || a.bottom > innerHeight - 40)) document.getElementById(anchorId)?.scrollIntoView({ block: "center" });
  }, [anchorId]);

  // Focus moves in, so a keyboard user lands on its first control.
  useEffect(() => {
    const all = [...(ref.current?.querySelectorAll<HTMLElement>("[data-autofocus], button, input, select, textarea, a[href]") ?? [])];
    const first = all.find((el) => el.hasAttribute("data-autofocus")) ?? all.find((el) => !el.classList.contains("tl-pop-close"));
    first?.focus({ preventScroll: true });
  }, [anchorId]);

  if (narrow) {
    return createPortal(
      <div className="tl-sheet-wrap">
        <button type="button" className="tl-scrim" aria-label="Close" onClick={onClose} />
        <div ref={ref} className="tl-sheet" role="dialog" aria-label={label}>
          <div className="tl-sheet-grip" aria-hidden="true" />
          <button type="button" className="tl-iconbtn tl-pop-close" aria-label="Close" onClick={onClose}>
            <Icon name="close" />
          </button>
          {children}
        </div>
      </div>,
      document.body,
    );
  }
  // Inside <main> so the direction's styles reach it, but outside every scrolling column.
  const host = document.querySelector<HTMLElement>("main.tl") ?? document.body;
  return createPortal(
    <div
      ref={ref}
      className="tl-pop"
      role="dialog"
      aria-label={label}
      data-side={pos?.side}
      style={{ width: `min(${width}px, calc(100vw - 16px))`, left: pos?.left ?? -9999, top: pos?.top ?? 0, maxHeight: pos?.maxHeight, visibility: pos ? "visible" : "hidden" }}
    >
      <button type="button" className="tl-iconbtn tl-pop-close" aria-label="Close" onClick={onClose}>
        <Icon name="close" />
      </button>
      {children}
    </div>,
    host,
  );
}
