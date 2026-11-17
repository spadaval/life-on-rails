// A page under /lab/ shares the dashboard's origin but cannot reach LoR's data (lor-zwfd).
// The page is served by the real /lab/ handler in front of a stand-in app that answers 200 to
// everything and counts what reaches it; the page, in the system Chrome, tries the API, a server
// function, a form post and a frame of a dashboard page. Needs Chrome, as the wall display does.
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { chromium } from "playwright-core";
import { serve } from "srvx";
import { afterAll, beforeAll, describe, expect, it } from "vitest";
// @ts-expect-error a plain .mjs module, no types
import { labStatic } from "../../scripts/lab/serve.mjs";

const ATTACK = `<!doctype html><meta charset="utf-8"><title>attack</title><link rel="icon" href="data:,">
<form id="f" method="post" action="/api/v1/status"><input name="a" value="1"></form>
<script>
window.violations = [];
document.addEventListener("securitypolicyviolation", (e) => violations.push(e.violatedDirective + " " + e.blockedURI));
const attempt = (p) => p.then((r) => "reached " + r.status, (e) => "refused");
window.run = async () => {
  const out = {};
  out.fixture = await fetch("/lab/fixture.json").then((r) => r.json()).then((j) => j.ok ? "read" : "wrong", () => "refused");
  out.api = await attempt(fetch("/api/v1/status"));
  out.serverFn = await attempt(fetch("/_serverFn/getStatus", { method: "POST", headers: { "content-type": "application/json" }, body: "{}" }));
  const frame = document.createElement("iframe");
  frame.src = "/tasks";
  document.body.append(frame);
  await new Promise((r) => setTimeout(r, 300));
  try { out.frame = frame.contentDocument && frame.contentDocument.body && frame.contentDocument.body.textContent.includes("from the app") ? "read" : "refused"; } catch { out.frame = "refused"; }
  document.getElementById("f").requestSubmit();
  await new Promise((r) => setTimeout(r, 300));
  out.stayed = location.pathname;
  return out;
};
</script>`;

let server: ReturnType<typeof serve>;
let base = "";
const hits: string[] = [];

beforeAll(async () => {
  const dir = path.join(mkdtempSync(path.join(tmpdir(), "lor-lab-browser-")), "lab");
  mkdirSync(dir, { recursive: true });
  writeFileSync(path.join(dir, "attack.html"), ATTACK);
  writeFileSync(path.join(dir, "fixture.json"), '{"ok":true}');
  server = serve({
    hostname: "127.0.0.1",
    port: 0,
    silent: true,
    gracefulShutdown: false,
    middleware: [labStatic(dir)],
    fetch: (request) => {
      hits.push(`${request.method} ${new URL(request.url).pathname}`);
      return new Response("<p>from the app</p>", { headers: { "content-type": "text/html" } });
    },
  });
  await server.ready();
  base = server.url!.replace(/\/$/, "");
});

afterAll(async () => {
  await server?.close(true);
});

describe("a page under /lab/", () => {
  it("reads /lab/ and nothing of LoR's", async () => {
    const browser = await chromium.launch({ channel: "chrome", headless: true });
    try {
      const page = await browser.newPage();
      await page.goto(`${base}/lab/attack.html`);
      const out = await page.evaluate("run()");
      const violations = (await page.evaluate("violations")) as string[];
      expect(out).toEqual({ fixture: "read", api: "refused", serverFn: "refused", frame: "refused", stayed: "/lab/attack.html" });
      expect(hits).toEqual([]);
      expect(violations.join("\n")).toMatch(/connect-src .*\/api\/v1\/status/);
      expect(violations.join("\n")).toMatch(/connect-src .*\/_serverFn\/getStatus/);
      expect(violations.join("\n")).toMatch(/frame-src .*\/tasks/);
      expect(violations.join("\n")).toMatch(/form-action/);
    } finally {
      await browser.close();
    }
  }, 30_000);
});
