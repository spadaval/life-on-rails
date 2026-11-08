---
created_at: "2026-11-08T02:18:21.835325970+00:00"
id: "lor-3ztb"
evidence_type: "validation"
commit: "1e0a51ee4e11b66d4f97ccd7466a1a8547c4a5b4"
result: "pass"
captured_at: "2026-11-08T02:18:21.835286176+00:00"
path: "/tmp/claude-1000/r0f9-emu"
agent_identity: "lor-r0f9-author"
target:
  kind: "issue"
  id: "lor-r0f9"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-r0f9"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "T3_Phone_API35, one session (20:58-21:06), build of eea4aea's code on boat; no LoR server needed (widgets read nothing from it). Input by agent-device (longpress, press, gesture pan), adb for installs, file reads, logcat, pm and am. Steps and screenshots in r0f9-emu (log-lines.txt has every LorWidgets line and file read): the home menu has Add widget (c0); the picker sheet lists the main profile's providers by app with preview, label and minimum size (c1, c2); Android's bind dialog shows (c5) and its Cancel deletes the id and leaves the file alone (c5b); the configure screen's back does the same (c6); Digital (Clock app) added after the dialog and its configure screen, 387x100 dp (d1, Undo bar 'Digital is on home.'); Google Calendar's schedule (the image's agenda widget, which shows Sign in here, so no list) and Fossify Calendar's event list added; the stack overflows and scrolls above the fixed dock, the grid moves at its edges (e1-e3); widget menu Resize/Remove (f1), Remove and Undo bring back the same id (f2, f3); Markor's todo list widget (40 lines from todo.txt) stands in for a scrolling agenda: resized by the frame's handle 80 to 264 dp with Undo (g2, g3), its list scrolls under a vertical swipe and the grid moves once it is at its top (h1, h2); a horizontal swipe on the clock moves the grid, a tap opens the Clock app (h3); force-stop and install -r keep ids 3, 5, 6 with no bind dialog (i1, i2); the removed id 4 stayed bound until the next start's sweep; an id bound with LoR stopped at the configure screen was swept at the next start; a disabled app's widget and a hand-written id 999 draw the note with Remove, which takes each out of the file (k1, k2, k3); Settings opens the clock's configure screen, its cancel keeps the id (l1, l2); locked, the home menu has no Add widget and the widget menu shows only Settings (m1, m2); uiautomator names the blocks Digital and Markor and the menu Resize, Settings, Remove (n1, n2). A long press on Markor while locked opened Markor's list item, since a widget with no menu keeps its taps."
updated_at: "2026-11-08T02:18:21.852265170+00:00"
---

T3_Phone_API35, one session (20:58-21:06), build of eea4aea's code on boat; no LoR server needed (widgets read nothing from it). Input by agent-device (longpress, press, gesture pan), adb for installs, file reads, logcat, pm and am. Steps and screenshots in r0f9-emu (log-lines.txt has every LorWidgets line and file read): the home menu has Add widget (c0); the picker sheet lists the main profile's providers by app with preview, label and minimum size (c1, c2); Android's bind dialog shows (c5) and its Cancel deletes the id and leaves the file alone (c5b); the configure screen's back does the same (c6); Digital (Clock app) added after the dialog and its configure screen, 387x100 dp (d1, Undo bar 'Digital is on home.'); Google Calendar's schedule (the image's agenda widget, which shows Sign in here, so no list) and Fossify Calendar's event list added; the stack overflows and scrolls above the fixed dock, the grid moves at its edges (e1-e3); widget menu Resize/Remove (f1), Remove and Undo bring back the same id (f2, f3); Markor's todo list widget (40 lines from todo.txt) stands in for a scrolling agenda: resized by the frame's handle 80 to 264 dp with Undo (g2, g3), its list scrolls under a vertical swipe and the grid moves once it is at its top (h1, h2); a horizontal swipe on the clock moves the grid, a tap opens the Clock app (h3); force-stop and install -r keep ids 3, 5, 6 with no bind dialog (i1, i2); the removed id 4 stayed bound until the next start's sweep; an id bound with LoR stopped at the configure screen was swept at the next start; a disabled app's widget and a hand-written id 999 draw the note with Remove, which takes each out of the file (k1, k2, k3); Settings opens the clock's configure screen, its cancel keeps the id (l1, l2); locked, the home menu has no Add widget and the widget menu shows only Settings (m1, m2); uiautomator names the blocks Digital and Markor and the menu Resize, Settings, Remove (n1, n2). A long press on Markor while locked opened Markor's list item, since a widget with no menu keeps its taps.
