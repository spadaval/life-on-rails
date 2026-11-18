// drawer (interaction 1, recorded): a swipe up on home raises the drawer with the finger; a swipe
// down on its header closes it. Ends on home. Run with --version panes --state demo.
import { expect, swipe, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("home on screen", (a, s) => shown(a, "home:Phone", s) && shown(a, "dock:search", s));
  await swipe("up", 460, { duration: 280, from: [240, 640] });
  await wait(600);
  await expect("the drawer open: every app, and the field at the thumb", (a, s) => shown(a, "drawer:grid", s) && shown(a, "search-field", s));
  await swipe("down", 520, { duration: 280, from: [240, 70] });
  await wait(600);
  await expect("home again", (a, s) => shown(a, "home:Phone", s) && !shown(a, "drawer:grid", s));
}
