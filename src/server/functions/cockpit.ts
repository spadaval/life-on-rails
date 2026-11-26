import { createServerFn } from "@tanstack/react-start";
import { sql } from "../../db/client.js";
import { getStatus } from "../status.js";
import { todayMeasures } from "../today.js";

export const getCaptureStatus = createServerFn({ method: "GET" }).handler(async () => {
  const status = await getStatus();
  // Without the database there is nothing to measure; the page says the database is down.
  const today = status.db ? await todayMeasures(sql) : { confirm_rate: null, coverage: null };
  return { ...status, today };
});
