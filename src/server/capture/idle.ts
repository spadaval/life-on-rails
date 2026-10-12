// Idle helpers. Idle detection pauses the running block only when a
// CLASS=user seat exists — never on the current manager-class session.
// Idle rows are source=linux_activity title=idle (never source=system).
export interface SeatInfo {
  sessionClass: string | null; // "user" | "manager" | ...
  hasSeat: boolean;
}

export function shouldReportIdle(seat: SeatInfo): boolean {
  return seat.sessionClass === "user" && seat.hasSeat;
}

/** Parse `loginctl show-session $XDG_SESSION_ID -p Class -p Seat`-style output. */
export function parseSeatInfo(output: string): SeatInfo {
  let sessionClass: string | null = null;
  let hasSeat = false;
  for (const line of output.split("\n")) {
    const [k, v] = line.split("=");
    if (k === "Class" && v) sessionClass = v.trim();
    if (k === "Seat" && v && v.trim() !== "") hasSeat = true;
  }
  return { sessionClass, hasSeat };
}
