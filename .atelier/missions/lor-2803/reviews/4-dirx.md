---
schema: atelier.review
schema_version: 3
round: 4
commit: 7c474d4ff66ff9dacfd341473ad170b363471884
reviewers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3Ae0974f41-1a38-425a-b549-e2b2581b897f
model: gpt-6-astra
verdict: approve
created_at: 2026-11-17T06:30:31.053708991Z
---

## Summary

Complexity: high. The mission introduces a browser target, extracts production Compose screens, and coordinates fixtures, gesture tools, publishing and two concept viewers across several items.

Value: high. The save-and-see loop and usable alternatives directly serve the requested design iteration, including states the audit could not reach.

Risk of merging it: medium. This commit is a reversible plan, but the work it authorizes moves production drawing and adds static serving beside the owner dashboard. The plan assigns unchanged-behaviour checks, test-only fixtures and listener checks to those changes.

The value justifies the scope. A smaller mockup-only effort would not provide the requested iteration on the app's own Compose code. The empty-module timing check, repeated measurements after extraction, and escalation of a slow loop give the lead a concrete point to reconsider before building the concepts.

Reviewed the mission and all thirteen items at 7c474d4, the related lor-3hnk, lor-sghw, lor-xdoi, lor-dkut and lor-3ilp items, the project spec, the audit's surface inventories, and the plan diff against main. Traced the prerequisites from demo data and the compiler upgrade through the lab, fixtures, moved screens, concepts, critique and acceptance. Checked each of the six carried findings against the revised item text; lor-ncpw/mission.md has no diff from main and retains its ready status and ready record.

All six carried findings are fixed. No new fix-now finding. The URL defaults now have explicit semantics; comparison content has its own path; the concept catalogue is in scope; and the prerequisite mission remains agreed. The future concept items are deliberately created by the lead under the mission's authority, with three to five concepts still required by acceptance.

This is a plan review, not execution evidence. I did not run builds, browser or device checks, measure the reload times, or validate the future CSP implementation. Those remain the named implementation and acceptance items' work.

## Finding 1: The lab's URL carries version and state only, but acceptance states and concept shots name a screen size and a wallpaper
Recommendation: fix now
Was: 3-d0t0.13
Now: fixed
lor-vbfd's second Done when now specifies version/state/screen/wallpaper in the hash, including cover/dark defaults. A link can express both acceptance's inner screen and pale wallpaper and the concept shot variants.

## Finding 2: `/lab/index.html` is written by both lor-zwfd's publish.sh and lor-glev's comparison page
Recommendation: fix now
Was: 3-d0t0.14
Now: fixed
lor-zwfd makes publish.sh the only writer of /lab/index.html and preserves separately owned folders. lor-glev now owns docs/design/lab/compare/ and /lab/compare/index.html, linked first from the generated index.

## Finding 3: lor-5fk6's Scope omits `shots/lab.json`, which its Done when edits
Recommendation: fix now
Was: 3-d0t0.15
Now: fixed
lor-5fk6's Scope explicitly includes shots/lab.json beside the concept sources.

## Finding 4: Merging this plan sets lor-ncpw back to open, so the lab's first items cannot start
Recommendation: fix now
Was: 3-js07.13
Now: fixed
lor-ncpw/mission.md matches main, including status: ready and its ready block. The separate lor-3ilp follow-up does not unagree the prerequisite mission.

## Finding 5: A lab link cannot open q165's cover, inner and wallpaper states
Recommendation: fix now
Was: 3-js07.14
Now: fixed
Fixed by the same explicit screen and wallpaper URL fields in lor-vbfd as Finding 1. lor-oech's loader consumes that lab hash; the frame fields need not also be duplicated into every fixture.

## Finding 6: lor-5fk6's Done when edits shots/lab.json; its Scope does not name that file
Recommendation: fix now
Was: 3-js07.15
Now: fixed
lor-5fk6's Scope now names shots/lab.json, resolving this duplicate of Finding 3.
