ALTER TYPE "public"."block_origin" ADD VALUE 'place';--> statement-breakpoint
ALTER TABLE "attribution_rule" ADD COLUMN "match_place" uuid;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "lat" real;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "lon" real;--> statement-breakpoint
ALTER TABLE "entity" ADD COLUMN "radius_m" real;--> statement-breakpoint
ALTER TABLE "attribution_rule" ADD CONSTRAINT "attribution_rule_match_place_entity_id_fk" FOREIGN KEY ("match_place") REFERENCES "public"."entity"("id") ON DELETE cascade ON UPDATE no action;