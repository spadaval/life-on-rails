CREATE TABLE "wall_pin" (
	"id" boolean PRIMARY KEY DEFAULT true NOT NULL,
	"text" text NOT NULL,
	"why" text,
	"until" timestamp with time zone,
	"source_ref" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "wall_pin_one_row" CHECK ("wall_pin"."id")
);
--> statement-breakpoint
ALTER TABLE "intent" ADD COLUMN "short_title" text;