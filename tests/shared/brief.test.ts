import { describe, expect, it } from "vitest";
import { splitBrief } from "../../src/components/format.js";

describe("splitBrief", () => {
  it("takes the first sentence of the first line, and leaves the rest of the line in the rest", () => {
    const { lead, rest } = splitBrief("Finish the spec before 14:00. Lena is waiting.\n\n- 09:30 standup");
    expect(lead).toBe("Finish the spec before 14:00.");
    expect(rest).toBe("Lena is waiting.\n\n- 09:30 standup");
  });

  it("does not split inside a time or a number", () => {
    expect(splitBrief("Run 12.5 km at 8.30 today.").lead).toBe("Run 12.5 km at 8.30 today.");
  });

  it("takes the whole first line when it has no full stop", () => {
    expect(splitBrief("Book the flights\n## This week\n- more")).toEqual({ lead: "Book the flights", rest: "## This week\n- more" });
  });

  it("leaves nothing to open on a one-line brief", () => {
    expect(splitBrief("  A quiet day.  \n")).toEqual({ lead: "A quiet day.", rest: "" });
  });
});
