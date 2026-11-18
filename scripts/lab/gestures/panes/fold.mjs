// fold (interaction 8): the thread open in its sheet on the cover, scrolled back, with a reply half
// written; unfolding shows it in the right pane beside the list with the same draft and scroll;
// folding again keeps them. Run with --state thread-long.
import { anchors, expect, fold, swipe, tap, type, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("the thread in its sheet", (a, s) => shown(a, "sheet:agent", s) && shown(a, "thread", s));
  await swipe("down", 260, { duration: 400, from: [240, 900], hold: 150 });
  await wait(1200);
  const s = { width: 480, height: 1120 };
  const at = first(await anchors(), "thread-at:", s);
  if (!at || at === "thread-at:0") throw new Error(`the thread didn't scroll (${at})`);
  await tap("reply-field");
  await expect("the keyboard up", (a, sc) => shown(a, "keyboard", sc));
  await type("half-written");
  await expect("a draft of 12", (a, sc) => shown(a, "draft:12", sc));
  await fold("inner");
  await wait(800);
  await expect(`unfolded: the thread beside the list, the draft and ${at} kept`, (a, sc) => shown(a, "draft:12", sc) && at in a && shown(a, "pane:detail", sc) && shown(a, "agent:rows", sc));
  await fold("cover");
  await wait(800);
  await expect(`folded: still the draft and ${at}`, (a, sc) => "draft:12" in a && at in a && shown(a, "sheet:agent", sc));
}
