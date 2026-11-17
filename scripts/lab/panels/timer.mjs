// timer: Start opens its menu, Untitled starts the timer (Today says "Timer: Untitled"), Stop stops it.
// node scripts/lab/run.mjs scripts/lab/panels/timer.mjs --version current --state agent-waiting --out <dir>
import { anchors, tap, wait } from "../gesture.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("timer-start");
  await wait(400);
  await tap("timer-untitled");
  await wait(600);
  if (!("timer-stop" in (await anchors()))) throw new Error("timer: no Stop after starting");
  await tap("timer-stop");
  await wait(600);
  if (!("timer-start" in (await anchors()))) throw new Error("timer: no Start after stopping");
}
