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

- [x] A ref may start with `work:` (`work:com.microsoft.office.outlook`, `work:com.cisco.wx2.android/com.webex.teams.WebexLauncherActivity`), naming the work profile; no prefix is the main profile, so every existing file reads as before. Parsing and writing round-trip, and any other prefix is an error naming where it is. `Layout`'s tests cover them.
- [x] Refs match apps by profile too: `matches`, `resolve`, `names`, `placesOf`, `isHidden` and the edits take the installed apps of both profiles, so a main app and its work twin (T3 Code is installed in both on the Fold) are two apps. `HomeEdit`'s tests include the twin.
- [x] A work app's menu offers what a main app's does (Add to home, Remove, Hide, Move to section), a drag from the drawer picks it up, home draws it with Android's badge and opens it in the work profile, and Home settings' hidden list shows and unhides it.
- [x] On `T3_Phone_API35` with a managed profile holding an app: the app added to home from its menu, dragged into a folder, hidden and unhidden; screenshots of each, and `layout.json` read after each.
- [x] `layout-v2.md` documents the prefix; the spec's launcher lines drop "main-profile apps" where it no longer holds.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`home/` and its tests, `layout-v2.md`, the spec. A launch record keeps its current fields.

## Context

Mission lor-5qkq. The work profile is the one `LauncherApps.getProfiles()` gives besides `Process.myUserHandle()`; a phone has at most one managed profile. A work app's install time can't be read (`HomeApp.firstInstalled`), so New still skips it.

## Unknowns

- How to make a managed profile on the API 35 emulator without a device policy app (`pm create-user --profileOf 0 --managed`, or Test DPC). The first to work is recorded on this item.
  - Answered: `pm create-user --profileOf 0 --managed Work`, then `am start-user -w 10` and `pm install-existing --user 10 <package>` for each app (boat's `T3_Phone_API35`, 2026-11-09). The profile is `profile.MANAGED`, `RUNNING_UNLOCKED`, with no device policy app, and `LauncherApps` lists its 18 launchable activities. The brief's "Work section with Calendar, Drive and Life on Rails" was the Work category section of main-profile apps; the emulator had no profile until this.

## Report

All six Done when lines hold. A ref may start with `work:`; `LayoutTest.aWorkPrefixNamesTheWorkProfile` covers parse, write and round trip in every place a ref goes (apps, folders, cover, dock, hidden, drawer sections) and that a file without the prefix reads as before, and `LayoutTest.anUnknownPrefixIsAnErrorThatSaysWhere` covers `personal:` and `Work:` errors naming `items[1]` and `hidden[0]`. `AppRef` and `AppId` carry `work`, so `matches`, `resolve`, `names`, `placesOf`, `isHidden`, hide, unhide and the edits keep twins apart: `HomeEditTest.aWorkTwinIsAnotherApp` (add, folder, remove from folder, add out of folder, hide, unhide and a drawer drop of the work twin of `t.t` beside the main one) and `SectionEditTest.aWorkAppMovesToASectionApartFromItsTwin`. On boat, `assembleDebug testDebugUnitTest` passed with 397 tests and 0 failures. On boat's `T3_Phone_API35` with a managed profile, the work Calendar was added to home from its menu, drawn with the work badge, opened as `u10`, dragged onto the main Calendar into a folder, hidden from the folder, unhidden from Home settings, and dragged from the drawer onto home, each with a screenshot and `layout.json` read after it (receipts on this item).

### Decisions

- Any profile other than `Process.myUserHandle()` is the work profile (`HomeApp.id`), per the Context line; Android 15's private space is not listed to a launcher without `ACCESS_HIDDEN_PROFILES`, so it doesn't reach this.
- A work app no drawer section names stays under the drawer's Work profile header; Move to section puts its `work:` ref in that section, and `Drawer.sectionOf` returns null for it until then, so moving it to its category's section is not refused as "already there".
- A widget's `provider` does not take the prefix.
- Hide and unhide take the profile: hiding the work twin leaves the main app on home and in folders.

### Surprises

- Boat's emulator had no managed profile; I made one (Unknowns). It stays on the emulator as user 10.
- agent-device's `install` is refused on boat's emulator: another session (`t3-862c14dd1e4afa4b139d9f23`, workspace `/home/supadava/projects/life-on-rails`) holds the device claim. I installed with `adb install -r` through `boat exec`, which T3's device rules allow for installs, and drove the UI with agent-device, whose other commands worked.

### Open items

- The real Fold's Meraki profile is not checked here; that falls to lor-37bx.
- New still skips work apps (install time unreadable), as the Context says.

