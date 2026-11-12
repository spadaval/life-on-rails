---
schema: atelier.item
schema_version: 3
id: lor-6txa
title: LoR's emulator checks run on the Mac, with boat as the fallback; buildbot runs none
status: building
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- 12e0fab0-5835-4a39-b208-398b9a0a22d4
ready:
  by: sumeet (chat, 2026-11-11)
  at: 2026-11-12T00:08:53.586358003Z
trivial: null
caused_by: null
created_at: 2026-11-12T00:08:41.350792118Z
---

## Goal

LoR's emulator checks run on the M2 MacBook Air, T3 Code's device host `mac`, which renders through the M2's GPU, boots the phone AVD in about 10 s and costs nothing. Boat (`lor-android-build`, device host `boat`) is the fallback when the Mac is unreachable. buildbot runs no emulator: one takes 3.6 to 5.8 GB of its 14 GB, and T3 Code starts it inside `t3code.service`, where oomd can't kill it. Sumeet's decision, 2026-11-11.

The Mac's setup is done (outside the repo): the AVDs `T3_Phone_API35` and `T3_Fold_API35` (arm64, `hw.gpu.mode=host`), `ssh mac`, and the always-on `lor-mac-tunnel` user unit on buildbot, which gives the Mac's 127.0.0.1:3857 to buildbot's, so `http://10.0.2.2:3857` reaches the test server.

## Done when

- [x] `verify.mjs` runs every adb command on the chosen device host over ssh: `mac` by default, `boat` with `--host boat` or `LOR_VERIFY_HOST=boat`, using `~/Android/Sdk/platform-tools/adb` there. `install` copies the APK to the host first. No command runs adb on buildbot.
- [x] `install` still refuses any serial that is not `emulator-N`, on either host, and `doctor` reports the host's reachability, its emulator, the app's `server_url` and the 3857 tunnel, with the fix for each.
- [x] `tests/verify-lor.test.ts` covers the host choice and the refusal, and passes.
- [x] SKILL.md, `references/features/launcher.md` and AGENTS.md say: emulators run on the Mac (`device_open` with hostId `mac`), boat is the fallback (resume, tunnel, hostId `boat`), and none run on buildbot.
- [x] On the Mac: `build`, `install`, the app on `T3_Phone_API35` loading data from the test server, and `logs`, with screenshots and the logs recorded as a receipt.

## Scope

In: `.claude/skills/verify-lor/` (verify.mjs, SKILL.md, references), `tests/verify-lor.test.ts`, AGENTS.md.

Out: deleting buildbot's AVDs and SDK, T3 Code's own emulator guidance, the real phones, and `android/boat-emulator-setup.sh`.

## Report

LoR's emulator checks now run on the M2 MacBook Air, with boat as the fallback, and verify-lor runs no adb on buildbot. `verify.mjs` takes `--host mac|boat` (or `LOR_VERIFY_HOST`), defaulting to `mac`. It runs every adb command on that host over ssh, at `~/Android/Sdk/platform-tools/adb` there, and refuses any other host, buildbot included, with usage exit 2. `install` copies the APK to the host's `/tmp` first and still refuses a serial that is not `emulator-N`. `doctor` adds three checks: the device host, the emulator on it, and the 3857 tunnel. Each comes with its fix: the Mac's Tailscale state, `device_open` with that hostId, and restarting `lor-mac-tunnel` (or, on boat, starting the reverse tunnel).

The run on the Mac (receipt `artifacts/receipt.md`) went `doctor`, `build`, `install`, `device_open`, then `logs`. LoR on `T3_Phone_API35` lists "Ship LoR", an intent in the test database, and its crash buffer is empty. It found one real bug: on the Mac's platform-tools 37, `adb exec-in` exits 0 and writes nothing, so `install` couldn't set `server_url`. The prefs write now pipes into `adb shell "run-as ... sh -c '...'"`, which works there. The Mac-only behaviour is a guess about platform-tools 37; boat wasn't rerun with the new write, though `adb shell` with stdin is the older and more common path.

Tests: `tests/verify-lor.test.ts` uses a fake `ssh` (`LOR_VERIFY_SSH=/bin/false`) in place of the fake adb. New cases cover the Mac default, `--host boat` and `LOR_VERIFY_HOST=boat`, the refusal of `local` and `buildbot`, a physical serial on boat, and `doctor`'s unreachable-host fix. 26 pass.

Docs: SKILL.md (Phone, Doctor, the launch section with the Mac and the boat fallback), `references/features/launcher.md` (install, `device_open` hostId, the `run-as` read and write over `ssh mac`), and AGENTS.md (the host rule and "The phone app").

Outside the repo, done before this item: the Mac's AVDs, `ssh mac`, T3 Code's `mac` device host and the `lor-mac-tunnel` unit. Left for Sumeet: whether buildbot's own AVDs and SDK get deleted, and T3 Code's own emulator guidance in `~/.claude/CLAUDE.md`.

