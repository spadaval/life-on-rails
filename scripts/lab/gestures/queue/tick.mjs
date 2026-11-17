// tick (interaction 6): on the queue, a task due today is ticked, struck, and Undo brings it back.
// Run with --state demo, which has tasks due. Starts on home; a swipe right shows the queue.
import { anchors, expect, swipe, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("right", 300, { duration: 260, from: [80, 500] });
  await wait(600);
  await expect("the queue", (a, s) => shown(a, "queue:count", s));
  await swipe("up", 500, { duration: 400, from: [240, 900], hold: 120 });
  await wait(500);
  const s = { width: 480, height: 1120 };
  const tick = first(await anchors(), "tick:task:", s);
  if (!tick) throw new Error("no task due on screen");
  await tap(tick);
  await expect("ticked, with Undo", (a, sc) => shown(a, "toast:undo", sc));
  await tap("toast:undo");
  await expect("Undo took it back", (a, sc) => !shown(a, "toast:undo", sc) && shown(a, tick, sc));
}
