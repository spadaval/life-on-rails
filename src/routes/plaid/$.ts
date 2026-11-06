// Linking bank logins with Plaid Link. A plain page and three calls, reached on
// the owner-only listener like the rest of the UI. The POSTs take JSON only,
// which a page on another site cannot send here without a preflight.
import { createFileRoute } from "@tanstack/react-router";
import { z, ZodError } from "zod";
import { sql } from "../../db/client.js";
import { errorMessage, log } from "../../server/log.js";
import { listAccounts } from "../../server/money/read.js";
import { PlaidError, plaid } from "../../server/money/plaid.js";
import { completeLink, linkToken, syncItem } from "../../server/money/sync.js";

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

const PAGE = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Link accounts · Life on Rails</title>
<style>
  :root { color-scheme: dark; }
  body { margin: 0; background: #0c0d12; color: #ece6dc; font: 15px/1.5 Saira, ui-sans-serif, system-ui, sans-serif; font-feature-settings: "tnum"; }
  main { max-width: 640px; margin: 0 auto; padding: 32px 16px; }
  h1 { font: 600 22px/1.5 "Saira Condensed", Saira, sans-serif; letter-spacing: .08em; text-transform: uppercase; margin: 0 0 4px; }
  p { color: #b1a596; margin: 0 0 20px; }
  button { font: 500 13.5px/1.5 Saira, sans-serif; background: #f08a2c; color: #0c0d12; border: 0; padding: 8px 16px; cursor: pointer; }
  button.quiet { background: transparent; color: #f08a2c; border: 1px solid #9a5c22; padding: 4px 12px; }
  ul { list-style: none; padding: 0; margin: 24px 0 0; border-top: 1px solid #1e2029; }
  li { padding: 12px 0; border-bottom: 1px solid #1e2029; display: flex; justify-content: space-between; gap: 12px; align-items: center; }
  .sub { color: #928777; font-size: 13px; }
  .bad { color: #e0735a; }
  #msg { margin-top: 16px; color: #6cc6ec; min-height: 1.5em; }
</style></head>
<body><main>
  <h1>Bank accounts</h1>
  <p>Each login you link counts once against Plaid's limit, even if it is removed later.</p>
  <button id="add">Link a login</button>
  <div id="msg"></div>
  <ul id="items"></ul>
</main>
<script src="https://cdn.plaid.com/link/v2/stable/link-initialize.js"></script>
<script>
const msg = (t) => { document.getElementById("msg").textContent = t; };
const post = async (path, body) => {
  const r = await fetch(path, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });
  const d = await r.json();
  if (!r.ok) throw new Error(d.error?.message ?? r.statusText);
  return d;
};
async function load() {
  const d = await (await fetch("/plaid/items")).json();
  const ul = document.getElementById("items");
  ul.replaceChildren();
  for (const i of d.items) {
    const li = document.createElement("li");
    const n = d.accounts.filter((a) => a.item_id === i.id).length;
    const left = document.createElement("div");
    left.innerHTML = '<div></div><div class="sub"></div>';
    left.children[0].textContent = i.institution;
    left.children[1].textContent = n + " account" + (n === 1 ? "" : "s") + (i.synced_at ? " · synced " + new Date(i.synced_at).toLocaleString() : " · not synced yet");
    if (i.error) { left.children[1].textContent += " · " + i.error; left.children[1].classList.add("bad"); }
    li.append(left);
    if (i.error) {
      const b = document.createElement("button");
      b.className = "quiet"; b.textContent = "Sign in again";
      b.onclick = () => startLink(i.id);
      li.append(b);
    }
    ul.append(li);
  }
}
async function startLink(itemId) {
  try {
    const { link_token } = await post("/plaid/link-token", itemId ? { item_id: itemId } : {});
    Plaid.create({
      token: link_token,
      onSuccess: async (public_token) => {
        msg("Saving…");
        try {
          const r = itemId ? await post("/plaid/relinked", { item_id: itemId }) : await post("/plaid/exchange", { public_token });
          msg((r.institution ?? "Login") + " linked. Transactions arrive over the next few minutes.");
        } catch (e) { msg("Failed: " + e.message); }
        load();
      },
      onExit: (err) => { if (err) msg("Plaid: " + (err.display_message || err.error_message || err.error_code)); },
    }).open();
  } catch (e) { msg("Failed: " + e.message); }
}
document.getElementById("add").onclick = () => startLink();
load();
</script>
</body></html>`;

const isJson = (request: Request) => (request.headers.get("content-type") ?? "").startsWith("application/json");

/** Sync a login just linked, without making the page wait for it. */
function syncSoon(itemId: string): void {
  void syncItem(sql, plaid, itemId).catch((err) => log("warn", "plaid first sync failed", { err: errorMessage(err) }));
}

async function handle(request: Request): Promise<Response> {
  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/plaid/, "");
  try {
    if (request.method === "GET" && path === "/link") return new Response(PAGE, { headers: { "content-type": "text/html; charset=utf-8" } });
    if (request.method === "GET" && path === "/items") return json(200, await listAccounts(sql));
    if (request.method === "POST" && isJson(request)) {
      const body = await request.json();
      if (path === "/link-token") return json(200, await linkToken(plaid, z.object({ item_id: z.string().optional() }).parse(body).item_id));
      if (path === "/exchange") {
        const r = await completeLink(sql, plaid, z.object({ public_token: z.string() }).parse(body).public_token);
        syncSoon(r.item_id);
        return json(200, r);
      }
      if (path === "/relinked") {
        const { item_id } = z.object({ item_id: z.string() }).parse(body);
        const [row] = await sql<{ institution: string }[]>`UPDATE money_item SET error = NULL WHERE id = ${item_id} RETURNING institution`;
        if (!row) return json(404, { error: { code: "not-found", message: "no such login" } });
        syncSoon(item_id);
        return json(200, { item_id, institution: row.institution });
      }
    }
    return json(404, { error: { code: "no-such-endpoint", message: `${request.method} ${url.pathname}` } });
  } catch (err) {
    if (err instanceof ZodError || err instanceof SyntaxError) return json(400, { error: { code: "bad-input", message: errorMessage(err) } });
    if (err instanceof PlaidError) return json(502, { error: { code: err.code, message: err.message } });
    log("error", "plaid endpoint failed", { path, err: errorMessage(err) });
    return json(500, { error: { code: "server-error", message: errorMessage(err) } });
  }
}

export const Route = createFileRoute("/plaid/$")({
  server: { handlers: { GET: ({ request }) => handle(request), POST: ({ request }) => handle(request) } },
});
