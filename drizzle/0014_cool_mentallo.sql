CREATE TABLE "money_account" (
	"id" text PRIMARY KEY NOT NULL,
	"item_id" text NOT NULL,
	"name" text NOT NULL,
	"official_name" text,
	"mask" text,
	"type" text NOT NULL,
	"subtype" text,
	"current" numeric(14, 2),
	"available" numeric(14, 2),
	"credit_limit" numeric(14, 2),
	"currency" text,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "money_item" (
	"id" text PRIMARY KEY NOT NULL,
	"institution_id" text,
	"institution" text NOT NULL,
	"cursor" text,
	"error" text,
	"linked_at" timestamp with time zone DEFAULT now() NOT NULL,
	"synced_at" timestamp with time zone
);
--> statement-breakpoint
CREATE TABLE "money_txn" (
	"id" text PRIMARY KEY NOT NULL,
	"account_id" text NOT NULL,
	"date" date NOT NULL,
	"authorized_date" date,
	"at" timestamp with time zone,
	"amount" numeric(14, 2) NOT NULL,
	"currency" text,
	"name" text NOT NULL,
	"merchant" text,
	"category" text,
	"category_detail" text,
	"pending" boolean DEFAULT false NOT NULL,
	"channel" text,
	"payload" jsonb DEFAULT '{}'::jsonb NOT NULL,
	"updated_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "money_account" ADD CONSTRAINT "money_account_item_id_money_item_id_fk" FOREIGN KEY ("item_id") REFERENCES "public"."money_item"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "money_txn" ADD CONSTRAINT "money_txn_account_id_money_account_id_fk" FOREIGN KEY ("account_id") REFERENCES "public"."money_account"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "money_account_item_idx" ON "money_account" USING btree ("item_id");--> statement-breakpoint
CREATE INDEX "money_txn_date_idx" ON "money_txn" USING btree ("date");--> statement-breakpoint
CREATE INDEX "money_txn_account_date_idx" ON "money_txn" USING btree ("account_id","date");