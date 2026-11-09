---
schema: atelier.review
schema_version: 2
id: lor-e38o
title: review on lor-57bm @a210de0e
target: lor-57bm
identity: gpt-6.1-sol-reviewer
commit: a210de0e316eafc1055a4c891c39f1c94444bdea
created_at: 2026-11-09T00:52:01.745634121Z
event:
  event: review
---

Verdict: merge. The spike establishes that emulator checks can leave buildbot without opening the boat VM to initiate tailnet connections. Complexity medium (VM setup, tailnet policy, SSH and T3 settings); value high (protects the live T3 server from emulator memory pressure); risk medium (live infrastructure changes, scoped and reversible).

Contract: pass on the supplied evidence and read-only checks, with historical verification limits below. Fitness: pass. No findings or open FIX NOW recommendations.

Reviewed the entire git diff 29b2885..a210de0: android/boat-emulator-setup.sh, the issue and its Report and decisions, and receipts lor-06j7, lor-x3g7 and lor-sphp. Read AGENTS.md, the relevant product spec and verify-lor instructions. This CLI requires review request, rather than review show with an issue ID; obtained the brief with review request lor-57bm --identity gpt-6.1-sol-reviewer --from 29b2885.

Done when assessment:
1. Pass: boat info reports lor-android-build as large, hydrated, 8 vCPU, 16 GB; SSH independently reports 8 CPUs and 15990 MiB. Read all five preserved build logs. They include assembleDebug and testDebugUnitTest and successful Gradle runs of 58, 28, 7, 31 and 27 seconds. Report gives before/after wall times and distinguishes clean runs. The 7-second large run has 40 tasks up to date, so the first-run row is not an equal-work speed comparison; the clean rows and the stated memory rationale avoid claiming a speedup.
2. Pass on recorded handoff: credential file exists at the homelab path with mode 0600. Did not read it. The request_secret handoff, OAuth scopes and non-expiry are historical claims in the issue, not independently reverified here.
3. Pass on the policy diff and recorded procedure: allow-all becomes autogroup:member to autogroup:member; the only new network grant is buildbot 100.124.153.80 to tag:boat TCP 22. tagOwners preserves tag:Buildbot and adds tag:boat for admins. No grant originates from tag:boat; tagged identity does not inherit user membership (Tailscale docs: https://tailscale.com/docs/features/tags). Report identifies the effect on future tagged and shared-in devices. The deny tests cover the named endpoints, not every possible destination or port. Original GET/ETag, validate and conditional-write sequence was not replayed or independently audited through the credential.
4. Pass on resume receipt plus current state: VM is online at lor-android-build.tail47515a.ts.net, 100.121.136.95, with tag:boat; tailscaled is enabled and active. Receipt records the distinct machines across the real stop/resume. Examined the latest 100 boat command-history entries without printing their command text or outputs: the join uses file-backed auth, no literal auth/client token pattern was found. No homelab OAuth file exists at the corresponding VM path, and the limited filename search found no temporary auth-key file. Key mint permissions, ten-minute expiry, scp mode and deletion on both ends remain historical claims; this is not a complete disk or history audit.
5. Pass: fresh VM-origin TCP probes to buildbot 22, 3848 and 3857 and Pixel 5555 all fail with four-second timeouts. Buildbot TCP 22 answers a control probe and SSH to the VM succeeds. Pixel is offline, so that negative probe alone does not establish ACL denial; policy analysis plus the responsive buildbot control does. Read containment.txt and its recorded broader probes.
6. Pass: viewed both emulator PNGs and the after-resume Fold PNG; they show LoR with Work and Ship LoR. device_list(hostId=boat) reports the SSH host ready, Android available, Fold running and the phone AVD present. Did not open, drive, boot or close either device. VM loopback 3857 returns HTTP 200; its listener is loopback-only. Buildbot tunnel unit uses BatchMode, ExitOnForwardFailure and ServerAliveInterval. The destination listener is 127.0.0.1:3857 with DATABASE_URL database name life_on_rails_test.
7. Pass on Report and preserved load-build log: clean build with both emulators reported at 48 seconds wall, 27 seconds Gradle; Report gives 14.5 GB peak and responsiveness assessment. Historical peak sampling and usability under simultaneous build load were not reproduced in this read-only review.
8. Pass: Report proposes who resumes and extends the VM, tunnel ownership, remote install safety, AGENTS changes and a single machine for timing comparisons, leaving adoption and local AVD retention to Sumeet.

Fitness checks: bash -n passes. Both requested git diff --check ranges pass. Read VM bashrc, SDK/Node symlinks and both AVD configs. Noninteractive SSH selects JDK 21; there is exactly one LOR JAVA block. Simulating the managed AVD key deletion/reappend in memory leaves both current configs unchanged and is idempotent. Static review finds no defect in rerunning on this configured VM; the mutating installer itself was not rerun. Credential-pattern scan found no literal Tailscale auth/client keys in the reviewed issue, evidence text, logs, policy diff or script.

Security: ~/.ssh/config scopes disabled host-key checking to this alias, resolves now to the expected Tailscale address, uses the selected identity only, and does not forward the agent. This relies on Tailscale/MagicDNS peer identity in place of SSH host-key pinning, as explicitly recorded; it is not protection against a compromised tailnet control plane or VM. Reverse tunnel deliberately gives the VM access to the throwaway test service despite outbound tailnet denial. No new exposure beyond that intended boundary was found.

Open questions: Sumeet still decides adoption, automatic VM/tunnel lifecycle and retaining local AVDs. Residual risk: two emulators plus build have little memory headroom; tunnel is transient; auto-stop can end a session; software rendering changes timing. No builds, config changes, policy changes, stop/resume/resize or device actions were performed. Only Atelier review records were written.
