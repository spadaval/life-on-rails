// The intent graph as a star map: goals are stars sized by their week's hours
// against target, with their projects and habits on fine lines around them.
// Tasks show around the selected goal, or everywhere under the Tasks filter.
import { useEffect, useMemo, useRef, useState } from "react";
import type { GraphNode } from "./IntentEditor.js";
import { STAR_PATH } from "./ui.js";

export type MapFilter = "all" | "goal" | "project" | "habit" | "task";

interface Placed {
  node: GraphNode;
  x: number;
  y: number;
  parent: string | null;
  /** Which side of its parent it sits on, so its label reads away from the parent. */
  left: boolean;
}

/** Map labels stop here; the full name is in the side panel. */
const LABEL_MAX = 28;
const CELL_W = 620;
const CELL_H = 440;
const RING = 150;

/** Where everything sits. Deterministic: same graph, same sky. */
export function layout(nodes: GraphNode[]): { placed: Map<string, Placed>; width: number; height: number } {
  const live = nodes.filter((n) => n.status !== "archived");
  const byId = new Map(live.map((n) => [n.id, n]));
  const kids = new Map<string, GraphNode[]>();
  const firstParent = (n: GraphNode) => n.parents.find((p) => byId.has(p.id))?.id ?? null;
  for (const n of live) {
    const p = firstParent(n);
    if (p) kids.set(p, [...(kids.get(p) ?? []), n]);
  }
  const goals = live.filter((n) => n.type === "goal").sort((a, b) => a.title.localeCompare(b.title));
  const orphans = live.filter((n) => n.type !== "goal" && n.type !== "task" && !firstParent(n));
  const roots: (GraphNode | null)[] = [...goals, ...(orphans.length > 0 ? [null] : [])];
  const cols = Math.max(1, Math.ceil(Math.sqrt(roots.length)));
  const rows = Math.ceil(roots.length / cols);
  const placed = new Map<string, Placed>();

  const ring = (center: { x: number; y: number }, list: GraphNode[], r: number, start: number, parent: string | null, spread = Math.PI * 2) => {
    list.forEach((n, i) => {
      if (placed.has(n.id)) return;
      const a = start + (spread * (i + 0.5)) / list.length;
      placed.set(n.id, { node: n, x: r1(center.x + Math.cos(a) * r), y: r1(center.y + Math.sin(a) * r * 0.86), parent, left: Math.cos(a) < -0.2 });
    });
  };

  roots.forEach((g, i) => {
    const col = i % cols;
    const row = Math.floor(i / cols);
    // Offset alternate rows, so the sky does not read as a table.
    const cx = col * CELL_W + CELL_W / 2 + (row % 2 ? CELL_W * 0.16 : -CELL_W * 0.04) + hash(g?.id ?? "orphans", 40);
    const cy = row * CELL_H + CELL_H / 2 + hash((g?.id ?? "o") + "y", 30);
    const center = { x: r1(cx), y: r1(cy) };
    const children = g ? (kids.get(g.id) ?? []) : orphans;
    if (g) placed.set(g.id, { node: g, x: center.x, y: center.y, parent: null, left: false });
    const order = (n: GraphNode) => (n.type === "project" ? 0 : n.type === "habit" ? 1 : n.type === "metric" ? 2 : 3);
    const sorted = [...children].sort((a, b) => order(a) - order(b) || a.title.localeCompare(b.title));
    // The ring leaves a gap straight below the star, where its label sits.
    ring(center, sorted, RING, Math.PI / 2 + 1.05, g?.id ?? null, Math.PI * 2 - 2.1);
    // Each project's own tasks sit just beyond it, fanned away from the goal.
    for (const p of sorted) {
      const pp = placed.get(p.id);
      if (!pp || p.type === "task") continue;
      const tasks = (kids.get(p.id) ?? []).filter((t) => t.type === "task");
      const away = Math.atan2(pp.y - cy, pp.x - cx);
      ring({ x: pp.x, y: pp.y }, tasks, 84, away - 0.7, p.id, 1.4);
    }
  });
  return { placed, width: cols * CELL_W + CELL_W * 0.3, height: Math.max(1, rows) * CELL_H };
}

// Rounded so server and browser place everything identically.
const r1 = (v: number) => Math.round(v * 10) / 10;

/** A stable small offset in [-amp/2, amp/2] from a string. */
function hash(s: string, amp: number): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) h = Math.imul(h ^ s.charCodeAt(i), 16777619);
  return ((h >>> 0) / 4294967295 - 0.5) * amp;
}

const goalOf = (id: string, placed: Map<string, Placed>): string | null => {
  let cur = placed.get(id);
  while (cur?.parent) cur = placed.get(cur.parent);
  return cur && cur.node.type === "goal" ? cur.node.id : null;
};

