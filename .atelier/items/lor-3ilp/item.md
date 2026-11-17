---
schema: atelier.item
schema_version: 3
id: lor-3ilp
title: 'lor-ncpw''s plan is brought up to date: the split before the sync, both fonts, and checks that prove same-pixels'
status: draft
mission: null
labels: []
priority: medium
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T06:21:28.827438780Z
---

## Goal

Mission lor-ncpw's plan matches the order its items now run in and its checks can prove what they claim. Two plan reviewers read it on 2026-11-17, when mission lor-2803 moved the shared module (lor-xdoi) ahead of the phone's sync (lor-dkut), and each asked for changes.

## Done when

- [ ] The mission's Outcome and `design.md` say the split comes before the sync, and what an agent can do to home after lor-pmzz alone.
- [ ] The Shape names both fonts the phone draws with, Saira and Saira Condensed.
- [ ] lor-nkm8's Done when can support same-pixels: physical dpi rather than `wm density`, and the wallpaper's ink, the insets and an emulator wallpaper among the uploads.
- [ ] lor-nnn5 names the test layout, both Fold screens, the 2 px limit and the frame-only exceptions.
- [ ] lor-dkut says schema version 2 where it means the layout's schema, and the folder move it describes is on lor-u0h7 or lor-nnn5.
- [ ] The plan is reviewed as a mission plan and merged.

## Scope

`.atelier/missions/lor-ncpw/` and its items' text. No code.

## Context

- The two reviews are in this repository's history: commits `94fdf20` (GPT-6 Astra) and `3e1ff35` (Grok 4.6) on branch `lor-2803`, files `.atelier/missions/lor-ncpw/reviews/1-3yca.md` and `1-c7c4.md`. They were withdrawn from that branch with the edit that prompted them, so that the design lab's plan did not wait on lor-ncpw's.
- Astra's one finding, that lor-xdoi left paging gestures and app menus in the Android app while requiring them in the browser, is fixed in lor-xdoi itself.
