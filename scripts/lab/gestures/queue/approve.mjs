// approve (interaction 3): Approve on the lead card shows it going, with Cancel, then the card
// clears and the question slides up into the reticle. Run with --state agent-waiting.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("the approval leads", (a, s) => shown(a, "approve:r-approve", s) && shown(a, "reticle", s));
  await tap("approve:r-approve");
  await expect("going, with Cancel", (a, s) => shown(a, "cancel:r-approve", s));
  await wait(3600);
  await expect("cleared; the question now leads", (a, s) => !("approve:r-approve" in a) && shown(a, "option:Stadia", s) && shown(a, "reticle", s), 3000);
}
