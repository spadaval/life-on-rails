---
schema: atelier.item
schema_version: 3
id: lor-ug9r
title: Today’s timer picker must accept mouse clicks at 1500×950
status: in_review
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- thread:delegated-task:command%3Amcp%3A4a2870e7-da4e-4b83-912b-01cba07c6a79%3Adelegate-task%3A0ee8586c-5f33-4ecb-8ca3-b634c56f2eba
ready: null
trivial: null
caused_by: null
created_at: 2026-11-26T00:09:06.539328102Z
---

## Goal

The owner can start a timer by clicking a search result on Today. At 1500×950, the picker under "Something else…" extends beyond the day arc and the Planned and actual hour row intercepts clicks on its intent buttons. Keyboard Enter works, which is why the repaired `screen today` flow in lor-w2tw can pass.

Reproduced during lor-w2tw review against 5be6dafd: open `/?at=15:30`, press "Something else…", search `ship`, then click "Ship Life on Rails". Playwright times out because `<div class="relative h-4 text-[12.5px] text-dim">` in `section[aria-label="Planned and actual"]` intercepts pointer events. `document.elementFromPoint` at the button's center returns that hour row. Screenshot: `/tmp/lor-review-w2tw/picker-overlap.png`.

## Done when

- [x] At 1500×950, opening Today's timer picker and searching for an intent lets a mouse click start the timer with that intent; no other section covers the result buttons.
- [x] The layout also keeps the picker and the Planned and actual lanes usable at 412 px wide.
- [x] `screen today` exercises the mouse selection and the feature map removes the Enter workaround once the fix is verified.

## Scope

Today's timer picker and day arc layout in `src/routes/index.tsx`, plus the `today` flow in `scripts/ui-check.mjs` and its verify-lor feature map. Preserve keyboard selection.

## Report

A mouse click on a search result in Today's timer picker now starts the timer at 1500×950. The cause: above phone width, `DayArc` lays its middle (`children`) over the picture with `sm:absolute`. The open picker made that middle taller than the arc, and the Planned and actual section, which comes later in the page, painted its hour row over the picker's buttons.

The fix moves the picker out of the overlaid middle. `DayArc` takes a new `below` slot, rendered under the picture in the page's flow (`src/components/DayArc.tsx`). The picker (`TimerPicker` with `id="timer-picker"` in `src/routes/index.tsx`) goes there, so opening it pushes the lanes down instead of sliding under them. The "Something else…" toggle stays in the arc's middle, with `aria-expanded` and `aria-controls="timer-picker"`. The open/closed state moved from `TimerCard` up to `Today` so both places can see it. The picker comes after the toggle in the DOM, so Tab goes from the toggle into the picker. Keyboard Enter in `IntentPicker` is unchanged.

`screen today` (`scripts/ui-check.mjs`):
- At 1500×950 it searches "ship", checks with `elementFromPoint` that nothing covers the Ship Life on Rails button and that the picker ends above the lanes, then clicks the button.
- At 412×915 it opens the picker, searches "health", and checks that neither the Health admin button nor the Dentist block in the Actual lane is covered. It then starts the timer with Enter, to prove the keyboard path, and stops it. A start and stop at the same fixed instant leaves no block.

The feature map (`references/features/today.md`) drops the Enter workaround gotcha and describes the new steps and screenshots.

Checked: `node .claude/skills/verify-lor/verify.mjs screen today /tmp/lor-verify/lor-ug9r` returned `ok: true` with no browser errors. See `today-1d-picker.png` (1500×950, picker under the arc and above the lanes) and `today-10-picker-412.png`. `pnpm test` passes (220), and so does `pnpm typecheck`. This worktree had no generated `src/routeTree.gen.ts`, so I copied the main checkout's (it is gitignored, lor-iurl).
