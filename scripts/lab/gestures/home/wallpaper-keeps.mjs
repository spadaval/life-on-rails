// wallpaper-keeps: Spotify dropped on Strava makes a folder; a new wallpaper then changes home's
// ink and nothing else: the folder and its Undo stay (lor-xdoi's review, 1-cd0k.3).
import { drag, expect, wait, wallpaper } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

const made = (a, s) => Object.keys(a).some((n) => n.startsWith("folder:") && n !== "folder:Work" && shown(a, n, s));

export default async function () {
  await drag(["home:Spotify", { anchor: "home:Spotify", by: [60, 0] }, "home:Strava"], [700, 0, 800], { speed: 500 });
  await wait(900);
  await expect("the new folder", made);
  await wallpaper("pale");
  await wait(600);
  await expect("the new folder after the wallpaper changed", made);
}
