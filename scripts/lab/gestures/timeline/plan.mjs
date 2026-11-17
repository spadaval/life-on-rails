// plan: a free gap on the route takes a task due with no time; it is planned there, dashed in cyan,
// and leaves the untimed list; Undo takes it back. Run with --state demo.
import { anchors, expect, swipe, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(700);
  const s = { width: 480, height: 1120 };
  const gap = first(await anchors(), "plan:", s);
  if (!gap) throw new Error("no free gap on screen");
  await tap(gap);
  await wait(400);
  await expect("tasks to plan", (a, sc) => shown(a, "sheet:plan", sc) && first(a, "plan-task:", sc));
  const task = first(await anchors(), "plan-task:", s);
  const title = task.slice("plan-task:".length);
  await tap(task);
  await wait(500);
  await expect(`${title} planned on the route`, (a, sc) => shown(a, `planned:${title}`, sc) && shown(a, "toast:undo", sc));
  await tap("toast:undo");
  await expect("Undo: the gap is free again", (a) => !(`planned:${title}` in a));
}
