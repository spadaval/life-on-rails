---
schema: atelier.review
schema_version: 3
round: 1
commit: 96850903dade25de42ada00cbbf3cc2a980f565b
reviewers:
- thread:delegated-task:command%3Amcp%3A4e28cea3-5d3f-4a78-96d9-57390e489329%3Adelegate-task%3A28fbde96-8fd1-46a8-85d7-eee915909c6f
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-12T00:17:06.584807606Z
---

## Summary

Complexity: medium. SSH command quoting, APK transfer and diagnostics must work across two device hosts.
Value: high. This moves emulator memory and CPU use away from buildbot, which runs the owner's T3 Code service.
Risk of merging it: medium. The helper changes an emulator's installed app and preferences on another host, but serial and host guards bound the target and failures are visible.

The value warrants this change. The shared SSH transport and two-entry host table are a small implementation of the requested Mac default and explicit boat fallback. No smaller change would satisfy the host routing, install and diagnostic requirements together.

Reviewed `git diff main...96850903dade25de42ada00cbbf3cc2a980f565b`, the complete verify CLI, its tests, the three changed instruction files and the receipt. Every adb invocation goes through `onHost`; the device list filters to `emulator-N`, and install rejects a supplied physical serial before any host command. APK transfer precedes remote install; the existing before-and-after preference writes and read-back check remain in place. Host flags and environment selection resolve to the two allowed hosts, and the command arguments are quoted for the remote shell.

Validation:

- `npx vitest run tests/verify-lor.test.ts`: 26 tests pass, including host selection, invalid hosts and physical serial refusal on both hosts.
- `node .claude/skills/verify-lor/verify.mjs doctor`: exit 0 on the Mac; host, T3_Phone_API35, test-server URL and 3857 tunnel all report ready.
- `node .claude/skills/verify-lor/verify.mjs doctor --host boat`: exit 1 as expected; the resumed boat host is reachable, with no running emulator or tunnel. The output names `device_open` on hostId `boat` and the reverse-tunnel command as fixes.
- Read the Mac emulator's preferences over SSH with `adb exec-out run-as ... cat`: `server_url` is `http://10.0.2.2:3857`.
- Checked Mac and boat install dry runs and the build dry run. The plans copy to the chosen host and build through `android/boat-build.sh`.
- All six evidence files match the receipt's SHA-256 hashes. Inspected the screenshot and snapshot showing "Ship LoR", the SyncWorker success log and empty crash buffer. The saved boat build log reports `BUILD SUCCESSFUL in 2m 5s`, and the APK hash matches the receipt's prefix.

All Done when requirements are supported by the code and recorded run. I did not rerun build or install, open or drive a device, change a tunnel, or contact the real database or service endpoints. Boat's preference-write path could not be exercised because it has no running emulator, and the reported platform-tools `exec-in` behavior was not independently reproduced. The saved Mac run and current read-only checks support approval.

No findings.
