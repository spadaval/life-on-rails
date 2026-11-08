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
- [ ] A Tailscale OAuth client (auth keys, devices and policy scopes; it never expires) is kept in a mode-0600 file under `~/.config/` for the whole homelab, not in LoR's env file. Sumeet creates it once in the admin console and hands it over through a `request_secret` card; that is the only web step.
- [ ] The tailnet policy is changed through the API, not the web UI: the current file read (`GET /api/v2/tailnet/-/acl`, HuJSON, keeping its ETag), only the `tag:boat` parts added (`tagOwners` for `tag:boat`, so keys can carry it; this host may reach `tag:boat` on port 22; `tag:boat` reaches nothing), checked with the API's validate call, and written back with `If-Match` on that ETag. If an existing rule (such as the default allow-all) would let `tag:boat` reach anything, that rule is narrowed so tagged devices are left out, and the Report says what else that changes. The diff is in the Report.
- [ ] Tailscale on the VM as a tagged node: a single-use, pre-authorized `tag:boat` auth key minted from this host through the API (`POST /api/v2/tailnet/-/keys`, short expiry), copied to the VM as a mode-0600 file with `boat scp` and used as `tailscale up --auth-key=file:<path> --advertise-tags=tag:boat`, then deleted; the OAuth client secret itself never goes to the VM, and no key appears in a command line, since `boat history` stores every command in full. `tailscaled` is enabled, so the VM rejoins after a resume under the same MagicDNS name and address, shown across one stop and resume.
- [ ] `tag:boat` reaches nothing, shown from the VM, not only by reading the policy: a connection to this host's other ports and to the phone's tailnet address fails, while this host reaches the VM on port 22.
- [ ] `T3_Fold_API35` and `T3_Phone_API35` run on the VM, reach the test server here through a reverse tunnel (`ssh -R 3857:127.0.0.1:3857` or `boat forward --reverse`) at `http://10.0.2.2:3857`, and show in T3 Code's Device panel as an SSH device host, driven through `device_open`. Screenshots recorded as an evidence receipt.
- [ ] One LoR build run while the Fold emulator is open: the build's time, the VM's peak memory, and whether the emulator stays usable, in the Report.
- [ ] The Report says what the verify skill and AGENTS.md would change (who resumes the VM before a check, where timing checks run), for Sumeet to decide.

## Context

- Boat's trial ended 2026-11-04: `large` is available, saved snapshots are allowed (memory: boat-plan). A resume is a new machine on the same disk; only `~/.cache` and `/tmp` are lost, so `/var/lib/tailscale` keeps the node's identity.
- The VM holds Sumeet's logins (`holdsCreatorLogins: true`), and the tailnet includes his phone's wireless adb, hence the narrow policy. The policy is one file for the whole tailnet, so only the `tag:boat` parts change.
- Why Tailscale (Sumeet, 2026-11-08): T3 Code's Device panel needs a stable address for the VM, and boat has none. A VM's SSH address is a port on a shared boat machine (`sshEndpoint` in `boat info`, such as `147.135.129.87:19037`), `null` while stopped and different after every resume; `boat host` gives a stable HTTPS URL for one web port only. An ssh config `ProxyCommand` that looks up the endpoint would work (T3 Code follows ssh config, and `localSshDeviceHost.ts` treats a proxied host as remote), but Sumeet rejected it as hacky, and Tailscale scales to more homelab machines.
- The `tailscale` CLI joins a machine but can't create keys, tag devices or edit the policy; the Tailscale API does all three. On 2026-11-08 the tailnet had no tagged devices.
- Minting a short-lived single-use key, not passing the OAuth client secret to `tailscale up` (which the API allows, `tskey-client-…?preauthorized=true`), keeps a credential that never expires off a cloud VM that holds Sumeet's logins (orchestrator's choice under the mission's Authority).
- Boat is x86_64 only; ARM emulators would need another host (an Apple Silicon Mac), out of scope.
