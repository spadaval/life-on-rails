// search: the drawer by a swipe up, its search button, "str" typed (the S and R the lab's toolbar
// would take go to the field), and Strava opened from the results. In the lab, opening an app says
// so in home's bar; nothing folds, turns or changes speed.
import { expect, swipe, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("up", 420, { duration: 260, from: [240, 760] });
  await wait(700);
  await tap("drawer-search");
  await wait(400);
  await type("str");
  await wait(600);
  await expect("Strava among the results", (a, s) => shown(a, "search:Strava", s) && shown(a, "search-field", s));
  await tap("search:Strava");
  await wait(400);
  await expect("the bar saying Strava opened", (a, s) => shown(a, "undo-bar", s));
}
