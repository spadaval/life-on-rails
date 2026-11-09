---
created_at: "2026-11-09T00:16:07.049922236+00:00"
id: "lor-06j7"
evidence_type: "validation"
commit: "29b2885f92ec4a0bb13d34ce51945e187b1cea38"
result: "pass"
captured_at: "2026-11-09T00:16:07.049819064+00:00"
path: "/home/supadava/.local/share/lor-evidence/lor-57bm"
agent_identity: "claude-opus-5-5"
target:
  kind: "issue"
  id: "lor-57bm"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-57bm"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "T3_Fold_API35 (2076x2152, unfolded) and T3_Phone_API35 boot on lor-android-build through T3 Code's device_open (device host 'boat', ssh over Tailscale). LoR's debug APK, installed with agent-device from buildbot, loads goals (Work, Ship LoR) from the test server on buildbot via http://10.0.2.2:3857, the VM's loopback and an ssh -R tunnel; nc from the emulator to 10.0.2.2:3857 returned HTTP/1.1 200. Screenshots: boat-fold-lor-test-server.png, boat-phone-lor.png."
updated_at: "2026-11-09T00:16:07.074125557+00:00"
---

T3_Fold_API35 (2076x2152, unfolded) and T3_Phone_API35 boot on lor-android-build through T3 Code's device_open (device host 'boat', ssh over Tailscale). LoR's debug APK, installed with agent-device from buildbot, loads goals (Work, Ship LoR) from the test server on buildbot via http://10.0.2.2:3857, the VM's loopback and an ssh -R tunnel; nc from the emulator to 10.0.2.2:3857 returned HTTP/1.1 200. Screenshots: boat-fold-lor-test-server.png, boat-phone-lor.png.
