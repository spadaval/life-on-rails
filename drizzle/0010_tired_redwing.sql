-- Who an observation was with, and who wrote it, as entity identifiers ("kind:value"),
-- for the generated columns below. Changing these needs the columns re-generated
-- (drop and re-add), since stored values are not recomputed.
CREATE FUNCTION observation_parties(source text, payload jsonb) RETURNS text[]
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT CASE
    WHEN source = 'gmail' AND coalesce(payload->>'direction', 'in') = 'sent' THEN
      (SELECT coalesce(array_agg(DISTINCT x ORDER BY x), '{}')
       FROM jsonb_array_elements_text(CASE WHEN jsonb_typeof(payload->'to_emails') = 'array' THEN payload->'to_emails' ELSE '[]' END) t,
            unnest(ARRAY['email:' || lower(t), 'domain:' || split_part(lower(t), '@', 2)]) x)
    WHEN source = 'gmail' AND payload->>'from_email' IS NOT NULL THEN
      ARRAY['email:' || lower(payload->>'from_email'), 'domain:' || split_part(lower(payload->>'from_email'), '@', 2)]
    WHEN source = 'gmail' THEN '{}'
    -- A group message is with the group.
    WHEN payload->>'group' = 'true' THEN
      CASE WHEN payload->>'chat' IS NOT NULL THEN ARRAY['whatsapp_group:' || lower(payload->>'chat')] ELSE '{}' END
    WHEN payload->>'phone' IS NOT NULL THEN ARRAY['phone:' || (payload->>'phone')]
    ELSE '{}'
  END
$$;
--> statement-breakpoint
CREATE FUNCTION observation_author(source text, payload jsonb) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT CASE
    WHEN source = 'gmail' AND coalesce(payload->>'direction', 'in') = 'in' AND payload->>'from_email' IS NOT NULL THEN 'email:' || lower(payload->>'from_email')
    WHEN source <> 'gmail' AND payload->>'direction' = 'in' AND payload->>'phone' IS NOT NULL THEN 'phone:' || (payload->>'phone')
  END
$$;
--> statement-breakpoint
CREATE TABLE "entity_member" (
	"entity_id" uuid NOT NULL,
	"kind" text NOT NULL,
	"value" text NOT NULL,
	CONSTRAINT "entity_member_entity_id_kind_value_pk" PRIMARY KEY("entity_id","kind","value")
);
--> statement-breakpoint
ALTER TABLE "entity" DROP CONSTRAINT "entity_kind_known";--> statement-breakpoint
ALTER TABLE "observation" ADD COLUMN "kind" text GENERATED ALWAYS AS (CASE WHEN source = 'gmail' THEN 'mail' ELSE payload->>'kind' END) STORED;--> statement-breakpoint
ALTER TABLE "observation" ADD COLUMN "parties" text[] GENERATED ALWAYS AS (observation_parties(source, payload)) STORED;--> statement-breakpoint
ALTER TABLE "observation" ADD COLUMN "author" text GENERATED ALWAYS AS (observation_author(source, payload)) STORED;--> statement-breakpoint
ALTER TABLE "entity_member" ADD CONSTRAINT "entity_member_entity_id_entity_id_fk" FOREIGN KEY ("entity_id") REFERENCES "public"."entity"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "entity_member_value_idx" ON "entity_member" USING btree ("kind","value");--> statement-breakpoint
CREATE INDEX "observation_kind_time_idx" ON "observation" USING btree ("kind","t_start");--> statement-breakpoint
CREATE INDEX "observation_parties_idx" ON "observation" USING gin ("parties");--> statement-breakpoint
CREATE INDEX "observation_author_idx" ON "observation" USING btree ("author");--> statement-breakpoint
ALTER TABLE "entity" ADD CONSTRAINT "entity_kind_known" CHECK ("entity"."kind" IN ('person', 'org', 'place', 'thing', 'group'));