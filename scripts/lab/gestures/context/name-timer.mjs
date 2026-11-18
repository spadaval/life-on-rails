// name-timer (the risk: a running timer with no label): Now says the time has no name yet and offers
// where it could count, the agent's suggestion first; a tap links it without stopping it, and the
// surface turns to that context, still timing, with Undo. Run with
// --state '{"base":"demo","timer":{"state":"RUNNING","start":"2026-11-17T19:40:00Z"},"where":{"cell":[0,0]}}'.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("an unnamed timer, with where it could go", (a, s) => shown(a, "ctx:An unnamed timer", s) && shown(a, "link-to:Offline maps", s) && shown(a, "ctx:readout", s));
  await tap("link-to:Offline maps");
  await wait(700);
  await expect("Offline maps, still timing, with Undo", (a, s) => shown(a, "ctx:Offline maps", s) && shown(a, "ctx:readout", s) && shown(a, "toast:undo", s));
}
