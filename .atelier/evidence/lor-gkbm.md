---
created_at: "2026-11-08T06:22:22.192155490+00:00"
id: "lor-gkbm"
evidence_type: "validation"
commit: "be8073111a3f66d85424a404950f0c5a3680441e"
result: "pass"
captured_at: "2026-11-08T06:22:22.192123570+00:00"
path: "/tmp/lor-verify/lor-wt-sfh9/final"
agent_identity: "lor-sfh9-author"
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
title: "Final end-to-end run of verify-lor at be80731, after the last change, against the throwaway server on :3857 (life_on_rails_test) and T3_Phone_API35 (emulator-5554). doctor-before.json: api/web/launcher not ready (no server, no copy). up, build on boat (APK sha256 bde8c802), install (server_url http://10.0.2.2:3857, data kept): doctor-after-launch.json all three ready. API: api/intent.json is GET /intents/<id> 200 for a task created with api POST /intents. Web: web/people-2-overdue.png shows Due a hello with only Priya Raman, 15 days past the cadence; people.log has acme cadence: 90. Launcher (driven with agent-device): launcher/settings.png and snapshot.txt show LoR address http://10.0.2.2:3857 and 'Connected. LoR sees you as this box.' after Test connection; launcher/home.png shows home fully drawn; logs/launcher-logcat.txt has 'read /device/home: ok in 554 ms'; crash buffer empty. down stopped the server and removed /tmp/lor-ui-lor-wt-sfh9; evidence on final/ listed the same 14 files with identical sha256 before and after (final-manifests/before.json, after.json); port 3857 free."
updated_at: "2026-11-08T06:22:22.202917895+00:00"
---

Final end-to-end run of verify-lor at be80731, after the last change, against the throwaway server on :3857 (life_on_rails_test) and T3_Phone_API35 (emulator-5554). doctor-before.json: api/web/launcher not ready (no server, no copy). up, build on boat (APK sha256 bde8c802), install (server_url http://10.0.2.2:3857, data kept): doctor-after-launch.json all three ready. API: api/intent.json is GET /intents/<id> 200 for a task created with api POST /intents. Web: web/people-2-overdue.png shows Due a hello with only Priya Raman, 15 days past the cadence; people.log has acme cadence: 90. Launcher (driven with agent-device): launcher/settings.png and snapshot.txt show LoR address http://10.0.2.2:3857 and 'Connected. LoR sees you as this box.' after Test connection; launcher/home.png shows home fully drawn; logs/launcher-logcat.txt has 'read /device/home: ok in 554 ms'; crash buffer empty. down stopped the server and removed /tmp/lor-ui-lor-wt-sfh9; evidence on final/ listed the same 14 files with identical sha256 before and after (final-manifests/before.json, after.json); port 3857 free.
