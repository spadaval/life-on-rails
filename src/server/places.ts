// Places and attribution rules, for the agent and the owner: where the phone
// keeps turning up, which of those places have names, which apps were used,
// and which app or place stands for which goal. Phone and place blocks follow
// from these (see attribution/fill.ts).
import type { Sql, Tx } from "../db/client.js";
import { uuidv7 } from "../shared/ids.js";
import { DEFAULT_RADIUS_M, type UnknownPlace, unknownPlaces } from "./attribution/activity.js";
import { fillWindow } from "./attribution/blocks.js";
import { loadFixes, loadPlaces, lockBlocks, syncFill } from "./attribution/fill.js";
import { globMatch, validateTitleRegex } from "./attribution/suggest.js";
import { BadInput, NotFound } from "./errors.js";
import { scheduleRecompute } from "./scoring/recompute.js";

/** How far back a new place or rule relabels blocks. */
const RELABEL_DAYS = 14;

async function relabel(db: Sql): Promise<void> {
  const now = new Date();
  const { from, to } = fillWindow(now, new Date(now.getTime() - RELABEL_DAYS * 86_400_000));
  await db.begin(async (tx) => {
    await lockBlocks(tx);
    await syncFill(tx, from, to);
  });
  scheduleRecompute();
}

export interface PlaceView {
  id: string;
  name: string;
  lat: number;
  lon: number;
  radius_m: number;
  intent: { id: string; title: string } | null;
}

export async function listPlaces(db: Sql, now = new Date()): Promise<{ places: PlaceView[]; unknown: UnknownPlace[] }> {
  const places = await loadPlaces(db);
  const rules = await db<{ match_place: string; intent_id: string; title: string }[]>`
    SELECT r.match_place, r.intent_id, i.title FROM attribution_rule r JOIN intent i ON i.id = r.intent_id WHERE r.match_place IS NOT NULL`;
  const ruleBy = new Map(rules.map((r) => [r.match_place, { id: r.intent_id, title: r.title }]));
  const fixes = await loadFixes(db, new Date(now.getTime() - 60 * 86_400_000), now);
  return {
    places: places.map((p) => ({ id: p.id, name: p.name, lat: p.lat, lon: p.lon, radius_m: p.radiusM, intent: ruleBy.get(p.id) ?? null })),
    unknown: unknownPlaces(fixes, places).slice(0, 10),
  };
}

/** Name a place (or move or resize one with that name), optionally saying what time there is for. */
export async function addPlace(db: Sql, input: { name: string; lat: number; lon: number; radiusM?: number | null; intentId?: string | null }): Promise<PlaceView> {
  const name = input.name.trim();
  if (!name) throw new BadInput("a place needs a name");
  if (!(Math.abs(input.lat) <= 90 && Math.abs(input.lon) <= 180)) throw new BadInput("--at must be <lat>,<lon>");
  const radius = input.radiusM ?? DEFAULT_RADIUS_M;
  if (!(radius >= 20 && radius <= 5000)) throw new BadInput("--radius must be between 20 and 5000 metres");
  let id = "";
  await db.begin(async (tx) => {
    const existing = await tx<{ id: string }[]>`SELECT id FROM entity WHERE kind = 'place' AND lower(name) = lower(${name}) LIMIT 1`;
    id = existing[0]?.id ?? uuidv7();
    if (existing[0]) {
      await tx`UPDATE entity SET lat = ${input.lat}, lon = ${input.lon}, radius_m = ${radius}, updated_at = now() WHERE id = ${id}`;
    } else {
      await tx`INSERT INTO entity (id, kind, name, lat, lon, radius_m) VALUES (${id}, 'place', ${name}, ${input.lat}, ${input.lon}, ${radius})`;
    }
    if (input.intentId) {
      await checkIntent(tx, input.intentId);
      await tx`DELETE FROM attribution_rule WHERE match_place = ${id}`;
      await tx`INSERT INTO attribution_rule (id, match_place, intent_id) VALUES (${uuidv7()}, ${id}, ${input.intentId})`;
    }
  });
  await relabel(db);
  const view = (await listPlaces(db)).places.find((p) => p.id === id);
  if (!view) throw new NotFound(`no place ${id}`);
  return view;
}

