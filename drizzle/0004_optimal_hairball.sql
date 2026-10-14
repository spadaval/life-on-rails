CREATE TABLE "mail_state" (
	"id" boolean PRIMARY KEY DEFAULT true NOT NULL,
	"history_id" text,
	"watch_expires_at" timestamp with time zone,
	"last_push_at" timestamp with time zone,
	"last_check_at" timestamp with time zone,
	CONSTRAINT "mail_state_one_row" CHECK ("mail_state"."id")
);
--> statement-breakpoint
CREATE TABLE "wake_queue" (
	"gmail_thread_id" text PRIMARY KEY NOT NULL,
	"observation_ids" uuid[] NOT NULL,
	"first_seen" timestamp with time zone NOT NULL,
	"last_seen" timestamp with time zone NOT NULL,
	"priority" boolean DEFAULT false NOT NULL
);
--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "priority" boolean DEFAULT false NOT NULL;