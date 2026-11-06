import { mkdtempSync, readFileSync, statSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { env } from "../../src/server/env.js";
import { listAccounts, listTxns, spend } from "../../src/server/money/read.js";
import { type PAccount, type PTxn, PlaidError, type PlaidPort, type SyncPage } from "../../src/server/money/plaid.js";
import { completeLink, syncMoney } from "../../src/server/money/sync.js";
import { dbDescribe } from "./helpers.js";

env.PLAID_ITEMS_FILE = path.join(mkdtempSync(path.join(tmpdir(), "lor-plaid-")), "plaid-items.json");

const account = (id: string, type: string, current: number): PAccount => ({
  account_id: id, name: id, mask: id.slice(-4), type, subtype: type === "credit" ? "credit card" : "checking",
  balances: { current, available: type === "credit" ? null : current, limit: type === "credit" ? 5000 : null, iso_currency_code: "USD" },
});

const txn = (id: string, acct: string, date: string, amount: number, category: string, merchant: string, pending = false): PTxn => ({
  transaction_id: id, account_id: acct, amount, iso_currency_code: "USD", date, name: merchant.toUpperCase(), merchant_name: merchant,
  pending, payment_channel: "in store", personal_finance_category: { primary: category, detailed: `${category}_OTHER` },
});

/** A bank whose transactions tests change between syncs. Each change is one page of /transactions/sync. */
function bank() {
  const pages: Omit<SyncPage, "next_cursor" | "has_more">[] = [];
  let failNext: PlaidError | null = null;
  const port: PlaidPort = {
    linkToken: async () => "link-sandbox-test",
    exchange: async (pt) => ({ itemId: `item-${pt}`, accessToken: `access-sandbox-${pt}` }),
    item: async () => ({ institutionId: "ins_1", institution: "First Platypus Bank" }),
    async sync(token, cursor) {
      expect(token).toMatch(/^access-sandbox-/);
      if (failNext) {
        const e = failNext;
        failNext = null;
        throw e;
      }
      const at = cursor ? Number(cursor) : 0;
      const page = pages[at] ?? { added: [], modified: [], removed: [], accounts: [] };
      const next = Math.min(at + 1, pages.length);
      return { ...page, next_cursor: String(next), has_more: next < pages.length };
    },
  };
  return {
    port,
    page(p: Partial<Omit<SyncPage, "next_cursor" | "has_more">>) {
      pages.push({ added: [], modified: [], removed: [], accounts: [], ...p });
    },
    fail(e: PlaidError) {
      failNext = e;
    },
  };
}

const q = (o: Record<string, string>) => new URLSearchParams(o);

dbDescribe("money", () => {
  it("links a login, keeps its token in the file only, and syncs pages, changes and removals", async () => {
    const b = bank();
    const linked = await completeLink(sql, b.port, "abc");
    expect(linked).toEqual({ item_id: "item-abc", institution: "First Platypus Bank" });
    expect(JSON.parse(readFileSync(env.PLAID_ITEMS_FILE, "utf8"))).toEqual({ "item-abc": "access-sandbox-abc" });
    expect(statSync(env.PLAID_ITEMS_FILE).mode & 0o777).toBe(0o600);
    const dump = JSON.stringify(await sql`SELECT * FROM money_item`);
    expect(dump).not.toContain("access-sandbox");

    const accounts = [account("chk-1111", "depository", 1200), account("card-2222", "credit", 300)];
    b.page({ accounts, added: [txn("t1", "chk-1111", "2026-11-02", 4.5, "FOOD_AND_DRINK", "Blue Bottle", true)] });
    b.page({ added: [txn("t2", "card-2222", "2026-11-03", 60, "GENERAL_MERCHANDISE", "Target"), txn("t3", "chk-1111", "2026-11-03", 300, "LOAN_PAYMENTS", "Chase card")] });
    expect(await syncMoney(sql, b.port)).toEqual({ items: 1, added: 3, modified: 0, removed: 0 });

    // The pending coffee posts: Plaid removes the pending one and adds the posted one; Target refunds $15.
    b.page({
      accounts: [account("chk-1111", "depository", 895.5), account("card-2222", "credit", 45)],
      removed: [{ transaction_id: "t1" }],
      added: [txn("t1b", "chk-1111", "2026-11-02", 4.75, "FOOD_AND_DRINK", "Blue Bottle"), txn("t4", "card-2222", "2026-11-04", -15, "GENERAL_MERCHANDISE", "Target")],
      modified: [txn("t2", "card-2222", "2026-11-03", 60, "GENERAL_MERCHANDISE", "Target Store")],
    });
    expect(await syncMoney(sql, b.port)).toEqual({ items: 1, added: 2, modified: 1, removed: 1 });
    // Nothing new: nothing changes.
    expect(await syncMoney(sql, b.port)).toEqual({ items: 1, added: 0, modified: 0, removed: 0 });

    const txns = await listTxns(sql, q({ from: "2026-11-01", to: "2026-11-30" }));
    expect(txns.results.map((t) => t.id).sort()).toEqual(["t1b", "t2", "t3", "t4"]);
    expect(txns.results.find((t) => t.id === "t2")?.merchant).toBe("Target Store");
    expect((await listTxns(sql, q({ from: "2026-11-01", to: "2026-11-30", q: "bottle" }))).results.map((t) => t.id)).toEqual(["t1b"]);
    expect((await listTxns(sql, q({ from: "2026-11-01", to: "2026-11-30", account: "2222" }))).results).toHaveLength(2);

    // The card payment is not spending; the refund nets against Target.
    const s = await spend(sql, q({ from: "2026-11-01", to: "2026-11-30", by: "merchant" }));
    expect(s.total).toBe(49.75);
    expect(s.results).toEqual([
      { key: "Target Store", total: 60, count: 1 },
      { key: "Blue Bottle", total: 4.75, count: 1 },
      { key: "Target", total: -15, count: 1 },
    ]);

    const a = await listAccounts(sql);
    expect(a.net).toBe(850.5);
    expect(a.items[0]).toMatchObject({ institution: "First Platypus Bank", error: null });
    expect(a.items[0]!.synced_at).toMatch(/Z$/);
  });

  it("records a login that needs signing in again, keeps syncing the others, and clears it once fixed", async () => {
    const b = bank();
    await completeLink(sql, b.port, "one");
    b.page({ accounts: [account("chk-1111", "depository", 10)], added: [txn("t1", "chk-1111", "2026-11-02", 5, "FOOD_AND_DRINK", "Cafe")] });
    b.fail(new PlaidError("ITEM_LOGIN_REQUIRED", "the login details of this item have changed"));
    await expect(syncMoney(sql, b.port)).rejects.toThrow(/First Platypus Bank: Plaid ITEM_LOGIN_REQUIRED/);
    expect((await listAccounts(sql)).items[0]!.error).toBe("ITEM_LOGIN_REQUIRED");
    expect(await syncMoney(sql, b.port)).toMatchObject({ added: 1 });
    expect((await listAccounts(sql)).items[0]!.error).toBeNull();
  });

  it("starts the pass again when Plaid's data changes during paging", async () => {
    const b = bank();
    await completeLink(sql, b.port, "two");
    b.page({ accounts: [account("chk-1111", "depository", 10)], added: [txn("t1", "chk-1111", "2026-11-02", 5, "FOOD_AND_DRINK", "Cafe")] });
    b.fail(new PlaidError("TRANSACTIONS_SYNC_MUTATION_DURING_PAGINATION", "retry"));
    expect(await syncMoney(sql, b.port)).toMatchObject({ added: 1 });
  });
});
