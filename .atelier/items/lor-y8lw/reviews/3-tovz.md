---
schema: atelier.review
schema_version: 3
round: 3
commit: 005dd95ca730ca1937a9725d83544045da006892
reviewers:
- thread:delegated-task:command%3Amcp%3Abe6422f9-64e1-4e23-bc27-e1a11d5d652e%3Adelegate-task%3A6977523e-64c4-4c30-897a-d33f3aacaa4a
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-26T07:27:26.411411301Z
---

## Summary

- Complexity: low. The follow-up normalizes two port comparisons and extends the existing subprocess tests; it adds no runtime component.
- Value: high. It closes a reproduced way for test commands to reach the owner's configured production listeners.
- Risk of merging: high. A missed production destination could silently admit writes to personal data, so refusal must happen before any request or browser startup.

The value warrants this small change. The carried finding is fixed, and I found no new problems requiring action.

I reviewed the changes from `09b9c6b6afa8b7123414ab2e4ac6677d5b007f52` to `005dd95ca730ca1937a9725d83544045da006892`, checked the whole change against the Done when lines and Report, and compared both guards with the server's numeric listener configuration. `pnpm test` passed all 230 tests in 28 files, including the no-network CLI and UI refusal cases; `pnpm typecheck` passed. Both commands printed the existing warning about the worktree's symlinked node_modules before running successfully. The non-Git-fixture test printed its expected Git diagnostic.

Additional probes used a temporary fake env file with `LOR_PORT=0443` and `LOR_SERVE_PORT=04000`, intercepted fetch, and blocked browser startup. The CLI refused HTTPS URLs with explicit and omitted port 443 and a named `04000` port with exit 6. UI-check refused the corresponding HTTPS URLs with exit 3. No fetch or browser startup occurred.

I inspected `/tmp/lor-verify/y8lw-r2/tasks.log`, which contains the reported 24 passing assertions and no browser-error report, and checked that the verify wrapper supplies the test base and the flow supplies its CLI port. I did not rebuild or rerun the browser flow, contact lor-web, or change either database. This round changes only the port comparisons and their regression cases; the existing browser-flow evidence remains applicable.

## Finding 1: Compare numeric listener ports and account for URL defaults
Recommendation: fix now
Was: 2-07n5.4
Response: fixed in 259bc81c
Now: fixed

Both guards convert the configured listener ports with `Number` and compare them with the effective URL port, using 80 for HTTP and 443 for HTTPS when URL parsing omits the default. The regression cases now cover the configured `04000` port, explicit port 80 and a URL without a port for both scripts. The tests and the independent HTTPS probes confirm refusal before any request or browser startup.
