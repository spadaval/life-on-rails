// Config is the env file and nothing else (spec: Config). Under systemd the
// unit's EnvironmentFile has already set these; for scripts, tests, and the
// CLI we load the same file here. Existing variables are never overridden.
import { existsSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";
import { z } from "zod";

export const ENV_FILE = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");

if (existsSync(ENV_FILE)) {
  try {
    process.loadEnvFile(ENV_FILE);
  } catch {
    // An unreadable env file is reported by whatever needs a missing value.
  }
}

const hm = z.string().regex(/^\d{2}:\d{2}$/);

const Env = z.object({
  DATABASE_URL: z.string().default("postgres:///life_on_rails?host=/var/run/postgresql"),
  LOR_BIND: z.string().default("127.0.0.1"),
  LOR_PORT: z.coerce.number().int().default(3847),
  LOR_SERVE_PORT: z.coerce.number().int().default(3848),
  LOR_OWNER_LOGIN: z.string().default(""),
  LOR_TZ: z.string().default("America/New_York"),
  LOR_WAKE_START: hm.default("08:00"),
  LOR_WAKE_END: hm.default("22:00"),
  LOR_SCORING_WEEKS: z.coerce.number().int().min(1).default(2),
  LOR_CALENDAR_POLL_S: z.coerce.number().int().min(30).default(300),
  LOR_TASKS_POLL_S: z.coerce.number().int().min(30).default(900),
  LOR_CONTACTS_POLL_S: z.coerce.number().int().min(60).default(21600),
  LOR_MAIL_POLL_S: z.coerce.number().int().min(30).default(60),
  // Country calling code for numbers written without one ("(415) 555-0100").
  LOR_PHONE_COUNTRY_CODE: z.string().regex(/^\d{1,3}$/).default("1"),
  LOR_AGENT_TOKEN: z.string().default(""),
  LOR_WAKE_QUIET_S: z.coerce.number().int().min(0).default(180),
  LOR_WAKE_MAX_S: z.coerce.number().int().min(0).default(900),
  // WhatsApp: the phone uploads its encrypted nightly backup here; the key is the 64-digit backup key, alone in its file (0600).
  LOR_WHATSAPP_DIR: z.string().default(path.join(homedir(), ".local/share/life-on-rails/whatsapp")),
  LOR_WHATSAPP_KEY_FILE: z.string().default(path.join(homedir(), ".config/life-on-rails/whatsapp-key")),
  // The TRMNL X on the wall: the LAN address its listener binds (empty: off), the device's MAC, and the key it is given.
  LOR_TRMNL_BIND: z.string().default(""),
  LOR_TRMNL_PORT: z.coerce.number().int().default(3849),
  LOR_TRMNL_MAC: z.string().default(""),
  LOR_TRMNL_KEY: z.string().default(""),
  LOR_TRMNL_FIRMWARE: z.string().default(""),
  // The owner's WhatsApp channel: the agent contact's API key (empty: off), the T3 thread the conversation lives in, and where files he sends are kept.
  LOR_CHANNEL_KEY: z.string().default(""),
  LOR_CHANNEL_THREAD: z.string().default(""),
  LOR_CHANNEL_DIR: z.string().default(path.join(homedir(), ".local/share/life-on-rails/channel")),
  LOR_T3_BIN: z.string().default(path.join(homedir(), ".local/bin/t3")),
  LOR_ASSISTANT_DIR: z.string().default(path.join(homedir(), "google/assistant")),
  GOOGLE_ACCOUNT: z.string().default(""),
  GOOGLE_OAUTH_CLIENT: z.string().default(path.join(homedir(), ".config/life-on-rails/google-client.json")),
  GOOGLE_TOKEN_FILE: z.string().default(path.join(homedir(), ".config/life-on-rails/google-token.json")),
});

export type Env = z.infer<typeof Env>;
export const env: Env = Env.parse(process.env);
