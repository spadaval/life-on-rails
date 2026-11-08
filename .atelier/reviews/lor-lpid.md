---
schema: atelier.review
schema_version: 2
id: lor-lpid
title: approval on lor-ph30 @749419f5
target: lor-ph30
identity: reviewer-ozj2-1
commit: 749419f591b91471e49b50ac65c4c466c000fb56
created_at: 2026-11-08T00:11:25.281031725Z
event:
  event: approval
---

Re-review of lor-0fbs's fix dd1eb62 and the merge of main 0c1d07b. Fix: AgentModel now reads one LorApp.cardCalls for the process, so a page rebuilt after a move away sees the call busy and answered; this resolves lor-0fbs. The new test aPageBuiltAgainSeesTheCallUnderWayAndDoesNotSendItTwice reuses one CardCalls object, so it repeats the busy guard and can't catch the regression; the fix is evident from the one-line diff (no action). Merge (git show --cc): FolderPanel keeps lor-gnk0's full-screen glass, 96 dp FolderStrip, FolderName, AppPicker and 6 or 4 columns, and its card, apps and sources are ported to DropMark.Area(Space.Card(tile)), DropMark.Item(slot) and Source(b, slot, 1), keyed by slot.key. DragTest keeps both sides' tests. dropHeld keeps lor-v692's stripEmptyPanels(h.original.current), and FolderExit reads the resolved zones. Receipt lor-s96u covers drag-out above and below, a folder emptied, a drag into a folder and item (a) on the merged APK. Boat build of 749419f with testDebugUnitTest --rerun: assembleDebug passes, 297 tests, 0 failures, including the new CardCallsTest case and DragTest's FolderExit test. git diff --check 0c1d07b^1..HEAD is clean.
