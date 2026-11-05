import { inflateSync } from "node:zlib";
import { describe, expect, it } from "vitest";
import { type PickInput, goalsHtml, nextScreen, pickOne, screenOf, shortTitle, splitBrief, tasksHtml, todayHtml } from "../../src/server/trmnl/frame.js";
import { grayPng2 } from "../../src/server/trmnl/png.js";
import { at, sample, sampleGoals, sampleTasks } from "./sample.js";
const text = (html: string) => html.replace(/<style>[\s\S]*?<\/style>/, "").replace(/<[^>]+>/g, " ").replace(/&nbsp;/g, " ").replace(/\s+/g, " ");

describe("grayPng2", () => {
  it("packs four pixels to a byte, leftmost highest, a filter byte per row", () => {
    const png = grayPng2(5, 2, Uint8Array.from([0, 1, 2, 3, 3, 3, 2, 1, 0, 0]));
    expect(png.subarray(1, 4).toString()).toBe("PNG");
    expect(png.readUInt32BE(16)).toBe(5);
    expect(png.readUInt32BE(20)).toBe(2);
    expect([png[24], png[25]]).toEqual([2, 0]);
    const idatLen = png.readUInt32BE(33);
    expect(png.subarray(37, 41).toString()).toBe("IDAT");
    const raw = inflateSync(png.subarray(41, 41 + idatLen));
    expect([...raw]).toEqual([0, 0b00011011, 0b11000000, 0, 0b11100100, 0]);
  });

  it("refuses a pixel count that does not match", () => {
    expect(() => grayPng2(2, 2, new Uint8Array(3))).toThrow();
  });
});

describe("todayHtml", () => {
  it("draws the one thing inside the arc, upcoming events and deadlines beside it, and no countdown", () => {
    const t = text(todayHtml(sample()));
    expect(t).toContain("Thursday 15 October");
    expect(t).toContain("Running since 13:20 Thesis");
    expect(t).toContain("16:00 Supervisor meeting");
    // Near the page edge a label is cut to the room it has, never below 20 characters.
    expect(t).toContain("19:30 Dinner with S…");
    expect(t).toContain("17:00 Submit reimburse…");
    const wide = text(todayHtml(sample({ now: at("09:00"), deadlines: [{ title: "Get Wake Emergency Physicians to resubmit the claim", at: at("15:00").toISOString() }] })));
    expect(wide).toContain("15:00 Get Wake Emergency Physic… (due)");
    expect(t).not.toContain("Lab standup");
    expect(t).not.toMatch(/left|Updated/);
  });

  it("names a goal's stretch once it runs 40 minutes, joining blocks a few minutes apart", () => {
    const t = text(todayHtml(sample()));
    expect(t.match(/Thesis/g)?.length).toBe(3); // the 09:00-10:30 stretch, the running one, and the one thing
    expect(t).not.toContain("Strength");
  });

  it("puts the time it was drawn beside the reticle, and leaves the reticle off outside the waking day", () => {
    expect(text(todayHtml(sample()))).toContain("14:55");
    expect(todayHtml(sample({ now: at("23:10") }))).not.toContain('class="now"');
  });

  it("says so when there is nothing pressing, and escapes what it prints", () => {
    expect(text(todayHtml(sample({ one: null })))).toContain("Nothing pressing");
    const html = todayHtml(sample({ alerts: ["Battery 12%"], one: { kicker: "Pinned", text: "<b>bold</b> & co", why: null } }));
    expect(html).toContain('<span class="alert cond">Battery 12%</span>');
    expect(html).toContain("&lt;b&gt;bold&lt;/b&gt; &amp; co");
  });

  it("fills the square of the current screen in the foot", () => {
    expect(todayHtml(sample())).toContain('<i class="on" data-screen="today">');
    expect(goalsHtml(sampleGoals())).toContain('<i class="on" data-screen="goals">');
    expect(tasksHtml(sampleTasks())).toContain('<i class="on" data-screen="tasks">');
  });
});

