import { describe, expect, it } from "vitest";
import { activeSigil, parseQuickAdd, suggest, tokenKey } from "../../src/shared/quickAdd.js";

// Thursday.
const today = "2026-10-15";
const parents = [
  { id: "p1", name: "Wedding vendors" },
  { id: "p2", name: "Wedding" },
  { id: "p3", name: "Neha and the wedding" },
  { id: "p4", name: "Life on Rails" },
  { id: "p5", name: "Move timing and notice" },
];
const people = [
  { id: "e1", name: "Dad" },
  { id: "e2", name: "Neha Pendem" },
  { id: "e3", name: "Dipika Badri" },
];
const parse = (text: string, ignore?: Set<string>) => parseQuickAdd(text, { today, parents, people, ignore });

describe("quick add", () => {
  it("takes the title, parent, due date and agent out of one line", () => {
    const p = parse("Book venue #Wedding vendors fri @agent");
    expect(p).toMatchObject({ title: "Book venue", parentId: "p1", due: "2026-10-16", assignee: "agent", waitingOnId: null });
    expect(p.tokens.map((t) => t.kind)).toEqual(["parent", "due", "agent"]);
  });

  it("prefers the longest whole name after #", () => {
    expect(parse("call caterer #wedding vendors").parentId).toBe("p1");
    expect(parse("call caterer #Wedding").parentId).toBe("p2");
    expect(parse("#wedding, then lunch").parentId).toBe("p2");
  });

  it("accepts a name without spaces, or the start of exactly one name", () => {
    expect(parse("x #weddingvendors").parentId).toBe("p1");
    expect(parse("x #lifeon").parentId).toBe("p4");
    expect(parse("x #wedd").parentId).toBeNull(); // two names start with it
    expect(parse("x #nothing").parentId).toBeNull();
  });

  it("reads @Name as waiting on that person", () => {
    const p = parse("Deposit back @Dad tomorrow");
    expect(p).toMatchObject({ title: "Deposit back", waitingOnId: "e1", due: "2026-10-16", assignee: "owner" });
    expect(parse("Reply @neha pendem").waitingOnId).toBe("e2");
    expect(parse("Reply @dip").waitingOnId).toBe("e3");
  });

  it("leaves # and @ inside words alone", () => {
    expect(parse("email bob@agent.com about C#").tokens).toEqual([]);
  });

  describe("dates", () => {
    const due = (s: string) => parse(`thing ${s}`).due;
    it("knows relative days", () => {
      expect(due("today")).toBe("2026-10-15");
      expect(due("tonight")).toBe("2026-10-15");
      expect(due("tomorrow")).toBe("2026-10-16");
      expect(due("tmr")).toBe("2026-10-16");
      expect(due("in 3 days")).toBe("2026-10-18");
      expect(due("in a week")).toBe("2026-10-22");
      expect(due("in 2 months")).toBe("2026-12-15");
    });
    it("counts weekdays forward, with today as today", () => {
      expect(due("thu")).toBe("2026-10-15");
      expect(due("friday")).toBe("2026-10-16");
      expect(due("mon")).toBe("2026-10-19");
      expect(due("wednesday")).toBe("2026-10-21");
      expect(due("next week")).toBe("2026-10-19");
      expect(due("next fri")).toBe("2026-10-23");
      expect(due("next month")).toBe("2026-11-01");
      expect(due("weekend")).toBe("2026-10-17");
    });
    it("reads written dates, rolling past ones into next year", () => {
      expect(due("oct 20")).toBe("2026-10-20");
      expect(due("20 October")).toBe("2026-10-20");
      expect(due("Nov 3rd")).toBe("2026-11-03");
      expect(due("jan 5")).toBe("2027-01-05");
      expect(due("oct 1")).toBe("2027-10-01");
      expect(due("10/20")).toBe("2026-10-20");
      expect(due("3/1/27")).toBe("2027-03-01");
      expect(due("2026-12-24")).toBe("2026-12-24");
      expect(due("feb 30")).toBeNull();
    });
    it("swallows on/by/due in front of a date", () => {
      const p = parse("Pay rent by fri");
      expect(p).toMatchObject({ title: "Pay rent", due: "2026-10-16" });
      expect(parse("Dentist on 20 oct").title).toBe("Dentist");
    });
    it("leaves words that only look like days alone unless asked", () => {
      expect(parse("Buy sun cream").due).toBeNull();
      expect(parse("Get wed").due).toBeNull();
      expect(parse("Picnic on sat").due).toBe("2026-10-17");
      expect(parse("Work on friday's deck").due).toBeNull();
      expect(parse("Mondays are hard").due).toBeNull();
    });
    it("uses the first date and keeps later ones as text", () => {
      const p = parse("move tue meeting to wednesday");
      expect(p.due).toBe("2026-10-20");
      expect(p.title).toBe("move meeting to wednesday");
    });
  });

  it("keeps a dismissed token as text", () => {
    const first = parse("Watch Friday Night Lights");
    expect(first.due).toBe("2026-10-16");
    const p = parse("Watch Friday Night Lights", new Set([tokenKey(first.tokens[0]!)]));
    expect(p).toMatchObject({ title: "Watch Friday Night Lights", due: null });
  });
});

describe("suggestions", () => {
  it("finds the sigil being typed", () => {
    expect(activeSigil("Book #wed", 9)).toEqual({ sigil: "#", start: 5, query: "wed" });
    expect(activeSigil("Book #wedding ve", 16)).toEqual({ sigil: "#", start: 5, query: "wedding ve" });
    expect(activeSigil("Book venue", 10)).toBeNull();
    expect(activeSigil("a@b", 3)).toBeNull();
  });

  it("ranks starts-with first, then word starts", () => {
    expect(suggest("wed", parents).map((n) => n.id)).toEqual(["p2", "p1", "p3"]);
    expect(suggest("weddi", parents).map((n) => n.id)).toEqual(["p2", "p1", "p3"]);
    expect(suggest("rails", parents).map((n) => n.id)).toEqual(["p4"]);
    expect(suggest("", people)).toHaveLength(3);
    expect(suggest("Wedding ", parents)).toEqual([]); // a whole name: nothing left to pick
    expect(suggest("life on rails", parents)).toEqual([]);
  });
});
