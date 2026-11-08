---
schema: atelier.review
schema_version: 2
id: lor-69ai
title: finding on lor-v692 @c24d6479
target: lor-v692
identity: reviewer-v692-1
commit: c24d64795e044ea0e43c9e6821ed15e3a4bbe3be
created_at: 2026-11-07T23:53:45.901463541Z
event:
  event: finding
  recommendation: defer
  disposition:
    decision: defer
    identity: orchestrator
    commit: f33b560a29393515617c2446e7e9751446bc38be
    recorded_at: 2026-11-07T23:54:17.639371112Z
    follow_up: lor-zzyy
    reason: A test-step problem in lor-zzyy's plan, not this code; lor-zzyy's record now carries it.
---

medium: android/app/src/main/java/com/spadaval/lor/home/HomeScreen.kt:480 (sibling lor-zzyy's plan) lor-zzyy's Done when line 'The store's refusal reaches the screen' writes "locked": true through adb while home is open, then expects a drop on home to be refused with 'Can't: home is locked; unlock it from the home menu.' It can't be: dropHeld's change throws HOME_CHANGED when the file read differs from h.original, and a file that only gained locked differs, so LayoutStore.edit throws 'Home changed; try again' before its lock check runs; HomeStore then rereads, home is locked, and the next drag picks nothing up. lor-v692's rule is right as lor-73li states it; the sibling's check is what fails. Fix in lor-zzyy's record: show the lock's words through an app menu edit (Remove from home on an app on home, which edits the file it reads), or expect HOME_CHANGED for the drop and the lock's words on the next menu edit.
