// timer (interaction 5): Start at the thumb opens the one timer; a label starts it, and the dock
// shows it running; Switch moves it to another label; Stop ends it, with Undo. Run with --state demo.
import { expect, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";
import { anchors } from "../../gesture.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("chip:start");
  await wait(400);
  await expect("the timer sheet, labels to start under", (a, s) => shown(a, "sheet:timer", s) && Object.keys(a).some((k) => k.startsWith("label:")));
  const s = { width: 480, height: 1120 };
  const label = first(await anchors(), "label:", s);
  await tap(label);
  await wait(500);
  await expect("running at the thumb", (a, sc) => shown(a, "chip:readout", sc) && shown(a, "chip:stop", sc));
  await tap("chip:switch");
  await wait(400);
  await expect("switching: the labels again", (a, sc) => shown(a, "sheet:timer", sc) && shown(a, "label:none", sc));
  await tap("label:none");
  await wait(500);
  await tap("chip:stop");
  await expect("stopped, with Undo", (a, sc) => shown(a, "chip:start", sc) && shown(a, "toast:undo", sc));
}
