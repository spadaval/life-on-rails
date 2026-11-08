---
created_at: "2026-11-08T03:36:23.275521639+00:00"
id: "lor-nkm8"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-dkut"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-ncpw"
    type: "advances"
  - kind: "issue"
    id: "lor-sbth"
    type: "blocked_by"
  - kind: "issue"
    id: "lor-xdoi"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The phone sends LoR what home looks like: icons, wallpaper, screens, font, widgets"
updated_at: "2026-11-08T03:36:23.275521639+00:00"
---

## Goal

The phone sends LoR what home looks like, so the web can draw it: each launchable app's icon as home draws it (icon pack, themed icons, shape), the wallpaper (both of the Fold's screens if they differ), each screen's size in pixels, dp and physical dpi with its insets and cutout, and each placed widget's last snapshot and its preview image. Sent when they change, not on every resume.

## Done when

Provisional:
- [ ] `pnpm test:db` covers storing and replacing each kind of upload per device.
- [ ] On `T3_Fold_API35` against the test server, after a resume, each kind is in LoR and correct (icons compared to the drawer's, both screens' sizes as `adb shell wm size` and `wm density` give them), and changing the icon pack or wallpaper sends only what changed.

## Unknowns

- Whether `WallpaperManager` gives the cover and inner wallpapers apart on One UI. (Whether All files access is granted on the phone is asked of Sumeet at the plan's walk-through and recorded here.)
- A widget that has never been on screen has no snapshot; whether to draw it off screen once or use its preview.
- Waits for the split (lor-xdoi), so the icons and widget snapshots come from the platform services it defines.
