// The wiki as a star map: every page a body, every [[link]] a fine line.
// Pages loaded into every agent run are stars; people are cyan; bodies grow
// with how many pages link to them. Pick one to light it and its neighbours.
import { useEffect, useMemo, useRef, useState } from "react";
import type { Point } from "../shared/wiki.js";
import { STAR_PATH } from "./ui.js";

export interface MapPage {
  slug: string;
  title: string;
  in_digest: boolean;
  links: string[];
}

/** Map labels stop here; the full name is in the side panel. */
const LABEL_MAX = 26;

const folder = (slug: string) => (slug.includes("/") ? slug.slice(0, slug.indexOf("/")) : "");

export function StarMap({ pages, layout, selected, onSelect, onOpen }: {
  pages: MapPage[];
  layout: Record<string, Point>;
  selected: string | null;
  onSelect: (slug: string | null) => void;
  /** A second click on the selected page, to read it. */
  onOpen: (slug: string) => void;
}) {
  const placed = useMemo(() => pages.filter((p) => layout[p.slug]).map((p) => ({ ...p, ...layout[p.slug]! })), [pages, layout]);
  const bySlug = useMemo(() => new Map(placed.map((p) => [p.slug, p])), [placed]);
  const degree = useMemo(() => {
    const d = new Map<string, number>();
    for (const p of placed) for (const l of p.links) d.set(l, (d.get(l) ?? 0) + 1);
    return d;
  }, [placed]);
  const edges = useMemo(() => placed.flatMap((p) => p.links.filter((l) => bySlug.has(l)).map((l) => [p.slug, l] as const)), [placed, bySlug]);

  const fit = useMemo(() => {
    if (placed.length === 0) return { x: -200, y: -150, w: 400, h: 300 };
    const xs = placed.map((p) => p.x);
    const ys = placed.map((p) => p.y);
    const x0 = Math.min(...xs) - 160;
    const y0 = Math.min(...ys) - 80;
    return { x: x0, y: y0, w: Math.max(...xs) + 160 - x0, h: Math.max(...ys) + 100 - y0 };
  }, [placed]);
  const [view, setView] = useState(fit);
  useEffect(() => setView(fit), [fit]);
  const svgRef = useRef<SVGSVGElement>(null);
  const [box, setBox] = useState({ w: 800, h: 600 });
  useEffect(() => {
    const el = svgRef.current;
    if (!el) return;
    const ro = new ResizeObserver(([e]) => e && setBox({ w: e.contentRect.width, h: e.contentRect.height }));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);
  // Labels keep their size on screen at any zoom: font sizes are divided by this.
  const k = Math.min(box.w / view.w, box.h / view.h);

  const neighbours = useMemo(() => {
    if (!selected) return null;
    const s = new Set([selected]);
    for (const [a, b] of edges) {
      if (a === selected) s.add(b);
      if (b === selected) s.add(a);
    }
    return s;
  }, [selected, edges]);

  const zoom = (factor: number, cx = view.x + view.w / 2, cy = view.y + view.h / 2) =>
    setView((v) => {
      const w = Math.min(fit.w * 3, Math.max(200, v.w * factor));
      const h = (w / v.w) * v.h;
      return { x: cx - ((cx - v.x) / v.w) * w, y: cy - ((cy - v.y) / v.h) * h, w, h };
    });

  const toWorld = (clientX: number, clientY: number) => {
    const r = svgRef.current!.getBoundingClientRect();
    return { x: view.x + ((clientX - r.left) / r.width) * view.w, y: view.y + ((clientY - r.top) / r.height) * view.h };
  };

  // Wheel zoom needs a non-passive listener to stop the page scrolling.
  useEffect(() => {
    const el = svgRef.current;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      e.preventDefault();
      const p = toWorld(e.clientX, e.clientY);
      zoom(e.deltaY > 0 ? 1.12 : 1 / 1.12, p.x, p.y);
    };
    el.addEventListener("wheel", onWheel, { passive: false });
    return () => el.removeEventListener("wheel", onWheel);
  });
  const drag = useRef<{ x: number; y: number; vx: number; vy: number; moved: boolean } | null>(null);

  return (
    <div className="relative h-full min-h-[520px] overflow-hidden bg-well grid-faint">
      <svg
        ref={svgRef}
        viewBox={`${view.x} ${view.y} ${view.w} ${view.h}`}
        preserveAspectRatio="xMidYMid meet"
        className="absolute inset-0 h-full w-full cursor-grab touch-none select-none active:cursor-grabbing"
        onPointerDown={(e) => {
          drag.current = { x: e.clientX, y: e.clientY, vx: view.x, vy: view.y, moved: false };
          svgRef.current?.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          const d = drag.current;
          if (!d) return;
          const r = svgRef.current!.getBoundingClientRect();
          const scale = Math.max(view.w / r.width, view.h / r.height);
          if (Math.abs(e.clientX - d.x) + Math.abs(e.clientY - d.y) > 3) d.moved = true;
          setView((v) => ({ ...v, x: d.vx - (e.clientX - d.x) * scale, y: d.vy - (e.clientY - d.y) * scale }));
        }}
        onPointerUp={(e) => {
          const d = drag.current;
          drag.current = null;
          if (d && !d.moved) {
            const hit = document.elementFromPoint(e.clientX, e.clientY);
            const slug = hit?.closest("[data-slug]")?.getAttribute("data-slug") ?? null;
            if (slug && slug === selected) onOpen(slug);
            else onSelect(slug);
          }
        }}
        role="img"
        aria-label="Map of the notes and the links between them"
      >
        {edges.map(([a, b]) => {
          const p = bySlug.get(a)!;
          const q = bySlug.get(b)!;
          const lit = neighbours !== null && (a === selected || b === selected);
          return <line key={`${a}>${b}`} x1={p.x} y1={p.y} x2={q.x} y2={q.y} stroke={lit ? "#f08a2c" : "#4a2e14"} strokeOpacity={lit ? 0.8 : 0.9} strokeWidth={lit ? 1.3 : 1} vectorEffect="non-scaling-stroke" />;
        })}
        {placed.map((p) => {
          const lit = neighbours?.has(p.slug) ?? false;
          return <Body key={p.slug} p={p} k={k} links={degree.get(p.slug) ?? 0} selected={selected === p.slug} lit={lit} dim={neighbours !== null && !lit} />;
        })}
      </svg>
      <p className="pointer-events-none absolute bottom-3 left-3 flex flex-wrap items-center gap-x-4 gap-y-1 bg-well/85 px-2 py-1 text-[12.5px] text-mute">
        <span className="flex items-center gap-1.5"><svg width="11" height="11" viewBox="-10 -10 20 20" aria-hidden="true"><path d={STAR_PATH} fill="#f08a2c" /></svg>loaded into every agent run</span>
        <span className="flex items-center gap-1.5"><span className="size-[7px] rounded-full bg-cy" />a person</span>
        <span className="flex items-center gap-1.5"><span className="size-[7px] rounded-full bg-mute" />a page, larger with more links in</span>
      </p>
      <div className="absolute right-3 bottom-3 flex gap-1.5">
        <button className="btn btn-sm min-w-8 bg-panel" onClick={() => zoom(1 / 1.25)} aria-label="Zoom in">
          <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true"><path d="M6 1v10M1 6h10" stroke="currentColor" strokeWidth="1.5" /></svg>
        </button>
        <button className="btn btn-sm min-w-8 bg-panel" onClick={() => zoom(1.25)} aria-label="Zoom out">
          <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden="true"><path d="M1 6h10" stroke="currentColor" strokeWidth="1.5" /></svg>
        </button>
        <button className="btn btn-sm bg-panel" onClick={() => setView(fit)}>
          Fit
        </button>
      </div>
      {placed.length === 0 ? <p className="absolute inset-0 grid place-items-center text-dim">No notes yet.</p> : null}
    </div>
  );
}

