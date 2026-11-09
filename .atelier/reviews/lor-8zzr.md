---
schema: atelier.review
schema_version: 2
id: lor-8zzr
title: approval on lor-72c3 @f35cfa41
target: lor-72c3
identity: gpt-6.1-sol
commit: f35cfa419ecac40bb431132db4e00e2329ced2ee
created_at: 2026-11-09T02:54:30.079880385Z
event:
  event: approval
---

Verdict: merge. Work-profile apps gain the existing launcher edits with a small extension to app identity and no parallel edit path.
Sizes: complexity high because the persisted app-ref contract must also be kept by the future web editor; value high because it enables a required current launcher path; risk medium because it changes visible, reversible placement and profile resolution.
Contract: pass. Reviewed the full 0266f6a..f35cfa41 diff, item acceptance, launcher-use strategy, layout-v2.md and surrounding parser, edits, drawer, picker, hidden list, icons and launch code. work: round-trips in all documented ref positions; no prefix remains main; invalid prefixes name their location; widget providers reject profile prefixes. Resolution, matching, overlaps, edit identity, hiding and section moves distinguish profile twins. Launch records are unchanged.
Fitness: pass. The change extends the existing AppRef/AppId and edit ownership, with no second source of truth. Home and drawer use the correct HomeApp, existing Android badging and user-aware launching.
Proof: independently ran android/boat-build.sh /home/supadava/.t3/worktrees/life-on-rails/t3-9d628f08 /tmp/lor-72c3-sol-review-build assembleDebug testDebugUnitTest; BUILD SUCCESSFUL; read the full build.log and XML reports: 397 tests, zero failures, errors or skips. All four named new tests ran. Both git diff --check HEAD^ HEAD and git diff --check 0266f6a..HEAD excluding .atelier passed.
Read all 15 receipt records and inspected their saved screenshots, layouts and logs, plus the extra open-folder screenshot. They support menu add, badged home placement, u10 launch, a folder holding both Calendar twins, hide preserving the main twin, settings unhide and drawer drag. These are inspected author artifacts, not an independently repeated UI flow. Receipt stale warnings come from checkbox completion; git diff 2fa3ba86..f35cfa41 shows no code change or narrowed acceptance.
Findings: none; no FIX NOW.
Open questions: none.
Residual risk/tests not run: no emulator interactions were rerun and no real phone was touched. Meraki/Fold validation remains lor-37bx. Future web-editor parsing is not built or exercised here; layout-v2.md documents the shared prefix contract. New continues to skip work apps as explicitly scoped. Build outputs and reports are in /tmp/lor-72c3-sol-review-build. Code stayed read-only; only this review record is written.
