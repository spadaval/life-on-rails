// answer, several: a question that takes several answers, unfolded beside the list; two options are
// ticked and sent together. Run with --state agent-waiting --screen inner, the multi-answer card
// made by a one-off (see the Report).
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await tap("nav:Agent");
  await wait(700);
  await tap("answer:r-question");
  await wait(500);
  await expect("the question beside the list", (a, s) => shown(a, "pane:detail", s) && shown(a, "option:MapTiler", s));
  await tap("option:MapTiler");
  await tap("option:Stadia");
  await expect("two picked, Send names them", (a, s) => shown(a, "answer:send", s));
  await tap("answer:send");
  await expect("going, with Cancel", (a, s) => shown(a, "detail:sending:r-question", s));
}
