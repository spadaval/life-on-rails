---
schema: atelier.review
schema_version: 3
round: 1
commit: 4664d1088d4bf804b98c58478c9502af6203c721
reviewers:
- thread:delegated-task:command%3Amcp%3A9ee2cc10-d87f-4b7c-8871-7ec80d7374d7%3Adelegate-task%3A49a7f9c8-4d9b-47fd-b0c6-1f1a4641ada8
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T00:24:52.902384816Z
---

## Summary

Complexity: low. This is a small rearrangement of Today, one local disclosure state, and a status read that reuses the existing measures.

Value: high. Seeing Needs you without scrolling directly serves Today's purpose.

Risk of merging it: low. The changes affect visible UI and are easy to revert; they introduce no data writes or schema changes.

The value is worth the change's size. I reviewed the complete diff against main at 4664d1088d4bf804b98c58478c9502af6203c721, the relevant product rules, and each Done when line. The section order, conditional timer chip, agent-task links, and transfer of the existing percentages to /status agree with the requested behavior. I inspected the Report's 1500 by 950 running screenshot, which shows Needs you and its first item without scrolling.

I independently ran pnpm typecheck, pnpm test (25 files, 212 tests), verify-lor screen today, and node scripts/shots.mjs --only Today --base http://127.0.0.1:3857. All passed; the catalogue produced 30 images without problems. The Today flow exercised mouse and keyboard disclosure, persistence across Pause's data refresh, both timer placements, the block panel's order, and the status descriptions. The changed source files match the built verification copy. Evidence is under /tmp/lor-verify/t3-e2e303f2-1331f518926b970a/zuqn-review/ (today.log and shots/).

I also exercised a one-sentence brief at 412px against the test server, restoring the original test page afterward. That check exposed Finding 1. I did not exercise the real service or the phone app, which this item does not change.

## Finding 1: Keep a truncated one-sentence brief expandable
Recommendation: fix now

In src/routes/index.tsx:395-402, the lead is truncated while closed, but the only button that can open it is rendered only when splitBrief returns a nonempty rest. A one-sentence brief therefore loses readable content on narrow screens, with no mouse/touch or keyboard control to reveal it. The title attribute requires hover and does not provide a keyboard or touch disclosure.

I set the test brief to "Finish the spec before the design review at 14:00." and opened Today at 412 by 915. The lead's scrollWidth was 342px inside a 237px clientWidth, with white-space: nowrap and zero buttons or links in the brief. The screenshot shows "Finish the spec before the desig...", hiding the deadline. Evidence: /tmp/lor-verify/t3-e2e303f2-1331f518926b970a/zuqn-review/single-sentence-412.png and single-sentence.json. This content was fully readable before the change.

Make the lead expandable even when there are no later sentences, and cover that case with the disclosure checks. Keep the one-line collapsed layout, while allowing touch and keyboard users to read the complete first sentence.
