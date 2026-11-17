// answer (interaction 4): Other on the question takes free words; Send sends them after a moment
// to cancel, and the card clears. Run with --state agent-waiting.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("option:other");
  await expect("a field for the answer", (a, s) => shown(a, "answer-field", s));
  await tap("answer-field");
  await type("MapTiler for now");
  await tap("answer:send");
  await expect("going, with Cancel", (a, s) => shown(a, "cancel:r-question", s));
  await wait(3600);
  await expect("the question cleared", (a, s) => !("option:Stadia" in a), 3000);
}
