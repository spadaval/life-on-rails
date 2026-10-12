CREATE TYPE "public"."block_origin" AS ENUM('auto_activity', 'calendar', 'manual', 'merged');--> statement-breakpoint
CREATE TYPE "public"."commitment_kind" AS ENUM('habit_completion', 'metric_value', 'time_spent', 'task_done', 'note');--> statement-breakpoint
CREATE TYPE "public"."horizon" AS ENUM('day', 'week');--> statement-breakpoint
CREATE TYPE "public"."intent_status" AS ENUM('active', 'paused', 'later', 'done', 'archived');--> statement-breakpoint
CREATE TYPE "public"."intent_type" AS ENUM('goal', 'project', 'habit', 'task', 'metric');--> statement-breakpoint
CREATE TYPE "public"."link_kind" AS ENUM('measures', 'blocks', 'derived_from');--> statement-breakpoint
CREATE TYPE "public"."metric_kind" AS ENUM('five_point', 'numeric', 'duration_hours', 'derived');--> statement-breakpoint
CREATE TYPE "public"."proposal_kind" AS ENUM('link_intent', 'time_attribution', 'habit_completion', 'metric_value', 'day_plan');--> statement-breakpoint
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
	"observation_ids" uuid[],
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "conversation" (
	"id" uuid PRIMARY KEY NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "google_event" (
	"event_id" text NOT NULL,
	"calendar_id" text NOT NULL,
	"ical_uid" text,
	"title" text,
	"t_start" timestamp with time zone NOT NULL,
	"t_end" timestamp with time zone NOT NULL,
	"all_day" boolean DEFAULT false NOT NULL,
	"status" text,
	"html_link" text,
	"payload" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"updated_at" timestamp with time zone NOT NULL,
	CONSTRAINT "google_event_calendar_id_event_id_t_start_pk" PRIMARY KEY("calendar_id","event_id","t_start")
);
--> statement-breakpoint
CREATE TABLE "google_sync_state" (
	"calendar_id" text PRIMARY KEY NOT NULL,
	"summary" text,
	"last_polled_at" timestamp with time zone,
	"last_error" text,
	"etag" text
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
	"archived_at" timestamp with time zone
);
--> statement-breakpoint
CREATE TABLE "intent_link" (
	"id" uuid PRIMARY KEY NOT NULL,
	"from_id" uuid NOT NULL,
	"to_id" uuid NOT NULL,
	"kind" "link_kind" NOT NULL,
	"weight" real DEFAULT 1 NOT NULL,
	"meta" jsonb DEFAULT '{}'::jsonb NOT NULL
);
--> statement-breakpoint
CREATE TABLE "intent_tag" (
	"intent_id" uuid NOT NULL,
	"tag_id" uuid NOT NULL
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
CREATE TABLE "message" (
	"id" uuid PRIMARY KEY NOT NULL,
	"conversation_id" uuid NOT NULL,
	"role" text NOT NULL,
	"content" text NOT NULL,
	"tool_name" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
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
	"kind" "proposal_kind" NOT NULL,
	"status" "proposal_status" DEFAULT 'pending' NOT NULL,
	"confidence" real NOT NULL,
	"time_block_id" uuid,
	"intent_id" uuid,
	"payload" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"reason" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"resolved_at" timestamp with time zone
);
--> statement-breakpoint
CREATE TABLE "score_snapshot" (
	"intent_id" uuid NOT NULL,
	"as_of" timestamp with time zone NOT NULL,
	"score" real NOT NULL,
	"parts" jsonb NOT NULL
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
	"calendar_t_start" timestamp with time zone,
	"title" text,
	"note" text,
	"idle_s" integer DEFAULT 0 NOT NULL,
	"committed" boolean DEFAULT false NOT NULL,
	"commitment_id" uuid,
	"confidence" real,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "time_block_observation" (
	"block_id" uuid NOT NULL,
	"observation_id" uuid NOT NULL
);
--> statement-breakpoint
CREATE TABLE "title_embedding" (
	"id" uuid PRIMARY KEY NOT NULL,
	"app" text,
	"title" text NOT NULL,
	"embedding" vector(384) NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "user_profile" (
	"id" boolean PRIMARY KEY DEFAULT true NOT NULL,
	"display_name" text DEFAULT 'me' NOT NULL,
	"timezone" text DEFAULT 'America/Los_Angeles' NOT NULL,
	"scoring_weeks" integer DEFAULT 2 NOT NULL,
	"wake_start" time DEFAULT '08:00' NOT NULL,
	"wake_end" time DEFAULT '22:00' NOT NULL,
	"idle_threshold_s" integer DEFAULT 180 NOT NULL,
	"xai_model" text DEFAULT 'grok-4.7' NOT NULL,
	"outbound_enabled" boolean DEFAULT false NOT NULL
);
--> statement-breakpoint
ALTER TABLE "attribution_rule" ADD CONSTRAINT "attribution_rule_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "google_event" ADD CONSTRAINT "google_event_calendar_id_google_sync_state_calendar_id_fk" FOREIGN KEY ("calendar_id") REFERENCES "public"."google_sync_state"("calendar_id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_link" ADD CONSTRAINT "intent_link_from_id_intent_id_fk" FOREIGN KEY ("from_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_link" ADD CONSTRAINT "intent_link_to_id_intent_id_fk" FOREIGN KEY ("to_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_tag" ADD CONSTRAINT "intent_tag_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "intent_tag" ADD CONSTRAINT "intent_tag_tag_id_tag_id_fk" FOREIGN KEY ("tag_id") REFERENCES "public"."tag"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "message" ADD CONSTRAINT "message_conversation_id_conversation_id_fk" FOREIGN KEY ("conversation_id") REFERENCES "public"."conversation"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "score_snapshot" ADD CONSTRAINT "score_snapshot_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
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
CREATE INDEX "intent_tag_pk" ON "intent_tag" USING btree ("intent_id","tag_id");--> statement-breakpoint
CREATE INDEX "job_run_name_started_idx" ON "job_run" USING btree ("name","started_at");--> statement-breakpoint
CREATE INDEX "observation_time_idx" ON "observation" USING btree ("t_start");--> statement-breakpoint
CREATE INDEX "observation_source_time_idx" ON "observation" USING btree ("source","t_start");--> statement-breakpoint
CREATE INDEX "proposal_pending_idx" ON "proposal" USING btree ("status","created_at") WHERE status = 'pending';--> statement-breakpoint
CREATE INDEX "proposal_block_idx" ON "proposal" USING btree ("time_block_id") WHERE time_block_id is not null;--> statement-breakpoint
CREATE INDEX "score_snapshot_pk" ON "score_snapshot" USING btree ("intent_id","as_of");--> statement-breakpoint
CREATE INDEX "time_block_day_idx" ON "time_block" USING btree ("t_start");--> statement-breakpoint
CREATE UNIQUE INDEX "time_block_one_open" ON "time_block" USING btree ((true)) WHERE t_end is null;--> statement-breakpoint
CREATE INDEX "time_block_uncommitted_idx" ON "time_block" USING btree ("t_start") WHERE committed = false;--> statement-breakpoint
CREATE INDEX "time_block_calendar_event_idx" ON "time_block" USING btree ("calendar_id","calendar_event_id","calendar_t_start") WHERE origin = 'calendar' and calendar_event_id is not null;--> statement-breakpoint
CREATE INDEX "time_block_observation_pk" ON "time_block_observation" USING btree ("block_id","observation_id");--> statement-breakpoint
CREATE UNIQUE INDEX "title_embedding_app_title" ON "title_embedding" USING btree ("app","title");