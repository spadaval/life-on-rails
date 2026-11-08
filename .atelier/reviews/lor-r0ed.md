---
schema: atelier.review
schema_version: 2
id: lor-r0ed
title: finding on lor-kbog @289fabd1
target: lor-kbog
identity: reviewer-spike-results-1
commit: 289fabd1afb007c07078e404586279134f3795d7
created_at: 2026-11-08T06:05:30.586799013Z
event:
  event: finding
  recommendation: fix_now
  disposition:
    decision: fix_now
    identity: reviewer-spike-results-1
    commit: 75f31d871cd251858e7529c868400c99e5a5e7eb
    recorded_at: 2026-11-08T06:06:47.609572552Z
    reason: 'Fixed in 75f31d8: the boat fallback is gone (''No other build path in the deploy''), and where the deploy keeps the bundle is left open for lor-kaq8 with the artifact expiry noted.'
---

'A boat build is the fallback when CI can't' is a fallback nobody defined, against AGENTS.md's 'no fallbacks ... until their absence is felt', and lor-kaq8's Done when builds the deploy step 'as decision lor-kbog says', so a worker may script a boat path into the deploy. Drop the sentence, or say it is a manual build by hand and not part of the deploy step.
