---
created_at: "2026-11-07T22:59:26.560142055+00:00"
id: "lor-er0u"
evidence_type: "validation"
commit: "db26b1d9f2b84b95f44c0c2b54bb2006d3974336"
result: "pass"
captured_at: "2026-11-07T22:59:26.560048962+00:00"
path: "/tmp/claude-1000/gnk0-emu"
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
title: "CLOSED FOLDER TILE and wallpaper contrast, T3_Phone_API35, db26b1d. The tile is a bare 2x2 of mini icons, no fill or edge (01-home.png, 20-home-restored.png). Measured from the pixels (bin/contrast.py: the label's extreme pixel against the wallpaper's): plain #ffffff wallpaper, the folder's label in dark ink 19.4:1 (10-white-home.png); plain #101010 wallpaper, light ink 15.3:1 (13-dark-home.png). Set through Photos' Set wallpaper; the emulator's original wallpaper (pulled with adb root as orig-wallpaper.png) was set back the same way, so its crop may differ slightly from before. A folder with a cover app wasn't re-shot; FolderIcon draws the cover first, unchanged."
updated_at: "2026-11-07T22:59:26.566110752+00:00"
---

CLOSED FOLDER TILE and wallpaper contrast, T3_Phone_API35, db26b1d. The tile is a bare 2x2 of mini icons, no fill or edge (01-home.png, 20-home-restored.png). Measured from the pixels (bin/contrast.py: the label's extreme pixel against the wallpaper's): plain #ffffff wallpaper, the folder's label in dark ink 19.4:1 (10-white-home.png); plain #101010 wallpaper, light ink 15.3:1 (13-dark-home.png). Set through Photos' Set wallpaper; the emulator's original wallpaper (pulled with adb root as orig-wallpaper.png) was set back the same way, so its crop may differ slightly from before. A folder with a cover app wasn't re-shot; FolderIcon draws the cover first, unchanged.