describe("pickOne", () => {
  const base = (over: Partial<PickInput> = {}): PickInput => ({
    now: at("14:55"),
    timezone: "America/New_York",
    pin: null,
    running: null,
    events: [],
    brief: null,
    flagged: [],
    ...over,
  });
  const meeting = { title: "Supervisor meeting", t_start: at("15:20").toISOString(), t_end: at("16:00").toISOString() };

  it("takes the first of: pin, timer, an event soon, the brief, a flag", () => {
    const all = base({
      pin: { text: "Call the landlord", why: "before 5", until: null },
      running: { name: "Thesis", since: at("13:20").toISOString() },
      events: [meeting],
      brief: "Send the Hawthorne notice. Due Friday.",
      flagged: [{ title: "Approve the dentist reschedule", note: "they need a yes" }],
    });
    expect(pickOne(all)).toEqual({ kicker: "Pinned", text: "Call the landlord", why: "before 5" });
    expect(pickOne({ ...all, pin: null })?.kicker).toBe("Running since 13:20");
    expect(pickOne({ ...all, pin: null, running: null })).toEqual({ kicker: "At 15:20", text: "Supervisor meeting", why: "15:20 – 16:00" });
    expect(pickOne({ ...all, pin: null, running: null, events: [] })).toEqual({ kicker: "From this morning's brief", text: "Send the Hawthorne notice.", why: "Due Friday." });
    expect(pickOne({ ...all, pin: null, running: null, events: [], brief: null })).toEqual({ kicker: "Flagged", text: "Approve the dentist reschedule", why: "they need a yes" });
    expect(pickOne(base())).toBeNull();
  });

  it("drops a pin once it expires", () => {
    const pin = { text: "Call the landlord", why: null, until: at("14:00").toISOString() };
    expect(pickOne(base({ pin, brief: "Do the thing today, please." }))?.kicker).toBe("From this morning's brief");
  });

  it("counts an event under way, and one starting within half an hour, not later", () => {
    const now = base({ events: [{ ...meeting, t_start: at("14:30").toISOString() }] });
    expect(pickOne(now)).toEqual({ kicker: "Now", text: "Supervisor meeting", why: "until 16:00" });
    expect(pickOne(base({ events: [{ ...meeting, t_start: at("15:40").toISOString() }] }))).toBeNull();
  });

  it("splits the brief's line into the action and its reason, but not on a short prefix", () => {
    expect(splitBrief("Give Hawthorne written notice. Due Fri 6 Nov, two days left.")).toEqual(["Give Hawthorne written notice.", "Due Fri 6 Nov, two days left."]);
    expect(splitBrief("Dr. Lee at 3 about the knee")).toEqual(["Dr. Lee at 3 about the knee", null]);
  });
});

describe("shortTitle", () => {
  it("prefers the goal's own short title, else cuts a trailing parenthesis", () => {
    expect(shortTitle("Make and play (hobbies and side projects)")).toBe("Make and play");
    expect(shortTitle("Make and play (hobbies and side projects)", "Make & play")).toBe("Make & play");
    expect(shortTitle("(just a parenthesis)")).toBe("(just a parenthesis)");
  });
});

describe("screens", () => {
  it("a timer or power-on draws Today", () => {
    expect(nextScreen("tasks", "timer", null)).toBe("today");
    expect(nextScreen("goals", "powercycle", null)).toBe("today");
    expect(nextScreen("goals", null, null)).toBe("today");
  });

  it("a middle tap from our firmware redraws the screen it is browsing", () => {
    expect(nextScreen("today", "button", null, "plugin-goals0-1793846222")).toBe("goals");
    expect(nextScreen("today", "button", null, "plugin-today0-1793846222")).toBe("today");
    expect(nextScreen("goals", "timer", null, "plugin-tasks0-1793846222")).toBe("today");
    expect(screenOf("current.bmp")).toBeNull();
  });

  it("stock firmware's Next Screen steps through and wraps; a special function goes home", () => {
    expect(nextScreen("today", "button", null)).toBe("goals");
    expect(nextScreen("goals", "button", null)).toBe("tasks");
    expect(nextScreen("tasks", "button", null)).toBe("today");
    expect(nextScreen("tasks", "button", "restart_playlist")).toBe("today");
  });
});

describe("goalsHtml", () => {
  it("shows the week against the target, a full meter at most, and the next task with how late it is", () => {
    const html = goalsHtml(sampleGoals());
    const t = text(html);
    expect(t).toContain("Fri 9 Oct – Thu 15 Oct");
    expect(t).toContain("Thesis Send chapter 3 outline · 27 Oct");
    expect(t).toContain("9.5 / 15h");
    expect(t).toContain("Book a tutor · 3 days late");
    expect(t).toContain("Strength Nothing open");
    expect(t).toContain("Find a flat no weekly target Call the agent");
    expect(html).toContain("width:100.0%");
  });

  it("counts goals beyond seven rows, and says so when there are none", () => {
    const goals = Array.from({ length: 9 }, (_, i) => ({ title: `G${i}`, ink: 4, hours: [0, 0, 0, 0, 0, 0, 1], target: 2, next: null }));
    const t = text(goalsHtml(sampleGoals({ goals })));
    expect(t).toContain("2 more");
    expect(t).not.toContain("G7");
    expect(text(goalsHtml(sampleGoals({ goals: [] })))).toContain("No goals yet");
  });
});

describe("tasksHtml", () => {
  it("groups by when they are due, says how late, and who they wait on", () => {
    const t = text(tasksHtml(sampleTasks()));
    expect(t).toContain("10 open · 1 late · 4 undated");
    expect(t).toMatch(/Late 1 Renew car registration 2 days late Today 1 Submit reimbursement form Today This week 3/);
    expect(t).toContain("waiting on Priya");
    expect(t).toContain("Later 1 Send chapter 3 outline to the committee 27 Oct");
    expect(t).toContain("No date 4 Fix the bike's back brake");
    expect(text(tasksHtml(sampleTasks({ tasks: [] })))).toContain("Nothing open");
  });

  it("folds what does not fit into a count", () => {
    const tasks = Array.from({ length: 40 }, (_, i) => ({ title: `T${i}`, due: i < 30 ? "2026-10-16" : null, overdue: false, waiting: null, flagged: false, ink: null }));
    const t = text(tasksHtml(sampleTasks({ tasks })));
    expect(t).toMatch(/\d+ not shown/);
    expect(t).toMatch(/and \d+ more/);
  });
});
