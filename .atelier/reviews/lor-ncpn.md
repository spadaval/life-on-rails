---
schema: atelier.review
schema_version: 2
id: lor-ncpn
title: finding on lor-clgw @4c535cb2
target: lor-clgw
identity: reviewer-clgw-final-1
commit: 4c535cb23938e6f11f48197f0b068de03a5315da
created_at: 2026-11-08T15:05:42.600128552Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: orchestrator-uj0r
    commit: 8674755f6093adb94b7ac57aa15b9ad055a74e67
    recorded_at: 2026-11-08T15:06:28.960817282Z
    follow_up: lor-85ct
    reason: 'Low impact: the column recovers by itself on reconnect and nothing is lost. Filed as backlog bug lor-85ct, outside the mission.'
---

low: android/app/src/main/java/com/spadaval/lor/agent/ThreadColumn.kt:265 (lor-iabt, 6f946a8) With 6f946a8 the column makes no read while T3 is not Live, but it still says 'Reading the thread…' for as long as the outage lasts when it has no messages yet (receipt lor-izgg 06: unfolded during a drop, 'Reading the thread…', no T3Thread read, and no disconnected line in the left column while a retry is under way). During a long outage this reads as a hung read. Fix: when !live and nothing has been read for the thread, show the wording the column already has for no thread, 'The chosen thread shows here once T3 is connected.' Defer: the column recovers by itself on reconnect (lor-izgg 07), and nothing wrong is written or lost.
