// The design tools' refusals (lor-nde9): scripts/ui-dev.sh and scripts/shots.mjs never
// point at lor-web or the real database. Every case here refuses before anything starts.
import { spawn, spawnSync } from "node:child_process";
import { createServer } from "node:net";
import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";
// @ts-expect-error a plain .mjs script, no types
import { groupsOf, refuseRealData, slug } from "../scripts/shots.mjs";

const base = "http://127.0.0.1:3858";

describe("shots.mjs", () => {
  it("refuses lor-web's ports unless every link is under /lab/", () => {
    expect(refuseRealData("http://127.0.0.1:3847", [{ link: "/tasks" }], {})).toMatch(/real data/);
    expect(refuseRealData("http://127.0.0.1:3848", [{ link: "/lab/a.html" }, { link: "/" }], {})).toMatch(/1 link/);
    expect(refuseRealData("http://127.0.0.1:3848", [{ link: "/lab/a.html" }], {})).toBeNull();
    expect(refuseRealData("http://127.0.0.1:4000", [{ link: "/" }], { LOR_PORT: "4000" })).toMatch(/real data/);
    expect(refuseRealData(base, [{ link: "/" }], {})).toBeNull();
  });

  it("runs shots with another data= or at= apart", () => {
    const shots = [{ link: "/?at=09:00&data=base" }, { link: "/tasks?data=base&at=09:00" }, { link: "/?at=15:00&data=base" }, { link: "/status?data=failing&at=09:00" }];
    expect(groupsOf(shots, base).map((g: unknown[]) => g.length)).toEqual([2, 1, 1]);
  });

  it("names every file uniquely in the dashboard catalogue, with at= and data= on every link", () => {
    const cat = JSON.parse(readFileSync("shots/dashboard.json", "utf8")) as { shots: { name: string; link: string }[] };
    const files = cat.shots.map((s) => slug(s.name));
    expect(new Set(files).size).toBe(files.length);
    for (const s of cat.shots) {
      const q = new URL(s.link, base).searchParams;
      expect([s.name, q.has("at"), q.has("data")]).toEqual([s.name, true, true]);
    }
  });
});

describe("ui-dev.sh", () => {
  // --dry-run stops after the guards, so a guard that regresses never starts Vite on a real database.
  const run = (env: Record<string, string>) =>
    spawnSync("scripts/ui-dev.sh", ["--dry-run"], { env: { ...process.env, LOR_UI_DEV_PORT: "3899", ...env }, encoding: "utf8", timeout: 10_000 });

  it("passes the test database on a free port", () => {
    const r = run({});
    expect([r.status, r.stdout]).toEqual([0, expect.stringContaining("life_on_rails_test")]);
  });

  it("refuses the ports the env file gives lor-web", () => {
    const dir = mkdtempSync(path.join(tmpdir(), "ui-dev-"));
    writeFileSync(path.join(dir, "env"), "# a comment\nLOR_PORT=4555\n");
    const r = run({ LOR_ENV_FILE: path.join(dir, "env"), LOR_UI_DEV_PORT: "4555" });
    expect([r.status, r.stderr]).toEqual([3, expect.stringContaining("lor-web")]);
  });

  it("refuses lor-web's ports", () => {
    for (const port of ["3847", "3848", "3849"]) {
      const r = run({ LOR_UI_DEV_PORT: port });
      expect([r.status, r.stderr]).toEqual([3, expect.stringContaining("lor-web")]);
    }
  });

  it("refuses a port already taken", async () => {
    const server = createServer().listen(0, "127.0.0.1");
    await new Promise((ok) => server.once("listening", ok));
    const port = String((server.address() as { port: number }).port);
    // Async, so this process's listener can still accept while the script looks.
    const r = await new Promise<{ status: number | null; stderr: string }>((ok) => {
      const child = spawn("scripts/ui-dev.sh", ["--dry-run"], { env: { ...process.env, LOR_UI_DEV_PORT: port } });
      let stderr = "";
      child.stderr.on("data", (d) => (stderr += d));
      child.on("close", (status) => ok({ status, stderr }));
    });
    server.close();
    expect([r.status, r.stderr]).toEqual([3, expect.stringContaining("already listens")]);
  });

  it("refuses any database but the test database", () => {
    for (const url of ["postgres:///life_on_rails?host=/var/run/postgresql", "postgres:///life_on_rails_dev", "postgres:///life_on_rails_test?host=/x&dbname=life_on_rails"]) {
      const r = run({ LOR_UI_DEV_DATABASE_URL: url });
      expect([r.status, r.stderr]).toEqual([3, expect.stringContaining("refused")]);
    }
  });
});
