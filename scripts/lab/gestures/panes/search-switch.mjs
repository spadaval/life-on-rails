// search, unfolded, as the query changes: Play Store chosen for "st"; erased and typed again as "wha", it no
// longer matches, and both panes show WhatsApp. Run with --state demo --screen inner.
import { anchors, expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await tap("dock:search");
  await wait(500);
  await type("st");
  await tap("app-more:Play Store");
  await expect("Play Store chosen", (a, s) => shown(a, "app-pane:Play Store", s));
  // Erased with the drawn keyboard's ⌫ (third row, right end), which keeps the choice, then typed again.
  await tap("search-field");
  await wait(300);
  const k = (await anchors())["keyboard"];
  for (let i = 0; i < 2; i++) { await tap([k[2] - 30, k[1] + 134]); await wait(150); }
  await type("wha");
  await wait(300);
  await expect("both panes on WhatsApp", (a, s) => shown(a, "search:WhatsApp", s) && "app-pane:WhatsApp" in a && !("app-pane:Play Store" in a));
}
