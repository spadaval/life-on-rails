---
schema: atelier.review
schema_version: 3
round: 2
commit: 5038be6b5512b6ccaae2eccca31bd49796267eed
reviewers:
- thread:delegated-task:command%3Amcp%3A477d945f-eee4-498f-b362-eba4c5ea883f%3Adelegate-task%3A23d51c4b-15c2-498c-8569-d1aa3ccecb34
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T16:02:49.937939009Z
---

## Summary

Complexity: high. This adds a shared Android/wasm module, platform font initialization, a browser entry point and a constrained continuous-build runner.

Value: high. Running the app's actual Compose code in a browser is needed for the launcher's design loop.

Risk of merging: medium. The extraction changes the app's dependencies and font resources, but failures are visible and the changes are reversible; the supplied before/after evidence supports unchanged Android rendering.

The value warrants the implementation. A smaller imitation would lose the shared-code outcome, and I found no warranted shrink or rejection.

I reviewed the whole `main...5038be6b5512b6ccaae2eccca31bd49796267eed` diff against every Done when line and the Report, and separately inspected the changes since round 1. This covered the common registry and fixtures, hash parsing, kit sheet, platform fonts, unchanged press implementation, Gradle targets and repositories, both build scripts, and the catalogue's integration with the existing screenshot runner and lab publisher. The catalogue produces a sibling gallery in the same screenshot collection, which the publisher already includes.

I ran `android/web-dev.sh`. Its wasm build passed in 19 seconds; webpack bound to loopback at port 3860. I checked the scope's MemoryMax=4294967296 and MemorySwapMax=536870912; peak usage was 2606161920 bytes with no swap. A second invocation was refused, and I stopped the scope after checking. Both scripts passed `bash -n`, and the code diff passed `git diff --check`. All nine tests in `tests/shots.test.ts` passed. `node scripts/shots.mjs --catalogue shots/lab.json --out /tmp/lor-vbfd-review-round2/shots` produced four images in three seconds with no problems.

In headless Chrome I exercised the empty hash, cover/inner switching, state and wallpaper selection, browser Back, an equivalent explicit-default hash, and unknown-version feedback. The frame and canvas dimensions agreed at 480x1120 and 875x971. There were no page errors or requests outside the lab's own folder, and `/local.properties` returned 404. I inspected screenshots of the alpha samples and the scrolled type, panel, selection, press and motion sections. The earlier deferred draw-marker issue is also fixed: `#current/home-full`, `#current/home-full/cover/dark` and `#concept-x/home-full` each set `data-drawn` to the raw hash being displayed. My browser results, screenshots, pixel comparisons and gallery are in `/tmp/lor-vbfd-review-round2/`.

I inspected the supplied before/after boat logs: both end in BUILD SUCCESSFUL, and the after log includes homecore's host tests and wasm distribution. All 119 AndroidX coordinates in the supplied resolved-dependency lists retain their versions. Independently decoding the supplied Settings and Timer PNG pairs found exactly 235 changed pixels in each, confined to x=155..173 and y=50..77 in the status-bar clock. The fonts are unchanged binary moves, and Android's font construction retains the same weights and variation settings.

I did not rerun Android builds, host tests or emulator installs. The supplied evidence has build logs but no test-result XML, so I could not independently verify the executed 422-before/422-after app test counts or eight homecore tests. I inspected the performance probe and recorded build/reload logs but did not repeat its source edits or the reported 11.2/11.4-second edit-to-draw measurements. I also did not repeat fresh-cache tool downloads or static-distribution CSP testing. These limitations leave no supported open blocker.

## Finding 1: Show the translucent swatches over the selected wallpaper
Recommendation: fix now
Was: 1-7fl5.1
Response: fixed in b5e1cd7: the five alpha samples sit straight on the wallpaper, with only their labels on a panel. `/tmp/lor-verify/lor-vbfd/round2/glazes-{dark,pale,mixed}.png` show them change with the wallpaper.
Now: fixed

`KitSheet.kt` now draws `Glazes()` outside the opaque Colours section. Only the labels have PanelFace backing; the five sample boxes composite directly over the wallpaper. I verified all five at fixed screenshot coordinates on dark, pale and mixed backgrounds. For example, the scrim sample at (428,740) changes from RGB (8,5,11) on dark to (94,92,88) on pale and (100,98,91) on mixed. Every other alpha sample also changes. See `/tmp/lor-vbfd-review-round2/cover-{dark,pale,mixed}.png` and `pixels.json`.
