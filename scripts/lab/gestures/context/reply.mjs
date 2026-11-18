// reply (interaction 9): on the inner screen the thread is read beside the context, and a reply
// goes from its foot. Run with --state thread-long.
import { anchors, expect, tap, type } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "inner" };

export default async function () {
  await expect("the thread beside the context", (a, s) => shown(a, "thread", s) && shown(a, "now:rows", s));
  const n = Number(Object.keys(await anchors()).find((k) => k.startsWith("messages:"))?.slice(9));
  await tap("reply-field");
  await type("Run it, then tell me the timings");
  await tap("reply:send");
  await expect(`the reply in the thread (${n + 1} messages)`, (a) => `messages:${n + 1}` in a && "draft:0" in a);
}
