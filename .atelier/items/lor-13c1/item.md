---
schema: atelier.item
schema_version: 3
id: lor-13c1
title: 'Real Fold: widgets and the clock block on One UI'
status: ready
mission: null
labels:
- launcher
- needs-human
- validation
priority: medium
blocked_by: []
implementers: []
ready:
  by: conversion
  at: 2026-11-07T22:18:57.220059145Z
trivial: null
caused_by: null
created_at: 2026-11-07T22:18:57.220059145Z
---

## Goal

Check on the real Fold that widgets and the clock block (lor-sbth) work on One UI: the Tesla widget added to home from the home menu, through Android's bind consent ("Allow LoR to create widgets and access their data?", Sumeet's tap) and the Tesla app's own configure screen if it has one; the widget resized and removed with Undo; widgets kept across a fold, an unfold, a phone restart and an update of LoR's app; taps and a list widget's scrolling working beside the grid's swipes; and the clock block's next alarm from Samsung's Clock app and the weather for where he is.

## Done when

- [ ] Each check has a result in an evidence receipt on this item, from Sumeet's use or a session he allows; a failure files a bug linked here. One result says whether One UI's Clock app reports its alarms through `AlarmManager.getNextAlarmClock` (if not, the alarm line stays empty on his phone).

## Context

Reason it is after merge: a device no agent can reach, installing on Sumeet's phone is his call, and the bind consent is a permission only he grants. The stand-in, run before merge, is lor-sbth's checks on `T3_Phone_API35` and `T3_Fold_API35` with the Clock app's and a list widget.
