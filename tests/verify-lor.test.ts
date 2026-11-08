// The verify-lor CLI refuses every target that is not a test one. Each case runs the
// CLI against a fake env file and a fake adb, so nothing here can reach the real
// database, lor-web or a phone. Every case passes --dry-run, so even a broken guard
// prints a plan instead of acting; only the listener case reaches a server, its own.
import { spawn, spawnSync, type ChildProcess } from "node:child_process";
import { mkdirSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { createServer, type Server } from "node:http";
import type { AddressInfo } from "node:net";
import { tmpdir } from "node:os";
import path from "node:path";
import { afterAll, beforeAll, describe, expect, it } from "vitest";

const ROOT = path.resolve(import.meta.dirname, "..");
const CLI = path.join(ROOT, ".claude/skills/verify-lor/verify.mjs");
const REAL_URL = "postgres:///life_on_rails?host=/var/run/postgresql";
const dir = mkdtempSync(path.join(tmpdir(), "verify-lor-test-"));
const envFile = path.join(dir, "env");
writeFileSync(envFile, [`DATABASE_URL=${REAL_URL}`, "LOR_PORT=3847", "LOR_SERVE_PORT=3848", "LOR_TRMNL_PORT=4999", "LOR_AGENT_TOKEN=x"].join("\n") + "\n");

function cli(args: string[], env: Record<string, string> = {}) {
  const r = spawnSync("node", [CLI, ...args], {
    encoding: "utf8",
    env: { ...process.env, LOR_ENV_FILE: envFile, LOR_VERIFY_ADB: "/bin/false", LOR_VERIFY_COPY: path.join(dir, "copy"), LOR_VERIFY_OUT: path.join(dir, "out"), ...env },
  });
  return { code: r.status, out: r.stdout, err: r.stderr };
}

function refused(r: ReturnType<typeof cli>, why: RegExp) {
  expect(r.err).toMatch(/refused/);
  expect(r.err).toMatch(why);
  expect(r.err).toMatch(/Instead:/);
  expect(r.code).toBe(3);
}

describe("verify-lor refuses the real database", () => {
  it("seed refuses the env file's DATABASE_URL", () => refused(cli(["seed", "admin", "--dry-run"], { LOR_VERIFY_DATABASE_URL: REAL_URL }), /real database/));
  it("seed refuses any database other than life_on_rails_test", () =>
    refused(cli(["seed", "admin", "--dry-run"], { LOR_VERIFY_DATABASE_URL: "postgres:///life_on_rails_dev?host=/var/run/postgresql" }), /not the test database/));
  it("api refuses the env file's DATABASE_URL", () => refused(cli(["api", "GET", "/status", "--dry-run"], { LOR_VERIFY_DATABASE_URL: REAL_URL }), /real database/));
  it("up refuses the env file's DATABASE_URL", () => refused(cli(["up", "--dry-run"], { LOR_VERIFY_DATABASE_URL: REAL_URL }), /real database/));
  it("seed refuses a dbname parameter that overrides the path", () =>
    refused(cli(["seed", "admin", "--dry-run"], { LOR_VERIFY_DATABASE_URL: "postgres:///life_on_rails_test?host=/var/run/postgresql&dbname=other" }), /can name another database/));
  it("doctor refuses a dbname parameter before it runs psql", () => {
    const r = cli(["doctor"], { LOR_VERIFY_DATABASE_URL: "postgres:///life_on_rails_test?host=/var/run/postgresql&dbname=other"});
    expect(r.out + r.err).toMatch(/can name another database/);
    expect(r.out + r.err).not.toMatch(/psql/);
  });
});

describe("verify-lor refuses lor-web's ports", () => {
  for (const port of ["3847", "3848", "3849"]) {
    it(`api refuses port ${port}`, () => refused(cli(["api", "GET", "/status", "--port", port, "--dry-run"]), /running lor-web/));
  }
  it("api refuses a port the env file gives lor-web", () => refused(cli(["api", "GET", "/status", "--port", "4999", "--dry-run"]), /running lor-web/));
  it("screen refuses port 3848", () => refused(cli(["screen", "tasks", path.join(dir, "shots"), "--port", "3848", "--dry-run"]), /running lor-web/));
  it("install refuses to point the app at port 3847", () => refused(cli(["install", "--port", "3847", "--dry-run"]), /running lor-web/));
});

describe("verify-lor refuses a server that is not a throwaway one", () => {
  let server: Server;
  let port = 0;
  beforeAll(async () => {
    server = createServer((_, res) => res.end("{}"));
    await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
    port = (server.address() as AddressInfo).port;
  });
  afterAll(() => server.close());
  it("api refuses a port held by another process", () => refused(cli(["api", "GET", "/status", "--port", String(port)]), /not a throwaway LoR server/));
});

describe("verify-lor keeps builds and evidence out of the checkout", () => {
  it("up refuses a copy dir inside the checkout", () => refused(cli(["up", "--dry-run"], { LOR_VERIFY_COPY: path.join(ROOT, "build-copy") }), /overlaps the checkout/));
  it("up refuses a copy dir that contains the checkout", () => refused(cli(["up", "--dry-run"], { LOR_VERIFY_COPY: path.dirname(ROOT) }), /overlaps the checkout/));
  it("screen refuses evidence inside the copy, which down deletes", () => refused(cli(["screen", "tasks", path.join(dir, "copy/shots"), "--dry-run"]), /down deletes/));
});

describe("verify-lor never touches a phone or the app's data", () => {
  it("install refuses a physical device serial", () => refused(cli(["install", "--serial", "adb-57211JEBF04781-YipJ96._adb-tls-connect._tcp", "--dry-run"]), /not an emulator/));
  it("install keeps app data and points the app at the test server", () => {
    const r = cli(["install", "--dry-run"]);
    expect(r.code).toBe(0);
    expect(r.out).toContain("install -r");
    expect(r.out).toContain("server_url=http://10.0.2.2:3857");
    const cmds = JSON.parse(r.out).steps.map((s: { cmd: string }) => s.cmd).join("\n");
    expect(cmds).not.toMatch(/pm clear|install -r -c|uninstall/);
  });
});

describe("verify-lor allows the test targets", () => {
  it("seed plans a load into life_on_rails_test", () => {
    const r = cli(["seed", "admin", "--dry-run"]);
    expect(r.code).toBe(0);
    expect(JSON.parse(r.out).steps[0].env.DATABASE_URL).toMatch(/\/life_on_rails_test\?/);
  });
  it("api plans a call to 127.0.0.1:3857", () => {
    const r = cli(["api", "GET", "/status", "--dry-run"]);
    expect(r.code).toBe(0);
    expect(JSON.parse(r.out).steps[0].url).toBe("http://127.0.0.1:3857/api/v1/status");
  });
});

describe("verify-lor keeps a shell's LOR_URL away from the flows", () => {
  // A stand-in throwaway server (a scripts/serve.mjs on the test database) and a stand-in
  // copy whose ui-check.mjs only reports the LOR_URL it was given.
  const copy = path.join(dir, "fake-copy");
  let server: ChildProcess;
  let port = 0;
  beforeAll(async () => {
    const probe = createServer();
    await new Promise<void>((resolve) => probe.listen(0, "127.0.0.1", resolve));
    port = (probe.address() as AddressInfo).port;
    await new Promise<void>((resolve) => probe.close(() => resolve()));
    mkdirSync(path.join(dir, "fake-server/scripts"), { recursive: true });
    writeFileSync(path.join(dir, "fake-server/scripts/serve.mjs"), `import { createServer } from "node:http"; createServer((_, r) => r.end("{}")).listen(Number(process.env.LOR_PORT), "127.0.0.1", () => console.log("up"));`);
    mkdirSync(path.join(copy, "scripts"), { recursive: true });
    writeFileSync(path.join(copy, "scripts/ui-check.mjs"), `console.log("LOR_URL=" + (process.env.LOR_URL ?? ""));`);
    server = spawn("node", [path.join(dir, "fake-server/scripts/serve.mjs")], {
      env: { ...process.env, LOR_PORT: String(port), DATABASE_URL: "postgres:///life_on_rails_test?host=/var/run/postgresql" },
      stdio: ["ignore", "pipe", "inherit"],
    });
    await new Promise<void>((resolve) => server.stdout!.once("data", () => resolve()));
  });
  afterAll(() => server.kill());
  it("screen does not pass a shell LOR_URL to the flow", () => {
    const out = path.join(dir, "shots-lor-url");
    const r = cli(["screen", "tasks", out, "--port", String(port)], { LOR_VERIFY_COPY: copy, LOR_URL: "http://127.0.0.1:3847" });
    const log = readFileSync(path.join(out, "tasks.log"), "utf8");
    expect(log).toMatch(/^LOR_URL=$/m);
    expect(log).not.toMatch(/3847/);
    expect(r.code).not.toBe(3);
  });
});
