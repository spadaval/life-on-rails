// Plaid, the money sensor. LoR calls Plaid's API directly with the team's keys
// from the env file. Each linked login (an Item) has an access token, kept in
// PLAID_ITEMS_FILE (0600) by item id, never in a table or a log.
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { z } from "zod";
import { env } from "../env.js";

/** Plaid answered with an error. `code` is Plaid's error_code (ITEM_LOGIN_REQUIRED, ...). */
export class PlaidError extends Error {
  constructor(
    public code: string,
    message: string,
  ) {
    super(`Plaid ${code}: ${message}`);
    this.name = "PlaidError";
  }
}

export function plaidConfigured(): boolean {
  return env.PLAID_CLIENT_ID !== "" && env.PLAID_SECRET !== "";
}

// ---- Plaid's shapes (only what LoR reads; the rest stays in the payload) ----

export const PAccount = z.object({
  account_id: z.string(),
  name: z.string(),
  official_name: z.string().nullish(),
  mask: z.string().nullish(),
  type: z.string(),
  subtype: z.string().nullish(),
  balances: z.object({
    current: z.number().nullish(),
    available: z.number().nullish(),
    limit: z.number().nullish(),
    iso_currency_code: z.string().nullish(),
    unofficial_currency_code: z.string().nullish(),
  }),
});
export type PAccount = z.infer<typeof PAccount>;

export const PTxn = z
  .object({
    transaction_id: z.string(),
    account_id: z.string(),
    amount: z.number(),
    iso_currency_code: z.string().nullish(),
    unofficial_currency_code: z.string().nullish(),
    date: z.string(),
    authorized_date: z.string().nullish(),
    datetime: z.string().nullish(),
    authorized_datetime: z.string().nullish(),
    name: z.string().nullish(),
    merchant_name: z.string().nullish(),
    pending: z.boolean(),
    payment_channel: z.string().nullish(),
    personal_finance_category: z.object({ primary: z.string(), detailed: z.string().nullish() }).nullish(),
  })
  .passthrough();
export type PTxn = z.infer<typeof PTxn>;

const SyncPage = z.object({
  added: z.array(PTxn),
  modified: z.array(PTxn),
  removed: z.array(z.object({ transaction_id: z.string() })),
  accounts: z.array(PAccount).default([]),
  next_cursor: z.string(),
  has_more: z.boolean(),
});
export type SyncPage = z.infer<typeof SyncPage>;

/** What LoR asks of Plaid. Tests pass a fake. */
export interface PlaidPort {
  /** For Link: a new login, or (with an access token) re-linking one that needs it. */
  linkToken(accessToken?: string): Promise<string>;
  exchange(publicToken: string): Promise<{ itemId: string; accessToken: string }>;
  item(accessToken: string): Promise<{ institutionId: string | null; institution: string | null }>;
  /** One page of /transactions/sync from the cursor (null: from the beginning). */
  sync(accessToken: string, cursor: string | null): Promise<SyncPage>;
}

const BASE = { sandbox: "https://sandbox.plaid.com", production: "https://production.plaid.com" } as const;

async function call(path: string, body: Record<string, unknown>): Promise<unknown> {
  if (!plaidConfigured()) throw new PlaidError("NOT_CONFIGURED", "set PLAID_CLIENT_ID and PLAID_SECRET in the env file");
  const res = await fetch(`${BASE[env.PLAID_ENV]}${path}`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ client_id: env.PLAID_CLIENT_ID, secret: env.PLAID_SECRET, ...body }),
  });
  const data = (await res.json().catch(() => ({}))) as Record<string, unknown>;
  if (!res.ok) {
    throw new PlaidError(String(data.error_code ?? `HTTP_${res.status}`), String(data.error_message ?? res.statusText));
  }
  return data;
}

export const plaid: PlaidPort = {
  async linkToken(accessToken) {
    const data = await call("/link/token/create", {
      client_name: "Life on Rails",
      user: { client_user_id: "owner" },
      country_codes: ["US"],
      language: "en",
      ...(accessToken
        ? { access_token: accessToken }
        : { products: ["transactions"], transactions: { days_requested: 730 } }),
    });
    return z.object({ link_token: z.string() }).parse(data).link_token;
  },
  async exchange(publicToken) {
    const data = await call("/item/public_token/exchange", { public_token: publicToken });
    const d = z.object({ item_id: z.string(), access_token: z.string() }).parse(data);
    return { itemId: d.item_id, accessToken: d.access_token };
  },
  async item(accessToken) {
    const data = await call("/item/get", { access_token: accessToken });
    const item = z
      .object({ item: z.object({ institution_id: z.string().nullish(), institution_name: z.string().nullish() }) })
      .parse(data).item;
    let name = item.institution_name ?? null;
    if (!name && item.institution_id) {
      const inst = await call("/institutions/get_by_id", { institution_id: item.institution_id, country_codes: ["US"] });
      name = z.object({ institution: z.object({ name: z.string() }) }).parse(inst).institution.name;
    }
    return { institutionId: item.institution_id ?? null, institution: name };
  },
  async sync(accessToken, cursor) {
    const data = await call("/transactions/sync", {
      access_token: accessToken,
      count: 500,
      ...(cursor ? { cursor } : {}),
      options: { include_personal_finance_category: true },
    });
    return SyncPage.parse(data);
  },
};

// ---- access tokens, by item id ----

const TokenFile = z.record(z.string(), z.string());

export function readAccessTokens(): Record<string, string> {
  if (!existsSync(env.PLAID_ITEMS_FILE)) return {};
  return TokenFile.parse(JSON.parse(readFileSync(env.PLAID_ITEMS_FILE, "utf8")));
}

export function accessToken(itemId: string): string {
  const token = readAccessTokens()[itemId];
  if (!token) throw new PlaidError("NO_ACCESS_TOKEN", `no access token for item ${itemId} in ${env.PLAID_ITEMS_FILE}; link it again`);
  return token;
}

export function saveAccessToken(itemId: string, token: string): void {
  const all = { ...readAccessTokens(), [itemId]: token };
  writeFileSync(env.PLAID_ITEMS_FILE, JSON.stringify(all, null, 2) + "\n", { mode: 0o600 });
}
