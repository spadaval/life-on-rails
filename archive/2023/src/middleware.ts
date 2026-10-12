import { authMiddleware } from "@clerk/nextjs";
import { fetchRequestHandler } from "@trpc/server/adapters/fetch";
import { env } from "./env/server.mjs";
import { appRouter } from "./server/api/root";
import { createContext } from "./server/api/trpc";

export default authMiddleware({
  afterAuth(auth, req, evt) {
    // console.log(req);
    // console.log(auth);
    if (!req.nextUrl.pathname.startsWith("/trpc")) {
      return;
    }
    return fetchRequestHandler({
      endpoint: "/trpc",
      createContext: createContext(auth),
      req,
      router: appRouter,
      onError:
        env.NODE_ENV === "development"
          ? ({ path, error }) => {
              console.error(
                `❌ tRPC failed on ${path ?? "<no-path>"}: ${error.message}`
              );
            }
          : undefined,
    });
  },
});

export const config = {
  runtime: "experimental-edge",
  regions: ["sfo1"],
  matcher: ["/((?!.*\\..*|_next).*)", "/", "/(api|trpc)(.*)"],
};
