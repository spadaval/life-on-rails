#!/usr/bin/env node
// verify-lor: drive Life on Rails on throwaway targets and collect evidence.
// Every command targets the test database (life_on_rails_test), the throwaway
// server on 127.0.0.1:3857 built in a copy of the checkout, and the
// T3_Phone_API35 emulator. Nothing here reaches the real database, lor-web
// (3847, 3848, 3849) or a physical phone. See SKILL.md next to this file.
//
// stdout is JSON; diagnostics go to stderr.
// Exit codes: 0 ok, 1 failed, 2 usage, 3 refused (the target was not a test one).
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readdirSync, readFileSync, readlinkSync, realpathSync, statSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const HELP = `verify-lor <command> [options]      (node .claude/skills/verify-lor/verify.mjs)

  doctor                       what is ready per surface (api, web, launcher), and the fix for what is not
  up [--dry-run]               copy the checkout to the copy dir, build there, migrate the test database,
                               and start the throwaway server on :3857
  down [--keep-copy] [--dry-run]   stop the throwaway server and delete the copy (never the evidence)
  seed <name> [--dry-run]      load seeds/<name>.json into the test database
  api <METHOD> <path> [--body '<json>'|-] [--out <file>] [--port N] [--dry-run]
                               call /api/v1<path> on the throwaway server; prints {status, body}
  screen <flow|/path> <out-dir> [--port N] [--dry-run]
                               a flow of scripts/ui-check.mjs (tasks, goals, people, notes, today),
                               or one screenshot of a path such as /goals
  build [--dry-run]            build the debug APK on boat (android/boat-build.sh), never Gradle here
  install [--apk <file>] [--serial emulator-N] [--port N] [--dry-run]
                               install the debug APK on the emulator (adb install -r, data kept) and
                               point the app at http://10.0.2.2:3857
  logs <out-dir>               the app's logcat, the crash buffer and the server log
  evidence <dir>               list the files in an evidence directory with sizes and sha256

Environment (all optional): LOR_ENV_FILE, LOR_VERIFY_DATABASE_URL, LOR_VERIFY_COPY, LOR_VERIFY_OUT,
LOR_VERIFY_AVD, LOR_VERIFY_ADB.
Exit codes: 0 ok, 1 failed, 2 usage, 3 refused.
`;

// ---------- configuration ----------

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");
const NAME = path.basename(ROOT);
const ENV_FILE = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");
const TEST_DB = "life_on_rails_test";
const TEST_DB_URL = process.env.LOR_VERIFY_DATABASE_URL ?? `postgres:///${TEST_DB}?host=/var/run/postgresql`;
const TEST_PORT = 3857;
const REAL_PORTS = [3847, 3848, 3849];
const COPY = path.resolve(process.env.LOR_VERIFY_COPY ?? `/tmp/lor-ui-${NAME}`);
const OUT = path.resolve(process.env.LOR_VERIFY_OUT ?? `/tmp/lor-verify/${NAME}`);
const APK = path.join(OUT, "apk/outputs/apk/debug/app-debug.apk");
const AVD = process.env.LOR_VERIFY_AVD ?? "T3_Phone_API35";
const ADB = process.env.LOR_VERIFY_ADB ?? path.join(homedir(), "Android/Sdk/platform-tools/adb");
const APP = "com.spadaval.lor";
const SERVER_LOG = "/tmp/lor-ui-server.log"; // written by scripts/ui-server.sh
const FLOWS = ["tasks", "goals", "people", "notes", "today"];

class Usage extends Error {}
class Refused extends Error {}
class Failed extends Error {}

