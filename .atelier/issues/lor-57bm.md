---
created_at: "2026-11-08T17:49:55.917951736+00:00"
id: "lor-57bm"
issue_type: "spike"
labels:
- "launcher"
workflow_branch:
  branch_base: main
  work_branch: lor-57bm
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
closed_at: "2026-11-09T00:52:41.889505601+00:00"
status: "done"
title: "Spike: LoR's emulators on its boat VM, reached over Tailscale"
updated_at: "2026-11-09T00:52:41.889505601+00:00"
---

## Goal

Find out whether LoR's emulator checks can move off this host onto LoR's boat VM, so the emulator (3.6 to 5.8 GB here, the likeliest thing to cause an OOM kill) stops competing with the T3 Code server. Sumeet agreed the shape on 2026-11-08: one VM, `lor-android-build` (`bx_fxc6p2bw`) resized to `large`, holding both the builds and the emulators; `t3-android-build` is left alone.

## Done when

- [x] `lor-android-build` runs as `large` (8 vCPU, 16 GB) and `android/boat-build.sh` still builds and tests on it; the build time before and after is in the Report.
- [x] A Tailscale OAuth client (auth keys, devices and policy scopes; it never expires) is kept in a mode-0600 file under `~/.config/` for the whole homelab, not in LoR's env file. Sumeet creates it once in the admin console and hands it over through a `request_secret` card; that is the only web step.
- [x] The tailnet policy is changed through the API, not the web UI: the current file read (`GET /api/v2/tailnet/-/acl`, HuJSON, keeping its ETag), only the `tag:boat` parts added (`tagOwners` for `tag:boat`, so keys can carry it; this host may reach `tag:boat` on port 22; `tag:boat` reaches nothing), checked with the API's validate call, and written back with `If-Match` on that ETag. If an existing rule (such as the default allow-all) would let `tag:boat` reach anything, that rule is narrowed so tagged devices are left out, and the Report says what else that changes. The diff is in the Report.
- [x] Tailscale on the VM as a tagged node: a single-use, pre-authorized `tag:boat` auth key minted from this host through the API (`POST /api/v2/tailnet/-/keys`, short expiry), copied to the VM as a mode-0600 file with `boat scp` and used as `tailscale up --auth-key=file:<path> --advertise-tags=tag:boat`, then deleted; the OAuth client secret itself never goes to the VM, and no key appears in a command line, since `boat history` stores every command in full. `tailscaled` is enabled, so the VM rejoins after a resume under the same MagicDNS name and address, shown across one stop and resume.
- [x] `tag:boat` reaches nothing, shown from the VM, not only by reading the policy: a connection to this host's other ports and to the phone's tailnet address fails, while this host reaches the VM on port 22.
- [x] `T3_Fold_API35` and `T3_Phone_API35` run on the VM, reach the test server here through a reverse tunnel (`ssh -R 3857:127.0.0.1:3857` or `boat forward --reverse`) at `http://10.0.2.2:3857`, and show in T3 Code's Device panel as an SSH device host, driven through `device_open`. Screenshots recorded as an evidence receipt.
- [x] One LoR build run while the Fold emulator is open: the build's time, the VM's peak memory, and whether the emulator stays usable, in the Report.
- [x] The Report says what the verify skill and AGENTS.md would change (who resumes the VM before a check, where timing checks run), for Sumeet to decide.

## Context

