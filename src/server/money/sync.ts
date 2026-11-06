// money_sync: each linked login's new, changed and removed transactions since
// its cursor, and its accounts' balances, from Plaid's /transactions/sync.
import type { Sql, Tx } from "../../db/client.js";
import { errorMessage, log } from "../log.js";
import { type PAccount, type PTxn, PlaidError, type PlaidPort, accessToken, saveAccessToken } from "./plaid.js";

/** A whole pass from the cursor, paged; restarted if Plaid's data changed mid-pass, as Plaid asks. */
async function fetchChanges(port: PlaidPort, token: string, from: string | null) {
  for (let attempt = 0; ; attempt++) {
    const added: PTxn[] = [];
    const modified: PTxn[] = [];
    const removed: string[] = [];
    let accounts: PAccount[] = [];
    let cursor = from;
    try {
      for (;;) {
        const page = await port.sync(token, cursor);
        added.push(...page.added);
        modified.push(...page.modified);
        removed.push(...page.removed.map((r) => r.transaction_id));
        if (page.accounts.length > 0) accounts = page.accounts;
        cursor = page.next_cursor;
        if (!page.has_more) break;
      }
      return { added, modified, removed, accounts, cursor };
    } catch (err) {
      if (err instanceof PlaidError && err.code === "TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION" && attempt < 3) continue;
      throw err;
    }
  }
}

async function upsertAccounts(tx: Tx, itemId: string, accounts: PAccount[]): Promise<void> {
  for (const a of accounts) {
    const b = a.balances;
    await tx`
      INSERT INTO money_account (id, item_id, name, official_name, mask, type, subtype, current, available, credit_limit, currency, updated_at)
      VALUES (${a.account_id}, ${itemId}, ${a.name}, ${a.official_name ?? null}, ${a.mask ?? null}, ${a.type}, ${a.subtype ?? null},
        ${b.current ?? null}, ${b.available ?? null}, ${b.limit ?? null}, ${b.iso_currency_code ?? b.unofficial_currency_code ?? null}, now())
      ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, official_name = EXCLUDED.official_name, mask = EXCLUDED.mask,
        type = EXCLUDED.type, subtype = EXCLUDED.subtype, current = EXCLUDED.current, available = EXCLUDED.available,
        credit_limit = EXCLUDED.credit_limit, currency = EXCLUDED.currency, updated_at = now()`;
  }
}

async function upsertTxns(tx: Tx, txns: PTxn[]): Promise<void> {
  for (const t of txns) {
    const at = t.datetime ?? t.authorized_datetime ?? null;
    await tx`
      INSERT INTO money_txn (id, account_id, date, authorized_date, at, amount, currency, name, merchant, category, category_detail, pending, channel, payload, updated_at)
      VALUES (${t.transaction_id}, ${t.account_id}, ${t.date}, ${t.authorized_date ?? null}, ${at}, ${t.amount},
        ${t.iso_currency_code ?? t.unofficial_currency_code ?? null}, ${t.name ?? t.merchant_name ?? ""}, ${t.merchant_name ?? null},
        ${t.personal_finance_category?.primary ?? null}, ${t.personal_finance_category?.detailed ?? null}, ${t.pending},
        ${t.payment_channel ?? null}, ${tx.json(t as never)}, now())
      ON CONFLICT (id) DO UPDATE SET account_id = EXCLUDED.account_id, date = EXCLUDED.date, authorized_date = EXCLUDED.authorized_date,
        at = EXCLUDED.at, amount = EXCLUDED.amount, currency = EXCLUDED.currency, name = EXCLUDED.name, merchant = EXCLUDED.merchant,
        category = EXCLUDED.category, category_detail = EXCLUDED.category_detail, pending = EXCLUDED.pending,
        channel = EXCLUDED.channel, payload = EXCLUDED.payload, updated_at = now()`;
  }
}

