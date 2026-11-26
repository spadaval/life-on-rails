// cli/lor.mjs run by its own path never reaches lor-web (lor-y8lw): on 25 Nov 2026 a worktree's
// `node cli/lor.mjs intent create ...`, meant for the test server, made a goal in the real LoR.
// Nothing here reaches a server: each child runs with fetch replaced by one that reports where the
// call would have gone and answers it. scripts/ui-check.mjs is only run where it refuses.
import { spawnSync } from "node:child_process";
import { mkdtempSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";

const cli = path.resolve("cli/lor.mjs");
const dir = mkdtempSync(path.join(tmpdir(), "lor-cli-"));
const envFile = path.join(dir, "env");
const installed = path.join(dir, "lor");
const noNetwork = path.join(dir, "no-network.mjs");
// lor-web on ports of its own, so a refusal is the env file's doing and not the built-in list's.
// One is written with a leading zero and one is http's default, which a URL spells differently.
writeFileSync(envFile, "LOR_AGENT_TOKEN=t\nLOR_PORT=04000\nLOR_SERVE_PORT=80\nLOR_TRMNL_PORT=4002\n");
writeFileSync(noNetwork, 'globalThis.fetch = async (url, init) => { process.stderr.write(`FETCH ${init?.method ?? "GET"} ${url}\\n`); return new Response("{}"); };\n');
symlinkSync(cli, installed);

const WRITE = ["intent", "create", "--type", "goal", "--title", "x", "--thread", "t"];
/** Run a script with only the given LOR_ variables, the fake env file, and no network. */
const run = (entry: string, env: Record<string, string>, args = WRITE) => {
  const clean = Object.fromEntries(Object.entries(process.env).filter(([k]) => !k.startsWith("LOR_")));
  const r = spawnSync(process.execPath, ["--import", noNetwork, entry, ...args], { encoding: "utf8", env: { ...clean, LOR_ENV_FILE: envFile, ...env } });
  return { code: r.status, err: r.stderr };
};

describe("cli/lor.mjs run as a file", () => {
  it("has no default server, for writes and reads", () => {
    for (const args of [WRITE, ["status"]]) {
      const r = run(cli, {}, args);
      expect(r).toMatchObject({ code: 6, err: expect.stringMatching(/no default server.*LOR_PORT=3857/) });
      expect(r.err).not.toContain("FETCH");
    }
  });

  it("refuses lor-web's ports even when named, however the host is spelled", () => {
    const named: Record<string, string>[] = [
      { LOR_PORT: "3847" },
      { LOR_PORT: "4000" },
      { LOR_URL: "http://localhost:3848" },
      { LOR_URL: "http://localhost.:4000" },
      { LOR_URL: "http://[::ffff:127.0.0.1]:3847" },
      { LOR_URL: "http://[::1]:80" },
      { LOR_URL: "http://127.0.0.1" },
      { LOR_PORT: "04000" },
      { LOR_URL: "http://box.tailnet.example:3848" },
    ];
    for (const env of named) {
      const r = run(cli, env);
      expect([env, r.code]).toEqual([env, 6]);
      expect(r.err).toMatch(/port of lor-web/);
      expect(r.err).not.toContain("FETCH");
    }
  });

  it("goes to a server the environment names", () => {
    expect(run(cli, { LOR_PORT: "3857" })).toMatchObject({ code: 0, err: "FETCH POST http://127.0.0.1:3857/api/v1/intents\n" });
    expect(run(cli, { LOR_URL: "http://127.0.0.1:3999" }, ["status"])).toMatchObject({ code: 0, err: "FETCH GET http://127.0.0.1:3999/api/v1/status\n" });
  });

  it("still prints help", () => {
    expect(run(cli, {}, ["help"])).toMatchObject({ code: 0, err: "" });
  });
});

describe("the installed lor, a symlink", () => {
  it("takes the env file's server by default", () => {
    expect(run(installed, {})).toMatchObject({ code: 0, err: "FETCH POST http://127.0.0.1:04000/api/v1/intents\n" });
  });
});

describe("scripts/ui-check.mjs", () => {
  it("refuses a base on lor-web's ports, built in or from the env file, before a browser starts", () => {
    for (const at of [":3847", ":3848", ":3849", ":4000", ":80", "", ":4002"]) {
      const r = run("scripts/ui-check.mjs", { LOR_UI_BASE: `http://localhost.${at}` }, ["tasks", path.join(dir, "out")]);
      expect([at, r.code]).toEqual([at, 3]);
      expect(r.err).toMatch(/port of lor-web/);
    }
  });
});
