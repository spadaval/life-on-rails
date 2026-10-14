import { describe, expect, it } from "vitest";
import { type FillEvent, wantedFill } from "../../src/server/attribution/fill.js";

const H = 3600_000;
const ev = (eventId: string, start: number, end: number): FillEvent => ({ calendarId: "c", eventId, title: eventId, tStart: start * H, tEnd: end * H });
const shape = (blocks: ReturnType<typeof wantedFill>) => blocks.map((b) => [b.eventId, b.tStart / H, b.tEnd / H]);

describe("wantedFill", () => {
  it("fills an event's whole span when nothing is taken", () => {
    expect(shape(wantedFill([ev("standup", 9, 9.5)], []))).toEqual([["standup", 9, 9.5]]);
  });

  it("leaves out taken time, splitting an event into fragments", () => {
    // Meeting 9–11, a timer ran 10–10:30.
    expect(shape(wantedFill([ev("m", 9, 11)], [{ t: 10 * H, tEnd: 10.5 * H }]))).toEqual([["m", 9, 10], ["m", 10.5, 11]]);
    expect(wantedFill([ev("m", 9, 10)], [{ t: 8 * H, tEnd: 11 * H }])).toEqual([]);
  });

  it("gives overlapping time to the shorter event", () => {
    // A one-hour dentist visit inside an afternoon-long hold.
    expect(shape(wantedFill([ev("hold", 13, 17), ev("dentist", 14, 15)], []))).toEqual([
      ["hold", 13, 14], ["dentist", 14, 15], ["hold", 15, 17],
    ]);
    // Equal length: the earlier start keeps the overlap.
    expect(shape(wantedFill([ev("b", 10.5, 11.5), ev("a", 10, 11)], []))).toEqual([["a", 10, 11], ["b", 11, 11.5]]);
  });

  it("an open timer takes everything after its start", () => {
    const open = [{ t: 10 * H, tEnd: 1e15 }];
    expect(shape(wantedFill([ev("m", 9, 11), ev("later", 15, 16)], open))).toEqual([["m", 9, 10]]);
  });

  it("drops slivers under a minute", () => {
    expect(wantedFill([ev("m", 9, 10)], [{ t: 9 * H + 30_000, tEnd: 10 * H }])).toEqual([]);
  });
});