export interface ItemSync {
  item: string;
  added: number;
  modified: number;
  removed: number;
  error?: string;
}

/** Sync one login. A Plaid error is recorded on the item (and returned), not thrown. */
export async function syncItem(sql: Sql, port: PlaidPort, itemId: string): Promise<ItemSync> {
  const [item] = await sql<{ cursor: string | null; institution: string }[]>`
    SELECT cursor, institution FROM money_item WHERE id = ${itemId}`;
  if (!item) throw new Error(`no money_item ${itemId}`);
  try {
    const c = await fetchChanges(port, accessToken(itemId), item.cursor);
    const applied = await sql.begin(async (tx) => {
      // Another sync of this item that finished first has already applied these changes.
      const [now] = await tx<{ cursor: string | null }[]>`SELECT cursor FROM money_item WHERE id = ${itemId} FOR UPDATE`;
      if ((now?.cursor ?? null) !== item.cursor) return false;
      await upsertAccounts(tx, itemId, c.accounts);
      await upsertTxns(tx, [...c.added, ...c.modified]);
      if (c.removed.length > 0) await tx`DELETE FROM money_txn WHERE id IN ${tx(c.removed)}`;
      await tx`UPDATE money_item SET cursor = ${c.cursor}, error = NULL, synced_at = now() WHERE id = ${itemId}`;
      return true;
    });
    if (!applied) return { item: item.institution, added: 0, modified: 0, removed: 0 };
    return { item: item.institution, added: c.added.length, modified: c.modified.length, removed: c.removed.length };
  } catch (err) {
    const code = err instanceof PlaidError ? err.code : "SYNC_FAILED";
    await sql`UPDATE money_item SET error = ${code} WHERE id = ${itemId}`;
    log("warn", "plaid sync failed", { institution: item.institution, err: errorMessage(err) });
    return { item: item.institution, added: 0, modified: 0, removed: 0, error: errorMessage(err) };
  }
}

/** Sync every login. Throws (the job shows red) when any login failed, after trying them all. */
export async function syncMoney(sql: Sql, port: PlaidPort): Promise<Record<string, unknown>> {
  const items = await sql<{ id: string }[]>`SELECT id FROM money_item ORDER BY linked_at`;
  const results: ItemSync[] = [];
  for (const i of items) results.push(await syncItem(sql, port, i.id));
  const failed = results.filter((r) => r.error);
  if (failed.length > 0) throw new Error(failed.map((r) => `${r.item}: ${r.error}`).join("; "));
  return {
    items: results.length,
    added: results.reduce((n, r) => n + r.added, 0),
    modified: results.reduce((n, r) => n + r.modified, 0),
    removed: results.reduce((n, r) => n + r.removed, 0),
  };
}

/**
 * Finish a Link: exchange the public token, keep the access token in its file,
 * and record the login. A re-link (update mode) keeps the same item and token,
 * and clears its error. Returns the item id; the caller syncs it.
 */
export async function completeLink(sql: Sql, port: PlaidPort, publicToken: string): Promise<{ item_id: string; institution: string }> {
  const { itemId, accessToken: token } = await port.exchange(publicToken);
  saveAccessToken(itemId, token);
  const inst = await port.item(token);
  const institution = inst.institution ?? inst.institutionId ?? "Unknown institution";
  await sql`
    INSERT INTO money_item (id, institution_id, institution) VALUES (${itemId}, ${inst.institutionId}, ${institution})
    ON CONFLICT (id) DO UPDATE SET institution_id = EXCLUDED.institution_id, institution = EXCLUDED.institution, error = NULL`;
  log("info", "plaid item linked", { institution });
  return { item_id: itemId, institution };
}

/** A Link token: for a new login, or to re-link one whose login Plaid needs again. */
export async function linkToken(port: PlaidPort, itemId?: string): Promise<{ link_token: string }> {
  return { link_token: await port.linkToken(itemId ? accessToken(itemId) : undefined) };
}
