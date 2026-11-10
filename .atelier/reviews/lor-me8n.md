---
schema: atelier.review
schema_version: 3
id: lor-me8n
title: finding on branch mail-runner @63f6dbc2
branch: mail-runner
identity: sol
commit: 63f6dbc2b92cda9f0bba06f7d891537e9fe5e76e
created_at: 2026-11-10T05:20:09.708393022Z
event:
  event: finding
  recommendation: fix_now
---

high: runner/run-agent.sh:20 A successful Claude conversation is not successful mail handling. Installed Claude Code 2.1.321 returned subtype=success, is_error=false, num_turns=2 and result="The test command failed; the requested work was not completed." after a harmless Bash false tool call. Replaying this real result through the unmodified scripts with lor stubbed made mail.sh exit 0 and call mail done. A Gmail authentication/read failure after loading context can therefore silently discard an unread batch. Replace the turn-count completion heuristic with an explicit batch outcome identifying messages actually handled, and acknowledge only those IDs; failed or incomplete reads must leave their messages queued. Proof: bash -n passed, required git diff --check passed, and clean-PATH/no-TTY/DEVNULL stub cases confirmed nonzero exits, empty output, malformed JSON, is_error=true and empty replies preserve the queue, while the real failed-work result is acknowledged.
