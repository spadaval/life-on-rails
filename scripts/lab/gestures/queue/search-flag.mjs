// search-flag (review 2-5x97.4): a flagged task opened from search and marked Done leaves the
// queue's flags too, with one Undo for both. Run with --state demo.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };
const FLAG = "flag:00000000-0000-7000-8000-000000000001";

export default async function () {
  await expect("the flag in the queue", (a) => `item:${FLAG}` in a);
  await tap("home:search");
  await wait(500);
  await type("dinner");
  await tap("task:Organise Sam's birthday dinner");
  await wait(800);
  await expect("opened as its flag", (a, s) => shown(a, "detail", s) && shown(a, `done:${FLAG}`, s));
  await tap(`done:${FLAG}`);
  await expect("done, and gone from the queue", (a, s) => shown(a, "toast:undo", s) && !(`item:${FLAG}` in a), 3000);
  await tap("toast:undo");
  await expect("Undo puts the flag back", (a) => `item:${FLAG}` in a, 3000);
}
