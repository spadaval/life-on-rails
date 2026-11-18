// agent flags: the agent page lists LoR's flags after T3's cards, the same ones Today has; Done on
// one there clears it from both pages, with Undo. Run with --state demo.
import { anchors, expect, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("nav:Agent");
  await wait(700);
  const s = { width: 480, height: 1120 };
  const done = first(await anchors(), "done:flag:", s);
  await expect("a flag after the approval", (a, sc) => !!done && shown(a, "approve:r-approve", sc) && a[done][1] > a["approve:r-approve"][1]);
  await tap(done);
  await expect("Done, with Undo", (a, sc) => shown(a, "toast:undo", sc) && !(done in a));
  await tap("nav:Today");
  await wait(700);
  await expect("gone from Today too", (a) => !(done in a));
}
