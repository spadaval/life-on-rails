// overscroll (the lab page, any version): a sideways swipe the leftmost page doesn't use stays in
// the lab. Before the page set overscroll-behavior and touch-action (index.html), Chrome took a
// swipe right there as Back and left the lab, on Sumeet's phone as here. Run on any version with
// --state agent-waiting, which opens the leftmost page.
import { anchors, expect, swipe, wait } from "../../gesture.mjs";

export const link = { screen: "cover" };

export default async function () {
  const before = Object.keys(await anchors()).length;
  if (before === 0) throw new Error("no anchors before the swipe");
  for (const y of [300, 600, 900]) {
    await swipe("right", 420, { duration: 160, from: [20, y] });
    await wait(400);
  }
  await wait(800);
  let a = {};
  try { a = await anchors(); } catch (e) { throw new Error(`the lab is gone after a swipe right on the leftmost page (${e.message})`); }
  if (Object.keys(a).length === 0) throw new Error("the lab is gone after a swipe right on the leftmost page");
  await expect("still the lab, drawn", (an) => Object.keys(an).length > 0);
}
