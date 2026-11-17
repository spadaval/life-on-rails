// sync (interaction 11): home's line says the sync failed; Fix opens the sync settings, the
// missing permissions are allowed, and Sync now succeeds. Run with --state sync-failed.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await expect("the failure on home", (a, s) => shown(a, "home:sync", s));
  await tap("home:sync");
  await wait(400);
  await expect("sync settings, failed, two to allow", (a, s) => shown(a, "sync:failed", s) && shown(a, "allow:calls", s) && shown(a, "allow:location", s));
  await tap("allow:calls");
  await expect("calls allowed", (a, s) => !shown(a, "allow:calls", s) && shown(a, "allow:location", s));
  await tap("allow:location");
  await expect("location allowed", (a, s) => !shown(a, "allow:location", s));
  await tap("sync:now");
  await expect("synced", (a, s) => shown(a, "sync:ok", s) && !shown(a, "allow:calls", s));
}
