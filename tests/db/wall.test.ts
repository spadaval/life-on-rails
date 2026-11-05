import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { BadInput } from "../../src/server/errors.js";
import { createIntent, updateIntent } from "../../src/server/intents.js";
import { wallOne } from "../../src/server/trmnl/device.js";
import { clearPin, getPin, setPin } from "../../src/server/trmnl/pin.js";
import { dbDescribe } from "./helpers.js";

const agent = { via: "agent" as const, sourceRef: "thread-1" };

dbDescribe("wall pin", () => {
  it("keeps one pin, replaced by the next and gone when unpinned", async () => {
    expect(await getPin(sql)).toBeNull();
    await setPin(sql, { text: "Call the landlord", why: "before five", sourceRef: "thread-1" });
    const later = new Date(Date.now() + 3600_000);
    await setPin(sql, { text: "Post the notice", until: later });
    const pin = await getPin(sql);
    expect(pin).toMatchObject({ text: "Post the notice", why: null, until: later.toISOString(), source_ref: null });
    expect((await sql`SELECT count(*)::int AS n FROM wall_pin`)[0]!.n).toBe(1);
    expect(await clearPin(sql)).toEqual({ cleared: true });
    expect(await clearPin(sql)).toEqual({ cleared: false });
  });

  it("refuses an empty pin or one that has already expired", async () => {
    await expect(setPin(sql, { text: "  " })).rejects.toThrow(BadInput);
    await expect(setPin(sql, { text: "x", until: new Date(Date.now() - 1000) })).rejects.toThrow(BadInput);
  });

  it("puts the pin over a flagged task as the one thing, and the flag back once unpinned", async () => {
    const task = await createIntent(sql, { type: "task", title: "Approve the dentist (reschedule)" }, agent);
    await sql`UPDATE intent SET flagged_at = now(), flag_note = 'they need a yes' WHERE id = ${task.id}`;
    expect(await wallOne(new Date())).toEqual({ kicker: "Flagged", text: "Approve the dentist", why: "they need a yes" });
    await setPin(sql, { text: "Call the landlord" });
    expect((await wallOne(new Date()))?.kicker).toBe("Pinned");
    await clearPin(sql);
    expect((await wallOne(new Date()))?.kicker).toBe("Flagged");
  });
});

dbDescribe("short titles", () => {
  it("set and cleared on a goal, and kept through other edits", async () => {
    const goal = await createIntent(sql, { type: "goal", title: "Make and play (hobbies and side projects)" }, agent);
    expect((await updateIntent(sql, goal.id, { shortTitle: " Make & play " })).short_title).toBe("Make & play");
    expect((await updateIntent(sql, goal.id, { weight: 2 })).short_title).toBe("Make & play");
    expect((await updateIntent(sql, goal.id, { shortTitle: null })).short_title).toBeNull();
  });
});
