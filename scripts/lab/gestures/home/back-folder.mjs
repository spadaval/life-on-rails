// back-folder: a tap opens the Work folder; a predictive back drawn to 60% and let go shrinks it and
// brings it back; a full one closes it into its tile. Ends on home with the folder shut.
import { back, expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("folder:Work");
  await wait(700);
  await expect("the folder open", (a, s) => shown(a, "folder-open:Work", s));
  await back({ predictive: true, duration: 400, to: 0.6, cancel: true });
  await wait(500);
  await expect("still open after a cancelled back", (a, s) => shown(a, "folder-open:Work", s));
  await back({ predictive: true, duration: 350 });
  await wait(700);
  await expect("the folder shut", (a, s) => !shown(a, "folder-open:Work", s) && shown(a, "folder:Work", s));
}
