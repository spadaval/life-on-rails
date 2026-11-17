// approve: on the agent page, Approve on the migration's card clears it from Needs you.
// node scripts/lab/run.mjs scripts/lab/panels/approve.mjs --version current --state agent-waiting --out <dir>
import { anchors, tap, wait } from "../gesture.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("card:t-approve/approve");
  await wait(400);
  if ("card:t-approve" in (await anchors())) throw new Error("approve: the card is still there");
}
