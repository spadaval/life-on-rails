// approve (interaction 3): on the day page, Approve on the pinned approval shows it going, with
// Cancel, then the card clears and the question moves up into its place. Run with --state agent-waiting.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("the approval pinned over the route", (a, s) => shown(a, "approve:r-approve", s) && shown(a, "pinned", s) && shown(a, "now", s));
  await tap("approve:r-approve");
  await expect("going, with Cancel", (a, s) => shown(a, "cancel:r-approve", s));
  await wait(3600);
  await expect("cleared; the question pinned first", (a, s) => !("approve:r-approve" in a) && shown(a, "answer:r-question", s), 3000);
}
