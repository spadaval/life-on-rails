---
schema: atelier.item
schema_version: 3
id: lor-q165
title: 'Acceptance: twenty states open by link, and every viewer interaction works in the lab'
status: draft
mission: lor-2803
labels:
- design-lab
- validation
priority: medium
blocked_by:
- lor-glev
- lor-xdoi
- lor-rgx4
- lor-t8lq
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T06:08:49.576981794Z
---

## Goal

Someone who built none of the lab shows that the mission's two acceptance criteria hold: any state opens by a link, and each viewer's interactions work in every concept.

## Done when

- [ ] By link alone, on the test server with the demo set, each of these ten dashboard states draws what its name says, with the link and a screenshot of each: Today at 00:05 before the waking day; Today at 15:00 with a running timer; Today on a past day with a block selected; Tasks with a flagged task open in its panel; Tasks with three rows picked and the bulk bar showing; quick add open with a parent and a date typed; Goals with a habit selected and its editor open; People's list on "Due a hello"; a person's hub with messages and mail; Notes on a page's history with a diff showing.
- [ ] By link alone, in the lab's `current` version, each of these ten launcher and app states draws what its name says on the screen size given, with the link and a screenshot of each: a full home on the cover screen over a pale wallpaper; a first-run home with the set-up card; the drawer's All tab mid-scroll; search with app, contact and settings results; an app's long-press menu; the agent page with one approval, one question and two LoR flags; the agent page with T3 disconnected and stale; a thread open on the inner screen with a long transcript; Sensors after a failed sync with two permissions missing; Home settings with hidden apps and four drawer tabs.
- [ ] At `/lab/`, from a desktop browser and from a phone browser over Tailscale, both viewers switch between `current` and every concept with the Version dropdown. In every concept, each of that viewer's interactions from the brief (lor-npa6) is done by hand and works; a recording of each interaction in one concept per viewer is attached.
- [ ] Each of the mission's three launcher loop measurements after the moves (lor-xdoi, lor-rgx4, lor-t8lq) and the dashboard's (lor-nde9) is repeated once and is within the mission's limits, or the number is with Sumeet.
- [ ] With the browser's network log open, running each viewer's interactions in `current` and one concept makes no request outside `/lab/`.
- [ ] Anything that fails is filed against the item that owns it and fixed before this item is done.

## Scope

Checking only: no code. Done by a thread that implemented none of the mission's items.
