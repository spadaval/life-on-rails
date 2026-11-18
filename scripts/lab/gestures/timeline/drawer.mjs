// drawer (interaction 1): a swipe up on home raises the drawer with the finger; a swipe down on its
// search field closes it. Ends on home. Run with --version timeline --state demo.
import { expect, swipe, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("home on screen", (a, s) => shown(a, "home:Phone", s));
  await swipe("up", 460, { duration: 280, from: [240, 640] });
  await wait(600);
  await expect("the drawer open, home's icons covered", (a, s) => shown(a, "search-field", s) && shown(a, "drawer:grid", s));
  await swipe("down", 520, { duration: 280, from: [200, 72] });
  await wait(600);
  await expect("home again", (a, s) => shown(a, "home:Phone", s) && !shown(a, "search-field", s));
}
