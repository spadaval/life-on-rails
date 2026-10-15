import { expect, it } from "vitest";
import { sql } from "../../src/db/client.js";
import { handleApi } from "../../src/server/api/v1.js";
import { BadInput, NotFound, Refused } from "../../src/server/errors.js";
import { getChanges } from "../../src/server/memory/changes.js";
import { getContext } from "../../src/server/memory/context.js";
import { entityForEmail, showEntity, upsertEntity } from "../../src/server/memory/entities.js";
import { createPage, pageAuthors, pageHistory, readPage, revertPage, searchPages, takeBackVersion, updatePage } from "../../src/server/memory/pages.js";
import { search } from "../../src/server/memory/search.js";
import { dbDescribe } from "./helpers.js";

const agent = { via: "agent" as const, sourceRef: "thread-1" };

async function refusal(p: Promise<unknown>): Promise<Refused> {
  try {
    await p;
  } catch (err) {
    if (err instanceof Refused) return err;
    throw err;
  }
  throw new Error("expected a refusal");
}

dbDescribe("seed pages", () => {
  it("creates agent, me, and inbox; agent and me are in the digest", async () => {
    const rows = await sql<{ slug: string; in_digest: boolean }[]>`SELECT slug, in_digest FROM page ORDER BY slug`;
    expect(rows).toEqual([
      { slug: "agent", in_digest: true },
      { slug: "inbox", in_digest: false },
      { slug: "me", in_digest: true },
    ]);
    expect((await readPage(sql, "agent")).body).toMatch(/The one rule/);
  });
});

