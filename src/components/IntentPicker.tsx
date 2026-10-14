import { useState } from "react";

export interface PickIntent {
  id: string;
  title: string;
  type?: string;
}

/** Recent intents as one-tap chips, then type to search the rest. */
export function IntentPicker({ intents, recent, onPick, inputId }: {
  intents: PickIntent[];
  recent: PickIntent[];
  onPick: (intent: PickIntent) => void;
  inputId?: string;
}) {
  const [q, setQ] = useState("");
  const needle = q.trim().toLowerCase();
  const matches = needle
    ? intents.filter((i) => i.title.toLowerCase().includes(needle)).slice(0, 8)
    : recent.length > 0
      ? recent
      : intents.slice(0, 8);
  return (
    <div className="space-y-2">
      <input
        id={inputId}
        className="input"
        placeholder="Search goals, projects, habits, tasks…"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && matches[0]) {
            e.preventDefault();
            onPick(matches[0]);
          }
        }}
      />
      <div className="flex flex-wrap gap-2">
        {matches.map((i) => (
          <button key={i.id} type="button" className="btn" onClick={() => onPick(i)}>
            {i.title}
            {i.type && i.type !== "system" ? <span className="muted ml-1 text-xs">{i.type}</span> : null}
          </button>
        ))}
        {matches.length === 0 ? <span className="muted text-sm">No match.</span> : null}
      </div>
    </div>
  );
}
