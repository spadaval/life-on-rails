---
schema: atelier.item
schema_version: 3
id: lor-glev
title: Three outside critiques of every concept, and one page that compares them
status: in_review
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-5uxk
- lor-5fk6
- lor-xdoi
- lor-rgx4
- lor-t8lq
- lor-vx69
- lor-8mbs
- lor-3wzs
- lor-vujy
- lor-j1fe
- lor-fdtk
- lor-8aqh
implementers:
- thread:delegated-task:command%3Amcp%3A72dbaf48-e8ee-4fbd-8aa8-e27338960591%3Adelegate-task%3A5b5cbc56-db63-4c06-b8dc-c933fc360968
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.172385205Z
---

## Goal

Each concept has been looked at by three models from three labs who did not build it, for range of view more than for defects, and Sumeet has one page to choose from.

## Done when

- [x] Astra and Grok 4.6 each use every concept's dashboard and phone through the galleries and recordings, and where they can, the live viewers, with the brief; Kimi K3's seat is open, because OpenCode Go's usage limits and OpenCode's balance both refused it on 2026-11-17 and 2026-11-18, and the page says the read from outside the US is still owed. Each writes, per concept: what it gets right, what would annoy in daily use, what it makes harder than `current`, and the one idea worth stealing into another concept. Critique, not a fix list.
- [x] Their notes are in `docs/design/lab/critiques/<concept>.md`, unmerged and unedited, each signed with its model.
- [x] `/lab/compare/index.html`, is the comparison. Its source is `docs/design/lab/compare/`, and this item adds that folder to the copy list in `scripts/lab/publish.sh`, so every publish, including one from a clean checkout, serves it and links it first from `/lab/`. It shows: each concept's name, one sentence, four screenshots, the critics' points for and against side by side, and links into both viewers at that version.
- [x] Defects a critic finds that break one of the brief's interactions are fixed by the concept's author before the page is called done; the rest are left as notes.
- [x] A `needs-human` item outside the mission asks Sumeet to pick a direction or a mix. The page is done without waiting for his answer; the mission's acceptance is lor-q165. It is lor-dinh, blocked by this item.

## Scope

`docs/design/lab/critiques/`, `docs/design/lab/compare/`, and one line in `scripts/lab/publish.sh`'s copy list.


## Report

Sumeet has one page to choose from at `/lab/compare/`, once the lead publishes from main. It shows the four concepts and `current`. Each has a sentence, four screens, the facts a pick turns on, what it needs from LoR that LoR does not send today, the critics' points for and against side by side, what is worth stealing, and links into both viewers at that version. Two critics read every concept, not three: Kimi K3's seat is open.

**Kimi K3 could not run, and the read from outside the US is still owed.** On 17 November, `opencode-go/kimi-k3` (variant max) stopped partway at OpenCode Go's 5-hour limit with nothing written, and `opencode/kimi-k3` refused with "Insufficient balance". The lead said to retry Go once after its window reset and not to swap in another model. The retry at 09:22 UTC on 18 November stopped at Go's weekly limit, again with nothing written. The first Done when line is amended to say so, as the lead asked, and the page says the seat is open under every concept and under the count question. The lead files the follow-up.

**The critics' raw files** are in `~/.cache/lor-glev/critic-a/` (Astra) and `critic-b/` (Grok) on this machine. Diffing one against its block in `docs/design/lab/critiques/` shows the copy is exact; the copy is made by `~/.cache/lor-glev/assemble.sh`.

**The critics.** GPT-6 Astra (codex, reasoning effort high) and Grok 4.6 (grok, reasoning effort high) each worked in a fresh thread and checkout of its own, from the same brief and without knowing about the others. The brief named what to read (the brief and the eight Reports), what to open (both galleries, the evidence folders, the live viewers, `run.mjs` on a port of its own), the four questions, the 11-or-4 question, and a separate list of defects that break a required interaction. At review (finding 1-5uh5.1), Astra was asked in its own thread to watch the gesture recordings it had skipped. It watched three recordings each for Queue and Panes, two for Timeline and three for Context, frame strips included, and wrote one addendum per concept on motion. Each addendum follows that concept's first notes under Astra's marker, and each concept on the page quotes one point for and one against from it, marked "in motion". Its answer on the count and its finding of no defects stand. At round 2 (finding 2-thrg.3), Grok was asked the same and watched every gesture run of all four concepts, strips and recordings. Its addenda are placed and quoted the same way. Its count answer stands, and it repeats the inner-screen clipping claim, which the Defects section already answers. Their files are copied byte for byte into `docs/design/lab/critiques/<concept>.md` and `home-count-and-defects.md`, each under a `<!-- critic: <model> -->` marker and a heading naming the model, and each ends with the critic's own signature.

