---
schema: atelier.item
schema_version: 3
id: lor-jkcf
title: 'shots.mjs captures cleanly: no repeated-band images and no first-link timeout'
status: ready
mission: null
labels:
- tools
priority: medium
blocked_by: []
implementers: []
ready:
  by: lead of lor-2803, found during the mission
  at: 2026-11-17T19:31:02.722778078Z
trivial: null
caused_by: null
created_at: 2026-11-17T19:31:02.681789073Z
---

## Goal

`scripts/shots.mjs` gives a clean image for every shot in one run. Today it draws six pages at once (`PARALLEL = 6`), and on this machine some full-page captures come out with the top of the page repeated down the image, and one or two lab shots time out, a different one each run. Implementers of lor-vx69, lor-oech and lor-rgx4 each retook shots by hand on 2026-11-17.

## Done when

- [ ] The cause of the repeated band is found and named in the Report (the suspects are six pages at once under the 4 GB session cap, and a full-page capture taken while the page is still laying out).
- [ ] Three runs in a row of `shots/prototype.json` and `shots/lab.json` report no problems and no image has a repeated band, checked by a script.
- [ ] A shot that fails is retaken once before it is reported, and the number of pages at once can be set from the command line.

## Scope

`scripts/shots.mjs`.

## Context

- Found during mission lor-2803 on 2026-11-17 and filed by its lead.
