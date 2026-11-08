---
created_at: "2026-11-08T09:24:21.293175019+00:00"
id: "lor-v9uo"
evidence_type: "validation"
commit: "1170d3e40089695403e19276e7ba0c6e15cdff81"
result: "pass"
captured_at: "2026-11-08T09:24:21.293119435+00:00"
path: "/tmp/lor-verify/lor-wt-clgw/rz50-failure"
agent_identity: "claude-opus-5.5-implementer"
target:
  kind: "issue"
  id: "lor-rz50"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-rz50"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "On failure, 1170d3e (APK rz50-b3, sha256 fc562d5a…), T3_Phone_API35 against :3857. With the server process paused (SIGSTOP): 01/02 Start's menu shows Untitled and 'Reading…'; 03/05 a tick under way leaves its toggle enabled:false (05, raw snapshot) until the call ends (06, done after SIGCONT). With the server stopped (07): 08/09 a tick's row is back unticked with 'LoR didn't answer. Is Tailscale on?' under it, the screenshot about 0.8 s after the tap; 10/11 Seen's card is back with the reason; 12/13 Give to the agent's row is back untagged with the reason; 14/15 Start's menu shows the reason under Untitled, and 16/17 Untitled fails with the reason under the timer. After 'verify.mjs up' (main 266f3c3): 18 Return the library books made a Google task by SQL after the read, 19/20 its tick refused with '409: this task lives in Google Tasks: complete it there and the next poll marks it done here.' logcat-lor.txt shows each write's ConnectException and the IOException; logcat-crash.txt is empty."
updated_at: "2026-11-08T09:24:21.313945417+00:00"
---

On failure, 1170d3e (APK rz50-b3, sha256 fc562d5a…), T3_Phone_API35 against :3857. With the server process paused (SIGSTOP): 01/02 Start's menu shows Untitled and 'Reading…'; 03/05 a tick under way leaves its toggle enabled:false (05, raw snapshot) until the call ends (06, done after SIGCONT). With the server stopped (07): 08/09 a tick's row is back unticked with 'LoR didn't answer. Is Tailscale on?' under it, the screenshot about 0.8 s after the tap; 10/11 Seen's card is back with the reason; 12/13 Give to the agent's row is back untagged with the reason; 14/15 Start's menu shows the reason under Untitled, and 16/17 Untitled fails with the reason under the timer. After 'verify.mjs up' (main 266f3c3): 18 Return the library books made a Google task by SQL after the read, 19/20 its tick refused with '409: this task lives in Google Tasks: complete it there and the next poll marks it done here.' logcat-lor.txt shows each write's ConnectException and the IOException; logcat-crash.txt is empty.
