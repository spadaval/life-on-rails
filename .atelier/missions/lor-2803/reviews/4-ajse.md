---
schema: atelier.review
schema_version: 3
round: 4
commit: 7c474d4ff66ff9dacfd341473ad170b363471884
reviewers:
- 01a1e832-f5ef-7cf1-aecc-9e88f00962a6
model: grok-4.6
verdict: changes
created_at: 2026-11-17T06:30:37.062429760Z
---

## Summary

Complexity: high. The plan still adds a design-lab subsystem, pulls the homecore split forward of ncpw's sync, then two viewers and three to five concepts; several owners, and a contract ncpw's items must keep.
Value: high. Sumeet named roughness as the biggest issue and asked for rapid-development tools plus 3–5 usable concepts with a dashboard prototype and an in-browser phone mockup.
Risk of merging it: medium. Agreeing the plan authorizes a Kotlin upgrade and extracting the running launcher into a shared module; a break shows on the emulator receipts, and the real phone is out of scope.

The outcome is still worth the size. I am not recording Shrink or Reject. Round 4 closed the six carried findings. One hole remains: the comparison page has a path, but no item both owns and copies it into the served `/lab/` tree, so a republish can still leave Sumeet with no page to pick from.

Checked commit `7c474d4` as it stands: the mission, all thirteen of its items, lor-xdoi, lor-dkut, lor-3hnk, lor-sghw, lor-3ilp, mission lor-ncpw (`mission.md` and `design.md`), `.atelier/missions/lor-2803/audit/` (README plus the three inventories), and the current code those items name (`useNow` / `useTick`, `people.tsx` `filter=overdue`, `notes.tsx` `version` for history, `home/Setup.kt` for the set-up card). Compared each item's `blocked_by` to its Done when, and each carried finding to the round-4 text. `atelier list --mission lor-ncpw` on this checkout reads ncpw as agreed, with lor-3hnk and lor-sghw able to start.

Could not check: whether the 15-second wasm loop holds under the lor-0v68 cap (lor-vbfd already takes a miss to Sumeet); a live `/lab/` (it does not exist yet); the two claude.ai artifact links (only Sumeet can open them; the inventories are in-repo). Did not read another reviewer's file. Left 3-js07.16 (ncpw's `design.md` still has the split wait for lor-dkut) with lor-3ilp; it was already deferred and is not needed before this mission starts once ncpw stays agreed.

## Finding 1: The lab's URL carries version and state only, but acceptance states and concept shots name a screen size and a wallpaper
Recommendation: fix now
Was: 3-d0t0.13
Now: fixed

lor-vbfd line 31 now puts version, state, screen and wallpaper in the hash (`#current/home-full/inner/pale`; screen is `cover` or `inner`, last two optional for cover on dark), and says a catalogue entry is only a name and a link. lor-q165's cover/pale and inner links, and lor-5fk6's three wallpaper shots, can be those links.

## Finding 2: `/lab/index.html` is written by both lor-zwfd's publish.sh and lor-glev's comparison page
Recommendation: fix now
Was: 3-d0t0.14
Now: fixed

lor-zwfd line 29 makes `publish.sh` the only writer of `/lab/index.html`. lor-glev line 33 moves the comparison to `/lab/compare/index.html` and drops the lab's index page from its Scope. One file, one owner. A leftover about how compare reaches the served tree is Finding 7.

## Finding 3: lor-5fk6's Scope omits `shots/lab.json`, which its Done when edits
Recommendation: fix now
Was: 3-d0t0.15
Now: fixed

lor-5fk6's Scope (line 40) now names `shots/lab.json` beside the concepts directory.

## Finding 4: Merging this plan sets lor-ncpw back to open, so the lab's first items cannot start
Recommendation: fix now
Was: 3-js07.13
Now: fixed

`.atelier/missions/lor-ncpw/mission.md` is `status: ready` with its `ready:` block restored. `atelier list --mission lor-ncpw` on this checkout reads the mission as agreed, and lor-3hnk and lor-sghw as ready to start. lor-vbfd can wait on them after merge.

## Finding 5: A lab link cannot open q165's cover, inner and wallpaper states
Recommendation: fix now
Was: 3-js07.14
Now: fixed

Same hash as Finding 1: lor-vbfd line 31 addresses screen and wallpaper in the link, so q165's "full home on the cover screen over a pale wallpaper" and "thread open on the inner screen" are URLs, not leftover page UI. Screen size and wallpaper stay lab chrome, so they do not need to live in lor-oech's fixture.

## Finding 6: lor-5fk6's Done when edits shots/lab.json; its Scope does not name that file
Recommendation: fix now
Was: 3-js07.15
Now: fixed

Same as Finding 3: lor-5fk6 line 40 names `shots/lab.json`.

## Finding 7: Nothing both owns and copies the comparison page into the served `/lab/` tree
Recommendation: fix now

lor-glev line 33 requires `/lab/compare/index.html`, "kept in `docs/design/lab/compare/` and copied by `scripts/lab/publish.sh`". Its Scope (line 39) is only `docs/design/lab/critiques/` and `docs/design/lab/compare/`: it cannot edit `publish.sh` or write into `LOR_LAB_DIR`.

lor-zwfd line 29 lists what `publish.sh` copies: shot galleries, the launcher lab's bundle, the dashboard prototype's build. Compare is not in that list. The same line then treats `/lab/compare/` as "a folder it did not make" that is "left as it is and linked first when present".

Those two stories disagree. glev says the script copies compare from docs; zwfd says the script never makes that folder and must not touch it. After glev lands, a clean publish therefore either never creates `/lab/compare/` (zwfd's copy list) or would have to edit a script glev does not own. A later concept publish (lor-5fk6 line 36, lor-5uxk line 31) re-runs the same script; "left as it is" only helps if compare was already in `LOR_LAB_DIR`.

Name one path in both items: add `docs/design/lab/compare/` to zwfd's copy list (copy when present, then link first from the index), or give glev `scripts/lab/publish.sh` in Scope so it can add that copy. The index stays zwfd's file.
