// The TRMNL X on the wall, speaking the firmware's own-server protocol. It
// wakes every quarter hour, asks /api/display for an image, draws it, and
// sleeps. scripts/serve.mjs maps /api/{setup,display,log} on the LAN listener
// to /trmnl/* here. One device: LOR_TRMNL_MAC gets LOR_TRMNL_KEY at setup, and
// every later request must carry that key.
import { randomUUID, timingSafeEqual } from "node:crypto";
import { sql } from "../../db/client.js";
import { addDays, localTimeOnDay, localYmd } from "../../shared/localTime.js";
import { env } from "../env.js";
import { needsOwner } from "../intents.js";
import { errorMessage, log } from "../log.js";
import { jobStatuses } from "../status.js";
import { getToday, wakeWindow } from "../today.js";
import { FRAME_H, FRAME_SCALE, FRAME_W, type FrameInput, frameHtml } from "./frame.js";
import { grayPng2 } from "./png.js";
import { frameFonts, renderFrame } from "./render.js";

const REFRESH_S = 900;
const LOW_BATTERY_PCT = 20;

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

const sameText = (a: string, b: string) => {
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
};
const normMac = (s: string | null) => (s ?? "").trim().toUpperCase();
const keyOk = (request: Request) => env.LOR_TRMNL_KEY !== "" && sameText(request.headers.get("access-token") ?? "", env.LOR_TRMNL_KEY);

/** The last frame drawn; the device fetches it by name right after /api/display. */
let latest: { name: string; png: Buffer } | null = null;

export async function frameInput(now: Date, alerts: string[]): Promise<FrameInput> {
  const date = localYmd(env.LOR_TZ, now);
  const [today, needs, jobs] = await Promise.all([getToday(sql, date, now), needsOwner(sql), jobStatuses(now)]);
  const wake = wakeWindow(date);
  const hours: Date[] = [];
  for (let d = 0; d < 2; d++) {
    for (let h = 0; h < 24; h++) {
      const t = localTimeOnDay(env.LOR_TZ, addDays(date, d), `${String(h).padStart(2, "0")}:00`);
      if (t >= wake.start && t <= wake.end) hours.push(t);
    }
  }
  const dayStart = new Date(today.day_start).getTime();
  const dayEnd = new Date(today.day_end).getTime();
  const recordedS = today.actual.reduce((s, b) => {
    const a = Math.max(new Date(b.t_start).getTime(), dayStart);
    const z = Math.min(b.t_end ? new Date(b.t_end).getTime() : now.getTime(), dayEnd);
    return s + Math.max(0, z - a) / 1000;
  }, 0);
  const unwell = jobs.filter((j) => j.state === "stale" || j.state === "error").length;
  const r = today.running;
  return {
    now,
    timezone: env.LOR_TZ,
    date,
    wakeStart: wake.start,
    wakeEnd: wake.end,
    hours,
    blocks: today.actual.map((b) => ({
      t_start: b.t_start,
      t_end: b.t_end,
      paused: b.paused_at !== null,
      committed: b.committed,
      bare: (b.origin === "auto_activity" || b.origin === "place") && !b.intent && !b.committed && !b.proposal,
    })),
    events: today.planned.filter((e) => !e.all_day).map((e) => ({ title: e.title ?? "(no title)", t_start: e.t_start })),
    running: r ? { name: r.intent ?? r.title ?? "Unlabelled", since: r.t_start } : null,
    recordedS,
    needsYou: needs.map((n) => n.title),
    toConfirm: today.to_confirm,
    alerts: [...(unwell > 0 ? [`${unwell} ${unwell === 1 ? "job needs" : "jobs need"} a look`] : []), ...alerts],
    fontCss: frameFonts(),
  };
}

export async function trmnlSetup(request: Request): Promise<Response> {
  const mac = normMac(request.headers.get("id"));
  if (!env.LOR_TRMNL_KEY || !mac || mac !== normMac(env.LOR_TRMNL_MAC)) {
    // The MAC is how the owner learns what to put in LOR_TRMNL_MAC.
    log("warn", "trmnl setup refused", { mac, model: request.headers.get("model") });
    return json(404, { status: 404, api_key: null, friendly_id: null, image_url: null, message: "Not this LoR's device" });
  }
  log("info", "trmnl setup", { mac, fw: request.headers.get("fw-version") });
  // The firmware insists on a setup image and draws its own screen over it; a blank one says nothing.
  const image_url = `${new URL(request.url).origin}/trmnl/image/setup.png`;
  return json(200, { status: 200, api_key: env.LOR_TRMNL_KEY, friendly_id: "LOR", image_url, filename: "setup", message: "Life on Rails" });
}

export async function trmnlDisplay(request: Request): Promise<Response> {
  if (!keyOk(request)) return json(500, { status: 500, error: "Device not found" });
  const h = (k: string) => request.headers.get(k);
  const pct = Number(h("percent-charged"));
  const charging = h("usb-connected") === "true" || h("battery-charging") === "1";
  log("info", "trmnl display", { battery: h("percent-charged") ?? h("battery-voltage"), usb: h("usb-connected"), rssi: h("rssi"), fw: h("fw-version") });
  const alerts = Number.isFinite(pct) && pct > 0 && pct < LOW_BATTERY_PCT && !charging ? [`Battery ${Math.round(pct)}%`] : [];
  try {
    const png = await renderFrame(frameHtml(await frameInput(new Date(), alerts)));
    latest = { name: randomUUID(), png };
  } catch (err) {
    // The last frame stays up with its own "Updated" time, which is the honest thing to show.
    log("error", "trmnl render failed", { err: errorMessage(err) });
    if (!latest) return json(500, { status: 500, error: "No frame yet" });
  }
  const origin = new URL(request.url).origin;
  return json(200, {
    status: 0,
    image_url: `${origin}/trmnl/image/${latest!.name}.png`,
    filename: latest!.name,
    refresh_rate: REFRESH_S,
    update_firmware: false,
    reset_firmware: false,
  });
}

let blank: Buffer | null = null;

export function trmnlImage(request: Request, name: string): Response {
  if (name === "setup") {
    const w = Math.round(FRAME_W * FRAME_SCALE);
    const h = Math.round(FRAME_H * FRAME_SCALE);
    blank ??= grayPng2(w, h, new Uint8Array(w * h).fill(3));
    return new Response(new Uint8Array(blank), { headers: { "content-type": "image/png", "content-length": String(blank.length) } });
  }
  if (!keyOk(request) || !latest || latest.name !== name) return new Response("not found\n", { status: 404 });
  return new Response(new Uint8Array(latest.png), { headers: { "content-type": "image/png", "content-length": String(latest.png.length) } });
}

export async function trmnlLog(request: Request): Promise<Response> {
  if (!keyOk(request)) return json(404, { status: 404 });
  const body = (await request.json().catch(() => null)) as { logs?: unknown[] } | null;
  for (const entry of body?.logs ?? []) log("warn", "trmnl device log", { entry });
  return json(200, { status: 200 });
}
