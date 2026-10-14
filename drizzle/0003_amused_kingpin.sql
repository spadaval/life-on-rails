ALTER TABLE "intent_link" ADD COLUMN "via" text DEFAULT 'ui' NOT NULL;--> statement-breakpoint
ALTER TABLE "intent_link" ADD COLUMN "source_ref" text;--> statement-breakpoint
ALTER TABLE "intent_link" ADD COLUMN "created_at" timestamp with time zone DEFAULT now() NOT NULL;