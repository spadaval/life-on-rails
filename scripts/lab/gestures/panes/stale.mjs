// stale: LoR last answered two hours ago. Today says so above the list it last sent; Done, Seen, the
// ticks, the timer and the due date are off, and tapping them saves nothing (no Undo notice). Run
// with --state stale.
import { anchors, expect, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("home says LoR isn't answering", (a, s) => shown(a, "home:stale", s));
  await tap("nav:Today");
  await wait(700);
  await expect("Today, with the note", (a, s) => shown(a, "note:lor", s) && shown(a, "today:rows", s));
  const s = { width: 480, height: 1120 };
  const a = await anchors();
  for (const name of [first(a, "done:flag:", s), first(a, "tick:task:", s), "chip:start"]) {
    await tap(name);
    await wait(300);
    await expect(`${name} saved nothing`, (b, sc) => !shown(b, "toast:undo", sc) && !shown(b, "sheet:timer", sc));
  }
  await tap(first(await anchors(), "row:task:", s));
  await wait(500);
  await tap("due:Tomorrow");
  await wait(300);
  await expect("the due date saved nothing", (b, sc) => shown(b, "sheet:today", sc) && !shown(b, "toast:undo", sc));
}
