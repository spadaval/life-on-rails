import { describe, expect, it } from "vitest";
import { blame, join, merge3, takeBack } from "../../src/shared/merge.js";

const base = "# Wedding\n\nVenue: open\nCaterer: open\nFlorist: open\n\nNotes.\n";

describe("three-way merge", () => {
  it("takes each side's edits when they touch different stretches", () => {
    const mine = base.replace("Venue: open", "Venue: Sunol Valley");
    const theirs = base.replace("Florist: open", "Florist: Petal & Stem").replace("Notes.\n", "Notes.\n- deposit due 1 Nov\n");
    const m = merge3(base, mine, theirs);
    expect(m.conflicts).toBe(0);
    expect(m.text).toBe("# Wedding\n\nVenue: Sunol Valley\nCaterer: open\nFlorist: Petal & Stem\n\nNotes.\n- deposit due 1 Nov\n");
  });

  it("is a conflict when both change the same lines differently, and settles by choice", () => {
    const mine = base.replace("Caterer: open", "Caterer: Dosa Place");
    const theirs = base.replace("Caterer: open", "Caterer: Biryani Pot");
    const m = merge3(base, mine, theirs);
    expect(m).toMatchObject({ conflicts: 1, text: null });
    const c = m.chunks.find((x) => x.kind === "conflict");
    expect(c).toEqual({ kind: "conflict", base: ["Caterer: open"], mine: ["Caterer: Dosa Place"], theirs: ["Caterer: Biryani Pot"] });
    expect(join(m.chunks, (x) => x.theirs)).toBe(theirs);
    expect(join(m.chunks, (x) => [...x.mine, ...x.theirs])).toContain("Caterer: Dosa Place\nCaterer: Biryani Pot");
  });

  it("merges different words of one paragraph, and an addition next to an edit", () => {
    const para = "She has a dog called Biscuit and lives in Austin.\n";
    expect(merge3(para, para.replace("a dog", "a big dog"), para.replace("Austin", "Round Rock")).text).toBe("She has a big dog called Biscuit and lives in Round Rock.\n");
    const list = "- one\n- two\n";
    expect(merge3(list, "- one\n- two\n- three\n", "- one\n- TWO\n").text).toBe("- one\n- TWO\n- three\n");
    // The same word changed two ways is still a clash.
    expect(merge3(para, para.replace("Biscuit", "Biscotti"), para.replace("Biscuit", "Bisquick")).conflicts).toBe(1);
  });

  it("the same edit on both sides is no conflict; neither is nothing changing", () => {
    const both = base.replace("Venue: open", "Venue: Sunol");
    expect(merge3(base, both, both).text).toBe(both);
    expect(merge3(base, base, base).text).toBe(base);
    expect(merge3(base, base, both).text).toBe(both);
  });

  it("handles deletions and additions at the ends", () => {
    const mine = base.replace("# Wedding\n\n", "");
    const theirs = `${base}Last line.`;
    expect(merge3(base, mine, theirs).text).toBe("Venue: open\nCaterer: open\nFlorist: open\n\nNotes.\nLast line.");
    expect(merge3("", "a", "").text).toBe("a");
  });
});

describe("taking back one change", () => {
  it("undoes an old change and keeps everything since", () => {
    const v1 = base;
    const v2 = base.replace("Florist: open", "Florist: WRONG");
    const v3 = v2.replace("Venue: open", "Venue: Sunol");
    expect(takeBack(v1, v2, v3)).toBe(base.replace("Venue: open", "Venue: Sunol"));
  });
  it("takes back a line even when the next line was added after it", () => {
    const v5 = "intro\n";
    const v6 = "intro\n- meanwhile\n";
    const v7 = "intro\n- meanwhile\n- and this\n";
    expect(takeBack(v5, v6, v7)).toBe("intro\n- and this\n");
  });
  it("refuses when later edits changed the same words", () => {
    const v2 = base.replace("Florist: open", "Florist: WRONG");
    const v3 = v2.replace("Florist: WRONG", "Florist: still wrong");
    expect(takeBack(base, v2, v3)).toBeNull();
  });
});

describe("who wrote what", () => {
  it("credits each line to the version that last wrote it", () => {
    const who = blame([
      { version: 3, via: "ui", body: "a\nB\nc\nd" },
      { version: 1, via: "seed", body: "a\nb\nc" },
      { version: 2, via: "agent", body: "a\nb\nc\nd" },
    ]);
    expect(who.map((w) => `${w.via}${w.version}`)).toEqual(["seed1", "ui3", "seed1", "agent2"]);
  });
});
