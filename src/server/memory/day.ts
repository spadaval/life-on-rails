// Date arguments for `lor day` and `lor confirm`.
import { type Ymd, addDays, isYmd, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { BadInput } from "../errors.js";

/** "today" (the default), "yesterday", or a YYYY-MM-DD local date. */
export function resolveDate(input: string | undefined | null, now = new Date()): Ymd {
  const today = localYmd(env.LOR_TZ, now);
  if (!input || input === "today") return today;
  if (input === "yesterday") return addDays(today, -1);
  if (!isYmd(input)) throw new BadInput(`bad date ${JSON.stringify(input)}: use YYYY-MM-DD, today, or yesterday`);
  return input;
}
