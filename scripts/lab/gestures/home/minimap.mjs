// minimap: a long press on empty home opens the home menu; Panels opens the minimap with home's
// start selected; its Remove is refused in the phone's words ("Can't: home starts there; choose
// another start first."). Ends with the minimap open and the bar saying why.
import { expect, longPress, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await longPress([240, 820]);
  await wait(400);
  await expect("the home menu", (a, s) => shown(a, "menu:Panels", s));
  await tap("menu:Panels");
  await wait(800);
  await expect("the minimap", (a, s) => shown(a, "map:Remove", s));
  await tap("map:Remove");
  await wait(500);
  await expect("the refusal", (a, s) => shown(a, "undo-bar", s));
}
