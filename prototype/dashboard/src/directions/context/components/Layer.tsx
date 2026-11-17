// What floats: a popover beside the control that opened it on a desktop, a bottom sheet at phone
// width. It is drawn over the whole page (a portal on body), placed against the viewport, so a
// scrolling column never clips it; Escape or a press outside closes it and focus goes back.
import { type ReactNode, useEffect, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Icon } from "./bits.js";
import { usePhone } from "../lib/ui.js";

/** The element that opened the last layer, so the popover can sit beside it. */
let lastAnchor: HTMLElement | null = null;
export const anchorFrom = (e: { currentTarget: EventTarget | null }) => {
  lastAnchor = e.currentTarget instanceof HTMLElement ? e.currentTarget : null;
};

export function Layer({ title, onClose, children, width = 400, label }: { title: ReactNode; onClose: () => void; children: ReactNode; width?: number; label: string }) {
  const phone = usePhone();
  const box = useRef<HTMLDivElement>(null);
  const [pos, setPos] = useState<{ left: number; top: number; maxH: number } | null>(null);
  const anchor = useRef<HTMLElement | null>(lastAnchor && document.body.contains(lastAnchor) ? lastAnchor : null);

  useLayoutEffect(() => {
    if (phone) return;
    const place = () => {
      const vw = window.innerWidth;
      const vh = window.innerHeight;
      const h = box.current?.offsetHeight ?? 420;
      const a = anchor.current?.getBoundingClientRect();
      const m = 12;
      let left = a ? a.left : (vw - width) / 2;
      let top = a ? a.bottom + 8 : 120;
      if (a && a.right + width + m < vw && a.width < 260) left = a.left;
      left = Math.min(Math.max(m, left), vw - width - m);
      const maxH = vh - 2 * m;
      if (a && top + Math.min(h, maxH) > vh - m) top = Math.max(m, a.top - 8 - Math.min(h, maxH));
      top = Math.min(Math.max(m, top), vh - m - Math.min(h, maxH));
      setPos({ left, top, maxH });
    };
    place();
    const ro = new ResizeObserver(place);
    if (box.current) ro.observe(box.current);
    window.addEventListener("resize", place);
    return () => {
      ro.disconnect();
      window.removeEventListener("resize", place);
    };
  }, [phone, width]);

  useEffect(() => {
    const back = document.activeElement as HTMLElement | null;
    const first = box.current?.querySelector<HTMLElement>("[data-autofocus], input, button:not([disabled]), [href]");
    first?.focus({ preventScroll: true });
    const key = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.stopPropagation();
        onClose();
      }
    };
    window.addEventListener("keydown", key, true);
    return () => {
      window.removeEventListener("keydown", key, true);
      (anchor.current ?? back)?.focus?.({ preventScroll: true });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return createPortal(
    <div className="cx-layer" data-phone={phone || undefined}>
      <div className="cx-layer-scrim" onClick={onClose} aria-hidden="true" />
      <div
        ref={box}
        className="cx-float"
        role="dialog"
        aria-label={label}
        style={phone ? undefined : { width, left: pos?.left ?? -9999, top: pos?.top ?? 0, maxHeight: pos?.maxH }}
      >
        <header className="cx-float-head">
          <div className="cx-float-title">{title}</div>
          <button type="button" className="cx-iconbtn" onClick={onClose} aria-label="Close">
            <Icon name="close" size={16} />
          </button>
        </header>
        <div className="cx-float-body">{children}</div>
      </div>
    </div>,
    document.body,
  );
}
