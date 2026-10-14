// Drizzle schema. Source for `pnpm db:generate`; queries are written as SQL
// with postgres.js, so this file exists for migrations, not for an ORM layer.
//
// Deletes (spec: Data model):
//   RESTRICT  commitment.intent_id, commitment.time_block_id, time_block.intent_id
//   CASCADE   proposal.time_block_id
//   SET NULL  proposal.intent_id, commitment.proposal_id, time_block.commitment_id,
//             observation.intent_hint
// proposal → time_block → commitment → proposal is a cycle; drizzle-kit emits
// the constraints after all three tables exist.
import {
  type AnyPgColumn,
  boolean,
  check,
  customType,
  date,
  index,
  integer,
  jsonb,
  pgEnum,
  pgTable,
  primaryKey,
  real,
  text,
  timestamp,
  uniqueIndex,
  uuid,
} from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";

const ts = (name: string) => timestamp(name, { withTimezone: true });
const tsvector = customType<{ data: string }>({ dataType: () => "tsvector" });

export const intentType = pgEnum("intent_type", ["goal", "project", "habit", "task", "metric"]);
// `waiting` = a task blocked on someone else. `later` = hidden by Reset Visibility.
export const intentStatus = pgEnum("intent_status", ["active", "waiting", "paused", "later", "done", "archived"]);
export const horizon = pgEnum("horizon", ["day", "week"]);
export const metricKind = pgEnum("metric_kind", ["five_point", "numeric", "duration_hours", "derived"]);
export const linkKind = pgEnum("link_kind", ["measures", "blocks", "derived_from"]);
export const proposalKind = pgEnum("proposal_kind", ["time_attribution"]);
export const proposalStatus = pgEnum("proposal_status", ["pending", "accepted", "rejected", "expired"]);
export const commitmentKind = pgEnum("commitment_kind", [
  "habit_completion",
  "metric_value",
  "time_spent",
  "task_done",
  "note",
]);
export const blockOrigin = pgEnum("block_origin", ["auto_activity", "calendar", "manual", "merged"]);

export const intent = pgTable(
  "intent",
  {
    id: uuid("id").primaryKey(),
    type: intentType("type").notNull(),
    title: text("title").notNull(),
    description: text("description"),
    status: intentStatus("status").notNull().default("active"),
    weight: real("weight").notNull().default(1),
    weeklyHourTarget: real("weekly_hour_target"),
    frequency: integer("frequency"),
    frequencyHorizon: horizon("frequency_horizon").default("day"),
    // Habit mix α only.
    completionWeight: real("completion_weight").default(0.7),
    // Goal mix β (children vs hours). Not completion_weight.
    goalMix: real("goal_mix").default(0.7),
    // Uncategorized / Meetings: cannot be archived or deleted.
    system: boolean("system").notNull().default(false),
    metricKind: metricKind("metric_kind"),
    metricMin: real("metric_min"),
    metricMax: real("metric_max"),
    estimateHours: real("estimate_hours"),
    dueAt: ts("due_at"),
    snoozedUntil: ts("snoozed_until"),
    // status=waiting: who owes the next move, and since when.
    waitingOnEntityId: uuid("waiting_on_entity_id").references((): AnyPgColumn => entity.id, { onDelete: "set null" }),
    waitingSince: ts("waiting_since"),
    // Set for tasks imported from Google Tasks. A column, not meta: code branches on it.
    googleTaskId: text("google_task_id"),
    // Tasks: who is doing it. `agent` is all that delegated work is.
    assignee: text("assignee").notNull().default("owner"),
    threadRef: text("thread_ref"), // the T3 thread working an agent-assigned task
    // Tasks: the agent asked for the owner's attention ("Needs you"), and why.
    // notified_at: when the phone was handed it, so it notifies once.
    flaggedAt: ts("flagged_at"),
    flagNote: text("flag_note"),
    notifiedAt: ts("notified_at"),
    via: text("via").notNull().default("ui"), // ui | agent | job: who created it
    sourceRef: text("source_ref"),
    meta: jsonb("meta").notNull().default({}),
    createdAt: ts("created_at").notNull().defaultNow(),
    updatedAt: ts("updated_at").notNull().defaultNow(),
    archivedAt: ts("archived_at"),
  },
  (t) => [
    index("intent_type_status_idx").on(t.type, t.status),
    uniqueIndex("intent_google_task_idx").on(t.googleTaskId).where(sql`google_task_id is not null`),
    check("intent_weight_nonneg", sql`${t.weight} >= 0`),
    check("intent_assignee_known", sql`${t.assignee} IN ('owner', 'agent')`),
  ],
);

