// timer-clock (review 1-j6j3.2): a timer started a while after the state opened starts at zero, and
// home's line and the timer sheet show the same running time. Run with --state demo.
import { anchors, expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

const secs = (a, prefix) => Number(Object.keys(a).find((k) => k.startsWith(prefix))?.slice(prefix.length));

export default async function () {
  await wait(6000);
  await tap("home:start");
  await expect("the timer sheet", (a, s) => shown(a, "label:Inbox zero", s));
  await tap("label:Inbox zero");
  await wait(2500);
  const home = secs(await anchors(), "home:at:");
  if (!(home >= 1 && home <= 4)) throw new Error(`home's line should read 1 to 4 s, not ${home}`);
  await tap("home:timer");
  await expect("the running timer's sheet", (a, s) => shown(a, "timer:readout", s));
  const a = await anchors();
  const sheet = secs(a, "timer:at:");
  if (Math.abs(sheet - secs(a, "home:at:")) > 1 || !(sheet >= home)) throw new Error(`the sheet reads ${sheet} s, home's line ${secs(a, "home:at:")} s`);
}
