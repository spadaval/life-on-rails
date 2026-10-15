import { describe, expect, it } from "vitest";
import { folderOf, replaceWikiLinks, wikiLayout, wikiLinks } from "../../src/shared/wiki.js";

describe("wiki links", () => {
  it("reads each linked page once, with or without a label", () => {
    expect(wikiLinks("See [[me]] and [[people/neha-pendem|Neha]], then [[me]] again. Not [[Bad Slug]].")).toEqual(["me", "people/neha-pendem"]);
  });

  it("rewrites links", () => {
    expect(replaceWikiLinks("[[a/b]] or [[c|see c]]", (s, l) => `<${l ?? s}>`)).toBe("<a/b> or <see c>");
  });

  it("names the folder", () => {
    expect(folderOf("people/neha-pendem")).toBe("people");
    expect(folderOf("me")).toBe("");
  });
});

describe("wiki layout", () => {
  const nodes = ["me", "agent", "people/a", "people/b", "people/c", "topics/x", "topics/y", "days/1"].map((id) => ({ id, group: folderOf(id) }));
  const edges: [string, string][] = [["me", "people/a"], ["me", "people/b"], ["days/1", "topics/x"], ["topics/x", "topics/y"]];
  const dist = (p: Map<string, { x: number; y: number }>, a: string, b: string) => Math.hypot(p.get(a)!.x - p.get(b)!.x, p.get(a)!.y - p.get(b)!.y);

  it("is the same every time, whatever order the pages come in", () => {
    expect(wikiLayout(nodes, edges)).toEqual(wikiLayout([...nodes].reverse(), edges));
  });

  it("puts linked pages nearer than unlinked ones, and nothing on top of anything", () => {
    const p = wikiLayout(nodes, edges);
    expect(dist(p, "me", "people/a")).toBeLessThan(dist(p, "me", "topics/y"));
    expect(dist(p, "topics/x", "topics/y")).toBeLessThan(dist(p, "topics/y", "people/c"));
    for (const a of nodes) for (const b of nodes) if (a !== b) expect(dist(p, a.id, b.id)).toBeGreaterThan(20);
  });
});
