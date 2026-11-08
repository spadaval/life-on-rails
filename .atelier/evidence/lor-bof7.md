---
created_at: "2026-11-07T23:26:30.665728080+00:00"
id: "lor-bof7"
evidence_type: "validation"
commit: "d349d1f38d94cf596b3fdfdc0b9c0fb882dac587"
result: "pass"
captured_at: "2026-11-07T23:26:30.665694106+00:00"
path: "/tmp/claude-1000/gnk0-emu/r1-runs-final.txt"
agent_identity: "lor-gnk0-author"
target:
  kind: "issue"
  id: "lor-gnk0"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-gnk0"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "REVIEW R1 (lor-0368) on T3_Phone_API35 (emulator-5554), the fix at d349d1f. Scenario (gnk0-emu/bin/r1.sh): the version 1 layout reloaded, folder Google opened, renamed to Kit with Done, waited until layout.json read Kit, Undo tapped while open, waited until it read Google, Kit typed again. Two endings: Done (r1.sh), and no Done with a tap on the glass so the close writes it (r1-close.sh). Before the fix (db26b1d's APK, built on boat): both endings left the file at Google, twice each; the log has the first write and the undo, no third write. After the fix (one guard for the field's life, cleared by LaunchedEffect(name)): both endings wrote Kit, twice each, with exactly one write after the Undo (no double write from Done and then the close). The reviewer's remember(name) alone (b9dc661) still loses the close ending: the close runs the first composition's commit(), which reads the first guard; an earlier run of that build showed it (typed Kit, closed, file stayed Google). Output in r1-runs-final.txt. The emulator's layout.json was put back (layout-before-r1.json, compared byte for byte)."
updated_at: "2026-11-07T23:26:30.672422192+00:00"
---

REVIEW R1 (lor-0368) on T3_Phone_API35 (emulator-5554), the fix at d349d1f. Scenario (gnk0-emu/bin/r1.sh): the version 1 layout reloaded, folder Google opened, renamed to Kit with Done, waited until layout.json read Kit, Undo tapped while open, waited until it read Google, Kit typed again. Two endings: Done (r1.sh), and no Done with a tap on the glass so the close writes it (r1-close.sh). Before the fix (db26b1d's APK, built on boat): both endings left the file at Google, twice each; the log has the first write and the undo, no third write. After the fix (one guard for the field's life, cleared by LaunchedEffect(name)): both endings wrote Kit, twice each, with exactly one write after the Undo (no double write from Done and then the close). The reviewer's remember(name) alone (b9dc661) still loses the close ending: the close runs the first composition's commit(), which reads the first guard; an earlier run of that build showed it (typed Kit, closed, file stayed Google). Output in r1-runs-final.txt. The emulator's layout.json was put back (layout-before-r1.json, compared byte for byte).
