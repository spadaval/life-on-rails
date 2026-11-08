---
schema: atelier.review
schema_version: 2
id: lor-c97o
title: finding on lor-ncpw @1267cf20
target: lor-ncpw
identity: reviewer-web-layout-plan-1
commit: 1267cf20b62f0089676f733b61cf021bae15892a
created_at: 2026-11-08T03:41:21.084335091Z
event:
  event: finding
  recommendation: fix_now
---

lor-llmf and strategy Risks miss the Android Gradle Plugin: android/build.gradle.kts uses AGP 9.2.1 with its built-in Kotlin (no kotlin-android plugin; only the compose compiler plugin pins 2.2.21) on Gradle 9.4.1. AGP 9 changed how a Kotlin Multiplatform module declares its Android target (the com.android.kotlin.multiplatform.library plugin rather than androidTarget() under com.android.library), so the compatibility question is AGP 9.2.1 plus built-in Kotlin plus BOM 2026.09.00, not BOM and Kotlin alone. Fix: the Risk names AGP 9.2.1 and built-in Kotlin; lor-llmf's first Done when says 'an Android target' rather than androidTarget, and its version note records which KMP Android plugin and Compose Multiplatform release work with AGP 9.2.1.
