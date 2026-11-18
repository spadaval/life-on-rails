// add-goal (review 1-2l4w.1): search's field reads "#offline" as the Offline maps context and shows
// it before acting; Add puts the task there, and Undo takes back both. A #word that names no context is refused, not dropped.
// Run with --state demo.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("dock:search");
  await wait(500);
  await type("Example tomorrow #nowhere");
  await expect("refused: no context matches", (a, s) => shown(a, "understood:no-goal", s) && !("understood:add" in a));
  await tap("search:clear");
  await tap("search-field");
  await wait(300);
  await type("Example tomorrow #offline");
  await expect("Add task, under Offline maps", (a, s) => shown(a, "understood:add", s));
  await tap("understood:add");
  await expect("added, with Undo", (a, s) => shown(a, "toast:undo", s));
  await tap("dock:search");
  await wait(500);
  await type("Example");
  await expect("found", (a, s) => shown(a, "task:Example", s));
  await tap("task:Example");
  await wait(900);
  await expect("it opens in Offline maps, not Loose ends", (a, s) => shown(a, "ctx:Offline maps", s) && "task:open" in a);
  // Undo takes back the task and its context together: a task added next, with no #goal, goes to
  // Loose ends, not to the undone task's context (review 2-dlid.2).
  await tap("toast:undo");
  await wait(400);
  await tap("dock:search");
  await wait(500);
  await type("Second tomorrow");
  await tap("understood:add");
  await expect("Second added", (a, s) => shown(a, "toast:undo", s));
  await tap("dock:search");
  await wait(500);
  await type("Second");
  await tap("task:Second");
  await wait(900);
  await expect("Second opens in Loose ends", (a, s) => shown(a, "ctx:Loose ends", s) && "task:open" in a);
}
