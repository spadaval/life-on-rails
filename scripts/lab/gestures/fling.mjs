// fling: a flick up the list at 4000 dp/s carries it on after the finger lifts.
// The list runs on to its end: the readout ends "top row 50", where the finger moved only 120 dp
// (two and a half rows).
import { fling } from "../gesture.mjs";

export default async function () {
  await fling("up", 4000, { from: { anchor: "list", at: [0.5, 0.8] }, distance: 120 });
}
