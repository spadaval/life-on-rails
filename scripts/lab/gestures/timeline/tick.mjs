// tick (interaction 6): on the day page, the line for tasks due with no time opens them; one is
// ticked, struck, and Undo brings it back. Run with --state demo. Starts on home; a swipe right
// shows the day page.
import { anchors, expect, swipe, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(600);
  await expect("the day page", (a, s) => shown(a, "untimed", s) && shown(a, "now", s));
  await tap("untimed");
  await wait(500);
  const s = { width: 480, height: 1120 };
  const tick = first(await anchors(), "tick:task:", s);
  if (!tick) throw new Error("no untimed task on screen");
  await tap(tick);
  await expect("ticked, with Undo", (a, sc) => shown(a, "toast:undo", sc));
  await tap("toast:undo");
  await expect("Undo took it back", (a, sc) => !shown(a, "toast:undo", sc) && shown(a, tick, sc));
}
