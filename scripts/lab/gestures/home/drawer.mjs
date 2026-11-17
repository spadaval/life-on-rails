// drawer: a swipe up from home opens the drawer (the cell under start), a swipe down closes it.
// Ends on home with "home:Phone" shown and no drawer search button.
import { expect, swipe, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("home on screen", (a, s) => shown(a, "home:Phone", s));
  await swipe("up", 420, { duration: 260, from: [240, 760] });
  await wait(700);
  await expect("the drawer open", (a, s) => shown(a, "drawer-search", s) && !shown(a, "home:Phone", s));
  await swipe("down", 420, { duration: 260, from: [240, 300] });
  await wait(700);
  await expect("home again", (a, s) => shown(a, "home:Phone", s) && !shown(a, "drawer-search", s));
}
