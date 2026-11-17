// `current`: the shipped dashboard's look (Elite: Mostly Harmless) on the same made-up data.
import type { Direction } from "../index.js";
import { App } from "./App.js";
import css from "./styles.css?url";

const direction: Direction = { css: [css], App };
export default direction;
