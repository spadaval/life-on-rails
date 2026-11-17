#!/usr/bin/env node
// The launcher lab's default fixture from the demo set (lor-oech): reads /device/home,
// /device/tasks and /device/people from a test server with the demo set loaded, at the
// dashboard's moment (2026-11-17 15:00, as scripts/ui-dev.sh and seeds/demo/README.md open
// on), and writes them into homecore's common code as DemoDay.kt, so the lab and the debug
// app draw the same made-up day as the dashboard.
//
//   LOR_FAKE_NOW=2026-11-17T15:00 node .claude/skills/verify-lor/verify.mjs up
//   LOR_FAKE_NOW=2026-11-17T15:00 pnpm demo:test
//   node scripts/lab/demo-fixture.mjs [--base http://127.0.0.1:3857] [--check]
//
// --check writes nothing and exits 1 when the checked-in file differs from what the server
// gives. The weather is a live Google call on the test server too, so it is replaced with a
// pinned reading (WEATHER below), and the rows' ids are renumbered, so the file stays the
// same from one load to the next.
// It refuses lor-web's ports, and a server that does not take at= (only the test database
// does, src/server/clock.ts), since then the reply is the real day, not the demo's.
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const AT = "2026-11-17T15:00";
const WEATHER = { temp_c: 11.6, condition: "Partly cloudy", is_day: true, observed_at: "2026-11-17T19:45:00.000Z" };
const OUT = path.join(path.dirname(fileURLToPath(import.meta.url)), "../../android/homecore/src/commonMain/kotlin/com/spadaval/lor/lab/DemoDay.kt");

const args = process.argv.slice(2);
const check = args.includes("--check");
const bi = args.indexOf("--base");
const base = (bi >= 0 ? args[bi + 1] : "http://127.0.0.1:3857").replace(/\/$/, "");
const port = new URL(base).port;
if (["3847", "3848", "3849"].includes(port)) {
  console.error(`demo-fixture: refused: port ${port} is lor-web's; point --base at the test server`);
  process.exit(3);
}

async function read(p) {
  const url = `${base}${p}${p.includes("?") ? "&" : "?"}at=${encodeURIComponent(AT)}&data=base`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${url}: ${res.status} ${await res.text()}`);
  return res.json();
}

const home = await read("/device/home?device=fold7");
if (!String(home.now).startsWith("2026-11-17T20:00")) {
  console.error(`demo-fixture: refused: ${base} answered now=${home.now} for at=${AT}; it is not the test server, or its zone is not America/New_York`);
  process.exit(3);
}
if (home.weather) home.weather = WEATHER;
const tasks = await read("/device/tasks");
const people = await read("/device/people");

// Each load gives its rows new uuidv7 ids, so they are renumbered in the order they appear,
// one map across the three replies (a task in needs_you keeps its id in /device/tasks).
const ids = new Map();
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const stable = (v) => {
  if (Array.isArray(v)) return v.map(stable);
  if (v && typeof v === "object") return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, stable(x)]));
  if (typeof v === "string" && UUID.test(v)) {
    if (!ids.has(v)) ids.set(v, `00000000-0000-7000-8000-${String(ids.size + 1).padStart(12, "0")}`);
    return ids.get(v);
  }
  return v;
};
const [homeS, tasksS, peopleS] = [stable(home), stable(tasks), stable(people)];

// A Kotlin raw string holds anything but `"""` and `$`; the payloads have no `"""`.
const raw = (o) => {
  const s = JSON.stringify(o, null, 2);
  if (s.includes('"""')) throw new Error('a payload holds """');
  return '"""\n' + s.replaceAll("$", "${'$'}") + '\n"""';
};
const kt = `package com.spadaval.lor.lab

// Written by scripts/lab/demo-fixture.mjs from the demo set (seeds/demo/) at ${AT}, the moment
// the dashboard's design tools open on. Don't edit by hand: load the demo set and run the script.
// The weather is pinned there (the test server's is a live call), and the ids are renumbered.

/** LoR's replies on the demo day, as the server sends them. */
object DemoDay {
    const val AT = "${AT}"

    /** \`GET /device/home\`. */
    val home = ${raw(homeS)}

    /** \`GET /device/tasks\`. */
    val tasks = ${raw(tasksS)}

    /** \`GET /device/people\`. */
    val people = ${raw(peopleS)}
}
`;
let before = "";
try { before = readFileSync(OUT, "utf8"); } catch {}
if (check) {
  if (before !== kt) { console.error(`demo-fixture: ${path.relative(process.cwd(), OUT)} differs from ${base}'s demo day`); process.exit(1); }
  console.log("demo-fixture: the checked-in demo day matches");
} else {
  writeFileSync(OUT, kt);
  console.log(`demo-fixture: wrote ${path.relative(process.cwd(), OUT)}${before === kt ? " (unchanged)" : ""}`);
}
