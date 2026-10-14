// What people tracking adds to the agent's digest: who is overdue against the
// owner's cadence, and whose birthday is within a week.
import { birthdayLine, dueLine, listPeople } from "../people.js";
import { registerContextSection } from "./context.js";

registerContextSection(async (db, now) => {
  const overdue = (await listPeople(db, { filter: "overdue", now, limit: 10 })).people;
  const birthdays = (await listPeople(db, { filter: "birthdays", now, birthdayWindow: 7, limit: 10 })).people;
  if (overdue.length === 0 && birthdays.length === 0) return null;
  const out: string[] = [];
  if (birthdays.length) out.push("Birthdays this week:", ...birthdays.map((p) => `- ${birthdayLine(p)}`));
  if (overdue.length) out.push(...(out.length ? [""] : []), "Overdue to catch up with:", ...overdue.map((p) => `- ${dueLine(p)}`));
  return { title: "People", markdown: out.join("\n") };
});
