// The push path: Gmail publishes to a Pub/Sub topic when the inbox changes,
// and this loop pulls LoR's subscription over plain HTTP. The connection is
// outbound, so nothing listens on the internet. A notification carries no
// content; it only says "look now".
import { sql } from "../../db/client.js";
import { env } from "../env.js";
import { type PubSubPort, googlePubSub } from "../google/mailClient.js";
import { runJob } from "../jobs/scheduler.js";
import { errorMessage, log } from "../log.js";
import { notePush } from "./check.js";

const PULL_TIMEOUT_MS = 120_000;
const BACKOFF_MS = 30_000;
let started = false;

export async function pullOnce(port: PubSubPort, subscription: string, onPush: () => Promise<void>): Promise<number> {
  const received = await port.pull(subscription, AbortSignal.timeout(PULL_TIMEOUT_MS));
  if (received.length === 0) return 0;
  // Acknowledge first: the check reads from a stored position, so a notification is never the only record.
  await port.ack(subscription, received.map((m) => m.ackId));
  await onPush();
  return received.length;
}

export function startMailPush(port: PubSubPort = googlePubSub): void {
  if (started) return;
  started = true;
  const subscription = env.GOOGLE_PUBSUB_SUBSCRIPTION;
  let lastError = "";
  const loop = async () => {
    // Pick up whatever arrived while LoR was down.
    void runJob("mail_check");
    for (;;) {
      try {
        await pullOnce(port, subscription, async () => {
          await notePush(sql);
          void runJob("mail_check");
        });
        lastError = "";
      } catch (err) {
        const message = errorMessage(err);
        // A pull that timed out with nothing to deliver is the normal quiet case.
        if (!/timeout|aborted/i.test(message)) {
          if (message !== lastError) log("error", "mail push pull failed", { err: message });
          lastError = message;
          await new Promise((r) => setTimeout(r, BACKOFF_MS));
        }
      }
    }
  };
  void loop();
  log("info", "mail push started", { subscription });
}
