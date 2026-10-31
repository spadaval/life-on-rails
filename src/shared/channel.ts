// What goes over the owner's WhatsApp channel, as text. Pure: no database, no
// network.

export const MAX_MESSAGE_CHARS = 4096;

/** Long text as several messages, cut at a paragraph, then a line, then a space. */
export function splitMessage(text: string, max = MAX_MESSAGE_CHARS): string[] {
  const parts: string[] = [];
  let rest = text.trim();
  while (rest.length > max) {
    const head = rest.slice(0, max);
    const at = [head.lastIndexOf("\n\n"), head.lastIndexOf("\n"), head.lastIndexOf(" ")].find((i) => i > max / 2) ?? max;
    parts.push(rest.slice(0, at).trimEnd());
    rest = rest.slice(at).trimStart();
  }
  if (rest) parts.push(rest);
  return parts;
}

export interface SentEarlier {
  /** Local time, already formatted. */
  at: string;
  /** Which run sent it. */
  from: string;
  text: string;
  /** The task it was about, so "done" can be acted on. */
  intentId?: string | null;
}

export interface Inbound {
  /** Local date and time, already formatted. */
  at: string;
  /** Where he said it: "WhatsApp", or "the dashboard". */
  via: string;
  text: string;
  /** A file he sent, saved on this box. */
  file?: { kind: string; path: string; voice: boolean } | null;
  /** What is on his screen, when he asked from the dashboard. */
  about?: string | null;
  sent: SentEarlier[];
}

/**
 * A message as the agent's thread receives it. The thread is compacted over
 * time, so everything it must not forget travels with every message: he is not
 * reading the thread, and only `lor message` reaches him.
 */
export function wrapInbound(m: Inbound): string {
  const lines = [
    `[From Sumeet via ${m.via}, ${m.at}. He is not reading this thread: only \`lor message\` reaches him, including any question (never use a question tool here). If this refers to something you don't remember, check \`lor message history\` first. Work that will take over a minute: send a short "on it" first.]`,
  ];
  if (m.about) lines.push("", `He is looking at: ${m.about}`);
  if (m.sent.length > 0) {
    lines.push("", "Sent to him by other runs since his last message:");
    for (const s of m.sent) lines.push(`- ${s.at}, ${s.from}${s.intentId ? ` (task ${s.intentId})` : ""}: ${s.text}`);
  }
  lines.push("");
  if (m.file) {
    lines.push(m.file.voice
      ? `(A voice note, at ${m.file.path}. Transcribe it with the \`transcribe\` skill.)`
      : `(He sent a ${m.file.kind}, at ${m.file.path}.)`);
  }
  if (m.text) lines.push(m.text);
  return lines.join("\n");
}
