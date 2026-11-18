// settings back: Fix on home opens settings at the sync, stacked over the section list; Back returns
// to the list, and the next Back closes settings. Run with --state sync-failed.
import { back, expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:sync");
  await wait(500);
  await expect("the sync section over the list", (a, s) => shown(a, "settings:pane", s) && shown(a, "sync:failed", s));
  await back();
  await wait(500);
  await expect("Back returned to the sections", (a, s) => !shown(a, "settings:pane", s) && shown(a, "section:sync", s) && shown(a, "sheet:settings", s));
  await back();
  await wait(500);
  await expect("the next Back closed settings", (a, s) => !shown(a, "sheet:settings", s) && shown(a, "home:Phone", s));
}
