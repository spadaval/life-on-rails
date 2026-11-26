// cli/lor.mjs run by its own path never reaches lor-web (lor-y8lw): on 25 Nov 2026 a worktree's
// `node cli/lor.mjs intent create ...`, meant for the test server, made a goal in the real LoR.
// Nothing here reaches a server: every allowed call is aimed at a port nothing listens on.
import { spawnSync } from "node:child_process";
import { mkdtempSync, symlinkSync, writeFileSync } from "node:fs";
import { createServer } from "node:net";
import { tmpdir } from "node:os";
import path from "node:path";
import { beforeAll, describe, expect, it } from "vitest";

const cli = path.resolve("cli/lor.mjs");
const dir = mkdtempSync(path.join(tmpdir(), "lor-cli-"));
const envFile = path.join(dir, "env");
const installed = path.join(dir, "lor");
let closed = "";

/** Run the CLI with only the given LOR_ variables, and an env file that names lor-web on 4000. */
const run = (entry: string, env: Record<string, string>, args = ["intent", "create", "--type", "goal", "--title", "x", "--thread", "t"]) => {
  const clean = Object.fromEntries(Object.entries(process.env).filter(([k]) => !k.startsWith("LOR_")));
  const r = spawnSync(process.execPath, [entry, ...args], { encoding: "utf8", env: { ...clean, LOR_ENV_FILE: envFile, ...env } });
  return { code: r.status, err: r.stderr };
};

beforeAll(async () => {
  writeFileSync(envFile, "LOR_AGENT_TOKEN=t\nLOR_PORT=4000\n");
  symlinkSync(cli, installed);
  // A port that was free a moment ago: a call allowed through fails with "cannot reach" (8).
  closed = await new Promise<string>((resolve) => {
    const s = createServer().listen(0, "127.0.0.1", () => {
      const { port } = s.address() as { port: number };
      s.close(() => resolve(String(port)));
    });
  });
});

describe("cli/lor.mjs run as a file", () => {
  it("has no default server, for writes and reads", () => {
    expect(run(cli, {})).toMatchObject({ code: 6, err: expect.stringMatching(/no default server.*LOR_PORT=3857/) });
    expect(run(cli, {}, ["status"]).code).toBe(6);
  });

  it("refuses lor-web's ports even when named", () => {
    const named: Record<string, string>[] = [{ LOR_PORT: "3847" }, { LOR_PORT: "4000" }, { LOR_URL: "http://localhost:3848" }];
    for (const env of named) {
      expect(run(cli, env)).toMatchObject({ code: 6, err: expect.stringMatching(/is lor-web/) });
    }
  });

  it("goes to a server the environment names", () => {
    expect(run(cli, { LOR_PORT: closed })).toMatchObject({ code: 8, err: expect.stringContaining(`cannot reach http://127.0.0.1:${closed}`) });
  });

  it("still prints help", () => {
    expect(run(cli, {}, ["help"]).code).toBe(0);
  });
});

describe("the installed lor, a symlink", () => {
  it("takes the env file's server by default", () => {
    expect(run(installed, {})).toMatchObject({ code: 8, err: expect.stringContaining("cannot reach http://127.0.0.1:4000") });
  });
});