function Body({ p, k, links, selected, lit, dim }: { p: MapPage & Point; k: number; links: number; selected: boolean; lit: boolean; dim: boolean }) {
  // Everything below is in screen pixels times u, so it holds its size at any zoom.
  const u = 1 / k;
  const label = p.title.length > LABEL_MAX ? `${p.title.slice(0, LABEL_MAX - 1)}…` : p.title;
  const r = Math.min(9, 2.8 + Math.sqrt(links) * 1.6);
  const kind = folder(p.slug);
  const fill = kind === "people" ? "#6cc6ec" : kind === "days" ? "#7d7266" : "#b1a596";
  // Dimmed is the muted ink at full strength, never faded text.
  const textFill = selected ? "#f08a2c" : dim ? "#928777" : "#ece6dc";
  const labelled = !dim && (lit || links >= 2 || p.in_digest || k >= 1.1);
  return (
    <g data-slug={p.slug} className="cursor-pointer" opacity={dim ? 0.55 : 1}>
      <circle cx={p.x} cy={p.y} r={14 * u} fill="transparent" />
      {p.in_digest ? (
        <path d={STAR_PATH} transform={`translate(${p.x} ${p.y}) scale(${((r + 4) / 10) * u})`} fill="#f08a2c" />
      ) : (
        <circle cx={p.x} cy={p.y} r={r * u} fill={lit && !selected ? "#ece6dc" : fill} />
      )}
      {selected ? <Reticle x={p.x} y={p.y} u={u} s={r + 7} /> : null}
      {labelled ? (
        <text x={p.x + (r + 6) * u} y={p.y + 4.5 * u} fill={textFill} fontSize={(p.in_digest ? 14 : 13) * u} fontWeight={p.in_digest ? 600 : 400}>
          {label}
        </text>
      ) : null}
    </g>
  );
}

function Reticle({ x, y, u, s: size }: { x: number; y: number; u: number; s: number }) {
  const s = size * u;
  const c = size * 0.4 * u;
  return (
    <g stroke="#f08a2c" strokeWidth={1.5 * u} fill="none">
      {([[-1, -1], [1, -1], [1, 1], [-1, 1]] as const).map(([sx, sy]) => (
        <path key={`${sx}${sy}`} d={`M${x + sx * s},${y + sy * (s - c)} L${x + sx * s},${y + sy * s} L${x + sx * (s - c)},${y + sy * s}`} />
      ))}
    </g>
  );
}
