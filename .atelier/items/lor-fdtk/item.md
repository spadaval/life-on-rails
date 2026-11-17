---
schema: atelier.item
schema_version: 3
id: lor-fdtk
title: 'The Panes direction on the phone in the lab: every required screen, state and interaction'
status: ready
mission: lor-2803
labels:
- launcher
- design
priority: medium
blocked_by:
- lor-j1fe
implementers: []
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.478110201Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.885709623Z
---

## Goal

Sumeet can use the Panes direction's phone in the lab: pick it in the Version dropdown and do the brief's eleven phone interactions by touch, on the cover screen and unfolded.

## Done when

- [ ] The lab has a `panes` version, drawn in Compose in `homecore`'s `wasmJsMain` from the same fixtures as `current`, built as `docs/design/lab/brief.md` describes it under "### 3. Panes" and with the brief's "Common ground for every concept", using the shared pieces lor-5fk6 made (the navigation shell, the sheet, the fixture-backed store).
- [ ] It draws the brief's phone screens: home, the drawer and search with results and with the keyboard up, the agent page and the LoR panel on the cover and the inner screen with both kinds of question, one menu and one sheet, a settings page, the timer, and a thread on the inner screen read and being replied to.
- [ ] It draws the brief's required states (nothing wants you, an empty day, a failed phone sync seen from home or the agent page, T3 disconnected, LoR unreachable with stale data, twelve cards waiting, font scale 1.3), each a named state opened by a link, and the Report says where each appears.
- [ ] The brief's eleven phone interactions work by touch and by `scripts/lab/gesture.mjs`, with recordings of the drawer swipe and one drag; fold and unfold keep the open thread, a half-written answer and the scroll position.
- [ ] `shots/lab.json` gains this version's states on a dark, a pale and a mixed wallpaper, and `scripts/lab/publish.sh` puts the build under `/lab/phone/`.
- [ ] The direction's "Its risk" from the brief is answered in the Report after using it: whether it showed up, with a screenshot.

## Scope

`android/homecore/src/wasmJsMain/.../concepts/` (this direction's package and its registration) and `shots/lab.json`. The app's own screens are not changed.

## Context

- Use the `refactoring-ui` skill (`~/.agents/skills/refactoring-ui/`) and the vendored impeccable skill's Operate guidance. Sumeet's aim, 2026-11-17: the app, launcher and dashboard should "feel designed and polished", "make common things easy and uncommon things accessible" and be "a delight to use"; the Elite theme is kept "only in spirit".
- Concept code is rough on purpose. The look is not: this is what Sumeet chooses from, so finish the visual design.
- lor-5fk6 builds Queue, the first direction, and the shared pieces. The wasm build here is one at a time (decision lor-0v68), so the lead runs the phone directions one after another.
- Decisions every concept follows are in the brief under "Decisions already made".
