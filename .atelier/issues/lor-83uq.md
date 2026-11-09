---
created_at: "2026-11-09T01:36:22.767987588+00:00"
id: "lor-83uq"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-5qkq"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "A short swipe opens the drawer, and Add moves an app out of its folder"
updated_at: "2026-11-09T01:36:22.767987588+00:00"
---

## Goal

Two fixes Sumeet hit on the Fold. A swipe up of a couple of centimetres should open the drawer: today a vertical move of home's grid commits past 30% of the screen's height (about 290 dp on the Fold's cover) or above 800 dp/s, and his logcat shows swipes of 150 to 190 dp at 460 to 700 dp/s falling back. And adding an app that sits in a folder on home should move it onto home rather than refuse: YouTube Music is in his "Google" folder, and a drag from the drawer says only "it's already on home".

## Done when

- [ ] A vertical move of the grid (the drawer's included) commits once the finger has travelled 110 dp toward the other cell, or on release faster than 400 dp/s toward it after at least 24 dp of travel; a release faster than 400 dp/s the other way does not commit. Horizontal moves keep today's rule. `FollowMotion`'s tests cover each, including these releases from the Fold's log at 960 dp of height: 0.155 at 700 dp/s goes, 0.194 at 462 dp/s goes, 0.15 at -489 dp/s stays, 0.01 at 440 dp/s (under 24 dp) stays.
- [ ] On `T3_Phone_API35` (with the verify skill), a swipe up of about 130 dp at a moderate speed opens the drawer and a 60 dp slow swipe does not; each shown by a screenshot and its `LorMotion` line.
- [ ] Add to home on an app that is in a folder on the active layout moves it out of the folder onto home, and so does a drag of it from the drawer onto home. The Undo bar says where it came from ("YouTube Music is on home, out of Google."), Undo puts it back in the folder at its place, and a folder the move leaves with no apps goes as today. `HomeEdit`'s tests cover the menu path, the drag path, a folder left empty, and an app already on home itself, which still refuses.
- [ ] On `T3_Phone_API35`, with an app in a folder, Add to home from its drawer menu puts it on home and out of the folder (screenshots before and after, and the Undo bar).
- [ ] `layout-v2.md`'s rules and the spec's launcher lines say both.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/Motion.kt`, `home/Follow.kt`, `home/GridHost.kt` (the vertical rule only), `home/HomeEdit.kt`, the menu's Add to home and the drawer's drop, their tests, `layout-v2.md`, the spec.

## Context

Mission lor-5qkq; strategy `docs/missions/launcher-use/strategy.md`. `FollowMotion.settles` and `commitPoint` take shares of the travel today; the vertical rule needs the cell's height in dp, which `gridFinger` has (`size.height`, `density`).