dbDescribe("pages", () => {
  it("create writes version 1 and refuses a taken slug or subject", async () => {
    const priya = await upsertEntity(sql, { kind: "person", name: "Priya Raman" });
    const page = await createPage(sql, { slug: "people/priya-raman", title: "Priya Raman", body: "Lives in Austin.\n", entityId: priya.id, ...agent });
    expect(page).toMatchObject({ version: 1, head_version: 1, via: "agent", source_ref: "thread-1", entity_id: priya.id });

    const sameSlug = await refusal(createPage(sql, { slug: "people/priya-raman", title: "x", body: "", ...agent }));
    expect(sameSlug.code).toBe("page-exists");
    expect(sameSlug.detail.existing).toBe("people/priya-raman");
    const sameEntity = await refusal(createPage(sql, { slug: "people/priya", title: "x", body: "", entityId: priya.id, ...agent }));
    expect(sameEntity.detail.existing).toBe("people/priya-raman");

    await expect(createPage(sql, { slug: "Bad Slug", title: "x", body: "", ...agent })).rejects.toBeInstanceOf(BadInput);
  });

  it("update needs the head as base; a stale base is refused with the current head", async () => {
    await createPage(sql, { slug: "topics/home-network", title: "Home network", body: "Router is a UDM.\n", ...agent });
    const second = await updatePage(sql, { slug: "topics/home-network", base: 1, body: "Router is a UDM Pro.\n", note: "model", ...agent });
    expect(second.page.version).toBe(2);
    expect(second.diff).toContain("- Router is a UDM.");
    expect(second.diff).toContain("+ Router is a UDM Pro.");

    const stale = await refusal(updatePage(sql, { slug: "topics/home-network", base: 1, body: "Overwrite.\n", ...agent }));
    expect(stale.code).toBe("stale-base");
    expect(stale.detail).toMatchObject({ head_version: 2, head_body: "Router is a UDM Pro.\n" });
    expect((await readPage(sql, "topics/home-network")).body).toBe("Router is a UDM Pro.\n");

    await expect(updatePage(sql, { slug: "topics/home-network", body: "no base\n", ...agent })).rejects.toBeInstanceOf(BadInput);
    await expect(updatePage(sql, { slug: "topics/nope", base: 1, body: "x", ...agent })).rejects.toBeInstanceOf(NotFound);
  });

  it("find/replace needs exactly one occurrence", async () => {
    await createPage(sql, { slug: "topics/t", title: "T", body: "one two two\n", ...agent });
    expect((await refusal(updatePage(sql, { slug: "topics/t", base: 1, find: "two", replace: "2", ...agent }))).detail.occurrences).toBe(2);
    expect((await refusal(updatePage(sql, { slug: "topics/t", base: 1, find: "three", replace: "3", ...agent }))).detail.occurrences).toBe(0);
    const ok = await updatePage(sql, { slug: "topics/t", base: 1, find: "one", replace: "$& 1", ...agent });
    expect(ok.page.body).toBe("$& 1 two two\n");
  });

  it("append needs no base and adds one paragraph", async () => {
    const before = await readPage(sql, "inbox");
    const after = await updatePage(sql, { slug: "inbox", append: "- Priya's birthday is in March\n", ...agent });
    expect(after.page.version).toBe(before.version + 1);
    expect(after.page.body.endsWith("\n\n- Priya's birthday is in March\n")).toBe(true);
    // A second list item, and a footnote after a footnote, continue the block.
    const more = await updatePage(sql, { slug: "inbox", append: "- Jo prefers Signal [^jo]\n", ...agent });
    expect(more.page.body.endsWith("- Priya's birthday is in March\n- Jo prefers Signal [^jo]\n")).toBe(true);
    expect(more.warnings).toEqual(["[^jo] is used but has no definition line"]);
    const cited = await updatePage(sql, { slug: "inbox", append: "[^jo]: thread:t1", ...agent });
    expect(cited.warnings).toEqual([]);
    const last = await updatePage(sql, { slug: "inbox", append: "[^x]: thread:t2", ...agent });
    expect(last.page.body.endsWith("[^jo]: thread:t1\n[^x]: thread:t2\n")).toBe(true);
  });

  it("dry run and no-op writes store nothing", async () => {
    await createPage(sql, { slug: "topics/d", title: "D", body: "a\n", ...agent });
    const dry = await updatePage(sql, { slug: "topics/d", base: 1, body: "b\n", dryRun: true, ...agent });
    expect(dry).toMatchObject({ dry_run: true, changed: true });
    const same = await updatePage(sql, { slug: "topics/d", base: 1, body: "a\n", ...agent });
    expect(same.changed).toBe(false);
    expect(await pageHistory(sql, "topics/d")).toHaveLength(1);
  });

  it("history keeps every version and revert writes a new one", async () => {
    await createPage(sql, { slug: "topics/h", title: "H", body: "first\n", ...agent });
    await updatePage(sql, { slug: "topics/h", base: 1, body: "second\n", via: "ui" });
    // A revert carries a note, so it is its own version even straight after typing.
    const reverted = await revertPage(sql, "topics/h", 1, { via: "ui" });
    expect(reverted.page).toMatchObject({ version: 3, body: "first\n", note: "revert to version 1" });
    expect((await pageHistory(sql, "topics/h")).map((v) => [v.version, v.via])).toEqual([[3, "ui"], [2, "ui"], [1, "agent"]]);
    expect((await readPage(sql, "topics/h", 2)).body).toBe("second\n");
  });

  it("a run of the owner's typing is one version, and still moves the number on", async () => {
    await createPage(sql, { slug: "topics/typing", title: "T", body: "a\n", ...agent });
    await updatePage(sql, { slug: "topics/typing", base: 1, body: "ab\n", via: "ui" });
    const again = await updatePage(sql, { slug: "topics/typing", base: 2, body: "abc\n", via: "ui" });
    expect(again.page).toMatchObject({ version: 3, body: "abc\n" });
    expect((await pageHistory(sql, "topics/typing")).map((v) => [v.version, v.via])).toEqual([[3, "ui"], [1, "agent"]]);
    // The agent read version 2, which the typing replaced: refused, not silently overwritten.
    expect((await refusal(updatePage(sql, { slug: "topics/typing", base: 2, body: "x\n", ...agent }))).code).toBe("stale-base");
    // After the agent writes, the owner's next save starts a new version.
    await updatePage(sql, { slug: "topics/typing", base: 3, append: "- agent\n", ...agent });
    const mine = await updatePage(sql, { slug: "topics/typing", base: 4, body: "mine\n", via: "ui" });
    expect((await pageHistory(sql, "topics/typing")).map((v) => v.version)).toEqual([5, 4, 3, 1]);
    expect(mine.page.version).toBe(5);
  });

  it("takes back one old change and keeps the edits since; says so when they overlap", async () => {
    await createPage(sql, { slug: "topics/tb", title: "TB", body: "Venue: open\nCaterer: open\nFlorist: open\n", via: "ui" });
    await updatePage(sql, { slug: "topics/tb", base: 1, find: "Florist: open", replace: "Florist: WRONG", ...agent });
    await updatePage(sql, { slug: "topics/tb", base: 2, find: "Venue: open", replace: "Venue: Sunol", note: "venue", via: "ui" });
    const r = await takeBackVersion(sql, "topics/tb", 2, { via: "ui" });
    expect(r.page).toMatchObject({ version: 4, body: "Venue: Sunol\nCaterer: open\nFlorist: open\n", note: "took back version 2" });
    expect((await pageAuthors(sql, "topics/tb")).map((a) => a.via)).toEqual(["ui", "ui", "ui", "ui"]);

    await updatePage(sql, { slug: "topics/tb", base: 4, find: "Caterer: open", replace: "Caterer: X", ...agent });
    await updatePage(sql, { slug: "topics/tb", base: 5, find: "Caterer: X", replace: "Caterer: Y", note: "fix", via: "ui" });
    expect((await refusal(takeBackVersion(sql, "topics/tb", 5, { via: "ui" }))).code).toBe("overlapping-edits");
    await expect(takeBackVersion(sql, "topics/tb", 1, { via: "ui" })).rejects.toBeInstanceOf(BadInput);
  });

  it("credits each line to whoever last wrote it", async () => {
    await createPage(sql, { slug: "topics/who", title: "Who", body: "mine\n", via: "ui" });
    await updatePage(sql, { slug: "topics/who", append: "theirs\n", ...agent });
    expect((await pageAuthors(sql, "topics/who")).map((a) => `${a.via}${a.version}`)).toEqual(["ui1", "ui1", "agent2", "agent2"]); // mine, a blank line, theirs, the final newline
  });

  it("two concurrent updates from the same base: one wins, one is refused", async () => {
    await createPage(sql, { slug: "topics/race", title: "Race", body: "start\n", ...agent });
    const results = await Promise.allSettled([
      updatePage(sql, { slug: "topics/race", base: 1, body: "from A\n", ...agent }),
      updatePage(sql, { slug: "topics/race", base: 1, body: "from B\n", ...agent }),
    ]);
    expect(results.filter((r) => r.status === "fulfilled")).toHaveLength(1);
    const rejected = results.find((r) => r.status === "rejected") as PromiseRejectedResult;
    expect((rejected.reason as Refused).code).toBe("stale-base");
    expect(await pageHistory(sql, "topics/race")).toHaveLength(2);
  });

  it("search finds a page by title and by body", async () => {
    await createPage(sql, { slug: "projects/austin-trip", title: "Austin trip", body: "Flights booked on Southwest for March.\n", ...agent });
    expect((await searchPages(sql, "austin"))[0]?.slug).toBe("projects/austin-trip");
    expect((await searchPages(sql, "southwest flights"))[0]?.slug).toBe("projects/austin-trip");
    expect(await searchPages(sql, "zeppelin")).toEqual([]);
  });
});

