// drag: a long press on the tile (the menu opens), held, then drawn right and down with a pause
// on the way; drifting past 8 dp shuts the menu. The readout ends "tile 216,136 · menu shut".
import { drag } from "../gesture.mjs";

export default async function () {
  await drag(["tile", { anchor: "tile", by: [120, 0] }, { anchor: "tile", by: [200, 120] }], [700, 250, 100]);
}
