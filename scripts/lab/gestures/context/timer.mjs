// timer (interaction 5): Start on home's card opens the timer, which offers the shown context first;
// starting it makes the card count; Stop on the card ends it, with Undo. Run with --state demo.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("home's card, idle", (a, s) => shown(a, "home:card:start", s));
  await tap("home:card:start");
  await wait(500);
  await expect("the timer, the context first", (a, s) => shown(a, "sheet:timer", s) && shown(a, "label:shown", s));
  await tap("label:shown");
  await wait(600);
  await expect("running on the card", (a, s) => shown(a, "home:card:readout", s) && shown(a, "home:card:stop", s));
  await tap("home:card:stop");
  await expect("stopped, with Undo", (a, s) => !shown(a, "home:card:readout", s) && shown(a, "toast:undo", s));
}