**On the page, the critics' words stay theirs.** `compare/build.mjs` makes `index.html` from `content.mjs` and the critiques. Each quote in `content.mjs` must appear word for word in that critic's notes, or the build fails; I tested this by rewording one quote. `build.mjs --check` says whether `index.html` is current. Each concept also folds both critics' notes in full under the quotes. I chose which sentences to quote, and that is the editorial step.

**Answers worth knowing before you open it.**
- Both critics would put 4 on home, not 11. Grok suggests Panes' split: Agent 4 on the switcher, Today 10 one tap away.
- Astra: Context is "the direction I most want LoR to grow into", and Panes is "the best foundation for an instrument you sit down to use".
- Grok calls Timeline "the one that looks designed", Context "the organisation LoR actually has", and Queue the only concept that treats home as a launcher.

**Defects: one reported, and it did not hold, so nothing was fixed.** Grok read the inner-screen thread (`thread-long/inner`) in Timeline, Panes and Context as having its start covered by the Approve bar. The thread opens at its newest message, as a chat does, so the top message is cut by the scroll position. I scrolled each one up in the published viewer, and all three show step 1 in full under the header. Queue's still happened to stop on a whole message. The page's Defects section says this, and Grok's notes are unchanged. Astra found no defects in what it used.

**Screenshots.** `compare/shots.mjs` remakes the 20 WebP files (704 KB). The phone's home and agent page on the cover screen come from the phone gallery, cropped to the device. Dashboard Today at 1500×950 and 412×892 is taken from the published prototype's first screen, not the gallery: the gallery's `current` afternoon capture at 1500 repeats a band of the page (Astra noticed it too, and lor-jkcf owns it). Both scripts need `playwright-core` or `markdown-it`, which they resolve from the main checkout's `node_modules` by default, or from `LOR_MODULES`. The page's fonts are Hanken Grotesk, copied from the Panes prototype with its OFL.

**Publishing.** `publish.sh`'s copy list now begins with `docs/design/lab/compare|compare|Pick a direction: …`, so the index links it first. I also removed the comment that held its place. I published to scratch `LOR_LAB_DIR`s from this branch and from a fresh clone of it. Both copied `compare/` with its 20 shots and linked it first. The phone and dashboard bundles were skipped there, as expected, since only the main checkout builds them. I never published to the real lab. The page links into `../dashboard/` and `../phone/`, so those links work only once the lead publishes from main. It loads nothing from outside `/lab/compare/`.

**Checked by eye** in Chrome at 390×844 (2×) and 1280×900. The page has no sideways scroll, the screenshots form a sideways strip on the phone, For and Against stack on the phone, and folded notes open. The only console error is the missing favicon's 404. The design follows refactoring-ui and impeccable: one ground, three surfaces and three text tones, amber the only accent, green and red dots beside the For and Against labels, a type scale of 13/15/17/22/32 with two weights, and 44 px targets. It has no eyebrows and no cards inside cards, and the motion is smooth scrolling, which is off under reduced motion.

**What each concept needs from LoR**, as the page states it, from the Reports. Queue and Panes need nothing new. Timeline needs the day's recorded blocks sent to the phone. Context needs goals, pages, week hours, task estimates and the agent's suggested links sent to the phone, and the agent recording which context a T3 thread belongs to.

lor-dinh is the needs-human item asking Sumeet to pick, and this item blocks it.

### Responses

1-5uh5.1: fixed in 6aafff75: Astra watched the recordings of all four concepts (`lor-5fk6`, `lor-j1fe`, `lor-fdtk`, `lor-8aqh` gestures, named in each addendum) and wrote one signed addendum per concept on motion. The addenda follow its unchanged first notes in each critique, the critiques' introductions now say what each critic used, and the page quotes one point for and one against from each addendum.
1-5uh5.2: fixed in cf0db78f: `build.mjs` strips trailing whitespace from every line before writing or checking, so `git diff --check main -- . ':!.atelier'` and `build.mjs --check` both pass.
2-thrg.3: fixed in 8630087e: Grok watched every gesture run under `lor-5fk6`, `lor-j1fe`, `lor-fdtk` and `lor-8aqh` (named in each addendum), from the 50 ms strips and from frames of each recording, and wrote one signed addendum per concept on motion, including Panes' half-sheet rest. The addenda follow its unchanged first notes in each critique. The critiques' introductions say both critics were asked for them at review. The page quotes one point for and one against from each addendum, and its Defects section notes that the addenda repeat the inner-screen clipping claim, which still does not hold.
