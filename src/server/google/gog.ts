// Subprocess wrapper for `gog` (spec: Google integration, option A).
// Poll path always: --json --no-input --readonly --results-only --enable-commands=calendar
// Server-only: uses node:child_process. Never logs env or stderr (may hold secrets).
import { spawn } from "node:child_process";

export const GOG_EXIT = {
  ok: 0,
  usage: 2,
  empty: 3,
  authRequired: 4,
  notFound: 5,
  denied: 6,
  rateLimited: 7,
  retryable: 8,
} as const;

export class AuthRequiredError extends Error {
  constructor() {
    super("Google auth required — check GOG_KEYRING_* in the systemd env, or run `gog auth add` interactively (prefer --readonly)");
    this.name = "AuthRequiredError";
  }
}
export class RateLimitedError extends Error {
  constructor(code: number) {
    super(`gog rate limited / retryable (exit ${code})`);
    this.name = "RateLimitedError";
  }
}

function baseArgv(readonly: boolean): string[] {
  return [
    "--json",
    "--no-input",
    "--results-only",
    "--enable-commands=calendar",
    "-a",
    process.env.GOG_ACCOUNT ?? "sumeet.padavala@gmail.com",
    ...(readonly ? ["--readonly"] : []),
  ];
}

/** Never pass --force. Never call gmail (allowlist makes it a usage error). */
export async function gogJson(args: string[], opts?: { readonly?: boolean; timeoutMs?: number }): Promise<unknown> {
  if (args.includes("--force")) throw new Error("gog: --force is forbidden");
  if (args[0] === "gmail" || args.includes("gmail")) throw new Error("gog: gmail is not allowlisted in v1");
  const bin = process.env.GOG_BIN ?? "gog";
  const argv = [...baseArgv(opts?.readonly !== false), ...args];
  const timeoutMs = opts?.timeoutMs ?? 30_000;

  const child = spawn(bin, argv, {
    stdio: ["ignore", "pipe", "pipe"],
    env: {
      ...process.env,
      GOG_HOME: process.env.GOG_HOME,
      GOG_KEYRING_BACKEND: process.env.GOG_KEYRING_BACKEND,
      GOG_KEYRING_PASSWORD: process.env.GOG_KEYRING_PASSWORD,
    },
  });
  let stdout = "";
  let killed = false;
  const timer = setTimeout(() => {
    killed = true;
    child.kill("SIGKILL");
  }, timeoutMs);
  child.stdout.on("data", (d: Buffer) => {
    stdout += d.toString("utf8");
  });
  // stderr intentionally not collected (may contain secrets).
  const code: number = await new Promise((resolve) => {
    child.on("error", () => resolve(1));
    child.on("close", (c) => resolve(c ?? 1));
  });
  clearTimeout(timer);
  if (killed) throw new Error("gog: timeout");
  if (code === GOG_EXIT.authRequired) throw new AuthRequiredError();
  if (code === GOG_EXIT.rateLimited || code === GOG_EXIT.retryable) throw new RateLimitedError(code);
  if (code === GOG_EXIT.empty) return [];
  if (code !== GOG_EXIT.ok) throw new Error(`gog failed (exit ${code})`);
  const trimmed = stdout.trim();
  if (!trimmed) return [];
  return JSON.parse(trimmed) as unknown;
}

export function rfc3339(d: Date): string {
  return d.toISOString();
}
