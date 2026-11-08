---
created_at: "2026-11-08T03:36:23.169876184+00:00"
id: "lor-djn2"
issue_type: "decision"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-llmf"
    type: "related"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-08T03:36:42.016341056+00:00"
status: "done"
title: "How the web editor draws home exactly: the phone's Compose code through Kotlin Multiplatform"
updated_at: "2026-11-08T03:36:42.016341056+00:00"
---

## Context

- Read 2026-11-07: `Grid.kt`, `GridHost.kt`, `Inner.kt`, `GridEdit.kt`, `HomeEdit.kt`, `Drag.kt`, `Follow.kt` and `Motion.kt` import only Compose and Kotlin (about 1,700 lines). `Layout.kt`, `HomeLook.kt` and `Drawer.kt` parse with `org.json`, Android-only. `HomeScreen.kt` (1,612 lines) uses a `Context` in 17 places. `Icons.kt`, `IconPacks.kt`, `HomeStore.kt` and `HomeActivity.kt` stay Android-only.
- A TypeScript port makes two renderers that drift with every change to home's look; a phone-rendered image can't be edited at size.
- The spike lor-llmf tests it before anything moves. If it finds this unworkable, the choice goes back to Sumeet (strategy, Authority).

## Question

How does the web editor draw home exactly as the phone does?

## Options

- Port home's layout math (cells, icon and label sizes, padding, `InnerScreens`) and the schema to TypeScript, and check it against phone screenshots.
- Run the phone's own Compose code in the browser: a Kotlin Multiplatform module, built for Android and for `wasmJs` with Compose Multiplatform.
- Have the phone render previews and the web only show images (no editing at size).

## Decision

The second (Sumeet, 2026-11-07: "Could we build this with Kotlin MPP to make this actually accurate?", then "Yeah. Put it together."). Home's drawing, the parser and `GridEdit`'s edits move into a shared module; the Android app builds from it unchanged in behaviour; the web hosts the same composables on a canvas, fed with what the phone uploaded.

## Report

Run the phone's own Compose code in the browser through Kotlin Multiplatform (wasmJs), Sumeet 2026-11-07. Spike lor-llmf tests it before anything moves; if unworkable, it goes back to Sumeet.
