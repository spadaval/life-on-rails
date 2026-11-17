// fling: a flick up the list at 4000 dp/s carries it on after the finger lifts.
// The list runs on far past the 120 dp the finger moved (two and a half rows): the readout ends
// at "top row 50", the list's end, or about 45. Identical runs settle differently as Chrome
// samples the moves differently (lor-293p's review, round 2), so don't aim a fling at one row.
import { fling } from "../gesture.mjs";

export default async function () {
  await fling("up", 4000, { from: { anchor: "list", at: [0.5, 0.8] }, distance: 120 });
}
