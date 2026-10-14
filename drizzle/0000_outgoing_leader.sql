CREATE TYPE "public"."block_origin" AS ENUM('auto_activity', 'calendar', 'manual', 'merged');--> statement-breakpoint
CREATE TYPE "public"."commitment_kind" AS ENUM('habit_completion', 'metric_value', 'time_spent', 'task_done', 'note');--> statement-breakpoint
CREATE TYPE "public"."horizon" AS ENUM('day', 'week');--> statement-breakpoint
CREATE TYPE "public"."intent_status" AS ENUM('active', 'waiting', 'paused', 'later', 'done', 'archived');--> statement-breakpoint
CREATE TYPE "public"."intent_type" AS ENUM('goal', 'project', 'habit', 'task', 'metric');--> statement-breakpoint
CREATE TYPE "public"."link_kind" AS ENUM('measures', 'blocks', 'derived_from');--> statement-breakpoint
CREATE TYPE "public"."metric_kind" AS ENUM('five_point', 'numeric', 'duration_hours', 'derived');--> statement-breakpoint
CREATE TYPE "public"."proposal_kind" AS ENUM('time_attribution');--> statement-breakpoint
CREATE TYPE "public"."proposal_status" AS ENUM('pending', 'accepted', 'rejected', 'expired');--> statement-breakpoint
CREATE TABLE "attribution_rule" (
	"id" uuid PRIMARY KEY NOT NULL,
	"match_device" text,
	"match_app" text,
	"match_title" text,
	"match_url" text,
	"intent_id" uuid NOT NULL,
	"auto_apply" boolean DEFAULT false NOT NULL,
	"priority" integer DEFAULT 0 NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "commitment" (
	"id" uuid PRIMARY KEY NOT NULL,
	"kind" "commitment_kind" NOT NULL,
	"intent_id" uuid NOT NULL,
	"t" timestamp with time zone NOT NULL,
	"t_end" timestamp with time zone,
	"value" real,
	"memo" text,
	"proposal_id" uuid,
	"time_block_id" uuid,
	"via" text DEFAULT 'ui' NOT NULL,
	"source_ref" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "google_event" (
	"calendar_id" text NOT NULL,
	"event_id" text NOT NULL,
	"title" text,
	"t_start" timestamp with time zone NOT NULL,
	"t_end" timestamp with time zone NOT NULL,
	"all_day" boolean DEFAULT false NOT NULL,
	"status" text DEFAULT 'confirmed' NOT NULL,
	"html_link" text,
	"payload" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "google_event_calendar_id_event_id_pk" PRIMARY KEY("calendar_id","event_id")
);
--> statement-breakpoint
CREATE TABLE "google_sync_state" (
	"calendar_id" text PRIMARY KEY NOT NULL,
	"summary" text,
	"last_polled_at" timestamp with time zone,
	"last_error" text
);
--> statement-breakpoint
CREATE TABLE "intent" (
	"id" uuid PRIMARY KEY NOT NULL,
	"type" "intent_type" NOT NULL,
	"title" text NOT NULL,
	"description" text,
	"status" "intent_status" DEFAULT 'active' NOT NULL,
	"weight" real DEFAULT 1 NOT NULL,
	"weekly_hour_target" real,
	"frequency" integer,
	"frequency_horizon" "horizon" DEFAULT 'day',
	"completion_weight" real DEFAULT 0.7,
	"goal_mix" real DEFAULT 0.7,
	"system" boolean DEFAULT false NOT NULL,
	"metric_kind" "metric_kind",
	"metric_min" real,
	"metric_max" real,
	"estimate_hours" real,
	"due_at" timestamp with time zone,
	"snoozed_until" timestamp with time zone,
	"meta" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	"archived_at" timestamp with time zone,
	CONSTRAINT "intent_weight_nonneg" CHECK ("intent"."weight" >= 0)
);
--> statement-breakpoint
CREATE TABLE "intent_link" (
	"id" uuid PRIMARY KEY NOT NULL,
	"from_id" uuid NOT NULL,
	"to_id" uuid NOT NULL,
	"kind" "link_kind" NOT NULL,
	"weight" real DEFAULT 1 NOT NULL,
	"meta" jsonb DEFAULT '{}'::jsonb NOT NULL,
	CONSTRAINT "intent_link_weight_nonneg" CHECK ("intent_link"."weight" >= 0),
	CONSTRAINT "intent_link_not_self" CHECK ("intent_link"."from_id" <> "intent_link"."to_id")
);
--> statement-breakpoint
CREATE TABLE "intent_tag" (
	"intent_id" uuid NOT NULL,
	"tag_id" uuid NOT NULL,
	CONSTRAINT "intent_tag_intent_id_tag_id_pk" PRIMARY KEY("intent_id","tag_id")
);
--> statement-breakpoint
CREATE TABLE "job_run" (
	"id" uuid PRIMARY KEY NOT NULL,
	"name" text NOT NULL,
	"started_at" timestamp with time zone DEFAULT now() NOT NULL,
	"finished_at" timestamp with time zone,
	"status" text NOT NULL,
	"error" text,
	"stats" jsonb DEFAULT '{}'::jsonb NOT NULL
);
--> statement-breakpoint
CREATE TABLE "observation" (
	"id" uuid PRIMARY KEY NOT NULL,
	"t_start" timestamp with time zone NOT NULL,
	"t_end" timestamp with time zone,
	"device" text NOT NULL,
	"source" text NOT NULL,
	"app" text,
	"calendar_id" text,
	"title" text,
	"url" text,
	"payload" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"intent_hint" uuid,
	"ingested_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "proposal" (
	"id" uuid PRIMARY KEY NOT NULL,
	"kind" "proposal_kind" DEFAULT 'time_attribution' NOT NULL,
	"status" "proposal_status" DEFAULT 'pending' NOT NULL,
	"confidence" real NOT NULL,
	"time_block_id" uuid NOT NULL,
	"intent_id" uuid,
	"payload" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"reason" text,
	"via" text DEFAULT 'ui' NOT NULL,
	"source_ref" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"resolved_at" timestamp with time zone,
	CONSTRAINT "proposal_confidence_range" CHECK ("proposal"."confidence" BETWEEN 0 AND 1)
);
--> statement-breakpoint
CREATE TABLE "score_snapshot" (
	"intent_id" uuid NOT NULL,
	"as_of" date NOT NULL,
	"score" real,
	"parts" jsonb NOT NULL,
	CONSTRAINT "score_snapshot_intent_id_as_of_pk" PRIMARY KEY("intent_id","as_of")
);
--> statement-breakpoint
CREATE TABLE "tag" (
	"id" uuid PRIMARY KEY NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT "tag_name_unique" UNIQUE("name")
);
--> statement-breakpoint
CREATE TABLE "time_block" (
	"id" uuid PRIMARY KEY NOT NULL,
	"t_start" timestamp with time zone NOT NULL,
	"t_end" timestamp with time zone,
	"paused_at" timestamp with time zone,
	"origin" "block_origin" NOT NULL,
	"intent_id" uuid,
	"calendar_id" text,
	"calendar_event_id" text,
	"title" text,
	"note" text,
	"idle_s" integer DEFAULT 0 NOT NULL,
	"committed" boolean DEFAULT false NOT NULL,
	"commitment_id" uuid,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "time_block_observation" (
	"block_id" uuid NOT NULL,
	"observation_id" uuid NOT NULL,
	CONSTRAINT "time_block_observation_block_id_observation_id_pk" PRIMARY KEY("block_id","observation_id")
);
--> statement-breakpoint
ALTER TABLE "attribution_rule" ADD CONSTRAINT "attribution_rule_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "commitment" ADD CONSTRAINT "commitment_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE restrict ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "commitment" ADD CONSTRAINT "commitment_proposal_id_proposal_id_fk" FOREIGN KEY ("proposal_id") REFERENCES "public"."proposal"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "commitment" ADD CONSTRAINT "commitment_time_block_id_time_block_id_fk" FOREIGN KEY ("time_block_id") REFERENCES "public"."time_block"("id") ON DELETE restrict ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "google_event" ADD CONSTRAINT "google_event_calendar_id_google_sync_state_calendar_id_fk" FOREIGN KEY ("calendar_id") REFERENCES "public"."google_sync_state"("calendar_id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_link" ADD CONSTRAINT "intent_link_from_id_intent_id_fk" FOREIGN KEY ("from_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_link" ADD CONSTRAINT "intent_link_to_id_intent_id_fk" FOREIGN KEY ("to_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_tag" ADD CONSTRAINT "intent_tag_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_tag" ADD CONSTRAINT "intent_tag_tag_id_tag_id_fk" FOREIGN KEY ("tag_id") REFERENCES "public"."tag"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "observation" ADD CONSTRAINT "observation_intent_hint_intent_id_fk" FOREIGN KEY ("intent_hint") REFERENCES "public"."intent"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "proposal" ADD CONSTRAINT "proposal_time_block_id_time_block_id_fk" FOREIGN KEY ("time_block_id") REFERENCES "public"."time_block"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "proposal" ADD CONSTRAINT "proposal_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "score_snapshot" ADD CONSTRAINT "score_snapshot_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "time_block" ADD CONSTRAINT "time_block_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE restrict ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "time_block" ADD CONSTRAINT "time_block_commitment_id_commitment_id_fk" FOREIGN KEY ("commitment_id") REFERENCES "public"."commitment"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "time_block_observation" ADD CONSTRAINT "time_block_observation_block_id_time_block_id_fk" FOREIGN KEY ("block_id") REFERENCES "public"."time_block"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "time_block_observation" ADD CONSTRAINT "time_block_observation_observation_id_observation_id_fk" FOREIGN KEY ("observation_id") REFERENCES "public"."observation"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "commitment_intent_t_idx" ON "commitment" USING btree ("intent_id","t");--> statement-breakpoint
CREATE INDEX "commitment_kind_t_idx" ON "commitment" USING btree ("kind","t");--> statement-breakpoint
CREATE UNIQUE INDEX "commitment_one_time_spent_per_block" ON "commitment" USING btree ("time_block_id") WHERE kind = 'time_spent';--> statement-breakpoint
CREATE INDEX "google_event_span_idx" ON "google_event" USING btree ("t_start","t_end");--> statement-breakpoint
CREATE INDEX "intent_type_status_idx" ON "intent" USING btree ("type","status");--> statement-breakpoint
CREATE UNIQUE INDEX "intent_link_unique" ON "intent_link" USING btree ("from_id","to_id","kind");--> statement-breakpoint
CREATE INDEX "intent_link_to_idx" ON "intent_link" USING btree ("to_id");--> statement-breakpoint
CREATE INDEX "intent_link_from_idx" ON "intent_link" USING btree ("from_id");--> statement-breakpoint
CREATE INDEX "job_run_name_started_idx" ON "job_run" USING btree ("name","started_at");--> statement-breakpoint
CREATE INDEX "observation_time_idx" ON "observation" USING btree ("t_start");--> statement-breakpoint
CREATE INDEX "observation_source_time_idx" ON "observation" USING btree ("source","t_start");--> statement-breakpoint
CREATE INDEX "observation_ingested_idx" ON "observation" USING btree ("ingested_at");--> statement-breakpoint
CREATE INDEX "proposal_pending_idx" ON "proposal" USING btree ("status","created_at") WHERE status = 'pending';--> statement-breakpoint
CREATE INDEX "proposal_block_idx" ON "proposal" USING btree ("time_block_id");--> statement-breakpoint
CREATE INDEX "time_block_day_idx" ON "time_block" USING btree ("t_start");--> statement-breakpoint
CREATE UNIQUE INDEX "time_block_one_open" ON "time_block" USING btree ((true)) WHERE t_end is null;--> statement-breakpoint
CREATE INDEX "time_block_calendar_event_idx" ON "time_block" USING btree ("calendar_id","calendar_event_id") WHERE origin = 'calendar';