---
schema: atelier.item
schema_version: 3
id: lor-npa6
title: 'A design brief: what every concept must cover, and three to five directions to build'
status: draft
mission: lor-2803
labels:
- design-lab
- design
priority: medium
blocked_by:
- lor-nde9
implementers: []
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:54.083533783Z
---

## Goal

Before anything is drawn, one page says what the concepts are for and how they differ, so three to five concepts are real alternatives and each covers the same ground.

## Done when

- [ ] `docs/design/lab/brief.md` states the aim in Sumeet's words of 2026-11-17: the app, launcher and dashboard 'feel designed and polished', 'make common things easy and uncommon things accessible', and are 'a delight to use'; the Elite theme is kept 'only in spirit', and 'nothing in the theme is actually sacred'. It lists his asks for the look: rounded corners, transparency, better separation, better chosen type sizes and scale.
- [ ] It names what is common and what is uncommon on each surface, from the audit's flows and the launch counts in `docs/making-lor-useful.md`, and gives each concept the same required set: on the dashboard Today, Tasks with a task open, Goals and People, at desktop and phone width; on the phone home, the drawer and search, the agent page and LoR panel on both screens, one menu and one sheet, a settings page and the timer; and the same eight interactions working in each (open the drawer, search and open, tick a task, approve a card, answer a question, start and stop a timer, open a task, move an icon).
- [ ] It records the decisions already made, which every concept follows: swipe down on home opens the notification shade; edit mode is gone; one implementation of the timer, of settings and of pairing; the 'no data yet' wording of lor-thnc.
- [ ] It proposes five directions, each a paragraph and three reference images or rough frames, differing in structure and interaction as well as surface (not five palettes), and each written against the `refactoring-ui` skill's areas (hierarchy, spacing, type, colour, depth) and the impeccable skill's Operate guidance.
- [ ] Astra, Grok 4.6 and the third critique seat each read the brief cold, with the audit and the current screens, and say which directions are too alike, which is missing, and what each would get wrong. Their notes are in `docs/design/lab/brief-critiques.md`, and the brief's final three to five directions say what changed because of them.

## Scope

`docs/design/lab/`. No code.

## Context

- The audit: https://claude.ai/artifact/TGotPxRg56dJfqe9kQKMuD and its three inventories. The first look sketch (three levels of one direction): https://claude.ai/artifact/SDuLeHtApftM7kJMZQJinC.
- `refactoring-ui` is at `~/.agents/skills/refactoring-ui`; impeccable is vendored at `.claude/skills/impeccable`.
