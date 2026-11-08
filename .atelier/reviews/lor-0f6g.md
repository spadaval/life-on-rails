---
schema: atelier.review
schema_version: 2
id: lor-0f6g
title: finding on lor-sghw @289fabd1
target: lor-sghw
identity: reviewer-spike-results-1
commit: 289fabd1afb007c07078e404586279134f3795d7
created_at: 2026-11-08T06:05:30.515961599Z
event:
  event: finding
  recommendation: fix_now
  disposition:
    decision: fix_now
    identity: reviewer-spike-results-1
    commit: 75f31d871cd251858e7529c868400c99e5a5e7eb
    recorded_at: 2026-11-08T06:06:47.534191506Z
    reason: 'Fixed in 75f31d8: the check is now width over that frame''s scale within 1 px of the width at rest, on frames slowed tenfold and captured by screencap loop or high-bit-rate screenrecord, with widths per frame recorded before and after.'
---

Done when 3 cannot be met as written: 'no glyph changes width between frames' is false for correct scaling, since a label drawn at scale 0.95 is 5% narrower than at 1.0. State what shimmer is instead, for example: a word's width divided by that frame's scale stays within 1 px across the motion (before the change it jumps). Also say how to capture frames that show it: adb screenrecord's H.264 at the default bit rate can smear single-pixel jumps, so slow the motion (settings put global animator_duration_scale 10) and raise --bit-rate, or take screencap sequences.