function readEnvFile() {
  const out = {};
  if (!existsSync(ENV_FILE)) return out;
  for (const line of readFileSync(ENV_FILE, "utf8").split("\n")) {
    const m = /^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/.exec(line);
    if (m && !line.trim().startsWith("#")) out[m[1]] = m[2].replace(/^(['"])(.*)\1$/, "$2");
  }
  return out;
}
const ENV = readEnvFile();
const REAL_DB_URL = ENV.DATABASE_URL || "postgres:///life_on_rails?host=/var/run/postgresql";

function dbOf(url) {
  try {
    const u = new URL(url);
    return {
      database: decodeURIComponent(u.pathname.replace(/^\//, "")) || "life_on_rails",
      host: u.hostname || u.searchParams.get("host") || "/var/run/postgresql",
    };
  } catch {
    throw new Refused(`${url} is not a database URL. Instead: leave LOR_VERIFY_DATABASE_URL unset to use ${TEST_DB}.`);
  }
}

// ---------- guards ----------

function guardDb(url = TEST_DB_URL) {
  const db = dbOf(url);
  const real = dbOf(REAL_DB_URL);
  if (db.database === real.database && db.host === real.host) {
    throw new Refused(`${url} is the real database (DATABASE_URL in ${ENV_FILE}). Instead: leave LOR_VERIFY_DATABASE_URL unset to use ${TEST_DB}.`);
  }
  if (db.database !== TEST_DB) {
    throw new Refused(`${url} is not the test database ${TEST_DB}. Instead: leave LOR_VERIFY_DATABASE_URL unset.`);
  }
  return url;
}

function guardPort(port) {
  const real = new Set([...REAL_PORTS, ...["LOR_PORT", "LOR_SERVE_PORT", "LOR_TRMNL_PORT"].map((k) => Number(ENV[k])).filter(Boolean)]);
  if (!Number.isInteger(port) || port <= 0) throw new Usage(`--port wants a number, got ${port}`);
  if (real.has(port)) {
    throw new Refused(`port ${port} is the running lor-web (real ports: ${[...real].sort().join(", ")}). Instead: use the throwaway server on ${TEST_PORT} (verify-lor up).`);
  }
  return port;
}

function inside(child, parent) {
  const rel = path.relative(parent, child);
  return rel === "" || (!rel.startsWith("..") && !path.isAbsolute(rel));
}
const real = (p) => (existsSync(p) ? realpathSync(p) : path.resolve(p));

function guardCopy() {
  const copy = real(COPY);
  const root = real(ROOT);
  if (inside(copy, root) || inside(root, copy) || copy === real(homedir())) {
    throw new Refused(`the copy dir ${COPY} overlaps the checkout ${ROOT}; building there would replace the dist/ that lor-web serves. Instead: set LOR_VERIFY_COPY to a fresh directory under /tmp.`);
  }
  if (existsSync(path.join(copy, ".git"))) {
    throw new Refused(`${COPY} holds a git checkout. Instead: set LOR_VERIFY_COPY to a fresh directory under /tmp.`);
  }
  return copy;
}

function guardEvidenceDir(dir) {
  const d = real(dir);
  if (inside(d, real(COPY))) throw new Refused(`${dir} is inside the copy ${COPY}, which down deletes. Instead: write evidence under ${OUT}.`);
  return d;
}

/** The process listening on 127.0.0.1:<port>, if any. */
function listener(port) {
  const r = run("ss", ["-ltnpH", `sport = :${port}`], { quiet: true });
  const m = /pid=(\d+)/.exec(r.stdout);
  if (r.stdout.trim() === "") return null;
  return { pid: m ? Number(m[1]) : null, line: r.stdout.trim() };
}

function procEnv(pid) {
  try {
    return Object.fromEntries(
      readFileSync(`/proc/${pid}/environ`, "utf8").split("\0").filter(Boolean).map((kv) => [kv.slice(0, kv.indexOf("=")), kv.slice(kv.indexOf("=") + 1)]),
    );
  } catch {
    return null;
  }
}

/** A throwaway server: scripts/serve.mjs started with this port and the test database. */
function isTestServer(pid, port) {
  const env = procEnv(pid);
  if (!env) return false;
  let cmd = "";
  try {
    cmd = readFileSync(`/proc/${pid}/cmdline`, "utf8");
  } catch {}
  if (!cmd.includes("scripts/serve.mjs") || env.LOR_PORT !== String(port) || !env.DATABASE_URL) return false;
  try {
    return dbOf(env.DATABASE_URL).database === TEST_DB;
  } catch {
    return false;
  }
}

function guardListener(port) {
  const l = listener(port);
  if (!l) throw new Failed(`nothing listens on 127.0.0.1:${port}. Instead: start the throwaway server with verify-lor up.`);
  if (!l.pid || !isTestServer(l.pid, port)) {
    throw new Refused(`port ${port} is held by something that is not a throwaway LoR server on ${TEST_DB} (${l.line}). Instead: stop it, then verify-lor up.`);
  }
  return l.pid;
}

function testServerPids(port = TEST_PORT) {
  const r = run("pgrep", ["-f", "scripts/serve.mjs"], { quiet: true });
  return r.stdout.split("\n").filter(Boolean).map(Number).filter((pid) => procEnv(pid)?.LOR_PORT === String(port));
}

// ---------- helpers ----------

function run(cmd, args, { cwd = ROOT, env = {}, input, quiet = false, inherit = false } = {}) {
  // A second line behind guardDb: no child starts with a database other than the test one,
  // and none inherits a DATABASE_URL from the caller's shell.
  const { DATABASE_URL: _inherited, ...base } = process.env;
  if (env.DATABASE_URL !== undefined && dbOf(env.DATABASE_URL).database !== TEST_DB) {
    throw new Refused(`refusing to start ${cmd} with DATABASE_URL=${env.DATABASE_URL}. Instead: use ${TEST_DB}.`);
  }
  const r = spawnSync(cmd, args, { cwd, env: { ...base, ...env }, input, encoding: "utf8", stdio: inherit ? ["ignore", 2, 2] : "pipe", maxBuffer: 64 << 20 });
  if (r.error && !quiet) throw new Failed(`${cmd}: ${r.error.message}`);
  return { status: r.status ?? (r.error ? 127 : 1), stdout: r.stdout ?? "", stderr: r.stderr ?? "" };
}

function must(cmd, args, opts = {}) {
  const r = run(cmd, args, opts);
  if (r.status !== 0) throw new Failed(`${cmd} ${args.join(" ")} exited ${r.status}${r.stderr ? `: ${r.stderr.trim().slice(-800)}` : ""}`);
  return r;
}

function print(obj) {
  process.stdout.write(JSON.stringify(obj, null, 2) + "\n");
}

function plan(steps) {
  print({ dry_run: true, steps });
  return 0;
}

function sha256(file) {
  return createHash("sha256").update(readFileSync(file)).digest("hex");
}

function head() {
  const sha = run("git", ["rev-parse", "--short", "HEAD"], { quiet: true }).stdout.trim();
  const dirty = run("git", ["status", "--porcelain"], { quiet: true }).stdout.split("\n").filter(Boolean).length;
  return { commit: sha, dirty_files: dirty };
}

function portFlag(flags) {
  return guardPort(flags.port === undefined ? TEST_PORT : Number(flags.port));
}

// ---------- adb (emulator only) ----------

function adb(serial, args, opts) {
  return run(ADB, ["-s", serial, ...args], { quiet: true, ...opts });
}

function emulators() {
  const r = run(ADB, ["devices"], { quiet: true });
  if (r.status !== 0) return { error: r.stderr.trim() || `cannot run ${ADB}`, emulators: [], physical: 0 };
  const lines = r.stdout.split("\n").slice(1).filter((l) => /\tdevice$/.test(l)).map((l) => l.split("\t")[0]);
  const emus = lines.filter((s) => /^emulator-\d+$/.test(s)).map((serial) => ({ serial, avd: adb(serial, ["emu", "avd", "name"]).stdout.split(/\r?\n/)[0].trim() }));
  return { emulators: emus, physical: lines.length - emus.length };
}

function pickEmulator(flags) {
  if (flags.serial !== undefined && !/^emulator-\d+$/.test(String(flags.serial))) {
    throw new Refused(`${flags.serial} is not an emulator; physical phones are never installed to or driven. Instead: use the ${AVD} emulator (omit --serial).`);
  }
  const { emulators: emus, error } = emulators();
  if (error) throw new Failed(`adb: ${error}. Instead: check ${ADB} exists (Android SDK platform-tools).`);
  const match = emus.find((e) => e.avd === AVD && (flags.serial === undefined || e.serial === flags.serial));
  if (!match) {
    throw new Failed(`the ${AVD} emulator is not running${emus.length ? ` (running: ${emus.map((e) => `${e.serial}=${e.avd}`).join(", ")})` : ""}. Instead: sg kvm -c start-t3-emulator, or call T3 Code's device_open (hostId local, id ${AVD}).`);
  }
  return match.serial;
}

function readServerUrl(serial) {
  const r = adb(serial, ["exec-out", "run-as", APP, "cat", "shared_prefs/lor.xml"]);
  if (r.status !== 0) return { installed: !/unknown package|is unknown|not debuggable/.test(r.stdout + r.stderr), url: null, xml: null };
  const m = /<string name="server_url">([^<]*)<\/string>/.exec(r.stdout);
  return { installed: true, url: m ? m[1] : null, xml: r.stdout };
}

function withServerUrl(xml, url) {
  const line = `<string name="server_url">${url}</string>`;
  if (!xml || !xml.includes("<map")) return `<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    ${line}\n</map>\n`;
  if (/<string name="server_url">[^<]*<\/string>/.test(xml)) return xml.replace(/<string name="server_url">[^<]*<\/string>/, line);
  if (/<map\s*\/>/.test(xml)) return xml.replace(/<map\s*\/>/, `<map>\n    ${line}\n</map>`);
  return xml.replace("</map>", `    ${line}\n</map>`);
}

/** Write server_url into the app's SharedPreferences. The app caches its prefs, so it is stopped before and after. */
function setServerUrl(serial, url) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    adb(serial, ["shell", "am", "force-stop", APP]);
    const xml = withServerUrl(readServerUrl(serial).xml, url);
    const w = adb(serial, ["exec-in", "run-as", APP, "sh", "-c", "'mkdir -p shared_prefs && cat > shared_prefs/lor.xml'"], { input: xml });
    if (w.status !== 0) throw new Failed(`could not write the app's prefs: ${w.stderr.trim()}. Instead: check the APK is a debug build (run-as needs one).`);
    // A home app restarts at once after a stop and may have rewritten the file from memory.
    adb(serial, ["shell", "am", "force-stop", APP]);
    spawnSync("sleep", ["2"]);
    if (readServerUrl(serial).url === url) return attempt;
  }
  throw new Failed(`the app kept overwriting server_url. Instead: open LoR > Settings on the emulator and set the server to ${url}.`);
}

// ---------- commands ----------

const commands = {
  doctor() {
    const checks = [];
    const add = (name, surfaces, ok, detail, fix) => checks.push({ name, surfaces, ok, detail, ...(ok ? {} : { fix }) });

    add("checkout", ["api", "web", "launcher"], true, { root: ROOT, ...head() });
    const nm = path.join(ROOT, "node_modules");
    add("node_modules", ["api", "web"], existsSync(nm), nm, `pnpm install in ${ROOT}, or ln -s ~/projects/life-on-rails/node_modules ${nm} in a worktree`);

    try {
      guardDb();
      const r = run("psql", [TEST_DB_URL, "-Atc", "SELECT created_at FROM drizzle.__drizzle_migrations"], { quiet: true });
      if (r.status !== 0) {
        add("test database", ["api", "web", "launcher"], false, r.stderr.trim() || "psql failed", `createdb ${TEST_DB}, then verify-lor up (it migrates)`);
      } else {
        const applied = new Set(r.stdout.split("\n").filter(Boolean));
        const journal = JSON.parse(readFileSync(path.join(ROOT, "drizzle/meta/_journal.json"), "utf8")).entries;
        const pending = journal.filter((e) => !applied.has(String(e.when))).map((e) => e.tag);
        const extra = applied.size - (journal.length - pending.length);
        add("test database", ["api", "web", "launcher"], true, { url: TEST_DB_URL, pending_migrations: pending, migrations_from_other_branches: extra });
      }
    } catch (err) {
      add("test database", ["api", "web", "launcher"], false, err.message, "leave LOR_VERIFY_DATABASE_URL unset");
    }

    const l = listener(TEST_PORT);
    if (!l) add(`port ${TEST_PORT}`, ["api", "web", "launcher"], false, "free: no throwaway server running", "verify-lor up");
    else if (l.pid && isTestServer(l.pid, TEST_PORT)) {
      let cwd = null;
      try {
        cwd = readlinkSync(`/proc/${l.pid}/cwd`);
      } catch {}
      add(`port ${TEST_PORT}`, ["api", "web", "launcher"], true, { server: "throwaway, test database", pid: l.pid, serving_from: cwd, log: SERVER_LOG });
    } else add(`port ${TEST_PORT}`, ["api", "web", "launcher"], false, `held by another process: ${l.line}`, "stop that process, then verify-lor up");

    add("lor-web ports", [], true, { ports: REAL_PORTS.map((p) => ({ port: p, listening: Boolean(listener(p)) })), note: "the real service; no command touches these" });
    add("copy", ["web"], existsSync(path.join(COPY, "dist/server/server.js")), COPY, "verify-lor up (builds the copy)");
    const chrome = ["/usr/bin/google-chrome", "/opt/google/chrome/chrome"].find((p) => existsSync(p));
    add("chrome", ["web"], Boolean(chrome), chrome ?? "not found", "install Google Chrome (playwright-core drives the system Chrome)");

    const devs = emulators();
    const emu = devs.emulators.find((e) => e.avd === AVD);
    add("emulator", ["launcher"], Boolean(emu), devs.error ?? { wanted: AVD, running: devs.emulators, physical_devices_connected: devs.physical }, `sg kvm -c start-t3-emulator, or T3 Code's device_open (hostId local, id ${AVD})`);
    if (emu) {
      const s = readServerUrl(emu.serial);
      const want = `http://10.0.2.2:${TEST_PORT}`;
      add("app on emulator", ["launcher"], s.installed && s.url === want, { serial: emu.serial, installed: s.installed, server_url: s.url ?? "(unset: the app uses the real server)" }, "verify-lor install");
    }
    add("debug apk", ["launcher"], existsSync(APK), APK, "verify-lor build");
    const boat = run("which", ["boat"], { quiet: true }).stdout.trim();
    add("boat", ["launcher"], Boolean(boat), boat || "not found", "install the boat CLI (see the boat skill); Gradle never runs on this machine");

    const surfaces = Object.fromEntries(["api", "web", "launcher"].map((s) => [s, checks.filter((c) => c.surfaces.includes(s)).every((c) => c.ok)]));
    print({ ok: Object.values(surfaces).every(Boolean), surfaces, checks });
    return Object.values(surfaces).every(Boolean) ? 0 : 1;
  },

  up(_, flags) {
    guardDb();
    guardPort(TEST_PORT);
    const copy = guardCopy();
    const modules = path.join(ROOT, "node_modules");
    if (!existsSync(modules)) throw new Failed(`${modules} is missing. Instead: pnpm install in ${ROOT}, or symlink the main checkout's node_modules there.`);
    const env = { DATABASE_URL: TEST_DB_URL };
    const steps = [
      ["rsync", "-a", "--delete", "--exclude=/node_modules", "--exclude=/dist", "--exclude=.git", "--exclude=/.atelier/runtime", "--exclude=/android/.gradle", "--exclude=/android/app/build", `${ROOT}/`, `${copy}/`],
      ["ln", "-sfn", realpathSync(modules), path.join(copy, "node_modules")],
      ["node_modules/.bin/vite", "build"],
      ["node_modules/.bin/tsx", "src/db/migrate.ts"],
      ["bash", "scripts/ui-server.sh"],
    ];
    if (flags["dry-run"]) return plan(steps.map((s) => ({ cwd: s[0] === "rsync" ? ROOT : copy, env: s[1] === "src/db/migrate.ts" ? env : undefined, cmd: s.join(" ") })));
    mkdirSync(copy, { recursive: true });
    mkdirSync(OUT, { recursive: true });
    const upLog = path.join(OUT, "up.log");
    writeFileSync(upLog, "");
    for (const [cmd, ...args] of steps) {
      process.stderr.write(`verify-lor: ${cmd} ${args.join(" ")}\n`);
      const r = run(cmd, args, { cwd: cmd === "rsync" ? ROOT : copy, env: args[0] === "src/db/migrate.ts" ? env : undefined });
      writeFileSync(upLog, `$ ${cmd} ${args.join(" ")}\n${r.stdout}${r.stderr}\n`, { flag: "a" });
      if (r.status !== 0) throw new Failed(`${cmd} ${args.join(" ")} exited ${r.status}; see ${upLog}. Last lines:\n${(r.stdout + r.stderr).trim().split("\n").slice(-15).join("\n")}`);
    }
    const l = listener(TEST_PORT);
    if (!l?.pid || !isTestServer(l.pid, TEST_PORT)) throw new Failed(`the server did not come up as a test server; see ${SERVER_LOG}`);
    // It must listen on the test port alone: no owner listener, no wall display listener.
    const others = run("ss", ["-ltnpH"], { quiet: true }).stdout.split("\n").filter((x) => x.includes(`pid=${l.pid},`) && !x.includes(`:${TEST_PORT} `));
    if (others.length) {
      process.kill(l.pid);
      throw new Failed(`the test server opened other listeners and was stopped: ${others.join("; ")}`);
    }
    print({ ok: true, url: `http://127.0.0.1:${TEST_PORT}`, emulator_url: `http://10.0.2.2:${TEST_PORT}`, pid: l.pid, copy, database: TEST_DB_URL, log: SERVER_LOG, build_log: upLog, ...head() });
    return 0;
  },

  down(_, flags) {
    const pids = testServerPids();
    const removeCopy = !flags["keep-copy"] && existsSync(COPY);
    if (removeCopy) guardCopy();
    if (flags["dry-run"]) return plan([...pids.map((p) => ({ cmd: `kill ${p}` })), ...(removeCopy ? [{ cmd: `rm -rf ${COPY}` }] : []), { note: `evidence under ${OUT} is kept` }]);
    for (const pid of pids) process.kill(pid);
    if (removeCopy) must("rm", ["-rf", COPY]);
    for (let i = 0; i < 20 && listener(TEST_PORT); i++) spawnSync("sleep", ["0.25"]);
    print({ ok: !listener(TEST_PORT), stopped: pids, removed_copy: removeCopy ? COPY : null, evidence_kept: OUT });
    return listener(TEST_PORT) ? 1 : 0;
  },

  seed([name], flags) {
    if (!name) throw new Usage(`seed <name>; have: ${readdirSync(path.join(ROOT, "seeds")).map((f) => f.replace(/\.json$/, "")).join(", ")}`);
    const url = guardDb();
    const file = path.join("seeds", `${name}.json`);
    if (!existsSync(path.join(ROOT, file))) throw new Usage(`no ${file}; have: ${readdirSync(path.join(ROOT, "seeds")).join(", ")}`);
    const cmd = ["node_modules/.bin/tsx", "scripts/seed-graph.ts", file];
    if (flags["dry-run"]) return plan([{ cwd: ROOT, env: { DATABASE_URL: url }, cmd: cmd.join(" ") }]);
    const r = must(cmd[0], cmd.slice(1), { env: { DATABASE_URL: url } });
    print({ ok: true, seed: file, database: url, output: r.stdout.trim().split("\n").slice(-5) });
    return 0;
  },

  async api([method, p], flags) {
    if (!method || !p) throw new Usage("api <METHOD> <path> [--body '<json>'|-] [--out <file>]");
    method = method.toUpperCase();
    if (!["GET", "POST", "PUT", "PATCH", "DELETE"].includes(method)) throw new Usage(`unknown method ${method}`);
    const port = portFlag(flags);
    guardDb();
    const pathname = p === "/health" || p.startsWith("/api/") ? p : `/api/v1${p.startsWith("/") ? p : `/${p}`}`;
    let body;
    if (flags.body !== undefined) {
      const text = flags.body === "-" ? readFileSync(0, "utf8") : String(flags.body);
      try {
        body = JSON.parse(text);
      } catch {
        throw new Usage(`--body is not JSON: ${text.slice(0, 80)}`);
      }
      // Every write names a T3 thread; a verify run has its own.
      if (method !== "GET" && body && typeof body === "object" && !Array.isArray(body) && body.thread === undefined) body.thread = "verify-lor";
    }
    const url = `http://127.0.0.1:${port}${pathname}`;
    if (flags["dry-run"]) return plan([{ method, url, body }]);
    guardListener(port);
    const res = await fetch(url, {
      method,
      headers: { authorization: `Bearer ${ENV.LOR_AGENT_TOKEN ?? ""}`, ...(body ? { "content-type": "application/json" } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    let data = text;
    try {
      data = JSON.parse(text);
    } catch {}
    const result = { method, path: pathname, status: res.status, body: data };
    if (flags.out) {
      const out = guardEvidenceDir(String(flags.out));
      mkdirSync(path.dirname(out), { recursive: true });
      writeFileSync(out, JSON.stringify({ ...result, at: new Date().toISOString(), ...head() }, null, 2) + "\n");
    }
    print(result);
    return res.ok ? 0 : 1;
  },

  screen([target, outDir], flags) {
    if (!target || !outDir) throw new Usage(`screen <${FLOWS.join("|")}|/path> <out-dir>`);
    if (!FLOWS.includes(target) && !target.startsWith("/")) throw new Usage(`unknown flow ${target}; have: ${FLOWS.join(", ")}, or a path such as /goals`);
    const port = portFlag(flags);
    guardDb();
    const out = guardEvidenceDir(outDir);
    const base = `http://127.0.0.1:${port}`;
    const flow = FLOWS.includes(target);
    const cmd = flow ? ["node", "scripts/ui-check.mjs", target, out] : ["node", "--input-type=module", "-e", SHOT, base + target, out];
    if (flags["dry-run"]) return plan([{ cwd: COPY, env: { LOR_UI_BASE: base }, cmd: flow ? cmd.join(" ") : `screenshot ${base}${target} into ${out}`, note: flow ? `the ${target} flow first deletes rows in ${TEST_DB}` : undefined }]);
    guardListener(port);
    if (!existsSync(path.join(COPY, "scripts/ui-check.mjs"))) throw new Failed(`no copy at ${COPY}. Instead: verify-lor up.`);
    mkdirSync(out, { recursive: true });
    const started = Date.now() - 1000;
    const r = run(cmd[0], cmd.slice(1), { cwd: COPY, env: { LOR_UI_BASE: base }, quiet: true });
    const log = path.join(out, `${flow ? target : "page"}.log`);
    writeFileSync(log, r.stdout + (r.stderr ? `\n--- stderr\n${r.stderr}` : ""));
    const shots = readdirSync(out).filter((f) => f.endsWith(".png") && statSync(path.join(out, f)).mtimeMs >= started);
    const failed = r.status !== 0 || /FLOW FAILED/.test(r.stderr);
    print({ ok: !failed, target, out, shots, log, browser_errors: /browser errors:/.test(r.stdout) });
    return failed ? 1 : 0;
  },

  build(_, flags) {
    const out = path.join(OUT, "apk");
    const cmd = [path.join(ROOT, "android/boat-build.sh"), ROOT, out, "assembleDebug"];
    if (flags["dry-run"]) return plan([{ cmd: cmd.join(" "), note: "runs on the boat VM; never ./gradlew on this machine" }]);
    must(cmd[0], cmd.slice(1), { inherit: true });
    if (!existsSync(APK)) throw new Failed(`boat-build finished but ${APK} is missing; see ${out}/build.log`);
    print({ ok: true, apk: APK, sha256: sha256(APK), ...head() });
    return 0;
  },

  install(_, flags) {
    const port = portFlag(flags);
    const apk = path.resolve(String(flags.apk ?? APK));
    const url = `http://10.0.2.2:${port}`;
    if (flags["dry-run"]) {
      if (flags.serial !== undefined && !/^emulator-\d+$/.test(String(flags.serial))) pickEmulator(flags);
      return plan([
        { cmd: `if installed: am force-stop ${APP}; run-as ${APP}: shared_prefs/lor.xml server_url=${url}`, note: "before the install, so a restart never reaches the real server" },
        { cmd: `${ADB} -s <${AVD} serial> install -r ${apk}`, note: "-r keeps app data; pm clear would bring back the real server" },
        { cmd: `am force-stop ${APP}; run-as ${APP}: shared_prefs/lor.xml server_url=${url}; am force-stop ${APP}` },
      ]);
    }
    const serial = pickEmulator(flags);
    if (!existsSync(apk)) throw new Failed(`no APK at ${apk}. Instead: verify-lor build.`);
    // An installed app restarts during install -r (at once, when it is the default home) and
    // would sync to the real server if server_url were unset, so it is set before as well as after.
    if (readServerUrl(serial).installed) setServerUrl(serial, url);
    const i = adb(serial, ["install", "-r", apk]);
    if (i.status !== 0 || !/Success/.test(i.stdout)) throw new Failed(`adb install failed: ${(i.stdout + i.stderr).trim()}`);
    const attempts = setServerUrl(serial, url);
    if (!listener(port)) process.stderr.write(`verify-lor: note: nothing listens on :${port} yet; run verify-lor up before using the app.\n`);
    print({ ok: true, serial, avd: AVD, apk, sha256: sha256(apk), server_url: url, prefs_attempts: attempts });
    return 0;
  },

  logs([outDir]) {
    if (!outDir) throw new Usage("logs <out-dir>");
    const out = guardEvidenceDir(outDir);
    mkdirSync(out, { recursive: true });
    const files = {};
    if (existsSync(SERVER_LOG)) {
      writeFileSync(path.join(out, "server.log"), readFileSync(SERVER_LOG));
      files.server = path.join(out, "server.log");
    }
    let serial = null;
    try {
      serial = pickEmulator({});
    } catch (err) {
      process.stderr.write(`verify-lor: no emulator logs: ${err.message}\n`);
    }
    if (serial) {
      const pid = adb(serial, ["shell", "pidof", APP]).stdout.trim();
      const app = pid ? adb(serial, ["logcat", "-d", "-v", "time", `--pid=${pid.split(" ")[0]}`]).stdout : "";
      writeFileSync(path.join(out, "launcher-logcat.txt"), app || `(${APP} not running)\n`);
      writeFileSync(path.join(out, "crash-logcat.txt"), adb(serial, ["logcat", "-d", "-v", "time", "-b", "crash"]).stdout);
      files.launcher = path.join(out, "launcher-logcat.txt");
      files.crash = path.join(out, "crash-logcat.txt");
    }
    print({ ok: true, serial, files });
    return 0;
  },

  evidence([dir]) {
    if (!dir) throw new Usage("evidence <dir>");
    if (!existsSync(dir)) throw new Failed(`${dir} does not exist`);
    const files = [];
    const walk = (d) => {
      for (const f of readdirSync(d).sort()) {
        const p = path.join(d, f);
        const s = statSync(p);
        if (s.isDirectory()) walk(p);
        else files.push({ path: path.relative(dir, p), bytes: s.size, sha256: sha256(p), modified: s.mtime.toISOString() });
      }
    };
    walk(dir);
    print({ dir: path.resolve(dir), count: files.length, files });
    return files.length ? 0 : 1;
  },
};

// One screenshot of a URL with the same browser setup as scripts/ui-check.mjs.
const SHOT = `
import path from "node:path";
import { mkdirSync } from "node:fs";
import { chromium } from "playwright-core";
const [url, out] = process.argv.slice(1);
mkdirSync(out, { recursive: true });
const browser = await chromium.launch({ channel: "chrome", headless: true });
const page = await (await browser.newContext({ viewport: { width: 1500, height: 950 }, timezoneId: "America/Los_Angeles" })).newPage();
const errors = [];
page.on("pageerror", (e) => errors.push(e.message));
await page.goto(url);
await page.waitForLoadState("networkidle");
const name = (new URL(url).pathname.replace(/^\\/|\\/$/g, "").replace(/[^a-z0-9]+/gi, "-") || "today") + ".png";
await page.screenshot({ path: path.join(out, name), fullPage: true });
console.log("shot " + name);
if (errors.length) console.log("browser errors:\\n" + errors.join("\\n"));
await browser.close();
`;

function parseArgs(argv) {
  const positionals = [];
  const flags = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (!a.startsWith("--")) {
      positionals.push(a);
      continue;
    }
    const name = a.slice(2);
    if (["dry-run", "keep-copy", "help"].includes(name)) flags[name] = true;
    else if (i + 1 < argv.length) flags[name] = argv[++i];
    else throw new Usage(`--${name} needs a value`);
  }
  return { positionals, flags };
}

async function main() {
  const { positionals, flags } = parseArgs(process.argv.slice(2));
  const [name, ...rest] = positionals;
  if (!name || flags.help || name === "help") {
    process.stdout.write(HELP);
    return name || flags.help ? 0 : 2;
  }
  const command = commands[name];
  if (!command) throw new Usage(`unknown command ${name}`);
  return await command(rest, flags);
}

main().then(
  (code) => process.exit(code),
  (err) => {
    const kind = err instanceof Refused ? "refused" : err instanceof Usage ? "usage" : "failed";
    process.stderr.write(`verify-lor: ${kind}: ${err.message}\n`);
    if (err instanceof Usage) process.stderr.write("Run `verify-lor help` for the commands.\n");
    process.exit({ refused: 3, usage: 2, failed: 1 }[kind]);
  },
);
