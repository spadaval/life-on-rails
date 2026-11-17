// The dashboard prototype (lor-5uxk): a static Vite and React app, no server. `pnpm proto:dev`
// serves it with hot reload on 127.0.0.1:3859; `pnpm proto:build` writes dist/, which
// scripts/lab/publish.sh serves at /lab/dashboard/.
import { execFileSync } from "node:child_process";
import { existsSync, realpathSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { type Plugin, defineConfig, searchForWorkspaceRoot } from "vite";

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, "../..");
// In a worktree node_modules is a symlink to the main checkout's: serve files from where it points.
const modules = existsSync(path.join(repo, "node_modules")) ? realpathSync(path.join(repo, "node_modules")) : null;

/** dist/.lor-stamp: the commit and time the build was made from, which the lab's index shows. */
function stamp(): Plugin {
  return {
    name: "lor-stamp",
    apply: "build",
    closeBundle() {
      const git = (...a: string[]) => execFileSync("git", ["-C", repo, ...a], { encoding: "utf8" }).trim();
      let commit = "unknown";
      try {
        commit = git("rev-parse", "--short", "HEAD") + (git("status", "--porcelain") ? " (uncommitted changes)" : "");
      } catch {}
      writeFileSync(path.join(here, "dist/.lor-stamp"), `commit=${commit}\ntime=${new Date().toISOString()}\n`);
    },
  };
}

export default defineConfig({
  root: here,
  // Relative, so the build works under /lab/dashboard/ or from any folder.
  base: "./",
  // Its own, not the main checkout's node_modules/.vite, which worktrees share through the symlink.
  cacheDir: path.join(here, ".vite"),
  plugins: [tailwindcss(), react(), stamp()],
  server: {
    host: "127.0.0.1",
    port: 3859,
    strictPort: true,
    fs: { allow: [searchForWorkspaceRoot(here), repo, ...(modules ? [modules] : [])] },
  },
  build: { outDir: path.join(here, "dist"), emptyOutDir: true, chunkSizeWarningLimit: 2000 },
});
