---
schema: atelier.item
schema_version: 3
id: lor-ug9r
title: Today’s timer picker must accept mouse clicks at 1500×950
status: open
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:09:06.539328102Z
---

## Goal

The owner can start a timer by clicking a search result on Today. At 1500×950, the picker under "Something else…" extends beyond the day arc and the Planned and actual hour row intercepts clicks on its intent buttons. Keyboard Enter works, which is why the repaired `screen today` flow in lor-w2tw can pass.

Reproduced during lor-w2tw review against 5be6dafd: open `/?at=15:30`, press "Something else…", search `ship`, then click "Ship Life on Rails". Playwright times out because `<div class="relative h-4 text-[12.5px] text-dim">` in `section[aria-label="Planned and actual"]` intercepts pointer events. `document.elementFromPoint` at the button's center returns that hour row. Screenshot: `/tmp/lor-review-w2tw/picker-overlap.png`.

## Done when

- [ ] At 1500×950, opening Today's timer picker and searching for an intent lets a mouse click start the timer with that intent; no other section covers the result buttons.
- [ ] The layout also keeps the picker and the Planned and actual lanes usable at 412 px wide.
- [ ] `screen today` exercises the mouse selection and the feature map removes the Enter workaround once the fix is verified.

## Scope

Today's timer picker and day arc layout in `src/routes/index.tsx`, plus the `today` flow in `scripts/ui-check.mjs` and its verify-lor feature map. Preserve keyboard selection.
