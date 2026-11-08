---
schema: atelier.review
schema_version: 2
id: lor-wueo
title: finding on lor-sbth @748a6909
target: lor-sbth
identity: reviewer-r0f9-1
commit: 748a690910c3b8808bc20f8851ac69694380a4fe
created_at: 2026-11-08T02:23:24.273232789Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: fix_now
    identity: orchestrator
    commit: 2f4bc418775b35030affb1d4abc8041fd3539508
    recorded_at: 2026-11-08T02:23:54.663954593Z
    reason: Small, and the author has the context; cheaper than a follow-up.
---

low: android/app/src/main/java/com/spadaval/lor/home/HomeActivity.kt:170-177 The widget being added (Adding) lives only in the HomeActivity instance, whose comment says the activity handles every configuration change itself. It does not: the manifest's configChanges (AndroidManifest.xml:86) leaves out locale, layoutDirection, touchscreen, colorMode and fontWeightAdjustment, and stateNotNeeded means a process restart behind the configure screen gives a fresh activity too. In those cases the bind dialog's or configure screen's result reaches an activity with adding == null and is dropped: the widget Sumeet just configured never appears, and its bound id stays held (the provider keeps updating an invisible widget) until the next process start's sweep. No layout data is lost. Smallest fix: hold the in-flight Adding in process scope (HomeStore, beside the host) so a recreated activity finds it, and correct the comment.
