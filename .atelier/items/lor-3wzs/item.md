---
schema: atelier.item
schema_version: 3
id: lor-3wzs
title: 'The Panes direction on the dashboard prototype: every required screen, state and interaction'
status: building
mission: lor-2803
labels:
- dashboard
- design
priority: medium
blocked_by:
- lor-5uxk
implementers:
- thread:delegated-task:command%3Amcp%3A275230a9-0f8b-446e-b3b1-4303d9895adf%3Adelegate-task%3A4bad2ea8-13a4-4e49-83dd-839a64f6850a
ready:
  by: lead of lor-2803, after the brief (lor-npa6)
  at: 2026-11-17T15:10:01.385344569Z
trivial: null
caused_by: null
created_at: 2026-11-17T15:09:34.810760408Z
---

## Goal

Sumeet can use the Panes direction's dashboard in the prototype: pick it in the Version dropdown and do the brief's ten dashboard interactions by hand, at desktop and at phone width, on the made-up data.

## Done when

- [ ] `prototype/dashboard/` has a `panes` direction, in a folder of its own with its own tokens, components and screens, built as `docs/design/lab/brief.md` describes it under "### 3. Panes" and with the brief's "Common ground for every concept" (the one search field that also adds and asks, Sumeet's four look asks, standard controls).
- [ ] It draws the brief's dashboard screens (Today in the afternoon and in the morning, Tasks with a task open, Goals with one goal at "no data yet", People with the list and one person) at 1440, 1100 and 412 px.
- [ ] It draws the brief's required states that reach the dashboard (nothing wants you, an empty day at 09:00, LoR's last data shown as stale, a crowded queue), each opened by a link, and the Report says where each appears.
- [ ] The brief's ten dashboard interactions work by hand at 1440 and at 412 px, Undo is offered only where the brief allows it, and the Report lists each interaction with how it is done in this direction.
- [ ] `shots/prototype.json` gains this direction's screens and states, `pnpm shots` captures them, and `scripts/lab/publish.sh` puts the build and the gallery under `/lab/`.
- [ ] The direction's "Its risk" from the brief is answered in the Report after using it: whether it showed up, with a screenshot.

## Scope

`prototype/dashboard/` (this direction's folder and its registration) and `shots/prototype.json`. Not the shell, not another direction's folder, not `src/`.

## Context

- Use the `refactoring-ui` skill (`~/.agents/skills/refactoring-ui/`) and the vendored impeccable skill's Operate guidance. Sumeet's aim, 2026-11-17: the app, launcher and dashboard should "feel designed and polished", "make common things easy and uncommon things accessible" and be "a delight to use"; the Elite theme is kept "only in spirit".
- Concept code is rough on purpose. The look is not: this is what Sumeet chooses from, so finish the visual design; the brief's frames in `docs/design/lab/directions/` show organisation only, and their faces are placeholders.
- The other three directions are built at the same time in their own folders. Expect to merge `shots/prototype.json` with theirs.
- T3 approvals and questions stay on the phone; no concept puts them on the dashboard.
