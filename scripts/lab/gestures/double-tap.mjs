// doubleTap: two quick taps on the tile. The readout ends "double taps 1".
import { doubleTap, wait } from "../gesture.mjs";

export default async function () {
  await doubleTap("tile");
  await wait(200);
}
