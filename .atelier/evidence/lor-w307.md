---
created_at: "2026-11-07T22:19:53.159698057+00:00"
id: "lor-w307"
evidence_type: "validation"
commit: "4cbf7484ec587dd965c0fb3b11ec569fc8fdce68"
result: "pass"
captured_at: "2026-11-07T22:19:53.159582661+00:00"
path: "/tmp/claude-1000/qdmy-perf"
agent_identity: "lor-qdmy-author"
target:
  kind: "issue"
  id: "lor-qdmy"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-qdmy"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Each cell on its own layer (placeWithLayer, 4cbf748), T3_Phone_API35 in one session, lor-3nhh's method (measure.sh's per-gesture framestats, ten opens and closes after gfxinfo reset; scripts copied to qdmy-perf/bin, arm.sh installs over the same data and loads qdmy-emu/layout-v1-two-pages.json, sha256 202062c9 read back each time). Three builds, three runs in rotated order: before = before-build (7f94a83), nolayer = 296ab86 (the lor-glg9 fix), layer = this commit. Drawer open, first frame median ms / slowest median ms / p90 of all drawer frames: run 1 (before, nolayer, layer): before 66.8/67.4/48, nolayer 16.8/33.6/40, layer 17.0/33.8/42. Run 2 (layer, nolayer, before): layer 47.2/51.0/40, nolayer 16.9/34.1/36, before 50.2/68.1/46. Run 3 (nolayer, layer, before): nolayer 58.0/66.8/48, layer 50.2/66.7/48, before 65.2/74.7/48. The emulator ran in two modes (about 62 frames per open, first frame 17 ms; or about 33 frames, first frame 47 to 67 ms), not tied to the build; the before build was in the slow mode all three times. Read like with like: layer's first frame (17.0 fast; 47.2 and 50.2 slow) was at or under before's (50.2 to 66.8) in every run, and its p90 (40 to 48) at or under before's (46 to 48). Close first frame 16.8 on all. Drop zones on this build: 4.5 Calendar held at home's right edge turned to [2,0] and dropped last there; at page 1's edge it reached the new [4,0] and the drop made it; dragged back with 4.5 s at the left edge it went [4,0] to [1,0], stayed, and [4,0] left the file; with four extra pages the drop went to the last page's end, no [6,0]; a new page then Undo put the version 1 text back byte for byte. 4.6 Voice Recorder from the drawer landed at the end of home, and F-Droid dropped onto Gmail made a folder of the two (NewPipe onto Gmail was refused: it is already on page 2)."
updated_at: "2026-11-07T22:19:53.165598900+00:00"
---

Each cell on its own layer (placeWithLayer, 4cbf748), T3_Phone_API35 in one session, lor-3nhh's method (measure.sh's per-gesture framestats, ten opens and closes after gfxinfo reset; scripts copied to qdmy-perf/bin, arm.sh installs over the same data and loads qdmy-emu/layout-v1-two-pages.json, sha256 202062c9 read back each time). Three builds, three runs in rotated order: before = before-build (7f94a83), nolayer = 296ab86 (the lor-glg9 fix), layer = this commit. Drawer open, first frame median ms / slowest median ms / p90 of all drawer frames: run 1 (before, nolayer, layer): before 66.8/67.4/48, nolayer 16.8/33.6/40, layer 17.0/33.8/42. Run 2 (layer, nolayer, before): layer 47.2/51.0/40, nolayer 16.9/34.1/36, before 50.2/68.1/46. Run 3 (nolayer, layer, before): nolayer 58.0/66.8/48, layer 50.2/66.7/48, before 65.2/74.7/48. The emulator ran in two modes (about 62 frames per open, first frame 17 ms; or about 33 frames, first frame 47 to 67 ms), not tied to the build; the before build was in the slow mode all three times. Read like with like: layer's first frame (17.0 fast; 47.2 and 50.2 slow) was at or under before's (50.2 to 66.8) in every run, and its p90 (40 to 48) at or under before's (46 to 48). Close first frame 16.8 on all. Drop zones on this build: 4.5 Calendar held at home's right edge turned to [2,0] and dropped last there; at page 1's edge it reached the new [4,0] and the drop made it; dragged back with 4.5 s at the left edge it went [4,0] to [1,0], stayed, and [4,0] left the file; with four extra pages the drop went to the last page's end, no [6,0]; a new page then Undo put the version 1 text back byte for byte. 4.6 Voice Recorder from the drawer landed at the end of home, and F-Droid dropped onto Gmail made a folder of the two (NewPipe onto Gmail was refused: it is already on page 2).
