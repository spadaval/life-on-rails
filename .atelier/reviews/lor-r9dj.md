---
schema: atelier.review
schema_version: 2
id: lor-r9dj
title: finding on lor-5qkq @2970a884
target: lor-5qkq
identity: gpt-6.1-sol
commit: 2970a884bbb9d4dcf0bb86b82173d7608cc7c879
created_at: 2026-11-09T01:42:58.819727255Z
event:
  event: finding
  recommendation: fix_now
---

R5 docs/missions/launcher-use/strategy.md:27 and .atelier/issues/lor-iyc3.md:33 conflict with lor-2imw's dynamic ranking: medium severity. 'An app no list names shows only in All and quick dial's New' excludes an established frequently opened app from quick dial's Now. Fresh installs have only Quick dial and All, so taken literally no established app can enter Now until somebody creates a list and assigns it; the loose end also deliberately leaves some apps to All. State that installed non-hidden apps remain eligible for Now regardless of list membership (if that is intended), and add that fixture to lor-2imw; keep the list-membership rule scoped to list tabs.
