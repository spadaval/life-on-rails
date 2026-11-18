// quick send (interaction 10, failure): with T3 away it says it didn't go and why, keeps the words,
// and offers Try again. Run with --state agent-t3-stale, which opens the agent page: the switcher
// goes home first.
import { doubleTap, expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("nav:Home");
  await wait(700);
  await doubleTap([240, 520]);
  await wait(500);
  await tap("quick:field");
  await type("Check the tile cache numbers");
  await tap("quick:send");
  await expect("not sent, the words kept, Try again", (a, s) => shown(a, "quick:failed", s) && shown(a, "quick:failed:action", s));
}
