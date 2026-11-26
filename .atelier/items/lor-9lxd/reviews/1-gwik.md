---
schema: atelier.review
schema_version: 3
round: 1
commit: 054a95fa2f195f117fe53485fd40fe4707509853
reviewers:
- thread:delegated-task:command%3Amcp%3A7626aff0-b2ca-41ee-8cfb-234235c1fbc9%3Adelegate-task%3Aaea0ebaa-e3e7-4b9a-b9f5-b366b1cdf4b1
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T00:20:54.517676076Z
---

## Summary

Complexity: low. The host change removes a trailing PATH override from two shell startup files and disables four corepack shims.

Value: high. Ordinary pnpm commands, including installation, tests and deployment builds, were blocked by the wrong executable.

Risk of merging it: medium. Shell PATH changes affect tools across this host's repositories, including the selected Node version, but the backups and corepack enable command make the change easy to undo.

The fix is worth its size. I found no functional problem with the host changes. One factual correction to the required regression report remains before approval.

I compared ~/.profile and ~/.bashrc with their dated backups: their only differences remove the final Antigravity comment and PATH override. Both startup files still add ~/.local/bin before sourcing Vite+'s environment. Bash syntax checks pass, and .profile also passes sh's syntax check. I confirmed the four ~/.local/bin corepack shims are absent, agy still resolves, and the Vite+ pnpm and node executables exist. The running lor-web unit is active and invokes ~/.local/opt/node/bin/node by absolute path. No removed shim paths are referenced in the searched systemd user units, scripts, or Android shell scripts.

In a fresh detached worktree of main at 9d21e8f1, plain pnpm resolves to ~/.local/share/vite-plus/bin/pnpm and reports 12.11.2 under bash -ic, bash -lic and env -i HOME=/home/supadava bash -lic. It also works in the agent shell and with a deliberately stale PATH placing ~/.local/bin before Vite+. pnpm install succeeds without borrowing another checkout's node_modules; pnpm test passes 24 files and 208 tests. pnpm typecheck starts correctly and exits 2 because src/routeTree.gen.ts is missing, with resulting router type errors. That existing defect is covered by lor-iurl, whose scope and status I checked.

I verified the original v2 package.json pins pnpm 12.11.2, inspected corepack 0.34.0's hard-coded pnpm.cjs mapping and both cached packages' .corepack metadata, and reproduced the missing pnpm.cjs failure by invoking the old shim directly with networking disabled. The profile backups and agy's matching timestamps support the Antigravity PATH explanation. Historical shell environments and the claim about exactly when previously running shells stopped working are not directly observable now; the cache timestamps do not support the report's stated download chronology, as detailed below. No deployment or service restart was needed for this host-only review.

## Finding 1: Correct the cache evidence for the regression timeline
Recommendation: fix now

In .atelier/items/lor-9lxd/item.md:47, the Report says corepack fetched pnpm 12.18.0 on 2026-11-22 at 02:09 and uses that to corroborate the November 21 shell regression. The cached package predates that event: stat reports birth and modification times of 2026-11-09 13:21:57 for ~/.cache/node/corepack/v1/pnpm/12.18.0/.corepack, and the bin directory and bin/pnpm.mjs were also created on November 9. The November 22 02:09 timestamp belongs to the later pnpm-native binary and the containing directory's modification time, not the package's initial cache population.

Correct or remove the November 22 package-fetch claim, distinguish the observed native-binary timestamp from the older package metadata, and label any inference about historical shell use as an inference. The backed-up startup files still support the PATH-order cause, so this needs a report correction rather than another host change. The required explanation should not present a directory modification time as proof of when corepack fetched the package.
