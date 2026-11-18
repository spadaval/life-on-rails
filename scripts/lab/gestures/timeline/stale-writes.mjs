// stale-writes (review 3-hx8l.3): with LoR not answering, a task's menu offers no date change, and
// search's Add task saves nothing and keeps the words. Run with --state stale --screen inner.
import { anchors, expect, swipe, tap, type, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await swipe("right", 400, { duration: 260, from: [120, 640] });
  await wait(700);
  await expect("the stale day, unfolded", (a, s) => shown(a, "note:lor", s) && shown(a, "pane:waiting", s));
  const p = (await anchors())["pane:waiting"];
  await swipe("up", 600, { duration: 400, from: [Math.round((p[0] + p[2]) / 2), 850], hold: 120 });
  await wait(600);
  const more = first(await anchors(), "more:task:", { width: 875, height: 971 });
  if (!more) throw new Error("no task menu in the pane");
  await tap(more);
  await wait(500);
  await expect("the task's menu, with no date change", (a, s) => shown(a, "menu", s) && !("menu:No date" in a) && !("menu:Due tomorrow" in a));
  await tap([40, 940]);
  await wait(400);
  await swipe("left", 400, { duration: 260, from: [700, 640] });
  await wait(700);
  await tap("home:search");
  await wait(600);
  await type("test today");
  await expect("Add task offered", (a, s) => shown(a, "understood:add", s));
  await tap("understood:add");
  await wait(500);
  await expect("nothing added: no Undo, the words kept", (a, s) => !shown(a, "toast:undo", s) && shown(a, "understood:add", s));
}
