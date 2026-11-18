// search (interaction 2): the dock's field opens search with the keyboard up; typed letters bring
// results up from the field, the first app nearest the thumb; a tap opens it. Run with --state demo.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("dock:search");
  await wait(500);
  await expect("search open, the keyboard up", (a, s) => shown(a, "search-field", s) && shown(a, "keyboard", s));
  await type("stra");
  await expect("Strava among the results, above the field", (a, s) => shown(a, "search:Strava", s) && a["search:Strava"][3] <= a["search-field"][1] + 4);
  await tap("search:Strava");
  await expect("it opens (the lab says so), and the drawer closes", (a, s) => shown(a, "toast", s) && !shown(a, "search-field", s), 2500);
}
