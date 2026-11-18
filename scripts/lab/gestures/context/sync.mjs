// sync (interaction 11): a failed sync shows on home's card; its Fix opens settings at the sync with
// what it needs; allowing them and syncing again succeeds. Run with --state sync-failed.
import { anchors, expect, tap, wait } from "../../gesture.mjs";
import { first, shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("the failure on home's card", (a, s) => shown(a, "home:sync", s));
  await tap("home:sync");
  await wait(500);
  await expect("settings at the sync, failed", (a, s) => shown(a, "sync:failed", s) && shown(a, "settings:back", s));
  const s = { width: 480, height: 1120 };
  for (let i = 0; i < 4; i++) {
    const allow = first(await anchors(), "allow:", s);
    if (!allow) break;
    await tap(allow);
    await wait(250);
  }
  await tap("sync:now");
  await expect("synced", (a, sc) => shown(a, "sync:ok", sc));
}
