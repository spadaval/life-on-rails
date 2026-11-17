---
schema: atelier.review
schema_version: 3
round: 1
commit: 77c85bd0508ce3b71fc00a6ca1e588ff7debf8c7
reviewers:
- thread:delegated-task:command%3Amcp%3Afc96210e-43a1-4feb-a582-f01db053fd3f%3Adelegate-task%3Aeb4f4d4e-eb92-44d0-b176-5894e16a3342
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T08:31:53.267989884Z
---

## Summary

Complexity: medium. The change adds one static middleware and one publication script, with contained listener wiring and tests.

Value: high. Serving the viewers on the existing owner address is required for the design lab.

Risk of merging it: high. The directory boundary and the promised separation from real data both have reproducible escapes.

The value warrants this size of change once the findings below are fixed.

Reviewed `git diff main...77c85bd0508ce3b71fc00a6ca1e588ff7debf8c7`, the product spec, every Done when line, the Report, listener ordering, publication sources, and all three new test files. `pnpm test` passes all 203 tests in 23 files, including the Chrome policy test. `pnpm typecheck` passes with the generated route tree already present in this checkout. The wasm MIME type, cache behavior, read-only methods, unset-directory behavior, and documentation changes are covered. The publisher tests exercise copying and replacing a fixture gallery.

I also ran independent temporary-directory reproductions of an index symlink escape and a Chrome reproduction of the popup policy escape, using synthetic data only. No production data, configuration, publication directory, or service was changed. I checked the production middleware ordering in code; I did not deploy, build the real app, or repeat the author's real-listener, phone, Tailscale Host-forwarding, and gallery visual checks. The future launcher and dashboard bundles do not exist yet, as the item allows. The artifact provenance requirement is not met by the generated index.

## Finding 1: [P1] Check containment again after adding index.html
Recommendation: fix now

In `scripts/lab/serve.mjs:91-92`, directory handling appends `index.html` after `resolveInside` has checked only the directory. `statSync` and `createReadStream` then follow that index's symlink without checking its destination. With `lab/gallery/index.html` symlinked to a synthetic `secret.txt` beside `lab`, requesting `/lab/gallery/index.html` correctly returns 404, but requesting `/lab/gallery/` returns 200 with the outside file's contents. The root index has the same problem. This contradicts the first Done when line and the Report's claim that outward symlinks are refused. Resolve and validate the final index target before stat/open, and test both root and nested directory indexes pointing outside.

## Finding 2: [P1] Close the popup escape from the lab's data boundary
Recommendation: fix now

`scripts/lab/serve.mjs:54-55` restricts connections and frames but leaves lab code able to open and control a same-origin dashboard window. In system Chrome against the real middleware and a synthetic app, a button in `/lab/index.html` called `window.dashboard = window.open('/tasks')`. After navigation, the lab read `window.dashboard.document.body.textContent`, then successfully called `window.dashboard.fetch('/_serverFn/syntheticWrite', {method:'POST', body:'{}'})`. The app recorded `GET /tasks` and `POST /_serverFn/syntheticWrite`, and both read and POST returned 200. The new window has the app's policy, so the lab's `connect-src` and `form-action` do not constrain those operations.

The Report accurately acknowledges the popup issue, but leaving it to lor-q165 does not satisfy this item's promise that a lab page cannot reach LoR's data, or the new claim in `AGENTS.md`. Enforce a boundary that covers popup/opener access as well, while keeping fixture reads and viewer execution working, and add this read-and-write case to the browser test. The existing positive fixture and negative fetch/form tests should continue to pass.

## Finding 3: [P2] Show the source revision and time for each published artifact
Recommendation: fix now

`scripts/lab/publish.sh:49-50,70-82` labels every source with the publisher checkout's current HEAD and one newest-file timestamp for the entire source directory. For `shots/`, several galleries captured at different revisions and times share that single metadata entry. Publishing an old gallery or stale ignored build from a newer clean checkout therefore gives no way to identify the revision that produced it. The third Done when line requires the commit and time of each linked artifact; the Report explicitly acknowledges that producer revisions are unavailable and substitutes publishing metadata instead.

Record and consume producer revision/time metadata, and display it for each linked gallery or bundle. Cover artifacts produced at different revisions and times so a later publish cannot replace their provenance with the publisher's HEAD. This requirement should not be marked complete while only publication metadata is available.
