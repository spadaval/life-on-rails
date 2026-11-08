---
created_at: "2026-11-08T06:18:26.604154415+00:00"
id: "lor-dz4i"
evidence_type: "validation"
commit: "5da1e2e798a4c168e007026378eb90daa2f1112f"
result: "pass"
captured_at: "2026-11-08T06:18:26.604093742+00:00"
path: "/tmp/lor-verify/lor-wt-sfh9"
agent_identity: "lor-sfh9-e2e"
target:
  kind: "issue"
  id: "lor-sfh9"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-sfh9"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Cleanup and survival, commit 5da1e2e: after verify.mjs down (stopped pid 965350, removed /tmp/lor-ui-lor-wt-sfh9) and device_close without shutdown, manifest-after-cleanup.json lists the same 14 files under e2e/ with the same sha256 as manifest-before-cleanup.json. Port 3857 has no listener and the copy directory is gone. Emulator left running with the app installed and pointed at the test server."
updated_at: "2026-11-08T06:18:26.614035355+00:00"
---

Cleanup and survival, commit 5da1e2e: after verify.mjs down (stopped pid 965350, removed /tmp/lor-ui-lor-wt-sfh9) and device_close without shutdown, manifest-after-cleanup.json lists the same 14 files under e2e/ with the same sha256 as manifest-before-cleanup.json. Port 3857 has no listener and the copy directory is gone. Emulator left running with the app installed and pointed at the test server.
