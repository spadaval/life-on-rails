---
schema: atelier.item
schema_version: 3
id: lor-glev
title: Three outside critiques of every concept, and one page that compares them
status: ready
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-5uxk
- lor-5fk6
- lor-xdoi
- lor-rgx4
- lor-t8lq
implementers: []
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.172385205Z
---

## Goal

Each concept has been looked at by three models from three labs who did not build it, for range of view more than for defects, and Sumeet has one page to choose from.

## Done when

- [ ] Astra, Grok 4.6 and Kimi K3 each use every concept's dashboard and phone through the galleries and recordings, and where they can, the live viewers, with the brief. Each writes, per concept: what it gets right, what would annoy in daily use, what it makes harder than `current`, and the one idea worth stealing into another concept. Critique, not a fix list.
- [ ] Their notes are in `docs/design/lab/critiques/<concept>.md`, unmerged and unedited, each signed with its model.
- [ ] `/lab/compare/index.html`, is the comparison. Its source is `docs/design/lab/compare/`, and this item adds that folder to the copy list in `scripts/lab/publish.sh`, so every publish, including one from a clean checkout, serves it and links it first from `/lab/`. It shows: each concept's name, one sentence, four screenshots, the critics' points for and against side by side, and links into both viewers at that version.
- [ ] Defects a critic finds that break one of the brief's interactions are fixed by the concept's author before the page is called done; the rest are left as notes.
- [ ] A `needs-human` item outside the mission asks Sumeet to pick a direction or a mix. The page is done without waiting for his answer; the mission's acceptance is lor-q165.

## Scope

`docs/design/lab/critiques/`, `docs/design/lab/compare/`, and one line in `scripts/lab/publish.sh`'s copy list.

