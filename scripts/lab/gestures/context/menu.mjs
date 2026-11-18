// menu: a long press on an app opens its menu, which stays while the finger rests. Run with --state demo.
import { expect, longPress, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await longPress("home:WhatsApp");
  await wait(400);
  await expect("WhatsApp's menu", (a, s) => shown(a, "menu", s) && shown(a, "menu:Open", s));
}
