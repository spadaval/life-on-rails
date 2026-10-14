ALTER TABLE "google_event" ADD COLUMN "transparency" text DEFAULT 'opaque' NOT NULL;--> statement-breakpoint
ALTER TABLE "google_event" ADD COLUMN "event_type" text DEFAULT 'default' NOT NULL;--> statement-breakpoint
ALTER TABLE "google_event" ADD COLUMN "self_response" text;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "waiting_on_entity_id" uuid;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "waiting_since" timestamp with time zone;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "google_task_id" text;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "assignee" text DEFAULT 'owner' NOT NULL;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "thread_ref" text;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "via" text DEFAULT 'ui' NOT NULL;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "source_ref" text;--> statement-breakpoint
ALTER TABLE "intent" ADD CONSTRAINT "intent_waiting_on_entity_id_entity_id_fk" FOREIGN KEY ("waiting_on_entity_id") REFERENCES "public"."entity"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "intent_google_task_idx" ON "intent" USING btree ("google_task_id") WHERE google_task_id is not null;--> statement-breakpoint
ALTER TABLE "intent" ADD CONSTRAINT "intent_assignee_known" CHECK ("intent"."assignee" IN ('owner', 'agent'));