// The shape of every page: a list pane and a detail pane. Each is a frosted layer with its own
// scroll. From 1280px they sit side by side, and with nothing open the detail side shows the
// page's overview, recessed. From 720 to 1279 the detail slides in over the list, which stays
// visible behind it, blurred. Below 720 the list is the page and the detail is a sheet that rests
// at half height (the list above stays live) or full; a visible button switches the two.
//
// A detail can carry a second pane stacked over it (a task opened from a goal or a person): the
// one in front is sharp and the one behind steps back and blurs. Escape closes only the pane in
// front; Close is Back when the pane was opened here (lib.ts closeHere).
import { type ReactNode, useEffect, useLayoutEffect, useRef } from "react";
import { type Fit, setHere, useFit, useLayer } from "./lib.js";
import { cx, Icon } from "./ui.js";

export interface Detail {
  /** Changes when a different thing opens, so the pane's scroll starts at the top. */
  key: string;
  /** The pane title, set in condensed caps: what kind of thing this is. */
  kicker: ReactNode;
  title: ReactNode;
  /** The pane's main actions, under the title. */
  actions?: ReactNode;
  body: ReactNode;
  close: () => void;
  /** An edge colour: amber for an open item (the default), cyan for the agent's. */
  tone?: "agent";
  /** Shown on the stacked pane's back button, at phone width: what is behind it. */
  backLabel?: string;
}

export function Panes({
  label,
  head,
  list,
  detail,
  sub,
  rest,
  sheet,
  restInList,
}: {
  /** The page's name, for the list pane's landmark. */
  label: string;
  head: ReactNode;
  list: ReactNode;
  detail: Detail | null;
  /** A second pane over the detail. */
  sub?: Detail | null;
  /** What the detail side shows at rest from 1280px: the page's overview. Below that it is in the list. */
  rest?: ReactNode;
  /** The sheet's height at phone width, from the link (`sheet=full`). */
  sheet?: string;
  /** Below 1280px, put the overview at the foot of the list (Today and People: it carries facts, not only a hint). */
  restInList?: boolean;
}) {
  const fit = useFit();
  const full = sheet === "full";
  const listRef = useRef<HTMLDivElement>(null);
  const open = Boolean(detail);
  const behind = open && (fit === "mid" || (fit === "narrow" && full));

  // Registered here in this order, so the stacked pane is in front of the detail for Escape.
  useLayer(open, () => detail?.close());
  useLayer(open && Boolean(sub), () => sub?.close());

  // The open row stays in sight: inside the list pane, or at phone width above the sheet.
  useLayoutEffect(() => {
    if (!detail) return;
    const row = listRef.current?.querySelector<HTMLElement>("[data-sel='true']");
    if (!row) return;
    if (fit === "narrow") {
      const r = row.getBoundingClientRect();
      const sheetTop = innerHeight * (full ? 0.1 : 0.5);
      const header = 112;
      if (r.bottom > sheetTop - 8) scrollBy({ top: r.bottom - sheetTop + 24 });
      else if (r.top < header) scrollBy({ top: r.top - header - 8 });
    } else row.scrollIntoView({ block: "nearest" });
  }, [detail?.key, fit, full]);

  return (
    <div className={cx("p-work", `is-${fit}`, open && "has-detail", sub && "has-sub")}>
      <div ref={listRef} className={cx("p-pane", "p-list", behind && "is-behind")} role="region" aria-label={label}>
        <header className="p-list__head">{head}</header>
        <div className="p-list__body">{list}</div>
        {fit !== "wide" && rest && restInList ? <div className="p-list__rest">{rest}</div> : null}
      </div>
      {fit === "wide" && !detail && rest ? (
        <aside className="p-pane p-rest" aria-label={`${label}: overview`}>
          {rest}
        </aside>
      ) : null}
      {detail ? <DetailLayer fit={fit} detail={detail} sub={sub ?? null} full={full} /> : null}
    </div>
  );
}

