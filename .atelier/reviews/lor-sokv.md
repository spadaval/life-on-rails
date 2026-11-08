---
schema: atelier.review
schema_version: 2
id: lor-sokv
title: finding on lor-ncpw @1267cf20
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: 1267cf20b62f0089676f733b61cf021bae15892a
created_at: 2026-11-08T03:41:21.433233043Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: fix_now
    identity: reviewer-web-layout-plan-1
    commit: b200c72aa8a9cecf72e229914d5ca4d0fb35e571
    recorded_at: 2026-11-08T03:43:40.662150426Z
    reason: 'Fixed in b200c72: lor-pmzz''s page picks a device (the one heard from last by default, with a choice), and the strategy''s Scope now includes editing a version as JSON.'
---

lor-pmzz gaps a worker would guess at: the page lists 'the device's versions', but versions are numbered per device and the host has more than one Android device (spec's Tailscale row names pixel-9a; the mission targets a Fold); say how the page picks one (as lor phone does, the device heard from last, with a switch). 'Edit as JSON' with CodeMirror is beyond the strategy's Scope ('versions, view, revert'); add it to Scope, since lor-dkut's rejected-version check relies on it.
