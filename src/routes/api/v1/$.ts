import { createFileRoute } from "@tanstack/react-router";
import { handleApi } from "../../../server/api/v1.js";

const handler = ({ request }: { request: Request }) => handleApi(request);

export const Route = createFileRoute("/api/v1/$")({
  server: {
    handlers: { GET: handler, POST: handler, PUT: handler, DELETE: handler },
  },
});
