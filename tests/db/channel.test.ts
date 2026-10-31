import { beforeEach, expect, it, vi } from "vitest";
import { sql } from "../../src/db/client.js";

vi.mock("../../src/server/env.js", async (original) => {
  const real = await original<typeof import("../../src/server/env.js")>();
  return { ...real, env: { ...real.env, LOR_CHANNEL_KEY: "key", LOR_CHANNEL_THREAD: "main-thread", LOR_CHANNEL_DIR: "/tmp/lor-channel-test" } };
});

const { askFromDashboard, messageHistory, pendingFlags, pollOnce, sendFlags, sendMessage } = await import("../../src/server/channel/channel.js");
const { createIntent, flagTask } = await import("../../src/server/intents.js");
import type { ChannelPort, InboundMessage, Outbound } from "../../src/server/channel/client.js";
import { dbDescribe } from "./helpers.js";

function text(id: string, body: string, ts = 1_793_000_000): InboundMessage {
  return { from: "user:1", id, timestamp: String(ts), type: "text", text: { body } };
}

function fake() {
  const sent: { to: string; m: Outbound }[] = [];
  const handed: string[] = [];
  const offsets: number[] = [];
  let queue: { messages: InboundMessage[]; nextOffset: number } | null = null;
  const port: ChannelPort = {
    updates: async (offset) => {
      offsets.push(offset);
      const q = queue;
      queue = null;
      return q;
    },
    send: async (to, m) => {
      sent.push({ to, m });
      return `wamid.out${sent.length}`;
    },
    markRead: async () => {},
    upload: async () => "media-1",
    download: async () => new Uint8Array([1, 2, 3]),
  };
  return { port, sent, handed, offsets, handoff: async (t: string) => void handed.push(t), arrive: (messages: InboundMessage[], nextOffset: number) => { queue = { messages, nextOffset }; } };
}

dbDescribe("the channel", () => {
  beforeEach(async () => {
    await sql`DELETE FROM sync_cursor WHERE name LIKE 'channel_%'`;
  });

  it("hands a message to the thread once, even when the same position is read again", async () => {
    const f = fake();
    f.arrive([text("wamid.1", "remember the dentist")], 5);
    expect(await pollOnce(sql, f.port, f.handoff)).toBe(1);
    expect(f.handed[0]).toContain("remember the dentist");
    expect(f.handed[0]).toContain("He is not reading this thread");

    f.arrive([text("wamid.1", "remember the dentist")], 5);
    expect(await pollOnce(sql, f.port, f.handoff)).toBe(0);
    expect(f.handed).toHaveLength(1);
    expect(f.offsets).toEqual([0, 5]);
  });

  it("keeps the position when the handover fails, so nothing is lost", async () => {
    const f = fake();
    f.arrive([text("wamid.1", "hello")], 9);
    await expect(pollOnce(sql, f.port, async () => { throw new Error("t3 down"); })).rejects.toThrow("t3 down");
    f.arrive([text("wamid.1", "hello")], 9);
    expect(await pollOnce(sql, f.port, f.handoff)).toBe(1);
    expect(f.offsets).toEqual([0, 0]);
  });

  it("cannot send until he has messaged, and then uses his latest id", async () => {
    const f = fake();
    await expect(sendMessage(sql, f.port, { text: "hi", thread: "t" })).rejects.toThrow("message it once");
    f.arrive([text("wamid.1", "hi")], 1);
    await pollOnce(sql, f.port, f.handoff);
    f.arrive([{ ...text("wamid.2", "new phone"), from: "user:2" }], 2);
    await pollOnce(sql, f.port, f.handoff);
    await sendMessage(sql, f.port, { text: "hello", thread: "t" });
    expect(f.sent[0]!.to).toBe("user:2");
  });

  it("tells the thread what other runs sent since his last message, and only those", async () => {
    const f = fake();
    f.arrive([text("wamid.1", "hi", Math.floor(Date.now() / 1000) - 60)], 1);
    await pollOnce(sql, f.port, f.handoff);
    await sendMessage(sql, f.port, { text: "Draft the notice?", thread: "claude:mail-run" });
    await sendMessage(sql, f.port, { text: "looking now", thread: "main-thread" });
    f.arrive([text("wamid.2", "yes", Math.floor(Date.now() / 1000) + 1)], 2);
    await pollOnce(sql, f.port, f.handoff);
    expect(f.handed[1]).toContain("claude:mail-run: Draft the notice?");
    expect(f.handed[1]).not.toContain("looking now");
    f.arrive([text("wamid.3", "and?", Math.floor(Date.now() / 1000) + 2)], 3);
    await pollOnce(sql, f.port, f.handoff);
    expect(f.handed[2]).not.toContain("Draft the notice?");
  });

  it("sends a flag once, and again when flagged with a new reason", async () => {
    const f = fake();
    f.arrive([text("wamid.1", "hi")], 1);
    await pollOnce(sql, f.port, f.handoff);
    const task = await createIntent(sql, { type: "task", title: "Sign the lease" }, { via: "agent", sourceRef: "t" });
    await flagTask(sql, task.id, "Due Monday");
    expect(await sendFlags(sql, f.port)).toEqual({ sent: 1 });
    expect(f.sent[0]!.m).toEqual({ type: "text", text: "Needs you: Sign the lease\nDue Monday" });
    expect(await pendingFlags(sql)).toEqual([]);
    await flagTask(sql, task.id, "Due tomorrow");
    expect(await sendFlags(sql, f.port)).toEqual({ sent: 1 });
  });

  it("finds earlier messages by date, words, and around a hit", async () => {
    const f = fake();
    const base = Math.floor(Date.now() / 1000) - 1000;
    f.arrive([text("wamid.1", "the hawthorne notice", base), text("wamid.2", "something else", base + 1), text("wamid.3", "third", base + 2)], 3);
    await pollOnce(sql, f.port, f.handoff);
    const hits = await messageHistory(sql, { search: "hawthorne" });
    expect(hits.map((m) => m.text)).toEqual(["the hawthorne notice"]);
    expect((await messageHistory(sql, { limit: 2 })).map((m) => m.text)).toEqual(["something else", "third"]);
    expect((await messageHistory(sql, { around: hits[0]!.id, limit: 2 })).map((m) => m.text)).toEqual(["the hawthorne notice", "something else"]);
  });

  it("takes a question from the dashboard into the same conversation", async () => {
    const f = fake();
    await askFromDashboard(sql, f.handoff, "Who is this?", "person: Priya");
    expect(f.handed[0]).toContain("via the dashboard");
    expect(f.handed[0]).toContain("He is looking at: person: Priya");
    expect((await messageHistory(sql, {})).map((m) => m.direction)).toEqual(["in"]);
  });
});
