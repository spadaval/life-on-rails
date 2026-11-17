// search (interaction 2): the field at the thumb opens search with the keyboard up; typing finds
// Spotify, and a tap opens it (the lab says so) and closes the drawer. Run with --state demo.
import { expect, tap, type, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:search");
  await wait(500);
  await expect("search with the keyboard up", (a, s) => shown(a, "search-field", s) && shown(a, "keyboard", s));
  await type("spo");
  await expect("Spotify found", (a, s) => shown(a, "search:Spotify", s) && shown(a, "understood:ask", s));
  await tap("search:Spotify");
  await wait(600);
  await expect("home again, saying it opens Spotify", (a, s) => shown(a, "home:Phone", s) && shown(a, "toast", s));
}
