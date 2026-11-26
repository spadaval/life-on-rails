// The dashboard's one look (DESIGN.md, lor-mxm9): every button names one of the three levels,
// and text takes the four sizes of the type scale, not a literal pixel size. The arc's readout
// is the one place with display sizes of its own.
import { readdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { describe, expect, it } from "vitest";

const ROOT = path.resolve(import.meta.dirname, "..");
const DIRS = ["src/routes", "src/components"];

function sources(): { file: string; text: string }[] {
  const out: { file: string; text: string }[] = [];
  const walk = (dir: string) => {
    for (const e of readdirSync(path.join(ROOT, dir), { withFileTypes: true })) {
      const rel = path.join(dir, e.name);
      if (e.isDirectory()) walk(rel);
      else if (e.name.endsWith(".tsx")) out.push({ file: rel, text: readFileSync(path.join(ROOT, rel), "utf8") });
    }
  };
  DIRS.forEach(walk);
  return out;
}

// Class strings that contain the word btn: a className="..." or className={`...`} value.
const classStrings = (text: string) => [...text.matchAll(/className=(?:"([^"]*)"|\{`([^`]*)`\})/g)].map((m) => m[1] ?? m[2]!);

describe("the look", () => {
  it("gives every .btn one of the three levels", () => {
    const bare: string[] = [];
    for (const { file, text } of sources()) {
      for (const cls of classStrings(text)) {
        if (!/(^|\s)btn(\s|$)/.test(cls)) continue;
        if (!/btn-(primary|secondary|quiet)\b/.test(cls)) bare.push(`${file}: ${cls}`);
      }
    }
    expect(bare).toEqual([]);
  });

  it("sizes text with the type scale, except the arc's readout", () => {
    const literal: string[] = [];
    for (const { file, text } of sources()) {
      for (const m of text.matchAll(/\btext-\[(\d[\d.]*)px\]/g)) literal.push(`${file}: ${m[0]}`);
      for (const m of text.matchAll(/\btext-(xs|sm|base|lg|xl|\dxl)\b/g)) literal.push(`${file}: ${m[0]}`);
    }
    // The arc's readout (src/routes/index.tsx): the display figure, its unit word and the running timer.
    const display = new Set(["text-[56px]", "text-[62px]", "text-[20px]", "text-[21px]"]);
    expect(literal.filter((l) => !(l.startsWith("src/routes/index.tsx") && display.has(l.split(": ")[1]!)))).toEqual([]);
  });
});
