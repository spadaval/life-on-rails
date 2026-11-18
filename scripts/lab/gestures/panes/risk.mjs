// risk (the brief's "nested scrolls and half-height sheets make Back and dismissal easy to get
// wrong"): the thread in its half sheet on the cover. A drag down on the transcript scrolls the
// transcript, not the sheet; the sheet lowers only from its handle (or once the transcript is at its
// first message). Back with the keyboard up lowers the keyboard, not the sheet; the next Back closes
// the sheet and leaves the page where it was. Run with --state thread-long.
import { anchors, back, expect, swipe, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  const s = { width: 480, height: 1120 };
  await expect("the thread at half height, the list live above", (a, sc) => shown(a, "sheet:agent:half", sc) && shown(a, "agent:rows", sc));
  await swipe("down", 220, { duration: 400, from: [240, 900], hold: 150 });
  await wait(800);
  const at = first(await anchors(), "thread-at:", s);
  await expect(`a drag on the transcript scrolled it (${at}), and the sheet stayed`, (a, sc) => at !== "thread-at:0" && shown(a, "sheet:agent:half", sc));
  await tap("reply-field");
  await expect("the keyboard up", (a, sc) => shown(a, "keyboard", sc));
  await back();
  await wait(400);
  await expect("Back lowered the keyboard; the sheet stays", (a, sc) => !shown(a, "keyboard", sc) && shown(a, "sheet:agent", sc));
  await back();
  await wait(500);
  await expect("the next Back closed the sheet, on the same page", (a, sc) => !shown(a, "sheet:agent", sc) && shown(a, "agent:rows", sc));
}
