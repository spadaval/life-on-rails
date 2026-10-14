import { describe, expect, it } from "vitest";
import { GPerson } from "../../src/server/google/contactsClient.js";
import { mapContact } from "../../src/server/google/contactsSync.js";
import { addressesIn } from "../../src/server/mail/bulk.js";
import { nextBirthday } from "../../src/server/people.js";
import { normalizePhone } from "../../src/shared/phone.js";

describe("normalizePhone", () => {
  it("writes one number one way", () => {
    for (const raw of ["(415) 555-0100", "415.555.0100", "4155550100", "1-415-555-0100", "+1 415 555 0100", "001 415 555 0100"]) {
      expect(normalizePhone(raw, "1")).toBe("+14155550100");
    }
    expect(normalizePhone("+44 20 7946 0018", "1")).toBe("+442079460018");
    expect(normalizePhone("020 7946 0018", "44")).toBe("+442079460018");
    expect(normalizePhone("72345", "1")).toBe("72345"); // a short code stays as it is
    expect(normalizePhone("  ", "1")).toBeNull();
  });
});

describe("mapContact", () => {
  const person = (raw: unknown) => GPerson.parse(raw);

  it("takes the primary name, nicknames as aliases, normalised numbers, and the birthday", () => {
    const c = mapContact(
      person({
        resourceName: "people/c1",
        names: [{ displayName: "Priya R.", metadata: {} }, { displayName: "Priya Raman", metadata: { primary: true } }],
        nicknames: [{ value: "PR" }],
        emailAddresses: [{ value: "Priya@Example.com" }, { value: "priya@example.com" }],
        phoneNumbers: [{ value: "(415) 555-0100", canonicalForm: "+14155550100" }, { value: "415 555 0199" }],
        birthdays: [{ date: { month: 3, day: 14 } }],
      }),
      "1",
    );
    expect(c).toEqual({
      googleId: "people/c1",
      kind: "person",
      name: "Priya Raman",
      aliases: ["PR", "Priya R."],
      emails: ["priya@example.com"],
      phones: ["+14155550100", "+14155550199"],
      birthday: { month: 3, day: 14, year: null },
    });
  });

  it("a card with only a company is an organisation; an empty card is skipped", () => {
    expect(mapContact(person({ resourceName: "people/c2", organizations: [{ name: "Acme Plumbing" }], phoneNumbers: [{ value: "555 0100 999" }] }), "1"))
      .toMatchObject({ kind: "org", name: "Acme Plumbing" });
    expect(mapContact(person({ resourceName: "people/c3", emailAddresses: [{ value: "x@y.com" }] }), "1")).toMatchObject({ kind: "person", name: "x@y.com" });
    expect(mapContact(person({ resourceName: "people/c4" }), "1")).toBeNull();
  });
});

describe("birthdays and addresses", () => {
  it("finds the next birthday, rolling to next year and handling 29 February", () => {
    expect(nextBirthday(10, 20, "2026-10-14")).toBe("2026-10-20");
    expect(nextBirthday(10, 14, "2026-10-14")).toBe("2026-10-14");
    expect(nextBirthday(3, 1, "2026-10-14")).toBe("2027-03-01");
    expect(nextBirthday(2, 29, "2026-10-14")).toBe("2027-02-28");
    expect(nextBirthday(2, 29, "2027-03-01")).toBe("2028-02-29");
  });

  it("pulls every address out of To and Cc", () => {
    expect(addressesIn('"Raman, Priya" <Priya@Example.com>, jo@x.org', "Team <team@list.example.com>; bob@b.io", null)).toEqual([
      "priya@example.com", "jo@x.org", "team@list.example.com", "bob@b.io",
    ]);
  });
});
