// pages: a swipe left goes to home's next page (the stack at [2, 0]), a swipe right comes back.
// Ends on the start page with "home:Phone" shown.
import { expect, swipe, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await swipe("left", 260, { duration: 240, from: [360, 700] });
  await wait(600);
  await expect("the second page", (a, s) => shown(a, "home:Photos", s) && !shown(a, "home:Phone", s));
  await swipe("right", 260, { duration: 240, from: [120, 700] });
  await wait(600);
  await expect("the start page", (a, s) => shown(a, "home:Phone", s) && !shown(a, "home:Photos", s));
}
