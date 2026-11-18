// answer (interaction 4): Answer on the question's row opens it in a sheet at full height (a
// question's answers are why it is opened; a tap on the row opens it at half), its options described; "Something else" takes his words, and Send sends them after a
// moment to cancel; the card clears and the sheet shows its thread. Run with --state agent-waiting.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("nav:Agent");
  await wait(700);
  await tap("answer:r-question");
  await wait(500);
  await expect("the question in its sheet, its options described", (a, s) => shown(a, "sheet:agent:full", s) && shown(a, "option:MapTiler", s) && shown(a, "option:other", s));
  await tap("option:other");
  await expect("a field for the answer", (a, s) => shown(a, "answer-field", s));
  await tap("answer-field");
  await type("MapTiler for now");
  await tap("answer:send");
  await expect("going, with Cancel", (a, s) => shown(a, "detail:sending:r-question", s));
  await wait(3600);
  await expect("the card cleared from the list", (a, s) => !shown(a, "answer:r-question", s), 3000);
}
