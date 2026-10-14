// TanStack Start server entry. The build turns this into dist/server/server.js,
// which exports a fetch handler and opens no socket; scripts/serve.mjs does that.
import handler, { createServerEntry } from "@tanstack/react-start/server-entry";
import { boot } from "./server/boot.js";

boot();

export default createServerEntry({
  fetch(request) {
    return handler.fetch(request);
  },
});
