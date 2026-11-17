// menu (the required menu): a long press on WhatsApp opens its menu, which stays after the finger
// lifts; Back closes it. Run with --state demo.
import { back, expect, longPress, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await longPress("home:WhatsApp");
  await wait(400);
  await expect("the menu, still open", (a, s) => shown(a, "menu", s) && shown(a, "menu:App info", s));
  await back();
  await expect("closed", (a, s) => !shown(a, "menu", s));
}
