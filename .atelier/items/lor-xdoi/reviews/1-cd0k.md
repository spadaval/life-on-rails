---
schema: atelier.review
schema_version: 3
round: 1
commit: aabec97ef7a4aaafdd72f226dafde22f1c47d95d
reviewers:
- thread:delegated-task:command%3Amcp%3A9355adb5-fe8e-4a8f-bf7c-ba1c83170f37%3Adelegate-task%3A102cc47e-cf12-4ca7-8cd6-319e8911affd
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-17T20:21:35.846759186Z
---

## Summary

Complexity: high. This moves the launcher across a multiplatform boundary and introduces a 521-line JSON compatibility reader, platform adapters, and fixture state.

Value: high. Sharing the actual home rendering and gestures is necessary for the browser lab and editor.

Risk of merging: medium. Parser regressions reach the phone's persisted layout; the other findings break reproducible lab states and editing, but the changes can be reverted.

The value warrants the extraction. I found no substantially smaller change that delivers the same outcome. Changes are needed for the five regressions below.

I reviewed `main...aabec97ef7a4aaafdd72f226dafde22f1c47d95d`, comparing moved files directly with their former app versions, including rendering, gestures, parser, platform wiring, file writes, tests and build scripts. The named files have owners and the platform split largely preserves the Android calls. I checked the Done when lines against the Report and its evidence.

I independently compared all 21 saved before/after PNG pairs: all match below row 110. The saved before/after layout writes are byte-identical. I inspected the saved test XML: before has 433 app tests and 24 homecore host tests; b5 has 131 debug, 131 release, 339 homecore host and 337 Node tests, without skips or failures. All eight submitted home gesture scripts passed again using real CDP touches against the saved b5 production bundle, loaded into Chrome through local file-backed request interception. I also ran focused checks of one-off readiness, wallpaper changes and a UTC browser, and compiled and exercised the submitted standalone JSON reader with the cached Kotlin compiler.

I did not rebuild the Android app or wasm bundle, start an emulator, or repeat the edit-time/memory measurement. The Report acknowledges that the final b5 tree was not rechecked on the Fold and that the Android named-state entry was unchecked. The latter has a concrete code defect below. The saved evidence supports the other reported comparisons; it does not substitute for testing the fixes.

## Finding 1: Preserve leading BOM support when replacing Android's JSON reader
Recommendation: fix now

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/json/Json.kt:251-253` starts at character zero without consuming a leading `\uFEFF`. Android's reader explicitly removes that optional BOM ([Android source](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/+/refs/heads/androidx-constraintlayout-release/android-35/org/json/JSONTokener.java)). In a compiled probe, `JSONTokener("{\"version\":2}").nextValue()` returns an object, while the same input prefixed with `\uFEFF` returns the BOM as a string. Consequently `LayoutFile.parse` rejects a previously readable layout with "the file must be a JSON object". A layout saved with a UTF-8 BOM now puts home into its broken-file state, contrary to the no-phone-behavior-change requirement. Consume the optional initial BOM and add a compatibility case using a complete valid layout.

## Finding 2: Compare both gesture-runner hashes in the same encoding
Recommendation: fix now

`scripts/lab/run.mjs:98` decodes `data-drawn` but compares it to the still-encoded `hash`. `oneOff()` above already replaces JSON quotes with `%22`. I loaded `#current/{%22base%22:%22demo%22,%22where%22:{%22open%22:%22MINIMAP%22}}/cover/dark` in Chrome: the page draws successfully and `data-drawn` equals that hash, but the new wait predicate evaluates false. Every one-off JSON state therefore times out before its script runs. The named `demo` control evaluates true. Normalize both operands, preserving the existing handling of literal percent signs, and verify an actual one-off run as well as a named state.

## Finding 3: Keep lab edits and Undo when the wallpaper changes
Recommendation: fix now

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/lab/LabHome.kt:238` keys the entire `LabPlatform` on `scene.wallpaper`. Changing wallpaper constructs a new platform with the original fixture layout and empty undo history. This contradicts `Main.kt`'s deliberate preservation of the surface across wallpaper changes. Reproduction on b5: run the submitted `folder` gesture; anchors include `folder:Work` and the newly made `folder:Folder`. Change the hash from `/dark` to `/pale`; only `folder:Work` remains. The bar still says "Folder made." and offers Undo, but the edit and its undo history have disappeared. Keep the editing and panel state alive while updating the wallpaper ink separately.

## Finding 4: Use the fixture's time zone for quick dial's header and ranking
Recommendation: fix now

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/home/DrawerScreen.kt:149-151` reads `TimeZone.currentSystemDefault()` inside `build`, bypassing `HomePlatform.zone`. The lab supplies its fixed New York zone and builds its quick-dial counts in that zone, so reading the browser's zone here mixes two hour slots. In a Chrome context with `timezoneId: "UTC"`, the unchanged demo fixture (15:00 New York) draws "Tuesday evening" instead of "Tuesday afternoon" and ranks against the evening slot. Screenshot: `/home/supadava/.cache/lor-xdoi-json-review-eiffr6us/drawer-utc.png`. Pass the platform zone into the builder and include it in the relevant remembered inputs, so the same fixture draws the same quick dial regardless of the viewer's zone.

## Finding 5: Enable named-state requests in the Android fixture process too
Recommendation: fix now

`android/homecore/src/commonMain/kotlin/com/spadaval/lor/home/AppMenu.kt:199` and `DrawerScreen.kt:344` gate the menu and scroll requests on `LabAnchors.on`. The only assignment of `true` is in `wasmJsMain/.../lab/Main.kt:92`; Android's debug `LabStateActivity` never sets it. That activity now also calls `LabHome`, which sets these requests through `applyWhere`. Thus Android's named `app-menu` state cannot open its menu, and `drawer-all-scrolled` cannot apply its requested 600 dp scroll. This follows directly from the two call paths and the sole flag assignment; it was not verified on a device. Enable the fixture behavior in the isolated Android lab process, or give fixture requests their own explicit enablement, while leaving ordinary phone home unchanged.
