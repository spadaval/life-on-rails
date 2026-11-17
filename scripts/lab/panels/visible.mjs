// Whether an anchor is wholly on the lab's screen, for the panels' scripts that scroll to one.
import { anchors } from "../gesture.mjs";

/** [height] is the screen's in dp: the cover screen's 1120 (the inner screen's is 971). */
export async function onScreen(name, height = 1120) {
  const r = (await anchors())[name];
  return !!r && r[1] >= 0 && r[3] <= height - 8 && r[3] > r[1];
}
