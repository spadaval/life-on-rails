// Panes (lor-3wzs): a list and its detail side by side on every page. See App.tsx.
import type { Direction } from "../index.js";
import { App } from "./App.js";
import tokens from "./tokens.css?url";
import css from "./styles.css?url";

const direction: Direction = { css: [tokens, css], App };
export default direction;
