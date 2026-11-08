---
schema: atelier.review
schema_version: 2
id: lor-kmac
title: finding on lor-ncpw @289fabd1
target: lor-ncpw
identity: reviewer-spike-results-1
commit: 289fabd1afb007c07078e404586279134f3795d7
created_at: 2026-11-08T06:05:30.621730533Z
event:
  event: finding
  recommendation: fix_now
  disposition:
    decision: fix_now
    identity: reviewer-spike-results-1
    commit: 75f31d871cd251858e7529c868400c99e5a5e7eb
    recorded_at: 2026-11-08T06:06:47.645373663Z
    reason: 'Fixed in 75f31d8: Environment now says Android Gradle builds on boat and the browser bundle in GitHub Actions (decision lor-kbog).'
---

The strategy contradicts itself on where the wasm build runs. Scope's Environment line still says 'Gradle builds, Android and wasm, on boat', while the new Shape bullet and decision lor-kbog put the browser bundle in GitHub Actions. Fix the Environment line: Android builds on boat, the browser bundle in GitHub Actions (with boat for the spike-style builds during the split, if that is meant).
