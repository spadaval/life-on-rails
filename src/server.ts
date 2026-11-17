// TanStack Start server entry. The build turns this into dist/server/server.js,
// which exports a fetch handler and opens no socket; scripts/serve.mjs does that.
import handler, { createServerEntry } from "@tanstack/react-start/server-entry";
import { boot } from "./server/boot.js";
import { clockForRequest } from "./server/clock.js";

boot();

export default createServerEntry({
  // On the test database alone, a request can fix the clock (`at=`; src/server/clock.ts).
  fetch(request) {
    return clockForRequest(request, () => handler.fetch(request));
  },
});
