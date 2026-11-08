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
    id: "lor-xdoi"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "The phone sends LoR what home looks like: icons, wallpaper, screens, widgets"
updated_at: "2026-11-08T03:36:23.275521639+00:00"
---

## Goal

The phone sends LoR what home looks like, so the web can draw it: each launchable app's icon as home draws it (icon pack, themed icons, shape), the wallpaper (one, used on both of the Fold's screens; Sumeet, 2026-11-08: "keep things simple and use one consistent wallpaper"), each screen's size in pixels, dp and physical dpi with its insets and cutout, and each placed widget's last snapshot and its preview image, and a snapshot of the `t3` panel. Sent when they change, not on every resume.

## Done when

Provisional:
- [ ] `pnpm test:db` covers storing and replacing each kind of upload per device.
- [ ] On `T3_Fold_API35` against the test server, after a resume, each kind is in LoR and correct (icons compared to the drawer's, both screens' sizes as `adb shell wm size` and `wm density` give them), and changing the icon pack or wallpaper sends only what changed.

## Unknowns

- Reading the wallpaper needs All files access. The app already asks for it on its Sensors screen (`ui/SensorsScreen.kt`, for WhatsApp backups), and it is granted on the phone: `adb shell appops get com.spadaval.lor MANAGE_EXTERNAL_STORAGE` gave `allow` on 2026-11-08. Where it isn't granted, the upload skips the wallpaper and the web draws a plain ground; nothing waits on it.
- A widget that has never been on screen has no snapshot; whether to draw it off screen once or use its preview.
- Waits for the split (lor-xdoi), so the icons and widget snapshots come from the platform interface it defines.
