// Drizzle pg schema — Life on Rails v2 (spec: Data Model Changes).
// FK delete matrix (normative, enforced partly here and partly in migrate.ts
// raw SQL because proposal → time_block → commitment → proposal is a cycle):
//   proposal.time_block_id → time_block(id) ON DELETE CASCADE
//   proposal.intent_id     → intent(id)      ON DELETE SET NULL
//   commitment.intent_id   → intent(id)      ON DELETE RESTRICT
//   commitment.proposal_id → proposal(id)    ON DELETE SET NULL
//   commitment.time_block_id → time_block(id) ON DELETE RESTRICT
//   time_block.commitment_id → commitment(id) ON DELETE SET NULL
//   time_block.intent_id   → intent(id)      ON DELETE RESTRICT
//   observation.intent_hint → intent(id)     ON DELETE SET NULL
import {
  boolean,
  index,
  integer,
  jsonb,
  pgEnum,
  pgTable,
  primaryKey,
  real,
  text,
  time,
  timestamp,
  uniqueIndex,
  uuid,
  customType,
} from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";
import { uuidv7 } from "../shared/ids.js";

// pgvector column. The HNSW index + extension live in migrate.ts raw SQL
// (Drizzle has no native vector/HNSW DDL).
const vector384 = customType<{ data: number[]; driverData: string }>({
  dataType: () => "vector(384)",
  toDriver: (v) => `[${v.join(",")}]`,
  fromDriver: (v: string): number[] => {
    const s = v.trim().replace(/^\[|\]$/g, "");
    if (!s) return [];
    return s.split(",").map(Number);
  },
});

export const intentType = pgEnum("intent_type", ["goal", "project", "habit", "task", "metric"]);
export const intentStatus = pgEnum("intent_status", ["active", "paused", "later", "done", "archived"]);
export const horizon = pgEnum("horizon", ["day", "week"]);
export const metricKind = pgEnum("metric_kind", ["five_point", "numeric", "duration_hours", "derived"]);
export const linkKind = pgEnum("link_kind", ["measures", "blocks", "derived_from"]);
export const proposalKind = pgEnum("proposal_kind", [
  "link_intent",
  "time_attribution",
  "habit_completion",
  "metric_value",
  "day_plan",
]);
export const proposalStatus = pgEnum("proposal_status", ["pending", "accepted", "rejected", "expired"]);
export const commitmentKind = pgEnum("commitment_kind", [
  "habit_completion",
  "metric_value",
  "time_spent",
  "task_done",
  "note",
]);
export const blockOrigin = pgEnum("block_origin", ["auto_activity", "calendar", "manual", "merged"]);

export const userProfile = pgTable("user_profile", {
  id: boolean("id").primaryKey().default(true),
  displayName: text("display_name").notNull().default("me"),
  timezone: text("timezone").notNull().default("America/Los_Angeles"),
  scoringWeeks: integer("scoring_weeks").notNull().default(2),
  wakeStart: time("wake_start").notNull().default("08:00"),
  wakeEnd: time("wake_end").notNull().default("22:00"),
  idleThresholdS: integer("idle_threshold_s").notNull().default(180),
  xaiModel: text("xai_model").notNull().default("grok-4.7"),
  outboundEnabled: boolean("outbound_enabled").notNull().default(false),
});

export const intent = pgTable(
  "intent",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    type: intentType("type").notNull(),
    title: text("title").notNull(),
    description: text("description"),
    status: intentStatus("status").notNull().default("active"),
    weight: real("weight").notNull().default(1),
    weeklyHourTarget: real("weekly_hour_target"),
    frequency: integer("frequency"),
    frequencyHorizon: horizon("frequency_horizon").default("day"),
    // Habit mix α only (NOT goal β).
    completionWeight: real("completion_weight").default(0.7),
    // Goal β (children vs hours). NOT completion_weight.
    goalMix: real("goal_mix").default(0.7),
    system: boolean("system").notNull().default(false),
    metricKind: metricKind("metric_kind"),
    metricMin: real("metric_min"),
    metricMax: real("metric_max"),
    estimateHours: real("estimate_hours"),
    dueAt: timestamp("due_at", { withTimezone: true }),
    snoozedUntil: timestamp("snoozed_until", { withTimezone: true }),
    meta: jsonb("meta").$type<Record<string, unknown>>().notNull().default({}),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
    archivedAt: timestamp("archived_at", { withTimezone: true }),
  },
  (t) => [index("intent_type_status_idx").on(t.type, t.status)],
);

