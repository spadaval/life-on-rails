// tick (interaction 6): on Today, a task due is ticked on its row, struck, with Undo; Undo takes it
// back. Run with --state demo. Starts on home; the switcher shows Today.
import { anchors, expect, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("nav:Today");
  await wait(700);
  const s = { width: 480, height: 1120 };
  const tick = first(await anchors(), "tick:task:", s);
  if (!tick) throw new Error("no task due on screen");
  await tap(tick);
  await expect("ticked, with Undo", (a, sc) => shown(a, "toast:undo", sc));
  await tap("toast:undo");
  await expect("Undo took it back", (a, sc) => !shown(a, "toast:undo", sc) && shown(a, tick, sc));
}
