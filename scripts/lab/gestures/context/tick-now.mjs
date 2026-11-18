// tick-now (interaction 6, on Now): a task opens in place in its row with its due date; its check ticks it. Run with --state demo.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:now");
  await wait(700);
  await tap("task:Draft the offline maps spec");
  await expect("open in place, its due date", (a, s) => shown(a, "task:open", s) && shown(a, "due:Tomorrow", s));
  await tap("due:Fri");
  await expect("due Friday, with Undo", (a, s) => "due:is:2026-11-20" in a && shown(a, "toast:undo", s));
  await tap("tick:Draft the offline maps spec");
  await expect("ticked, with Undo", (a, s) => shown(a, "toast:undo", s));
}
