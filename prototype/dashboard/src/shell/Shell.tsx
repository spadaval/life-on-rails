// The frame around every direction: it reads the hash, loads the direction it names, links
// that direction's stylesheets and only those, keeps the store on the scene the link names,
// and draws the Version control. It draws nothing else, so a direction owns the whole page.
import { type ReactNode, useEffect, useLayoutEffect, useState } from "react";
import { getState, isScene, resetScene, SCENES, setStill, useUndo, type SceneId } from "../data/store.js";
import { DEFAULT_VERSION, type Direction, VERSIONS } from "../directions/index.js";
import { format, setSticky, switchVersion, useRoute } from "./nav.js";
import "./shell.css";

const loaded = new Map<string, Direction>();

/** A stylesheet's path, without Vite's `?t=` cache-buster: a hot update re-links the same sheet. */
const sheetPath = (href: string) => new URL(href, location.href).pathname;

/** Link a direction's stylesheets, wait for them, then drop every other direction's. */
async function linkSheets(id: string, urls: string[]): Promise<void> {
  const head = document.head;
  const mine = [...head.querySelectorAll<HTMLLinkElement>(`link[data-direction="${id}"]`)].map((l) => sheetPath(l.href));
  const waits = urls.map((url) => {
    if (mine.includes(sheetPath(url))) return Promise.resolve();
    const el = document.createElement("link");
    el.rel = "stylesheet";
    el.href = url;
    el.dataset.direction = id;
    const done = new Promise<void>((resolve) => {
      el.onload = () => resolve();
      el.onerror = () => resolve();
    });
    head.appendChild(el);
    return done;
  });
  await Promise.all(waits);
  for (const el of head.querySelectorAll<HTMLLinkElement>("link[data-direction]")) {
    if (el.dataset.direction !== id) el.remove();
  }
  document.documentElement.dataset.version = id;
}

export function Shell() {
  const route = useRoute();
  const known = VERSIONS.find((v) => v.id === route.version);
  const [direction, setDirection] = useState<{ id: string; d: Direction } | null>(null);
  const [failed, setFailed] = useState<string | null>(null);

  // An empty or unknown version opens the shipped look, at the same path.
  useEffect(() => {
    if (!known) history.replaceState(null, "", format({ version: DEFAULT_VERSION, path: route.version ? "/" : route.path, params: route.params, sticky: route.sticky }));
    if (!known) window.dispatchEvent(new HashChangeEvent("hashchange"));
  }, [known, route]);

  // The scene and the clock follow the link.
  const scene: SceneId = isScene(route.sticky.scene) ? route.sticky.scene : "afternoon";
  const still = route.sticky.still === "1";
  useLayoutEffect(() => {
    if (getState().scene !== scene) resetScene(scene, still);
    else setStill(still);
  }, [scene, still]);

  useEffect(() => {
    if (!known) return;
    let live = true;
    const show = (d: Direction) =>
      void linkSheets(known.id, d.css).then(() => {
        if (live) setDirection({ id: known.id, d });
      });
    const have = loaded.get(known.id);
    if (have) show(have);
    else
      known
        .load()
        .then((m) => {
          loaded.set(known.id, m.default);
          show(m.default);
        })
        .catch((err: unknown) => live && setFailed(err instanceof Error ? err.message : String(err)));
    return () => {
      live = false;
    };
  }, [known]);

  if (failed) return <p className="proto-failed">This version did not load: {failed}</p>;
  const App = direction?.d.App;
  return (
    <>
      {App && direction.id === known?.id ? <App route={route} /> : null}
      {route.sticky.shell === "0" ? null : <VersionControl version={known?.id ?? DEFAULT_VERSION} scene={scene} />}
    </>
  );
}

function VersionControl({ version, scene }: { version: string; scene: SceneId }) {
  const [open, setOpen] = useState(false);
  const undo = useUndo();
  return (
    <div className="proto-shell" data-open={open || undefined}>
      {open ? (
        <div className="proto-panel" role="group" aria-label="Prototype">
          <Field label="Version">
            <select aria-label="Version" value={version} onChange={(e) => switchVersion(e.target.value)}>
              {VERSIONS.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Scene">
            <select aria-label="Scene" value={scene} onChange={(e) => setSticky({ scene: e.target.value === "afternoon" ? undefined : e.target.value })}>
              {(Object.keys(SCENES) as SceneId[]).map((s) => (
                <option key={s} value={s}>
                  {s}: {SCENES[s]}
                </option>
              ))}
            </select>
          </Field>
          <p className="proto-note">
            Made-up data, kept in this tab only. {undo ? `Last change: ${undo}.` : "Nothing changed yet."}{" "}
            <button type="button" onClick={() => resetScene(scene)}>
              Start over
            </button>
          </p>
        </div>
      ) : null}
      <button type="button" className="proto-toggle" aria-expanded={open} onClick={() => setOpen((v) => !v)} title="Switch version">
        <span className="proto-dot" />
        {VERSIONS.find((v) => v.id === version)?.name ?? version}
        <span aria-hidden="true">{open ? "▾" : "▴"}</span>
      </button>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="proto-field">
      <span>{label}</span>
      {children}
    </label>
  );
}
