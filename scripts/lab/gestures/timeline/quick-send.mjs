// quick send (interaction 10): a double tap on home opens it; words and Send. With T3 live it says
// it went; with T3 away (--state agent-t3-stale) it says why not and keeps the words.
import { anchors, doubleTap, expect, screen, swipe, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  // A state that opens on the day page (agent-t3-stale) needs a swipe left to home first.
  if (!shown(await anchors(), "home:Phone", await screen())) { await swipe("left", 320, { duration: 260, from: [400, 600] }); await wait(600); }
  await doubleTap([240, 520]);
  await wait(500);
  await expect("quick send", (a, s) => shown(a, "sheet:quick", s) && shown(a, "quick:field", s));
  await tap("quick:field");
  await type("Find a physio near the studio for Thursday");
  await tap("quick:send");
  await expect("an answer: sent or not", (a, s) => shown(a, "quick:sent", s) || shown(a, "quick:failed", s));
  if (!("quick:sent" in (await anchors()))) return;
  // Sent: the field is empty again, and typing the next message brings Send back.
  await tap("quick:field");
  await type("And book it for 9:00");
  await expect("Send again for the second message", (a, s) => shown(a, "quick:send", s) && !shown(a, "quick:sent", s));
  await tap("quick:send");
  await expect("the second one sent", (a, s) => shown(a, "quick:sent", s));
}