async function checkIntent(db: Tx, intentId: string): Promise<void> {
  const rows = await db`SELECT 1 FROM intent WHERE id = ${intentId} AND status <> 'archived'`;
  if (rows.length === 0) throw new NotFound(`no intent ${intentId}`);
}

export interface RuleView {
  id: string;
  app: string | null;
  title: string | null;
  place: string | null;
  intent_id: string;
  intent: string;
  auto_apply: boolean;
}

export async function listRules(db: Sql): Promise<RuleView[]> {
  return db<RuleView[]>`
    SELECT r.id, r.match_app AS app, r.match_title AS title, p.name AS place, r.intent_id, i.title AS intent, r.auto_apply
    FROM attribution_rule r JOIN intent i ON i.id = r.intent_id LEFT JOIN entity p ON p.id = r.match_place
    ORDER BY r.priority DESC, r.created_at`;
}

/** One rule: an app (package glob), a calendar title (regex), or a place stands for an intent. */
export async function addRule(db: Sql, input: { app?: string | null; title?: string | null; place?: string | null; intentId: string }): Promise<RuleView> {
  const given = [input.app, input.title, input.place].filter((v) => v).length;
  if (given !== 1) throw new BadInput("give exactly one of --app, --title, --place");
  if (input.title && !validateTitleRegex(input.title)) throw new BadInput("--title must be a regex of at most 200 characters");
  let placeId: string | null = null;
  if (input.place) {
    const p = await db<{ id: string }[]>`
      SELECT id FROM entity WHERE kind = 'place' AND (id::text = ${input.place} OR lower(name) = lower(${input.place})) LIMIT 1`;
    if (!p[0]) throw new NotFound(`no place ${input.place}; name it first with \`lor place add\``);
    placeId = p[0].id;
  }
  await checkIntent(db, input.intentId);
  const id = uuidv7();
  await db.begin(async (tx) => {
    if (input.app) await tx`DELETE FROM attribution_rule WHERE match_app = ${input.app} AND match_title IS NULL AND match_place IS NULL`;
    if (placeId) await tx`DELETE FROM attribution_rule WHERE match_place = ${placeId}`;
    await tx`INSERT INTO attribution_rule (id, match_app, match_title, match_place, intent_id)
      VALUES (${id}, ${input.app ?? null}, ${input.title ?? null}, ${placeId}, ${input.intentId})`;
  });
  await relabel(db);
  return (await listRules(db)).find((r) => r.id === id)!;
}

export async function deleteRule(db: Sql, id: string): Promise<{ deleted: boolean }> {
  const rows = await db`DELETE FROM attribution_rule WHERE id = ${id} RETURNING id`;
  if (rows.length > 0) await relabel(db);
  return { deleted: rows.length > 0 };
}

export interface AppUse {
  app: string;
  name: string;
  hours: number;
  sessions: number;
  intent: string | null;
}

/** Which apps took the phone's time lately, and what each is labelled as. */
export async function listApps(db: Sql, days = 14, now = new Date()): Promise<AppUse[]> {
  const rows = await db<{ app: string; name: string; hours: number; sessions: number }[]>`
    SELECT app, max(title) AS name, round((sum(extract(epoch FROM t_end - t_start)) / 3600)::numeric, 1)::float8 AS hours, count(*)::int AS sessions
    FROM observation
    WHERE kind = 'app' AND app IS NOT NULL AND t_end IS NOT NULL
      AND t_start > ${new Date(now.getTime() - days * 86_400_000)}
    GROUP BY app ORDER BY sum(t_end - t_start) DESC LIMIT 40`;
  const rules = await db<{ match_app: string; title: string }[]>`
    SELECT r.match_app, i.title FROM attribution_rule r JOIN intent i ON i.id = r.intent_id
    WHERE r.match_app IS NOT NULL ORDER BY r.priority DESC, r.created_at`;
  return rows.map((r) => ({ ...r, intent: rules.find((x) => globMatch(x.match_app, r.app))?.title ?? null }));
}
