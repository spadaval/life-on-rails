#!/usr/bin/env node
// lor: a thin client for Life on Rails' /api/v1. No dependencies; runs on any
// recent Node. Installed as ~/.local/bin/lor (a symlink to this file).
//
// stdout is data only (JSON, or markdown with --md); diagnostics go to stderr.
// Exit codes: 0 ok, 2 usage, 3 empty result, 4 auth, 5 not found,
//             6 refused by a write rule, 8 server unavailable, 1 anything else.
import { existsSync, readFileSync } from "node:fs";
import { homedir } from "node:os";
import path from "node:path";

const HELP = `lor — Life on Rails memory for the agent

orientation
  lor context [--task <id>] [--md]        the digest; run first in every session
  lor status                              job and sensor health
  lor changes --since <rfc3339|24h|7d>    what arrived or changed after that time
  lor mail pending                        mail waiting for the agent, once the burst is quiet

read
  lor search <query> [--in page,entity,intent,obs] [--from <t>] [--to <t>] [--limit N]
  lor page search <query> [--limit N]
  lor page read <slug> [--version N] [--md]
  lor page history <slug>
  lor entity show <id|name|email|phone>
  lor people [<search>] [--overdue | --birthdays [--days N] | --groups | --all] [--limit N]
  lor day [<YYYY-MM-DD>]
  lor graph [--starving] [--waiting]
  lor intent show <id>
  lor score explain <intent_id>
  lor places                              named places, and places the phone keeps visiting that have no name
  lor apps [--days N]                     which phone apps took the time, and what each is labelled as
  lor rules                               which app, calendar title, or place stands for which goal

write (free for the agent; every write needs --thread <your T3 thread id>)
  lor page create <slug> --title "..." [--entity <id> | --intent <id>] [--note "..."] < body.md
  lor page update <slug> --base <N> [--note "..."] [--dry-run] < body.md
  lor page update <slug> --base <N> --find "..." --replace "..."
  lor page update <slug> --append < fragment.md
  lor entity upsert --kind person|org|place|thing|group --name "..." [--email a@b]... [--alias "..."]...
  lor entity set <id|name|email> [--keep-in-touch <days>|off] [--birthday MM-DD|off] [--priority on|off]
        [--name "..."] [--alias "..."]... [--unalias "..."]...   a --name stays even when Google calls them something else
  lor entity merge <keep> <merge-in>       fold the second into the first (identifiers, messages, page, tasks); the second is deleted
  lor log "<text>" [--intent <id>]
  lor mail done <observation_id>...       take handled mail off the queue
  lor intent create --type goal|project|habit|task|metric --title "..." [--parent <id>]
        [--due <t>] [--waiting-on <entity_id>] [--assign agent] [--weight N] [--hours N] [--description "..."]
        [--flag "<why it needs Sumeet>"]
  lor link --from <id> --to <id> --kind measures|blocks|derived_from [--weight N]
  lor unlink --from <id> --to <id> --kind measures|blocks|derived_from
  lor intent status <id> active|paused|later|archived   only when Sumeet asks
  lor task assign <id> owner|agent
  lor task flag <id> "<why it needs Sumeet>"   Needs you on Today, and one phone notification
  lor done <task_id> [--memo "..."]
  lor propose time --block <id> --intent <id> [--reason "..."]
  lor place add "<name>" --at <lat>,<lon> [--radius <m>] [--intent <id>]
  lor rule add (--app <package> | --title <regex> | --place <name>) --intent <id>
  lor rule delete <rule_id>

the owner's evidence (only when the owner asks in the current thread)
  lor timer start|stop|pause|resume [--intent <id>] [--title "..."]
  lor recategorize <block_id> <intent_id>
  lor confirm <date> [--all | --accept <ids> --reject <ids>]
  lor habit <intent_id> [--date <date>]
  lor metric <intent_id> <value> [--date <date>]

A body comes from stdin, or from --file <path>.
--thread may also be set once as LOR_THREAD in the environment.
`;

