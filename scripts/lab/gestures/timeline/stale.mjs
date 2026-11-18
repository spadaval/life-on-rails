// stale (review 1-fbke.3): while LoR isn't answering, no path says yes to a guess: the route has no
// ticks, the day's menu has no "Say yes to every guess", and a guess's sheet can't confirm.
// Run with --state stale; it opens on home, and a swipe right shows the day page.
import { anchors, expect, back, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(700);
  await expect("the stale day: guesses without ticks", (a, s) => shown(a, "note:lor", s) && shown(a, "block:Lunch with Priya", s) && !Object.keys(a).some((k) => k.startsWith("confirm:")));
  await tap("day:more");
  await wait(500);
  await expect("the day's menu, with no yes to every guess", (a, s) => shown(a, "menu", s) && !Object.keys(a).some((k) => k.startsWith("menu:Say yes")));
  await back();
  await wait(400);
  await tap("block:Lunch with Priya");
  await wait(600);
  await tap("sheet:confirm");
  await wait(500);
  await expect("the guess still a guess: nothing saved, no Undo", (a, s) => shown(a, "sheet:block", s) && !shown(a, "toast:undo", s));
}
