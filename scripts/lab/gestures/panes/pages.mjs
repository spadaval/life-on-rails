// pages: the three pages follow the finger. From home a swipe right shows the agent page, two swipes
// left show Today, and the switcher at the top says which is showing. Run with --state demo.
import { expect, swipe, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(700);
  await expect("the agent page", (a, s) => shown(a, "agent:rows", s) && shown(a, "nav:Agent", s));
  await swipe("left", 300, { duration: 260, from: [400, 640] });
  await wait(700);
  await swipe("left", 300, { duration: 260, from: [400, 640] });
  await wait(700);
  await expect("Today", (a, s) => shown(a, "today:rows", s));
}
