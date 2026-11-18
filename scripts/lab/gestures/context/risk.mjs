// risk: things linked to no context. Loose ends holds the tasks under nothing, each with the
// agent's suggested context; Link moves one there, with Undo. Run with --state demo.
import { expect, swipe, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:now");
  await wait(700);
  await tap("bar:all");
  await wait(500);
  // Loose ends is the last group of the sheet: scroll to it.
  await swipe("up", 700, { duration: 500, from: [240, 1000], hold: 300 });
  await wait(1500);
  await tap("context:Loose ends");
  await wait(700);
  await expect("Loose ends, with the agent's suggestions", (a, s) => shown(a, "ctx:Loose ends", s) && shown(a, "suggest:Return the library books", s));
  await tap("link:Return the library books");
  await expect("linked, with Undo; gone from Loose ends", (a, s) => shown(a, "toast:undo", s) && !("suggest:Return the library books" in a), 2500);
}
