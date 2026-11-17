// `context`: everything about what you are doing, in one place (docs/design/lab/brief.md,
// "### 4. Context"). Item lor-vujy.
import type { Direction } from "../index.js";
import { App } from "./App.js";
import css from "./styles.css?url";

const direction: Direction = { css: [css], App };
export default direction;
