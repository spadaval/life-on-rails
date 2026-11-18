// answer (interaction 4): Answer on the question's card opens its thread in a sheet with the options
// and their descriptions; "Something else" takes his words; Send gives a moment to cancel, then the
// card clears. Run with --state agent-waiting.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("answer:r-question");
  await wait(500);
  await expect("the question in its sheet, its options described", (a, s) => shown(a, "sheet:open", s) && shown(a, "option:MapTiler", s) && shown(a, "option:other", s));
  await tap("option:other");
  await expect("a field for the answer", (a, s) => shown(a, "answer-field", s));
  await tap("answer-field");
  await type("MapTiler for now");
  await tap("answer:send");
  await expect("going, with Cancel", (a, s) => shown(a, "detail:sending:r-question", s));
  await wait(3600);
  await tap("sheet:open-scrim");
  await wait(500);
  await expect("the card cleared from Now", (a, s) => !shown(a, "answer:r-question", s) && shown(a, "approve:r-approve", s), 3000);
}
