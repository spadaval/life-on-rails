---
schema: atelier.item
schema_version: 3
id: lor-zwfd
title: The lab is served at /lab on LoR's owner listener, so the viewers open on the PC and the phone
status: changes_requested
mission: lor-2803
labels:
- design-lab
- tool
priority: medium
blocked_by:
- lor-nde9
implementers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A4d68b643-f52b-4848-a13e-86377a0c862a
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.054446931Z
---

## Goal

Sumeet agreed on 2026-11-17 that `/lab/` on LoR's own listener is the place. He opens the dashboard prototype, the launcher lab and the screenshot galleries from his PC and from his phone, at an address that already works for him, with nothing new listening.

## Done when

- [x] With `LOR_LAB_DIR` set in the env file, lor-web serves that directory's files read-only under `/lab/` on the owner listener (`:3848`, behind the identity check) and on loopback; unset, `/lab/` is a 404. The directory is served as static files, and a test shows a path cannot escape it.
- [x] A page under `/lab/` cannot reach LoR's data, although it is on the dashboard's origin and behind the same identity check. Every `/lab/` response carries a Content-Security-Policy whose `connect-src` allows only `/lab/` on this origin and whose `form-action` is `'none'`, and no prototype or lab code calls LoR: the dashboard prototype and the lab read fixture stores only, which lor-5uxk and lor-vbfd each state and lor-q165 checks. A browser test loads a page under `/lab/` that tries `fetch('/api/v1/status')`, a server function and a form post, and all three are refused; The same check, run over each viewer's interactions, is lor-q165's.
- [x] `scripts/lab/publish.sh` copies whichever of these exist into `LOR_LAB_DIR` (default `~/.local/share/lor-lab`), each under its own folder: the latest shot galleries, the launcher lab's built bundle, the dashboard prototype's build. It writes `/lab/index.html` linking what is there with the commit and time of each, and it is the only writer of that file. What it copies is a short list of source and folder pairs at the top of the script, so a later item adds one line: lor-glev adds `docs/design/lab/compare/` to `/lab/compare/`, which the index links first when present. A publish from a clean checkout rebuilds the whole served tree from that list, and nothing in `LOR_LAB_DIR` is kept that the list does not name. It is shown here with lor-nde9's gallery; the lab and the prototype are published by their own items when they exist.
- [x] A `.wasm` file is served as `application/wasm` and a file with a hashed name is cached, shown by a test with small stand-in files.
- [x] `AGENTS.md`'s listener rule and the spec's line on what lor-web serves say `/lab/` exists and what it is.
- [x] `pnpm test` and `pnpm typecheck` pass.

## Scope

`scripts/serve.mjs` or the server's static handler, `scripts/lab/publish.sh`, `AGENTS.md`, `docs/v2-product-engineering-spec.md`.

## Context

- The lab's bundle is a development build made on this host or boat; it is not part of `pnpm build` and does not follow decision lor-kbog, which is about the dashboard's shipped page.

## Unknowns

- Whether the phone should open the lab full screen as an installed web page, so the mockup fills the real screen.

## Report

`/lab/` is live on both UI listeners once `LOR_LAB_DIR` is in the env file. Nothing was deployed and the real env file was not touched: after merge, add `LOR_LAB_DIR=/home/supadava/.local/share/lor-lab` on its own line, `pnpm build && systemctl --user restart lor-web`, then check `curl -sI http://buildbot:3847/lab/` from a tailnet device names `connect-src http://buildbot:3847/lab/` (see the open point below).

