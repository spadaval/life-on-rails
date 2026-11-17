// search-task (review 1-j6j3.1): a task search finds that isn't in the queue (no date) opens and
// stays open, and its date can be set. Run with --state demo (cover), or --screen inner, where it
// opens in the queue's right pane.
import { expect, screen, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export default async function () {
  const wide = (await screen()).width >= 600;
  await tap("home:search");
  await wait(500);
  await type("walnut");
  await tap("task:Cut the walnut boards to length");
  await wait(1200);
  await expect("the task open, and staying", (a, s) => shown(a, "detail", s) && shown(a, "due:Friday", s) && (wide || shown(a, "sheet:item", s)));
  await tap("due:Friday");
  await expect("dated, with Undo", (a, s) => shown(a, "toast:undo", s) && shown(a, "detail", s));
}
