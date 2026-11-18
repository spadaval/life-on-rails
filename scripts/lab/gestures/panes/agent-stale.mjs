// agent stale: with LoR not answering, the agent page says its flags are what LoR last sent, above
// them, and their Done and Seen are off. Run with --state stale.
import { expect, tap, wait } from "../../gesture.mjs";
import { anyShown, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("nav:Agent");
  await wait(700);
  await expect("the LoR note above the flags", (a, s) => shown(a, "note:lor", s) && anyShown(a, "done:flag:", s));
}
