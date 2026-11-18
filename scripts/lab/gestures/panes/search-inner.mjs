// search, unfolded: results on the left and the chosen app's pane on the right, with its recent
// conversations; one is opened. Run with --state demo --screen inner.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await tap("dock:search");
  await wait(500);
  await type("wha");
  await expect("WhatsApp chosen, its pane beside the results", (a, s) => shown(a, "search:WhatsApp", s) && shown(a, "app-pane:WhatsApp", s) && shown(a, "app-pane:Maya Okafor", s));
  await tap("app-pane:Maya Okafor");
  await expect("the conversation opens", (a, s) => shown(a, "toast", s), 2500);
}