export function StarMap({ nodes, selected, onSelect, filter, showDone }: {
  nodes: GraphNode[];
  selected: string | null;
  onSelect: (id: string | null) => void;
  filter: MapFilter;
  showDone: boolean;
}) {
  const shown = useMemo(() => nodes.filter((n) => showDone || (n.status !== "done" && n.status !== "later")), [nodes, showDone]);
  const { placed, width, height } = useMemo(() => layout(shown), [shown]);
  const fit = useMemo(() => ({ x: -60, y: -40, w: width + 120, h: height + 80 }), [width, height]);
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

  /** Frame a star's whole constellation, with room for its labels, at no more than full size. */
  const focus = (id: string) => {
    const goal = goalOf(id, placed) ?? id;
    const members = [...placed.values()].filter((p) => p.node.id === goal || goalOf(p.node.id, placed) === goal);
    if (members.length === 0) return;
    const xs = members.map((p) => p.x);
    const ys = members.map((p) => p.y);
    // Labels keep their screen size, so the room they need is in pixels, not map units:
    // about 8px a character beside a body, 90px either side of a goal's centred name.
    const reach = (p: Placed) => Math.min(p.node.title.length, LABEL_MAX) * 8 + 24;
    const leftPx = Math.max(90, ...members.filter((p) => p.node.type !== "goal" && p.left).map(reach));
    const rightPx = Math.max(90, ...members.filter((p) => p.node.type !== "goal" && !p.left).map(reach));
    // Measured now: the observed size can lag a layout that is still settling.
    const r = svgRef.current?.getBoundingClientRect() ?? { width: box.w, height: box.h };
    const spanX = Math.max(1, Math.max(...xs) - Math.min(...xs));
    const spanY = Math.max(1, Math.max(...ys) - Math.min(...ys));
    const scale = Math.max(0.3, Math.min(1, (r.width - 32 - leftPx - rightPx) / spanX, (r.height - 140) / spanY));
    const x0 = Math.min(...xs) - leftPx / scale;
    const x1 = Math.max(...xs) + rightPx / scale;
    const y0 = Math.min(...ys) - 40 / scale;
    const y1 = Math.max(...ys) + 80 / scale;
    const w = r.width / scale;
    const h = r.height / scale;
    setView({ x: (x0 + x1) / 2 - w / 2, y: (y0 + y1) / 2 - h / 2, w, h });
  };
  // Arriving with a star already selected (a link, a reload) opens on it.
  const arrived = useRef(false);
  useEffect(() => {
    if (arrived.current || box.w === 800) return;
    arrived.current = true;
    if (selected) focus(selected);
  }, [box]);
  const drag = useRef<{ x: number; y: number; vx: number; vy: number; moved: boolean } | null>(null);

  const selGoal = selected ? goalOf(selected, placed) : null;
  const visible = (p: Placed) => {
    const t = p.node.type;
    if (t === "goal") return true;
    if (filter === "goal") return false;
    if (filter === "project") return t === "project";
    if (filter === "habit") return t === "habit";
    if (filter === "task") return true;
    // All: projects, habits and metrics everywhere; tasks around the selected goal.
    return t !== "task" || (selGoal !== null && goalOf(p.node.id, placed) === selGoal);
  };
  const items = [...placed.values()].filter(visible);
  const lit = (p: Placed) => selGoal !== null && goalOf(p.node.id, placed) === selGoal;

  const zoom = (factor: number, cx = view.x + view.w / 2, cy = view.y + view.h / 2) =>
    setView((v) => {
      const w = Math.min(fit.w * 2, Math.max(300, v.w * factor));
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
            const id = hit?.closest("[data-id]")?.getAttribute("data-id") ?? null;
            onSelect(id);
            if (id) focus(id);
          }
        }}
        role="img"
        aria-label="Star map of goals, projects, habits and tasks"
      >
        {items.map((p) => {
          if (!p.parent) return null;
          const q = placed.get(p.parent);
          if (!q || !visible(q)) return null;
          return <line key={`l${p.node.id}`} x1={q.x} y1={q.y} x2={p.x} y2={p.y} stroke={lit(p) ? "#f08a2c" : "#4a2e14"} strokeOpacity={lit(p) ? 0.75 : 0.9} strokeWidth={lit(p) ? 1.3 : 1} vectorEffect="non-scaling-stroke" />;
        })}
        {items.map((p) => (
          <Body key={p.node.id} p={p} k={k} selected={selected === p.node.id} lit={lit(p)} dim={selGoal !== null && !lit(p)} labelled={lit(p) || (selGoal === null && k >= 0.85)} />
        ))}
      </svg>
      <p className="pointer-events-none absolute bottom-3 left-3 flex flex-wrap items-center gap-x-4 gap-y-1 bg-well/85 px-2 py-1 text-[12.5px] text-mute">
        <span className="flex items-center gap-1.5"><svg width="11" height="11" viewBox="-10 -10 20 20" aria-hidden="true"><path d={STAR_PATH} fill="#f08a2c" /></svg>goal, sized by this week's hours</span>
        <span className="flex items-center gap-1.5"><span className="size-[7px] rounded-full bg-mute" />project</span>
        <span className="flex items-center gap-1.5"><span className="size-[7px] border border-mute" />habit</span>
        <span className="flex items-center gap-1.5"><span className="size-[4px] rounded-full bg-txt" />task</span>
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
      {placed.size === 0 ? <p className="absolute inset-0 grid place-items-center text-dim">Nothing in the graph yet. Add a goal.</p> : null}
    </div>
  );
}

