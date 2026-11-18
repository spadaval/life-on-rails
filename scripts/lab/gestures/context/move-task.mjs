// move-task (review 1-2l4w.2): a task's ⋯ › Move to another context… opens the contexts to move it
// to; picking Public beta moves it there, with Undo, and the surface follows it there (it is the one thing's). Run with --state demo.
import { expect, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:now");
  await wait(700);
  await tap("task:Draft the offline maps spec");
  await wait(300);
  await tap("more:task:Draft the offline maps spec");
  await wait(300);
  await tap("menu:Move to another context…");
  await wait(500);
  await expect("the contexts to move it to", (a, s) => shown(a, "contexts:move", s) && "context:Public beta" in a);
  // Public beta is the last project: scroll to it.
  await swipe("up", 700, { duration: 500, from: [240, 1000], hold: 300 });
  await wait(1500);
  await tap("context:Public beta");
  await wait(600);
  // It is the one thing's task, so the current context follows it to Public beta.
  await expect("moved, with Undo: Now follows the one thing into Public beta", (a, s) => shown(a, "ctx:Public beta", s) && shown(a, "toast:undo", s) && "task:Draft the offline maps spec" in a);
  await tap("toast:undo");
  await wait(700);
  await expect("Undo puts it back under Offline maps", (a, s) => shown(a, "ctx:Offline maps", s) && "task:Draft the offline maps spec" in a);
}
