---
schema: atelier.review
schema_version: 3
round: 1
commit: ca989d1108948a126daefbb2ff40464a03ab413b
reviewers:
- thread:delegated-task:command%3Amcp%3A8fc5f5c5-b8f3-43a5-a270-7a549eedb6c5%3Adelegate-task%3Ae8aa877f-9524-4c72-8b04-5212809a4b50
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T08:13:48.194559511Z
---

## Summary

Complexity: medium. This adds a catalogue runner and gallery plus URL state across six routes, while reusing the existing test clock and demo variants.

Value: high. Fast dashboard iteration and reusable screenshot catalogues are required for the design lab.

Risk of merging: low. The tools target the test database, their failures are visible, and the UI additions are reversible. The value warrants the size; I found no substantially smaller change that delivers the requested links and reusable catalogues.

Reviewed `git diff main...ca989d1108948a126daefbb2ff40464a03ab413b`, including the dev-server guards, Vite configuration, route state, screenshot grouping, catalogue, gallery and verify skill documentation. All Done when lines are covered by the implementation and Report. No actionable findings.

Independently checked:

- TypeScript passes. All 189 unit tests and all 144 database tests pass. The earlier intermittent database failure reported by the author did not reproduce; this does not establish its cause.
- Started `scripts/ui-dev.sh` on 127.0.0.1:3858 and loaded the demo set. A complete catalogue run produced 81 PNGs, with no reported problems, in 27.8 seconds. A subsequent quick-add-only run produced three images in 2.2 seconds and retained their previous images.
- Opened representative state links directly: three picked Tasks rows, Portuguese lesson with its editor, Lisbon history with its diff, yesterday's Run block, quick add with typed parent/date, and Maya's hub. Checked their rendered content and inspected the mobile block-panel and person-hub images. The hub includes both messages and mail. The browser reported no page errors. Ordinary Tasks starts without picked rows, and ordinary quick add opens and closes.
- Opened the gallery from a file and exercised Before, confirming that the image source switches to `previous/`. Checked `--list` and the catalogue's coverage of all ten dashboard acceptance names. The generic selector/function wait mechanism is implemented without dashboard-specific capture actions.
- Read the author's verify screen receipts for Tasks, Goals, People and Notes; each reports success without browser errors. Read the boot path to verify that Vite cannot start jobs or the channel and that the production listeners are outside it.

Review screenshots are outside the repository at `/tmp/lor-nde9-review-shots/`. The demo set was restored after the database suite. I did not edit source files to repeat the author's hot-reload timing measurements, and `/lab/shots/` hosting remains conditional on lor-zwfd as the item permits. No Android surface is changed.
