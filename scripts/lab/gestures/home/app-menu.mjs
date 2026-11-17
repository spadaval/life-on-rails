// app-menu: a long press on WhatsApp opens its menu by the icon: Move to tab…, then App info,
// Remove, Hide and Uninstall. Ends with the menu open.
import { expect, longPress, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await longPress("home:WhatsApp");
  await wait(400);
  await expect("WhatsApp's menu", (a, s) => shown(a, "menu", s) && shown(a, "menu:Move to tab…", s));
}
