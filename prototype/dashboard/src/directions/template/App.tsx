import { useState } from "react";
import { openTasks, needsYou, search } from "../../data/derive.js";
import { running, setDone, startTimer, stopTimer, undo, unflag, useNow, useStore, useUndo } from "../../data/store.js";
import { href, type Route } from "../../shell/nav.js";

export function App({ route }: { route: Route }) {
  const s = useStore();
  const now = useNow(1000);
  const last = useUndo();
  const [q, setQ] = useState(route.params.q ?? "");
  const r = running(s);
  const tasks = openTasks(s).slice(0, 8);
  return (
    <main className="tpl">
      <header>
        <h1>Template</h1>
        <nav>
          <a href={href("/")}>Home</a> · <a href={href("/search", { q: "lisbon" })}>Search “lisbon”</a>
        </nav>
        <p>
          Path <code>{route.path}</code>, at {new Date(now).toLocaleTimeString()}.
        </p>
      </header>
      <section>
        <h2>Timer</h2>
        {r ? (
          <p>
            {r.intent ?? "No label"}, {Math.round((now - Date.parse(r.t_start)) / 60000)} min <button onClick={stopTimer}>Stop</button>
          </p>
        ) : (
          <button onClick={() => startTimer()}>Start</button>
        )}
      </section>
      <section>
        <h2>Needs you</h2>
        {needsYou(s).map((n) => (
          <p key={n.id}>
            {n.title} <button onClick={() => unflag(n.id)}>Seen</button>
          </p>
        ))}
      </section>
      <section>
        <h2>Tasks</h2>
        {tasks.map((t) => (
          <label key={t.id}>
            <input type="checkbox" checked={t.done} onChange={(e) => setDone(t.id, e.target.checked)} /> {t.title}
          </label>
        ))}
      </section>
      <section>
        <h2>Search</h2>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search" />
        {search(s, route.path === "/search" && !q ? (route.params.q ?? "") : q).slice(0, 6).map((h) => (
          <p key={h.kind + h.id}>
            {h.kind}: {h.title}
          </p>
        ))}
      </section>
      {last ? (
        <p className="tpl-undo">
          {last}. <button onClick={() => undo()}>Undo</button>
        </p>
      ) : null}
    </main>
  );
}
