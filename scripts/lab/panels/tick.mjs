// tick: in the LoR panel's Due soon, the first task's tick marks it done (struck through).
// node scripts/lab/run.mjs scripts/lab/panels/tick.mjs --version current --state '{"base":"demo","where":{"cell":[3,0]}}' --out <dir>
import { anchors, swipe, wait } from "../gesture.mjs";
import * as g from "../gesture.mjs";
import { onScreen } from "./visible.mjs";

export const link = { screen: "cover" };

/** The first tick in Due soon: the tick anchors in order of their place on screen. */
async function firstTick() {
  const all = Object.entries(await anchors()).filter(([n]) => n.startsWith("tick:"));
  all.sort((a, b) => a[1][1] - b[1][1]);
  return all[0]?.[0];
}

export default async function () {
  // Due soon is under Today, Needs you and the agent's queue: scroll until a tick shows.
  for (let i = 0; i < 6; i++) {
    const t = await firstTick();
    if (t && (await onScreen(t))) break;
    await swipe("up", 300);
    await wait(300);
  }
  const t = await firstTick();
  if (!t) throw new Error("tick: no task in Due soon");
  await g.tap(t);
  await wait(400);
}
