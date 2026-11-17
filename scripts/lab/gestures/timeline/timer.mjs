// timer (interaction 5): Start on home's strip opens the timer; a label starts it; Stop on the
// strip stops it, with Undo. Then on the day page, a recent label under the now line starts one
// that grows from the line, and Stop on the route stops it. Run with --state demo.
import { expect, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:start");
  await expect("the timer sheet", (a, s) => shown(a, "sheet:timer", s) && shown(a, "label:Offline maps spec", s));
  await tap("label:Offline maps spec");
  await wait(500);
  await expect("running, with Stop on the strip", (a, s) => shown(a, "home:stop", s) && shown(a, "home:readout", s));
  await wait(1500);
  await tap("home:stop");
  await expect("stopped, with Undo", (a, s) => shown(a, "home:start", s) && shown(a, "toast:undo", s));
  await wait(4500);
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(700);
  await expect("the day page, Start under now", (a, s) => shown(a, "route:start:Offline maps spec", s));
  await tap("route:start:Offline maps spec");
  await expect("a block growing from the now line", (a, s) => shown(a, "route:running", s) && shown(a, "route:stop", s));
  await wait(1500);
  await tap("route:stop");
  await expect("stopped again, with Undo", (a, s) => shown(a, "route:start", s) && shown(a, "toast:undo", s));
}
