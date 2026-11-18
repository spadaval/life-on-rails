// sync-now (interaction 11, from Now): the failed sync is Now's first note, and its Fix opens the same place. Run with --state sync-failed.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:now");
  await wait(700);
  await expect("the note at the top of Now", (a, s) => shown(a, "note:sync", s));
  await tap("note:sync:action");
  await wait(500);
  await expect("settings at the sync", (a, s) => shown(a, "sync:failed", s));
}
