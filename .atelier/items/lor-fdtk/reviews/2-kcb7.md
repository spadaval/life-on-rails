---
schema: atelier.review
schema_version: 3
round: 2
commit: d334f8b2968ffdf8de75989c513b1efd4c785aa7
reviewers:
- thread:delegated-task:command%3Amcp%3A66276f89-0d39-4dbb-a3a6-0a84e6042e28%3Adelegate-task%3Af051b967-078e-47ea-82e4-28fcdd3895c9
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-18T02:04:37.748242917Z
---

## Summary

- **Complexity: high.** About 4,300 added lines introduce a complete concept with its own selection, focus, sheet, drag and fold state across several screens.
- **Value: high.** A usable third direction is required for the design comparison; still images would not test its defining list-and-detail interaction.
- **Risk of merging: low.** The concept is confined to the fixture-backed browser lab; the shared keyboard change also stays in the concepts kit, and both other concepts' search checks pass.

The value warrants the size. The layout and interaction work implement the requested comparison rather than a reusable production subsystem; I found no substantially smaller change that would deliver the required phone experience.

Reviewed `main...d334f8b2968ffdf8de75989c513b1efd4c785aa7` and the changes since round 1 against the product spec, lab brief, each Done when line and the Report. Read every Panes source file, the registration/font additions, shared keyboard change, relevant kit shell/store/sheet code, gesture scripts, screenshot catalogue and publish script. The catalogue adds 98 Panes shots across the three wallpapers; inspected representative supplied larger-text and screen evidence. The existing publish source includes the phone bundle under `/lab/phone/`; the supplied scratch-publish evidence shows it loading there.

Served the saved full boat bundle from `~/.cache/lor-fdtk/boat/lab` on loopback port 3861. The Report identifies it as the 998f11fb build; the only changes between that commit and the reviewed commit are the item's Report. Independently reran all 23 Panes gesture scripts, including the four round-1 regressions, plus Queue and Timeline search: all 25 passed with no page errors. Also selected two question answers, folded and unfolded, and verified that sending retained both choices. `pnpm test`: 23 files, 206 tests passed. Fresh recordings, final frames and run metadata are in `/home/supadava/.cache/lor-fdtk-review2/`. Separate touch probes reproduced Findings 5 and 6 below.

Did not rebuild Gradle, rerun the reported 940 boat tests, recapture all 98 catalogue rows, publish to the real lab or test on a physical phone/emulator. This item changes only a browser concept and lab resources. TypeScript typechecking was not rerun; no TypeScript source changed. The four carried findings are fixed, but the settings dismissal regression below needs correction before merge.

## Finding 1: Put LoR flags alongside T3 cards on the agent page
Recommendation: fix now
Was: 1-i3i0.1
Response: fixed in 998f11fb: the agent page lists LoR's flags after T3's cards with Done and Seen, its count and the switcher's include them, and its calm line needs both sources read; `agent-flags.mjs` checks a flag done there leaves Today too.
Now: fixed

## Finding 2: Back from a settings section closes the whole settings sheet
Recommendation: fix now
Was: 1-i3i0.2
Response: fixed in 998f11fb: on the cover, Back (and the scrim) from a settings section returns to the sections, going through the sheet's own dismissal so it can't be overtaken; `settings-back.mjs` checks it with Back.
Now: fixed

## Finding 3: Search can highlight one app while offering to open another
Recommendation: fix now
Was: 1-i3i0.3
Response: fixed in 998f11fb: the right pane resolves the chosen app against the current results, as the list does; `search-switch.mjs` erases with the drawn ⌫ and checks both panes move to WhatsApp.
Now: fixed

## Finding 4: The touch keyboard's Enter action keeps an obsolete query
Recommendation: fix now
Was: 1-i3i0.4
Response: fixed in 04cdfbb0 and 998f11fb: the cause was the kit keyboard's keys taking focus on touch, so the field lost it and the key never acted; keys now detect taps. The field also keeps the keyboard's Return on its latest callback while focused. `enter.mjs` uses the drawn Return.
Now: fixed


## Finding 5: Settle the settings sheet when a drag returns to the section list
Recommendation: fix now

[Sheets.kt:64](/home/supadava/.t3/worktrees/life-on-rails/t3-acaa9160-48ef7750c893ae79/android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/panes/Sheets.kt:64) now handles every section dismissal by changing `Sheet.Settings(section)` to `Sheet.Settings(null)`. That works for Back and the scrim, but a downward drag has already moved KitSheet's `shown` fraction. KitSheet's `settle` calls `onDismiss()` instead of animating when the target is zero (`kit/Sheet.kt:82`), and its opening animation is keyed only on `open` (`kit/Sheet.kt:67`). Settings remains open, so neither path restores its position.

Reproduced with real touch helpers on `#panes/sync-failed/cover/dark`: tap `home:sync`, then `gesture.drag([[240,92],[240,1000]], 0, {speed:800})`. The section disappears, but the Settings list remains stuck at y=810 on the 1120px screen instead of its resting y=78. Its lower rows are clipped offscreen and the modal scrim remains. A further Back closes it, but the drag itself should finish in a usable resting state. Restore the detent when returning to the list, or distinguish a sheet-dismissal drag from navigating back within Settings. Exercise the drag, since `settings-back.mjs` covers only Back.

Evidence: [touch probe](/home/supadava/.cache/lor-fdtk-review2/probe.mjs), [before](/home/supadava/.cache/lor-fdtk-review2/settings-before-drag.png), [after](/home/supadava/.cache/lor-fdtk-review2/settings-after-drag.png), and [after-drag anchor bounds](/home/supadava/.cache/lor-fdtk-review2/settings-after-drag.json).

## Finding 6: Label cached LoR flags on the agent page as stale
Recommendation: defer

[Agent.kt:79](/home/supadava/.t3/worktrees/life-on-rails/t3-acaa9160-48ef7750c893ae79/android/homecore/src/webMain/kotlin/com/spadaval/lor/concepts/panes/Agent.kt:79) shows sync and T3 notices but never checks `store.homeLoad`. Now that the agent page includes LoR flags, `panes/stale` displays its three cached flags without saying LoR is unreachable or when they were read. Done and Seen are disabled, but the only explanation on the page concerns T3 reconnecting, a separate source. Today and home correctly label the same LoR data as stale.

Reproduced by opening `#panes/stale/cover/dark` and tapping `nav:Agent`; [screenshot](/home/supadava/.cache/lor-fdtk-review2/agent-stale.png), [probe](/home/supadava/.cache/lor-fdtk-review2/agent-stale.mjs). Reuse the LoR freshness notice above these flags. Deferred because home and Today already provide the brief's required unreachable state, writes remain disabled, and this is a fixture-only concept; it does not need to block the comparison once Finding 5 is fixed.
