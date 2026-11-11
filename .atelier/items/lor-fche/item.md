---
schema: atelier.item
schema_version: 3
id: lor-fche
title: Themed icons for every app, not only those with a monochrome layer
status: draft
mission: null
labels:
- launcher
- feature
priority: low
blocked_by: []
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-07T20:51:15.726020141Z
---

## Goal

With themed icons on, every app on home is drawn in one style, not only the apps that ship a monochrome layer. Today Settings, Camera and most third-party apps keep their full-colour icon beside Google's tinted ones, so home looks mixed. Smart Launcher tints every icon from the wallpaper, and the 6.8 design critique said home with stock icons "could be any launcher". This is probably the single change that most alters how home looks.

## Done when

- [ ] Provisional: - An app with no `monochrome` layer and no icon-pack entry gets a generated themed icon in LoR's colours (Dust on Panel Face, as 6.5's themed icons), made off the main thread and kept in the icon cache. `iconSource` gains the case, with JVM tests. - On `T3_Phone_API35` with themed icons on: home, a folder and the drawer screenshotted, with no full-colour icon left among the apps shown. In the screenshots, Settings, Camera, Chrome, Files and at least four sideloaded third-party apps can each be told apart from the others; any that can't are listed on this item with the method's other failure cases. - Turning themed icons off brings back every app's own icon; the setting's line in Home settings says it now covers every app.

## Unknowns

- How to make a recognisable one-colour icon from a full-colour one: Lawnchair's and Kvaesitso's approaches (read first, name the source and licence), luminance thresholding of the foreground layer, or an icon pack fallback such as Arcticons for the apps it covers.

## Background

From the Smart Launcher comparison (`docs/research/launcher-vs-smart-launcher.md`). Launcher polish 6.5 built themed icons from the monochrome layer (`IconPacks.kt`, `Icons.kt`); its review noted that apps without one stay in colour, as Pixel's launcher does. Outside the Panels mission (lor-uj0r): it has nothing to do with the grid. The colours stay LoR's own rather than following the wallpaper as Smart Launcher's do, settled by `DESIGN.md` (no colour for decoration).
