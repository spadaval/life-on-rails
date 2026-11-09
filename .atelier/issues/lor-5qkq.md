---
created_at: "2026-11-09T01:35:04.880290408+00:00"
id: "lor-5qkq"
issue_type: "mission"
labels:
- "launcher"
strategy:
  path: docs/missions/launcher-use/strategy.md
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "draft"
title: "Home and drawer built around what Sumeet opens"
updated_at: "2026-11-09T01:35:04.880290408+00:00"
---

## Outcome

The launcher fits how Sumeet uses the Fold. A short swipe opens the drawer. Home has no dock and is rebuilt from his usage, with the most-used apps by the thumb and small folders instead of a 21-app Google folder. Work-profile apps go anywhere an app can. The drawer is tabs kept in `layout.json`: quick dial first, which changes through the day, then lists an agent sorts, then All. Search hides behind a button. The app menu is a chamfered popover pointing at its icon. The strategy is `docs/missions/launcher-use/strategy.md`; the mocks are in `docs/missions/launcher-use/mocks/`.

## Background

- Sumeet's launcher polish thread, 2026-11-08: the swipe is too heavy, no dock, tabs with icons managed by LoR, the category sort sucks, the search bar is too big, Recent and Frequent duplicate each other and quick dial replaces them, popovers should centre on their icon with an arrow, YouTube Music can't be added, work apps can't be added, and a home designed from usage data. He approved the mocks and the plan ("Yeah, this looks good. Do it.").
- The swipe: his Fold's logcat (`LorMotion`) shows drawer swipes of 150 to 190 dp at 460 to 700 dp/s falling back, against a commit point of 30% of 960 dp or 800 dp/s.
- YouTube Music is in the "Google" folder on home; `drop` refuses it as "it's already on home" without naming the folder (`HomeEdit.kt`).
- Work apps: `AppRef` has no profile, so `DrawerScreen.kt` refuses to drag them and `AppMenu.kt` shows no edits for them. The Fold has 14 in a Meraki managed profile.