function DetailLayer({ fit, detail, sub, full }: { fit: Fit; detail: Detail; sub: Detail | null; full: boolean }) {
  if (fit === "narrow") {
    // At phone width a stacked pane takes the sheet, with a way back to what is behind it.
    const shown = sub ?? detail;
    return (
      <>
        {full ? <div className="p-scrim p-scrim--sheet" aria-hidden="true" /> : null}
        <div className={cx("p-sheet", full ? "is-full" : "is-half", shown.tone === "agent" && "is-agent")} role="dialog" aria-label={typeof shown.title === "string" ? shown.title : "Open item"}>
          <div className="p-sheet__bar">
            {sub ? (
              <button type="button" className="p-sheet__back" onClick={sub.close}>
                <Icon name="back" /> {sub.backLabel ?? "Back"}
              </button>
            ) : (
              <span className="p-sheet__spacer" />
            )}
            <button type="button" className="p-sheet__grip" aria-label={full ? "Show the list too: half height" : "Full height"} onClick={() => setHere({ sheet: full ? undefined : "full" })}>
              <span aria-hidden="true" />
            </button>
            <div className="p-sheet__btns">
              <button type="button" className="p-iconbtn" aria-label={full ? "Half height" : "Full height"} title={full ? "Half height" : "Full height"} onClick={() => setHere({ sheet: full ? undefined : "full" })}>
                <Icon name={full ? "collapse" : "expand"} />
              </button>
              <button type="button" className="p-iconbtn" aria-label="Close" title="Close (Esc)" onClick={shown.close}>
                <Icon name="close" />
              </button>
            </div>
          </div>
          <PaneBody d={shown} />
        </div>
      </>
    );
  }
  return (
    <>
      <section className={cx("p-pane", "p-detail", fit === "mid" && "is-over", sub && "is-behind", detail.tone === "agent" && "is-agent")} aria-label={typeof detail.title === "string" ? detail.title : "Open item"} key={detail.key}>
        <PaneTop d={detail} />
        <PaneBody d={detail} />
      </section>
      {sub ? (
        <section className={cx("p-pane", "p-detail", "p-detail--sub", fit === "mid" && "is-over", sub.tone === "agent" && "is-agent")} aria-label={typeof sub.title === "string" ? sub.title : "Open item"} key={`sub-${sub.key}`}>
          <PaneTop d={sub} back={detail} />
          <PaneBody d={sub} />
        </section>
      ) : null}
    </>
  );
}

function PaneTop({ d, back }: { d: Detail; back?: Detail }) {
  return (
    <div className="p-detail__bar">
      {back ? (
        <button type="button" className="p-detail__back" onClick={d.close}>
          <Icon name="back" /> {d.backLabel ?? "Back"}
        </button>
      ) : (
        <span />
      )}
      <button type="button" className="p-iconbtn" aria-label="Close" title="Close (Esc)" onClick={d.close}>
        <Icon name="close" />
      </button>
    </div>
  );
}

function PaneBody({ d }: { d: Detail }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    ref.current?.scrollTo({ top: 0 });
  }, [d.key]);
  return (
    <div className="p-detail__scroll" ref={ref}>
      <header className="p-detail__head">
        <p className="p-kicker">{d.kicker}</p>
        <h2 className="p-detail__title">{d.title}</h2>
        {d.actions ? <div className="p-detail__acts">{d.actions}</div> : null}
      </header>
      <div className="p-detail__body">{d.body}</div>
    </div>
  );
}

/** The list pane's header: the page title in condensed caps, a line under it, and its tools. */
export function ListHead({ title, sub, tools, children }: { title: string; sub?: ReactNode; tools?: ReactNode; children?: ReactNode }) {
  return (
    <>
      <div className="p-list__top">
        <div>
          <h1 className="p-pagetitle">{title}</h1>
          {sub ? <p className="p-list__sub">{sub}</p> : null}
        </div>
        {tools ? <div className="p-list__tools">{tools}</div> : null}
      </div>
      {children}
    </>
  );
}

/** "More": what is uncommon about an item, under its common fields, collapsed. */
export function More({ children, label = "More" }: { children: ReactNode; label?: string }) {
  return (
    <details className="p-more">
      <summary>
        <Icon name="chev" />
        {label}
      </summary>
      <div className="p-more__body">{children}</div>
    </details>
  );
}