const BOOLEAN_FLAGS = new Set(["md", "history", "append", "dry-run", "all", "starving", "waiting", "help", "overdue", "birthdays", "groups"]);
const REPEATABLE_FLAGS = new Set(["email", "alias", "unalias", "id"]);

class Usage extends Error {}

function parseArgs(argv) {
  const positionals = [];
  const flags = {};
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (!arg.startsWith("--") || arg === "--") {
      positionals.push(arg);
      continue;
    }
    const eq = arg.indexOf("=");
    const name = eq === -1 ? arg.slice(2) : arg.slice(2, eq);
    let value;
    if (BOOLEAN_FLAGS.has(name)) value = true;
    else if (eq !== -1) value = arg.slice(eq + 1);
    else {
      value = argv[++i];
      if (value === undefined) throw new Usage(`--${name} needs a value`);
    }
    if (REPEATABLE_FLAGS.has(name)) (flags[name] ??= []).push(value);
    else flags[name] = value;
  }
  return { positionals, flags };
}

function loadConfig() {
  const file = process.env.LOR_ENV_FILE ?? path.join(homedir(), ".config/life-on-rails/env");
  const fromFile = {};
  if (existsSync(file)) {
    for (const line of readFileSync(file, "utf8").split("\n")) {
      const m = /^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/.exec(line);
      if (m && !line.trim().startsWith("#")) fromFile[m[1]] = m[2].replace(/^(['"])(.*)\1$/, "$2");
    }
  }
  const get = (k) => process.env[k] ?? fromFile[k] ?? "";
  return { token: get("LOR_AGENT_TOKEN"), port: get("LOR_PORT") || "3847", url: get("LOR_URL") };
}

function readBody(flags) {
  if (typeof flags.file === "string") return readFileSync(flags.file, "utf8");
  if (process.stdin.isTTY) throw new Usage("this command reads a body from stdin (or --file <path>)");
  return readFileSync(0, "utf8");
}

const int = (value, name) => {
  if (value === undefined) return undefined;
  const n = Number(value);
  if (!Number.isInteger(n)) throw new Usage(`--${name} must be a whole number`);
  return n;
};

const num = (value, name) => {
  if (value === undefined) return undefined;
  const n = Number(value);
  if (!Number.isFinite(n)) throw new Usage(`--${name} must be a number`);
  return n;
};

/** "24h", "90m", "7d" → an RFC 3339 time that long ago; anything else passes through. */
function sinceTime(value) {
  const m = /^(\d+)([mhd])$/.exec(value);
  if (!m) return value;
  const ms = Number(m[1]) * { m: 60_000, h: 3_600_000, d: 86_400_000 }[m[2]];
  return new Date(Date.now() - ms).toISOString();
}

const need = (value, what) => {
  if (value === undefined || value === "" || value === true) throw new Usage(`missing ${what}`);
  return value;
};

const list = (value) => (typeof value === "string" ? value.split(",").map((s) => s.trim()).filter(Boolean) : []);

/** Turn a command line into one HTTP call. */
function plan(positionals, flags) {
  const [a, b, c, d] = positionals;
  const thread = flags.thread ?? process.env.LOR_THREAD;
  const write = (body) => ({ ...body, thread });
  const q = (params) => {
    const sp = new URLSearchParams();
    for (const [k, v] of Object.entries(params)) if (v !== undefined && v !== false) sp.set(k, v === true ? "1" : String(v));
    const s = sp.toString();
    return s ? `?${s}` : "";
  };
  const slugPath = (slug) => need(slug, "<slug>").split("/").map(encodeURIComponent).join("/");

  switch (a) {
    case "context":
      return { method: "GET", path: `/context${q({ task: flags.task })}`, md: true };
    case "status":
      return { method: "GET", path: "/status" };
    case "places":
      return { method: "GET", path: "/places" };
    case "apps":
      return { method: "GET", path: `/apps${q({ days: int(flags.days, "days") })}` };
    case "rules":
      return { method: "GET", path: "/rules" };
    case "place":
      if (b === "add") {
        const at = need(flags.at, "--at <lat>,<lon>").split(",").map(Number);
        if (at.length !== 2 || at.some((n) => !Number.isFinite(n))) throw new Usage("--at must be <lat>,<lon>");
        return {
          method: "POST",
          path: "/places",
          body: write({ name: need(c, '"<name>"'), lat: at[0], lon: at[1], radius_m: num(flags.radius, "radius"), intent_id: flags.intent }),
        };
      }
      break;
    case "rule":
      if (b === "add") return { method: "POST", path: "/rules", body: write({ app: flags.app, title: flags.title, place: flags.place, intent_id: need(flags.intent, "--intent") }) };
      if (b === "delete") return { method: "DELETE", path: `/rules/${need(c, "<rule_id>")}`, body: write({}) };
      break;
    case "mail":
      if (b === "pending") return { method: "GET", path: "/mail/pending" };
      if (b === "done") {
        need(c, "<observation_id>...");
        return { method: "POST", path: "/mail/done", body: write({ observation_ids: positionals.slice(2) }) };
      }
      break;
    case "changes":
      return { method: "GET", path: `/changes${q({ since: sinceTime(need(flags.since, "--since")) })}` };
    case "search":
      return {
        method: "GET",
        path: `/search${q({ q: need(positionals.slice(1).join(" "), "<query>"), in: flags.in, from: flags.from, to: flags.to, limit: int(flags.limit, "limit") })}`,
      };
    case "day":
      return { method: "GET", path: `/day${q({ date: b })}`, md: true };
    case "log":
      return { method: "POST", path: "/log", body: write({ text: need(b, "<text>"), intent_id: flags.intent }) };

    case "page":
      switch (b) {
        case "search":
          return { method: "GET", path: `/pages${q({ q: need(positionals.slice(2).join(" "), "<query>"), limit: int(flags.limit, "limit") })}` };
        case "read":
          return { method: "GET", path: `/pages/${slugPath(c)}${q({ version: int(flags.version, "version"), history: flags.history })}`, md: true };
        case "history":
          return { method: "GET", path: `/pages/${slugPath(c)}${q({ history: true })}` };
        case "create":
          return {
            method: "POST",
            path: "/pages",
            body: write({ slug: need(c, "<slug>"), title: need(flags.title, "--title"), body: readBody(flags), entity_id: flags.entity, intent_id: flags.intent, note: flags.note }),
          };
        case "update": {
          const body = { base: int(flags.base, "base"), title: flags.title, note: flags.note, dry_run: flags["dry-run"] === true };
          if (flags.find !== undefined) Object.assign(body, { find: flags.find, replace: need(flags.replace, "--replace") });
          else if (flags.append) Object.assign(body, { append: readBody(flags) });
          else if (flags.title === undefined || !process.stdin.isTTY || flags.file) Object.assign(body, { body: readBody(flags) });
          return { method: "PUT", path: `/pages/${slugPath(c)}`, body: write(body) };
        }
      }
      break;

    case "entity":
      switch (b) {
        case "upsert":
          return {
            method: "POST",
            path: "/entities",
            body: write({
              kind: need(flags.kind, "--kind"),
              name: need(flags.name, "--name"),
              emails: flags.email,
              aliases: flags.alias,
              identifiers: (flags.id ?? []).map((pair) => {
                const i = pair.indexOf(":");
                if (i < 1) throw new Usage("--id takes kind:value, for example domain:example.com");
                return { kind: pair.slice(0, i), value: pair.slice(i + 1) };
              }),
            }),
          };
        case "show":
          return { method: "GET", path: `/entities/${encodeURIComponent(need(positionals.slice(2).join(" "), "<id|name|email>"))}` };
        case "set": {
          const onOff = (v, name) => {
            if (v === undefined) return undefined;
            if (v === "on") return true;
            if (v === "off") return false;
            throw new Usage(`--${name} takes on or off`);
          };
          const kit = flags["keep-in-touch"];
          const bday = flags.birthday;
          return {
            method: "POST",
            path: `/entities/${encodeURIComponent(need(c, "<id|name|email>"))}`,
            body: write({
              keep_in_touch_days: kit === undefined || kit === "off" ? undefined : int(kit, "keep-in-touch"),
              clear_keep_in_touch: kit === "off" || undefined,
              birthday: bday === undefined || bday === "off" ? undefined : bday,
              clear_birthday: bday === "off" || undefined,
              priority: onOff(flags.priority, "priority"),
              name: flags.name,
              add_aliases: flags.alias,
              remove_aliases: flags.unalias,
            }),
          };
        }
        case "merge":
          return {
            method: "POST",
            path: `/entities/${encodeURIComponent(need(c, "<keep>"))}/merge`,
            body: write({ from: need(d, "<merge-in>") }),
          };
      }
      break;

    case "people": {
      const filter = flags.overdue ? "overdue" : flags.birthdays ? "birthdays" : flags.groups ? "groups" : flags.all ? "all" : undefined;
      return { method: "GET", path: `/people${q({ filter, q: positionals.slice(1).join(" ") || undefined, days: flags.days, limit: int(flags.limit, "limit") })}`, md: true };
    }

    case "graph":
      return { method: "GET", path: `/graph${q({ starving: flags.starving, waiting: flags.waiting })}`, md: true };
    case "intent":
      switch (b) {
        case "show":
          return { method: "GET", path: `/intents/${need(c, "<id>")}` };
        case "status":
          return { method: "POST", path: `/intents/${need(c, "<id>")}/status`, body: write({ status: need(d, "active|paused|later|archived") }) };
        case "create":
          return {
            method: "POST",
            path: "/intents",
            body: write({
              type: need(flags.type, "--type"),
              title: need(flags.title, "--title"),
              description: flags.description,
              parent_id: flags.parent,
              due_at: flags.due,
              waiting_on_entity_id: flags["waiting-on"],
              assignee: flags.assign,
              weight: num(flags.weight, "weight"),
              weekly_hour_target: num(flags.hours, "hours"),
              frequency: int(flags.frequency, "frequency"),
              frequency_horizon: flags.per,
              estimate_hours: num(flags.estimate, "estimate"),
              flag: flags.flag,
            }),
          };
      }
      break;
    case "link":
      return {
        method: "POST",
        path: "/links",
        body: write({ from_id: need(flags.from, "--from"), to_id: need(flags.to, "--to"), kind: need(flags.kind, "--kind"), weight: num(flags.weight, "weight") }),
      };
    case "unlink":
      return {
        method: "POST",
        path: "/links/remove",
        body: write({ from_id: need(flags.from, "--from"), to_id: need(flags.to, "--to"), kind: need(flags.kind, "--kind") }),
      };
    case "task":
      if (b === "assign") return { method: "POST", path: `/intents/${need(c, "<id>")}/assign`, body: write({ assignee: need(d, "owner|agent") }) };
      if (b === "flag") return { method: "POST", path: `/intents/${need(c, "<id>")}/flag`, body: write({ note: need(positionals.slice(3).join(" "), '"<why it needs Sumeet>"') }) };
      break;
    case "done":
      return { method: "POST", path: `/intents/${need(b, "<task_id>")}/done`, body: write({ memo: flags.memo }) };
    case "score":
      if (b === "explain") return { method: "GET", path: `/intents/${need(c, "<intent_id>")}/score`, md: true };
      break;
    case "habit":
      return { method: "POST", path: `/intents/${need(b, "<intent_id>")}/habit`, body: write({ date: flags.date, memo: flags.memo }) };
    case "metric":
      return { method: "POST", path: `/intents/${need(b, "<intent_id>")}/metric`, body: write({ value: num(need(c, "<value>"), "value"), date: flags.date, memo: flags.memo }) };

    case "propose":
      if (b === "time") {
        return { method: "POST", path: "/proposals", body: write({ block_id: need(flags.block, "--block"), intent_id: need(flags.intent, "--intent"), reason: flags.reason }) };
      }
      break;
    case "timer":
      if (["start", "stop", "pause", "resume"].includes(b)) {
        return { method: "POST", path: `/timer/${b}`, body: write({ intent_id: flags.intent, title: flags.title }) };
      }
      break;
    case "recategorize":
      return { method: "POST", path: `/blocks/${need(b, "<block_id>")}/recategorize`, body: write({ intent_id: need(c, "<intent_id>") }) };
    case "confirm":
      return {
        method: "POST",
        path: "/confirm",
        body: write({ date: need(b, "<date>"), all: flags.all === true, accept: list(flags.accept), reject: list(flags.reject) }),
      };
  }
  throw new Usage(`unknown command: ${positionals.join(" ") || "(none)"}`);
}

function printMarkdown(data) {
  if (typeof data.markdown === "string") return process.stdout.write(data.markdown);
  if (typeof data.body === "string" && typeof data.slug === "string") {
    return process.stdout.write(`<!-- ${data.slug} · version ${data.version} of ${data.head_version} · ${data.via} -->\n${data.body}${data.body.endsWith("\n") ? "" : "\n"}`);
  }
  process.stdout.write(JSON.stringify(data, null, 2) + "\n");
}

async function main() {
  const { positionals, flags } = parseArgs(process.argv.slice(2));
  if (flags.help || positionals[0] === "help" || positionals.length === 0) {
    process.stdout.write(HELP);
    return positionals.length === 0 && !flags.help ? 2 : 0;
  }
  const call = plan(positionals, flags);
  if (call.method !== "GET" && !call.body?.thread) {
    throw new Usage("writes need --thread <your T3 thread id> (or LOR_THREAD)");
  }
  const config = loadConfig();
  const base = config.url || `http://127.0.0.1:${config.port}`;
  let response;
  try {
    response = await fetch(`${base}/api/v1${call.path}`, {
      method: call.method,
      headers: { authorization: `Bearer ${config.token}`, ...(call.body ? { "content-type": "application/json" } : {}) },
      body: call.body ? JSON.stringify(call.body) : undefined,
    });
  } catch (err) {
    process.stderr.write(`lor: cannot reach ${base}: ${err.cause?.code ?? err.message}\n`);
    return 8;
  }
  const text = await response.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    process.stderr.write(`lor: unexpected response (${response.status}): ${text.slice(0, 200)}\n`);
    return 1;
  }
  if (!response.ok) {
    const e = data.error ?? {};
    process.stderr.write(`lor: ${e.code ?? response.status}: ${e.message ?? "request failed"}\n`);
    if (e.detail && Object.keys(e.detail).length > 0) process.stderr.write(JSON.stringify(e.detail, null, 2) + "\n");
    return { 400: 2, 401: 4, 404: 5, 409: 6 }[response.status] ?? 1;
  }
  // A page write echoes what changed, not the whole body the caller just sent.
  if (call.method !== "GET" && call.path.startsWith("/pages")) {
    const page = data.page ?? data;
    data = {
      slug: page.slug,
      version: page.version,
      head_version: page.head_version,
      ...(data.page ? { changed: data.changed, dry_run: data.dry_run, diff: data.diff } : {}),
      warnings: data.warnings ?? [],
    };
  }
  // Other writes echo the row they made; the empty fields are noise.
  else if (call.method !== "GET" && data && typeof data === "object" && !Array.isArray(data)) {
    data = Object.fromEntries(Object.entries(data).filter(([, v]) => v !== null));
  }
  if (flags.md && call.md) printMarkdown(data);
  else {
    // The digest's markdown is the same content again; leave it out of JSON.
    if (call.md && typeof data.markdown === "string") delete data.markdown;
    process.stdout.write(JSON.stringify(data, null, 2) + "\n");
  }
  return Array.isArray(data.results) && data.results.length === 0 ? 3 : 0;
}

main().then(
  (code) => process.exit(code),
  (err) => {
    if (err instanceof Usage) {
      process.stderr.write(`lor: ${err.message}\nRun \`lor help\` for the verbs.\n`);
      process.exit(2);
    }
    process.stderr.write(`lor: ${err.stack ?? err}\n`);
    process.exit(1);
  },
);
