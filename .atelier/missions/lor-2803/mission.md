---
schema: atelier.mission
schema_version: 3
id: lor-2803
title: 'Design lab: fast tools and three to five concepts for the dashboard, launcher and app'
status: open
labels:
- design-lab
lead: null
planners:
- b91a140f-5930-4d4f-9af7-6681cc69ea05
created_at: 2026-11-17T05:56:53.698751316Z
---

## Why

Sumeet, 2026-11-17, after the audit of the dashboard and launcher (its inventories are in `.atelier/missions/lor-2803/audit/`): "the biggest issue with the system is that it's rough, janky, and annoying to use", and fixing it "will take more effort than a single pass. Let's iterate and refine the design." He asked for a small mission "starting with refactors and tool installs and ending with a set of 3-5 concepts, and viewers for me to see and interact with the outputs (a dashboard prototype and a virtual in-browser interactive app mockup (with a dropdown for the version))", and said to treat the tools as "rapid-development tools, not QA tools".

What he wants from the design: the app, launcher and dashboard "feel designed and polished", "make common things easy and uncommon things accessible", and "feel like a delight to use". The Elite-inspired theme stays "only in spirit. Nothing in the theme is actually sacred." His asks for the look: rounded corners, transparency effects, better separation between elements, better chosen font sizes and scales.

Today none of that can be worked on quickly. There is no screenshot set, the test server's screens are empty, and seeing a launcher change means a build on boat, an install on an emulator and driving it by hand. Many states could not be seen at all in the audit: T3 cards, the unfolded screen, a failed sync.

## Outcome

- **Changing a screen is a save-and-see loop.** The dashboard reloads on made-up data in under two seconds. The launcher and app's own Compose code runs in a browser page on this machine and shows a change within about fifteen seconds, with no boat build and no emulator.
- **Any screen in any state is one name away.** A full demo life on the test server, a fixed clock for Today, and named fixtures for every launcher and app surface (first run, twelve cards waiting, T3 unpaired, a failed sync, a thread open on the inner screen), in the browser and in the debug app on a device.
- **Touch and motion can be tried and replayed** in the browser: swipes that follow the finger, long press into drag, fold and unfold, at full or slow speed, by hand or by script with a recording.
- **Every page and state is one command's screenshots,** in a gallery that flips between before and after.
- **Three to five concepts, each one usable.** A dashboard prototype and a phone mockup in the browser, each with a Version dropdown that switches between today's look and every concept, on the same made-up data, opened from the PC and from the phone at `/lab/`.
- **Each concept has three outside critiques** from three labs, one of them outside the United States, on one page that compares the concepts, ready for Sumeet to pick from or mix.

Nothing Sumeet uses changes its look or behaviour in this mission. The current screens move into a shared module unchanged; the concepts live beside them. Rebuilding the real screens to a chosen concept is the next mission.

## Success criteria

- fast-loop: On this machine, with the dev servers running, a one-line change to a dashboard route shows in the open page in under 2 seconds, and a one-line change to a launcher composable shows in the lab in at most 15 seconds, each timed three times. The launcher's time is taken twice: on the empty lab (lor-vbfd's Report), as a first check, and again after home, the panels and the settings and app screens have moved in (the Reports of lor-xdoi, lor-rgx4 and lor-t8lq), which is the measurement that counts. Shown by those Reports and lor-nde9's. If either launcher measurement is over 15 seconds, the lead brings the number to Sumeet before more concepts are built on it.
- any-state: Someone who built none of it opens, by URL alone, each of twenty states picked from the audit's inventory (ten dashboard, ten launcher and app, including one T3 approval, one failed sync and one inner-screen thread), and each draws the state named. The dashboard's links are lor-nde9's and the phone's are lor-oech's. Shown by lor-q165, with the twenty links and screenshots.
- usable-concepts: At `/lab/`, from a desktop browser and from a phone browser over Tailscale, the dashboard prototype and the phone mockup each switch between `current` and at least three concepts with the Version dropdown, and in each concept that viewer's interactions from the brief work by hand (the dashboard's on the dashboard, the phone's on the phone). Shown by lor-q165, with a recording of each of a viewer's interactions in one concept on that viewer, and by lor-glev's comparison page.

## Scope

In: the demo dataset and fixed clock for the test server; a hot-reload dashboard dev server on the test database; the shot script and gallery; the Kotlin 2.3.20 upgrade (lor-3hnk) and Animated text (lor-sghw), which the shared module waits on; the shared module `android/homecore` with its browser build and the lab page; fixtures and the debug app's fixture entry; the gesture scripts; moving home's drawing (lor-xdoi), the agent page, LoR panel, blocks and quick send (lor-rgx4), and Home settings and the app's tabs (lor-t8lq) into the module with no visible change; serving `/lab/` from lor-web's owner listener; the brief; the two viewers; three to five concepts in each; the critiques and comparison page.

lor-3hnk, lor-sghw and lor-xdoi stay items of mission lor-ncpw and are worked first by this mission's lead. lor-xdoi now comes before the phone's layout sync (lor-dkut) instead of after it, and its module set-up moves to lor-vbfd.

Out:
- rebuilding the real dashboard, launcher or app to a concept, and rewriting `DESIGN.md`: the next mission, after Sumeet picks;
- the behaviour changes Sumeet settled on 2026-11-17 (swipe down on home opens the notification shade; edit mode is removed): every concept draws them, and the real app gets them in the next mission;
- the three defects the audit's review found (a failed task tick stays ticked; Today's Ask box clears its own message; sync drops rejected rows): backlog items, not this mission;
- screenshot tests, pixel comparisons as merge checks, or anything that gates a merge on how a screen looks: these are design tools;
- the dashboard's real-size home page and web layout editor (lor-kaq8, lor-u0h7), which stay with lor-ncpw;
- the wall display.

This mission replaces, for design work, the order in `docs/making-lor-useful.md` ("slice first", "what pauses"): Sumeet, 2026-11-17, "Pacing the changes keeps it in a bad state longer; let's nuke that plan and just fix the whole thing." Mission lor-l9a7's items are unaffected and can run beside it.

Environment: this host for the dashboard tools and, capped as decision lor-0v68 says, for `homecore`'s browser build; boat for every Android build and test; `T3_Phone_API35` and `T3_Fold_API35` on the Mac for the before-and-after checks of moved screens; the test database only. Sumeet's phone and Fold are used only as browsers opening `/lab/`.

Roles: implementers are Claude Opus 5.5 and code reviews GPT-6.1 Sol, as usual. The plan's reviewers and the design critics are GPT-6 Astra, Grok 4.6 and Kimi K3 through OpenCode (`opencode-go/kimi-k3`), chosen by Sumeet for range of view, the third to bring a perspective from a model made outside the United States. He named an OpenCode model that was not in its list on 2026-11-17 and then said to pick one with a good name for design; Kimi K3, from Moonshot AI in China, was second on Design Arena's website board in October and November 2026.

## Authority

The lead may add, split, reorder and re-scope items, including one item per concept per viewer; choose library versions, file and package names, fixture names, the gallery's and viewers' design, and which files move in which item; drop a tool that turns out not to be needed for the concepts; and choose the three to five directions after the critics have read the brief. It records each choice on its item.

These come back to Sumeet:
- giving up the browser build of the launcher for another approach, or a launcher loop slower than 15 seconds;
- any visible change to the real dashboard, launcher or app, and anything installed on his phone;
- a new listener, or `/lab/` serving anything but static files;
- fewer than three concepts, or more than five;
- changing the decisions recorded in the brief (lor-npa6).

## Result
