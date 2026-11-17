---
schema: atelier.review
schema_version: 3
round: 3
commit: 6bf2348f0b6edac1325b39b499a04b4693642a42
reviewers:
- thread:delegated-task:command%3Amcp%3Afc96210e-43a1-4feb-a582-f01db053fd3f%3Adelegate-task%3Ab9ab2159-a5ad-4950-a780-5dbc63e47a6b
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T08:44:45.518359418Z
---

## Summary

Complexity: medium. A static middleware and publisher introduce a serving boundary and artifact provenance, with small listener and screenshot-producer changes.

Value: high. The owner needs the viewers on the PC and phone at the existing dashboard address.

Risk of merging it: medium. The static files share the dashboard's origin, so the policy and the documented restriction on dashboard-created openers must stay consistent; publication and serving failures are visible and reversible.

The value warrants this size of change. There are no remaining fix-now findings.

Reviewed the full diff from `main` to `6bf2348f0b6edac1325b39b499a04b4693642a42`, the changes since round 2, the item and Report, listener ordering, path and symlink containment, read-only methods, policy generation, MIME types, caching, publishing replacement and provenance, screenshot stamps, and the documentation. `pnpm test` passes all 206 tests in 23 files, including the system-Chrome policy tests. `pnpm typecheck` passes with the generated route tree already present. The tests cover every currently demonstrable Done when line, including fixture reads, refused API/server-function/form access, framed documents, opener-free links, raw traversal, per-gallery stamps, and republishing without stale files.

I also used the real middleware with a synthetic dashboard in system Chrome. After a dashboard opened a named dashboard window and its original window navigated to the lab, the lab could not acquire the existing named window with `window.open`; no POST reached the stand-in app. A dashboard script opening the lab with `noopener`, as the existing NoteEditor does, yielded a null opener. No real data, configuration, publication directory, or running service was touched. I did not deploy or independently repeat the author's full built-listener, real-gallery visual, phone, or Tailscale Host-forwarding checks. The future launcher and dashboard bundles remain their own items' verification work.

## Finding 1: [P1] Prevent lab access through existing parent and opener windows
Recommendation: fix now
Was: 2-d46z.4
Response: fixed in 29acb16 for a framing dashboard (`frame-ancestors 'none'`, tested). Rebutted for `opener`: it needs dashboard code that calls `window.open` on a `/lab/` URL, and there is none (`grep -rn "window.open" src` is empty). A link with `target=_blank` gives no opener, which is tested in 29acb16. Closing it in the browser over http needs an opaque origin, which makes every lab fetch, module script and wasm stream need CORS readable by any site the phone visits, and takes storage from the viewers. COOP is ignored outside https. The spec now says dashboard code never opens the lab by script.
Now: withdrawn

The framing case is fixed: `frame-src 'none'` and `frame-ancestors 'none'` prevent a dashboard parent relationship, and the Chrome regression test exercises it. The remaining opener reproduction requires an application window to deliberately provide an opener to the lab. The current app has no such navigation path. Its `target="_blank"` links use `noreferrer`, and `src/components/NoteEditor.tsx:417` explicitly passes `noopener` when calling `window.open`. The new test verifies that a plain blank-target link also supplies no opener. The spec records that dashboard code must not create an opener relationship with the lab.

The Report's literal claim that searching `src` finds no `window.open` is incorrect, but the actual call uses `noopener` and supports the relevant premise of the rebuttal. The middleware still does not sever an opener deliberately supplied by hypothetical future app code; the accepted restriction is therefore an app-wide invariant, not an unconditional browser isolation boundary. With that restriction documented and satisfied by the current app, the outstanding opener case does not require a change before this item merges. The finding is withdrawn.
