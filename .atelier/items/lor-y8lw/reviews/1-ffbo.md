---
schema: atelier.review
schema_version: 3
round: 1
commit: 1f220dbb2cb3ed94191ca61863a1501eeed483c9
reviewers:
- thread:delegated-task:command%3Amcp%3Aec88d753-5d22-4bb6-a39f-69b19bba51d2%3Adelegate-task%3Aba81e731-886f-4f33-ba93-d4fe37f4078f
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T07:18:40.983452872Z
---

## Summary

- Complexity: low. Two startup guards, subprocess tests and their documentation; no new runtime component.
- Value: high. This prevents a demonstrated way for a checkout's test command to write to the owner's LoR.
- Risk of merging: high. An incomplete destination check can still silently admit writes to the real service.

The value warrants the small change, but three gaps need fixing before approval. I reviewed the full diff at `1f220dbb2cb3ed94191ca61863a1501eeed483c9`, the Done when lines, the Report, configuration loading, the verify wrapper and its environment handling, and the CLI calls in the UI flows.

`pnpm test` passed all 229 tests in 28 files, and `pnpm typecheck` passed. Both emitted the previously reported pnpm warning about the worktree's symlinked node_modules before running successfully. The missing-server refusal, default production ports, help and explicit test destination behave as reported. I checked the existing `/tmp/lor-verify/y8lw/tasks.log`; it contains the reported 24 passing assertions. I did not rebuild or rerun the browser flow, change either LoR database, or send a write to the live service. Additional checks used fake env files, an intercepted fetch/browser startup, and temporary HTTP servers owned by this review.

## Finding 1: Refuse alternate loopback spellings of the same production listener
Recommendation: fix now

`cli/lor.mjs:151` recognizes only three literal hostnames. `localhost.` and `[::ffff:127.0.0.1]` also reach the IPv4 loopback listener, but neither matches this list. Consequently, an explicit LOR_URL using either spelling and port 3847 is allowed to send a write with the loaded agent token, despite the second Done when line and the claim that a source-file invocation never reaches lor-web.

I reproduced this without touching lor-web: a temporary HTTP server bound to `127.0.0.1:43879`, with `LOR_PORT=43879` in a fake env file, received `POST /api/v1/intents` from both aliases and the CLI exited 0. The exact `127.0.0.1` spelling correctly exited 6 without a request. A fetch interception separately confirmed the same bypass for port 3847. Make the production-port refusal cover these equivalent local destinations and add regression cases that cannot write to a real service.

## Finding 2: Include configured production ports in the UI-check refusal
Recommendation: fix now

`scripts/ui-check.mjs:15` blocks only 3847, 3848 and 3849. It does not include the ports in the env file, unlike the CLI guard and the existing verify and shots guards. If lor-web uses another configured port, a LOR_UI_BASE pointing at that production listener passes this check. This leaves the UI-check Done when line incomplete; the guard should reject the target before browser startup or any flow work.

With a fake env file containing `LOR_PORT=4000` and `LOR_SERVE_PORT=4001`, I intercepted `chromium.launch` to stop before a browser or flow could run. Both bases on 4000 and 4001 reached that interception, whereas 3847, 3848 and 3849 exited 3. Read the configured production ports for this guard too and cover them in a refusal test.

## Finding 3: Keep allowed-path tests from posting to an unowned port
Recommendation: fix now

`tests/lor-cli.test.ts:25` hardcodes port 4000 in the fake env file, and line 60 invokes the symlink with the default `intent create` command from line 18. This is a real fetch to `127.0.0.1:4000`; nothing checks or reserves that port. The test therefore depends on the machine's running services and can send a write to one, contrary to its comment and the Done when requirement to test allowed paths without reaching a server. The separately allocated and then closed ephemeral port also does not guarantee that a later connection cannot find a listener.

I bound a review-owned HTTP fixture to 4000 and reran only this test file. The fixture received `POST /api/v1/intents`, and the symlink test failed because the CLI exited 0 instead of 8. Intercept fetch in the child process for these tests and assert the destination and request there, so allowed calls can prove configuration selection without relying on a closed port or reaching another service.
