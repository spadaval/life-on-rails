// Phone evidence → actual-lane blocks, as pure functions. App sessions roll up
// into a handful of blocks by what they were for; location fixes become stays
// at known places. Glances are too short to be blocks, but they are counted:
// they measure distraction, not time.
import { type Span, normalize, subtractSpans } from "./intervals.js";
import { type Rule, matchRule } from "./suggest.js";

export interface AppSession {
  id: string;
  t: number;
  tEnd: number;
  /** Package, e.g. com.google.android.youtube. */
  app: string;
  /** What the phone calls it, e.g. YouTube. */
  name: string;
  device: string | null;
}

export interface WantedActivity {
  /** The intent the rule points at, or null: phone time nobody has labelled. */
  intentId: string | null;
  ruleId: string | null;
  tStart: number;
  tEnd: number;
  /** "T3 Code 14m · Reddit 1m". */
  title: string;
  observationIds: string[];
}

/** Sessions with the same label this close together are one block. */
export const ACTIVITY_GAP_MS = 10 * 60_000;
/**
 * A session shorter than this inside a block belongs to it, whatever it was (a reply in the middle of work).
 * Only one that starts within PICKUP_GAP_MS of the block's end counts as inside: a blink on home after the
 * phone sat locked must not stretch the block over the locked time.
 */
export const INTERRUPTION_MS = 2 * 60_000;
/** A block with less screen time than this is a glance, not time spent. */
export const MIN_SCREEN_MS = 3 * 60_000;
/** Fragments shorter than this, after taken time is cut out, are dropped. */
const MIN_FRAGMENT_MS = 60_000;

const minutes = (ms: number) => Math.round(ms / 60_000);

function breakdown(sessions: AppSession[]): string {
  const by = new Map<string, number>();
  for (const s of sessions) by.set(s.name, (by.get(s.name) ?? 0) + (s.tEnd - s.t));
  const top = [...by.entries()].sort((a, b) => b[1] - a[1]);
  const shown = top.filter(([, ms], i) => i === 0 || ms >= 60_000).slice(0, 3);
  return shown.map(([name, ms]) => `${name} ${minutes(ms)}m`).join(" · ");
}

/**
 * Which phone blocks should exist. Each session takes the label of the first
 * rule that matches its app; same-label sessions within ACTIVITY_GAP_MS join,
 * short interruptions are absorbed, blocks with under MIN_SCREEN_MS on screen
 * are dropped, and taken time is cut out.
 */
