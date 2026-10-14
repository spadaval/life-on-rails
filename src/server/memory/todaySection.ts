// What Today adds to the agent's digest: the plan, the timer, and what is unconfirmed.
import { localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { getToday, renderToday } from "../today.js";
import { registerContextSection } from "./context.js";

registerContextSection(async (db, now) => {
  const today = await getToday(db, localYmd(env.LOR_TZ, now), now);
  // Drop the "# date" heading; the digest has its own.
  const markdown = renderToday(today).split("\n").slice(2).join("\n").replace(/^## /gm, "### ");
  return { title: "Today", markdown };
});
