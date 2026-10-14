// LoR's own Google login: an OAuth client of its own and a refresh token in a
// 0600 file outside the repo. The scopes are read-only, so LoR cannot write to
// Google or read a mail body even by mistake. `gog` is not involved.
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { z } from "zod";
import { env } from "../env.js";

export const SCOPES = {
  calendar: "https://www.googleapis.com/auth/calendar.readonly",
  tasks: "https://www.googleapis.com/auth/tasks.readonly",
  contacts: "https://www.googleapis.com/auth/contacts.readonly",
  gmail: "https://www.googleapis.com/auth/gmail.metadata",
} as const;

/** The token is missing, revoked, or lacks a scope. Fix: `pnpm google:auth`. */
export class AuthRequiredError extends Error {
  constructor(detail: string) {
    super(`Google auth required: run \`pnpm google:auth\` (${detail})`);
    this.name = "AuthRequiredError";
  }
}

/** Rate limit or a 5xx: the next scheduled run tries again. */
export class RetryableError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "RetryableError";
  }
}

const ClientFile = z.object({
  installed: z.object({
    client_id: z.string(),
    client_secret: z.string(),
    token_uri: z.string().default("https://oauth2.googleapis.com/token"),
    auth_uri: z.string().default("https://accounts.google.com/o/oauth2/auth"),
  }),
});

const TokenFile = z.object({
  refresh_token: z.string(),
  scope: z.string().default(""),
  access_token: z.string().optional(),
  /** Epoch ms. */
  expires_at: z.number().optional(),
});
export type TokenFile = z.infer<typeof TokenFile>;

export function readClient(): z.infer<typeof ClientFile>["installed"] {
  if (!existsSync(env.GOOGLE_OAUTH_CLIENT)) throw new AuthRequiredError(`no OAuth client at ${env.GOOGLE_OAUTH_CLIENT}`);
  return ClientFile.parse(JSON.parse(readFileSync(env.GOOGLE_OAUTH_CLIENT, "utf8"))).installed;
}

export function readToken(): TokenFile {
  if (!existsSync(env.GOOGLE_TOKEN_FILE)) throw new AuthRequiredError("no token yet");
  return TokenFile.parse(JSON.parse(readFileSync(env.GOOGLE_TOKEN_FILE, "utf8")));
}

export function writeToken(token: TokenFile): void {
  writeFileSync(env.GOOGLE_TOKEN_FILE, JSON.stringify(token, null, 2) + "\n", { mode: 0o600 });
}

export type GoogleAuthState =
  | { state: "ok"; scopes: string[] }
  | { state: "not-configured"; detail: string }
  | { state: "auth-required"; detail: string };

/** What the cockpit shows, from the files alone (no network). */
export function authState(): GoogleAuthState {
  if (!existsSync(env.GOOGLE_OAUTH_CLIENT)) {
    return { state: "not-configured", detail: `no OAuth client at ${env.GOOGLE_OAUTH_CLIENT}` };
  }
  try {
    const token = readToken();
    if (lastAuthFailure) return { state: "auth-required", detail: lastAuthFailure };
    return { state: "ok", scopes: token.scope.split(" ").filter(Boolean) };
  } catch (err) {
    return { state: "auth-required", detail: err instanceof Error ? err.message : String(err) };
  }
}

let lastAuthFailure: string | null = null;
let cached: { token: string; expiresAt: number } | null = null;

export function hasScope(scope: string): boolean {
  try {
    return readToken().scope.split(" ").includes(scope);
  } catch {
    return false;
  }
}

/** A valid access token, refreshed when it has under a minute left. */
export async function accessToken(fetchImpl: typeof fetch = fetch): Promise<string> {
  if (cached && cached.expiresAt - Date.now() > 60_000) return cached.token;
  const client = readClient();
  const stored = readToken();
  if (stored.access_token && stored.expires_at && stored.expires_at - Date.now() > 60_000) {
    cached = { token: stored.access_token, expiresAt: stored.expires_at };
    return cached.token;
  }
  const res = await fetchImpl(client.token_uri, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: client.client_id,
      client_secret: client.client_secret,
      refresh_token: stored.refresh_token,
      grant_type: "refresh_token",
    }),
  });
  const body = (await res.json().catch(() => ({}))) as { access_token?: string; expires_in?: number; error?: string; error_description?: string };
  if (!res.ok || !body.access_token) {
    if (res.status >= 500) throw new RetryableError(`token endpoint ${res.status}`);
    lastAuthFailure = body.error_description ?? body.error ?? `token endpoint ${res.status}`;
    cached = null;
    throw new AuthRequiredError(lastAuthFailure);
  }
  lastAuthFailure = null;
  const expiresAt = Date.now() + (body.expires_in ?? 3600) * 1000;
  cached = { token: body.access_token, expiresAt };
  writeToken({ ...stored, access_token: body.access_token, expires_at: expiresAt });
  return cached.token;
}

export function forgetAccessToken(): void {
  cached = null;
}
