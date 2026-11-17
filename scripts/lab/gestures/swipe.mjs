// swipe: the pager follows a finger drawn 300 dp left over half a second, held still before it
// lifts, then a second swipe left. The readout ends "page 3/5".
import { swipe, wait } from "../gesture.mjs";

export default async function () {
  await swipe("left", 300, { from: { anchor: "pager", at: [0.85, 0.5] }, duration: 500, hold: 150 });
  await wait(300);
  await swipe("left", 300, { from: { anchor: "pager", at: [0.85, 0.5] }, duration: 250 });
}
