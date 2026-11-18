// approve (interaction 3): Now leads with T3's cards; Approve on the card says it is going, with
// Cancel and no Undo, then the card clears and the question moves up. Run with --state agent-waiting.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("Now, the approval first", (a, s) => shown(a, "now", s) && shown(a, "approve:r-approve", s));
  await tap("approve:r-approve");
  await expect("going, with Cancel", (a, s) => shown(a, "cancel:r-approve", s) && !shown(a, "toast:undo", s));
  await wait(3600);
  await expect("cleared, no Undo; the question first", (a, s) => !("approve:r-approve" in a) && shown(a, "answer:r-question", s) && !shown(a, "toast:undo", s), 3000);
}
