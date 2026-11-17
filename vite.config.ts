import { existsSync, realpathSync } from "node:fs";
import { defineConfig, searchForWorkspaceRoot } from "vite";
import { tanstackStart } from "@tanstack/react-start/plugin/vite";
import viteReact from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// In a worktree node_modules is a symlink to the main checkout's (verify-lor, scripts/ui-dev.sh):
// the dev server may serve files from where it points, or the fonts are refused.
const modules = existsSync("node_modules") ? realpathSync("node_modules") : null;

export default defineConfig({
  // scripts/ui-dev.sh gives each checkout its own, so worktrees sharing node_modules do not share it.
  cacheDir: process.env.LOR_VITE_CACHE_DIR || "node_modules/.vite",
  plugins: [
    tailwindcss(),
    tanstackStart(), // MUST come before react()
    viteReact(),
  ],
  server: {
    host: "127.0.0.1",
    port: 3847,
    fs: { allow: [searchForWorkspaceRoot(process.cwd()), ...(modules ? [modules] : [])] },
  },
});
