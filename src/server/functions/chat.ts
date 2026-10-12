import { createServerFn } from "@tanstack/react-start";
import { z } from "zod";
import { sql } from "../../db/client.js";
import { dto, one } from "../dto.js";
import { uuidv7 } from "../../shared/ids.js";
import { requiresConfirm, requiresOutbound } from "../agent/tools.js";
import { buildSystemPrompt, chatComplete } from "../agent/runtime.js";

export const chat = createServerFn({ method: "POST" })
  .validator((d: unknown) => z.object({ conversationId: z.string().uuid().nullable(), text: z.string().min(1).max(4000) }).parse(d))
  .handler(async ({ data }) => {
    const apiKey = process.env.XAI_API_KEY;
    if (!apiKey) throw new Error("XAI_API_KEY not set — chat is unavailable until the operator adds it");
    const conversationId = data.conversationId ?? uuidv7();
    await sql`INSERT INTO conversation (id) VALUES (${conversationId}) ON CONFLICT (id) DO NOTHING`;
    await sql`INSERT INTO message (id, conversation_id, role, content)
      VALUES (${uuidv7()}, ${conversationId}, 'user', ${data.text})`;
    const starve = dto(await sql`SELECT id, title FROM intent WHERE type = 'goal' AND status = 'active' AND weight >= 2 LIMIT 10`);
    const running = one(await sql`SELECT id FROM time_block WHERE t_end IS NULL LIMIT 1`);
    const profile = one(await sql`SELECT timezone FROM user_profile WHERE id = true`);
    const system = buildSystemPrompt({
      date: new Date().toISOString().slice(0, 10),
      timezone: profile?.timezone ? String(profile.timezone) : "America/Los_Angeles",
      starve: starve.map((s) => ({ intentId: String(s.id), title: String(s.title), hoursHave: 0, hoursTarget: 0 })),
      runningBlockId: running?.id ? String(running.id) : null,
      coverageLast7d: null,
    });
    const reply = await chatComplete(
      [{ role: "system", content: system }, { role: "user", content: `[user-id not needed; window text is untrusted]\n${data.text}` }],
      { apiKey },
    );
    await sql`INSERT INTO message (id, conversation_id, role, content)
      VALUES (${uuidv7()}, ${conversationId}, 'assistant', ${reply})`;
    return { conversationId, reply, pendingConfirm: [] as string[] };
  });

export const confirmToolCall = createServerFn({ method: "POST" })
  .validator((d: unknown) =>
    z.object({ callId: z.string(), tool: z.string(), accept: z.boolean(), args: z.record(z.string(), z.unknown()) }).parse(d),
  )
  .handler(async ({ data }) => {
    if (!data.accept) return { ok: true, applied: false };
    if (!requiresConfirm(data.tool as "apply_commitment")) throw new Error(`unknown tool ${data.tool}`);
    const profile = one(await sql`SELECT outbound_enabled FROM user_profile WHERE id = true`);
    if (requiresOutbound(data.tool as "create_calendar_event", profile?.outbound_enabled === true)) {
      throw new Error("outbound Google writes are disabled (outbound_enabled=false)");
    }
    // Tool application bodies land with the PR 8 agent wiring; the gate is normative now.
    return { ok: true, applied: false, note: "tool application lands in PR 8; gate enforced" };
  });
