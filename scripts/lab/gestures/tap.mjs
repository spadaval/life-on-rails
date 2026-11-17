// tap: a tap on the list's third row selects it. The readout ends "row 3".
import { tap, wait } from "../gesture.mjs";

export default async function () {
  await tap({ anchor: "list", at: [0.3, 0], by: [0, 2.5 * 48] });
  await wait(200);
}
