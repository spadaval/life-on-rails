// settings drag: from a settings section, a drag down on the sheet steps back to the section list,
// and the sheet returns to where it rests rather than staying where the finger left it; a second drag
// closes settings. Run with --state sync-failed.
import { anchors, drag, expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:sync");
  await wait(900);
  const rest = (await anchors())["sheet:settings"];
  await drag([[240, 92], [240, 1000]], 0, { speed: 800 });
  await wait(700);
  await expect("back at the sections, the sheet where it rests", (a, s) => !shown(a, "settings:pane", s) && shown(a, "section:sync", s) && Math.abs(a["sheet:settings"][1] - rest[1]) < 4);
  await drag([[240, 92], [240, 1000]], 0, { speed: 800 });
  await wait(700);
  await expect("closed", (a, s) => !shown(a, "sheet:settings", s) && shown(a, "home:Phone", s));
}