**What changed**
- `scripts/lab/serve.mjs`: an srvx middleware placed after `apiFromLoopbackOnly`/`ownerOnly` and before the app's assets in `scripts/serve.mjs`, so `:3848` checks identity first. It owns every path that is `/lab` or under `/lab/` and never calls `next()` for one: a miss or an unset dir is its own 404, any method but GET/HEAD is 405, a directory without its slash is a 308. Containment is a resolve plus `realpath` check, so `..`, encoded `..`, absolute paths, NUL and symlinks pointing out are refused. `.wasm` is `application/wasm`. A content-hashed name gets `max-age=31536000, immutable`: a stem or a `-`/`.`-separated segment of 8+ characters that is hex with a digit (`homecore.4f2a9c1e.wasm`, and Kotlin's `0d7a432110fc5479b82d.wasm` as the spike builds it) or mixes upper case, lower case and a digit (Vite's `index-B3x9aZ_k.js`). Everything else gets `no-cache` with an ETag and 304. Gallery names like `slug@1100.png` and hand-made names like `home-version2.png` and `today-concept2a.png` are tested as not hashed, because a missed hash costs only a 304 while a false one would leave a file stale for a year.
- The policy on every `/lab/` response, errors included: `connect-src` and `frame-src` limited to `<origin>/lab/`, `worker-src` to that and `blob:`, plus `object-src 'none'`, `form-action 'none'` and `base-uri 'none'`. I added `frame-src` and `object-src` beyond the Done when, because a same-origin iframe, `<object>` or `<embed>` of `/tasks` would be readable and `connect-src` does not cover them. The browser test fails without `object-src`. Scripts and styles are not restricted, because the galleries use inline script and the launcher lab is wasm.
- `scripts/lab/publish.sh`: `SOURCES` at the top is `source|folder|name` lines, in index order: `~/.local/state/lor-shots` to `shots/`, `prototype/dashboard/dist` to `dashboard/` (lor-5uxk's folder), and `android/homecore/build/dist/wasmJs/developmentExecutable` to `launcher/`. The launcher path is where spike lor-llmf's build (`/home/supadava/projects/lor-kmp-spike`) writes its `index.html`, `homecore.js` and wasm; lor-vbfd should change the line if its module lands elsewhere. lor-glev adds `docs/design/lab/compare|compare|Comparison` as the first line. The script builds a staged tree beside `LOR_LAB_DIR` and swaps it in, so nothing the list does not name survives. It refuses a relative path, `/`, `$HOME`, anything inside the checkout, and any non-empty directory without its `.lor-lab` marker. A source without its own `index.html` (the galleries, one folder per catalogue) gets each subfolder's page linked from the index.
- `AGENTS.md`'s listener rule and the spec's process line and listener list now describe `/lab/`.

**Shown**
- `tests/lab/serve.test.ts` covers serving, redirects, the 404 when unset or missing, passing on `/labs`, `/lab-x` and `/api/...`, seven escape attempts through `Request`, read-only, the policy with `Host: buildbot:3847`, wasm (a stand-in module that `WebAssembly.validate` accepts), hashed caching and 304.
- `tests/lab/browser.test.ts` runs the system Chrome against the real handler in front of a stand-in app that answers 200 and counts hits. A page under `/lab/` reads `/lab/fixture.json` (the positive control). It is refused on `fetch('/api/v1/status')`, a POST to `/_serverFn/getStatus`, an iframe of `/tasks`, an `<object>` and `<embed>` of dashboard pages, and a form post, with zero hits at the app and a `securitypolicyviolation` for each. With the policy swapped for a permissive one, or `object-src` removed, the test fails. The same file sends six raw `..` paths over a socket (fetch and curl normalise them away), and none returns the secret file beside the lab directory.
- `tests/lab/publish.test.ts` shows a stale file and folder are gone after a republish, `previous/` is kept, the index links the gallery, and an unmarked directory, `$HOME`, `/`, an in-checkout path and a relative path are each refused.
- With lor-nde9's gallery: `scripts/lab/publish.sh` published `~/.local/state/lor-shots` to `~/.local/share/lor-lab`. Through the handler in Chrome at 1300 and 412 px, the gallery opened from `/lab` and flipped to "Before" with no console errors or failed requests. I also built a copy and ran its real `scripts/serve.mjs` on ports 3877 and 3878 with `LOR_OWNER_LOGIN` and `LOR_LAB_DIR` set. Loopback served `/lab/`, and the owner listener returned 403 without the identity header and 200 with it. POST returned 405, the CSP named `http://buildbot:3847/lab/` for that Host, and `/` still reached the app.
- `pnpm test`: 203 tests pass. `pnpm typecheck` passes.

**Open points**
- The origin in the policy comes from the request's Host. If `tailscale serve` does not forward the browser's Host, the lab's own fetches (fixtures, wasm streaming) would be refused on the phone only. The post-deploy `curl` above checks this.
- The policy does not stop a lab page from calling `window.open('/tasks')` and reading that window, since popups are not a CSP fetch directive. A `sandbox` directive without `allow-popups` would close the gap, but it also blocks every `target=_blank` link in a viewer. I left it out and leave the call to lor-q165's check that no lab code calls LoR.
- "The commit and time of each": a gallery or a build output does not record the commit that made it. The index shows the newest file's time as "built" and the checkout's HEAD as "published from", marked when the checkout has uncommitted changes. An exact commit per source would need each producer (`shots.mjs`, the builds) to write one.
- Tooling: `pnpm typecheck` fails with 65 errors in a fresh worktree, because the gitignored `src/routeTree.gen.ts` is only made by Vite. I copied it from the main checkout. This is not fixed here.
