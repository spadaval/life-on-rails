// answer (interaction 4): Answer on the pinned question opens it with its options and their
// descriptions; Other takes free words, and Send sends them after a moment to cancel; the card
// clears. Run with --state agent-waiting.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("answer:r-question");
  await wait(400);
  await expect("the question open, its options described", (a, s) => shown(a, "sheet:item", s) && shown(a, "option:MapTiler", s) && shown(a, "option:other", s));
  await tap("option:other");
  await expect("a field for the answer", (a, s) => shown(a, "answer-field", s));
  await tap("answer-field");
  await type("MapTiler for now");
  await tap("answer:send");
  await expect("going, with Cancel", (a, s) => shown(a, "detail:cancel:r-question", s));
  await wait(3600);
  await expect("the question cleared, its sheet closed", (a, s) => !("answer:r-question" in a) && !shown(a, "sheet:item", s), 3000);
}
