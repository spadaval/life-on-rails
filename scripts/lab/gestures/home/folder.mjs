// folder: Spotify picked up by a long press and held over Strava makes a folder of the two when let
// go (Kvaesitso's wait before a target takes, 300 ms, then the drop). Ends with the new folder on
// home and the Undo bar saying so; neither app is on home alone any more.
import { drag, expect, wait } from "../../gesture.mjs";
import { anyShown, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await drag(["home:Spotify", { anchor: "home:Spotify", by: [60, 0] }, "home:Strava"], [700, 0, 800], { speed: 500 });
  await wait(900);
  await expect("a folder of Spotify and Strava", (a, s) =>
    !shown(a, "home:Spotify", s) && !shown(a, "home:Strava", s) && Object.keys(a).some((n) => n.startsWith("folder:") && n !== "folder:Work" && shown(a, n, s)));
  await expect("the Undo bar", (a, s) => anyShown(a, "undo-bar", s));
}
