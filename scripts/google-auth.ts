// pnpm google:auth [--mail]
//
// One-off consent for LoR's own Google login. Writes the refresh token to
// GOOGLE_TOKEN_FILE (0600, outside the repo). Read-only scopes only:
//   calendar.readonly, tasks.readonly            (always)
//   gmail.metadata, pubsub                        (with --mail)
//
// The box has no browser, so this prints a URL to open on any device. After
// you approve, the browser is sent to http://127.0.0.1:<port>/?code=... . On
// this box that completes by itself; on another device the page fails to load,
// and you paste the address from the address bar back here.
import { createServer } from "node:http";
import { createInterface } from "node:readline";
import { SCOPES, readClient, writeToken } from "../src/server/google/auth.js";
import { env } from "../src/server/env.js";

const mail = process.argv.includes("--mail");
const scopes = [SCOPES.calendar, SCOPES.tasks, ...(mail ? [SCOPES.gmail, SCOPES.pubsub] : [])];

const SETUP = `
LoR needs its own Google OAuth client (it does not use gog's login).

  1. https://console.cloud.google.com → create a project for Life on Rails.
  2. APIs & Services → Library: enable "Google Calendar API" and "Tasks API"
     (and, for mail, "Gmail API" and "Cloud Pub/Sub API").
  3. APIs & Services → OAuth consent screen: user type External; publish the
     app ("In production") so the sign-in does not expire after seven days.
  4. APIs & Services → Credentials → Create credentials → OAuth client ID →
     application type "Desktop app". Download the JSON and save it as
       ${env.GOOGLE_OAUTH_CLIENT}
     then: chmod 600 ${env.GOOGLE_OAUTH_CLIENT}
  5. Run \`pnpm google:auth\` again.
`;

let client: ReturnType<typeof readClient>;
try {
  client = readClient();
} catch {
  console.error(SETUP);
  process.exit(1);
}

function codeFrom(input: string): string | null {
  const text = input.trim();
  if (!text) return null;
  try {
    const url = new URL(text);
    return url.searchParams.get("code");
  } catch {
    return text; // a bare code
  }
}

const server = createServer();
await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
const port = (server.address() as { port: number }).port;
const redirectUri = `http://127.0.0.1:${port}`;

const consent = new URL(client.auth_uri);
consent.search = new URLSearchParams({
  client_id: client.client_id,
  redirect_uri: redirectUri,
  response_type: "code",
  scope: scopes.join(" "),
  access_type: "offline",
  prompt: "consent",
  ...(env.GOOGLE_ACCOUNT ? { login_hint: env.GOOGLE_ACCOUNT } : {}),
}).toString();

console.log(`\nOpen this URL and approve access for ${env.GOOGLE_ACCOUNT || "your account"}:\n\n${consent}\n`);
console.log("If the browser then shows a page that will not load, paste its full address here and press Enter.\n");

const code = await new Promise<string>((resolve, reject) => {
  server.on("request", (req, res) => {
    const got = new URL(req.url ?? "/", redirectUri).searchParams;
    const c = got.get("code");
    res.writeHead(c ? 200 : 400, { "content-type": "text/plain" });
    res.end(c ? "Life on Rails is signed in. You can close this tab.\n" : `No code: ${got.get("error") ?? "unknown error"}\n`);
    if (c) resolve(c);
    else reject(new Error(got.get("error") ?? "consent was not granted"));
  });
  const rl = createInterface({ input: process.stdin });
  rl.on("line", (line) => {
    const c = codeFrom(line);
    if (c) resolve(c);
  });
});
server.close();

const res = await fetch(client.token_uri, {
  method: "POST",
  headers: { "content-type": "application/x-www-form-urlencoded" },
  body: new URLSearchParams({
    code,
    client_id: client.client_id,
    client_secret: client.client_secret,
    redirect_uri: redirectUri,
    grant_type: "authorization_code",
  }),
});
const body = (await res.json()) as { refresh_token?: string; access_token?: string; expires_in?: number; scope?: string; error?: string; error_description?: string };
if (!res.ok || !body.refresh_token) {
  console.error(`Sign-in failed: ${body.error_description ?? body.error ?? res.status}`);
  process.exit(1);
}
writeToken({
  refresh_token: body.refresh_token,
  scope: body.scope ?? scopes.join(" "),
  access_token: body.access_token,
  expires_at: Date.now() + (body.expires_in ?? 3600) * 1000,
});
console.log(`\nSigned in. Token written to ${env.GOOGLE_TOKEN_FILE}`);
console.log(`Scopes: ${(body.scope ?? "").split(" ").map((s) => s.replace("https://www.googleapis.com/auth/", "")).join(", ")}`);
console.log("Restart the service to pick it up now: systemctl --user restart lor-web\n");
process.exit(0);
