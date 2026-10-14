// Errors the API and UI translate into statuses, and the CLI into exit codes.
import type { JsonObject } from "../shared/json.js";

/** A write rule said no. HTTP 409, `lor` exit 6. */
export class Refused extends Error {
  constructor(
    public code: string,
    message: string,
    public detail: JsonObject = {},
  ) {
    super(message);
    this.name = "Refused";
  }
}

/** HTTP 404, `lor` exit 5. */
export class NotFound extends Error {
  constructor(message: string) {
    super(message);
    this.name = "NotFound";
  }
}

/** HTTP 400, `lor` exit 2. */
export class BadInput extends Error {
  constructor(message: string) {
    super(message);
    this.name = "BadInput";
  }
}
