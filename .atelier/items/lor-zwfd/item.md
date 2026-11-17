---
schema: atelier.item
schema_version: 3
id: lor-zwfd
title: The lab is served at /lab on LoR's owner listener, so the viewers open on the PC and the phone
status: building
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

- [ ] With `LOR_LAB_DIR` set in the env file, lor-web serves that directory's files read-only under `/lab/` on the owner listener (`:3848`, behind the identity check) and on loopback; unset, `/lab/` is a 404. The directory is served as static files, and a test shows a path cannot escape it.
- [ ] A page under `/lab/` cannot reach LoR's data, although it is on the dashboard's origin and behind the same identity check. Every `/lab/` response carries a Content-Security-Policy whose `connect-src` allows only `/lab/` on this origin and whose `form-action` is `'none'`, and no prototype or lab code calls LoR: the dashboard prototype and the lab read fixture stores only, which lor-5uxk and lor-vbfd each state and lor-q165 checks. A browser test loads a page under `/lab/` that tries `fetch('/api/v1/status')`, a server function and a form post, and all three are refused; The same check, run over each viewer's interactions, is lor-q165's.
- [ ] `scripts/lab/publish.sh` copies whichever of these exist into `LOR_LAB_DIR` (default `~/.local/share/lor-lab`), each under its own folder: the latest shot galleries, the launcher lab's built bundle, the dashboard prototype's build. It writes `/lab/index.html` linking what is there with the commit and time of each, and it is the only writer of that file. What it copies is a short list of source and folder pairs at the top of the script, so a later item adds one line: lor-glev adds `docs/design/lab/compare/` to `/lab/compare/`, which the index links first when present. A publish from a clean checkout rebuilds the whole served tree from that list, and nothing in `LOR_LAB_DIR` is kept that the list does not name. It is shown here with lor-nde9's gallery; the lab and the prototype are published by their own items when they exist.
- [ ] A `.wasm` file is served as `application/wasm` and a file with a hashed name is cached, shown by a test with small stand-in files.
- [ ] `AGENTS.md`'s listener rule and the spec's line on what lor-web serves say `/lab/` exists and what it is.
- [ ] `pnpm test` and `pnpm typecheck` pass.

## Scope

`scripts/serve.mjs` or the server's static handler, `scripts/lab/publish.sh`, `AGENTS.md`, `docs/v2-product-engineering-spec.md`.

## Context

- The lab's bundle is a development build made on this host or boat; it is not part of `pnpm build` and does not follow decision lor-kbog, which is about the dashboard's shipped page.

## Unknowns

- Whether the phone should open the lab full screen as an installed web page, so the mockup fills the real screen.
