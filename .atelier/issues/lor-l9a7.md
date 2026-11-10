---
created_at: "2026-11-10T06:11:15.540367025+00:00"
id: "lor-l9a7"
issue_type: "mission"
labels:
- "chain"
strategy:
  path: docs/missions/working-chain/strategy.md
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "draft"
title: "LoR's chain works end to end: the phone's link, one line on the wall, honest numbers"
updated_at: "2026-11-10T06:11:15.540367025+00:00"
---

## Outcome

On the surfaces LoR already has: the phone collects with no network, uploads the moment one returns, and reports its state with every sync, so LoR shows when the link was down and why; after 90 minutes of silence in waking hours LoR sends one WhatsApp message, at most one a day; the morning run pins the day's action in 60 characters or fewer, and the wall and the phone's home show it whole; decisions waiting on Sumeet are under Needs you without a message each; Today, Goals, the wall and People stop showing figures that are wrong or that read as failure where nothing was measured; and LoR records its own use, printed by `lor use`. The strategy is `docs/missions/working-chain/strategy.md`.

## Success criteria

- link-heals: On `T3_Phone_API35` against the test server, with the emulator's network off while the app collects for 30 minutes and then on, the queued observations and status rows reach the server within one minute of the network returning, and `lor status` on the test server shows the phone's link as down for that span.
- one-line: On the test server, a brief written and pinned the way the `agent` page then instructs gives a wall Today frame whose one thing is at most 60 characters and is drawn whole, and Today's Needs you lists each decision the brief names, with no `lor:flag` message sent for them.

## Background

- `docs/making-lor-useful.md` (2026-11-10): six audits, the live database and Sumeet's messages. He agreed the outline that day: the slice first, new features pause until the current ones are improved, LoR may message him itself over WhatsApp, the project tour is weekly. He warned against reading low use as a feature being unwanted.
- Asked why Tailscale is off on the Fold when it is off, he answered "both, or not sure" (2026-11-10), so the route is decided from status data (the decision item linked here), not in this mission.
- The mail runner (`runner/run-agent.sh` empty since `3219c19`) is being restored on the `mail-runner` branch from the agent's work queue, outside this mission.
