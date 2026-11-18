// sheet (the risk): on Today a task opens in a sheet at half height with the list above it live;
// another row opens into the same sheet; the chevron takes it to full and the list steps back; a tap
// on the list brings it to half; Back closes it, and a second Back does not leave the page. Run with
// --state demo.
import { anchors, back, expect, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("nav:Today");
  await wait(700);
  const s = { width: 480, height: 1120 };
  const rows = Object.keys(await anchors()).filter((k) => k.startsWith("row:task:"));
  await tap(rows[0]);
  await wait(500);
  await expect("half sheet, the list live above", (a, sc) => shown(a, "sheet:today:half", sc) && shown(a, "due:Today", sc));
  await tap(first(await anchors(), "row:flag:", s));
  await wait(500);
  await expect("a flag opened into the same sheet", (a, sc) => shown(a, "sheet:today:half", sc) && shown(a, "detail:seen", sc));
  await tap("sheet:today:expand");
  await wait(500);
  await expect("full, the list behind", (a, sc) => shown(a, "sheet:today:full", sc) && shown(a, "sheet:today:scrim", sc));
  await tap([240, 60]);
  await wait(500);
  await expect("back to half", (a, sc) => shown(a, "sheet:today:half", sc));
  await back();
  await wait(500);
  await expect("Back closed the sheet, still on Today", (a, sc) => !shown(a, "sheet:today", sc) && shown(a, "today:rows", sc));
}
