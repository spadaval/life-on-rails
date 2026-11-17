// Choosing what time counts toward: the recent labels as one-press chips, and a field that
// finds any goal, project, habit or task. Used to relabel a block, start a timer and switch one.
import { useMemo, useState } from "react";
import { pickable } from "../../../data/derive.js";
import { useStore } from "../../../data/store.js";

const KIND: Record<string, string> = { goal: "Goal", project: "Project", habit: "Habit", task: "Task", system: "Label" };

export function LabelPicker({ current, suggestion, onPick, allowNone }: {
  current?: string | null;
  /** The agent's guess, offered first. */
  suggestion?: { id: string; title: string } | null;
  onPick: (id: string | null, title: string) => void;
  /** Offer "No label yet" (starting a timer). */
  allowNone?: boolean;
}) {
  const s = useStore();
  const [q, setQ] = useState("");
  const all = useMemo(() => pickable(s), [s]);
  const recent = [
    ...(suggestion ? [suggestion] : []),
    ...s.recent.filter((r) => r.id !== suggestion?.id),
    ...s.system.filter((x) => x.title === "Meetings"),
  ].slice(0, 6);
  const needle = q.trim().toLowerCase();
  const found = needle ? all.filter((x) => x.title.toLowerCase().includes(needle)).slice(0, 7) : [];
  return (
    <div className="tl-picker">
      <div className="tl-chips" role="group" aria-label="Recent labels">
        {recent.map((r) => (
          <button
            key={r.id}
            type="button"
            className="tl-chip"
            data-on={r.id === current || undefined}
            data-suggested={r.id === suggestion?.id || undefined}
            onClick={() => onPick(r.id, r.title)}
          >
            {r.id === suggestion?.id ? <span className="tl-chip-k">Suggested</span> : null}
            {r.title}
          </button>
        ))}
        {allowNone ? (
          <button type="button" className="tl-chip tl-chip-ghost" onClick={() => onPick(null, "Unlabelled")}>
            No label yet
          </button>
        ) : null}
      </div>
      <input
        className="tl-input"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Something else…"
        aria-label="Find a label"
        onKeyDown={(e) => {
          if (e.key === "Enter" && found[0]) onPick(found[0].id, found[0].title);
        }}
      />
      {needle ? (
        <ul className="tl-picklist">
          {found.length ? (
            found.map((f) => (
              <li key={f.id}>
                <button type="button" onClick={() => onPick(f.id, f.title)}>
                  <span>{f.title}</span>
                  <small>{KIND[f.type] ?? f.type}</small>
                </button>
              </li>
            ))
          ) : (
            <li className="tl-empty-line">Nothing called “{q.trim()}”. Labels are goals, projects, habits and tasks.</li>
          )}
        </ul>
      ) : null}
    </div>
  );
}
