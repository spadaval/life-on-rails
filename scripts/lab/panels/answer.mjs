// answer: Answer opens the question's choices, a tap picks MapTiler, Send answer clears the card.
// node scripts/lab/run.mjs scripts/lab/panels/answer.mjs --version current --state agent-waiting --out <dir>
import { anchors, swipe, tap, wait } from "../gesture.mjs";
import { onScreen } from "./visible.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("card:t-question/answer");
  await wait(300);
  await tap("card:t-question/q0/o0");
  await wait(300);
  // The choices may push Send answer under the screen's foot: scroll the page up until it shows.
  for (let i = 0; i < 4 && !(await onScreen("card:t-question/send")); i++) {
    await swipe("up", 250);
    await wait(300);
  }
  await tap("card:t-question/send");
  await wait(400);
  if ("card:t-question" in (await anchors())) throw new Error("answer: the card is still there");
}
