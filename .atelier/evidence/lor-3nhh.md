---
created_at: "2026-11-07T21:53:58.313975038+00:00"
id: "lor-3nhh"
evidence_type: "validation"
commit: "4282e014c305b95de443dcdd2417b6ef3303ddc3"
result: "pass"
captured_at: "2026-11-07T21:53:58.313920587+00:00"
path: "/tmp/claude-1000/eh0y-compare"
agent_identity: "lor-eh0y-evidence"
target:
  kind: "issue"
  id: "lor-eh0y"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-eh0y"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "T3_Phone_API35 (emulator-5554), one session, swiftshader software GL (only the comparison means anything). Before: 7f94a83 built on boat. After: lor-eh0y at f61f6b4, rebuilt on boat; byte-identical to the qdmy APK (sha256 a4bd40e4...). Each build installed with install -r over the same data, then the same version 1 layout.json with two extra pages (qdmy-emu/layout-v1-two-pages.json, sha256 202062c9... read back on the device); no LoR server (both show the same offline agent page). SCREENSHOTS (eh0y-compare/{before,after}/): home, agent, page2, drawer, plus home-back, home-again, drawer-closed. Every pair is pixel-identical below the status bar (ffmpeg difference blend over y 110-2400: YMAX 0); the only differences are the status bar's clock. Dots: four, the agent page's square, the same on both (one row, so per-row dots equal per-page dots); the open drawer shows no dots on either build, so neither intended difference is visible on this layout. HOME-PRESS LATENCY (am start -W HOME with Settings in front, one warm-up then ten): round 1 before 48 62 70 140 148 108 46 80 35 67 median 68.5, after 49 43 50 47 95 50 39 63 60 48 median 49.5; round 2 (reinstalled, same session) before 56 59 59 77 80 77 57 40 65 68 median 62.0, after 95 72 75 49 60 52 40 44 53 49 median 52.5. After is within 10% (faster both rounds; spread run to run is wider, so read as no worse). DRAWER FRAMES (gfxinfo reset, ten opens input swipe 540 1800 540 600 300 and closes 540 700 540 1900 300, home in front checked by screenshot before and after): before 692 frames, janky 15.2% (legacy 88.4%), p50 29 ms, p90 42 ms; after 746 frames, janky 10.1% (legacy 92.9%), p50 32 ms, p90 44 ms: +4.8%, within 10%. Per gesture (framestats after a reset, FrameCompleted - IntendedVsync, medians of ten): open first frame before 51.1, after 60.3 ms (+18%, 7.8 calls a worse first frame a finding for the review); open slowest 66.9 both; close first 16.8 both; close slowest 59.7 before, 64.9 after. A first before drawer pass was discarded: a home press with home in front had opened quick send (before/gfxinfo-void-quicksend.txt). Emulator's layout.json (e86e7fb8...) and launch-stats.json (b3920779...) put back; the after APK left installed."
updated_at: "2026-11-07T21:53:58.318724599+00:00"
---

T3_Phone_API35 (emulator-5554), one session, swiftshader software GL (only the comparison means anything). Before: 7f94a83 built on boat. After: lor-eh0y at f61f6b4, rebuilt on boat; byte-identical to the qdmy APK (sha256 a4bd40e4...). Each build installed with install -r over the same data, then the same version 1 layout.json with two extra pages (qdmy-emu/layout-v1-two-pages.json, sha256 202062c9... read back on the device); no LoR server (both show the same offline agent page). SCREENSHOTS (eh0y-compare/{before,after}/): home, agent, page2, drawer, plus home-back, home-again, drawer-closed. Every pair is pixel-identical below the status bar (ffmpeg difference blend over y 110-2400: YMAX 0); the only differences are the status bar's clock. Dots: four, the agent page's square, the same on both (one row, so per-row dots equal per-page dots); the open drawer shows no dots on either build, so neither intended difference is visible on this layout. HOME-PRESS LATENCY (am start -W HOME with Settings in front, one warm-up then ten): round 1 before 48 62 70 140 148 108 46 80 35 67 median 68.5, after 49 43 50 47 95 50 39 63 60 48 median 49.5; round 2 (reinstalled, same session) before 56 59 59 77 80 77 57 40 65 68 median 62.0, after 95 72 75 49 60 52 40 44 53 49 median 52.5. After is within 10% (faster both rounds; spread run to run is wider, so read as no worse). DRAWER FRAMES (gfxinfo reset, ten opens input swipe 540 1800 540 600 300 and closes 540 700 540 1900 300, home in front checked by screenshot before and after): before 692 frames, janky 15.2% (legacy 88.4%), p50 29 ms, p90 42 ms; after 746 frames, janky 10.1% (legacy 92.9%), p50 32 ms, p90 44 ms: +4.8%, within 10%. Per gesture (framestats after a reset, FrameCompleted - IntendedVsync, medians of ten): open first frame before 51.1, after 60.3 ms (+18%, 7.8 calls a worse first frame a finding for the review); open slowest 66.9 both; close first 16.8 both; close slowest 59.7 before, 64.9 after. A first before drawer pass was discarded: a home press with home in front had opened quick send (before/gfxinfo-void-quicksend.txt). Emulator's layout.json (e86e7fb8...) and launch-stats.json (b3920779...) put back; the after APK left installed.