// from_id = child / source, to_id = parent / target.
export const intentLink = pgTable(
  "intent_link",
  {
    id: uuid("id").primaryKey(),
    fromId: uuid("from_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    toId: uuid("to_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    kind: linkKind("kind").notNull(),
    weight: real("weight").notNull().default(1),
    meta: jsonb("meta").notNull().default({}),
    via: text("via").notNull().default("ui"),
    sourceRef: text("source_ref"),
    createdAt: ts("created_at").notNull().defaultNow(),
  },
  (t) => [
    uniqueIndex("intent_link_unique").on(t.fromId, t.toId, t.kind),
    index("intent_link_to_idx").on(t.toId),
    index("intent_link_from_idx").on(t.fromId),
    check("intent_link_weight_nonneg", sql`${t.weight} >= 0`),
    check("intent_link_not_self", sql`${t.fromId} <> ${t.toId}`),
  ],
);

export const tag = pgTable("tag", {
  id: uuid("id").primaryKey(),
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
  (t) => [primaryKey({ columns: [t.intentId, t.tagId] })],
);

export const observation = pgTable(
  "observation",
  {
    id: uuid("id").primaryKey(),
    tStart: ts("t_start").notNull(),
    tEnd: ts("t_end"),
    device: text("device").notNull(),
    source: text("source").notNull(),
    app: text("app"),
    calendarId: text("calendar_id"),
    title: text("title"),
    url: text("url"),
    payload: jsonb("payload").notNull().default({}),
    intentHint: uuid("intent_hint").references(() => intent.id, { onDelete: "set null" }),
    ingestedAt: ts("ingested_at").notNull().defaultNow(),
    search: tsvector("search").generatedAlwaysAs(sql`to_tsvector('english', coalesce(title, ''))`),
  },
  (t) => [
    index("observation_time_idx").on(t.tStart),
    index("observation_source_time_idx").on(t.source, t.tStart),
    index("observation_ingested_idx").on(t.ingestedAt),
    index("observation_search_idx").using("gin", t.search),
  ],
);

export const proposal = pgTable(
  "proposal",
  {
    id: uuid("id").primaryKey(),
    kind: proposalKind("kind").notNull().default("time_attribution"),
    status: proposalStatus("status").notNull().default("pending"),
    confidence: real("confidence").notNull(),
    timeBlockId: uuid("time_block_id")
      .notNull()
      .references((): AnyPgColumn => timeBlock.id, { onDelete: "cascade" }),
    // Null only after the intent was deleted; a live proposal always has one.
    intentId: uuid("intent_id").references(() => intent.id, { onDelete: "set null" }),
    payload: jsonb("payload").notNull().default({}),
    reason: text("reason"),
    via: text("via").notNull().default("ui"), // ui | agent | rule | job
    sourceRef: text("source_ref"), // for agent writes, the T3 thread id
    createdAt: ts("created_at").notNull().defaultNow(),
    resolvedAt: ts("resolved_at"),
  },
  (t) => [
    index("proposal_pending_idx").on(t.status, t.createdAt).where(sql`status = 'pending'`),
    index("proposal_block_idx").on(t.timeBlockId),
    check("proposal_confidence_range", sql`${t.confidence} BETWEEN 0 AND 1`),
  ],
);

export const commitment = pgTable(
  "commitment",
  {
    id: uuid("id").primaryKey(),
    kind: commitmentKind("kind").notNull(),
    intentId: uuid("intent_id")
      .notNull()
      .references(() => intent.id, { onDelete: "restrict" }),
    t: ts("t").notNull(),
    tEnd: ts("t_end"),
    // Display-only for time_spent; the scorer recomputes from [t, t_end].
    value: real("value"),
    memo: text("memo"),
    proposalId: uuid("proposal_id").references((): AnyPgColumn => proposal.id, { onDelete: "set null" }),
    timeBlockId: uuid("time_block_id").references((): AnyPgColumn => timeBlock.id, { onDelete: "restrict" }),
    via: text("via").notNull().default("ui"),
    sourceRef: text("source_ref"),
    createdAt: ts("created_at").notNull().defaultNow(),
  },
  (t) => [
    index("commitment_intent_t_idx").on(t.intentId, t.t),
    index("commitment_kind_t_idx").on(t.kind, t.t),
    uniqueIndex("commitment_one_time_spent_per_block").on(t.timeBlockId).where(sql`kind = 'time_spent'`),
  ],
);

export const timeBlock = pgTable(
  "time_block",
  {
    id: uuid("id").primaryKey(),
    tStart: ts("t_start").notNull(),
    tEnd: ts("t_end"), // null = open (running or paused)
    pausedAt: ts("paused_at"),
    origin: blockOrigin("origin").notNull(),
    intentId: uuid("intent_id").references(() => intent.id, { onDelete: "restrict" }),
    calendarId: text("calendar_id"),
    calendarEventId: text("calendar_event_id"), // several fragments of one event share it
    title: text("title"),
    note: text("note"),
    idleS: integer("idle_s").notNull().default(0),
    // Denormalized; the truth is `commitment`.
    committed: boolean("committed").notNull().default(false),
    commitmentId: uuid("commitment_id").references((): AnyPgColumn => commitment.id, { onDelete: "set null" }),
    createdAt: ts("created_at").notNull().defaultNow(),
    updatedAt: ts("updated_at").notNull().defaultNow(),
  },
  (t) => [
    index("time_block_day_idx").on(t.tStart),
    uniqueIndex("time_block_one_open").on(sql`(true)`).where(sql`t_end is null`),
    index("time_block_calendar_event_idx")
      .on(t.calendarId, t.calendarEventId)
      .where(sql`origin = 'calendar'`),
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
  (t) => [primaryKey({ columns: [t.blockId, t.observationId] })],
);

export const attributionRule = pgTable("attribution_rule", {
  id: uuid("id").primaryKey(),
  matchDevice: text("match_device"),
  matchApp: text("match_app"), // exact, or a * / ? glob
  matchTitle: text("match_title"), // case-insensitive regex; ≤200 chars, must compile
  matchUrl: text("match_url"), // * glob on origin + path
  intentId: uuid("intent_id")
    .notNull()
    .references(() => intent.id, { onDelete: "cascade" }),
  autoApply: boolean("auto_apply").notNull().default(false),
  priority: integer("priority").notNull().default(0),
  createdAt: ts("created_at").notNull().defaultNow(),
});

export const googleSyncState = pgTable("google_sync_state", {
  calendarId: text("calendar_id").primaryKey(),
  summary: text("summary"),
  lastPolledAt: ts("last_polled_at"),
  lastError: text("last_error"),
});

export const googleEvent = pgTable(
  "google_event",
  {
    calendarId: text("calendar_id")
      .notNull()
      .references(() => googleSyncState.calendarId, { onDelete: "cascade" }),
    eventId: text("event_id").notNull(), // the instance id; recurring instances differ
    title: text("title"),
    tStart: ts("t_start").notNull(), // all-day events store their local-day bounds
    tEnd: ts("t_end").notNull(),
    allDay: boolean("all_day").notNull().default(false),
    status: text("status").notNull().default("confirmed"), // confirmed | tentative | cancelled
    // What decides whether an event fills the actual lane (see calendarSync.fillsActual).
    transparency: text("transparency").notNull().default("opaque"), // opaque | transparent ("free")
    eventType: text("event_type").notNull().default("default"),
    selfResponse: text("self_response"), // the owner's RSVP, when the event has attendees
    // The other attendees' addresses, lowercased: who the owner met.
    attendees: text("attendees").array().notNull().default(sql`'{}'::text[]`),
    htmlLink: text("html_link"),
    payload: jsonb("payload").notNull().default({}),
    updatedAt: ts("updated_at").notNull().defaultNow(),
  },
  (t) => [primaryKey({ columns: [t.calendarId, t.eventId] }), index("google_event_span_idx").on(t.tStart, t.tEnd)],
);

export const jobRun = pgTable(
  "job_run",
  {
    id: uuid("id").primaryKey(),
    name: text("name").notNull(),
    startedAt: ts("started_at").notNull().defaultNow(),
    finishedAt: ts("finished_at"),
    status: text("status").notNull(), // running | ok | error
    error: text("error"),
    stats: jsonb("stats").notNull().default({}),
  },
  (t) => [index("job_run_name_started_idx").on(t.name, t.startedAt)],
);

export const scoreSnapshot = pgTable(
  "score_snapshot",
  {
    intentId: uuid("intent_id")
      .notNull()
      .references(() => intent.id, { onDelete: "cascade" }),
    asOf: date("as_of").notNull(), // local date
    score: real("score"), // null = undefined (nothing to score yet)
    parts: jsonb("parts").notNull(), // what `lor score explain` shows
  },
  (t) => [primaryKey({ columns: [t.intentId, t.asOf] })],
);

// ---- Memory: entities and the wiki ----

export const entity = pgTable(
  "entity",
  {
    id: uuid("id").primaryKey(),
    kind: text("kind").notNull(), // person | org | place | thing
    name: text("name").notNull(),
    aliases: text("aliases").array().notNull().default(sql`'{}'::text[]`),
    // Mail from this entity skips the wake debounce.
    priority: boolean("priority").notNull().default(false),
    // Set for people imported from Google Contacts ("people/c123..."). Google is the source for their name and birthday.
    googleContactId: text("google_contact_id").unique(),
    // Birthday: month and day, year when known.
    birthdayMonth: integer("birthday_month"),
    birthdayDay: integer("birthday_day"),
    birthdayYear: integer("birthday_year"),
    // Keep in touch at least this often; null = no expectation.
    keepInTouchDays: integer("keep_in_touch_days"),
    createdAt: ts("created_at").notNull().defaultNow(),
    updatedAt: ts("updated_at").notNull().defaultNow(),
  },
  (t) => [
    index("entity_name_trgm").using("gin", t.name.op("gin_trgm_ops")),
    check("entity_kind_known", sql`${t.kind} IN ('person', 'org', 'place', 'thing')`),
    check("entity_birthday_valid", sql`(${t.birthdayMonth} IS NULL) = (${t.birthdayDay} IS NULL) AND (${t.birthdayMonth} IS NULL OR (${t.birthdayMonth} BETWEEN 1 AND 12 AND ${t.birthdayDay} BETWEEN 1 AND 31))`),
    check("entity_keep_in_touch_positive", sql`${t.keepInTouchDays} IS NULL OR ${t.keepInTouchDays} > 0`),
  ],
);

// Where an incremental sync got to (the Google Contacts sync token).
export const syncCursor = pgTable("sync_cursor", {
  name: text("name").primaryKey(),
  token: text("token"),
  updatedAt: ts("updated_at").notNull().defaultNow(),
});

// How an entity is recognised in raw data (a mail From:, a calendar attendee).
export const entityIdentifier = pgTable(
  "entity_identifier",
  {
    entityId: uuid("entity_id")
      .notNull()
      .references(() => entity.id, { onDelete: "cascade" }),
    kind: text("kind").notNull(), // email | phone | handle | domain
    value: text("value").notNull(), // lowercased
  },
  (t) => [primaryKey({ columns: [t.kind, t.value] }), index("entity_identifier_entity_idx").on(t.entityId)],
);

// One page per subject. Plain markdown, complete in itself.
export const page = pgTable(
  "page",
  {
    id: uuid("id").primaryKey(),
    slug: text("slug").notNull().unique(),
    title: text("title").notNull(),
    entityId: uuid("entity_id")
      .unique()
      .references(() => entity.id, { onDelete: "set null" }),
    intentId: uuid("intent_id")
      .unique()
      .references(() => intent.id, { onDelete: "set null" }),
    headVersion: integer("head_version").notNull(),
    body: text("body").notNull(), // copy of the head version, kept here for search
    inDigest: boolean("in_digest").notNull().default(false), // included in every `lor context`
    search: tsvector("search").generatedAlwaysAs(
      sql`setweight(to_tsvector('english', title), 'A') || to_tsvector('english', body)`,
    ),
    createdAt: ts("created_at").notNull().defaultNow(),
    updatedAt: ts("updated_at").notNull().defaultNow(),
  },
  (t) => [
    index("page_search_idx").using("gin", t.search),
    index("page_title_trgm").using("gin", t.title.op("gin_trgm_ops")),
    check("page_slug_shape", sql`${t.slug} ~ '^[a-z0-9][a-z0-9/-]*$'`),
    check("page_one_subject", sql`num_nonnulls(${t.entityId}, ${t.intentId}) <= 1`),
  ],
);

// Every write is a whole new copy, so history, diff, and revert need no machinery.
export const pageVersion = pgTable(
  "page_version",
  {
    pageId: uuid("page_id")
      .notNull()
      .references(() => page.id, { onDelete: "cascade" }),
    version: integer("version").notNull(),
    title: text("title").notNull(),
    body: text("body").notNull(),
    via: text("via").notNull(), // ui | agent | seed
    sourceRef: text("source_ref"), // T3 thread id
    note: text("note"), // one-line edit summary
    createdAt: ts("created_at").notNull().defaultNow(),
  },
  (t) => [primaryKey({ columns: [t.pageId, t.version] })],
);

// ---- Mail: notifications, not a mirror ----

// One row: where the last check got to, and the health of the push path.
export const mailState = pgTable(
  "mail_state",
  {
    id: boolean("id").primaryKey().default(true),
    historyId: text("history_id"),
    lastCheckAt: ts("last_check_at"),
  },
  (t) => [check("mail_state_one_row", sql`${t.id}`)],
);

// Arrivals waiting to wake the agent, one row per Gmail thread.
export const wakeQueue = pgTable("wake_queue", {
  gmailThreadId: text("gmail_thread_id").primaryKey(),
  observationIds: uuid("observation_ids").array().notNull(),
  firstSeen: ts("first_seen").notNull(),
  lastSeen: ts("last_seen").notNull(),
  priority: boolean("priority").notNull().default(false),
});
