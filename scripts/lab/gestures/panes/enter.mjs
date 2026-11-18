// enter: search typed on the drawn keyboard's own Return: "stra", then Return opens Strava and
// closes the drawer. Run with --state demo.
import { anchors, expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("dock:search");
  await wait(500);
  await type("stra");
  await expect("Strava shown", (a, s) => shown(a, "search:Strava", s) && shown(a, "keyboard", s));
  const k = (await anchors())["keyboard"];
  await tap([k[2] - 30, k[3] - 30]);
  await expect("Return opened it and closed the drawer", (a, s) => shown(a, "toast", s) && !shown(a, "search-field", s), 2500);
}
