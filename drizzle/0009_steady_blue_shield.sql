ALTER TABLE "observation" drop column "search";--> statement-breakpoint
ALTER TABLE "observation" ADD COLUMN "search" "tsvector" GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(payload->>'text', ''))) STORED;--> statement-breakpoint
CREATE INDEX "observation_search_idx" ON "observation" USING gin ("search");
