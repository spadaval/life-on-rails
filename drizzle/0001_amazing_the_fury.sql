CREATE EXTENSION IF NOT EXISTS pg_trgm;--> statement-breakpoint
CREATE TABLE "entity" (
	"id" uuid PRIMARY KEY NOT NULL,
	"kind" text NOT NULL,
	"name" text NOT NULL,
	"aliases" text[] DEFAULT '{}'::text[] NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "entity_kind_known" CHECK ("entity"."kind" IN ('person', 'org', 'place', 'thing'))
);
--> statement-breakpoint
CREATE TABLE "entity_identifier" (
	"entity_id" uuid NOT NULL,
	"kind" text NOT NULL,
	"value" text NOT NULL,
	CONSTRAINT "entity_identifier_kind_value_pk" PRIMARY KEY("kind","value")
);
--> statement-breakpoint
CREATE TABLE "page" (
	"id" uuid PRIMARY KEY NOT NULL,
	"slug" text NOT NULL,
	"title" text NOT NULL,
	"entity_id" uuid,
	"intent_id" uuid,
	"head_version" integer NOT NULL,
	"body" text NOT NULL,
	"in_digest" boolean DEFAULT false NOT NULL,
	"search" "tsvector" GENERATED ALWAYS AS (setweight(to_tsvector('english', title), 'A') || to_tsvector('english', body)) STORED,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "page_slug_unique" UNIQUE("slug"),
	CONSTRAINT "page_entity_id_unique" UNIQUE("entity_id"),
	CONSTRAINT "page_intent_id_unique" UNIQUE("intent_id"),
	CONSTRAINT "page_slug_shape" CHECK ("page"."slug" ~ '^[a-z0-9][a-z0-9/-]*$'),
	CONSTRAINT "page_one_subject" CHECK (num_nonnulls("page"."entity_id", "page"."intent_id") <= 1)
);
--> statement-breakpoint
CREATE TABLE "page_version" (
	"page_id" uuid NOT NULL,
	"version" integer NOT NULL,
	"title" text NOT NULL,
	"body" text NOT NULL,
	"via" text NOT NULL,
	"source_ref" text,
	"note" text,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "page_version_page_id_version_pk" PRIMARY KEY("page_id","version")
);
--> statement-breakpoint
ALTER TABLE "observation" ADD COLUMN "search" "tsvector" GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, ''))) STORED;--> statement-breakpoint
ALTER TABLE "entity_identifier" ADD CONSTRAINT "entity_identifier_entity_id_entity_id_fk" FOREIGN KEY ("entity_id") REFERENCES "public"."entity"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "page" ADD CONSTRAINT "page_entity_id_entity_id_fk" FOREIGN KEY ("entity_id") REFERENCES "public"."entity"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "page" ADD CONSTRAINT "page_intent_id_intent_id_fk" FOREIGN KEY ("intent_id") REFERENCES "public"."intent"("id") ON DELETE set null ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "page_version" ADD CONSTRAINT "page_version_page_id_page_id_fk" FOREIGN KEY ("page_id") REFERENCES "public"."page"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "entity_name_trgm" ON "entity" USING gin ("name" gin_trgm_ops);--> statement-breakpoint
CREATE INDEX "entity_identifier_entity_idx" ON "entity_identifier" USING btree ("entity_id");--> statement-breakpoint
CREATE INDEX "page_search_idx" ON "page" USING gin ("search");--> statement-breakpoint
CREATE INDEX "page_title_trgm" ON "page" USING gin ("title" gin_trgm_ops);--> statement-breakpoint
CREATE INDEX "observation_search_idx" ON "observation" USING gin ("search");