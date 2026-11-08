---
created_at: "2026-11-08T17:49:55.917951736+00:00"
id: "lor-57bm"
issue_type: "spike"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-8a7n"
    type: "related"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Spike: LoR's emulators on its boat VM, reached over Tailscale"
updated_at: "2026-11-08T17:49:55.917951736+00:00"
---

## Goal

Find out whether LoR's emulator checks can move off this host onto LoR's boat VM, so the emulator (3.6 to 5.8 GB here, the likeliest thing to cause an OOM kill) stops competing with the T3 Code server. Sumeet agreed the shape on 2026-11-08: one VM, `lor-android-build` (`bx_fxc6p2bw`) resized to `large`, holding both the builds and the emulators; `t3-android-build` is left alone.

## Done when

- [ ] `lor-android-build` runs as `large` (8 vCPU, 16 GB) and `android/boat-build.sh` still builds and tests on it; the build time before and after is in the Report.
- [ ] Tailscale on the VM as a tagged node (`tag:boat`, joined once with a pre-approved auth key Sumeet supplies through `request_secret`), with `tailscaled` enabled so it rejoins after a resume under the same MagicDNS name and address, shown across one stop and resume. The tailnet policy (Sumeet's change) lets only this host reach `tag:boat`, and only on port 22, and lets `tag:boat` reach nothing.
- [ ] `T3_Fold_API35` and `T3_Phone_API35` run on the VM, reach the test server here through a reverse tunnel (`ssh -R 3857:127.0.0.1:3857` or `boat forward --reverse`) at `http://10.0.2.2:3857`, and show in T3 Code's Device panel as an SSH device host, driven through `device_open`. Screenshots recorded as an evidence receipt.
- [ ] One LoR build run while the Fold emulator is open: the build's time, the VM's peak memory, and whether the emulator stays usable, in the Report.
- [ ] The Report says what the verify skill and AGENTS.md would change (who resumes the VM before a check, where timing checks run), for Sumeet to decide.

## Context

- Boat's trial ended 2026-11-04: `large` is available, saved snapshots are allowed (memory: boat-plan). A resume is a new machine on the same disk; only `~/.cache` and `/tmp` are lost, so `/var/lib/tailscale` keeps the node's identity.
- The VM holds Sumeet's logins (`holdsCreatorLogins: true`), and the tailnet includes his phone's wireless adb, hence the narrow policy.
- Boat is x86_64 only; ARM emulators would need another host (an Apple Silicon Mac), out of scope.
