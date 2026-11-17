// drift: a long press on Camera opens its menu; drifting on picks it up (the menu shuts) and carries
// it to the end of the grid, past T3 Code, where it lands. Ends with Camera after T3 Code.
import { anchors, drag, expect, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  const before = (await anchors())["home:Camera"];
  await drag(["home:Camera", { anchor: "home:Camera", by: [24, 0] }, { anchor: "home:T3 Code", by: [0, 98] }], [700, 300, 800], { speed: 450 });
  await wait(900);
  await expect("no menu, and Camera moved", (a, s) => !shown(a, "menu", s) && shown(a, "home:Camera", s) && a["home:Camera"][1] > before[1] + 50);
}
