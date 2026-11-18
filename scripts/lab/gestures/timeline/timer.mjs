// timer (interaction 5): Start on home's strip opens the timer; a label starts it; Stop on the
// strip stops it, with Undo, and its time stays on the route as a block. Then on the day page, a
// recent label under the now line starts one that grows from the line; Switch ends it as a block
// and starts another; Stop on the route ends that one too. Run with --state demo.
import { anchors, expect, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:start");
  await expect("the timer sheet", (a, s) => shown(a, "sheet:timer", s) && shown(a, "label:Offline maps spec", s));
  await tap("label:Offline maps spec");
  await wait(500);
  await expect("running, with Stop on the strip", (a, s) => shown(a, "home:stop", s) && shown(a, "home:readout", s));
  await wait(1500);
  await tap("home:stop");
  await expect("stopped, with Undo", (a, s) => shown(a, "home:start", s) && shown(a, "toast:undo", s));
  await wait(4500);
  await swipe("right", 300, { duration: 260, from: [80, 640] });
  await wait(700);
  await expect("the day page: the stopped timer's block, and Start under now", (a, s) => shown(a, "block:Offline maps spec", s) && shown(a, "route:start:Morning run", s));
  await tap("route:start:Morning run");
  await expect("a block growing from the now line", (a, s) => shown(a, "route:running", s) && shown(a, "route:stop", s));
  await wait(1500);
  await tap("route:switch");
  await expect("the running timer's sheet, at rest", (a, s) => shown(a, "sheet:timer", s) && shown(a, "timer:switch", s));
  await wait(500);
  await tap("timer:switch");
  await expect("labels to switch to", (a, s) => shown(a, "label:Inbox zero", s));
  await tap("label:Inbox zero");
  await wait(500);
  await expect("switched: Morning run kept as a block, Inbox zero running", (a, s) => shown(a, "block:Morning run", s) && shown(a, "route:running", s));
  await wait(1500);
  await tap("route:stop");
  await expect("stopped: Inbox zero kept as a block too, with Undo", (a, s) => shown(a, "route:start", s) && shown(a, "block:Inbox zero", s) && shown(a, "block:Morning run", s) && shown(a, "toast:undo", s));
  // The two blocks keep their true ends: a few seconds each, Inbox zero starting where Morning run
  // ended, neither past the moment it stopped (review 2-wum1.6).
  const span = (a, title) => { const k = Object.keys(a).find((n) => n.startsWith(`span:${title}:`)); const [s, e] = k.slice(`span:${title}:`.length).split("-").map(Number); return { s, e }; };
  const a = await anchors();
  const run = span(a, "Morning run"), inbox = span(a, "Inbox zero");
  if (!(run.e - run.s < 30 && inbox.e - inbox.s < 30)) throw new Error(`blocks should be seconds long: Morning run ${run.e - run.s} s, Inbox zero ${inbox.e - inbox.s} s`);
  if (Math.abs(inbox.s - run.e) > 1) throw new Error(`Inbox zero should start where Morning run ended: ${run.e} then ${inbox.s}`);
  await tap("toast:undo");
  await expect("Undo: Inbox zero running again, its block gone", (a, s) => shown(a, "route:running", s) && !("block:Inbox zero" in a));
}
