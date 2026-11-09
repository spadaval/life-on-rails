---
created_at: "2026-11-09T03:28:52.663937148+00:00"
id: "lor-vp03"
evidence_type: "validation"
commit: "f4040d95d7b71533f17a3540543b7d6e4d0bde79"
result: "fail"
captured_at: "2026-11-09T03:28:52.663864092+00:00"
path: "/tmp/LorBdusFixReview.java"
agent_identity: "gpt-6.1-sol"
target:
  kind: "issue"
  id: "lor-bdus"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-bdus"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Independent review of f4040d95: reran R1-R3 fixtures on boat against this checkout’s freshly built Kotlin classes under /tmp/lor-build.lock using /tmp/LorBdusFixReview.java. R1 retains both refs, resolves to activities A and B, and round-trips in both directions. R2 accepts narrow-start and wide-start variants beside five pages, appends d.d to page 1, and round-trips. R3 strict read refuses d.d; readGood is null; readLastGood returns 120 original home items plus left=[d.d]; both source files remain byte-identical. Combined R2+R3 fixture: identical old files, dock-only narrow start [0,0], five full apps stacks [1,0]..[5,0], dock [d.d]. Recovery returns start Stack(blocks=[Clock]) with no Apps. Source trace, not a rendered UI observation: HomeScreen.kt:788-799 contains its sole home BrokenBanner inside the apps lambda; :828 invokes that lambda only for StackBlock.Apps, and :692 passes start=true only at the selected start cell. Thus the clock-only start cannot display the error or omission banner; other apps panels have start=false. Settings still exposes a banner. This contradicts layout-v2.md:141’s promise that the banner announces omitted dock apps. No emulator, phone or Compose rendering used; temporary fixture files removed. This receipt records the uncovered interaction failure, not the passing boat build."
updated_at: "2026-11-09T03:28:52.682934663+00:00"
---

Independent review of f4040d95: reran R1-R3 fixtures on boat against this checkout’s freshly built Kotlin classes under /tmp/lor-build.lock using /tmp/LorBdusFixReview.java. R1 retains both refs, resolves to activities A and B, and round-trips in both directions. R2 accepts narrow-start and wide-start variants beside five pages, appends d.d to page 1, and round-trips. R3 strict read refuses d.d; readGood is null; readLastGood returns 120 original home items plus left=[d.d]; both source files remain byte-identical. Combined R2+R3 fixture: identical old files, dock-only narrow start [0,0], five full apps stacks [1,0]..[5,0], dock [d.d]. Recovery returns start Stack(blocks=[Clock]) with no Apps. Source trace, not a rendered UI observation: HomeScreen.kt:788-799 contains its sole home BrokenBanner inside the apps lambda; :828 invokes that lambda only for StackBlock.Apps, and :692 passes start=true only at the selected start cell. Thus the clock-only start cannot display the error or omission banner; other apps panels have start=false. Settings still exposes a banner. This contradicts layout-v2.md:141’s promise that the banner announces omitted dock apps. No emulator, phone or Compose rendering used; temporary fixture files removed. This receipt records the uncovered interaction failure, not the passing boat build.
