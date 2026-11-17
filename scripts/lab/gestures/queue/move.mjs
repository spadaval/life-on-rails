// move (interaction 7, the recorded drag): a long press on Camera opens its menu, which stays while
// the finger rests; a drag from there lifts the icon, the menu goes, and Camera lands after T3 Code.
import { anchors, drag, expect, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  const before = (await anchors())["home:Camera"];
  await drag(["home:Camera", { anchor: "home:Camera", by: [10, 0] }, { anchor: "home:T3 Code", by: [0, 0] }], [700, 400, 600], { speed: 400 });
  await wait(900);
  await expect("no menu, and Camera moved down", (a, s) => !shown(a, "menu", s) && shown(a, "home:Camera", s) && a["home:Camera"][1] > before[1] + 50 && shown(a, "toast:undo", s));
}
