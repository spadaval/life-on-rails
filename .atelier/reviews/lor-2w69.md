---
schema: atelier.review
schema_version: 2
id: lor-2w69
title: finding on lor-ncpw @1267cf20
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: 1267cf20b62f0089676f733b61cf021bae15892a
created_at: 2026-11-08T03:41:21.028544674Z
event:
  event: finding
  recommendation: fix_now
---

Decision lor-djn2 Context and strategy Risks: the code inventory is wrong. HomeScreen.kt uses a Context in 2 places (view.context at 441 and 1086), not 17; its real Android ties are the blur-behind window flags (1083-1090), PredictiveBackHandler (560), AgentPage (601), LaunchFrom, android.util.Log, lifecycle and java.time. 'Grid.kt...Motion.kt import only Compose and Kotlin' misses GridHost's android.util.Log (305, 367, 385, 410), GridHost's use of HomeUi defined in HomeScreen.kt:182, and Follow's import of ui.Motion from ui/Look.kt, which imports R; the eight files total 1,841 lines. Layout.kt also uses java.io.File/FileOutputStream and java.time (Instant, OffsetDateTime), which wasmJs lacks, not only org.json, yet lor-llmf's first Done when only moves parsing off org.json. Fix: correct the inventory in the decision and the Risks, and have lor-llmf's copy of Layout.kt also replace its file IO and java.time (kotlinx-datetime or plain strings).
