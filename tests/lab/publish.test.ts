// scripts/lab/publish.sh (lor-zwfd): the served tree is rebuilt from the script's list, and
// nothing else in LOR_LAB_DIR survives.
import { spawnSync } from "node:child_process";
import { existsSync, mkdirSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";

function publish(home: string, lab: string) {
  return spawnSync("scripts/lab/publish.sh", [], {
    encoding: "utf8",
    env: { PATH: process.env.PATH, HOME: home, LOR_ENV_FILE: "/dev/null", LOR_LAB_DIR: lab },
  });
}

function home() {
  const h = mkdtempSync(path.join(tmpdir(), "lor-lab-home-"));
  const gallery = path.join(h, ".local/state/lor-shots/dashboard");
  mkdirSync(path.join(gallery, "previous"), { recursive: true });
  writeFileSync(path.join(gallery, "index.html"), "gallery");
  writeFileSync(path.join(gallery, "today@1500.png"), "png");
  writeFileSync(path.join(gallery, "previous/today@1500.png"), "old png");
  return h;
}

describe("publish.sh", () => {
  it("copies the galleries, writes the index, and keeps nothing it does not name", () => {
    const h = home();
    const lab = path.join(h, "served/lab");
    const first = publish(h, lab);
    expect(first.status, first.stderr).toBe(0);
    writeFileSync(path.join(lab, "stale.html"), "left over");
    mkdirSync(path.join(lab, "old-folder"));

    const again = publish(h, lab);
    expect(again.status, again.stderr).toBe(0);
    expect(readFileSync(path.join(lab, "shots/dashboard/index.html"), "utf8")).toBe("gallery");
    expect(readFileSync(path.join(lab, "shots/dashboard/previous/today@1500.png"), "utf8")).toBe("old png");
    expect(existsSync(path.join(lab, "stale.html"))).toBe(false);
    expect(existsSync(path.join(lab, "old-folder"))).toBe(false);
    const index = readFileSync(path.join(lab, "index.html"), "utf8");
    expect(index).toContain('Screenshot galleries: <a href="shots/dashboard/">dashboard</a>');
    expect(index).toMatch(/published from [0-9a-f]{7,}/);
    expect(index).not.toContain('href="dashboard/"');
  });

  it("refuses a directory it did not make, and one inside the checkout", () => {
    const h = home();
    const other = path.join(h, "documents");
    mkdirSync(other);
    writeFileSync(path.join(other, "keep.txt"), "mine");
    const res = publish(h, other);
    expect(res.status).not.toBe(0);
    expect(res.stderr).toMatch(/did not publish/);
    expect(readFileSync(path.join(other, "keep.txt"), "utf8")).toBe("mine");

    for (const dir of [h, "/", path.resolve("scripts/lab-out"), "relative/lab"]) {
      const r = publish(h, dir);
      expect(r.status, dir).not.toBe(0);
    }
    expect(existsSync("scripts/lab-out")).toBe(false);
  });
});
