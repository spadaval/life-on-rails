ALTER TABLE "intent" ADD COLUMN "flagged_at" timestamp with time zone;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "flag_note" text;--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "notified_at" timestamp with time zone;