dbDescribe("entities", () => {
  it("upsert finds by identifier, then by name, and only ever adds", async () => {
    const a = await upsertEntity(sql, { kind: "person", name: "Priya", identifiers: [{ kind: "email", value: "Priya@Example.com" }] });
    expect(a.created).toBe(true);
    expect(a.identifiers).toEqual([{ kind: "email", value: "priya@example.com" }]);

    const b = await upsertEntity(sql, { kind: "person", name: "Priya Raman", identifiers: [{ kind: "email", value: "priya@example.com" }], aliases: ["PR"] });
    expect(b).toMatchObject({ id: a.id, created: false, name: "Priya Raman" });
    expect(b.aliases.sort()).toEqual(["PR", "Priya"]);

    const c = await upsertEntity(sql, { kind: "person", name: "priya", identifiers: [{ kind: "email", value: "priya@work.example" }] });
    expect(c.id).toBe(a.id);
    expect(c.identifiers).toHaveLength(2);

    expect((await showEntity(sql, "priya@work.example")).id).toBe(a.id);
    expect((await showEntity(sql, "Priya Raman")).id).toBe(a.id);
    expect((await showEntity(sql, a.id)).id).toBe(a.id);
    await expect(showEntity(sql, "nobody at all")).rejects.toBeInstanceOf(NotFound);
  });

  it("refuses identifiers that belong to two different entities", async () => {
    await upsertEntity(sql, { kind: "person", name: "A", identifiers: [{ kind: "email", value: "a@x.com" }] });
    await upsertEntity(sql, { kind: "person", name: "B", identifiers: [{ kind: "email", value: "b@x.com" }] });
    const r = await refusal(upsertEntity(sql, { kind: "person", name: "C", identifiers: [{ kind: "email", value: "a@x.com" }, { kind: "email", value: "b@x.com" }] }));
    expect(r.code).toBe("identifiers-span-entities");
  });

  it("recognises a sender by address, then by domain", async () => {
    const org = await upsertEntity(sql, { kind: "org", name: "Acme", identifiers: [{ kind: "domain", value: "acme.com" }] });
    const person = await upsertEntity(sql, { kind: "person", name: "Jo", identifiers: [{ kind: "email", value: "jo@acme.com" }] });
    expect((await entityForEmail(sql, "Jo@acme.com"))?.id).toBe(person.id);
    expect((await entityForEmail(sql, "billing@acme.com"))?.id).toBe(org.id);
    expect(await entityForEmail(sql, "x@other.com")).toBeNull();
  });
});

