---
created_at: "2026-11-09T01:36:23.138491107+00:00"
id: "lor-37bx"
issue_type: "loose_end"
labels:
- "launcher"
- "needs-human"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-2imw"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-72c3"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-83uq"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-bdus"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-fhk5"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-iyc3"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-09T14:09:57.862429444+00:00"
status: "done"
title: "On the Fold: the mission's launcher, a home built from use, and the first tab sort"
updated_at: "2026-11-09T14:09:57.862429444+00:00"
---

## Goal

Put the mission's launcher on Sumeet's Fold with a home built from his usage and his apps sorted into tabs, once he says to install.

## Done when

- [ ] The merged build is installed on the Fold (debug build from boat, over wireless adb), with Sumeet's go-ahead.
- [ ] Its `layout.json` is written with a backup kept: home's stack holds, in reading order, the folders Money (Chase, Robinhood, Robinhood Banking, Rocket Money), Travel (United, Uber, Tesla, Priceline), Access (STRATIS, PDK Access, Tailscale, Duo Mobile) and Work (the work profile's Webex, Outlook, AnyConnect, Meraki); then Maps, Wallet, Clock, Settings; Gmail, Messages, Phone, Grok; Claude, X, Reddit, YouTube Music; T3 Code, WhatsApp, Vivaldi, YouTube. Each is checked as installed first (by package and profile); one missing is left out and named in the result. Recheck the order against LoR's `app` observations from the Fold since 2026-11-05 before writing, swapping an app into the thumb rows only if it now has clearly more days of use.
- [ ] `drawer.tabs` holds Quick dial, Work, Talk, Media, Go, Money and All, with every launchable app sorted into one list tab or left to All, by an agent reading the phone's app list.
- [ ] A screenshot of home and of each tab from the Fold, sent to Sumeet.

## Report

Installed main 42a3c80 (debug, boat) on the Fold over adb with Sumeet's go-ahead; wrote the usage-built home (Money, Travel, Access, Work folders; Maps, Wallet, Clock, Settings; Reddit, Messages, Phone, Grok; Claude, X, YT Music, Gmail; T3 Code, WhatsApp, Vivaldi, YouTube; Gmail and Reddit swapped from the plan on the Fold's 2026-11-05..09 usage) and tabs Quick dial, Work 46, Talk 21, Media 28, Go 24, Money 24, All, every ref checked installed by profile. Backup of the old layout.json, layout.good.json and launch-stats.json in ~/.local/share/lor-evidence/lor-37bx/backup-before; screenshots there show home, quick dial with the brief card and people, and the Work tab; a 150 dp swipe opened the drawer; no crash.
