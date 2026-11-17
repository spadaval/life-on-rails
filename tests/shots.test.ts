// The design tools' refusals (lor-nde9): scripts/ui-dev.sh and scripts/shots.mjs never
// point at lor-web or the real database. Every case here refuses before anything starts.
import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";
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
  const run = (env: Record<string, string>) => spawnSync("scripts/ui-dev.sh", { env: { ...process.env, ...env }, encoding: "utf8" });

  it("refuses lor-web's ports", () => {
    for (const port of ["3847", "3848", "3849"]) {
      const r = run({ LOR_UI_DEV_PORT: port });
      expect([r.status, r.stderr]).toEqual([3, expect.stringContaining("lor-web")]);
    }
  });

  it("refuses any database but the test database", () => {
    for (const url of ["postgres:///life_on_rails?host=/var/run/postgresql", "postgres:///life_on_rails_dev", "postgres:///life_on_rails_test?host=/x&dbname=life_on_rails"]) {
      const r = run({ LOR_UI_DEV_DATABASE_URL: url });
      expect([r.status, r.stderr]).toEqual([3, expect.stringContaining("refused")]);
    }
  });
});
