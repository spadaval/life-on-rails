---
schema: atelier.review
schema_version: 3
round: 1
commit: 0f39bd3e15d6f35c6cbf6c3c4568bb5b089fea3f
reviewers:
- thread:delegated-task:command%3Amcp%3A37e14bde-fb3b-4826-acbc-f5c79fe37580%3Adelegate-task%3Adb9d68ae-cad8-48b9-b9b6-de0a2dfcbce7
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-26T00:13:20.937900654Z
---

## Summary

Complexity: medium. The change is one visual system, but its shared CSS and class migrations reach every dashboard route and the note editor.

Value: high. A consistent panel, button, type and control vocabulary is the foundation the page items need.

Risk of merging: medium. Shared styles reach the whole dashboard, but failures are visible and this change is easy to revert; no data writes or server behavior change.

Reviewed `main...0f39bd3e15d6f35c6cbf6c3c4568bb5b089fea3f`, including all changed components/routes, styles, DESIGN.md and the two new source checks. The scope is appropriate: there is no new palette, typeface, content or ordering. Plain panels are the default, selected editors opt into the frame, and the classes and documented shapes agree. The four-size mapping and both the base and resulting counts in the Report match independent source counts. All 81 gallery images exist and have their named widths.

Inspected the saved 1500px views for Today with a running timer, Tasks with an editor, the habit editor, and the person's hub. The frame appears only on the open editor where present; the habit's selected period and the block time fields are readable. Compared saved Today with a selected block and Tasks at 412px against `previous/`: the wrapped gap chips stay inside the page and task titles gain room from capped parent chips. Increased control/text heights add some vertical space, but I found no new lost content or horizontal overflow in those comparisons.

Independently ran `node_modules/.bin/tsc --noEmit` and `node_modules/.bin/vitest run`: typecheck passed and all 208 tests in 24 files passed. `verify-lor up` built the reviewed sources successfully outside the checkout. After the shared :3857 listener was replaced by another checkout, I served this built copy on :3861 on `life_on_rails_test` with jobs and external listeners disabled. `verify-lor screen /tasks --port 3861` passed with no browser errors; its screenshot/log are in `/tmp/lor-verify/lor-mxm9-review/built-tasks/`. Independent DOM checks confirmed 15px body/control text, the themed person cadence selector, and 412px document width on Today and Tasks. The :3861 listener is stopped.

I could not repeat the full demo-state interactive flows reliably because other threads were resetting the shared test data. The dense state and before/after comparisons above therefore use the saved gallery, rather than claiming a fresh full flow. The first live screenshot from :3857 is excluded as evidence: it came from another checkout despite `screen` reporting success. Filed and committed `lor-3ydk` to make verification reject that mismatch; it is a pre-existing tooling problem and does not block this visual change.

The value warrants this scope. No open fix-now finding.

## Finding 1: Small primary buttons intentionally use an outlined treatment
Recommendation: no action

`src/styles.css:101-106` gives `.btn-primary.btn-sm` an amber outline rather than the full-size primary fill. This is a documented departure from the literal "primary (filled)" wording. That same acceptance line also asks for Done/Did it to be primary in each row while allowing at most one primary per panel, which cannot both hold for multi-row panels. The chosen treatment keeps the main row action visibly stronger than Seen/Close, preserves the filled editor/toolbar action, and is consistently documented in DESIGN.md and the Report. I accept that resolution; it does not need a fourth named button level or a larger redesign.
