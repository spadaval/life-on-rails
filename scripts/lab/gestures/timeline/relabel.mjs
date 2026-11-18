// relabel (the signature move): on the route, the time just before now with nothing recorded asks
// to be named; a tap lifts it into a sheet, a label counts it, and Undo takes it back. Then a
// guess is said yes to with its tick, also with Undo. Run with --state demo.
import { expect, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(700);
  await expect("47 minutes with nothing recorded, just above now", (a, s) => shown(a, "gap:14:13", s) && shown(a, "now", s));
  await tap("gap:14:13");
  await wait(400);
  await expect("its sheet, with labels", (a, s) => shown(a, "sheet:block", s) && shown(a, "label:Offline maps spec", s));
  await tap("label:Offline maps spec");
  await wait(500);
  await expect("counted: a tracked block where the gap was, with Undo", (a, s) => shown(a, "toast:undo", s) && !shown(a, "sheet:block", s) && shown(a, "block:Offline maps spec", s) && !("gap:14:13" in a));
  await tap("toast:undo");
  await expect("Undo: nothing recorded again", (a, s) => shown(a, "gap:14:13", s));
  await wait(400);
  await tap("confirm:b16");
  await expect("the guess said yes to: Beta design review counted as Meetings", (a, s) => !("confirm:b16" in a) && shown(a, "toast:undo", s));
}
