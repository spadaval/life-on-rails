// fold (interaction 8): a thread open in its sheet on the cover, scrolled back, with a reply half
// written; unfolding shows it in the right pane beside the route with the same draft and scroll;
// folding again keeps them, and the route's scroll too. Run with --state thread-long.
import { anchors, expect, fold, swipe, tap, type, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("the thread in its sheet", (a, s) => shown(a, "sheet:item", s) && shown(a, "thread", s));
  await swipe("down", 300, { duration: 400, from: [240, 760], hold: 150 });
  await wait(1200);
  const s = { width: 480, height: 1120 };
  const at = first(await anchors(), "thread-at:", s);
  if (!at || at === "thread-at:0") throw new Error(`the thread didn't scroll (${at})`);
  const route = Object.keys(await anchors()).find((k) => k.startsWith("route-at:"));
  console.log(`before the fold: ${at}, ${route}`);
  await tap("reply-field");
  await expect("the keyboard up", (a, sc) => shown(a, "keyboard", sc));
  await type("half-written");
  await expect("a draft of 12", (a, sc) => shown(a, "draft:12", sc));
  await fold("inner");
  await wait(800);
  await expect(`unfolded: the thread beside the route, the draft and ${at} kept`, (a, sc) => shown(a, "draft:12", sc) && at in a && shown(a, "pane:back", sc) && shown(a, "now", sc) && route in a);
  await fold("cover");
  await wait(800);
  await expect(`folded: still the draft, ${at} and ${route}`, (a) => "draft:12" in a && at in a && route in a);
}