export function wantedActivity(sessions: AppSession[], rules: Rule[], taken: Span[]): WantedActivity[] {
  interface Draft { key: string; intentId: string | null; t: number; tEnd: number; sessions: AppSession[]; rules: Map<string, number> }
  const drafts: Draft[] = [];
  let cur: Draft | null = null;
  for (const s of [...sessions].sort((a, b) => a.t - b.t || a.id.localeCompare(b.id))) {
    const rule = matchRule(rules, { app: s.app, title: s.name, url: null, device: s.device, place: null });
    const intentId = rule?.intentId ?? null;
    const key = intentId ?? "phone";
    const gap = cur ? s.t - cur.tEnd : Infinity;
    const interrupts = gap < PICKUP_GAP_MS && s.tEnd - s.t < INTERRUPTION_MS;
    if (cur && ((gap <= ACTIVITY_GAP_MS && key === cur.key) || interrupts)) {
      cur.tEnd = Math.max(cur.tEnd, s.tEnd);
      cur.sessions.push(s);
      if (key === cur.key && rule) cur.rules.set(rule.id, (cur.rules.get(rule.id) ?? 0) + (s.tEnd - s.t));
      continue;
    }
    cur = { key, intentId, t: s.t, tEnd: s.tEnd, sessions: [s], rules: new Map(rule ? [[rule.id, s.tEnd - s.t]] : []) };
    drafts.push(cur);
  }

  const used = normalize(taken);
  const out: WantedActivity[] = [];
  for (const d of drafts) {
    const screen = d.sessions.reduce((n, s) => n + (s.tEnd - s.t), 0);
    if (screen < MIN_SCREEN_MS) continue;
    const ruleId = [...d.rules.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? null;
    for (const f of subtractSpans([{ t: d.t, tEnd: d.tEnd }], used)) {
      if (f.tEnd - f.t < MIN_FRAGMENT_MS) continue;
      const inside = d.sessions.filter((s) => s.tEnd > f.t && s.t < f.tEnd);
      if (inside.length === 0) continue;
      out.push({ intentId: d.intentId, ruleId, tStart: f.t, tEnd: f.tEnd, title: breakdown(inside), observationIds: inside.map((s) => s.id) });
    }
  }
  return out;
}

// ---- places ----

export interface Fix {
  id: string;
  t: number;
  lat: number;
  lon: number;
  accuracyM: number;
}

export interface Place {
  id: string;
  name: string;
  lat: number;
  lon: number;
  radiusM: number;
}

export interface WantedStay {
  placeId: string;
  tStart: number;
  tEnd: number;
  observationIds: string[];
}

/** Fixes this far apart at one place are still one stay (the phone reports every 15 to 60 minutes). */
export const STAY_GAP_MS = 75 * 60_000;
/** A stay starts a little before its first fix and ends a little after its last. */
const STAY_PAD_MS = 10 * 60_000;
/** Fixes vaguer than this say nothing about which building. */
export const MAX_ACCURACY_M = 250;
export const DEFAULT_RADIUS_M = 150;

export function distanceM(aLat: number, aLon: number, bLat: number, bLon: number): number {
  const rad = Math.PI / 180;
  const dLat = (bLat - aLat) * rad;
  const dLon = (bLon - aLon) * rad;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(aLat * rad) * Math.cos(bLat * rad) * Math.sin(dLon / 2) ** 2;
  return 2 * 6_371_000 * Math.asin(Math.sqrt(h));
}

export function placeOf(fix: Fix, places: Place[]): Place | null {
  if (fix.accuracyM > MAX_ACCURACY_M) return null;
  let best: Place | null = null;
  let bestD = Infinity;
  for (const p of places) {
    const d = distanceM(fix.lat, fix.lon, p.lat, p.lon);
    if (d <= p.radiusM && d < bestD) {
      best = p;
      bestD = d;
    }
  }
  return best;
}

/**
 * Stays at known places: runs of consecutive fixes at the same place, no more
 * than STAY_GAP_MS apart, padded at each end but never past the neighbouring
 * fix that was somewhere else.
 */
export function wantedStays(fixes: Fix[], places: Place[]): WantedStay[] {
  const sorted = [...fixes].filter((f) => f.accuracyM <= MAX_ACCURACY_M).sort((a, b) => a.t - b.t);
  const at = sorted.map((f) => placeOf(f, places)?.id ?? null);
  const out: WantedStay[] = [];
  let i = 0;
  while (i < sorted.length) {
    const placeId = at[i];
    if (!placeId) {
      i++;
      continue;
    }
    let j = i;
    while (j + 1 < sorted.length && at[j + 1] === placeId && sorted[j + 1]!.t - sorted[j]!.t <= STAY_GAP_MS) j++;
    const before = i > 0 ? sorted[i - 1]!.t : -Infinity;
    const after = j + 1 < sorted.length ? sorted[j + 1]!.t : Infinity;
    out.push({
      placeId,
      tStart: Math.max(sorted[i]!.t - STAY_PAD_MS, before),
      tEnd: Math.min(sorted[j]!.t + STAY_PAD_MS, after),
      observationIds: sorted.slice(i, j + 1).map((f) => f.id),
    });
    i = j + 1;
  }
  return out;
}

export interface UnknownPlace {
  lat: number;
  lon: number;
  fixes: number;
  /** Rough time there: gaps between consecutive fixes in the cluster, each at most STAY_GAP_MS. */
  hours: number;
  first_seen: string;
  last_seen: string;
}

/** Where the phone keeps turning up that is not a known place yet, most time first. */
export function unknownPlaces(fixes: Fix[], places: Place[], minFixes = 3): UnknownPlace[] {
  interface Cluster { lat: number; lon: number; fixes: Fix[] }
  const clusters: Cluster[] = [];
  const sorted = [...fixes].filter((f) => f.accuracyM <= MAX_ACCURACY_M && !placeOf(f, places)).sort((a, b) => a.t - b.t);
  const owner: (Cluster | null)[] = [];
  for (const f of sorted) {
    let c = clusters.find((k) => distanceM(f.lat, f.lon, k.lat, k.lon) <= DEFAULT_RADIUS_M) ?? null;
    if (!c) {
      c = { lat: f.lat, lon: f.lon, fixes: [] };
      clusters.push(c);
    }
    c.fixes.push(f);
    c.lat = c.fixes.reduce((n, x) => n + x.lat, 0) / c.fixes.length;
    c.lon = c.fixes.reduce((n, x) => n + x.lon, 0) / c.fixes.length;
    owner.push(c);
  }
  const ms = new Map<Cluster, number>();
  for (let i = 1; i < sorted.length; i++) {
    const gap = sorted[i]!.t - sorted[i - 1]!.t;
    if (owner[i] === owner[i - 1] && gap <= STAY_GAP_MS) ms.set(owner[i]!, (ms.get(owner[i]!) ?? 0) + gap);
  }
  return clusters
    .filter((c) => c.fixes.length >= minFixes)
    .map((c) => ({
      lat: Math.round(c.lat * 1e5) / 1e5,
      lon: Math.round(c.lon * 1e5) / 1e5,
      fixes: c.fixes.length,
      hours: Math.round(((ms.get(c) ?? 0) / 3_600_000) * 10) / 10,
      first_seen: new Date(c.fixes[0]!.t).toISOString(),
      last_seen: new Date(c.fixes[c.fixes.length - 1]!.t).toISOString(),
    }))
    .sort((a, b) => b.hours - a.hours || b.fixes - a.fixes);
}

// ---- distraction ----

export interface PhoneStats {
  /** Seconds on screen. */
  screen_s: number;
  /** Times the phone was picked up: a session after at least PICKUP_GAP_MS of nothing. */
  pickups: number;
  /** Pickups that were over within GLANCE_MS. */
  glances: number;
  top: { name: string; s: number }[];
}

export const PICKUP_GAP_MS = 2 * 60_000;
export const GLANCE_MS = 2 * 60_000;

export function phoneStats(sessions: AppSession[], from: number, to: number): PhoneStats {
  const inside = sessions
    .map((s) => ({ ...s, t: Math.max(s.t, from), tEnd: Math.min(s.tEnd, to) }))
    .filter((s) => s.tEnd > s.t)
    .sort((a, b) => a.t - b.t);
  let screen = 0;
  let pickups = 0;
  let glances = 0;
  let stretch: { t: number; tEnd: number } | null = null;
  const by = new Map<string, number>();
  const close = () => {
    if (stretch && stretch.tEnd - stretch.t < GLANCE_MS) glances++;
  };
  for (const s of inside) {
    screen += s.tEnd - s.t;
    by.set(s.name, (by.get(s.name) ?? 0) + (s.tEnd - s.t));
    if (!stretch || s.t - stretch.tEnd >= PICKUP_GAP_MS) {
      close();
      pickups++;
      stretch = { t: s.t, tEnd: s.tEnd };
    } else {
      stretch.tEnd = Math.max(stretch.tEnd, s.tEnd);
    }
  }
  close();
  const top = [...by.entries()].sort((a, b) => b[1] - a[1]).slice(0, 3).map(([name, ms]) => ({ name, s: Math.round(ms / 1000) }));
  return { screen_s: Math.round(screen / 1000), pickups, glances, top };
}
