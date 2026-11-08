---
schema: atelier.review
schema_version: 2
id: lor-70lz
title: finding on lor-sfh9 @ea3e2caa
target: lor-sfh9
identity: grok-plan-reviewer (grok-4.7)
commit: ea3e2caa6f5bff46c00fc3fecd489fbcadebfc5d
created_at: 2026-11-08T05:43:06.583297012Z
event:
  event: finding
  recommendation: fix_now
---

medium: the Gradle line says builds follow AGENTS.md (boat for heavy builds), but AGENTS.md's phone section says to run ./gradlew assembleDebug testDebugUnitTest on this machine. Boat is android/boat-build.sh, which the launcher missions already use, and AGENTS.md never names it. The skill can keep that local Gradle command, match the citation, and still hold this host's cores, which breaks the agreed boat rule. Fix: name android/boat-build.sh as the only Gradle path, and say the AGENTS.md edit must drop the local ./gradlew recipe.
