// The job list. Each build step adds its jobs here.
import { sql } from "../../db/client.js";
import { applyAutoRules, fillWindow } from "../attribution/blocks.js";
import { channelConfigured, lastChannelPoll, pendingFlags, sendFlags } from "../channel/channel.js";
import { whatsappChannel } from "../channel/client.js";
import { lockBlocks, syncFill } from "../attribution/fill.js";
import { env } from "../env.js";
import { syncCalendars } from "../google/calendarSync.js";
import { SCOPES, hasScope } from "../google/auth.js";
import { googleCalendar, googleTasks } from "../google/client.js";
import { googleContacts } from "../google/contactsClient.js";
import { syncContacts } from "../google/contactsSync.js";
import { googleMail } from "../google/mailClient.js";
import { syncTasks } from "../google/tasksSync.js";
import { wakeSnoozed } from "../intents.js";
import { mailCheck } from "../mail/check.js";
import { plaid, plaidConfigured } from "../money/plaid.js";
import { syncMoney } from "../money/sync.js";
import { recomputeScores, scheduleRecompute, setConfirmRateProvider } from "../scoring/recompute.js";
import { trailingConfirmRate } from "../today.js";

setConfirmRateProvider(trailingConfirmRate);
import { importWhatsapp, whatsappPending } from "../whatsapp/import.js";
import { overnightClose } from "./overnight.js";
import type { Job } from "./scheduler.js";

const startedAt = Date.now();

/** Mail needs the Gmail-headers scope on LoR's login (`pnpm google:auth --mail`). */
export function mailConfigured(): boolean {
  return hasScope(SCOPES.gmail);
}

/** The channel long-polls on its own; these report on it and send what the agent flags. */
const CHANNEL_JOBS: Job[] = [
  {
    name: "channel",
    everyS: 300,
    run: async () => {
      const age = Date.now() - lastChannelPoll();
      if (age > 120_000) throw new Error("no answer from WhatsApp for over two minutes");
      return { last_poll_s: Math.round(age / 1000) };
    },
  },
  {
    name: "channel_flags",
    everyS: 30,
    shouldRun: async () => (await pendingFlags(sql)).length > 0,
    run: async () => sendFlags(sql, whatsappChannel),
  },
];

export const JOBS: Job[] = [
  ...(channelConfigured() ? CHANNEL_JOBS : []),
  {
    name: "heartbeat",
    everyS: 300,
    run: async () => ({ uptime_s: Math.round((Date.now() - startedAt) / 1000) }),
  },
  {
    name: "calendar_poll",
    everyS: env.LOR_CALENDAR_POLL_S,
    run: async () => {
      const stats = await syncCalendars(sql, googleCalendar);
      const { from, to } = fillWindow(new Date());
      const fill = await sql.begin(async (tx) => {
        await lockBlocks(tx);
        const f = await syncFill(tx, from, to);
        return { ...f, auto_committed: await applyAutoRules(tx, from, to) };
      });
      if (fill.auto_committed > 0) scheduleRecompute();
      return { ...stats, fill };
    },
  },
  {
    name: "tasks_poll",
    everyS: env.LOR_TASKS_POLL_S,
    run: async () => {
      const { touched, ...stats } = await syncTasks(sql, googleTasks);
      if (touched.length > 0) scheduleRecompute();
      return stats;
    },
  },
  {
    name: "contacts_poll",
    everyS: env.LOR_CONTACTS_POLL_S,
    run: async () => syncContacts(sql, googleContacts),
  },
  {
    name: "mail_check",
    everyS: env.LOR_MAIL_POLL_S,
    shouldRun: async () => mailConfigured(),
    run: async () => mailCheck(sql, googleMail),
  },
  {
    name: "whatsapp_import",
    everyS: 300,
    shouldRun: whatsappPending,
    run: async () => importWhatsapp(sql),
  },
  {
    name: "money_sync",
    everyS: env.LOR_PLAID_POLL_S,
    shouldRun: async () => plaidConfigured(),
    run: async () => syncMoney(sql, plaid),
  },
  {
    name: "overnight_close",
    dailyAt: "01:30",
    run: async () => overnightClose(sql),
  },
  {
    name: "score_tick",
    everyS: 3600,
    run: async () => ({ woken: await wakeSnoozed(sql), ...(await recomputeScores(sql)) }),
  },
];

/**
 * A job is stale when its last successful run is older than this. Jobs that
 * only run when there is something to do are never stale.
 */
export function staleAfterMs(job: Pick<Job, "everyS" | "dailyAt" | "shouldRun">): number {
  if (job.shouldRun || (!job.everyS && !job.dailyAt)) return Infinity;
  if (job.everyS) return job.everyS * 3 * 1000;
  return 26 * 3600_000;
}