function Body({ p, k, selected, lit, dim, labelled }: { p: Placed; k: number; selected: boolean; lit: boolean; dim: boolean; labelled: boolean }) {
  // Everything below is in screen pixels times u, so it holds its size at any zoom.
  const u = 1 / k;
  const n = p.node;
  const done = n.status === "done";
  // Goals keep their whole name; smaller bodies shorten long ones.
  const label = n.type !== "goal" && n.title.length > LABEL_MAX ? `${n.title.slice(0, LABEL_MAX - 1)}…` : n.title;
  // Dimmed is the muted ink at full strength, never faded text.
  const textFill = done ? "#928777" : dim ? "#b1a596" : "#ece6dc";

  if (n.type === "goal") {
    const target = n.weekly_hour_target ?? 0;
    const hours = n.week_hours ?? 0;
    const fed = target > 0 ? Math.min(1, hours / target) : hours > 0 ? 1 : 0;
    const size = 8 + fed * 8;
    const color = fed > 0 ? "#f08a2c" : "#8a6a4a";
    const lines = wrap(label, 20);
    return (
      <g data-id={n.id} className="cursor-pointer">
        <circle cx={p.x} cy={p.y} r={22 * u} fill="transparent" />
        {selected ? <Reticle x={p.x} y={p.y} u={u} s={20} /> : null}
        <path d={STAR_PATH} transform={`translate(${p.x} ${p.y}) scale(${(size / 10) * u})`} fill={color} opacity={dim ? 0.6 : 1} />
{dim ? null : (
          <>
        <text x={p.x} y={p.y + 40 * u} textAnchor="middle" fill={selected ? "#f08a2c" : textFill} fontSize={15 * u} fontWeight={600} className="font-cond uppercase" letterSpacing="0.06em">
          {lines.map((line, i) => (
            <tspan key={i} x={p.x} dy={i === 0 ? 0 : 16 * u}>
              {line}
            </tspan>
          ))}
        </text>
        <text x={p.x} y={p.y + (56 + (lines.length - 1) * 16) * u} textAnchor="middle" fill="#b1a596" fontSize={12.5 * u}>
          {target ? `${round1(hours)} / ${round1(target)}h` : `${round1(hours)}h`}
        </text>
          </>
        )}
      </g>
    );
  }

  const flagged = n.type === "task" && n.flagged_at !== null;
  return (
    <g data-id={n.id} className="cursor-pointer">
      <circle cx={p.x} cy={p.y} r={12 * u} fill="transparent" />
      {flagged ? (
        <path d={STAR_PATH} transform={`translate(${p.x} ${p.y}) scale(${0.6 * u})`} fill="#f08a2c" />
      ) : n.type === "habit" ? (
        <rect x={p.x - 3.5 * u} y={p.y - 3.5 * u} width={7 * u} height={7 * u} fill="#0f1016" stroke={lit ? "#f08a2c" : "#b1a596"} strokeWidth={u} />
      ) : n.type === "task" ? (
        <circle cx={p.x} cy={p.y} r={2.2 * u} fill={done ? "#7d7266" : "#ece6dc"} />
      ) : (
        <circle cx={p.x} cy={p.y} r={3.5 * u} fill={lit ? "#ece6dc" : "#b1a596"} />
      )}
      {selected ? <Reticle x={p.x} y={p.y} u={u} s={9} /> : null}
      {labelled ? (
      <text
        x={p.x + (p.left ? -9 : 9) * u}
        y={p.y + 4.5 * u}
        textAnchor={p.left ? "end" : "start"}
        fill={selected ? "#f08a2c" : textFill}
        fontSize={(n.type === "task" ? 12 : 13) * u}
        textDecoration={done ? "line-through" : undefined}
      >
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

/** Break a name into lines of about `width` characters, at spaces. */
function wrap(text: string, width: number): string[] {
  const out: string[] = [];
  let line = "";
  for (const word of text.split(" ")) {
    if (line && (line + " " + word).length > width) {
      out.push(line);
      line = word;
    } else line = line ? `${line} ${word}` : word;
  }
  if (line) out.push(line);
  return out;
}

const round1 = (n: number) => String(Math.round(n * 10) / 10);
