// back: the menu opened by a long press, then a predictive back drawn to 60% and let go (the
// menu shrinks and comes back), then a full one that shuts it, then the back button with nothing
// left to close. The readout ends "menu shut · ... back 1".
import { back, longPress, wait } from "../gesture.mjs";

export default async function () {
  await longPress("tile");
  await wait(150);
  await back({ predictive: true, duration: 400, to: 0.6, cancel: true });
  await wait(250);
  await back({ predictive: true, duration: 300 });
  await wait(250);
  await back();
  await wait(150);
}
