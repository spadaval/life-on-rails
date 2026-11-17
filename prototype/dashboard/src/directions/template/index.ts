// The template a direction item copies: `cp -r template <name>`, then add it to ../index.ts.
// It shows the whole contract in one small page: own stylesheet, the route, the store's
// lists and actions, the clock, and undo. Replace all of it.
import type { Direction } from "../index.js";
import { App } from "./App.js";
import css from "./styles.css?url";

const direction: Direction = { css: [css], App };
export default direction;
