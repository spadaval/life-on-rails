// quick send (interaction 10): a double tap on home opens it; words and Send. With T3 live it says
// it went; with T3 away (--state agent-t3-stale) it says why not and keeps the words.
import { anchors, doubleTap, expect, screen, swipe, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  // A state that opens on the queue page (agent-t3-stale) needs a swipe left to home first.
  if (!shown(await anchors(), "home:Phone", await screen())) { await swipe("left", 320, { duration: 260, from: [400, 600] }); await wait(600); }
  await doubleTap([240, 520]);
  await wait(500);
  await expect("quick send", (a, s) => shown(a, "sheet:quick", s) && shown(a, "quick:field", s));
  await tap("quick:field");
  await type("Find a physio near the studio for Thursday");
  await tap("quick:send");
  await expect("an answer: sent or not", (a, s) => shown(a, "quick:sent", s) || shown(a, "quick:failed", s));
}
