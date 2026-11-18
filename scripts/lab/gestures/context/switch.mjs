// switch (the signature move, and interaction 5's Switch): on Now, the bar turns the surface to
// another context; a running timer is switched there from its header, which makes it the current
// context again. Run with --state demo.
import { expect, tap, wait } from "../../gesture.mjs";
import { shown } from "./shown.mjs";

export const link = { screen: "cover" };

export default async function () {
  await tap("home:now");
  await wait(700);
  await expect("Now on Offline maps", (a, s) => shown(a, "ctx:Offline maps", s) && shown(a, "bar:now", s));
  await tap("ctx:start");
  await wait(400);
  await expect("timing here", (a, s) => shown(a, "ctx:readout", s));
  await tap("bar:all");
  await wait(500);
  await tap("context:Public beta");
  await wait(700);
  await expect("the surface turned to Public beta, chosen, and a tab for it in the bar", (a, s) => shown(a, "ctx:Public beta", s) && shown(a, "ctx:why:chosen", s) && shown(a, "ctx:switch-here", s) && "bar:Public beta" in a);
  await tap("ctx:switch-here");
  await wait(700);
  await expect("timing Public beta, now the current context", (a, s) => shown(a, "ctx:Public beta", s) && shown(a, "ctx:readout", s) && shown(a, "ctx:why:timer", s));
}
