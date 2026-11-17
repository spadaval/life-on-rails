// timer (interaction 5): Start on home's line opens the timer; a label starts it; Stop on the line
// stops it, with Undo. Run with --state demo.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:start");
  await expect("the timer sheet", (a, s) => shown(a, "sheet:timer", s) && shown(a, "label:Offline maps spec", s));
  await tap("label:Offline maps spec");
  await wait(500);
  await expect("running, with Stop on the line", (a, s) => shown(a, "home:stop", s));
  await wait(1500);
  await tap("home:stop");
  await expect("stopped, with Undo", (a, s) => shown(a, "home:start", s) && shown(a, "toast:undo", s));
}