- Boat's trial ended 2026-11-04: `large` is available, saved snapshots are allowed (memory: boat-plan). A resume is a new machine on the same disk; only `~/.cache` and `/tmp` are lost, so `/var/lib/tailscale` keeps the node's identity.
- The VM holds Sumeet's logins (`holdsCreatorLogins: true`), and the tailnet includes his phone's wireless adb, hence the narrow policy. The policy is one file for the whole tailnet, so only the `tag:boat` parts change.
- Why Tailscale (Sumeet, 2026-11-08): T3 Code's Device panel needs a stable address for the VM, and boat has none. A VM's SSH address is a port on a shared boat machine (`sshEndpoint` in `boat info`, such as `147.135.129.87:19037`), `null` while stopped and different after every resume; `boat host` gives a stable HTTPS URL for one web port only. An ssh config `ProxyCommand` that looks up the endpoint would work (T3 Code follows ssh config, and `localSshDeviceHost.ts` treats a proxied host as remote), but Sumeet rejected it as hacky, and Tailscale scales to more homelab machines.
- The `tailscale` CLI joins a machine but can't create keys, tag devices or edit the policy; the Tailscale API does all three. On 2026-11-08 the tailnet had no tagged devices.
- Minting a short-lived single-use key, not passing the OAuth client secret to `tailscale up` (which the API allows, `tskey-client-…?preauthorized=true`), keeps a credential that never expires off a cloud VM that holds Sumeet's logins (orchestrator's choice under the mission's Authority).
- Boat is x86_64 only; ARM emulators would need another host (an Apple Silicon Mac), out of scope.

## Notes

- 2026-11-08 [note] Tailscale OAuth client received from Sumeet (2026-11-08) through request_secret and stored in ~/.config/homelab/tailscale-oauth (mode 0600); its token reads the policy (HTTP 200). The policy today: one allow-all grant (src *, dst *, ip *), tagOwners {tag:Buildbot: [autogroup:admin]} (no device carries it), and an ssh check rule for autogroup:member to autogroup:self. So the allow-all grant must be narrowed (for example src autogroup:member) for tag:boat to reach nothing; no tagged device exists today, so nothing else loses access. Not yet checked: the auth-keys and devices scopes (first key mint).
- 2026-11-08 [decision] Estimate: no app code. Changes are this issue's records, a VM setup script (about 50-100 lines, beside android/boat-setup.sh), the tailnet policy diff, and T3 Code's device-host config. If the spike needs a change to T3 Code itself, stop and report.
  Reason: implement asks for an estimate when the assignment gives none
- 2026-11-08 [decision] The ssh alias lor-android-build (~/.ssh/config on buildbot) skips host-key checking (StrictHostKeyChecking no, UserKnownHostsFile /dev/null). The connection only runs over Tailscale to the MagicDNS name, and WireGuard authenticates the peer by its node key, which survives resumes (same 100.121.136.95 across the stop). Rejected: restoring saved host keys at boot (depends on when boat regenerates them) and Tailscale SSH (a second auth model and a policy ssh rule).
  Reason: every boat resume is a new machine with a new SSH host key (shown 2026-11-08: box-node-230a89a4 -> box-node-5a359f64, 'REMOTE HOST IDENTIFICATION HAS CHANGED'), and T3 Code's ssh runs with BatchMode=yes, so a pinned key breaks the device host after every resume

## Report

LoR's emulators now run on the boat VM and show in T3 Code's Device panel. `T3_Fold_API35` and `T3_Phone_API35` boot on `lor-android-build` through `device_open` (device host `boat`), and LoR on both loads its data from the test server on buildbot through `10.0.2.2:3857` (receipt lor-06j7). The VM runs as `large` and both build tasks pass there. It is a `tag:boat` node that the policy keeps from reaching anything, checked from the VM (containment receipt). It rejoined under the same name and address after a real stop and resume onto a new machine, and T3 Code reconnected without changes (resume receipt). Every Done when line holds. Evidence is in `~/.local/share/lor-evidence/lor-57bm/`, not `/tmp` (lor-ea15).

**Builds** (`android/boat-build.sh`, default tasks; logs in the evidence folder):

| build | `default` (4 vCPU, 8 GB) | `large` (8 vCPU, 16 GB) |
| --- | --- | --- |
| first after a resume, cold daemon | 73 s (Gradle 58 s) | 27 s (Gradle 7 s) |
| `clean`, warm daemon | 42 s (Gradle 28 s) | 57 s (Gradle 31 s) |
| `clean`, both emulators running | n/a | 48 s (Gradle 27 s) |

