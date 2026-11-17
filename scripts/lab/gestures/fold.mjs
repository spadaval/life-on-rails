// fold: the pager turned to page 2 and the list flung, then a swipe on the pager that unfolds the
// phone halfway through, finger still down, and lifts on the inner screen's two panes. The page,
// the list's scroll and the tile keep their place through the fold (the readout ends "page 2/5 ·
// ... two panes 875×971"). The swipe in progress does not: the pager moves into the left pane,
// so its drag starts again from where the finger is, as a layout change does on the phone.
import { at, down, fling, fold, move, swipe, up, wait } from "../gesture.mjs";

export default async function () {
  await swipe("left", 300, { from: { anchor: "pager", at: [0.85, 0.5] }, duration: 300, hold: 100 });
  await fling("up", 3000, { from: { anchor: "list", at: [0.5, 0.8] } });
  await wait(500);
  const start = await at({ anchor: "pager", at: [0.6, 0.5] });
  await down(start);
  await move([start[0] - 60, start[1]], 150);
  await fold("inner");
  await move([start[0] - 120, start[1]], 150);
  await up();
  await wait(300);
}
