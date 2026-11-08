---
schema: atelier.review
schema_version: 2
id: lor-0fbs
title: finding on lor-ph30 @20f95287
target: lor-ph30
identity: reviewer-ozj2-1
commit: 20f9528791f0180980bef42432103d2684203b8d
created_at: 2026-11-08T00:05:06.670212529Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: fix_now
    identity: orchestrator
    commit: 839f8b7042609670f7dbf871edef5ee02388c9cd
    recorded_at: 2026-11-08T00:05:35.403159626Z
    reason: A double send of a card's command is a real cost, and holding one CardCalls for the process is small; fix it with the merge from main.
---

Minor (lor-ozj2): android/app/src/main/java/com/spadaval/lor/agent/AgentModel.kt:55 keeps CardCalls inside AgentModel, which AgentPage.kt:101 remembers in the page's composition. A move two cells away (the composition window) disposes the page, so coming back makes a new AgentModel with an empty busy and answered while the old call may still be in flight (up to T3Connection's 30 s CALL_TIMEOUT_MS). The card shows enabled again and a second tap sends a second command, the double act busy exists to stop. The call itself does complete: LiveUpdates holds the T3Connection for the process, so the page's dispose() doesn't halt the socket, and the late result writes into the orphaned CardCalls, which is then collected. lor-ozj2's F4 decision note and the Report's open item say only a process death or a locale change shows the gap; a move away and back shows it too. Fix: hold one CardCalls for the process (beside LorApp.scope) so busy and answered outlive the page, or correct the note. Defer: an improvement on before (the call was cancelled), the window is short, and the server answers a request once.
