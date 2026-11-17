// longPress: a held press on the tile opens its menu, and the finger lifts with it open.
// The readout ends "menu open".
import { longPress } from "../gesture.mjs";

export default async function () {
  await longPress("tile");
}
