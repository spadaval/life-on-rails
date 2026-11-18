// fold-context (interaction 8): a context chosen on Now and Now scrolled down survive an unfold and a fold. Run with --state demo.
import { anchors, expect, fold, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:now");
  await wait(700);
  await tap("bar:all");
  await wait(500);
  await tap("context:Public beta");
  await wait(700);
  await swipe("up", 500, { duration: 500, from: [240, 900], hold: 150 });
  await wait(800);
  const y = (await anchors())["next"]?.[1];
  await expect("Public beta, scrolled to the next event", (a, s) => "ctx:Public beta" in a && shown(a, "next", s));
  await fold("inner");
  await wait(800);
  await expect("unfolded: still Public beta, the page beside it", (a, s) => shown(a, "ctx:Public beta", s) || ("ctx:Public beta" in a && shown(a, "pane:rest", s)));
  await fold("cover");
  await wait(800);
  await expect("folded: Public beta and the same scroll", (a, s) => "ctx:Public beta" in a && shown(a, "next", s) && Math.abs(a["next"][1] - y) < 40);
}
