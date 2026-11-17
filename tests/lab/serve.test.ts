// /lab/ on lor-web's listeners (lor-zwfd): static files from LOR_LAB_DIR, read-only, and
// nothing outside it.
import { mkdirSync, mkdtempSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";
// @ts-expect-error a plain .mjs module, no types
import { isHashed, labStatic } from "../../scripts/lab/serve.mjs";

type Handler = (request: Request, next: () => Response | Promise<Response>) => Response | Promise<Response>;

const fromApp = () => new Response("from the app", { status: 299 });

function setup() {
  const base = mkdtempSync(path.join(tmpdir(), "lor-lab-"));
  const dir = path.join(base, "lab");
  mkdirSync(path.join(dir, "shots/dashboard"), { recursive: true });
  mkdirSync(path.join(dir, "launcher"), { recursive: true });
  writeFileSync(path.join(dir, "index.html"), "<h1>lab</h1>");
  writeFileSync(path.join(dir, "shots/dashboard/index.html"), "gallery");
  writeFileSync(path.join(dir, "shots/dashboard/today@1500.png"), "png");
  // A stand-in wasm module (the magic number and version) and a hashed bundle.
  writeFileSync(path.join(dir, "launcher/homecore.wasm"), Buffer.from([0, 0x61, 0x73, 0x6d, 1, 0, 0, 0]));
  writeFileSync(path.join(dir, "launcher/index-B3x9aZ_k.js"), "export {}");
  writeFileSync(path.join(base, "secret.txt"), "outside");
  symlinkSync(path.join(base, "secret.txt"), path.join(dir, "link-out.txt"));
  return { base, dir, lab: labStatic(dir) as Handler };
}

const get = (lab: Handler, p: string, init: RequestInit & { host?: string } = {}) =>
  lab(new Request(`http://${init.host ?? "127.0.0.1:3848"}${p}`, init), fromApp);

describe("labStatic", () => {
  it("serves the directory's files and its indexes", async () => {
    const { lab } = setup();
    const res = await get(lab, "/lab/");
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toMatch(/^text\/html/);
    expect(await res.text()).toBe("<h1>lab</h1>");
    expect(await (await get(lab, "/lab/shots/dashboard/")).text()).toBe("gallery");
    expect((await get(lab, "/lab/shots/dashboard/today@1500.png")).headers.get("content-type")).toBe("image/png");
  });

  it("redirects a directory to its slash, so relative links resolve", async () => {
    const { lab } = setup();
    const root = await get(lab, "/lab");
    expect([root.status, root.headers.get("location")]).toEqual([308, "/lab/"]);
    const sub = await get(lab, "/lab/shots/dashboard?x=1");
    expect([sub.status, sub.headers.get("location")]).toEqual([308, "/lab/shots/dashboard/?x=1"]);
  });

  it("is a 404 when LOR_LAB_DIR is unset or missing, and never reaches the app", async () => {
    for (const lab of [labStatic(null), labStatic("/nonexistent/lor-lab")] as Handler[]) {
      expect((await get(lab, "/lab/")).status).toBe(404);
      expect((await get(lab, "/lab/x.html")).status).toBe(404);
    }
    const { lab } = setup();
    expect((await get(lab, "/lab/missing.html")).status).toBe(404);
  });

  it("passes every other path on, /labs included", async () => {
    const { lab } = setup();
    for (const p of ["/", "/tasks", "/labs", "/api/v1/status", "/lab-x"]) expect((await get(lab, p)).status).toBe(299);
  });

  it("cannot be walked out of", async () => {
    const { lab } = setup();
    for (const p of [
      "/lab/../secret.txt",
      "/lab/%2e%2e/secret.txt",
      "/lab/..%2fsecret.txt",
      "/lab/shots/%2e%2e%2f%2e%2e%2fsecret.txt",
      "/lab/%2fetc%2fpasswd",
      "/lab/link-out.txt",
      "/lab/%00",
    ]) {
      const res = await get(lab, p);
      expect([400, 404, 299], p).toContain(res.status);
      expect(await res.text(), p).not.toBe("outside");
      expect(res.status, p).not.toBe(200);
    }
  });

  it("is read-only", async () => {
    const { lab } = setup();
    for (const method of ["POST", "PUT", "DELETE", "PATCH"]) {
      const res = await get(lab, "/lab/index.html", { method, body: method === "DELETE" ? undefined : "x" });
      expect(res.status, method).toBe(405);
    }
    const head = await get(lab, "/lab/index.html", { method: "HEAD" });
    expect([head.status, head.headers.get("content-length")]).toEqual([200, "12"]);
  });

  it("keeps a page to /lab/ on the origin the browser used", async () => {
    const { lab } = setup();
    for (const p of ["/lab/", "/lab/missing", "/lab", "/lab/launcher/homecore.wasm"]) {
      const csp = (await get(lab, p, { host: "buildbot:3847" })).headers.get("content-security-policy") ?? "";
      expect(csp, p).toContain("connect-src http://buildbot:3847/lab/");
      expect(csp, p).toContain("frame-src http://buildbot:3847/lab/");
      expect(csp, p).toContain("form-action 'none'");
      expect(csp, p).toContain("object-src 'none'");
    }
    expect((await get(lab, "/lab/", { method: "POST" })).headers.get("content-security-policy")).toContain("form-action 'none'");
  });

  it("serves wasm as application/wasm and caches hashed names", async () => {
    const { lab } = setup();
    const wasm = await get(lab, "/lab/launcher/homecore.wasm");
    expect(wasm.headers.get("content-type")).toBe("application/wasm");
    expect(wasm.headers.get("cache-control")).toBe("no-cache");
    expect(await WebAssembly.validate(new Uint8Array(await wasm.arrayBuffer()))).toBe(true);
    const js = await get(lab, "/lab/launcher/index-B3x9aZ_k.js");
    expect(js.headers.get("cache-control")).toBe("public, max-age=31536000, immutable");
    expect(js.headers.get("content-type")).toMatch(/^text\/javascript/);
    expect((await get(lab, "/lab/index.html")).headers.get("cache-control")).toBe("no-cache");
  });

  it("answers a revalidation with 304", async () => {
    const { lab } = setup();
    const etag = (await get(lab, "/lab/index.html")).headers.get("etag")!;
    const again = await get(lab, "/lab/index.html", { headers: { "if-none-match": etag } });
    expect(again.status).toBe(304);
  });
});

describe("isHashed", () => {
  it("knows a bundler's hash from a gallery's names", () => {
    for (const n of ["index-B3x9aZ_k.js", "homecore.4f2a9c1e.wasm", "app.0123456789abcdef.css", "chunk-Dq3k_1aZ.mjs", "0d7a432110fc5479b82d.wasm"]) expect(isHashed(n), n).toBe(true);
    for (const n of [
      "index.html",
      "homecore.wasm",
      "goals-with-a-habit-selected-and-its-editor-open@1100.png",
      "a-person-s-hub-with-messages-and-mail@412.png",
      "composeApp.js",
      "today-concept2a.png",
      "home-version2.png",
      "tasks-recording1.webm",
      "my-page-20261117.html",
      "skiko.wasm",
      "homecore.js",
      "screenshots.png",
      "dashboard.json",
    ])
      expect(isHashed(n), n).toBe(false);
  });
});
