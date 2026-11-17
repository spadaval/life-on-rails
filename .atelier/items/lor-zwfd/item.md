---
schema: atelier.item
schema_version: 3
id: lor-zwfd
title: The lab is served at /lab on LoR's owner listener, so the viewers open on the PC and the phone
status: draft
mission: lor-2803
labels:
- design-lab
- tool
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.054446931Z
---

## Goal

Sumeet agreed on 2026-11-17 that `/lab/` on LoR's own listener is the place. He opens the dashboard prototype, the launcher lab and the screenshot galleries from his PC and from his phone, at an address that already works for him, with nothing new listening.

## Done when

- [ ] With `LOR_LAB_DIR` set in the env file, lor-web serves that directory's files read-only under `/lab/` on the owner listener (`:3848`, behind the identity check) and on loopback; unset, `/lab/` is a 404. The directory is served as static files, and a test shows a path cannot escape it.
- [ ] A page under `/lab/` cannot reach LoR's data, although it is on the dashboard's origin and behind the same identity check. Every `/lab/` response carries a Content-Security-Policy whose `connect-src` allows only `/lab/` on this origin and whose `form-action` is `'none'`, and no prototype or lab code calls LoR: the dashboard prototype and the lab's `current` version read fixture stores only. A browser test loads a page under `/lab/` that tries `fetch('/api/v1/status')`, a server function and a form post, and all three are refused; a second test runs each viewer's interactions and finds no request outside `/lab/`.
- [ ] `scripts/lab/publish.sh` copies the launcher lab's built bundle, the dashboard prototype's build and the latest galleries into `LOR_LAB_DIR` (default `~/.local/share/lor-lab`), each under its own folder, and writes `/lab/index.html` linking them with the commit and time of each.
- [ ] The wasm bundle is served with the right content type and is cached by its hashed name; the lab's first load over Tailscale is measured in the Report.
- [ ] `AGENTS.md`'s listener rule and the spec's line on what lor-web serves say `/lab/` exists and what it is.
- [ ] `pnpm test` and `pnpm typecheck` pass.

## Scope

`scripts/serve.mjs` or the server's static handler, `scripts/lab/publish.sh`, `AGENTS.md`, `docs/v2-product-engineering-spec.md`.

## Context

- The lab's bundle is a development build made on this host or boat; it is not part of `pnpm build` and does not follow decision lor-kbog, which is about the dashboard's shipped page.

## Unknowns

- Whether the phone should open the lab full screen as an installed web page, so the mockup fills the real screen.
