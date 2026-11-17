---
schema: atelier.item
schema_version: 3
id: lor-glev
title: Three outside critiques of every concept, and one page that compares them
status: draft
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-5uxk
- lor-5fk6
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.172385205Z
---

## Goal

Each concept has been looked at by three models from three labs who did not build it, for range of view more than for defects, and Sumeet has one page to choose from.

## Done when

- [ ] Astra, Grok 4.6 and the third critique seat each use every concept's dashboard and phone through the galleries and recordings, and where they can, the live viewers, with the brief. Each writes, per concept: what it gets right, what would annoy in daily use, what it makes harder than `current`, and the one idea worth stealing into another concept. Critique, not a fix list.
- [ ] Their notes are in `docs/design/lab/critiques/<concept>.md`, unmerged and unedited, each signed with its model.
- [ ] `/lab/index.html` leads with a comparison: each concept's name, one sentence, four screenshots, the critics' points for and against side by side, and links into both viewers at that version.
- [ ] Defects a critic finds that break one of the eight interactions are fixed by the concept's author before the page is called done; the rest are left as notes.
- [ ] A `needs-human` item outside the mission asks Sumeet to pick a direction or a mix. The mission is finished when the page is up, without waiting for his answer.

## Scope

`docs/design/lab/critiques/`, the lab's index page.

## Context

- The third seat is the OpenCode model Sumeet called 'Space Bunny Free' on 2026-11-17. No model of that name was in `opencode models` or T3's catalogue that day, so the seat is named when he says which model he meant.
