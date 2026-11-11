---
schema: atelier.item
schema_version: 3
id: lor-yt09
title: An agent sorts the phone's apps into the drawer's tabs
status: draft
mission: lor-5qkq
labels:
- launcher
- feature
priority: medium
blocked_by:
- lor-dkut
- lor-pmzz
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-09T01:36:32.995643647Z
---

## Goal

An agent sorts the phone's apps into the drawer's list tabs, so the tabs stay right as apps come and go without Sumeet sorting by hand. It reads the phone's app list from LoR (`lor phone apps`, from lor-pmzz), writes each tab's `apps` with a short reason as a new layout version (`lor phone layout set`), and never moves an app Sumeet placed by hand.

## Done when

- [ ] A tab app placed by hand on the phone is marked (or the tab is), and the sort leaves it.
- [ ] A run places each app no list names, and an app no longer installed leaves its list, as one layout version with a note; the phone shows it with Undo (lor-dkut).
- [ ] It runs nightly and when the phone's app list changes.

## Unknowns

- These Done when lines are provisional. Where the run lives (the nightly assistant run, or a step of its own), and how a hand placement is marked in the file. Settled when lor-pmzz and lor-dkut have merged.
