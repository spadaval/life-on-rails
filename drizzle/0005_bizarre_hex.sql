CREATE TABLE "sync_cursor" (
	"name" text PRIMARY KEY NOT NULL,
	"token" text,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "google_contact_id" text;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "birthday_month" integer;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "birthday_day" integer;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "birthday_year" integer;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "keep_in_touch_days" integer;--> statement-breakpoint
ALTER TABLE "google_event" ADD COLUMN "attendees" text[] DEFAULT '{}'::text[] NOT NULL;--> statement-breakpoint
ALTER TABLE "entity" ADD CONSTRAINT "entity_google_contact_id_unique" UNIQUE("google_contact_id");--> statement-breakpoint
ALTER TABLE "entity" ADD CONSTRAINT "entity_birthday_valid" CHECK (("entity"."birthday_month" IS NULL) = ("entity"."birthday_day" IS NULL) AND ("entity"."birthday_month" IS NULL OR ("entity"."birthday_month" BETWEEN 1 AND 12 AND "entity"."birthday_day" BETWEEN 1 AND 31)));--> statement-breakpoint
ALTER TABLE "entity" ADD CONSTRAINT "entity_keep_in_touch_positive" CHECK ("entity"."keep_in_touch_days" IS NULL OR "entity"."keep_in_touch_days" > 0);