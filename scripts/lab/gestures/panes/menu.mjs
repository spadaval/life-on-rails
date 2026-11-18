// menu (one menu): a long press on WhatsApp opens its menu beside it, with its conversations; it
// stays after the finger lifts, and Back closes it. Run with --state demo.
import { back, expect, longPress, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await longPress("home:WhatsApp");
  await wait(400);
  await expect("the menu, with a conversation", (a, s) => shown(a, "menu", s) && shown(a, "menu:Maya Okafor", s));
  await back();
  await wait(300);
  await expect("closed by Back", (a, s) => !shown(a, "menu", s));
}
