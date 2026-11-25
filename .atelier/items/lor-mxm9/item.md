---
schema: atelier.item
schema_version: 3
id: lor-mxm9
title: 'One look for every page: panels by tone, three button levels, four type sizes, styled form controls'
status: open
mission: lor-40i0
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-25T23:37:45.914953157Z
---

## Goal

Every dashboard page uses one small set of looks: panels separated by tone with the bronze frame kept for what should stand out, three levels of button, four text sizes, and form controls drawn in the theme. This is the base the page items build on.

## Done when

- [ ] `src/styles.css` names a tone for a plain panel and keeps the framed, chamfered panel as a separate class. `Panel` in `src/components/ui.tsx` draws the plain one unless asked for the frame.
- [ ] On each of Today, Tasks, Goals and a person's page at 1500 by 950, at most two panels on screen carry the bronze frame, and they are the ones that are open or live (the running timer, an open editor, a selected block). The Report lists which panels keep it and why.
- [ ] Buttons are one of three named levels in `src/styles.css`: primary (filled, at most one per panel or toolbar), secondary (outlined) and quiet (text only). Every `<button>` and button-styled link in `src/routes` and `src/components` uses one of them. Done and Did it are the primary of their row; Seen, Close, Pause and Cancel are quiet or secondary.
- [ ] Body text in `src/routes` and `src/components` uses four sizes, each with one job written in `DESIGN.md` (for example: small print, body, emphasis, page title). `grep -o 'text-\[[0-9.]*px\]'` over both directories shows only those four and the arc's display sizes; the Report gives the before and after counts.
- [ ] `<select>`, `<input type="date">`, `<input type="time">`, checkboxes and textareas are drawn in the theme on every page that has one, and no control's text is clipped at 1500px (today the habit editor's "per day" select shows "per d").
- [ ] Corners take a small radius where Sumeet asked for rounded corners (his look asks of 2026-11-17, `docs/design/lab/brief.md`), and the chamfer stays on the framed panel. `DESIGN.md` says which shape is used where.
- [ ] `DESIGN.md` describes the panel tones, the frame's job, the button levels and the type scale as built.
- [ ] `node scripts/shots.mjs` runs over `shots/dashboard.json` at 1500px and the Report says the gallery's path. No page is worse at 412px than at the base commit: the Report compares Today and Tasks at 412px before and after.
- [ ] `pnpm typecheck` and `pnpm test` pass.

## Scope

`src/styles.css`, `src/components/ui.tsx`, class names across `src/routes` and `src/components`, and `DESIGN.md`. No change to what any page shows, its order, or its copy: those belong to the page items. No new palette and no new typeface.

## Context

- Today's counts in `src/routes` and `src/components`: `text-[13px]` 47, `text-[12.5px]` 42, `text-[14px]` 33, `text-[13.5px]` 14.
- The look's rules are in `DESIGN.md`; the vendored `refactoring-ui` skill's hierarchy, type and depth areas apply.
- Kept on purpose: the arc, the amber, condensed caps for page titles and section heads, the reticle.
