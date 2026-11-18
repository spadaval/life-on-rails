// quick send (interaction 10, success): a double tap on home opens it; words go to the chosen
// project, and it says they went. Run with --state demo.
import { doubleTap, expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await doubleTap([240, 520]);
  await wait(500);
  await expect("quick send", (a, s) => shown(a, "sheet:quick", s) && shown(a, "quick:send", s));
  await tap("quick:field");
  await type("Check the tile cache numbers");
  await tap("quick:send");
  await expect("sent", (a, s) => shown(a, "quick:sent", s));
}
