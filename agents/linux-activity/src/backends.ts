// Probe order: hyprland → sway/wlroots → x11 → loginctl-if-user-seat → none.
// No exploit detail: compositor CLIs only, no keystroke/clipboard/screenshot.
import { execFile } from "node:child_process";

export type Backend = "hyprland" | "sway" | "x11" | "loginctl" | "none";

export interface FocusSample {
  app: string | null;
  title: string | null;
}

function run(cmd: string, args: string[], timeoutMs = 3000): Promise<string | null> {
  return new Promise<string | null>((resolve) => {
    execFile(cmd, args, { timeout: timeoutMs, encoding: "utf8" }, (err, stdout) => {
      if (err) return resolve(null);
      resolve(stdout);
    });
  });
}

export async function detectBackend(): Promise<Backend> {
  if (process.env.HYPRLAND_INSTANCE_SIGNATURE) {
    const out = await run("hyprctl", ["activewindow", "-j"]);
    if (out !== null) return "hyprland";
  }
  if (process.env.SWAYSOCK || process.env.WAYLAND_DISPLAY) {
    const out = await run("swaymsg", ["-t", "get_tree"]);
    if (out !== null) return "sway";
  }
  if (process.env.DISPLAY) {
    const out = await run("xprop", ["-root", "_NET_ACTIVE_WINDOW"]);
    if (out !== null) return "x11";
  }
  return "none";
}

export async function sampleFocus(backend: Backend): Promise<FocusSample | null> {
  try {
    if (backend === "hyprland") {
      const out = await run("hyprctl", ["activewindow", "-j"]);
      if (!out) return null;
      const j = JSON.parse(out) as { class?: string; title?: string };
      return { app: j.class ?? null, title: j.title ?? null };
    }
    if (backend === "sway") {
      const out = await run("swaymsg", ["-t", "get_tree"]);
      if (!out) return null;
      return { app: "sway", title: out.slice(0, 200) };
    }
    if (backend === "x11") {
      const out = await run("xprop", ["-root", "_NET_ACTIVE_WINDOW"]);
      if (!out) return null;
      return { app: "x11", title: out.slice(0, 200) };
    }
  } catch {
    return null;
  }
  return null;
}