export const intentLink = pgTable(
  "intent_link",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    // Convention: from_id = child/source, to_id = parent/target.
    fromId: uuid("from_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    toId: uuid("to_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    kind: linkKind("kind").notNull(),
    weight: real("weight").notNull().default(1),
    meta: jsonb("meta").$type<Record<string, unknown>>().notNull().default({}),
  },
  (t) => [
    uniqueIndex("intent_link_unique").on(t.fromId, t.toId, t.kind),
    index("intent_link_to_idx").on(t.toId),
    index("intent_link_from_idx").on(t.fromId),
  ],
);

export const tag = pgTable("tag", {
  id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
  name: text("name").notNull().unique(),
});

export const intentTag = pgTable(
  "intent_tag",
  {
    intentId: uuid("intent_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    tagId: uuid("tag_id")
      .notNull()
      .references(() => tag.id, { onDelete: "cascade" }),
  },
  (t) => [index("intent_tag_pk").on(t.intentId, t.tagId)],
);

export const observation = pgTable(
  "observation",
  {
    id: uuid("id").primaryKey(),
    tStart: timestamp("t_start", { withTimezone: true }).notNull(),
    tEnd: timestamp("t_end", { withTimezone: true }),
    device: text("device").notNull(),
    source: text("source").notNull(),
    app: text("app"),
    calendarId: text("calendar_id"),
    title: text("title"),
    url: text("url"),
    payload: jsonb("payload").$type<Record<string, unknown>>().notNull().default({}),
    // SET NULL applied in migrate.ts raw SQL (kept plain here to avoid ordering issues).
    intentHint: uuid("intent_hint"),
    ingestedAt: timestamp("ingested_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => [
    index("observation_time_idx").on(t.tStart),
    index("observation_source_time_idx").on(t.source, t.tStart),
  ],
);

export const titleEmbedding = pgTable(
  "title_embedding",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    app: text("app"),
    title: text("title").notNull(),
    embedding: vector384("embedding").notNull(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => [uniqueIndex("title_embedding_app_title").on(t.app, t.title)],
);

// Cycle member: time_block_id / intent_id FKs are added in migrate.ts raw SQL
// (proposal → time_block → commitment → proposal). Columns stay plain here.
export const proposal = pgTable(
  "proposal",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    kind: proposalKind("kind").notNull(),
    status: proposalStatus("status").notNull().default("pending"),
    confidence: real("confidence").notNull(),
    timeBlockId: uuid("time_block_id"),
    intentId: uuid("intent_id"),
    payload: jsonb("payload").$type<Record<string, unknown>>().notNull().default({}),
    reason: text("reason"),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
    resolvedAt: timestamp("resolved_at", { withTimezone: true }),
  },
  (t) => [
    index("proposal_pending_idx").on(t.status, t.createdAt).where(sql`status = 'pending'`),
    index("proposal_block_idx").on(t.timeBlockId).where(sql`time_block_id is not null`),
  ],
);

// Cycle member: proposal_id / time_block_id FKs added in migrate.ts raw SQL.
export const commitment = pgTable(
  "commitment",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    kind: commitmentKind("kind").notNull(),
    intentId: uuid("intent_id").notNull(),
    t: timestamp("t", { withTimezone: true }).notNull(),
    tEnd: timestamp("t_end", { withTimezone: true }),
    // Display-only copy for time_spent; scorer recomputes from [t, t_end].
    value: real("value"),
    memo: text("memo"),
    proposalId: uuid("proposal_id"),
    timeBlockId: uuid("time_block_id"),
    observationIds: uuid("observation_ids").array(),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => [
    index("commitment_intent_t_idx").on(t.intentId, t.t),
    index("commitment_kind_t_idx").on(t.kind, t.t),
    uniqueIndex("commitment_one_time_spent_per_block")
      .on(t.timeBlockId)
      .where(sql`kind = 'time_spent'`),
  ],
);

// Cycle member: intent_id / commitment_id FKs added in migrate.ts raw SQL.
export const timeBlock = pgTable(
  "time_block",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    tStart: timestamp("t_start", { withTimezone: true }).notNull(),
    tEnd: timestamp("t_end", { withTimezone: true }),
    pausedAt: timestamp("paused_at", { withTimezone: true }),
    origin: blockOrigin("origin").notNull(),
    intentId: uuid("intent_id"),
    calendarId: text("calendar_id"),
    calendarEventId: text("calendar_event_id"),
    // Instance start: part of the fill lookup key with (calendar_id, calendar_event_id).
    calendarTStart: timestamp("calendar_t_start", { withTimezone: true }),
    title: text("title"),
    note: text("note"),
    idleS: integer("idle_s").notNull().default(0),
    // Denormalized; truth = commitment.
    committed: boolean("committed").notNull().default(false),
    commitmentId: uuid("commitment_id"),
    confidence: real("confidence"),
    createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
  },
  (t) => [
    index("time_block_day_idx").on(t.tStart),
    uniqueIndex("time_block_one_open").on(sql`(true)`).where(sql`t_end is null`),
    index("time_block_uncommitted_idx").on(t.tStart).where(sql`committed = false`),
    index("time_block_calendar_event_idx")
      .on(t.calendarId, t.calendarEventId, t.calendarTStart)
      .where(sql`origin = 'calendar' and calendar_event_id is not null`),
  ],
);

export const timeBlockObservation = pgTable(
  "time_block_observation",
  {
    blockId: uuid("block_id")
      .notNull()
      .references(() => timeBlock.id, { onDelete: "cascade" }),
    observationId: uuid("observation_id")
      .notNull()
      .references(() => observation.id, { onDelete: "cascade" }),
  },
  (t) => [index("time_block_observation_pk").on(t.blockId, t.observationId)],
);

export const attributionRule = pgTable("attribution_rule", {
  id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
  matchDevice: text("match_device"),
  // Exact or */? glob (matched in JS, not SQL).
  matchApp: text("match_app"),
  // POSIX regex evaluated with ~* (case-insensitive). Validated on write (must
  // compile, ≤200 chars); evaluated under statement_timeout=2s.
  matchTitle: text("match_title"),
  // * glob on origin+path (matched in JS).
  matchUrl: text("match_url"),
  intentId: uuid("intent_id")
    .notNull()
    .references(() => intent.id, { onDelete: "cascade" }),
  autoApply: boolean("auto_apply").notNull().default(false),
  priority: integer("priority").notNull().default(0),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const googleSyncState = pgTable("google_sync_state", {
  calendarId: text("calendar_id").primaryKey(),
  summary: text("summary"),
  lastPolledAt: timestamp("last_polled_at", { withTimezone: true }),
  lastError: text("last_error"),
  etag: text("etag"),
});

export const googleEvent = pgTable(
  "google_event",
  {
    // gog instance id. Recurring instances MUST differ here; if gog ever
    // returns a shared id, the PK's t_start keeps instances distinct.
    eventId: text("event_id").notNull(),
    calendarId: text("calendar_id")
      .notNull()
      .references(() => googleSyncState.calendarId, { onDelete: "cascade" }),
    icalUid: text("ical_uid"),
    title: text("title"),
    // Bounds always set (all-day events store their local-day bounds).
    tStart: timestamp("t_start", { withTimezone: true }).notNull(),
    tEnd: timestamp("t_end", { withTimezone: true }).notNull(),
    allDay: boolean("all_day").notNull().default(false),
    status: text("status"),
    htmlLink: text("html_link"),
    payload: jsonb("payload").$type<Record<string, unknown>>().notNull().default({}),
    updatedAt: timestamp("updated_at", { withTimezone: true }).notNull(),
  },
  (t) => [
    primaryKey({ columns: [t.calendarId, t.eventId, t.tStart] }),
    index("google_event_span_idx").on(t.tStart, t.tEnd),
  ],
);

export const conversation = pgTable("conversation", {
  id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const message = pgTable("message", {
  id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
  conversationId: uuid("conversation_id")
    .notNull()
    .references(() => conversation.id, { onDelete: "cascade" }),
  role: text("role").notNull(),
  content: text("content").notNull(),
  toolName: text("tool_name"),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
});

export const jobRun = pgTable(
  "job_run",
  {
    id: uuid("id").primaryKey().$defaultFn(() => uuidv7()),
    name: text("name").notNull(),
    startedAt: timestamp("started_at", { withTimezone: true }).notNull().defaultNow(),
    finishedAt: timestamp("finished_at", { withTimezone: true }),
    status: text("status").notNull(),
    error: text("error"),
    stats: jsonb("stats").$type<Record<string, unknown>>().notNull().default({}),
  },
  (t) => [index("job_run_name_started_idx").on(t.name, t.startedAt)],
);

export const scoreSnapshot = pgTable(
  "score_snapshot",
  {
    intentId: uuid("intent_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    asOf: timestamp("as_of", { withTimezone: true }).notNull(),
    score: real("score").notNull(),
    parts: jsonb("parts").$type<Record<string, unknown>>().notNull(),
  },
  (t) => [index("score_snapshot_pk").on(t.intentId, t.asOf)],
);
