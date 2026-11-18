// answer-pick (interaction 4, one tap): a tap on an option sends it after a moment to cancel. Run with --state agent-waiting.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("answer:r-question");
  await wait(500);
  await tap("option:Stadia");
  await expect("going, with Cancel, no Undo", (a, s) => shown(a, "detail:sending:r-question", s) && !shown(a, "toast:undo", s));
  await wait(3800);
  await expect("sent", (a, s) => shown(a, "toast", s) && !shown(a, "detail:sending:r-question", s), 3000);
}
