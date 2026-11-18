// stale-inner (review 2-wum1.7): unfolded, with LoR not answering, the right pane's tasks due with
// no time can't be ticked, as the cover's sheet can't. Run with --state stale --screen inner.
import { anchors, expect, swipe, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await swipe("right", 400, { duration: 260, from: [120, 640] });
  await wait(700);
  await expect("the stale day, unfolded", (a, s) => shown(a, "note:lor", s) && shown(a, "pane:waiting", s));
  const p = (await anchors())["pane:waiting"];
  await swipe("up", 600, { duration: 400, from: [Math.round((p[0] + p[2]) / 2), 850], hold: 120 });
  await wait(600);
  const tick = first(await anchors(), "tick:task:", { width: 875, height: 971 });
  if (!tick) throw new Error("no task tick in the pane");
  await tap(tick);
  await wait(500);
  await expect("nothing saved: no Done notice", (a, s) => !shown(a, "toast:undo", s));
}
