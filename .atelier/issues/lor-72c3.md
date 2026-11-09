---
created_at: "2026-11-09T01:36:22.823663076+00:00"
id: "lor-72c3"
issue_type: "feature"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: t3/work-profile-app-refs
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-5qkq"
    type: "advances"
  - kind: "issue"
    id: "lor-83uq"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "Work-profile apps go on home, in folders and hidden: refs name a profile"
updated_at: "2026-11-09T02:15:39.152989595+00:00"
---

## Goal

Work-profile apps can go anywhere a main-profile app can: on home, in folders, hidden, and (once the drawer has tabs) in a tab's list. Today an app ref (`AppRef` in `home/Layout.kt`) is a package and an optional activity with no profile, so `DrawerScreen.kt` refuses to drag a work app and `AppMenu.kt` shows it no edits. The Fold has 14 apps in a Meraki managed profile (Webex, Outlook, AnyConnect, Duo among them), which Sumeet can only reach by search.

## Done when

- [ ] A ref may start with `work:` (`work:com.microsoft.office.outlook`, `work:com.cisco.wx2.android/com.webex.teams.WebexLauncherActivity`), naming the work profile; no prefix is the main profile, so every existing file reads as before. Parsing and writing round-trip, and any other prefix is an error naming where it is. `Layout`'s tests cover them.
- [ ] Refs match apps by profile too: `matches`, `resolve`, `names`, `placesOf`, `isHidden` and the edits take the installed apps of both profiles, so a main app and its work twin (T3 Code is installed in both on the Fold) are two apps. `HomeEdit`'s tests include the twin.
- [ ] A work app's menu offers what a main app's does (Add to home, Remove, Hide, Move to section), a drag from the drawer picks it up, home draws it with Android's badge and opens it in the work profile, and Home settings' hidden list shows and unhides it.
- [ ] On `T3_Phone_API35` with a managed profile holding an app: the app added to home from its menu, dragged into a folder, hidden and unhidden; screenshots of each, and `layout.json` read after each.
- [ ] `layout-v2.md` documents the prefix; the spec's launcher lines drop "main-profile apps" where it no longer holds.
- [ ] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/` and its tests, `layout-v2.md`, the spec. A launch record keeps its current fields.

## Context

Mission lor-5qkq. The work profile is the one `LauncherApps.getProfiles()` gives besides `Process.myUserHandle()`; a phone has at most one managed profile. A work app's install time can't be read (`HomeApp.firstInstalled`), so New still skips it.

## Unknowns

- How to make a managed profile on the API 35 emulator without a device policy app (`pm create-user --profileOf 0 --managed`, or Test DPC). The first to work is recorded on this item.
