---
schema: atelier.review
schema_version: 2
id: lor-40fi
title: finding on lor-iabt @5e9c7031
target: lor-iabt
identity: reviewer-iabt-1
commit: 5e9c7031353bb618c8caf9c0a3b81ebeeca5d03d
created_at: 2026-11-08T10:12:50.321356150Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: orchestrator-uj0r
    commit: 950faf1dc6224657f6f367088ca95a23ac30fbf4
    recorded_at: 2026-11-08T10:13:18.751702519Z
    follow_up: lor-y32o
    reason: 'Low impact: line 5 holds, and the cost is a needless check, not a lost or doubled reply. Filed as backlog bug lor-y32o, outside the mission.'
---

low: android/app/src/main/java/com/spadaval/lor/agent/ThreadColumn.kt:104-106 (lor-iabt) Replies.failure says 'Not confirmed: … It may still reach the thread' for every failure except 'Not connected to T3.', including the ones known not to have gone. A refusal from T3 (message.dispatch's OrchestrationV2DispatchCommandError or EnvironmentAuthorizationError, an Rpc.Exit error turned into T3CallFailed(message) at T3Connection.kt:285) means T3 answered no, and 'The connection is closing.' (T3Connection.kt:289) means the request was never written. Both tell the owner to check before resending a reply that cannot arrive. The timeout and dropped-connection wording is right, as lor-65ew showed. Fix: say 'Not confirmed' only for the two unknown outcomes (the 30 s timeout and 'The connection dropped before T3 answered.'), for example by a distinct exception type for those, and 'Not sent' otherwise, with a ThreadChoiceTest case for a refusal. Defer: line 5 asks only that the reason shows and the text stays, which holds, and the extra clause costs a needless check, not a lost or doubled reply.
