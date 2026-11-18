// tick (interaction 6): the next task on home's card ticks with one tap and offers Undo; Undo
// brings it back. Run with --state demo.
import { expect, tap } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:card:tick");
  await expect("done, with Undo", (a, s) => shown(a, "toast:undo", s));
  await tap("toast:undo");
  await expect("back, not done", (a, s) => shown(a, "home:card:tick", s) && !shown(a, "toast:undo", s), 2500);
}
