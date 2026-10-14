import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { type GMessage, HistoryExpiredError, type MailPort } from "../../src/server/google/mailClient.js";
import { mailCheck, renewWatch } from "../../src/server/mail/check.js";
import { flushWake, wakeDue } from "../../src/server/mail/wake.js";
import { showEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { dbDescribe } from "./helpers.js";

function message(id: string, opts: { thread?: string; from?: string; subject?: string; labels?: string[]; headers?: Record<string, string> } = {}): GMessage {
  const headers = { From: opts.from ?? "Jo Bloggs <jo@example.com>", Subject: opts.subject ?? `Subject ${id}`, ...(opts.headers ?? {}) };
  return {
    id,
    threadId: opts.thread ?? `thread-${id}`,
    labelIds: opts.labels ?? ["INBOX", "CATEGORY_PERSONAL"],
    internalDate: String(Date.parse("2026-10-14T17:00:00Z")),
    payload: { headers: Object.entries(headers).map(([name, value]) => ({ name, value })) },
  };
}

/** A mailbox: messages by id, and which ids were added at each history step. */
function mailbox() {
  const messages = new Map<string, GMessage>();
  const history: { id: number; added: string[] }[] = [];
  let historyId = 100;
  let expired = false;
  const calls = { metadata: 0, watch: 0 };
  const port: MailPort = {
    currentHistoryId: async () => String(historyId),
    addedSince: async (start) => {
      if (expired) throw new HistoryExpiredError();
      return { ids: history.filter((h) => h.id > Number(start)).flatMap((h) => h.added), historyId: String(historyId) };
    },
    recentMessages: async (max) => [...messages.keys()].reverse().slice(0, max),
    metadata: async (id) => {
      calls.metadata++;
      return messages.get(id) ?? null;
    },
    watch: async () => {
      calls.watch++;
      return { historyId: String(historyId), expiration: new Date("2026-10-21T00:00:00Z") };
    },
  };
  return {
    port,
    calls,
    arrive(...msgs: GMessage[]) {
      historyId++;
      for (const m of msgs) messages.set(m.id, m);
      history.push({ id: historyId, added: msgs.map((m) => m.id) });
    },
    expire() {
      expired = true;
    },
  };
}

const arrivals = () => sql<{ title: string; bulk: boolean; from_email: string; gmail_id: string }[]>`
  SELECT title, (payload->>'bulk')::boolean AS bulk, payload->>'from_email' AS from_email, payload->>'gmail_id' AS gmail_id
  FROM observation WHERE source = 'gmail' ORDER BY payload->>'gmail_id'`;
const queue = () => sql<{ thread: string; n: number; priority: boolean }[]>`
  SELECT gmail_thread_id AS thread, cardinality(observation_ids) AS n, priority FROM wake_queue ORDER BY gmail_thread_id`;

dbDescribe("mail check", () => {
  it("starts from now with no backfill, then records one observation per new inbox message", async () => {
    const box = mailbox();
    box.arrive(message("old"));
    expect(await mailCheck(sql, box.port)).toMatchObject({ seen: 0, recorded: 0, note: "started watching from now" });
    expect(await arrivals()).toEqual([]);

    box.arrive(message("m1", { from: '"Priya Raman" <priya@example.com>', subject: "Lunch on Friday?" }));
    expect(await mailCheck(sql, box.port)).toMatchObject({ seen: 1, recorded: 1, bulk: 0, queued: 1 });
    expect(await arrivals()).toEqual([{ title: "Lunch on Friday?", bulk: false, from_email: "priya@example.com", gmail_id: "m1" }]);
    const [o] = await sql<{ keys: string[] }[]>`SELECT array(SELECT jsonb_object_keys(payload) ORDER BY 1) AS keys FROM observation WHERE source = 'gmail'`;
    expect(o!.keys).toEqual(["bulk", "direction", "entity_id", "from", "from_email", "gmail_id", "labels", "thread_id"]); // no body, ever

    // Nothing new: nothing recorded, and no message is fetched again.
    const before = box.calls.metadata;
    expect(await mailCheck(sql, box.port)).toMatchObject({ seen: 0, recorded: 0 });
    expect(box.calls.metadata).toBe(before);
  });

  it("records bulk mail but never queues it; a known sender is not bulk; sent mail is recorded without a wake; drafts are skipped", async () => {
    const box = mailbox();
    await mailCheck(sql, box.port);
    await upsertEntity(sql, { kind: "org", name: "Acme", identifiers: [{ kind: "domain", value: "acme.com" }] });
    box.arrive(
      message("news", { from: "Deals <deals@shop.example>", headers: { "List-Unsubscribe": "<mailto:u@shop.example>" } }),
      message("promo", { labels: ["INBOX", "CATEGORY_PROMOTIONS"] }),
      message("acme", { from: "Acme Billing <billing@acme.com>", headers: { "List-Unsubscribe": "<mailto:u@acme.com>" } }),
      message("sent", { labels: ["SENT"], from: "Sumeet <owner@example.com>", headers: { To: '"Priya" <Priya@example.com>, jo@example.com', Cc: "owner@example.com" } }),
      message("draft", { labels: ["DRAFT"] }),
      message("archived", { labels: ["CATEGORY_PERSONAL"] }),
    );
    expect(await mailCheck(sql, box.port)).toMatchObject({ seen: 6, recorded: 4, sent: 1, bulk: 2, queued: 1, skipped: 2 });
    expect((await arrivals()).map((a) => [a.gmail_id, a.bulk])).toEqual([["acme", false], ["news", true], ["promo", true], ["sent", null]]);
    expect(await queue()).toEqual([{ thread: "thread-acme", n: 1, priority: false }]);
    expect((await showEntity(sql, "acme.com")).recent_mail.map((m) => m.gmail_id)).toEqual(["acme"]);
    const [sent] = await sql<{ direction: string; to_emails: string[] }[]>`
      SELECT payload->>'direction' AS direction, array(SELECT jsonb_array_elements_text(payload->'to_emails')) AS to_emails
      FROM observation WHERE payload->>'gmail_id' = 'sent'`;
    expect(sent).toEqual({ direction: "sent", to_emails: ["priya@example.com", "jo@example.com"] });
  });

  it("coalesces a thread into one queue row, and a priority sender marks it", async () => {
    const box = mailbox();
    await mailCheck(sql, box.port);
    const boss = await upsertEntity(sql, { kind: "person", name: "Boss", identifiers: [{ kind: "email", value: "boss@work.example" }] });
    await sql`UPDATE entity SET priority = true WHERE id = ${boss.id}`;
    box.arrive(message("a1", { thread: "T" }), message("a2", { thread: "T" }));
    await mailCheck(sql, box.port);
    box.arrive(message("a3", { thread: "T", from: "Boss <boss@work.example>" }), message("b1", { thread: "U" }));
    await mailCheck(sql, box.port);
    expect(await queue()).toEqual([{ thread: "T", n: 3, priority: true }, { thread: "U", n: 1, priority: false }]);
  });

  it("when Gmail has dropped the history, catches up from recent messages it has no record of", async () => {
    const box = mailbox();
    await mailCheck(sql, box.port);
    box.arrive(message("seen"));
    await mailCheck(sql, box.port);
    box.arrive(message("missed-1"), message("missed-2"));
    box.expire();
    const stats = await mailCheck(sql, box.port);
    expect(stats).toMatchObject({ recorded: 2, note: "history expired; caught up from recent messages" });
    expect((await arrivals()).map((a) => a.gmail_id)).toEqual(["missed-1", "missed-2", "seen"]);
  });

  it("renewing the watch records its expiry and runs a check", async () => {
    const box = mailbox();
    const stats = await renewWatch(sql, box.port, "projects/p/topics/t");
    expect(stats.expires).toBe("2026-10-21T00:00:00.000Z");
    expect(box.calls.watch).toBe(1);
    const [s] = await sql<{ history_id: string; expires: Date; checked: boolean }[]>`
      SELECT history_id, watch_expires_at AS expires, last_check_at IS NOT NULL AS checked FROM mail_state`;
    expect(s).toMatchObject({ history_id: "100", checked: true });
  });
});

dbDescribe("the wake", () => {
  const cfg = { url: "http://t3.invalid/api/hooks/h/tok", quietS: 180, maxS: 900 };

  async function queued(box: ReturnType<typeof mailbox>, at: Date, ...msgs: GMessage[]) {
    box.arrive(...msgs);
    await mailCheck(sql, box.port, at);
  }

  it("two messages in one thread a minute apart cause one wake, after the quiet period", async () => {
    const box = mailbox();
    const t0 = new Date("2026-10-14T17:00:00Z");
    await mailCheck(sql, box.port, t0);
    await queued(box, t0, message("a1", { thread: "T", subject: "Quote" }));
    await queued(box, new Date(t0.getTime() + 60_000), message("a2", { thread: "T", subject: "Re: Quote" }));
    expect(await wakeDue(sql, cfg, new Date(t0.getTime() + 120_000))).toBe(false);
    expect(await wakeDue(sql, cfg, new Date(t0.getTime() + 241_000))).toBe(true);

    const posts: { url: string; body: { count: number; text: string; messages: { gmail_id: string }[] } }[] = [];
    const stats = await flushWake(sql, cfg, async (url, body) => {
      posts.push({ url, body: body as never });
      return { ok: true, status: 200 };
    });
    expect(stats).toMatchObject({ threads: 1, messages: 2, posted: true });
    expect(posts).toHaveLength(1);
    expect(posts[0]!.body.messages.map((m) => m.gmail_id)).toEqual(["a1", "a2"]);
    expect(posts[0]!.body.text).toContain("Re: Quote");
    expect(await queue()).toEqual([]);
    expect(await wakeDue(sql, cfg, new Date(t0.getTime() + 999_000))).toBe(false);
  });

  it("a failed POST leaves the queue as it was", async () => {
    const box = mailbox();
    await mailCheck(sql, box.port);
    await queued(box, new Date(), message("a1"));
    await expect(flushWake(sql, cfg, async () => ({ ok: false, status: 429 }))).rejects.toThrow(/429/);
    expect(await queue()).toHaveLength(1);
  });

  it("with no wake URL nothing is due and nothing is posted", async () => {
    const box = mailbox();
    await mailCheck(sql, box.port);
    await queued(box, new Date(Date.now() - 3600_000), message("a1"));
    expect(await wakeDue(sql, { ...cfg, url: "" })).toBe(false);
  });
});
