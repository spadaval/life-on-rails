// answer-cancel (review 1-fbke.4): an answer cancelled in its window keeps what was typed, so it
// can be revised and sent. Run with --state agent-waiting.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("answer:r-question");
  await expect("the question open", (a, s) => shown(a, "sheet:item", s) && shown(a, "option:other", s));
  await wait(500);
  await tap("option:other");
  await expect("a field for the answer", (a, s) => shown(a, "answer-field", s));
  await tap("answer-field");
  await type("keep this answer");
  await tap("answer:send");
  await expect("going, with Cancel in the open card", (a, s) => shown(a, "detail:cancel:r-question", s));
  await tap("detail:cancel:r-question");
  await expect("cancelled: the words still there", (a, s) => shown(a, "answer-field", s) && shown(a, "answer:send", s) && shown(a, "sheet:item", s));
  await tap("answer:send");
  await wait(3600);
  await expect("sent this time, the card cleared", (a, s) => !("answer:r-question" in a) && !shown(a, "sheet:item", s), 3000);
}
