---
schema: atelier.review
schema_version: 2
id: lor-kjzc
title: approval on lor-sbth @d2830527
target: lor-sbth
identity: reviewer-leh4-1
commit: d2830527f3d96819cf61eb330acfd73f02f94961
created_at: 2026-11-08T03:17:10.877849911Z
event:
  event: approval
---

Review of lor-leh4's slice (3229a0e..d283052, first parent, skipping the main merge e9d358a): 44252ef and the page-dot fix 8d301b6. Each Done when line checked against the code: DeviceHome.parse reads weather with optJSONObject (object, null, missing field), tested; alarmLine and weatherLine are pure with JVM tests for 24 h inclusive, past, none, both units, rounding and the 2 h edge; the lines are drawn under clock and date styles in the event line's type and ink, not under none, with TalkBack text and an 'Open alarms' action; the clock and AgentModel both hold LorApp.reads (LorReads) via repeatOnLifecycle(STARTED), so reads stop when home stops. LorReads lifecycle: holders counted on the main thread only, matched by each hold's finally; the loop is cancelled at zero and a new one launched at one, so it cannot leak or stop while held (one minor in-flight handoff case recorded as lor-5i88, no action). Agent page: what shows, Retry and errors read the same fields as before; the one change is the Done when's own, at most one read a minute, so a return to home within a minute shows the kept reply or error without reading at once. 8d301b6: FillColumn adds endPad only when its children's sum exceeds the column's minimum, and a fitting stack's fill child makes the sum equal the minimum, so a stack that fits measures as before (receipt lor-moup and 20-font1-fits.png agree). The spec now lists weather on GET /device/home (lor-0ri5). lor-lz97 can extend LorReads: hold() and the schedule are generic; it will need per-endpoint state beside home/error/current. Proof: android/boat-build.sh ... assembleDebug testDebugUnitTest --rerun at d283052, BUILD SUCCESSFUL, 44 suites, 323 tests, 0 failures, LorReadsTest, HeaderTest and DeviceHomeTest run fresh; git diff --check clean. Emulator not driven; judged from receipts lor-pnlz, lor-ap2z, lor-2p20, lor-ipzv, lor-8qz3, lor-yia7, lor-moup and their screenshots.
