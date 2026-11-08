---
schema: atelier.review
schema_version: 2
id: lor-ikc4
title: finding on lor-ncpw @1267cf20
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: 1267cf20b62f0089676f733b61cf021bae15892a
created_at: 2026-11-08T03:41:21.188456864Z
event:
  event: finding
  recommendation: fix_now
---

Ordering: lor-xdoi (the split, which the strategy says touches every home file and is done in one go, waiting until no branch is open on home/) is not blocked_by lor-dkut, which rewrites HomeStore, HomeActivity, HomeModel and adds LayoutSync/LayoutSyncer/AgentNotice in home/; once Panels and the spike finish, both can start together. lor-nkm8 (icons, widget snapshots, wallpaper, all home/ code) and lor-xdoi also have no order between them and both unblock in parallel. Fix: link lor-xdoi blocked_by lor-dkut, and order lor-nkm8 against lor-xdoi with one more link.
