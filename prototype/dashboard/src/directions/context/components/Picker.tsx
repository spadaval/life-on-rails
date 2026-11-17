// The one label picker: switching the timer, starting one on something, and relabelling a block
// all choose from the same list (recent first, then anything open, then Meetings and the like).
import { useMemo, useState } from "react";
import { isOpen } from "../../../data/derive.js";
import { useStore } from "../../../data/store.js";
import { homeOf } from "../lib/model.js";
import { Icon, kindIcon } from "./bits.js";

export interface Choice {
  id: string;
  title: string;
  /** task, project, goal, habit or label. */
  type: string;
  /** Its context, as a line under the title. */
  where: string | null;
}

export function Picker({ onPick, current, disabled }: { onPick: (c: Choice) => void; current?: string | null; disabled?: boolean }) {
  const s = useStore();
  const [q, setQ] = useState("");
  const all = useMemo<Choice[]>(() => {
    const nodes = s.nodes
      .filter((n) => isOpen(n) && n.type !== "metric")
      .map((n) => {
        const h = homeOf(s, n);
        return { id: n.id, title: n.title, type: n.type, where: h.node && h.node.id !== n.id ? h.title : n.type === "goal" ? null : h.kind === "loose" ? "Loose ends" : null };
      });
    return [...nodes, ...s.system.map((x) => ({ id: x.id, title: x.title, type: "label", where: null }))];
  }, [s]);
  const byId = new Map(all.map((c) => [c.id, c]));
  const meetings = s.system.find((x) => x.title === "Meetings");
  const recentIds = [...new Set([...(current ? [current] : []), ...s.recent.map((r) => r.id), ...(meetings ? [meetings.id] : [])])];
  const recent = recentIds.map((id) => byId.get(id)).filter((c): c is Choice => !!c).slice(0, 6);
  const needle = q.trim().toLowerCase();
  const found = needle
    ? all.filter((c) => `${c.title} ${c.where ?? ""}`.toLowerCase().includes(needle)).sort((a, b) => Number(!a.title.toLowerCase().startsWith(needle)) - Number(!b.title.toLowerCase().startsWith(needle))).slice(0, 12)
    : all.filter((c) => c.type === "project" || c.type === "goal").slice(0, 14);

  const row = (c: Choice) => (
    <li key={c.id}>
      <button type="button" className="cx-pick" onClick={() => onPick(c)} disabled={disabled} aria-current={c.id === current || undefined}>
        <span className="cx-pick-icon" data-type={c.type}>
          <Icon name={c.type === "task" ? "tasks" : c.type === "label" ? "clock" : c.type === "habit" ? "spark" : kindIcon(c.type)} size={16} />
        </span>
        <span className="cx-pick-text">
          <span className="cx-pick-title">{c.title}</span>
          {c.where ? <span className="cx-pick-where">{c.where}</span> : null}
        </span>
        {c.id === current ? <span className="cx-pick-now">Now</span> : null}
      </button>
    </li>
  );

  return (
    <div className="cx-picker">
      <label className="cx-field cx-field-search">
        <Icon name="search" size={16} />
        <input data-autofocus value={q} onChange={(e) => setQ(e.target.value)} placeholder="Find a task, project or goal" aria-label="Find a label" disabled={disabled} />
      </label>
      {!needle ? (
        <section>
          <h3 className="cx-label">Recent</h3>
          <ul className="cx-pick-list">{recent.map(row)}</ul>
        </section>
      ) : null}
      <section>
        <h3 className="cx-label">{needle ? `${found.length} found` : "Projects and goals"}</h3>
        {found.length ? <ul className="cx-pick-list">{found.map(row)}</ul> : <p className="cx-empty-line">Nothing open is called that.</p>}
      </section>
    </div>
  );
}
