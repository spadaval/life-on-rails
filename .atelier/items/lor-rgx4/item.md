---
schema: atelier.item
schema_version: 3
id: lor-rgx4
title: The agent page and the LoR panel draw from plain state in the shared module
status: draft
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-oech
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.995744340Z
---

## Goal

The two panels Sumeet called 'pretty bad overall' (lor-hybv) can be seen and changed in the lab. Their drawing takes plain state and reports taps; reading LoR and T3 stays in the app.

## Done when

- [ ] The drawing of the agent page, the thread column, the pair card, the LoR panel, LoR's four blocks and the quick-send sheet is in `homecore`'s common code as functions of a state object and a callbacks object, with no `Context`, `LorClient`, `T3Connection` or `Prefs` in them. `Pill`, `Field`, `SectionHeader` and the state lines move to the module's kit and quick send's copies are deleted.
- [ ] The app builds those state objects from what it reads today and behaves as before: on `T3_Phone_API35` and `T3_Fold_API35`'s inner screen against the test server with the demo set, each panel is screenshotted before and after and is pixel-identical (an evidence receipt).
- [ ] The lab's `current` version shows both panels, the blocks and the sheet at both screen sizes in these states: T3 unpaired, connecting, disconnected and stale; nothing waiting; one approval, a question with options, a failed turn and two LoR flags together; twelve cards; a running timer; a thread open with a long transcript; the LoR panel's Tasks and People tabs; quick send with a screenshot.
- [ ] boat: `assembleDebug testDebugUnitTest :homecore:allTests` pass, with the test counts before and after in the Report.

## Scope

`agent/`, `lor/`, `quicksend/` drawing files and their state builders; fixtures for them. No change to what the panels show or how they look.

## Context

- The audit's inventory of these screens is `inventory/launcher-panels-app.md` beside the audit page; the file list is under its section 1.
