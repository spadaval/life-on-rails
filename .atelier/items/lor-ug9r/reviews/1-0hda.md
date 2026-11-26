---
schema: atelier.review
schema_version: 3
round: 1
commit: 3dcd664ccb2a68c19d4fcc6abbf5c634bad89ba4
reviewers:
- thread:delegated-task:command%3Amcp%3A62208c7c-61b9-41e3-9908-1b3b35ef8e61%3Adelegate-task%3A29716a72-4012-4214-b033-b172f78549b0
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T00:46:46.008445349Z
---

## Summary

Complexity: low. The change adds one DayArc slot, moves picker state within Today, and updates one verification flow and its map.

Value: high. Starting a timer by clicking its intent is a core Today action, and the original interception is reproducible.

Risk of merging: medium. The change affects visible timer controls at several widths, but a failure is apparent and the code is easy to revert.

Reviewed `git diff main...3dcd664ccb2a68c19d4fcc6abbf5c634bad89ba4`, DayArc, Today, IntentPicker, the updated flow and feature map, and the product spec. `pnpm typecheck` passed. `pnpm test` passed all 220 tests. Built the throwaway copy with verify-lor and ran `screen today /tmp/lor-verify/lor-ug9r-review`: `ok: true`, no browser errors. That exercises the three Done when lines at 1500×950 and 412×915, including clicking Ship Life on Rails and selecting Health admin with Enter. The feature map correctly removes the old Enter workaround.

Additional headless Chrome checks opened the picker at 1500, 1100, 800, 640, and 412 px, with empty and `ship` searches. Screenshots and `elementFromPoint` measurements are in `/tmp/lor-verify/lor-ug9r-review/extra-*.png` and `extra.json`. They expose the new input overlap in finding 1. The basic fix is worth keeping, but these regressions should be fixed before merge.

Further live checks of recent-intent starts and a taller result list could not finish reliably: other threads replaced the shared :3857 server and reset the test database during this review. I did not count those interrupted checks as product failures or successful validation. Finding 2 follows directly from the changed start handlers and state ownership; it has not been independently exercised in the browser. Filed the coordination problem separately as lor-j0qy. No production service or phone was changed.

## Finding 1: Keep the arc's middle from covering the new picker inputs
Recommendation: fix now

In `src/components/DayArc.tsx:177-178`, `below` is now part of the same relative section that supplies the containing block for `sm:absolute sm:top-[34%]`. Opening the picker therefore increases the section height and moves the middle down toward it. At narrower desktop widths the middle is also taller than the remaining SVG space. Normal flow protects the picker from the following lanes, but it does not protect it from this absolute sibling, which paints over its inputs.

Reproduced immediately after the passing today flow with its existing Ship Life on Rails and Health admin recent chips: at 1100×950, open Something else and search `ship`. The recent chips and Close button overlap the optional title input; a hit test at the title input's center returns the Health admin button. At 800×950 the waking-day sentence covers the title input and the recent chips cover the search input; the search input's center likewise hits Health admin. The screenshots `extra-1100-ship.png` and `extra-800-ship.png` show the collisions, and `extra.json` records the targets. At 412 px normal flow keeps them apart.

Keep the middle's positioning independent of the expanding picker and reserve enough space for the middle at every applicable breakpoint. Check the title and search inputs as well as the result button at an intermediate width such as 1100 or 800 px; otherwise the current `onTop(pickShip)` assertion misses this regression.

## Finding 2: Preserve the title and picker cleanup when starting a recent intent
Recommendation: fix now

In `src/routes/index.tsx:245-250`, the recent-intent buttons now start the timer with `title: null` and never reset `picking`. Before this change they called the same `start(r.id)` as the picker, which used the optional title and then closed the picker. Moving the title into TimerPicker and replacing that handler changes both behaviors.

With a recent intent available, open Something else, enter an optional title, and choose the recent chip in the arc's middle. The new handler drops the title. It also leaves Today's `picking` state true: `!t.running` temporarily hides TimerPicker while the timer runs, but after Stop it remounts immediately with Close still shown and the draft erased. Neither behavior occurs when selecting the identical intent inside TimerPicker because that path uses the title and calls `onStarted`.

Keep the existing title behavior and close/reset the picker for either start path. Include the outer recent-intent chip in the regression check, since the updated flow currently starts only through TimerPicker.
