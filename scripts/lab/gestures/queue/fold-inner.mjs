// fold-inner (interaction 8, from the inner screen): the thread open beside the queue, scrolled
// back, a reply half written; folding shows it in a sheet with the same draft and scroll, and
// unfolding brings the pane back. Then Approve clears the card, and no empty sheet is left over.
// Run with --state thread-long --screen inner.
import { anchors, expect, fold, swipe, tap, type, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await expect("the thread beside the list", (a, s) => shown(a, "thread", s) && shown(a, "queue:count", s));
  const t = (await anchors())["thread"];
  await swipe("down", 300, { duration: 400, from: [Math.round((t[0] + t[2]) / 2), Math.round(t[1] + 120)], hold: 150 });
  await wait(1200);
  const at = first(await anchors(), "thread-at:", { width: 875, height: 971 });
  if (!at || at === "thread-at:0") throw new Error(`the thread didn't scroll (${at})`);
  await tap("reply-field");
  await type("half-written");
  await expect("a draft of 12", (a, s) => shown(a, "draft:12", s));
  await fold("cover");
  await wait(900);
  await expect(`folded: the thread in a sheet, the draft and ${at} kept`, (a, s) => shown(a, "sheet:item", s) && shown(a, "thread", s) && "draft:12" in a && at in a);
  await fold("inner");
  await wait(900);
  await expect("unfolded: the pane again, no sheet", (a, s) => shown(a, "thread", s) && !shown(a, "sheet:item", s) && "draft:12" in a);
  await tap("approve:r-approve");
  await wait(3600);
  await expect("approved: the card gone, no sheet over the pane", (a, s) => !("approve:r-approve" in a) && !shown(a, "sheet:item", s) && shown(a, "detail", s), 3000);
}
