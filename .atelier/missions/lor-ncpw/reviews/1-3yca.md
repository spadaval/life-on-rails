---
schema: atelier.review
schema_version: 3
round: 1
commit: dc0babead1a56537b6dfb7e03f5db29e71878dca
reviewers:
- thread:delegated-task:command%3Amcp%3A336528d8-568a-46be-80e7-fa243e638044%3Adelegate-task%3A91e1d29d-5da7-4657-9b79-ac9af66a9339
model: gpt-6-astra
verdict: changes
created_at: 2026-11-17T06:09:24.633038077Z
---

## Summary

Reviewed the plan at dc0babead1a56537b6dfb7e03f5db29e71878dca: the mission, design and notes, all nine mission items, and the new lab prerequisites lor-vbfd, lor-oech and lor-293p and their parent mission lor-2803. Compared the split's platform boundary with GridHost.kt, HomeScreen.kt and AppMenu.kt and read the product/engineering spec. This is a plan review; I did not run builds, emulator checks or pixel comparisons, and have not verified the proposed performance or fidelity targets.

Complexity: high. The change extracts the launcher's renderer, parser and editing state across Android and wasm, adds version sync and uploaded assets, and now coordinates two missions.

Value: high. It directly provides the requested desktop layout editor and makes the real launcher available for browser design iteration.

Risk of merging the planned implementation: high. Parser or sync mistakes could silently replace the owner's home layout; shared input changes also affect the daily launcher. Version history, base checks and before/after validation address these risks.

The value warrants the work. The staged history, view and editor deliveries are useful independently, and the requested shared drawing rules out a cheap independent web mockup as an equivalent substitute. One platform-boundary contradiction needs resolving before work starts.

## Finding 1: Give browser paging and app menus an implementation owner in the split
Recommendation: fix now

`.atelier/items/lor-xdoi/item.md:30` explicitly keeps `GridHost.kt`'s `gridFinger` and `AppMenu.kt`'s menu drawing in the Android app. Its newly added lab requirement at line 44 nevertheless requires scripts exercising real paging, an app menu, and a long press drifting into a drag immediately after this split. The platform interface at line 31 does not assign browser implementations of those excluded behaviors.

These are functional implementations, not just Android service hooks. `android/app/src/main/java/com/spadaval/lor/home/GridHost.kt:371` contains swipe-axis selection, scroll arbitration, velocity tracking and settling in `gridFinger`; `HomeScreen.kt:692` attaches it to the home root. `AppMenu.kt:192` opens the menu on long press, and the menu below it contains the layout-edit actions as well as platform actions. Merely moving GridEdit and injecting icons/shortcuts does not make those interactions available to wasm. lor-293p supplies input events and proves its runner on a synthetic test surface; it deliberately assigns real home scripts back to lor-xdoi, so it does not fill the gap.

Amend lor-xdoi's split boundary to explicitly own these browser interactions: share their platform-independent gesture and menu behavior, keeping Android services behind adapters, or identify and scope browser adapters that preserve the same behavior. Make the required real-home gesture scripts part of its Done when criteria. This lets the implementer satisfy both the file boundary and the browser acceptance requirements without inventing a second interaction implementation or discovering the missing scope after the split.
