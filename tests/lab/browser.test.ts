// A page under /lab/ shares the dashboard's origin but cannot reach LoR's data (lor-zwfd).
// The page is served by the real /lab/ handler in front of a stand-in app that answers 200 to
// everything and counts what reaches it; the page, in the system Chrome, tries the API, a server
// function, a form post and a frame of a dashboard page. Needs Chrome, as the wall display does.
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { request as httpRequest } from "node:http";
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
  const obj = document.createElement("object");
  obj.data = "/tasks";
  obj.type = "text/html";
  const embed = document.createElement("embed");
  embed.src = "/notes";
  embed.type = "text/html";
  document.body.append(obj, embed);
  await new Promise((r) => setTimeout(r, 300));
  // A window on the dashboard carries the app's policy, so the lab must not get one.
  try {
    const w = window.open("/tasks");
    await new Promise((r) => setTimeout(r, 500));
    out.popup = w ? "opened" : "refused";
    if (w) {
      out.popupRead = w.document.body.textContent;
      out.popupWrite = await w.fetch("/_serverFn/write", { method: "POST", body: "{}" }).then((r) => r.status);
    }
  } catch { out.popup = "refused"; }
  document.getElementById("f").requestSubmit();
  await new Promise((r) => setTimeout(r, 300));
  out.stayed = location.pathname;
  return out;
};
</script>`;

// A lab page that, given a dashboard window, reads it and posts through it.
const PROBE = `<!doctype html><meta charset="utf-8"><link rel="icon" href="data:,"><script>
window.probe = async (w) => {
  if (!w) return "no window";
  try { return { read: w.document.body.textContent, write: await w.fetch("/_serverFn/write", { method: "POST", body: "{}" }).then((r) => r.status) }; }
  catch (e) { return "refused"; }
};
window.result = window.parent !== window ? probe(window.parent) : null;
</script>`;

// Dashboard pages that frame the lab, and link to it the way a page would.
const DASHBOARD: Record<string, string> = {
  "/dash-frame": `<!doctype html><link rel="icon" href="data:,"><p id="data">dashboard data</p><iframe src="/lab/probe.html"></iframe>`,
  "/dash-link": `<!doctype html><link rel="icon" href="data:,"><p id="data">dashboard data</p><a id="go" href="/lab/probe.html" target="_blank">lab</a>`,
};

let server: ReturnType<typeof serve>;
let base = "";
const hits: string[] = [];

beforeAll(async () => {
  const top = mkdtempSync(path.join(tmpdir(), "lor-lab-browser-"));
  const dir = path.join(top, "lab");
  mkdirSync(dir, { recursive: true });
  writeFileSync(path.join(top, "secret.txt"), "outside the lab");
  writeFileSync(path.join(dir, "attack.html"), ATTACK);
  writeFileSync(path.join(dir, "fixture.json"), '{"ok":true}');
  writeFileSync(path.join(dir, "probe.html"), PROBE);
  server = serve({
    hostname: "127.0.0.1",
    port: 0,
    silent: true,
    gracefulShutdown: false,
    middleware: [labStatic(dir)],
    fetch: (request) => {
      const p = new URL(request.url).pathname;
      hits.push(`${request.method} ${p}`);
      const html = DASHBOARD[p] ?? "<p>from the app</p>";
      return new Response(html, { headers: { "content-type": "text/html" } });
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
      expect(out).toEqual({ fixture: "read", api: "refused", serverFn: "refused", frame: "refused", popup: "refused", stayed: "/lab/attack.html" });
      expect(hits).toEqual([]);
      expect(violations.join("\n")).toMatch(/connect-src .*\/api\/v1\/status/);
      expect(violations.join("\n")).toMatch(/connect-src .*\/_serverFn\/getStatus/);
      expect(violations.join("\n")).toMatch(/frame-src/);
      expect(violations.join("\n")).toMatch(/form-action/);
      expect(violations.join("\n")).toMatch(/object-src/);
    } finally {
      await browser.close();
    }
  }, 30_000);
});

describe("a lab page next to a dashboard page", () => {
  it("is not shown in the dashboard's frame, and a link gives it no opener", async () => {
    const browser = await chromium.launch({ channel: "chrome", headless: true });
    try {
      const context = await browser.newContext();
      const dash = await context.newPage();
      hits.length = 0;
      await dash.goto(`${base}/dash-frame`);
      await dash.waitForTimeout(500);
      const frame = dash.frames().find((f) => f.url().includes("/lab/probe.html"));
      const framed = frame ? await frame.evaluate("window.result").catch(() => "blocked") : "blocked";
      expect(framed === null || framed === "blocked" || framed === "no window").toBe(true);

      await dash.goto(`${base}/dash-link`);
      const [lab] = await Promise.all([context.waitForEvent("page"), dash.click("#go")]);
      await lab.waitForLoadState();
      expect(await lab.evaluate("window.opener === null")).toBe(true);
      expect(await lab.evaluate("probe(window.opener)")).toBe("no window");
      expect(hits.filter((h) => h.startsWith("POST"))).toEqual([]);
    } finally {
      await browser.close();
    }
  }, 30_000);
});

// A browser and fetch() normalise `..` away before sending; a raw socket does not.
const raw = (p: string) =>
  new Promise<{ status: number; body: string }>((resolve, reject) => {
    const u = new URL(base);
    const req = httpRequest({ host: u.hostname, port: u.port, path: p, method: "GET" }, (res) => {
      let body = "";
      res.on("data", (c) => (body += c));
      res.on("end", () => resolve({ status: res.statusCode ?? 0, body }));
    });
    req.on("error", reject);
    req.end();
  });

describe("a raw path under /lab/", () => {
  it("never reads outside the directory", async () => {
    for (const p of ["/lab/../secret.txt", "/lab/x/../../secret.txt", "/lab/..%2fsecret.txt", "/lab/..%2f..%2fsecret.txt", "/lab/%2e%2e/secret.txt", "/lab/..\\secret.txt"]) {
      const res = await raw(p);
      expect(res.body, p).not.toContain("outside the lab");
    }
  });
});