dbDescribe("search, context, changes", () => {
  it("unified search returns pages ahead of raw observations and never a full body", async () => {
    const long = "Passport renewal. " + "The appointment is at the federal building. ".repeat(40);
    await createPage(sql, { slug: "topics/passport", title: "Passport renewal", body: long, ...agent });
    await sql`INSERT INTO observation (id, t_start, t_end, device, source, title)
      VALUES (gen_random_uuid(), now(), now(), 'buildbot', 'gmail', 'Your passport renewal appointment')`;
    const hits = await search(sql, { q: "passport renewal" });
    expect(hits.map((h) => h.kind)).toEqual(["page", "obs"]);
    expect(hits[0]!.snippet.length).toBeLessThanOrEqual(400);
    expect(await search(sql, { q: "passport", in: ["obs"] })).toHaveLength(1);
    expect(await search(sql, { q: "nothing-matches-this" })).toEqual([]);
  });

  it("the digest carries the in_digest pages and lists other recent pages by title only", async () => {
    await createPage(sql, { slug: "topics/secret-recipe", title: "Secret recipe", body: "Eleven herbs.\n", ...agent });
    const ctx = await getContext(sql);
    expect(ctx.pages.map((p) => p.slug)).toEqual(["agent", "me"]);
    expect(ctx.markdown).toContain("The one rule");
    expect(ctx.markdown).toContain("topics/secret-recipe");
    expect(ctx.markdown).not.toContain("Eleven herbs");
  });

  it("changes reports what was recorded after a time, keyed on arrival", async () => {
    const since = new Date(Date.now() - 1000);
    // An old event recorded just now still counts.
    await sql`INSERT INTO observation (id, t_start, t_end, device, source, title)
      VALUES (gen_random_uuid(), now() - interval '3 days', now() - interval '3 days', 'buildbot', 'agent', 'filed the invoice')`;
    await createPage(sql, { slug: "topics/c", title: "C", body: "x\n", ...agent });
    const changes = await getChanges(sql, since);
    expect(changes.results.map((g) => g.kind).sort()).toEqual(["observation:agent", "page"]);
    expect(changes.results.find((g) => g.kind === "page")!.items[0]!.id).toBe("topics/c");
    expect((await getChanges(sql, new Date(Date.now() + 1000))).results).toEqual([]);
  });
});

dbDescribe("/api/v1", () => {
  const call = (method: string, path: string, body?: unknown, token = "test-agent-token-000") =>
    handleApi(
      new Request(`http://127.0.0.1:3847/api/v1${path}`, {
        method,
        headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
        body: body === undefined ? undefined : JSON.stringify(body),
      }),
    );

  it("needs the bearer token", async () => {
    expect((await call("GET", "/context", undefined, "wrong")).status).toBe(401);
    expect((await handleApi(new Request("http://127.0.0.1:3847/api/v1/context"))).status).toBe(401);
    expect((await call("GET", "/context")).status).toBe(200);
  });

  it("refuses an agent write with no thread, and stamps via=agent with the thread", async () => {
    const noThread = await call("POST", "/pages", { slug: "topics/x", title: "X", body: "x" });
    expect(noThread.status).toBe(400);
    const ok = await call("POST", "/pages", { slug: "topics/x", title: "X", body: "x", thread: "t-42" });
    expect(ok.status).toBe(200);
    expect(await ok.json()).toMatchObject({ via: "agent", source_ref: "t-42", version: 1 });
  });

  it("maps refusals to 409, missing things to 404, and nested slugs work", async () => {
    await call("POST", "/pages", { slug: "people/a/b", title: "AB", body: "x", thread: "t" });
    expect((await call("GET", "/pages/people/a/b")).status).toBe(200);
    const stale = await call("PUT", "/pages/people/a/b", { base: 7, body: "y", thread: "t" });
    expect(stale.status).toBe(409);
    expect((await stale.json()).error.code).toBe("stale-base");
    expect((await call("GET", "/pages/people/none")).status).toBe(404);
    expect((await call("GET", "/nope")).status).toBe(404);
    const log = await call("POST", "/log", { text: "replied to the plumber", thread: "t" });
    expect(log.status).toBe(200);
    const [o] = await sql<{ source: string; thread: string }[]>`SELECT source, payload->>'thread' AS thread FROM observation`;
    expect(o).toEqual({ source: "agent", thread: "t" });
  });
});
