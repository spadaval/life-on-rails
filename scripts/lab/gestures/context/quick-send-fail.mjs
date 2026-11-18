// quick-send-fail (interaction 10, failure): with T3 away, the agent button at the thumb opens quick
// send; Send fails and says why, keeping the words. Run with --state agent-t3-stale.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("dock:ask");
  await wait(500);
  await tap("quick:field");
  await type("Check the tile cache numbers");
  await tap("quick:send");
  await expect("not sent, the words kept, Try again", (a, s) => shown(a, "quick:failed", s) && shown(a, "quick:failed:action", s));
}
