// Where details open in this direction: a popover beside the thing on the desktop, a bottom
// sheet at phone width. There is no detail pane. The popover lives inside the nearest
// [data-pop-root] (the route, a list), so it scrolls with what it points at.
import { type ReactNode, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Icon, useNarrow } from "./bits.js";

export function Popover({ anchorId, onClose, label, children, width = 380 }: { anchorId: string; onClose: () => void; label: string; children: ReactNode; width?: number }) {
  const narrow = useNarrow();
  const ref = useRef<HTMLDivElement>(null);
  const [root, setRoot] = useState<HTMLElement | null>(null);
  const [pos, setPos] = useState<{ left: number; top: number; side: "right" | "left" | "below" } | null>(null);

  useLayoutEffect(() => {
    if (narrow) return;
    const place = () => {
      const anchor = document.getElementById(anchorId);
      const r = anchor?.closest<HTMLElement>("[data-pop-root]");
      if (!anchor || !r) return;
      setRoot(r);
      const a = anchor.getBoundingClientRect();
      const b = r.getBoundingClientRect();
      const h = ref.current?.offsetHeight ?? 320;
      const top0 = a.top - b.top + r.scrollTop;
      const spaceRight = b.right - a.right;
      let left: number;
      let top: number;
      let side: "right" | "left" | "below";
      if (a.width < b.width * 0.55 && spaceRight > width + 24) {
        side = "right";
        left = a.right - b.left + 12;
        top = top0 - 12;
      } else if (a.left - b.left > width + 24) {
        side = "left";
        left = a.left - b.left - width - 12;
        top = top0 - 12;
      } else {
        // A wide block: under it, at its right end, so the block itself stays readable.
        side = "below";
        left = Math.max(8, a.right - b.left - width - 8);
        top = top0 + a.height + 8;
      }
      const maxTop = Math.max(r.scrollHeight, r.clientHeight) - h - 8;
      setPos({ left, top: Math.max(8, Math.min(top, maxTop)), side });
    };
    place();
    const ro = new ResizeObserver(place);
    if (ref.current) ro.observe(ref.current);
    window.addEventListener("resize", place);
    return () => {
      ro.disconnect();
      window.removeEventListener("resize", place);
    };
  }, [anchorId, narrow, width, root]);

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
  const r = root ?? document.getElementById(anchorId)?.closest<HTMLElement>("[data-pop-root]") ?? null;
  if (!r) return null;
  return createPortal(
    <div
      ref={ref}
      className="tl-pop"
      role="dialog"
      aria-label={label}
      data-side={pos?.side}
      style={{ width, left: pos?.left ?? -9999, top: pos?.top ?? 0, visibility: pos ? "visible" : "hidden" }}
    >
      <button type="button" className="tl-iconbtn tl-pop-close" aria-label="Close" onClick={onClose}>
        <Icon name="close" />
      </button>
      {children}
    </div>,
    r,
  );
}