LoR's builds are too small for `large` to speed up. It pays for memory: with both emulators up, the VM used 14.1 of 16 GB before the build and peaked at 14.5 GB (swap 68 MB, load 11.3), and both emulators stayed usable. The emulator sizes RAM to the screen, as it does on buildbot: the Fold's qemu holds 5.2 GB, the phone's 3.5 GB, and the Gradle and Kotlin daemons about 3.5 GB after a build. One emulator plus builds fits easily; two plus a build is near the limit.

**Tailnet policy** (diff in `tailnet-policy.diff`): the allow-all grant became `autogroup:member` to `autogroup:member`. A new grant lets `buildbot` (a `hosts` alias for 100.124.153.80) reach `tag:boat` on `tcp:22` only. `tag:boat` is added to `tagOwners`, and a `tests` block makes Tailscale reject any policy that lets `tag:boat` reach buildbot's 22 and 3857, the phones' adb, or the PC's RDP; it rejected the old allow-all policy and passes the new one. The four devices (buildbot, Legend, Pixel 9a, Z Fold7) are all yours and untagged, with no subnet routes, exit nodes or shared devices, so nothing they did before changes. What does change: a future tagged device, or one shared in from another tailnet, reaches nothing until a grant names it.

**Joining**: the key was single-use, pre-authorized, `tag:boat` and expired in 10 minutes. It was minted here, copied as a 0600 file with `boat scp`, used as `--auth-key=file:`, then shredded on both ends. The OAuth secret never left buildbot.

### Deviations

- SSH host-key checking is off for the `lor-android-build` alias (decision note). Every resume is a new machine with a new host key, which broke T3 Code's `BatchMode` ssh.
- The `10.0.2.2:3857` tunnel ran as a transient unit (`systemd-run --user --unit=lor-boat-tunnel ... ssh -N -R 3857:127.0.0.1:3857 lor-android-build`), not committed anywhere. Making it permanent is in the proposal below.

### Surprises

- After `boat stop` reports `stopped`, the machine keeps running for about 11 minutes, and a resume in that window takes it back with its memory intact (Tailscale showed it `active` the whole time). Only after that does a resume get a new machine.
- `agent-device install` works from buildbot straight to the remote emulator, so nothing has to copy APKs by hand.
- A `large` VM bills at 2x: each hour uses two of the plan's 556 VM hours. Six emulator hours a day would use about 360 a month on top of T3 Code's builds.
- T3 Code's device tools on buildbot start emulators inside `t3code.service`, the server's own cgroup, where oomd can't kill them. That is a T3 Code bug, and moving emulators to boat sidesteps it.

### Proposal for the verify skill and AGENTS.md (for Sumeet to decide)

1. **Who resumes the VM.** T3 Code doesn't, and a stopped VM has no address. Proposal: a `verify-lor boat up` command that runs `boat resume bx_fxc6p2bw` (as `large`), waits for ssh over the tailnet, starts the tunnel, and extends auto-stop. T3 Code doesn't extend it, so a long emulator session ends at the deadline (1 h after a resume by default; `boat-build.sh` cuts it to 30 min).
2. **The tunnel.** It should become a user unit on buildbot with `Restart=always`, started by `boat up` rather than always on, since it retries every 20 s while the VM is stopped.
3. **`install` and the app's server URL.** `verify-lor install` uses local adb and refuses serials that aren't `emulator-N` here. On boat, install with the `agent-device` command `device_open` returns, and write `server_url` with `adb` over ssh on the VM, the same `run-as` steps. The refusal stays as the safety check, applied to the VM's serials.
4. **AGENTS.md, "The phone app".** It should say the emulator check runs on the boat host `boat` in T3 Code's Device panel, and that the local AVDs stay only as a fallback, or get deleted, which is Sumeet's call. Timing checks on the emulator (frame times) should run on one machine only; the VM's swiftshader rendering differs from buildbot's.
5. **T3 Code's own work** can use the same host: it is in T3 Code's settings for every project.

### Open items

- The real Fold and Pixel are untouched; their checks stay as they were.
- `android/boat-emulator-setup.sh` changes the VM's `~/.bashrc` (a JDK 21 block above the interactive guard) so `avdmanager` finds Java over ssh. A boat environment rebuild would need the script run again.
