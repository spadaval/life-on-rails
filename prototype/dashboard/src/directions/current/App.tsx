// The shipped dashboard's look on the prototype's data: src/routes and src/components copied
// here with their server calls pointed at the store (server/functions/), so Sumeet can flip
// between today's screens and each concept. Copied on 2026-11-17 from 48477ea; it does not
// follow later changes to src/ by itself.
import { useEffect } from "react";
import type { Route } from "../../shell/nav.js";
import { Route as Goals } from "./routes/goals.js";
import { Route as Today } from "./routes/index.js";
import { Route as Notes } from "./routes/notes.js";
import { Route as People } from "./routes/people.js";
import { Route as Search } from "./routes/search.js";
import { Route as Tasks } from "./routes/tasks.js";
import { RootComponent } from "./routes/__root.js";

const SCREENS: Record<string, { options: { component: () => React.ReactNode } }> = {
  "/": Today,
  "/tasks": Tasks,
  "/goals": Goals,
  "/people": People,
  "/notes": Notes,
  "/search": Search,
};

export function App({ route }: { route: Route }) {
  const screen = SCREENS[route.path] ?? Today;
  const Screen = screen.options.component;
  const scroll = Number(route.params.scroll);
  useEffect(() => {
    document.title = "Life on Rails · current";
  }, []);
  return (
    <RootComponent add={route.params.add} scroll={Number.isFinite(scroll) ? scroll : undefined}>
      <Screen key={route.path} />
    </RootComponent>
  );
}
