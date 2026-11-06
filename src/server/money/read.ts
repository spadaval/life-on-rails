// What the agent and the screens read about money: accounts and balances,
// transactions, and spending by category, merchant or account.
import type { Sql } from "../../db/client.js";
import { addDays, isYmd, localYmd } from "../../shared/localTime.js";
import { BadInput } from "../errors.js";
import { env } from "../env.js";

/**
 * Not spending: money moving between the owner's own accounts (transfers,
 * card and loan payments), money coming in, and money borrowed. Refunds stay
 * in, as negative amounts in the category they reverse.
 */
export const NOT_SPENDING = ["TRANSFER_IN", "TRANSFER_OUT", "LOAN_PAYMENTS", "LOAN_DISBURSEMENTS", "INCOME"] as const;

const money = (n: number) => (n < 0 ? "-" : "") + "$" + Math.abs(n).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export async function listAccounts(sql: Sql) {
  const items = await sql<{ id: string; institution: string; error: string | null; synced_at: string | null }[]>`
    SELECT id, institution, error, to_char(synced_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS synced_at
    FROM money_item ORDER BY institution`;
  const accounts = await sql<
    { id: string; item_id: string; name: string; mask: string | null; type: string; subtype: string | null; current: number | null; available: number | null; credit_limit: number | null; currency: string | null }[]
  >`
    SELECT id, item_id, name, mask, type, subtype, current::float8 AS current, available::float8 AS available,
      credit_limit::float8 AS credit_limit, currency
    FROM money_account ORDER BY type, name`;
  // Owed on cards and loans counts against; everything else for.
  let net = 0;
  for (const a of accounts) net += (a.type === "credit" || a.type === "loan" ? -1 : 1) * (a.current ?? 0);
  const lines: string[] = [];
  for (const i of items) {
    lines.push(`## ${i.institution}${i.error ? ` (needs attention: ${i.error})` : ""}`);
    for (const a of accounts.filter((x) => x.item_id === i.id)) {
      const owed = a.type === "credit" || a.type === "loan";
      lines.push(
        `- ${a.name}${a.mask ? ` ··${a.mask}` : ""} (${a.subtype ?? a.type}): ${a.current == null ? "?" : money(a.current)}${owed ? " owed" : ""}` +
          (a.available != null && !owed ? `, ${money(a.available)} available` : "") +
          (a.credit_limit != null ? ` of ${money(a.credit_limit)} limit` : "") +
          `  \`${a.id}\``,
      );
    }
  }
  const markdown = items.length === 0 ? "No accounts linked. Link one at /plaid/link.\n" : `Net: ${money(net)}\n\n${lines.join("\n")}\n`;
  return { items, accounts, net: Math.round(net * 100) / 100, markdown };
}

function range(query: URLSearchParams): { from: string; to: string } {
  const today = localYmd(env.LOR_TZ, new Date());
  const from = query.get("from") ?? `${today.slice(0, 8)}01`;
  const to = query.get("to") ?? today;
  for (const [k, v] of [["from", from], ["to", to]] as const) if (!isYmd(v)) throw new BadInput(`--${k} must be YYYY-MM-DD`);
  return { from, to };
}

export async function listTxns(sql: Sql, query: URLSearchParams) {
  const today = localYmd(env.LOR_TZ, new Date());
  const from = query.get("from") ?? addDays(today, -30);
  const to = query.get("to") ?? today;
  for (const [k, v] of [["from", from], ["to", to]] as const) if (!isYmd(v)) throw new BadInput(`--${k} must be YYYY-MM-DD`);
  const q = query.get("q");
  const category = query.get("category")?.toUpperCase() ?? null;
  const account = query.get("account");
  const limit = Math.min(Number(query.get("limit") ?? 100) || 100, 1000);
  const rows = await sql<
    { id: string; date: string; amount: number; name: string; merchant: string | null; category: string | null; category_detail: string | null; pending: boolean; account: string; mask: string | null }[]
  >`
    SELECT t.id, t.date::text AS date, t.amount::float8 AS amount, t.name, t.merchant, t.category, t.category_detail, t.pending,
      a.name AS account, a.mask
    FROM money_txn t JOIN money_account a ON a.id = t.account_id
    WHERE t.date BETWEEN ${from} AND ${to}
      ${q ? sql`AND (t.name ILIKE ${"%" + q + "%"} OR t.merchant ILIKE ${"%" + q + "%"})` : sql``}
      ${category ? sql`AND (t.category = ${category} OR t.category_detail = ${category})` : sql``}
      ${account ? sql`AND (a.id = ${account} OR a.mask = ${account} OR a.name ILIKE ${"%" + account + "%"})` : sql``}
    ORDER BY t.date DESC, t.id
    LIMIT ${limit}`;
  const markdown =
    rows.length === 0
      ? `No transactions from ${from} to ${to}.\n`
      : rows
          .map((r) => `- ${r.date} ${money(r.amount)} ${r.merchant ?? r.name} (${r.category_detail ?? r.category ?? "uncategorised"}; ${r.account}${r.mask ? ` ··${r.mask}` : ""})${r.pending ? " pending" : ""}`)
          .join("\n") + "\n";
  return { from, to, results: rows, markdown };
}

const GROUPS = {
  category: "coalesce(t.category, 'UNCATEGORISED')",
  detail: "coalesce(t.category_detail, t.category, 'UNCATEGORISED')",
  merchant: "coalesce(t.merchant, t.name)",
  account: "a.name || coalesce(' ··' || a.mask, '')",
} as const;

/** Spending (outflows less refunds) from..to, grouped. Amounts are positive for money spent. */
export async function spend(sql: Sql, query: URLSearchParams) {
  const { from, to } = range(query);
  const by = (query.get("by") ?? "category") as keyof typeof GROUPS;
  if (!(by in GROUPS)) throw new BadInput(`--by must be one of ${Object.keys(GROUPS).join(", ")}`);
  const rows = await sql<{ key: string; total: number; count: number }[]>`
    SELECT ${sql.unsafe(GROUPS[by])} AS key, sum(t.amount)::float8 AS total, count(*)::int AS count
    FROM money_txn t JOIN money_account a ON a.id = t.account_id
    WHERE t.date BETWEEN ${from} AND ${to}
      AND coalesce(t.category, '') NOT IN ${sql(NOT_SPENDING as unknown as string[])}
    GROUP BY 1 ORDER BY 2 DESC`;
  const total = rows.reduce((n, r) => n + r.total, 0);
  const markdown = `Spent ${money(total)} from ${from} to ${to}, by ${by}\n\n${rows.map((r) => `- ${r.key}: ${money(r.total)} (${r.count})`).join("\n")}\n`;
  return { from, to, by, total: Math.round(total * 100) / 100, results: rows, markdown };
}
