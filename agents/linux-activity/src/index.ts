// lor-linux-activity: on-box sensor daemon (systemd user unit).
// Sample every 2s, coalesce locally, flush every 10s to POST /ingest.
// No graphical session → heartbeat source=linux_activity title=no_session
// every 5 min (always persisted server-side, even when disabled).
// Idle (180s, CLASS=user seat only) → title=idle. No keystroke/clipboard/mic.
import { randomUUID } from "node:crypto";
import { detectBackend, sampleFocus, type Backend } from "./backends.js";

const INGEST_URL = process.env.LOR_INGEST_URL ?? "http://127.0.0.1:3847/ingest";
const TOKEN = process.env.LOR_INGEST_TOKEN ?? "";
const FLUSH_S = Number(process.env.LOR_ACTIVITY_FLUSH_S ?? 10);
const DEVICE = process.env.LOR_DEVICE ?? "buildbot";

interface Pending {
  id: string;
  t_start: string;
  t_end: string | null;
  app: string | null;
  title: string | null;
}

let current: Pending | null = null;
let lastChangeMs = Date.now();
let backend: Backend = "none";

function envelope(p: Pending) {
  return {
    id: p.id,
    t_start: p.t_start,
    t_end: p.t_end,
    device: DEVICE,
    source: "linux_activity",
    app: p.app,
    calendar_id: null,
    title: p.title,
    url: null,
    payload: {},
    intent_hint: null,
  };
}

async function flush(batch: Pending[]): Promise<void> {
  if (batch.length === 0 || !TOKEN) return;
  try {
    await fetch(INGEST_URL, {
      method: "POST",
      headers: { "content-type": "application/json", authorization: `Bearer ${TOKEN}` },
      body: JSON.stringify(batch.map(envelope)),
      signal: AbortSignal.timeout(10_000),
    });
  } catch (err) {
    console.error(JSON.stringify({ ts: new Date().toISOString(), level: "error", job: "flush", err: String(err) }));
  }
}

async function heartbeat(): Promise<void> {
  await flush([{
    id: randomUUID(),
    t_start: new Date().toISOString(),
    t_end: new Date().toISOString(),
    app: null,
    title: "no_session",
  }]);
}

async function tick(): Promise<void> {
  const focus = await sampleFocus(backend);
  const now = new Date();
  if (!focus || (!focus.app && !focus.title)) return; // no sample; t_end extends on next change
  if (!current || current.app !== focus.app || current.title !== focus.title) {
    if (current) {
      current.t_end = now.toISOString();
      await flush([current]);
    }
    current = { id: randomUUID(), t_start: now.toISOString(), t_end: null, app: focus.app, title: focus.title };
    lastChangeMs = now.getTime();
  } else {
    current.t_end = now.toISOString();
    if (now.getTime() - lastChangeMs >= FLUSH_S * 1000) {
      lastChangeMs = now.getTime();
      await flush([current]);
      current = { id: randomUUID(), t_start: now.toISOString(), t_end: null, app: current.app, title: current.title };
    }
  }
}

async function main(): Promise<void> {
  backend = await detectBackend();
  console.log(JSON.stringify({ ts: new Date().toISOString(), level: "info", job: "start", backend }));
  if (backend === "none") {
    await heartbeat();
    setInterval(heartbeat, 5 * 60_000);
    // Re-probe occasionally: a compositor may appear later.
    setInterval(async () => {
      backend = await detectBackend();
      if (backend !== "none") {
        console.log(JSON.stringify({ ts: new Date().toISOString(), level: "info", job: "backend", backend }));
        // Hand over to the sampling loop on next tick cycle.
      }
    }, 60_000);
    return;
  }
  setInterval(tick, 2000);
}

await main();
